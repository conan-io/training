import conan.ci.step.UtilitySteps

def call(Map<String, String> config, Closure body) {
    UtilitySteps.withMapEnv(this, config, body)
}


