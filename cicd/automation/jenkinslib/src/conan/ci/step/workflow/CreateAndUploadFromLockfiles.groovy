package conan.ci.step.workflow


import conan.ci.artifactory.ArtifactoryConanApi
import conan.ci.artifactory.ArtifactoryConanPackage
import conan.ci.conan.BuildOrder
import conan.ci.conan.BuildOrderFile
import conan.ci.conan.BuildOrderList
import conan.ci.conan.CombinedBuildOrderFile
import conan.ci.conan.Lockfile
import conan.ci.conan.Package
import conan.ci.conan.PackageReferenceShort
import conan.ci.conan.Stage
import conan.ci.constant.DemoConstant
import conan.ci.jenkins.CauseWrapper
import conan.ci.jenkins.Search
import conan.ci.jenkins.Workspace
import conan.ci.util.DemoLogger
import org.jenkinsci.plugins.pipeline.utility.steps.fs.FileWrapper

class CreateAndUploadFromLockfiles {

    static void run(def currentBuild) {
        DemoLogger.debug(currentBuild, "running CreateAndUploadFromLockfiles.run")
        conan.ci.step.DockerSteps.dockerCpWorkspaceToContainer(currentBuild)
        ArtifactoryConanApi artifactoryApi = ArtifactoryConanApi.construct(currentBuild)
        PackageReferenceShort pkgRef = Package.construct(currentBuild).packageReferenceShort
        List<ArtifactoryConanPackage> consumers = artifactoryApi.findConanConsumers(pkgRef.toString())
        downloadLockfiles(currentBuild, consumers)
        downloadConsumerDockerImageNames(currentBuild, consumers)
        List<Lockfile> lockfiles = readLockfilesFromDisk(currentBuild)
        List<String> uniqueFullPackageRefs = getUniqueFullPackageRefs(currentBuild, lockfiles, pkgRef.toString())
        List<Lockfile> lockfilesToUseForBuild = selectLockfilesForBuild(currentBuild, lockfiles, uniqueFullPackageRefs)
        List<Stage> conanStages = makeStagesFromLockfiles(currentBuild, lockfilesToUseForBuild, pkgRef.toString())
        List<String> packageIds = conanStages.collect { it.packageId }
        List<BuildOrderFile> buildOrderFiles = conanCreateInDockerParallel(currentBuild, packageIds, conanStages, lockfiles)
        List<BuildOrder> buildOrders = buildOrderFiles.collect { BuildOrder.fromSingleBuildOrder(it.json) }
        BuildOrderList buildOrderList = new BuildOrderList(buildOrders)
        writeNewCombinedBuildOrderToDisk(currentBuild, buildOrderList)
        conan.ci.step.CopyArtifactSteps.saveCombinedBuildOrderForDownstreams(currentBuild)
        invokeDownstreamJobs(currentBuild, buildOrderList, pkgRef.name)
    }

    static void downloadLockfiles(def currentBuild, List<ArtifactoryConanPackage> consumers) {
        String lastCause = new CauseWrapper(currentBuild).lastCauseName
        DemoLogger.debug(currentBuild, "lastCause ${lastCause}")
        if (lastCause.contains("UpstreamCause")) {
            downloadUpstreamLockfiles(currentBuild)
        } else {
            downloadConsumerLockfiles(currentBuild, consumers)
        }
    }

    static void downloadUpstreamLockfiles(def currentBuild) {
        DemoLogger.debug(currentBuild, "running downloadUpstreamLockfiles")
        conan.ci.step.CopyArtifactSteps.getLockfilesFromUpstream(currentBuild)
        FileWrapper[] lockfilesInfo = currentBuild.findFiles(glob: "**/*${DemoConstant.CONAN_LOCKFILE_FILENAME}")
        DemoLogger.debug(currentBuild, "Copied Lockfiles from upstreams:" +
                "\n${lockfilesInfo.collect { it.path }.join('\n')}")
    }

    static void downloadConsumerLockfiles(def currentBuild, List<ArtifactoryConanPackage> consumers) {
        DemoLogger.debug(currentBuild, "running downloadConsumerLockfiles")
        String currentPackageRef = Package.construct(currentBuild).packageReferenceShort.toString()
        DemoLogger.debug(currentBuild, "currentPackageRef = ${currentPackageRef}")
        ArtifactoryConanApi artifactoryApi = ArtifactoryConanApi.construct(currentBuild)
        artifactoryApi.downloadConanMetadataFiles(consumers, DemoConstant.CONAN_LOCKFILE_FILENAME)
        FileWrapper[] lockfilesInfo = currentBuild.findFiles(glob: "**/*${DemoConstant.CONAN_LOCKFILE_FILENAME}")
        DemoLogger.debug(currentBuild, "\"Copied Lockfiles from consumers:" +
                "\n${lockfilesInfo.collect { it.path }.join('\n')}")
    }

