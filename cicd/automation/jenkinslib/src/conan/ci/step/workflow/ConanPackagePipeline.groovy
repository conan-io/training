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
        dcr.run("python scripts/find_source_branch.py --git_dir=workspace")
        dcr.run("python scripts/calculate_lock_branch_name.py --conanfile_dir=workspace")
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
        dcr.run("python scripts/copy_lockfiles_containing_package.py" +
                " --conanfile_dir=workspace locks/prod locks/dev")
    }

    void createPackageIdMap(DockerCommandRunner dcr) {
        dcr.run("python scripts/create_package_id_map.py" +
                " --conanfile_dir=workspace locks/dev")
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
        String packageIdMapStr = dcr.run(dcr.dockerClient.readFileCommand('package_id_map.txt'), true)
        base.currentBuild.echo("packageIdMapStr : ${packageIdMapStr}")
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
                base.currentBuild.echo(
                        "The lockfiles in these directories will all be updated in the build of this packageId: \n" +
                                "packageId : " + stageName + "\n" +
                                "lockfileDirs : \n" +
                                packageIdMap[stageName].join("\n")
                )
                launchBuildContainer(stageName, packageIdMap[stageName], dockerImageName)
            }
        }
    }

    void launchBuildContainer(String stageName, List<String> lockfileDirs, String dockerImageName) {
        DockerClient dockerClient = base.dockerClientFactory.get(base.config, dockerImageName)
        dockerClient.withRun(stageName) { DockerCommandRunner dcr ->
            dockerClient.configureGit(dcr)
            cloneGitRepos(dcr)
            checkoutLockBranch(dcr)
            base.configureConan(dcr)
            //TODO: convert the function below into a python script or 2
            performConanBuild(dcr, lockfileDirs)
        }
    }

    void performConanBuild(DockerCommandRunner dcr, List<String> lockfileDirs) {

        String user = base.args.asMap['conanUser']
        String channel = base.args.asMap['conanChannel']
        String targetPkgName = dcr.run("conan inspect workspace --raw name", true)
        String targetPkgVersion = dcr.run("conan inspect workspace --raw version", true)
        String targetPkgNameVersion = "${targetPkgName}/${targetPkgVersion}"

        // First we have to install each product into the local cache before any calls to conan create
        // Conan create produces produces a new revision in the cache, making conan refuse subsequent lockfile installs
        lockfileDirs.each { String lockfileDir ->
            def (String lockfilePkgName, String lockfilePkgVersion, _) = lockfileDir.split("/")
            String lockfilePkgRef = "${lockfilePkgName}/${lockfilePkgVersion}@${user}/${channel}"
            dcr.run("conan install ${lockfilePkgRef} --lockfile locks/dev/${targetPkgNameVersion}/${lockfileDir}")
        }

        // Now, we loop over each lockfile and produce a conan-new.lock
        lockfileDirs.each { String lockfileDir ->
            def (String lockfilePkgName, String lockfilePkgVersion, _) = lockfileDir.split("/")
            String lockfilePkgRef = "${lockfilePkgName}/${lockfilePkgVersion}@${user}/${channel}"

            // Because many lockfiles might share the same package_id for a build, we have special handling
            // We only perform a full rebuild with "conan create" once (on the first lockfile)
            // For subsequent lockfiles, we call conan create but with --keep-build and --test-package=None
            // This bypasses the rebuild but updates the lockfiles with the new revision as if it was rebuilt
            // This is a workaround in lieu of better first-class feature to achieve this outcome
            String extraFlags = (lockfileDir == lockfileDirs.head()) ? " --keep-build --test-folder=None" : ""

            dcr.run("conan lock create workspace/conanfile.py --user ${user} --channel ${channel}" +
                    " --lockfile=locks/dev/${targetPkgNameVersion}/${lockfileDir}/conan.lock" +
                    " --lockfile-out=locks/dev/${targetPkgNameVersion}/${lockfileDir}/temp1.lock")

            dcr.run("conan create workspace ${user}/${channel} " +
                    " --lockfile=locks/dev/${targetPkgNameVersion}/${lockfileDir}/temp1.lock" +
                    " --lockfile-out=locks/dev/${targetPkgNameVersion}/${lockfileDir}/temp2.lock" +
                    " --build ${targetPkgName}" + extraFlags)

            dcr.run("conan lock create --reference ${lockfilePkgRef}" +
                    " --lockfile=locks/dev/${targetPkgNameVersion}/${lockfileDir}/temp2.lock" +
                    " --lockfile-out=locks/dev/${targetPkgNameVersion}/${lockfileDir}/conan-new.lock")
        }

        dcr.run("conan upload ${targetPkgNameVersion}@* --all -r ${base.args.asMap['conanRemoteUploadName']} --confirm")

        commitLockfileChanges(dcr, "update locks for package pipeline for package: " +
                "${targetPkgNameVersion} ${lockfileDirs.join(",")} "
        )
    }
}