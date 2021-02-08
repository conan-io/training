package conan.ci.arg

class GitArgs {

    static List<Argument> get(def currentBuild) {
        return [
                new Argument<String>(
                        name: "gitBranch",
                        group: "git",
                        envVarName: "GIT_BRANCH",
                        description: "Branch which current job is building under.",
                ),
                new Argument<String>(
                        name: "gitCommit",
                        group: "git",
                        envVarName: "GIT_COMMIT",
                        description: "The commit which triggered this build, or the most recent if not triggered.",
                ),
                new Argument<String>(
                        name: "gitUrl",
                        group: "git",
                        envVarName: "GIT_URL",
                        description: "The URL of the git repository associated with this job.",
                ),
        ]
    }
}
