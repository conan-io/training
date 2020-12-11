package conan.ci.arg

class MetadataArgs {
    def currentBuild
    def env

    MetadataArgs(def currentBuild) {
        this.currentBuild = currentBuild
        this.env = currentBuild.env
    }

    String getProdRepo() {
        return env["DEMO_METADATA_PROD_REPO"] ?: ""
    }

    String getTempRepo() {
        return env["DEMO_METADATA_TEMP_REPO"] ?: ""
    }
}
