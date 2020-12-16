package conan.ci.util

import org.jenkinsci.plugins.workflow.cps.CpsScript

class DemoLogger {

    static void info(CpsScript currentBuild, String message) {
        String level = currentBuild.env["DEMO_LOG_LEVEL"] ?: ""
        if (level == "INFO"){
            currentBuild.echo("DEMO_INFO: ${message}")
        }
    }

    static void debug(CpsScript currentBuild, String message) {
        String level = currentBuild.env["DEMO_LOG_LEVEL"] ?: ""
        if (level == "DEBUG"){
            currentBuild.echo("DEMO_DEBUG: ${message}")
        }
    }
}
