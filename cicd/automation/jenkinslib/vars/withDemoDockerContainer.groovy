import conan.ci.step.DockerSteps
def call(Closure body) {
    DockerSteps.withDemoDockerContainer(this, body)
}


