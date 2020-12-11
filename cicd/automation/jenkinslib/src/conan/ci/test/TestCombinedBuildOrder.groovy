package conan.ci.test

import conan.ci.conan.BuildOrder
import conan.ci.conan.BuildOrderList
import conan.ci.constant.DemoConstant
import conan.ci.util.DemoLogger
import net.sf.json.JSONArray
class TestCombinedBuildOrder {

    static void run(def currentBuild) {
        DemoLogger.debug(currentBuild, "running testCombinedBuildOrder")
        def jsonStr = """[
 [
  [
   "3",
   "libB/1.0@mycompany/stable#ddd949983d7d98ecff163e995530f655:df10e7c5c10b0aade71ef35fee88e6f75779b39f"
  ],
  [
   "5",
   "libC/1.0@mycompany/stable#2d72442493a73de4d5a04077194112a4:df10e7c5c10b0aade71ef35fee88e6f75779b39f"
  ]
 ],
 [
  [
   "2",
   "libD/1.0@mycompany/stable#e4b55773f5af3b54872da5cb3925481c:e0b8c47e930b65636d70aa71191b06d4cdc51120"
  ]
 ],
 [
  [
   "1",
   "App/1.0@mycompany/stable#0218ff21ca6ce217dbd7ad0129305878:9addeceb117b18112908457333a43de9c0283f51"
  ]
 ]
]"""
        def jsonStr2 = """[
 [
  [
   "2",
   "libC/1.0@mycompany/stable#2d72442493a73de4d5a04077194112a4:df10e7c5c10b0aade71ef35fee88e6f75779b39f"
  ]
 ],
 [
  [
   "1",
   "App2/1.0@mycompany/stable#3c116b1419db8510703215324e10e569:48a682987676aac09c92bad21ca96ffa43a4f9b9"
  ]
 ]
]"""
        List<String> buildOrderStrings = [jsonStr, jsonStr2]
        List<JSONArray> buildOrderJsons = buildOrderStrings.collect { currentBuild.readJSON(text: it) as JSONArray }
        List<BuildOrder> buildOrders = buildOrderJsons.collect { BuildOrder.fromSingleBuildOrder(it) }
        BuildOrderList buildOrderList = new BuildOrderList(buildOrders)
        String json = buildOrderList.combinedBuildOrderJsonString
        currentBuild.writeFile(
                file: DemoConstant.CONAN_COMBINED_BUILDORDER_FILENAME,
                text: json
        )
        List<String> nextPackageNames = buildOrderList.nextPackageNames
        DemoLogger.debug(currentBuild, "nextPackageNames :\n${nextPackageNames.join("\n")}")
    }

}
