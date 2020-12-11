package conan.ci.step.workflow

import conan.ci.jenkins.Stage
import conan.ci.runner.NativeCommandRunner
import org.jenkinsci.plugins.workflow.cps.CpsScript

// spawn docker node with any suitable docker image, will not build anything, but must run python scripts
// Clone repo
// Clone lockfile repo
// Clone scripts repo
// Create and Checkout Unique Feature Branch
// Calculate working dir root path for locks for this job (eg: locks/dev/libb)
// If it exists, (this is a successive commit in an existing feature branch)
//    do nothing
// Else (this is a new commit on a new feature branch)
//    make the directory
//    copy the lockfiles to it
//    (future/advanced/hard/important) de-duplicate the lockfiles from many aplication lockfiles
//    add/commit/push them all to git so they're cached for successive commits
// Done
// enumerate the unique lockfile directories into a list (eg.  app1/1.0/release-gcc7-app1)
// create parallel stages, one for each item in the list

class ConanPackagePipeline {
    CpsScript currentBuild

    ConanPackagePipeline(CpsScript currentBuild) {
        this.currentBuild = currentBuild
    }

    void run() {
        prepareLockfiles()
//        executeStages()
    }

    void prepareLockfiles(){
        NativeCommandRunner cmdRunner = new NativeCommandRunner(currentBuild)

        String sourceBranchName = currentBuild.env['BRANCH_NAME']
        String user = currentBuild.env['CONAN_USER']
        String channel = currentBuild.env['CONAN_CHANNEL']
        String name = cmdRunner.run("conan inspect . --raw -a name")
        String version = cmdRunner.run("conan inspect . --raw -a version")
        String packageRef = "${name}/${version}@${user}/${channel}"
        String branchDir = "${packageRef}/${sourceBranchName}"
        cmdRunner.run("python ~/ci_scripts/copy_lockfiles_containing_package.py locks/dev/${packageRef}/${branchDir}")
    }

    void executeStages(){
        String lockfileNames = currentBuild.readFile("lockfile_names.txt") as String
        List<String> stages = lockfileNames.readLines()
        Stage.parallelLimitedBranches(currentBuild, stages, 100, this.&executeStage)
    }

    void executeStage(String stageName){

    }
}
