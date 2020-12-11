package conan.ci.util

class DemoLogger {

    static void info(def currentBuild, String message) {
        String level = currentBuild.env["DEMO_LOG_LEVEL"] ?: ""
        if (level == "INFO"){
            currentBuild.echo("DEMO_INFO: ${message}")
        }
    }

    static void debug(def currentBuild, String message) {
        String level = currentBuild.env["DEMO_LOG_LEVEL"] ?: ""
        if (level == "DEBUG"){
            currentBuild.echo("DEMO_DEBUG: ${message}")
        }
    }
}