    static void downloadConsumerDockerImageNames(def currentBuild, List<ArtifactoryConanPackage> consumers) {
        DemoLogger.debug(currentBuild, "running getConsumerDockerImageNames")
        String currentPackageRef = Package.construct(currentBuild).packageReferenceShort.toString()
        DemoLogger.debug(currentBuild, "currentPackageRef = ${currentPackageRef}")
        ArtifactoryConanApi artifactoryApi = ArtifactoryConanApi.construct(currentBuild)
        artifactoryApi.downloadConanMetadataFile(consumers, DemoConstant.DOCKER_IMAGE_FILENAME)
        FileWrapper[] dockerImageFileInfos = currentBuild.findFiles(glob: "**/*${DemoConstant.DOCKER_IMAGE_FILENAME}")
        DemoLogger.debug(currentBuild, "Copied DockerImageNames from consumers" +
                "\n${dockerImageFileInfos.collect { it.path }.join('\n')}")
    }

    static List<Stage> makeStagesFromLockfiles(
            def currentBuild,
            List<Lockfile> lockfilesToUseForBuild,
            String currentPackageRef
    ) {
        DemoLogger.debug(currentBuild, "running makeStagesFromLockfiles")
        DemoLogger.debug(currentBuild, "currentPackageRef ${currentPackageRef}")
        List<Stage> conanStages = lockfilesToUseForBuild.collect { Lockfile lockfile ->
            String fullPackageRef = lockfile.getNodeForPackage(currentPackageRef).pRef
            String dockerImageName = currentBuild.readFile(
                    new File(lockfile.file.parentFile, DemoConstant.DOCKER_IMAGE_FILENAME).path
            )
            return new Stage(currentBuild, fullPackageRef, lockfile, dockerImageName)
        }
        return conanStages
    }

    static List<Lockfile> readLockfilesFromDisk(
            def currentBuild
    ) {
        FileWrapper[] lockfileInfos = currentBuild.findFiles(glob: "**/*${DemoConstant.CONAN_LOCKFILE_FILENAME}")
        List<Lockfile> lockfiles = lockfileInfos.collect { FileWrapper lockfileInfo ->
            return Lockfile.construct(currentBuild, lockfileInfo)
        }
        return lockfiles
    }

    static List<String> getUniqueFullPackageRefs(
            def currentBuild,
            List<Lockfile> lockfiles,
            String currentPackageRef
    ) {
        List<String> uniqueFullPackageRefs = lockfiles
                .findAll { Lockfile lockfile -> lockfile.containsPackageReference(currentPackageRef) }
                .collect { Lockfile lockfile -> lockfile.getNodeForPackage(currentPackageRef).pRef }
                .unique()

        DemoLogger.debug(currentBuild, "uniqueFullPackageRefs :\n${uniqueFullPackageRefs.join('\n')}")
        return uniqueFullPackageRefs
    }

    static List<Lockfile> selectLockfilesForBuild(
            def currentBuild,
            List<Lockfile> lockfiles,
            List<String> uniqueFullPackageRefs
    ) {
        //Find the first lockfile that uses this uniquePackageRef

        DemoLogger.debug(currentBuild, "running selectLockfilesForBuild")
        List<Lockfile> lockfilesToUseForBuild = uniqueFullPackageRefs
                .collect { String fullPackageRef ->
                    lockfiles.find { Lockfile lockfile ->
                        lockfile.containsPackageReference(fullPackageRef)
                    }
                }

        DemoLogger.debug(currentBuild, "lockfilesToUseForBuild \n" +
                "${lockfilesToUseForBuild.collect { it.rootPackageReference }.join("\n")}")

        return lockfilesToUseForBuild
    }

