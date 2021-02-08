package conan.ci.runner

interface ICommandRunner {
    def run(String commandToRun)
    def run(String commandToRun, String workDir)
    def run(String commandToRun, Boolean returnStdout)
    def run(String commandToRun, String workDir, Boolean returnStdout)
}