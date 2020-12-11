import conan.ci.step.DockerSteps
def call(Map<String, String> config = [:]) {
    DockerSteps.dockerCpToContainer(this, config)
}


