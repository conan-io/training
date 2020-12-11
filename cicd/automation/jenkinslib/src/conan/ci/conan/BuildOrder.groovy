package conan.ci.conan

import net.sf.json.JSONArray
import net.sf.json.JSONObject

class BuildOrder {
    List<PackageOccurrence> packageOccurences

    static BuildOrder fromSingleBuildOrder(JSONArray jsonArray){
        BuildOrder buildOrder = new BuildOrder()
        buildOrder.packageOccurences = jsonArray
                .withIndex()
                .collectMany { Tuple2<JSONArray, Integer> level ->
                    Integer index = level.second
                    level.first.collect{JSONArray occurrence->
                        PackageOccurrence.construct(index, occurrence as Tuple2<String, String>)
                    }
                }
        return buildOrder
    }

    static BuildOrder fromCombinedBuildOrder(JSONArray jsonArray){
        BuildOrder buildOrder = new BuildOrder()
        buildOrder.packageOccurences = jsonArray
                .collect{ JSONObject obj ->
                    PackageOccurrence.construct(obj)
                }
        return buildOrder
    }

    private BuildOrder() {
    }
}
