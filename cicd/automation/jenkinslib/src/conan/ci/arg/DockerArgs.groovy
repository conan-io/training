package conan.ci.arg

class DockerArgs {
    def currentBuild
    def env

    DockerArgs(def currentBuild) {
        this.currentBuild = currentBuild
        this.env = currentBuild.env
    }

    String getRunCommand(String commandToRun = "", String detectedShell = "") {
        List<String> cliArgs = ["docker run -d -t"]
        if (containerNameCmd()) {
            cliArgs.add(containerNameCmd())
        }
        if (networkNameCmd()) {
            cliArgs.add(networkNameCmd())
        }
        if (runTtyCmd()) {
            cliArgs.add(runTtyCmd())
        }
        if (runRemoveCmd()) {
            cliArgs.add(runRemoveCmd())
        }
        if (memoryCmd()) {
            cliArgs.add(memoryCmd())
        }
        if (entrypointCmd()) {
            cliArgs.add(entrypointCmd())
        }
        if (userCmd()) {
            cliArgs.add(userCmd())
        }
        cliArgs.addAll(envVarsCmd())
        cliArgs.addAll(addMountsCmd())
        cliArgs.addAll(runArgsCmd())
        if (workingDirCmd()) {
            cliArgs.add(workingDirCmd())
        }
        if (containerUserCmd()) {
            cliArgs.add(containerUserCmd())
        }
        cliArgs.add(imageCmd())
        if (shellRunCmd(commandToRun, detectedShell)) {
            cliArgs.add(shellRunCmd(commandToRun, detectedShell))
        }
        return cliArgs.join(" ")
    }

    String getExecCommand(String commandToRun, String workingDirectory = "", String user = "", String shell = "") {
        List<String> cliArgs = ["docker exec"]
        cliArgs.addAll(envVarsCmd())
        cliArgs.addAll(execArgsCmd())
        if (workingDirCmd(workingDirectory)) {
            cliArgs.add(workingDirCmd(workingDirectory))
        }
        if (containerUserCmd(user)) {
            cliArgs.add(containerUserCmd())
        }
        if (getContainerName()) {
            cliArgs.add(getContainerName())
        }
        cliArgs.add(shellExecCmd(commandToRun, shell))
        return cliArgs.join(" ")
    }

    String getCpCommand(String src, String dst) {
        List<String> cliArgs = ["docker cp"]
        cliArgs.add(src)
        cliArgs.add(dst)
        return cliArgs.join(" ")
    }

    String getChownCommand(String path = ".") {
        return "chown -hR ${containerUser} ${path}"
    }

    String getContainerUser() {
        return env["DEMO_CONTAINER_USER"] ?: "conan"
    }

    String containerUserCmd(String user) {
        if (containerUser == "") {
            return containerUser
        } else {
            return "--user ${containerUser}"
        }
    }

    String getTagCommand() {
        List<String> cliArgs = ["docker tag"]
        if (containerNameCmd()) {
            cliArgs.add(containerNameCmd())
        }
        return cliArgs.join(" ")
    }

    String getRmiCommand() {
        List<String> cliArgs = ["docker rmi"]
        return cliArgs.join(" ")
    }

    String getBuildCommand() {
        List<String> cliArgs = ["docker build"]
        return cliArgs.join(" ")
    }

    String getPushCommand() {
        List<String> cliArgs = ["docker push"]
        return cliArgs.join(" ")
    }

    String getPullCommand() {
        List<String> cliArgs = ["docker pull"]
        return cliArgs.join(" ")
    }

    String getInspectCommand(String args = "") {
        List<String> cliArgs = ["docker inspect"]
        if (args == "") {
            cliArgs.addAll(inspectArgsCmd())
        } else {
            cliArgs.add(args)
        }
        if (imageCmd()) {
            cliArgs.add(imageCmd())
        }
        return cliArgs.join(" ")
    }

    String getPsCommand(String args = "") {
        List<String> cliArgs = ["docker ps -q"]
        if (containerNameFilterCmd()) {
            cliArgs.add(containerNameFilterCmd())
        }
        if (args == "") {
            cliArgs.addAll(psArgsCmd())
        } else {
            cliArgs.add(args)
        }
        return cliArgs.join(" ")
    }

    static String getStopCommand(String containerId) {
        List<String> cliArgs = ["docker stop -f ${containerId}"]
        return cliArgs.join(" ")
    }

    static String getRmCommand(String containerId) {
        List<String> cliArgs = ["docker rm -f ${containerId}"]
        return cliArgs.join(" ")
    }

    String getImage() {
        return env["DEMO_DOCKER_IMAGE"] ?: ""
    }

    String imageCmd() {
        if (image == "") {
            throw new Exception("DEMO_DOCKER_IMAGE must be set")
        } else {
            return image
        }
    }

    String getNetworkName() {
        return env["DEMO_DOCKER_NETWORK_NAME"] ?: ""
    }

    String networkNameCmd() {
        if (networkName == "") {
            return networkName
        } else {
            return "--network ${networkName}"
        }
    }

    String getContainerName() {
        return env["DEMO_DOCKER_CONTAINER_NAME"] ?: "${env["BUILD_TAG"]}_${env["STAGE_NAME"]}"
    }

