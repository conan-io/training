package conan.ci.step.workflow


import conan.ci.artifactory.ArtifactoryConanApi
import conan.ci.artifactory.ArtifactoryConanPackage
import conan.ci.conan.Lockfile
import conan.ci.conan.Package
import conan.ci.conan.PackageReferenceFull
import conan.ci.conan.PackageReferenceShort
import conan.ci.constant.DemoConstant
import conan.ci.runner.CommandRunnerSelector
import conan.ci.runner.DockerCommandRunner
import conan.ci.util.DemoLogger
import org.jenkinsci.plugins.pipeline.utility.steps.fs.FileWrapper

class CreateAndUpload {

    static void run(def currentBuild) {
        DemoLogger.debug(currentBuild, "running CreateAndUpload.run")
        if(CommandRunnerSelector.select(currentBuild) instanceof DockerCommandRunner){
            conan.ci.step.DockerSteps.dockerCpWorkspaceToContainer(currentBuild)
        }
        conan.ci.step.ConanCommandSteps.conanConfigInstall(currentBuild)
        conan.ci.step.ConanCommandSteps.conanRemotesAdd(currentBuild)
        conan.ci.step.ConanCommandSteps.conanUser(currentBuild)
        conan.ci.step.ConanCommandSteps.conanGraphLock(currentBuild)
        boolean isNewPackageId = isNewPackageId(currentBuild)
        if (isNewPackageId) {
            skipBuildAndTriggerUpstreams(currentBuild)
        } else {
            executeBuildAndUpload(currentBuild)
        }
    }

    static void skipBuildAndTriggerUpstreams(Object currentBuild) {
        DemoLogger.debug(currentBuild, "New package id detected. Skipping build and triggering upstreams.")
    }

    static void executeBuildAndUpload(Object currentBuild) {
        conan.ci.step.ConanCommandSteps.conanCreate(currentBuild)
        conan.ci.step.ConanCommandSteps.conanUpload(currentBuild)
        PackageReferenceShort pkgRef = Package.construct(currentBuild).packageReferenceShort
        // graph lock again, but this time the remote pkg after upload so we can get the full reference
        conan.ci.step.ConanCommandSteps.conanGraphLock(currentBuild, pkgRef.toString())
        conan.ci.step.DockerSteps.dockerCpFromContainer(currentBuild, ["src": DemoConstant.CONAN_LOCKFILE_FILENAME])
        FileWrapper[] lockfilesInfo = currentBuild.findFiles(glob: "**/*${DemoConstant.CONAN_LOCKFILE_FILENAME}")
        Lockfile lockfile = Lockfile.construct(currentBuild, lockfilesInfo.first())
        PackageReferenceFull packageReference = PackageReferenceFull.construct(lockfile.rootPackageReference)
        conan.ci.step.UtilitySteps.writeEnvVarsToFile(currentBuild, ["DEMO_DOCKER_IMAGE": DemoConstant.DOCKER_IMAGE_FILENAME])
        File dockerImageFile = new File(DemoConstant.DOCKER_IMAGE_FILENAME)
        ArtifactoryConanApi artifactoryConanApi = ArtifactoryConanApi.construct(currentBuild)
        conan.ci.arg.MetadataArgs metadataArgs = new conan.ci.arg.MetadataArgs(currentBuild)
        artifactoryConanApi.uploadFile(metadataArgs.tempRepo, packageReference.artifactoryPath, lockfile.file)
        artifactoryConanApi.uploadFile(metadataArgs.tempRepo, packageReference.artifactoryPath, dockerImageFile)
    }


    static boolean isNewPackageId(Object currentBuild) {
        conan.ci.arg.MetadataArgs metadataArgs = new conan.ci.arg.MetadataArgs(currentBuild)
        Package conanPackage = Package.construct(currentBuild)
        ArtifactoryConanApi artifactoryApi = ArtifactoryConanApi.construct(currentBuild)
        ArtifactoryConanPackage acp = artifactoryApi.findConanPackage(conanPackage.packageReferenceShort.toString())
        if (acp) {
            artifactoryApi.downloadConanMetadataFile(metadataArgs.prodRepo, acp, DemoConstant.CONAN_LOCKFILE_FILENAME)
            FileWrapper[] lockfilesInfo = currentBuild.findFiles(glob: "**/*${DemoConstant.CONAN_LOCKFILE_FILENAME}")
            if (lockfilesInfo) {
                return false
            } else {
                return true
            }
        } else {
            return true
        }
    }
}
