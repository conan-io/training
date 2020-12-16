package conan.ci.conan
//import spock.lang.Specification


//class BuildOrderSpec extends Specification {
//    def "constructBuildOrder object properly"() {
//        when:
//        File rootDir = new File('test/resources/build-order-files')
//
//        List<File> buildOrderFilePaths = []
//        rootDir.eachFileRecurse { File f ->
//            if (f.name == TrainingConstant.CONAN_BUILDORDER_FILENAME) {
//                buildOrderFilePaths.add(f)
//            }
//        }
//
//        JSONArray jsonArray = new JsonSlurper().parse(buildOrderFilePaths.first()) as JSONArray
//        BuildOrder buildOrder = BuildOrder.fromSingleBuildOrder(jsonArray)
//        println(new JsonBuilder(buildOrder.packageOccurences).toPrettyString())
//
//        then:
//        true
//
//    }
//
//    def "combineBuildOrdersProperly"() {
//        when:
//        File rootDir = new File('test/resources/build-order-files')
//
//        List<File> buildOrderFilePaths = []
//        rootDir.eachFileRecurse { File f ->
//            if (f.name == TrainingConstant.CONAN_BUILDORDER_FILENAME) {
//                buildOrderFilePaths.add(f)
//            }
//        }
//
//        List<BuildOrder> buildOrders = buildOrderFilePaths.collect { File file ->
//            JSONArray jsonArray = new JsonSlurper().parse(file) as JSONArray
//            return BuildOrder.fromSingleBuildOrder(jsonArray)
//        }
//
//        BuildOrderList bol = new BuildOrderList(buildOrders)
//        print(bol.combinedBuildOrderJsonString)
//
//        then:
//        true
//
//    }
//
//    def "parseCombinedBuildOrder"() {
//        when:
//        File rootDir = new File('test/resources/build-order-files')
//
//        File bof = new File(rootDir, TrainingConstant.CONAN_COMBINED_BUILDORDER_FILENAME)
//        JSONArray jsonArray = new JsonSlurper().parse(bof) as JSONArray
//
//        print(jsonArray.toString())
//
//        then:
//        true
//
//    }
//}