    String containerNameCmd() {
        if (containerName == "") {
            return containerName
        } else {
            return "--name ${containerName}"
        }
    }

    String getContainerNameFilter() {
        return env["DEMO_DOCKER_CONTAINER_NAME"] ?: "${env["BUILD_TAG"]}"
    }

    String containerNameFilterCmd() {
        if (containerNameFilter == "") {
            return containerNameFilter
        } else {
            return "--filter \"name=${containerNameFilter}\""
        }
    }

    String getMemory() {
        return env["DEMO_DOCKER_MEMORY"] ?: ""
    }

    String memoryCmd() {
        if (memory == "") {
            return memory
        } else {
            return "-m ${memory}"
        }
    }

    String getShell() {
        return env["DEMO_DOCKER_SHELL"] ?: ""
    }

    String shellRunCmd(String commandToRun = "", String shell = "") {
        if (this.shell == "" || this.shell == "none") {
            if (commandToRun) {
                return "\"${commandToRun}\""
            } else {
                return commandToRun
            }
        } else {
            if (commandToRun == "") {
                throw new Exception("commandToRun required if DEMO_DOCKER_SHELL is not \"none\" or \"\" or unset")
            } else {
                if (this.shell == "detect") {
                    return "${shell} \"${commandToRun}\""
                } else {
                    return "${this.shell} \"${commandToRun}\""
                }
            }
        }
    }

    String shellExecCmd(String commandToRun, String shell = "") {
        if (this.shell == "none") {
            return "\"${commandToRun}\""
        } else if (this.shell == "" || this.shell == "detect") {
            return "${shell} '${commandToRun}'"
        } else {
            return "${this.shell} '${commandToRun}'"
        }
    }

    String getWorkingDirectory() {
        return env["DEMO_DOCKER_WORKING_DIR"] ?: ""
    }

    String workingDirCmd(String workingDir = "") {
        if (this.workingDirectory == "") {
            if (workingDir == "") {
                return workingDir
            } else {
                return "-w ${workingDir}"
            }
        } else {
            return "-w ${this.workingDirectory}"
        }
    }

    String getUser() {
        return env["DEMO_DOCKER_USER"] ?: ""
    }

    String userCmd(String user = "") {
        if (this.user == "") {
            if (user == "") {
                return user
            } else {
                return "-w ${user}"
            }
        } else {
            return "-w ${this.user}"
        }
    }

    String getEntrypoint() {
        return env["DEMO_DOCKER_ENTRY_POINT"] ?: ""
    }

    String entrypointCmd() {
        if (entrypoint == "") {
            return entrypoint
        } else {
            return "--entrypoint ${entrypoint}"
        }
    }

    String getRunRemove() {
        return env["DEMO_DOCKER_RUN_REMOVE"] ?: ""
    }

    String runRemoveCmd() {
        if (runRemove == "") {
            return runRemove
        } else {
            return "--rm ${runRemove}"
        }
    }

    String getCpSrc(String path = "") {
        return path ?: env["DEMO_DOCKER_CP_SRC"] ?: ""
    }

    String getCpDst(String path = "") {
        return path ?: env["DEMO_DOCKER_CP_DST"] ?: ""
    }

    String getRunTty() {
        return env["DEMO_DOCKER_RUN_TTY"] ?: ""
    }

    String runTtyCmd() {
        if (runTty == "") {
            return runTty
        } else {
            return "-t"
        }
    }

    String getEnvVars() {
        return env["DEMO_DOCKER_ENV_VARS"] ?: ""
    }

    List<String> envVarsCmd() {
        if (envVars == "") {
            return []
        } else {
            return envVars.split(',')?.collect { "-e ${it}".toString() }
        }
    }

    String getRunArgs() {
        return env["DEMO_DOCKER_RUN_ARGS"] ?: ""
    }

    List<String> runArgsCmd() {
        if (runArgs == "") {
            return []
        } else {
            return runArgs.split(',')?.collect { "${it}".toString() }
        }
    }

    String getExecArgs() {
        return env["DEMO_DOCKER_EXEC_ARGS"] ?: ""
    }

    List<String> execArgsCmd() {
        if (execArgs == "") {
            return []
        } else {
            return execArgs.split(',')?.collect { "${it}".toString() }
        }
    }

    String getInspectArgs() {
        return env["DEMO_DOCKER_INSPECT_ARGS"] ?: ""
    }

    List<String> inspectArgsCmd() {
        if (inspectArgs == "") {
            return []
        } else {
            return inspectArgs.split(',')?.collect { "${it}".toString() }
        }
    }

    String getPsArgs() {
        return env["DEMO_DOCKER_PS_ARGS"] ?: ""
    }

    List<String> psArgsCmd() {
        if (psArgs == "") {
            return []
        } else {
            return psArgs.split(',')?.collect { "${it}".toString() }
        }
    }

    String getAddMounts() {
        return env["DEMO_DOCKER_ADD_MOUNTS"] ?: ""
    }

    String getContainerOs() {
        return env["DEMO_DOCKER_CONTAINER_OS"] ?: ""
    }

    List<String> addMountsCmd() {
        if (addMounts == "") {
            return []
        } else {
            return addMounts.split(',')?.collect { "-v ${it}".toString() }
        }
    }
}