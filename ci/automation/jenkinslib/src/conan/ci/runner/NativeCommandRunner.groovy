package conan.ci.runner

import org.jenkinsci.plugins.workflow.cps.CpsScript

abstract class NativeCommandRunner implements ICommandRunner {
    CpsScript currentBuild

    @Override
    def run(String commandToRun, Boolean returnStdout = true) {
        return runInCurrentDirectory(commandToRun, returnStdout)
    }

    @Override
    def run(String commandToRun, String workDir, Boolean returnStdout = true) {
        if (workDir) {
            currentBuild.dir(workDir) {
                return runInCurrentDirectory(commandToRun, returnStdout)
            }
        } else {
            return runInCurrentDirectory(commandToRun, returnStdout)
        }
    }

    abstract def runInCurrentDirectory(String commandToRun, Boolean returnStdout)

}
