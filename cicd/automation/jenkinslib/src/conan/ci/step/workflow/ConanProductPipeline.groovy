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
                checkoutLockBranch(dcr)
                calculateBuildOrder(dcr)
                commitLockfileChanges(dcr, "add build order files from product pipeline")
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
    }


    void checkoutLockBranch(DockerCommandRunner dcr) {
        dcr.run("python scripts/find_source_branch.py --git_dir=workspace")
        dcr.run("python scripts/calculate_lock_branch_name.py --conanfile_dir=workspace")
        String lockBranch = dcr.run(dcr.dockerClient.readFileCommand('lock_branch_name.txt'), true)
        dcr.run("git checkout ${lockBranch}", "locks")
    }

    void calculateBuildOrder(DockerCommandRunner dcr) {
        String pkgName = dcr.run("conan inspect workspace --raw name", true)
        String pkgVersion = dcr.run("conan inspect workspace --raw version", true)
        dcr.run("python scripts/list_lockfile_names.py locks/dev/${pkgName}/${pkgVersion}")
        dcr.run("python ~/scripts/create_combined_build_order.py locks/dev/${pkgName}/${pkgVersion}")
        String cbo = dcr.run(dcr.dockerClient.readFileCommand(
                "locks/dev/${pkgName}/${pkgVersion}/combined_build_order.json"), true)

        currentBuild.echo("Combined Build Order : ${cbo}")

    }

    void commitLockfileChanges(DockerCommandRunner dcr, String message) {
        dcr.run("git add .", "locks")
        String gitLocksStatus = dcr.run("git status", "locks", true)
        currentBuild.echo(gitLocksStatus)
        String lockBranch = dcr.run(dcr.dockerClient.readFileCommand('lock_branch_name.txt'), true)
        if (!gitLocksStatus.contains("nothing to commit, working tree clean")) {
            dcr.run("git commit -m \"${message} for branch ${lockBranch}\"", "locks")
        }
        currentBuild.retry(5) {
            dcr.run("git pull", "locks")
            dcr.run("git push -u origin ${lockBranch}", "locks")
        }
    }

    void triggerDownstreamJobs(DockerCommandRunner dcr) {
        String pkgName = dcr.run("conan inspect workspace --raw name", true)
        String pkgVersion = dcr.run("conan inspect workspace --raw version", true)
        String cbo = dcr.run(dcr.dockerClient.readFileCommand(
                "locks/dev/${pkgName}/${pkgVersion}/combined_build_order.json"), true)

        def cboJson = currentBuild.readJSON(text: cbo)
        cboJson.each { String level, List<String> pkgNameAndVersions ->
            Stage.parallelLimitedBranches(currentBuild, pkgNameAndVersions, 100) { String pkgNameAndVersion ->
                triggerDownstreamJob(dcr, pkgNameAndVersion)
            }
        }
    }

    void triggerDownstreamJob(DockerCommandRunner dcr, String pkgNameAndVersion) {
        String lockBranch = dcr.run(dcr.dockerClient.readFileCommand('lock_branch_name.txt'), true)
        String pkgName = pkgNameAndVersion.split("/")[0]
        String targetBranch = "conan_from_upstream"
        currentBuild.build(
                job: "gitbucket/${pkgName}/${targetBranch}" as String,
                parameters: [
                        [$class: 'StringParameterValue', name: 'LOCK_BRANCH', value: lockBranch],
                        [$class: 'StringParameterValue', name: 'PACKAGE_NAME_AND_VERSION', value: pkgNameAndVersion],
                ]
        )
    }
}