package conan.ci.credentials

class Credential {
    String username
    String password

    Credential(def env, String credentialName) {
        this.username = env["${credentialName}_USR"]
        this.password = env["${credentialName}_PSW"]
    }
}
