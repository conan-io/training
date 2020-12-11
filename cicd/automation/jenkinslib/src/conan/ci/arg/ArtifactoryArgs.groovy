package conan.ci.arg

class ArtifactoryArgs {
    def currentBuild
    def env

    ArtifactoryArgs(def currentBuild) {
        this.currentBuild = currentBuild
        this.env = currentBuild.env
    }

    String getUrl() {
        return env["DEMO_ARTIFACTORY_URL"] ?: ""
    }

    String getMetadataProdRepo() {
        return env["DEMO_METADATA_PROD_REPO"] ?: ""
    }

    String getMetadataTempRepo() {
        return env["DEMO_METADATA_TEMP_REPO"] ?: ""
    }
}
