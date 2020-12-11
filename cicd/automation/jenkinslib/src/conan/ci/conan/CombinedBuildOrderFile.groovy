package conan.ci.conan


import net.sf.json.JSONArray
import org.jenkinsci.plugins.pipeline.utility.steps.fs.FileWrapper
class CombinedBuildOrderFile {
    def currentBuild
    JSONArray json

    static CombinedBuildOrderFile construct(def currentBuild, FileWrapper fileWrapper) {
        JSONArray json = currentBuild.readJSON(file: fileWrapper.path) as JSONArray
        return construct(currentBuild, json)
    }

    static CombinedBuildOrderFile construct(def currentBuild, JSONArray json) {
        CombinedBuildOrderFile buildOrderFile = new CombinedBuildOrderFile(currentBuild)
        buildOrderFile.json = json
        return buildOrderFile
    }

    private CombinedBuildOrderFile(def currentBuild) {
        this.currentBuild = currentBuild
    }

    List<String> findCurrentJobSiblings(String name) {
        Integer level = findOriginalGraphLevelOfPackage(name)
        List<String> siblings = flattenBuildOrderList()
                .findAll { PackageOccurrence pkg -> pkg.position == level }
                .findAll { PackageOccurrence pkg -> pkg.pRef.name != name } // Filter out current package
                .collect { PackageOccurrence pkg -> pkg.pRef.name }
        return siblings
    }

    List<String> findNextLevelJobs(String name) {
        Integer level = findOriginalGraphLevelOfPackage(name)
        List<String> siblings = flattenBuildOrderList()
                .findAll { PackageOccurrence pkg -> pkg.position == level + 1}
                .collect { PackageOccurrence pkg -> pkg.pRef.name }
        return siblings
    }

    Integer findOriginalGraphLevelOfPackage(String name) {
        Integer position = flattenBuildOrderList()
                .find { PackageOccurrence pkg -> pkg.pRef.name == name }.position
        return position
    }

    List<PackageOccurrence> flattenBuildOrderList() {
        List<BuildOrder> combinedBuildOrder = json.collect { JSONArray json ->
            return BuildOrder.fromCombinedBuildOrder(json)
        }
        BuildOrderList buildOrderList = new BuildOrderList(combinedBuildOrder)
        return buildOrderList.allPackageOccurences
    }


}
