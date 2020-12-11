package conan.ci.arg

import conan.ci.credentials.GitCredentials

class GitArgs {
    def currentBuild
    def env

    GitArgs(def currentBuild) {
        this.currentBuild = currentBuild
        this.env = currentBuild.env
    }

    String getCloneCommand(String destinationDir = "") {
        List<String> cliArgs = ["git clone"]
        if (urlCmd()) {
            cliArgs.add(urlCmd())
        }
        cliArgs.add(destinationDirCmd(destinationDir))
        cliArgs.addAll(argsCmd())
        return cliArgs.join(" ")
    }

    String getGitUrl() {
        return env["CHANGE_URL"] ?: env["GIT_URL"] ?: ""
    }

    String urlCmd() {
        if (gitUrl == "") {
            throw new Exception("DEMO_GIT_URL must be set")
        } else {
            String gitCredentialVars = gitCredentialVars()
            if (gitCredentialVars) {
                return gitUrl.replace("://", "://${gitCredentialVars}@")
            } else {
                return gitUrl
            }
        }
    }

    String getDestinationDir() {
        return env["DEMO_GIT_DESTINATION_DIR"] ?: ""
    }

    String destinationDirCmd(String destDir = "") {
        if (destinationDir == "") {
            return destDir
        } else {
            return destinationDir
        }
    }

    String gitCredentialVars() {
        GitCredentials gitCredentials = new GitCredentials(env)
        return gitCredentials.gitCredentials
    }

    String getArgs() {
        return env["DEMO_GIT_ARGS"] ?: ""
    }

    List<String> argsCmd() {
        if (args == "") {
            return []
        } else {
            return args.split(',')?.collect { "${it}".toString() }
        }
    }
}