    static List<BuildOrderFile> conanCreateInDockerParallel(
            def currentBuild,
            List<String> packageIds,
            List<Stage> conanStages,
            List<Lockfile> lockfiles
    ) {
        DemoLogger.debug(currentBuild, "running conanCreateInDockerParallel")
        conan.ci.arg.DemoArgs demoArgs = new conan.ci.arg.DemoArgs(currentBuild.env)
        String parallelAgentLabel = demoArgs.parallelAgentLabel ?: "any"
        int maxParallelBranches = demoArgs.maxParallelBranches
        List<BuildOrderFile> buildOrderFiles = []
        Stage.parallelLimitedBranches(currentBuild, packageIds, maxParallelBranches, false) { String packageId ->
            Stage stage = conanStages.find { it.fullPackageRef.contains(packageId) }
            currentBuild.stage(packageId) {
                currentBuild.node(parallelAgentLabel) {
                    conan.ci.step.UtilitySteps.withMapEnv(currentBuild, ["DEMO_WORKSPACE_SUBDIR": packageId]) {
                        Workspace.withCleanWorkspace(currentBuild) {
                            List<BuildOrderFile> stageBofs = runOperationsInContainer(
                                    currentBuild, stage, packageId, lockfiles
                            )
                            buildOrderFiles.addAll(stageBofs)
                        }
                    }
                }
            }
        }
        return buildOrderFiles
    }

    static List<BuildOrderFile> runOperationsInContainer(
            def currentBuild,
            Stage stage,
            String packageId,
            List<Lockfile> lockfiles
    ) {
        DemoLogger.debug(currentBuild, "running runOperationsInContainer")
        List<BuildOrderFile> buildOrderFiles = []
        conan.ci.arg.JenkinsArgs jenkinsArgs = new conan.ci.arg.JenkinsArgs(currentBuild)
        Map<String, String> stageEnv = [
                "DEMO_COMMAND_RUNNER"       : "docker",
                "DEMO_DOCKER_CONTAINER_NAME": "${jenkinsArgs.buildNumber}_${packageId}",
                "DEMO_DOCKER_IMAGE"         : stage.dockerImage
        ]
        conan.ci.step.UtilitySteps.withMapEnv(currentBuild, stageEnv) {
            conan.ci.step.DockerSteps.withDemoDockerContainer(currentBuild) {
                conan.ci.step.DockerSteps.dockerCpWorkspaceToContainer(currentBuild)
                conan.ci.step.ConanCommandSteps.conanConfigInstall(currentBuild)
                conan.ci.step.ConanCommandSteps.conanRemotesAdd(currentBuild)
                conan.ci.step.ConanCommandSteps.conanUser(currentBuild)
                conan.ci.step.UtilitySteps.withMapEnv(currentBuild, ["DEMO_CONAN_LOCKFILE": stage.lockfile.parent]) {
                    conan.ci.step.ConanCommandSteps.conanCreate(currentBuild)
                }
                conan.ci.step.ConanCommandSteps.conanUpload(currentBuild)
                // Run conan create again on the rest of the lockfiles which use the same package id
                // Documented here: https://github.com/conan-io/conan/issues/7180
                lockfiles.each { Lockfile lockfile ->
                    if (lockfile.containsPackageReference(stage.fullPackageRef)) {
                        Map<String, String> conanCreateEnv = [
                                "DEMO_CONAN_LOCKFILE"    : lockfile.parent,
                                "DEMO_CONAN_CREATE_FLAGS": "--keep-build,--test-folder=None",
                        ]
                        conan.ci.step.UtilitySteps.withMapEnv(currentBuild, conanCreateEnv) {
                            conan.ci.step.ConanCommandSteps.conanCreate(currentBuild)
                            conan.ci.step.ConanCommandSteps.conanGraphBuildOrder(currentBuild)
                        }

                        String bofPath = new File(lockfile.parent, DemoConstant.CONAN_BUILDORDER_FILENAME).path
                        String difPath = new File(lockfile.parent, DemoConstant.DOCKER_IMAGE_FILENAME).path
                        conan.ci.step.DockerSteps.dockerCpFromContainer(currentBuild, lockfile.path)
                        conan.ci.step.DockerSteps.dockerCpFromContainer(currentBuild, difPath)
                        conan.ci.step.DockerSteps.dockerCpFromContainer(currentBuild, DemoConstant.CONAN_BUILDORDER_FILENAME, bofPath)

                    }
                }
                // Now we need to get the lockfiles and buildorder files out of the container so we can artifact them
                conan.ci.step.CopyArtifactSteps.saveFilesFromAllBuildsForDownstreams(currentBuild)
                FileWrapper[] buildOrderFileInfos = currentBuild.findFiles(
                        glob: "**/*${DemoConstant.CONAN_BUILDORDER_FILENAME}"
                )
                buildOrderFileInfos.each { FileWrapper buildOrderFileInfo ->
                    BuildOrderFile bof = BuildOrderFile.construct(currentBuild, buildOrderFileInfo)
                    buildOrderFiles.add(bof)
                }
            }
        }
        return buildOrderFiles
    }

