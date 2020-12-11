package conan.ci.test


import conan.ci.conan.BuildOrder
import conan.ci.conan.BuildOrderList
import conan.ci.constant.DemoConstant
import conan.ci.jenkins.Search
import net.sf.json.JSONArray
import net.sf.json.groovy.JsonSlurper
import org.jenkinsci.plugins.workflow.steps.BodyInvoker
import org.jenkinsci.plugins.workflow.steps.EchoStep
import org.jenkinsci.plugins.workflow.steps.StepContext

class TestGetRelatedSuccessfulBuildNames {

    static void run(def currentBuild) {
        conan.ci.util.DemoLogger.debug(currentBuild, "running testGetRelatedSuccessfulBuildNames")
        String description = "This test intends to validate the mechanism for identifying all downstream jobs which" +
                "have been spawned from the same upstream as the current job.  This logic is only relevant for jobs " +
                "which have at least 1 upstreamCause in the list of upstream causes."
        conan.ci.util.DemoLogger.debug(currentBuild, description)

        String currentJobName = currentBuild.env['JOB_NAME']
        String finalDownstream = currentBuild.env['FINAL_DOWNSTREAM']
        String allDownstreamsStr = currentBuild.env['ALL_DOWNSTREAMS']
        List<String> allDownstreamJobs = allDownstreamsStr.split(",")

        List<String> currentJobSiblings = allDownstreamJobs
                .findAll{it != currentJobName}
                .findAll{it != finalDownstream}

        conan.ci.util.DemoLogger.debug(currentBuild, "skipping trigger of siblings ${currentJobSiblings.toString()}")

        // Get the names of all all related and successful builds (spawned from the same original upstream)
        List<String> relatedSuccessfulBuildNames = new Search(currentBuild, currentJobName, currentJobSiblings)
                .getRelatedSuccessfulBuildNames()

        conan.ci.util.DemoLogger.debug(currentBuild, "relatedSuccessfulBuildNames ${relatedSuccessfulBuildNames.toString()}")
        // If all the siblings are in the related and successful list, we can trigger downstreams
        // If not, we don't trigger anything, and the last sibling to finish will trigger downstreams
        if (relatedSuccessfulBuildNames.containsAll(currentJobSiblings)) {
            currentBuild.build(finalDownstream)
        } else {
            List<String> incompleteJobs = currentJobSiblings
                    .findAll { !relatedSuccessfulBuildNames.contains(it) }

            conan.ci.util.DemoLogger.debug(currentBuild, "skipping triggers of common downstreams because the following " +
                    "sibling builds have not completed yet: \n${incompleteJobs.join("\n")}")
        }
    }

    static void triggerDownstreamTest(def currentBuild) {
        conan.ci.util.DemoLogger.debug(currentBuild, "running triggerDownstreamTest")
        currentBuild.build("testDownstream1")
        currentBuild.build("testDownstream2")
    }

    static void testStepAsJavaCall(def currentBuild) {
        StepContext ctx = currentBuild.getContext(["type": BodyInvoker]) as StepContext
        new EchoStep("hello step").start(ctx)
    }

    static void testParallelStageBuildStep(def currentBuild) {
        String downstreamBranch = "master"
        List<String> nextPackageNames = ["libB", "libC"]
        nextPackageNames
                .collect { "${it}/${downstreamBranch}".toString() }
                .each { currentBuild.build(job: it, wait: false) }
    }

    static void conanBuildOrderParse(def currentBuild) {
        conan.ci.util.DemoLogger.debug(currentBuild, "running conanCreateAndUploadFromLockfiles")
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
        JSONArray jsonArray = new JsonSlurper().parseText(jsonStr) as JSONArray
        jsonArray.withIndex().each { Tuple2 tuple -> currentBuild.echo("${tuple.first} : ${tuple.second}") }
        BuildOrder.fromSingleBuildOrder(jsonArray)

    }

    static void testCombinedBuildOrder(def currentBuild) {
        conan.ci.util.DemoLogger.debug(currentBuild, "running testCombinedBuildOrder")
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
        conan.ci.util.DemoLogger.debug(currentBuild, "nextPackageNames :\n${nextPackageNames.join("\n")}")
    }


}
