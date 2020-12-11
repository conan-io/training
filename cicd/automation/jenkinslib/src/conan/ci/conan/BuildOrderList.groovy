package conan.ci.conan

import com.cloudbees.groovy.cps.NonCPS
import groovy.json.JsonBuilder

class BuildOrderList {
    List<BuildOrder> buildOrders

    BuildOrderList(List<BuildOrder> buildOrders) {
        this.buildOrders = buildOrders
    }

    List<PackageOccurrence> getAllPackageOccurences() {
        return buildOrders.collectMany { BuildOrder bo -> bo.packageOccurences }
    }

    // the algorithm is currently simple
    // given a list of build orders (wherein a package might appear in different "positions" in different build orders)
    //     flatten the lists to a single lists of all occurrences of all packages, adding the "position" attribute
    //     convert the flattened list to a map of "package to list-of-package-occurrences"
    //     from each of those maps, sort the list by position, and take the one with the highest number
    //     this will return a list of unique package occurrences
    //     create a map which groups those by position
    //     sort the map which uses the maps keys as the sort criteria by default
    //
    List<List<PackageOccurrence>> getCombinedBuildOrder() {
        List<List<PackageOccurrence>> ret = allPackageOccurences
                .groupBy { it.pRef.getName() }
                .collect { String _, List<PackageOccurrence> pos -> getLastPackageOccurrence(pos) }
                .groupBy { PackageOccurrence po -> po.position }
                .sort()
                .values()
                .collect { it }
        return ret

    }

    // This method only exists because we can't call sort() inside a non-cps method
    @NonCPS
    static PackageOccurrence getLastPackageOccurrence(List<PackageOccurrence> packageOccurrences) {
        return packageOccurrences.sort().last()
    }


    List<List<String>> getCombinedBuildOrderNames() {
        List<List<String>> ret = combinedBuildOrder
                .collect { List<PackageOccurrence> pos ->
                    pos.collect { PackageOccurrence po ->
                        po.pRef.name
                    }
                }
        return ret
    }

    String getCombinedBuildOrderJsonString() {
        return new JsonBuilder(combinedBuildOrder).toPrettyString()
    }

    List<String> getNextPackageNames() {
        List<String> ret = getCombinedBuildOrderNames().first()
        return ret
    }
}