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

class ConanRebaseLockfiles {

    PipelineBase base
    static ConanRebaseLockfiles construct(CpsScript currentBuild, Map config) {
        def it = new ConanRebaseLockfiles()
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
                evaluateLockfiles(dcr)
                createPackageIdMap(dcr)
            }
            if(readPackageIdMap(dcr).isEmpty()){
                base.currentBuild.echo("No lockfiles found containing this package, nothing to do, returning.")
                return
            }
            commitLockfileChanges(dcr, "copy and consolidate lockfiles for ${base.args.asMap['packageNameAndVersion']}")
            base.currentBuild.stage("Launch Builds") {
                launchBuildContainers(dcr)
            }
        }
    }

    void cloneGitRepos(DockerCommandRunner dcr) {
        dcr.run("git clone ${base.args.asMap['scriptsUrl']} scripts")
        dcr.run("git clone ${base.args.asMap['conanConfigUrl']} configs")
        dcr.run("git clone ${base.args.asMap['conanLocksUrl']} locks")
        dcr.run("git checkout ${base.args.asMap['lockBranch']}", "locks")
    }

    void evaluateLockfiles(DockerCommandRunner dcr) {
        String nameAndVersion = "${base.args.asMap['packageNameAndVersion']}"
        dcr.run("python ~/scripts/list_lockfile_names.py locks/dev")
        dcr.run("python ~/scripts/copy_direct_upstream_lockfiles.py" +
                " locks/dev ${nameAndVersion}")

        dcr.run("python ~/scripts/consolidate_lockfiles.py" +
                " --lockfile_base_dir=locks/dev/${nameAndVersion} ")
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
        if (!gitLocksStatus.contains("nothing to commit, working tree clean")) {
            dcr.run("git commit -m \"${message} for branch ${base.args.asMap['lockBranch']}\"", "locks")
        }
        base.currentBuild.retry(5){
            dcr.run("git pull", "locks")
            dcr.run("git push -u origin ${base.args.asMap['lockBranch']}", "locks")
        }
    }

    void launchBuildContainers(DockerCommandRunner dcr) {
        Map<String, List<String>> packageIdMap = readPackageIdMap(dcr)
        List<String> stages = packageIdMap.keySet() as List
        base.currentBuild.echo("Preparing build stages based on the following packageIdMap: \n" +
                new JsonBuilder(packageIdMap).toPrettyString()
        )
        String packageNameAndVersion = "${base.args.asMap['packageNameAndVersion']}"
        Stage.parallelLimitedBranches(base.currentBuild, stages, 100) { String stageName ->
            String firstLockFileDir = packageIdMap[stageName].head()
            String dockerImageNameFile = "locks/dev/${packageNameAndVersion}/${firstLockFileDir}/ci_build_env_tag.txt"
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
            base.configureConan(dcr)
            performConanBuild(dcr, stageName)
        }
    }

    void performConanBuild(DockerCommandRunner dcr, String packageId) {
        String nameAndVersion = "${base.args.asMap['packageNameAndVersion']}"

        // re-generate the package_id_map.json file inside the container
        dcr.run("python ~/scripts/create_package_id_map.py" +
                " --conanfile_dir=workspace locks/dev")

        // run conan install for each product, the script will build once with first lockfile, and update the rest
        dcr.run("python ~/scripts/conan_install_build_and_update_all_lockfiles.py" +
                " --conanfile_dir=workspace ${packageId} locks/dev")

        dcr.run("conan upload ${nameAndVersion}@* --all -r ${base.args.asMap['conanRemoteUploadName']} --confirm")

        commitLockfileChanges(dcr, "update locks for downstream build of product pipeline")
    }
}