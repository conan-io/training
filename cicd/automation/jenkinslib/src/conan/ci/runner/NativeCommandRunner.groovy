package conan.ci.runner

import conan.ci.arg.RunArgs
import org.jenkinsci.plugins.workflow.cps.CpsScript

class NativeCommandRunner implements ICommandRunner {
    CpsScript currentBuild

    NativeCommandRunner(CpsScript currentBuild) {
        this.currentBuild = currentBuild
    }

    @Override
    def run(String commandToRun, Boolean returnStdOut = true) {
        return runInCurrentDirectory(commandToRun, returnStdOut)
    }

    @Override
    def run(String commandToRun, String workingDirectory, Boolean returnStdOut = true) {
        if (workingDirectory) {
            currentBuild.dir(workingDirectory) {
                return runInCurrentDirectory(commandToRun, returnStdOut)
            }
        } else {
            return runInCurrentDirectory(commandToRun, returnStdOut)
        }
    }

    private def runInCurrentDirectory(String commandToRun, Boolean returnStdOut) {
        // This command looks like it could be refactored, but there is a strange bug in jenkins
        // If you try to move the .sh or .bat scripts to a separate function, it fails. It is completely bizarre.
        // Also, we avoid hitting "isUnix()" function when we get an agentOs from RunArgs because of unit testing cases

        String agentOs = new RunArgs(currentBuild).agentOs
        if (agentOs) {
            if (agentOs.contains("linux")) {
                return currentBuild.sh(script: commandToRun, returnStdout: returnStdOut)
            } else {
                return currentBuild.bat(script: commandToRun, returnStdout: returnStdOut)
            }
        } else {
            if (currentBuild.isUnix()) {
                return currentBuild.sh(script: commandToRun, returnStdout: returnStdOut)
            } else {
                return currentBuild.bat(script: commandToRun, returnStdout: returnStdOut)
            }
        }
    }

}
