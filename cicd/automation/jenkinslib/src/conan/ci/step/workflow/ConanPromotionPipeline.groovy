package conan.ci.step.workflow

import conan.ci.arg.*
import conan.ci.docker.DockerClient
import conan.ci.docker.DockerClientFactory
import conan.ci.jenkins.JenkinsAgentFactory
import conan.ci.jenkins.Stage
import conan.ci.runner.DockerCommandRunner
import org.jenkinsci.plugins.workflow.cps.CpsScript

class ConanPromotionPipeline extends ConanPipeline {

    static ConanPromotionPipeline construct(CpsScript currentBuild, Map config) {
        def it = new ConanPromotionPipeline()
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
            currentBuild.stage("Evaluate Lockfiles") {
                evaluateLockfiles(dcr)
            }
            currentBuild.stage("Promote Packages") {
                launchPromotionContainers(dcr)
            }
            currentBuild.stage("Promote Lockfiles") {
                commitLockfileChanges(dcr, "promote dev locks to prod")
                deleteDevLockfiles(dcr)
                pushLockfileChanges(dcr)
            }
        }
    }

    void cloneGitRepos(DockerCommandRunner dcr) {
        dcr.run("git clone ${args.asMap['scriptsUrl']} scripts")
        dcr.run("git clone ${args.asMap['conanConfigUrl']} configs")
        dcr.run("git clone ${args.asMap['conanLocksUrl']} locks")
        dcr.run("git clone ${args.asMap['gitUrl']} workspace")
        dcr.run("git checkout ${args.asMap['gitCommit']}", "workspace")
        dcr.run("python scripts/find_source_branch.py --git_dir=workspace")
        dcr.run("python scripts/calculate_lock_branch_name.py --conanfile_dir=workspace")
        String lockBranch = dcr.run(dcr.dockerClient.readFileCommand('lock_branch_name.txt'), true)
        //TODO: Replace the following command with cross-platform alternative
        dcr.run("git checkout ${lockBranch} 2>/dev/null || git checkout -B ${lockBranch}", "locks")
    }

    void evaluateLockfiles(DockerCommandRunner dcr) {
        dcr.run("python scripts/list_product_lockfiles_in_dev.py locks/dev")
    }

    void launchPromotionContainers(DockerCommandRunner dcr) {
        String lockNamesStr = dcr.run(dcr.dockerClient.readFileCommand('lockfile_names.txt'), true)
        List<String> stages = lockNamesStr.trim().split("\n")
        String pkgName = dcr.run("conan inspect workspace --raw name", true)
        String pkgVersion = dcr.run("conan inspect workspace --raw version", true)
        Stage.parallelLimitedBranches(currentBuild, stages, 100) { String stageName ->
            String dockerImageNameFile = "locks/dev/${pkgName}/${pkgVersion}/${stageName}/ci_build_env_tag.txt"
            String dockerImageName = dcr.run(dcr.dockerClient.readFileCommand(dockerImageNameFile), true)
            currentBuild.stage(stageName) {
                launchPromotionContainer(stageName, dockerImageName)
            }
        }
    }

    void launchPromotionContainer(String stageName, String dockerImageName) {
        DockerClient dockerClient = dockerClientFactory.get(config, dockerImageName)
        dockerClient.withRun(stageName) { DockerCommandRunner dcr ->
            dockerClient.configureGit(dcr)
            cloneGitRepos(dcr)
            configureConan(dcr)
            performConanInstall(dcr, stageName)
        }
    }

    void performConanInstall(DockerCommandRunner dcr, String lockfileDir) {
        String user = args.asMap['conanUser']
        String channel = args.asMap['conanChannel']
        def (name, ver, _) = lockfileDir.split("/")
        String nameAndVersion = "${name}/${ver}"
        String targetPkgRef = "${name}/${ver}@${user}/${channel}"
        dcr.run("conan install ${targetPkgRef}" +
                " --lockfile locks/dev/${nameAndVersion}/${lockfileDir}/conan.lock")

        dcr.run("conan upload \"*\" --all -r ${args.asMap['conanRemoteUploadName']} --confirm")
    }

    void commitLockfileChanges(DockerCommandRunner dcr, String message) {
        String lockBranch = dcr.run(dcr.dockerClient.readFileCommand('lock_branch_name.txt'), true)
        dcr.run("git checkout develop", "locks")
        dcr.run("git merge ${lockBranch}", "locks")
        dcr.run("python scripts/copy_dev_lockfiles_to_prod.py locks/dev locks/prod")
        dcr.run("git add .", "locks")
        dcr.run("git commit -m \"${message} for branch ${lockBranch}\"", "locks")
    }

    void deleteDevLockfiles(DockerCommandRunner dcr) {
        String lockBranch = dcr.run(dcr.dockerClient.readFileCommand('lock_branch_name.txt'), true)
        dcr.run("python scripts/remove_lockfiles_dir.py locks/dev")
        dcr.run("git add .", "locks")
        dcr.run("git commit -m \"Remove lockfiles from branch ${lockBranch}\"", "locks")
    }

    void pushLockfileChanges(DockerCommandRunner dcr) {
        dcr.run("git push -u origin develop", "locks")
    }
}