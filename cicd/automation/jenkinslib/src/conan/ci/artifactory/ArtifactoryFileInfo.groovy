package conan.ci.artifactory


import net.sf.json.JSONObject
class ArtifactoryFileInfo {
    def currentBuild
    String uri
    String artifactoryStorageUri
    String repo
    String binaryUri

    static ArtifactoryFileInfo construct(def currentBuild, JSONObject json) {
        ArtifactoryFileInfo afi = new ArtifactoryFileInfo(currentBuild)
        afi.uri = json["uri"]
        conan.ci.arg.ArtifactoryArgs artifactoryArgs = new conan.ci.arg.ArtifactoryArgs(currentBuild)
        afi.artifactoryStorageUri = "${artifactoryArgs.url}/${conan.ci.constant.DemoConstant.ART_API_STORAGE_PATH}"
        conan.ci.arg.ConanArgs conanArgs = new conan.ci.arg.ConanArgs(currentBuild)
        afi.repo = conanArgs.conanRemoteProd
        afi.binaryUri = afi.uri
                .replace("${afi.artifactoryStorageUri}/", "")
                .replace("${afi.repo}/", "")
                .replace("/${conan.ci.constant.DemoConstant.CONAN_INFO_FILENAME}", "")

        return afi
    }

    private ArtifactoryFileInfo(def currentBuild){
        this.currentBuild = currentBuild
    }
}