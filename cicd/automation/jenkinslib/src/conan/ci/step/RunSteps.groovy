package conan.ci.step


import conan.ci.runner.CommandRunnerSelector
import conan.ci.util.DemoLogger

class RunSteps {
    static void demoRun(def currentBuild, Map<String, String> runConfig) {
        DemoLogger.debug(currentBuild, "Running demoRun")
        def commandRunner = CommandRunnerSelector.select(currentBuild)
        conan.ci.arg.RunArgs runArgs = new conan.ci.arg.RunArgs(currentBuild, runConfig)

        commandRunner.run(runArgs.runCommand, false)
    }
}
