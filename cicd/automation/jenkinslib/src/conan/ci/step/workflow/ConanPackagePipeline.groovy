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

class ConanPackagePipeline {

    PipelineBase base

    static ConanPackagePipeline construct(CpsScript currentBuild, Map config) {
        def it = new ConanPackagePipeline()
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
            base.currentBuild.stage("Evaluate Lockfiles") {
                checkoutLockBranch(dcr)
                evaluateLockfiles(dcr)
                createPackageIdMap(dcr)
                commitLockfileChanges(dcr, "initialize locks")
            }
            if(readPackageIdMap(dcr).isEmpty()){
                base.currentBuild.echo("No lockfiles found containing this package, nothing to do, returning.")
                return
            }
            base.currentBuild.stage("Launch Builds") {
                launchBuildContainers(dcr)
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
        if (lockBranchExists.trim()) {
            dcr.run("git checkout ${lockBranch}", "locks")
        } else {
            dcr.run("git checkout -b ${lockBranch}", "locks")
            dcr.run("git push -u origin ${lockBranch}", "locks")
        }
    }

    void evaluateLockfiles(DockerCommandRunner dcr) {
        dcr.run("python ~/scripts/copy_lockfiles_containing_package.py" +
                " --conanfile_dir=workspace locks/prod locks/dev")
    }

    void createPackageIdMap(DockerCommandRunner dcr) {
        dcr.run("python ~/scripts/create_package_id_map.py" +
                " --conanfile_dir=workspace locks/dev")
    }

    Map<String, List<String>> readPackageIdMap(DockerCommandRunner dcr) {
        String packageIdMapStr = dcr.run(dcr.dockerClient.readFileCommand('package_id_map.json'), true)
        return base.currentBuild.readJSON(text: packageIdMapStr, returnPojo: true) as Map<String, List<String>>
    }

    void commitLockfileChanges(DockerCommandRunner dcr, String message) {
        dcr.run("git add .", "locks")
        String gitLocksStatus = dcr.run("git status", "locks", true)
        base.currentBuild.echo(gitLocksStatus)
        String lockBranch = dcr.run(dcr.dockerClient.readFileCommand('lock_branch_name.txt'), true)
        if (!gitLocksStatus.contains("nothing to commit, working tree clean")) {
            dcr.run("git commit -m \"${message} for branch ${lockBranch}\"", "locks")
        }
        base.currentBuild.retry(5) {
            dcr.run("git pull", "locks")
            dcr.run("git push -u origin ${lockBranch}", "locks")
        }
    }

    void launchBuildContainers(DockerCommandRunner dcr) {
        Map<String, List<String>> packageIdMap = readPackageIdMap(dcr)
        List<String> stages = packageIdMap.keySet() as List
        String pkgName = dcr.run("conan inspect workspace --raw name", true)
        String pkgVersion = dcr.run("conan inspect workspace --raw version", true)

        Stage.parallelLimitedBranches(base.currentBuild, stages, 100) { String stageName ->
            String firstLockFileDir = packageIdMap[stageName].head()
            String dockerImageNameFile = "locks/dev/${pkgName}/${pkgVersion}/${firstLockFileDir}/ci_build_env_tag.txt"
            String dockerImageName = dcr.run(dcr.dockerClient.readFileCommand(dockerImageNameFile), true)
            base.currentBuild.stage(stageName) {
                base.currentBuild.echo(
                        "The lockfiles in these directories will all be updated in the build of this packageId: \n" +
                                "packageId : " + stageName + "\n" +
                                "lockfileDirs : \n" +
                                packageIdMap[stageName].join("\n")
                )
                launchBuildContainer(stageName, dockerImageName)
            }
        }
    }

    void launchBuildContainer(String stageName, String dockerImageName) {
        DockerClient dockerClient = base.dockerClientFactory.get(base.config, dockerImageName)
        dockerClient.withRun(stageName) { DockerCommandRunner dcr ->
            dockerClient.configureGit(dcr)
            cloneGitRepos(dcr)
            checkoutLockBranch(dcr)
            base.configureConan(dcr)
            performConanBuild(dcr, stageName)
        }
    }

    void performConanBuild(DockerCommandRunner dcr, String packageId) {
        String targetPkgName = dcr.run("conan inspect workspace --raw name", true)
        String targetPkgVersion = dcr.run("conan inspect workspace --raw version", true)
        String targetPkgNameVersion = "${targetPkgName}/${targetPkgVersion}"

        // re-generate the package_id_map.json file inside the container
        dcr.run("python ~/scripts/create_package_id_map.py" +
                " --conanfile_dir=workspace locks/dev")

        // install all the packages from all lockfiles to establish the cache with all locked versions/revisions
        dcr.run("python ~/scripts/conan_install_all_lockfiles.py" +
                " --conanfile_dir=workspace ${packageId} locks/dev")

        // re-create the target package and update all the lockfiles for that package id
        dcr.run("python ~/scripts/conan_create_and_update_all_lockfiles.py" +
                " --conanfile_dir=workspace ${packageId} locks/dev")

        // upload the new build of the target package
        dcr.run("conan upload ${targetPkgNameVersion}@* --all -r ${base.args.asMap['conanRemoteUploadName']} --confirm")

        // commit and push the updated lockfiles
        commitLockfileChanges(dcr, "update locks for package pipeline for packageId: ${packageId}")
    }
}