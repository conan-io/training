package conan.ci.arg

class RunArgs {
    Object currentBuild
    def env
    private String _command

    RunArgs(Object currentBuild, Map config = [:]) {
        this.currentBuild = currentBuild
        this.env = currentBuild.env
        if (config) {
            _command = config.command
        }
    }

    String getRunCommand() {
        return _command ?: env["DEMO_RUN_COMMAND"] ?: ""
    }

    String getAgentOs() {
        return _command ?: env["DEMO_RUN_AGENT_OS"] ?: ""
    }

}
