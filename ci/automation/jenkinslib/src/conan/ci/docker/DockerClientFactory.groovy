package conan.ci.docker

import conan.ci.runner.NativeCommandRunner
import conan.ci.runner.NativeCommandRunnerFactory
import org.jenkinsci.plugins.workflow.cps.CpsScript

class DockerClientFactory {
    CpsScript currentBuild
    NativeCommandRunner nativeCommandRunner

    static DockerClientFactory construct(CpsScript currentBuild) {
        def it = new DockerClientFactory()
        it.currentBuild = currentBuild
        it.nativeCommandRunner = NativeCommandRunnerFactory.construct(currentBuild).get()
        return it
    }

    DockerClient get(Map config, String imageName) {
        DockerClient client = queryImageOs(imageName) == "linux"
                ? DockerClientLinux.construct(currentBuild, config, imageName)
                : DockerClientWindows.construct(currentBuild, config, imageName)
        return client
    }

    String queryContainerOs(String containerId) {
        String imageName = nativeCommandRunner.run("docker inspect -f '{{ .Config.Image }}' ${containerId}").trim()
        return queryImageOs(imageName)
    }

    String queryImageOs(String imageName) {
        return nativeCommandRunner.run("docker inspect -f '{{ .Os }}' ${imageName}").trim()
    }
}
