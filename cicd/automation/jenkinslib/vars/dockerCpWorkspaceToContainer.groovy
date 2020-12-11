import conan.ci.step.DockerSteps
def call() {
    DockerSteps.dockerCpWorkspaceToContainer(this)
}


