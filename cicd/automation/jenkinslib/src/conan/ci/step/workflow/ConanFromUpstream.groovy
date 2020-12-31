package conan.ci.step.workflow

import conan.ci.arg.*
import conan.ci.docker.DockerClient
import conan.ci.docker.DockerClientFactory
import conan.ci.jenkins.JenkinsAgentFactory
import conan.ci.jenkins.Stage
import conan.ci.runner.DockerCommandRunner
import org.jenkinsci.plugins.workflow.cps.CpsScript

class ConanFromUpstream extends ConanPipeline {

    static ConanFromUpstream construct(CpsScript currentBuild, Map config) {
        def it = new ConanFromUpstream()
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
        DockerClient dockerClient = dockerClientFactory.get(config, args.asMap['dockerDefaultImage'])
        dockerClient.withRun("Start Container") { DockerCommandRunner dcr ->
            dockerClient.configureGit(dcr)
            cloneGitRepos(dcr)
            configureConan(dcr)
            currentBuild.stage("Evaluate Lockfiles") {
                evaluateLockfiles(dcr)
            }
            currentBuild.stage("Launch Builds") {
                launchBuilds(dcr)
            }
        }
    }

    void cloneGitRepos(DockerCommandRunner dcr) {
        dcr.run("git clone ${args.asMap['scriptsUrl']} scripts")
        dcr.run("git clone ${args.asMap['conanConfigUrl']} configs")
        dcr.run("git clone ${args.asMap['conanLocksUrl']} locks")
        dcr.run("git checkout ${args.asMap['lockBranch']}", "locks")
    }

    void evaluateLockfiles(DockerCommandRunner dcr) {
        String packageNameAndVersion = "${args.asMap['packageNameAndVersion']}"
        dcr.run("python scripts/list_lockfile_names.py locks/dev")
        dcr.run("python scripts/copy_direct_upstream_lockfiles.py" +
                " locks/dev locks/dev/${packageNameAndVersion}")
        dcr.run("python scripts/consolidate_lockfiles.py" +
                " --lockfile_base_dir=locks/dev/${packageNameAndVersion} " +
                " --lockfile_names_file=lockfile_names.txt")
        commitLockfileChanges(dcr, "copy and consolidate lockfiles for ${packageNameAndVersion}")
    }

    void commitLockfileChanges(DockerCommandRunner dcr, String message) {
        dcr.run("git add .", "locks")
        String gitLocksStatus = dcr.run("git status", "locks", true)
        currentBuild.echo(gitLocksStatus)
        currentBuild.retry(5){
            dcr.run("git pull", "locks")
            if (!gitLocksStatus.contains("nothing to commit, working tree clean")) {
                dcr.run("git commit -m \"${message} for branch ${args.asMap['lockBranch']}\"", "locks")
            }
            dcr.run("git push -u origin ${args.asMap['lockBranch']}", "locks")
        }
    }

    void launchBuilds(DockerCommandRunner dcr) {
        String packageNameAndVersion = "${args.asMap['packageNameAndVersion']}"
        dcr.run("python scripts/list_lockfile_names.py locks/dev")
        String lockNamesStr = dcr.run(dcr.dockerClient.readFileCommand('lockfile_names.txt'), true)
        List<String> stages = lockNamesStr.trim().split("\n")
        Stage.parallelLimitedBranches(currentBuild, stages, 100) { String stageName ->
            String dockerImageNameFile = "locks/dev/${packageNameAndVersion}/${stageName}/ci_build_env_tag.txt"
            String dockerImageName = dcr.run(dcr.dockerClient.readFileCommand(dockerImageNameFile), true)
            currentBuild.stage(stageName) {
                launchBuild(stageName, dockerImageName)
            }
        }
    }

    void launchBuild(String stageName, String dockerImageName) {
        DockerClient dockerClient = dockerClientFactory.get(config, dockerImageName)
        dockerClient.withRun(stageName) { DockerCommandRunner dcr ->
            dockerClient.configureGit(dcr)
            cloneGitRepos(dcr)
            configureConan(dcr)
            performConanBuild(dcr, stageName)
        }
    }

    void performConanBuild(DockerCommandRunner dcr, String lockfileDir) {
        String user = args.asMap['conanUser']
        String channel = args.asMap['conanChannel']
        String packageNameAndVersion = "${args.asMap['packageNameAndVersion']}"
        String targetPkgNameVersion = "${packageNameAndVersion}"
        String targetPkgRef = "${packageNameAndVersion}@${user}/${channel}"
        dcr.run("conan install ${targetPkgRef}" +
                " --build ${targetPkgRef}" +
                " --lockfile locks/dev/${targetPkgNameVersion}/${lockfileDir}/conan.lock" +
                " --lockfile-out=locks/dev/${targetPkgNameVersion}/${lockfileDir}/conan-new.lock")

        dcr.run("conan lock build-order" +
                " locks/dev/${targetPkgNameVersion}/${lockfileDir}/conan-new.lock" +
                " --json=locks/dev/${targetPkgNameVersion}/${lockfileDir}/build-order.json")

        dcr.run("conan upload ${targetPkgNameVersion}@* --all -r ${args.asMap['conanRemoteUploadName']} --confirm")

        commitLockfileChanges(dcr, "update locks for downstream build of product pipeline")
    }
}