package conan.ci.artifactory

class ArtifactoryConanPackage {
    def currentBuild
    conan.ci.conan.PackageReferenceFull packageReference
    String binaryUri

    static ArtifactoryConanPackage construct(def currentBuild, String binaryUri) {
        ArtifactoryConanPackage acp = new ArtifactoryConanPackage(currentBuild)
        acp.binaryUri = binaryUri
        acp.packageReference = conan.ci.conan.PackageReferenceFull.construct(binaryUri.toURI())
        return acp
    }

    private ArtifactoryConanPackage(def currentBuild) {
        this.currentBuild = currentBuild
    }

}