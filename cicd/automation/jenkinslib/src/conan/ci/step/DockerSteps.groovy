package conan.ci.step


import conan.ci.docker.DockerContainerInspector
import conan.ci.runner.CommandRunnerSelector
import conan.ci.runner.DockerCommandRunner
import conan.ci.runner.NativeCommandRunner
import conan.ci.util.DemoLogger

class DockerSteps {

    static void dockerRmForce(def currentBuild, String containerId) {
        DemoLogger.debug(currentBuild, "Running dockerRm")
        conan.ci.arg.DockerArgs dockerArgs = new conan.ci.arg.DockerArgs(currentBuild)
        String command = dockerArgs.getRmCommand(containerId)
        DemoLogger.debug(currentBuild, "command: ${command}")
        def commandRunner = new NativeCommandRunner(currentBuild)

        commandRunner.run(command)
    }

    static void dockerCp(def currentBuild, String src, String dst) {
        DemoLogger.debug(currentBuild, "Running dockerCp")
        conan.ci.arg.DockerArgs dockerArgs = new conan.ci.arg.DockerArgs(currentBuild)
        String command = dockerArgs.getCpCommand(src, dst)
        DemoLogger.debug(currentBuild, "command: ${command}")
        def commandRunner = new NativeCommandRunner(currentBuild)

        commandRunner.run(command)
    }

    static void dockerCpFromContainer(def currentBuild, Map<String, String> copyConfig) {
        DemoLogger.debug(currentBuild, "Running dockerCpFromContainer")
        String detectedWorkingDir = new DockerContainerInspector(currentBuild).detectWorkingDirectory()
        String fullWorkingDir = new conan.ci.arg.DirArgs(currentBuild, ["workingDir": detectedWorkingDir]).getFullWorkingDir()
        conan.ci.arg.DockerArgs dockerArgs = new conan.ci.arg.DockerArgs(currentBuild)
        String containerName = dockerArgs.getContainerName()
        String cpSrc = dockerArgs.getCpSrc(copyConfig['src'])
        String cpDst = dockerArgs.getCpDst(copyConfig['dst'] ?: copyConfig['src'])
        conan.ci.arg.JenkinsArgs jenkinsArgs = new conan.ci.arg.JenkinsArgs(currentBuild)
        String stageName = jenkinsArgs.stageName
        currentBuild.dir(stageName) {
            currentBuild.touch(cpDst)
            dockerCp(currentBuild, "${containerName}:${fullWorkingDir}/${cpSrc}", cpDst)
        }
    }

    static void dockerCpToContainer(def currentBuild, Map<String, String> copyConfig) {
        DemoLogger.debug(currentBuild, "Running dockerCpToContainer")
        String detectedWorkingDir = new DockerContainerInspector(currentBuild).detectWorkingDirectory()
        String fullWorkingDir = new conan.ci.arg.DirArgs(currentBuild, ["workingDir": detectedWorkingDir]).getFullWorkingDir()
        conan.ci.arg.DockerArgs dockerArgs = new conan.ci.arg.DockerArgs(currentBuild)
        String containerName = dockerArgs.getContainerName()
        String cpSrc = dockerArgs.getCpSrc(copyConfig['src'])
        String cpDst = dockerArgs.getCpDst(copyConfig['dst'] ?: copyConfig['src'])
        conan.ci.arg.JenkinsArgs jenkinsArgs = new conan.ci.arg.JenkinsArgs(currentBuild)
        String stageName = jenkinsArgs.stageName
        currentBuild.dir(stageName) {
            currentBuild.touch(cpDst)
            dockerCp(currentBuild, cpSrc, "${containerName}:${fullWorkingDir}/${cpDst}")
        }
    }

    static void dockerCpWorkspaceToContainer(def currentBuild) {
        DemoLogger.debug(currentBuild, "Running dockerCpWorkspaceToContainer")
        DockerContainerInspector containerInspector = new DockerContainerInspector(currentBuild)
        String detectedOs = containerInspector.dockerInpsectOs()
        String detectedWorkingDir = containerInspector.detectWorkingDirectory()
        String detectedWorkingSubdir = new conan.ci.arg.DirArgs(currentBuild).getWorkingSubDir()
        Map dirArgConfig = ["workingDir": detectedWorkingDir, "workingSubDir": detectedWorkingSubdir]
        String fullWorkingDir = new conan.ci.arg.DirArgs(currentBuild, dirArgConfig).getFullWorkingDir()
        conan.ci.arg.DockerArgs dockerArgs = new conan.ci.arg.DockerArgs(currentBuild)
        String containerName = dockerArgs.getContainerName()
        conan.ci.arg.JenkinsArgs jenkinsArgs = new conan.ci.arg.JenkinsArgs(currentBuild)
        if (jenkinsArgs.workspaceFile.exists()) {
            dockerCp(currentBuild, "${jenkinsArgs.workspaceFile.path}/.", "${containerName}:${fullWorkingDir}")
        }
        if (jenkinsArgs.parentWorkspaceFile.exists()) {
            dockerCp(currentBuild, "${jenkinsArgs.parentWorkspaceFile.path}/.", "${containerName}:${fullWorkingDir}")
        }
        if (jenkinsArgs.startingWorkspaceFile.exists()) {
            dockerCp(currentBuild, "${jenkinsArgs.startingWorkspaceFile.path}/.", "${containerName}:${fullWorkingDir}")
        }
        if (detectedOs == "linux") {
            String command = dockerArgs.getChownCommand(fullWorkingDir,)
            DemoLogger.debug(currentBuild, "command: ${command}")
            UtilitySteps.withMapEnv(currentBuild, ["DEMO_CONTAINER_USER": "root"]) {
                def commandRunner = CommandRunnerSelector.select(currentBuild)

                commandRunner.run(command, "", false)
            }
        }
    }

    static void withDemoDockerContainer(def currentBuild, Closure body) {
        DemoLogger.debug(currentBuild, "Running withDemoDockerContainer")
        conan.ci.arg.DockerArgs dockerArgs = new conan.ci.arg.DockerArgs(currentBuild)
        String runCommand = dockerArgs.getRunCommand()
        DemoLogger.debug(currentBuild, "command: ${runCommand}")
        def commandRunner = new NativeCommandRunner(currentBuild)
        commandRunner.run(runCommand)

        String workingDir = new conan.ci.arg.DirArgs(currentBuild).getWorkingDir()
        String workingSubdir = new conan.ci.arg.DirArgs(currentBuild).getWorkingSubDir()
        if (workingSubdir) {
            DockerCommandRunner dockerCommandRunner = new DockerCommandRunner(currentBuild)
            dockerCommandRunner.run("mkdir ${workingSubdir}", workingDir)
        }

        Map<String, String> withDockerEnv = [
                "DEMO_COMMAND_RUNNER"       : "docker",
                "DEMO_DOCKER_CONTAINER_NAME": dockerArgs.containerName
        ]
        try {
            UtilitySteps.withMapEnv(currentBuild, withDockerEnv, body)
        } finally {
            String rmCommand = dockerArgs.getRmCommand(dockerArgs.getContainerName())
            DemoLogger.debug(currentBuild, "command: ${rmCommand}")
            commandRunner.run(rmCommand)
        }
    }

}