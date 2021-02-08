package conan.ci.condition

import conan.ci.arg.*
import conan.ci.docker.DockerClient
import conan.ci.docker.DockerClientFactory
import conan.ci.jenkins.JenkinsAgentFactory
import conan.ci.pipeline.PipelineBase
import conan.ci.runner.DockerCommandRunner
import org.jenkinsci.plugins.workflow.cps.CpsScript

class ConanBuildOrderHasSteps {

    PipelineBase base
    static ConanBuildOrderHasSteps construct(CpsScript currentBuild, Map config) {
        def it = new ConanBuildOrderHasSteps()
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

    boolean run() {
        base.currentBuild.echo("conanBuildOrderHasSteps() condition will now be evaluated.")
        DockerClient dockerClient = base.dockerClientFactory.get(base.config, base.args.asMap['dockerDefaultImage'])
        boolean result = dockerClient.withRun("Start Container") { DockerCommandRunner dcr ->
            dockerClient.configureGit(dcr)
            cloneGitRepos(dcr)
            base.configureConan(dcr)
            checkoutLockBranch(dcr)
            calculateBuildOrder(dcr)
            return buildOrderHasSteps(dcr)
        }
        return result
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
        dcr.run("git checkout ${lockBranch}", "locks")
    }

    void calculateBuildOrder(DockerCommandRunner dcr) {
        String pkgName = dcr.run("conan inspect workspace --raw name", true)
        String pkgVersion = dcr.run("conan inspect workspace --raw version", true)
        dcr.run("python ~/scripts/list_lockfile_names.py locks/dev/${pkgName}/${pkgVersion}")
        //TODO: Implement modified or new version of script which identifies the "latest" build order
        // to achieve "only re-run those downstream builds which are not completed".
        // Load original build order, and iterate through it.  For each level and node:
        // Look for file named "job_status.txt" in the lockfile root for that node (eg. locks/dev/libb)
        // Read it if exists. If it contains "completed successfully", then remove the node from the build order.
        // The levels and nodes that remain are the new build order, so put them in a new file and use that.
        // This all requires that each job write the exit status o the  file in a new "always" block in the pipeline.
        dcr.run("python ~/scripts/create_combined_build_order.py locks/dev/${pkgName}/${pkgVersion}")
    }

    boolean buildOrderHasSteps(DockerCommandRunner dcr) {
        String pkgName = dcr.run("conan inspect workspace --raw name", true)
        String pkgVersion = dcr.run("conan inspect workspace --raw version", true)
        String result = dcr.run(
                "python ~/scripts/combined_build_order_has_steps.py locks/dev/${pkgName}/${pkgVersion}", true)
        boolean buildOrderHasSteps = (result.trim() == "True")
        base.currentBuild.echo("conanBuildOrderHasSteps() condition is returning value of ${buildOrderHasSteps}")
        return buildOrderHasSteps
    }
}