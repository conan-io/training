package conan.ci.jenkins

import conan.ci.runner.NativeCommandRunnerFactory
import org.jenkinsci.plugins.workflow.cps.CpsScript

class JenkinsAgentLinux extends JenkinsAgent {

    static JenkinsAgentLinux construct(CpsScript currentBuild, Map config) {
        def it = new JenkinsAgentLinux()
        it.currentBuild = currentBuild
        it.config = config
        it.isUnix = true
        it.nativeCommandRunner = NativeCommandRunnerFactory.construct(currentBuild).get(it.isUnix)
        return it
    }
}
