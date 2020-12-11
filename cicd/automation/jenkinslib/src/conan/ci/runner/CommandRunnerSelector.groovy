package conan.ci.runner

class CommandRunnerSelector {

    static def select(def currentBuild){
        String runner = currentBuild.env["DEMO_COMMAND_RUNNER"] ?: ""
        def commandRunner
        switch(runner) {
            case "docker":
                commandRunner = new DockerCommandRunner(currentBuild)
                break
            case "echo":
                commandRunner = new EchoCommandRunner(currentBuild)
                break
            default:
                commandRunner = new NativeCommandRunner(currentBuild)
        }
        return commandRunner
    }
}
