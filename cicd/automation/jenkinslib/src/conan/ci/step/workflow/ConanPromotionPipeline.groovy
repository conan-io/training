package conan.ci.step.workflow

import conan.ci.arg.*
import conan.ci.docker.DockerClient
import conan.ci.docker.DockerClientFactory
import conan.ci.jenkins.JenkinsAgentFactory
import conan.ci.jenkins.Stage
import conan.ci.pipeline.PipelineBase
import conan.ci.runner.DockerCommandRunner
import groovy.json.JsonBuilder
import org.jenkinsci.plugins.workflow.cps.CpsScript

class ConanPromotionPipeline {

    PipelineBase base
    static ConanPromotionPipeline construct(CpsScript currentBuild, Map config) {
        def it = new ConanPromotionPipeline()
        it.base = new PipelineBase()
        it.base.currentBuild = currentBuild
        it.base.jenkinsAgent = JenkinsAgentFactory.construct(currentBuild).get(config)
        it.base.dockerClientFactory = DockerClientFactory.construct(currentBuild)
        it.base.args = ArgumentList.construct(currentBuild, it.base.pipelineArgs())
        it.base.args.addAll(ConanArgs.get() + DockerArgs.get() + GitArgs.get() + ArtifactoryArgs.get())
        it.base.args.parseArgs(config)
        it.base.config = config
        it.base.printClass(it.class.simpleName)
        it.base.printArgs()
        return it
    }

    void run() {
        DockerClient dockerClient = base.dockerClientFactory.get(base.config, base.args.asMap['dockerDefaultImage'])
        dockerClient.withRun("Start Container") { DockerCommandRunner dcr ->
            dockerClient.configureGit(dcr)
            cloneGitRepos(dcr)
            base.configureConan(dcr)
            base.currentBuild.stage("Evaluate Lockfiles") {
                checkoutLockBranch(dcr)
                evaluateLockfiles(dcr)
                createPackageIdMap(dcr)
            }
            base.currentBuild.stage("Promote Packages") {
                launchPromotionContainers(dcr)
            }
            base.currentBuild.stage("Promote Lockfiles") {
                commitLockfileChanges(dcr, "promote dev locks to prod")
            }
        }
    }

    void cloneGitRepos(DockerCommandRunner dcr) {
        dcr.run("git clone ${base.args.asMap['scriptsUrl']} scripts")
        dcr.run("git clone ${base.args.asMap['conanConfigUrl']} configs")
        dcr.run("git clone ${base.args.asMap['conanLocksUrl']} locks")
        dcr.run("git clone ${base.args.asMap['gitUrl']} workspace")
        dcr.run("git checkout ${base.args.asMap['gitCommit']}", "workspace")
    }

    void checkoutLockBranch(DockerCommandRunner dcr) {
        dcr.run("python ~/scripts/find_source_branch.py --git_dir=workspace")
        dcr.run("python ~/scripts/calculate_lock_branch_name.py --conanfile_dir=workspace")
        String lockBranch = dcr.run(dcr.dockerClient.readFileCommand('lock_branch_name.txt'), true)
        String lockBranchExists = dcr.run("git branch --all --list *${lockBranch}", "locks", true)
        if(lockBranchExists.trim()){
            dcr.run("git checkout ${lockBranch}", "locks")
        }else{
            dcr.run("git checkout -b ${lockBranch}", "locks")
            dcr.run("git push -u origin ${lockBranch}", "locks")
        }
    }

    void evaluateLockfiles(DockerCommandRunner dcr) {
        dcr.run("python ~/scripts/list_product_lockfiles_in_dev.py locks/dev")
    }

    void createPackageIdMap(DockerCommandRunner dcr) {
        dcr.run("python ~/scripts/create_package_id_map.py" +
                " --conanfile_dir=workspace locks/dev")
    }

    void launchPromotionContainers(DockerCommandRunner dcr) {
        String packageIdMapStr = dcr.run(dcr.dockerClient.readFileCommand('package_id_map.txt'), true)
        Map<String, List<String>> packageIdMap = packageIdMapStr.split("\n").collectEntries { String line ->
            def (String packageId, String lockfileDirsStr) = line.split(":")
            [(packageId): lockfileDirsStr.split(',').toList()]
        }
        List<String> stages = packageIdMap.keySet() as List
        base.currentBuild.echo("Preparing build stages based on the following packageIdMap: \n" +
                new JsonBuilder(packageIdMap).toPrettyString()
        )
        String pkgName = dcr.run("conan inspect workspace --raw name", true)
        String pkgVersion = dcr.run("conan inspect workspace --raw version", true)
        Stage.parallelLimitedBranches(base.currentBuild, stages, 100) { String stageName ->
            String firstLockFileDir = packageIdMap[stageName].head()
            String dockerImageNameFile = "locks/dev/${pkgName}/${pkgVersion}/${firstLockFileDir}/ci_build_env_tag.txt"
            String dockerImageName = dcr.run(dcr.dockerClient.readFileCommand(dockerImageNameFile), true)
            base.currentBuild.stage(stageName) {
                launchPromotionContainer(stageName, dockerImageName)
            }
        }
    }

    void launchPromotionContainer(String stageName, String dockerImageName) {
        DockerClient dockerClient = base.dockerClientFactory.get(base.config, dockerImageName)
        dockerClient.withRun(stageName) { DockerCommandRunner dcr ->
            dockerClient.configureGit(dcr)
            cloneGitRepos(dcr)
            checkoutLockBranch(dcr)
            base.configureConan(dcr)
            performConanInstall(dcr, stageName)
        }
    }

    void performConanInstall(DockerCommandRunner dcr, String packageId) {
        // re-generate the package_id_map.txt file inside the container
        dcr.run("python ~/scripts/create_package_id_map.py" +
                " --conanfile_dir=workspace locks/dev")

        // install all the packages from all lockfiles for this packageId for promotion
        dcr.run("python ~/scripts/conan_install_all_lockfiles.py" +
                " --conanfile_dir=workspace ${packageId} locks/dev")

        dcr.run("conan upload \"*\" --all -r ${base.args.asMap['conanRemoteUploadName']} --confirm")
    }

    void commitLockfileChanges(DockerCommandRunner dcr, String message) {
        String lockBranch = dcr.run(dcr.dockerClient.readFileCommand('lock_branch_name.txt'), true)
        dcr.run("python ~/scripts/promote_dev_lockfiles_to_prod.py locks/dev locks/prod")
        dcr.run("python ~/scripts/remove_lockfiles_dir.py locks/dev")
        dcr.run("git add .", "locks")
        String gitLocksStatus = dcr.run("git status", "locks", true)
        base.currentBuild.echo(gitLocksStatus)
        if (!gitLocksStatus.contains("nothing to commit, working tree clean")) {
            dcr.run("git commit -m \"${message} for branch ${lockBranch}\"", "locks")
        }
        base.currentBuild.retry(5){
            dcr.run("git checkout ${lockBranch}", "locks")
            dcr.run("git merge develop -s ours", "locks")
            dcr.run("git checkout develop", "locks")
            dcr.run("git merge ${lockBranch}", "locks")
            dcr.run("git pull", "locks")
            dcr.run("git push -u origin develop", "locks")
        }
    }
}