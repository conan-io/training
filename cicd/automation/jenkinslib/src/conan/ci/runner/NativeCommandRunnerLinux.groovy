package conan.ci.runner

import org.jenkinsci.plugins.workflow.cps.CpsScript

class NativeCommandRunnerLinux extends NativeCommandRunner {
    CpsScript currentBuild

    static NativeCommandRunnerLinux construct(CpsScript currentBuild) {
        def it = new NativeCommandRunnerLinux()
        it.currentBuild = currentBuild
        return it
    }

    @Override
    def run(String commandToRun, String workingDirectory, Boolean returnStdout = true) {
        if (workingDirectory) {
            currentBuild.dir(workingDirectory) {
                return runInCurrentDirectory(commandToRun, returnStdout)
            }
        } else {
            return runInCurrentDirectory(commandToRun, returnStdout)
        }
    }

    @Override
    def runInCurrentDirectory(String commandToRun, Boolean returnStdout) {
        return currentBuild.sh(script: commandToRun, returnStdout: returnStdout)
    }

}
