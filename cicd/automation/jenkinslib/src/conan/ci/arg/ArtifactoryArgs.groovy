package conan.ci.arg

class ArtifactoryArgs {

    static List<Argument> get() {
        return [
                new Argument<String>(
                        name: "artifactoryUser",
                        group: "artifactory",
                        envVarName: "TRAINING_ART_CREDS_USR",
                        description: "Username for artifactory.",
                ),
                new Argument<String>(
                        name: "artifactoryPassword",
                        group: "artifactory",
                        envVarName: "TRAINING_ART_CREDS_PSW",
                        description: "Password for artifactory.",
                ),
        ]
    }
}
