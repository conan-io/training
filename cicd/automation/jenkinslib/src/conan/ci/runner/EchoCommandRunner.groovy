package conan.ci.runner


class EchoCommandRunner implements ICommandRunner {
    def currentBuild

    EchoCommandRunner(def currentBuild) {
        this.currentBuild = currentBuild
    }

    @Override
    def run(String commandToRun, Boolean returnStdOut = false) {
        runInCurrentDirectory(commandToRun, returnStdOut)
    }

    @Override
    def run(String commandToRun, String workingDirectory, Boolean returnStdOut = false) {
        runInCurrentDirectory(commandToRun, returnStdOut)
    }

    private def runInCurrentDirectory(String commandToRun, Boolean returnStdOut) {
        String message = "Command: ${commandToRun}, WorkingDirectory: ."
        currentBuild.echo(message)
        if(returnStdOut){
            return message
        }
    }

}
