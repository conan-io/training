package conan.ci.credentials

class GitCredentials {
    def env

    GitCredentials(def env) {
        this.env = env
    }

    // Specifically not defining to string to avoid leaking in dumps
    String getGitCredentials() {
        return "${gitUsername}:${gitPassword}"
    }

    String getGitUsername() {
        String user = env["DEMO_GIT_CREDS_USR"] ?: ""
        if (user == "") {
            throw new Exception("DEMO_GIT_CREDS_USR must be set")
        } else {
            return user
        }
    }

    String getGitPassword() {
        String password = env["DEMO_GIT_CREDS_PSW"] ?: ""
        if (password == "") {
            throw new Exception("DEMO_GIT_CREDS_PSW must be set")
        } else {
            return password
        }
    }
}
