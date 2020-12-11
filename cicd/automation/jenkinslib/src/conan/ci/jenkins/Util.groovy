package conan.ci.jenkins

class Util {

    static List convertMapToJenkinsEnv(Map<String, String> srcEnv) {
        return srcEnv.collect { "${it.key}=${it.value}" }
    }
}
