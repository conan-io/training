package conan.ci.runner

import org.jenkinsci.plugins.workflow.cps.CpsScript

class NativeCommandRunnerWindows extends NativeCommandRunner {
    CpsScript currentBuild

    static NativeCommandRunnerWindows construct(CpsScript currentBuild) {
        def it = new NativeCommandRunnerWindows()
        it.currentBuild = currentBuild
        return it
    }

    @Override
    def runInCurrentDirectory(String commandToRun, Boolean returnStdout) {
        return currentBuild.bat(script: commandToRun, returnStdout: returnStdout)
    }
}
