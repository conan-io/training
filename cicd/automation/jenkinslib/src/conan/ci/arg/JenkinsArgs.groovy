package conan.ci.arg

class JenkinsArgs {
    def currentBuild
    def env

    JenkinsArgs(def currentBuild) {
        this.currentBuild = currentBuild
        this.env = currentBuild.env
    }

    String getStageName() {
        return env["STAGE_NAME"] ?: ""
    }

    String getWorkspace(){
        return env['WORKSPACE']
    }

    File getWorkspaceFile(){
        return new File(workspace)
    }

    String getParentWorkspace(){
        return env['PARENT_WORKSPACE']
    }

    File getParentWorkspaceFile(){
        return new File(parentWorkspace)
    }

    String getStartingWorkspace(){
        return env['STARTING_WORKSPACE']
    }

    File getStartingWorkspaceFile(){
        return new File(startingWorkspace)
    }

    String getBuildNumber(){
        return env['BUILD_NUMBER']
    }

}
