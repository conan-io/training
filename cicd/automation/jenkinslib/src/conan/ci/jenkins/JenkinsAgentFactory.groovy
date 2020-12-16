package conan.ci.jenkins

import org.jenkinsci.plugins.workflow.cps.CpsScript

class JenkinsAgentFactory {
    CpsScript currentBuild

    static JenkinsAgentFactory construct(CpsScript currentBuild) {
        def it = new JenkinsAgentFactory()
        it.currentBuild = currentBuild
        return it
    }

    JenkinsAgent get(Map config) {
        //TODO: implement logic to choose a new agent based on label
        // Then spawn that new agent with node or something
        // Then have all the methods function on that node
        // For now, just use the current node
        JenkinsAgent client = currentBuild.isUnix()
                ? JenkinsAgentLinux.construct(currentBuild, config)
                : JenkinsAgentWindows.construct(currentBuild, config)
        return client
    }
}
