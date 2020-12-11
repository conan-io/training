import conan.ci.jenkins.Workspace
def call(Closure body) {
    Workspace.withCleanWorkspace(this, body)
}


