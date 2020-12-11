package conan.ci.step


import conan.ci.conan.RecipeInspector
import conan.ci.runner.CommandRunnerSelector
import conan.ci.util.DemoLogger

class ConanCommandSteps {

    static void conanConfigInstall(def currentBuild) {
        DemoLogger.debug(currentBuild, "Running conanConfigInstall")
        conan.ci.arg.ConanArgs conanArgs = new conan.ci.arg.ConanArgs(currentBuild)
        String command = conanArgs.getConfigInstallCommand()
        DemoLogger.debug(currentBuild, "command: ${command}")
        def commandRunner = CommandRunnerSelector.select(currentBuild)

        commandRunner.run(command, false)
    }

    static void conanRemotesAdd(def currentBuild) {
        DemoLogger.debug(currentBuild, "running conanRemotesAdd")
        conan.ci.arg.ConanArgs conanArgs = new conan.ci.arg.ConanArgs(currentBuild)
        List<String> commands = conanArgs.getRemoteAddCommands()
        def commandRunner = CommandRunnerSelector.select(currentBuild)

        commands.each { String command ->
            DemoLogger.debug(currentBuild, "command: ${command}")
            commandRunner.run(command, false)
        }
    }

    static void conanUser(def currentBuild) {
        DemoLogger.debug(currentBuild, "running conanUser")
        conan.ci.arg.ConanArgs conanArgs = new conan.ci.arg.ConanArgs(currentBuild)
        List<String> commands = conanArgs.getUserCommands()
        def commandRunner = CommandRunnerSelector.select(currentBuild)

        commands.each { String command ->
            DemoLogger.debug(currentBuild, "command: ${command}")
            commandRunner.run(command, false)
        }
    }

    static void conanGraphLock(def currentBuild, String packageRef = "") {
        DemoLogger.debug(currentBuild, "Running conanGraphLock")
        conan.ci.arg.ConanArgs conanArgs = new conan.ci.arg.ConanArgs(currentBuild)
        String command = conanArgs.getGraphLockCommand(packageRef ?: ".")
        DemoLogger.debug(currentBuild, "command: ${command}")
        def commandRunner = CommandRunnerSelector.select(currentBuild)

        commandRunner.run(command, false)
    }

    static void conanCreate(def currentBuild) {
        DemoLogger.debug(currentBuild, "Running conanCreate")
        conan.ci.arg.ConanArgs conanArgs = new conan.ci.arg.ConanArgs(currentBuild)
        String command = conanArgs.getCreateCommand(".")
        DemoLogger.debug(currentBuild, "command: ${command}")
        def commandRunner = CommandRunnerSelector.select(currentBuild)

        commandRunner.run(command, false)
    }

    static void conanUpload(def currentBuild) {
        DemoLogger.debug(currentBuild, "running conanUpload")
        conan.ci.arg.ConanArgs conanArgs = new conan.ci.arg.ConanArgs(currentBuild)
        String recipeName = RecipeInspector.construct(currentBuild).detectRecipeName().denormalize()
        String uploadPattern = conanArgs.uploadPattern ?: recipeName
        String command = conanArgs.getUploadCommand("${uploadPattern}")
        DemoLogger.debug(currentBuild, "command: ${command}")
        def commandRunner = CommandRunnerSelector.select(currentBuild)

        commandRunner.run(command, false)
    }

    static void conanInfo(def currentBuild) {
        DemoLogger.debug(currentBuild, "running conanInfo")
        conan.ci.arg.ConanArgs conanArgs = new conan.ci.arg.ConanArgs(currentBuild)
        String recipeName = RecipeInspector.construct(currentBuild).detectRecipeName().denormalize()
        String command = conanArgs.getInfoCommand("${recipeName}")
        DemoLogger.debug(currentBuild, "command: ${command}")
        def commandRunner = CommandRunnerSelector.select(currentBuild)

        commandRunner.run(command, false)
    }

    static void conanGraphBuildOrder(def currentBuild) {
        DemoLogger.debug(currentBuild, "running conanGraphBuildOrder")
        conan.ci.arg.ConanArgs conanArgs = new conan.ci.arg.ConanArgs(currentBuild)
        String command = conanArgs.getGraphBuildOrderCommand()
        DemoLogger.debug(currentBuild, "command: ${command}")
        def commandRunner = CommandRunnerSelector.select(currentBuild)

        commandRunner.run(command, false)
    }

}
