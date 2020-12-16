package conan.ci.runner

import conan.ci.docker.DockerClient
import org.jenkinsci.plugins.workflow.cps.CpsScript

class DockerCommandRunner implements ICommandRunner {
    CpsScript currentBuild
    DockerClient dockerClient
    String containerId
    NativeCommandRunner nativeCommandRunner

    static DockerCommandRunner construct(CpsScript currentBuild, DockerClient dockerClient, String containerId) {
        def it = new DockerCommandRunner()
        it.currentBuild = currentBuild
        it.dockerClient = dockerClient
        it.containerId = containerId
        it.nativeCommandRunner = NativeCommandRunnerFactory.construct(currentBuild).get()
        return it
    }

    @Override
    def run(String commandToRun, Boolean returnStdout = true) {
        String workingDir = dockerClient.workdir
        run(commandToRun, workingDir, returnStdout)
    }

    @Override
    def run(String commandToRun, String workDir, Boolean returnStdout = true) {
        String workDirAbs = (workDir.contains(dockerClient.workdir))
                ? workDir
                : new File(dockerClient.workdir, workDir).toString()

        String execCommand = "docker exec " +
                "--workdir ${workDirAbs} " +
                "${dockerClient.args.asMap['dockerUser']} " +
                "${dockerClient.args.asMap['dockerEnvVars']} " +
                "${containerId} " +
                "${dockerClient.wrapCommandForShell(commandToRun)}"
        return nativeCommandRunner.run(execCommand, returnStdout)
    }

}
