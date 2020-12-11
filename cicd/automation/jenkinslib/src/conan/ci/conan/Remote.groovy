package conan.ci.conan

import conan.ci.credentials.Credential

class Remote {
    def env
    String name
    String index
    String cred
    String serverUrl
    String serverVar
    String url
    Boolean verifySsl

    Remote(def currentBuild, String remoteEnvVarName, String index) {
        this.env = currentBuild.env
        String remoteDefinitionString = getRemoteDefinitionFromEnv(remoteEnvVarName)
        remoteDefinitionString
                .split(";")
                .each { String entry ->
                    def pair = entry.split('=')
                    this.(pair.first()) = pair.last()
                }
        this.serverUrl = getServerDefinitionFromEnv(this.serverVar)
        this.index = index
        this.url = "${this.serverUrl}/conan/${name}"
    }

    List<String> getRemoteAddCommandArgs() {
        List<String> args = [name, url]
        if (verifySsl) {
            args.add("--verifySsl")
        }
        if (index) {
            args.add("--insert=${index}")
        }
        return args
    }

    List<String> getUserCommandArgs() {
        List<String> args = []
        String credentialStr = env[cred] ?: ""
        if (credentialStr == "") {
            throw new Exception("There is no credential matching the name: ${cred}")
        } else {
            Credential credential = new Credential(env, cred)
            args.add("-p ${credential.password}")
            args.add("-r ${name}")
            args.add("${credential.username}")
            return args
        }
    }

    String getRemoteDefinitionFromEnv(String remoteEnvVarName) {
        return env[remoteEnvVarName]
    }

    String getServerDefinitionFromEnv(String serverEnvVarName) {
        return env[serverEnvVarName]
    }
}