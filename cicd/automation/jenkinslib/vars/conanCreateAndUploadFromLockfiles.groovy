import conan.ci.step.ConanWorkflowSteps
def call() {
    ConanWorkflowSteps.createAndUploadFromLockfiles(this)
}