    static void writeNewCombinedBuildOrderToDisk(def currentBuild, BuildOrderList buildOrderList) {
        currentBuild.writeFile(
                file: DemoConstant.CONAN_COMBINED_BUILDORDER_FILENAME,
                text: buildOrderList.combinedBuildOrderJsonString,
        )
    }

    static void getOriginalCombinedBuildOrderFromUpstream(def currentBuild) {
        DemoLogger.debug(currentBuild, "running getOriginalCombinedBuildOrderFromUpstream")
        conan.ci.step.CopyArtifactSteps.getCombinedBuildOrderFromUpstream(currentBuild)
        DemoLogger.debug(currentBuild, "Copied ${DemoConstant.CONAN_COMBINED_BUILDORDER_FILENAME} from upstream.")
    }

    static void getSiblingJobStatuses(def currentBuild) {
        DemoLogger.debug(currentBuild, "running getSiblingJobStatuses")
        CauseWrapper causeEvaluator = new CauseWrapper(currentBuild)

        DemoLogger.debug(currentBuild, "Copied ${DemoConstant.CONAN_COMBINED_BUILDORDER_FILENAME} from upstream.")
    }

    static void invokeDownstreamJobs(def currentBuild, BuildOrderList buildOrderList, String currentPackageName) {
        DemoLogger.debug(currentBuild, "running invokeDownstreamJobs")
        conan.ci.arg.DemoArgs demoArgs = new conan.ci.arg.DemoArgs(currentBuild.env)
        String downstreamBranch = demoArgs.downstreamBranchName
        String nameAndBranch = "${currentPackageName}/${downstreamBranch}"

        // Downstream trigger behavior depends on cause
        CauseWrapper causeEvaluator = new CauseWrapper(currentBuild)
        String lastCauseName = causeEvaluator.getLastCauseName()
        DemoLogger.debug(currentBuild, "lastCauseName ${lastCauseName}")
        List<String> nextPackageNames
        if (lastCauseName.contains("UpstreamCause")) {
            getOriginalCombinedBuildOrderFromUpstream(currentBuild)
            FileWrapper[] combinedBuildOrderFileInfos = currentBuild.findFiles(
                    glob: "upstream/**/*${DemoConstant.CONAN_COMBINED_BUILDORDER_FILENAME}"
            )
            CombinedBuildOrderFile cbof = CombinedBuildOrderFile
                    .construct(currentBuild, combinedBuildOrderFileInfos.first())
            List<String> currentJobSiblings = cbof.findCurrentJobSiblings(currentPackageName)

            List<String> siblingsWithBranch = currentJobSiblings.collect {
                "${it}/${downstreamBranch}".toString()
            }

            // Get the names of all all related and successful builds (spawned from the same original upstream)
            List<String> relatedSuccessfulBuildNames = new Search(currentBuild, nameAndBranch, siblingsWithBranch)
                    .getRelatedSuccessfulBuildNames()

            DemoLogger.debug(currentBuild, "relatedSuccessfulBuildNames ${relatedSuccessfulBuildNames.toString()}")
            // If all the siblings are in the related and successful list, we can trigger downstreams
            // If not, we don't trigger anything, and the last sibling to finish will trigger downstreams
            if (relatedSuccessfulBuildNames.containsAll(siblingsWithBranch)) {
                nextPackageNames = cbof.findNextLevelJobs(currentPackageName)
            } else {
                nextPackageNames = [] as List<String>
                List<String> incompleteJobs = siblingsWithBranch
                        .findAll { !relatedSuccessfulBuildNames.contains(it) }
                DemoLogger.debug(currentBuild, "skipping triggers of common downstreams because the following " +
                        "sibling builds have not completed yet: \n${incompleteJobs.join("\n")}")
            }
        } else {
            nextPackageNames = buildOrderList.nextPackageNames
        }

        DemoLogger.debug(currentBuild, "nextPackageNames :\n${nextPackageNames.join("\n")}")
        nextPackageNames
                .collect { "${it}/${downstreamBranch}".toString() }
                .each { currentBuild.build(job: it, wait: false) }
    }
}