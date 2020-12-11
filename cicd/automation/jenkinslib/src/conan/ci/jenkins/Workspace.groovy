package conan.ci.jenkins

class Workspace {

    private def currentBuild
    private def env
    conan.ci.arg.JenkinsArgs jenkinsArgs

    Workspace(def currentBuild) {
        this.currentBuild = currentBuild
        this.env = currentBuild.env
        jenkinsArgs = new conan.ci.arg.JenkinsArgs(currentBuild)
    }

    String getStartingWorkspace() {
        return env['STARTING_WORKSPACE'] ?: jenkinsArgs.workspace
    }

    String getNewWorkspacesRoot() {
        String startingWorkspaceBaseName = startingWorkspace.split("@")[0]
        String startingWorkspaceDirName = new File(startingWorkspaceBaseName).name
        return "${startingWorkspaceDirName}_workspaces"
    }

    String getBuildWorkspace() {
        return "${newWorkspacesRoot}/${jenkinsArgs.buildNumber}"
    }

    String getWorkspaceSubdir() {
        return env["DEMO_WORKSPACE_SUBDIR"] ?: jenkinsArgs.stageName
    }

    Map<String, String> getWorkspaceEnv() {
        Map<String, String> workspaceEnv = [
                "STARTING_WORKSPACE": startingWorkspace,
                "PARENT_WORKSPACE"  : jenkinsArgs.workspace,
        ]
        return workspaceEnv
    }

    static void withCleanWorkspace(def currentBuild, Closure body) {
        Workspace ws = new Workspace(currentBuild)
        String newWorkspace = "${ws.buildWorkspace}/${ws.workspaceSubdir}"

        conan.ci.step.UtilitySteps.withMapEnv(currentBuild, ws.workspaceEnv) {
            currentBuild.ws(newWorkspace) {
                try {
                    body()
                } finally {
                    if (!keepWorkspaces(currentBuild)) {
                        currentBuild.deleteDir()
                    }
                }
            }
        }
    }

    static void demoCleanRootWs(def currentBuild) {
        if (!keepWorkspaces(currentBuild)) {
            currentBuild.deleteDir()
        }
    }

    static Boolean keepWorkspaces(def currentBuild) {
        String keepWorkspaces = currentBuild.env['DEMO_KEEP_WORKSPACES'] ?: ""
        return keepWorkspaces.toBoolean()
    }
}
