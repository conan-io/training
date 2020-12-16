package conan.ci.arg


class DockerArgs {

    static List<Argument> get() {
        return [
                new Argument<String>(
                        name: "dockerDefaultImage",
                        group: "docker",
                        envVarName: "TRAINING_DOCKER_DEFAULT_IMAGE",
                        description: "Docker image to be used in withRun method if none is provided as an argument.",
                ),
                new Argument<String>(
                        name: "dockerUser",
                        group: "docker",
                        envVarName: "TRAINING_DOCKER_USER",
                        serializer: { String s -> "--user ${s}" },
                        description: "User inside container to perform commands as.",
                ),
                new Argument<String>(
                        name: "dockerNetworkName",
                        group: "docker",
                        envVarName: "TRAINING_DOCKER_NETWORK_NAME",
                        serializer: { String s -> "--network ${s}" },
                        description: "All docker containers will be connected this network.",
                ),
                new Argument<String>(
                        name: "dockerRunCommandRaw",
                        group: "docker",
                        envVarName: "TRAINING_DOCKER_RUN_COMMAND_RAW",
                        description: "Skip prefixing commands in docker with a shell (such as /bin/bash -c or cmd -c)",
                ),
                new Argument<String>(
                        name: "dockerMemory",
                        group: "docker",
                        envVarName: "TRAINING_DOCKER_MEMORY",
                        serializer: { String s -> "-m ${s}" },
                        description: "Amount of memory to set for container with the -m flag.",
                ),
                new Argument<String>(
                        name: "dockerEntryPoint",
                        group: "docker",
                        envVarName: "TRAINING_DOCKER_ENTRY_POINT",
                        serializer: { String s -> "--entrypoint ${s}" },
                        description: "Override the default entrypoint with custom command or script.",
                ),
                new Argument<List<String>>(
                        name: "dockerEnvVars",
                        group: "docker",
                        envVarName: "TRAINING_DOCKER_ENV_VARS",
                        deserializer: { String s -> s.split(",").collect { it } },
                        serializer: { List<String> l -> l.collect { String s -> "--env ${s}" }.join(" ") },
                        description: "Comma-separated list of environment variables to pass with -e flag.",
                ),
                new Argument<List<String>>(
                        name: "dockerArgs",
                        group: "docker",
                        envVarName: "TRAINING_DOCKER_ARGS",
                        deserializer: { String s -> s.split(",").collect { it } },
                        serializer: { List<String> l -> l.collect { String s -> s }.join(" ") },
                        description: "Comma-separated list of arbitrary arguments to send to docker run command.",
                ),
        ]
    }
}
