package conan.ci.arg

class DemoArgs {
    def currentBuild
    def env

    DemoArgs(def currentBuild) {
        this.currentBuild = currentBuild
        this.env = currentBuild.env
    }

    String getDownstreamBranchName() {
        return env["DEMO_JENKINS_DOWNSTREAM_BRANCH_NAME"] ?: ""
    }

    String getParallelAgentLabel() {
        return env["DEMO_PARALLEL_AGENT_LABEL"] ?: ""
    }

    int getMaxParallelBranches() {
        return (env["DEMO_MAX_PARALLEL_BRANCHES"] ?: "2").toString().toInteger()
    }
}
