package conan.ci.step.workflow

import conan.ci.arg.*
import conan.ci.docker.DockerClient
import conan.ci.docker.DockerClientFactory
import conan.ci.jenkins.JenkinsAgentFactory
import conan.ci.jenkins.Stage
import conan.ci.runner.DockerCommandRunner
import org.jenkinsci.plugins.workflow.cps.CpsScript

class ConanProductPipeline extends ConanPipeline {

    static ConanProductPipeline construct(CpsScript currentBuild, Map config) {
        def it = new ConanProductPipeline()
        it.currentBuild = currentBuild
        it.jenkinsAgent = JenkinsAgentFactory.construct(currentBuild).get(config)
        it.dockerClientFactory = DockerClientFactory.construct(currentBuild)
        it.args = ArgumentList.construct(currentBuild, it.pipelineArgs())
        it.args.addAll(ConanArgs.get() + DockerArgs.get() + GitArgs.get() + ArtifactoryArgs.get())
        it.args.parseArgs(config)
        it.config = config
        it.printArgs()
        return it
    }

    void run() {
        //detect successive commits and rebuild package under product
        DockerClient dockerClient = dockerClientFactory.get(config, args.asMap['dockerDefaultImage'])
        dockerClient.withRun("Start Container") { DockerCommandRunner dcr ->
            dockerClient.configureGit(dcr)
            cloneGitRepos(dcr)
            configureConan(dcr)
            currentBuild.stage("Calculate BuildOrder") {
                calculateBuildOrder(dcr)
            }
            currentBuild.stage("Trigger Downstreams") {
                triggerDownstreamJobs(dcr)
            }
        }
    }

    void cloneGitRepos(DockerCommandRunner dcr) {
        dcr.run("git clone ${args.asMap['scriptsUrl']} scripts")
        dcr.run("git clone ${args.asMap['conanConfigUrl']} configs")
        dcr.run("git clone ${args.asMap['conanLocksUrl']} locks")
        dcr.run("git clone ${args.asMap['gitUrl']} workspace")
        dcr.run("git checkout ${args.asMap['gitCommit']}", "workspace")
        dcr.run("python scripts/calculate_lock_branch_name.py --conanfile_dir=workspace")
        String lockBranch = dcr.run(dcr.dockerClient.readFile('lock_branch_name.txt'))
        //TODO: Replace the following command with cross-platform alternative
        dcr.run("git checkout ${lockBranch} 2>/dev/null || git checkout -B ${lockBranch}", "locks")
    }

    void calculateBuildOrder(DockerCommandRunner dcr) {
        String pkgName = dcr.run("conan inspect workspace --raw name")
        String pkgVersion = dcr.run("conan inspect workspace --raw version")
        dcr.run("python scripts/list_lockfile_names.py locks/dev/${pkgName}/${pkgVersion}")
        dcr.run("python ~/scripts/create_combined_build_order.py locks/dev/${pkgName}/${pkgVersion}")
        String cbo = dcr.run(dcr.dockerClient.readFile("locks/dev/${pkgName}/${pkgVersion}/combined_build_order.json"))
        currentBuild.echo("Combined Build Order : ${cbo}")

        dcr.run("python ~/scripts/list_next_build_order_level.py" +
                " locks/dev/${pkgName}/${pkgVersion}/combined_build_order.json" +
                " --output_file=locks/dev/${pkgName}/${pkgVersion}/next_level.txt")
        String nbol = dcr.run(dcr.dockerClient.readFile("locks/dev/${pkgName}/${pkgVersion}/next_level.txt"))
        currentBuild.echo("next_build_order_level : ${nbol}")

        commitLockfileChanges(dcr, "add build order files from product pipeline")
    }

    void commitLockfileChanges(DockerCommandRunner dcr, String message) {
        dcr.run("git pull", "locks") // Support diamond deps 
        dcr.run("git add .", "locks")
        String gitLocksStatus = dcr.run("git status", "locks")
        currentBuild.echo(gitLocksStatus)
        if (!gitLocksStatus.contains("nothing to commit, working tree clean")) {
            String lockBranch = dcr.run(dcr.dockerClient.readFile('lock_branch_name.txt'))
            dcr.run("git commit -m \"${message} for branch ${lockBranch}\"", "locks")
            dcr.run("git push -u origin ${lockBranch}", "locks")
        }
    }

    void triggerDownstreamJobs(DockerCommandRunner dcr) {
        String pkgName = dcr.run("conan inspect workspace --raw name")
        String pkgVersion = dcr.run("conan inspect workspace --raw version")
        String cbo = dcr.run(dcr.dockerClient.readFile("locks/dev/${pkgName}/${pkgVersion}/combined_build_order.json"))
        def cboJson = currentBuild.readJSON(text: cbo)
        cboJson.each { String level, List<String> pkgRefs ->
            List<String> pkgNameAndVersions = pkgRefs.collect { it.split("@")[0] }
            Stage.parallelLimitedBranches(currentBuild, pkgNameAndVersions, 100) { String pkgNameAndVersion ->
                triggerDownstreamJob(dcr, pkgNameAndVersion)
            }
        }
    }

    void triggerDownstreamJob(DockerCommandRunner dcr, String pkgNameAndVersion) {
        String lockBranch = dcr.run(dcr.dockerClient.readFile('lock_branch_name.txt'))
        String pkgName = pkgNameAndVersion.split("/")[0]
        String targetBranch = "conan_from_upstream"
        currentBuild.build(
                job: "gitbucket/${pkgName}/${targetBranch}" as String,
                parameters: [[$class: 'StringParameterValue', name: 'LOCK_BRANCH', value: lockBranch]]
        )
    }
}