package conan.ci.jenkins

import conan.ci.runner.NativeCommandRunnerFactory
import org.jenkinsci.plugins.workflow.cps.CpsScript

class JenkinsAgentWindows extends JenkinsAgent {
    static JenkinsAgentWindows construct(CpsScript currentBuild, Map config) {
        def it = new JenkinsAgentWindows()
        it.currentBuild = currentBuild
        it.config = config
        it.isUnix = false
        it.nativeCommandRunner = NativeCommandRunnerFactory.construct(currentBuild).get(it.isUnix)
        return it
    }
}
