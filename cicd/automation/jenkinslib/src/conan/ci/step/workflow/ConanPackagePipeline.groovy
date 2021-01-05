package conan.ci.step.workflow

import conan.ci.arg.*
import conan.ci.docker.DockerClient
import conan.ci.docker.DockerClientFactory
import conan.ci.jenkins.JenkinsAgentFactory
import conan.ci.jenkins.Stage
import conan.ci.runner.DockerCommandRunner
import groovy.json.JsonBuilder
import org.jenkinsci.plugins.workflow.cps.CpsScript

class ConanPackagePipeline extends ConanPipeline {

    static ConanPackagePipeline construct(CpsScript currentBuild, Map config) {
        def it = new ConanPackagePipeline()
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
            currentBuild.stage("Evaluate Lockfiles") {
                evaluateLockfiles(dcr)
                createPackageIdMap(dcr)
            }
            currentBuild.stage("Launch Builds") {
                launchBuildContainers(dcr)
            }
        }
    }

    void cloneGitRepos(DockerCommandRunner dcr) {
        dcr.run("git clone ${args.asMap['scriptsUrl']} scripts")
        dcr.run("git clone ${args.asMap['conanConfigUrl']} configs")
        dcr.run("git clone ${args.asMap['conanLocksUrl']} locks")
        dcr.run("git clone ${args.asMap['gitUrl']} -b ${args.asMap['gitBranch']} workspace")
        dcr.run("git checkout ${args.asMap['gitCommit']}", "workspace")
        dcr.run("python scripts/calculate_lock_branch_name.py --conanfile_dir=workspace")
        String lockBranch = dcr.run(dcr.dockerClient.readFileCommand('lock_branch_name.txt'), true)
        //TODO: Replace the following command with cross-platform alternative
        dcr.run("git checkout ${lockBranch} 2>/dev/null || git checkout -B ${lockBranch}", "locks")
    }

    void evaluateLockfiles(DockerCommandRunner dcr) {
        dcr.run("python scripts/copy_lockfiles_containing_package.py" +
                " --conanfile_dir=workspace locks/prod locks/dev")
        commitLockfileChanges(dcr, "initialize locks")
    }

    void createPackageIdMap(DockerCommandRunner dcr) {
        dcr.run("python scripts/create_package_id_map.py" +
                " --conanfile_dir=workspace locks/dev")
    }

    void commitLockfileChanges(DockerCommandRunner dcr, String message) {
        dcr.run("git add .", "locks")
        String gitLocksStatus = dcr.run("git status", "locks", true)
        currentBuild.echo(gitLocksStatus)
        String lockBranch = dcr.run(dcr.dockerClient.readFileCommand('lock_branch_name.txt'), true)
        if (!gitLocksStatus.contains("nothing to commit, working tree clean")) {
            dcr.run("git commit -m \"${message} for branch ${lockBranch}\"", "locks")
            dcr.run("git push -u origin ${lockBranch}", "locks")
        }
        currentBuild.retry(5) {
            dcr.run("git pull", "locks")
            dcr.run("git push -u origin ${lockBranch}", "locks")
        }
    }

    void launchBuildContainers(DockerCommandRunner dcr) {
        String packageIdMapStr = dcr.run(dcr.dockerClient.readFileCommand('package_id_map.txt'), true)
        Map<String, List<String>> packageIdMap = packageIdMapStr.split("\n").collectEntries { String line ->
            def (String packageId, String lockfileDirsStr) = line.split(":")
            [(packageId): lockfileDirsStr.split(',').toList()]
        }
        List<String> stages = packageIdMap.keySet() as List
        currentBuild.echo("Preparing build stages based on the following packageIdMap: \n" +
                new JsonBuilder(packageIdMap).toPrettyString()
        )
        String pkgName = dcr.run("conan inspect workspace --raw name", true)
        String pkgVersion = dcr.run("conan inspect workspace --raw version", true)
        Stage.parallelLimitedBranches(currentBuild, stages, 100) { String stageName ->
            String firstLockFileDir = packageIdMap[stageName].head()
            String dockerImageNameFile = "locks/dev/${pkgName}/${pkgVersion}/${firstLockFileDir}/ci_build_env_tag.txt"
            String dockerImageName = dcr.run(dcr.dockerClient.readFileCommand(dockerImageNameFile), true)
            currentBuild.stage(stageName) {
                currentBuild.echo(
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
        DockerClient dockerClient = dockerClientFactory.get(config, dockerImageName)
        dockerClient.withRun(stageName) { DockerCommandRunner dcr ->
            dockerClient.configureGit(dcr)
            cloneGitRepos(dcr)
            configureConan(dcr)
            performConanBuild(dcr, lockfileDirs)
        }
    }

    void performConanBuild(DockerCommandRunner dcr, List<String> lockfileDirs) {

        String user = args.asMap['conanUser']
        String channel = args.asMap['conanChannel']
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

        dcr.run("conan upload ${targetPkgNameVersion}@* --all -r ${args.asMap['conanRemoteUploadName']} --confirm")

        commitLockfileChanges(dcr, "update locks for package pipeline for package: " +
                "${targetPkgNameVersion} ${lockfileDirs.join(",")} "
        )
    }
}