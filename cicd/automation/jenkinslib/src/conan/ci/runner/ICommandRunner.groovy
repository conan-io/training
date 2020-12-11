package conan.ci.runner

interface ICommandRunner {
    def run(String commandToRun)
    def run(String commandToRun, String workingDirectory)
    def run(String commandToRun, Boolean returnStdOut)
    def run(String commandToRun, String workingDirectory, Boolean returnStdOut)
}