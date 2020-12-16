package conan.ci.runner

import org.jenkinsci.plugins.workflow.cps.CpsScript


class EchoCommandRunner implements ICommandRunner {
    CpsScript currentBuild

    static EchoCommandRunner construct(CpsScript currentBuild) {
        def it = new EchoCommandRunner()
        it.currentBuild = currentBuild
        return it
    }

    @Override
    def run(String commandToRun, Boolean returnStdout = false) {
        runInCurrentDirectory(commandToRun, returnStdout)
    }

    @Override
    def run(String commandToRun, String workingDirectory, Boolean returnStdout = false) {
        runInCurrentDirectory(commandToRun, returnStdout)
    }

    private def runInCurrentDirectory(String commandToRun, Boolean returnStdout) {
        String message = "Command: ${commandToRun}, WorkingDirectory: ."
        currentBuild.echo(message)
        if(returnStdout){
            return message
        }
    }

}
