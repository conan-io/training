package conan.ci.credentials

import org.jenkinsci.plugins.workflow.cps.EnvActionImpl

class ArtifactoryCredentials {
    private EnvActionImpl env

    ArtifactoryCredentials(EnvActionImpl env) {
        this.env = env
    }

    String getArtifactoryUsername() {
        String user = env["DEMO_ART_CREDS_USER"] ?: ""
        if (user == "") {
            throw new Exception("DEMO_ART_CREDS_USER must be set")
        } else {
            return user
        }
    }

    String getArtifactoryPassword() {
        String password = env["DEMO_ART_CREDS_PSW"] ?: ""
        if (password == "") {
            throw new Exception("DEMO_ART_CREDS_PSW must be set")
        } else {
            return password
        }
    }
}
