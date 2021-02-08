package conan.ci.condition

import conan.ci.arg.*
import conan.ci.docker.DockerClient
import conan.ci.docker.DockerClientFactory
import conan.ci.jenkins.JenkinsAgentFactory
import conan.ci.pipeline.PipelineBase
import conan.ci.runner.DockerCommandRunner
import org.jenkinsci.plugins.workflow.cps.CpsScript

class ConanLockfilesNeedRebase {

    PipelineBase base
    static ConanLockfilesNeedRebase construct(CpsScript currentBuild, Map config) {
        def it = new ConanLockfilesNeedRebase()
        it.base = new PipelineBase()
        it.base.currentBuild = currentBuild
        it.base.jenkinsAgent = JenkinsAgentFactory.construct(currentBuild).get(config)
        it.base.dockerClientFactory = DockerClientFactory.construct(currentBuild)
        it.base.args = ArgumentList.construct(currentBuild, it.base.pipelineArgs())
        it.base.args.addAll(ConanArgs.get() + DockerArgs.get() + GitArgs.get() + ArtifactoryArgs.get())
        it.base.args.parseArgs(config)
        it.base.config = config
        it.base.printClass(it.class.simpleName)
        it.base.printArgs()
        return it
    }

    boolean run() {
        base.currentBuild.echo("conanLockfilesNeedRebase() condition will now be evaluated.")
        DockerClient dockerClient = base.dockerClientFactory.get(base.config, base.args.asMap['dockerDefaultImage'])
        boolean result = dockerClient.withRun("Start Container") { DockerCommandRunner dcr ->
            dockerClient.configureGit(dcr)
            cloneGitRepos(dcr)
            base.configureConan(dcr)
            return lockfilesNeedRebase(dcr)
        }
        return result
    }

    void cloneGitRepos(DockerCommandRunner dcr) {
        dcr.run("git clone ${base.args.asMap['scriptsUrl']} scripts")
        dcr.run("git clone ${base.args.asMap['conanConfigUrl']} configs")
        dcr.run("git clone ${base.args.asMap['gitUrl']} workspace")
        dcr.run("git checkout ${base.args.asMap['gitCommit']}", "workspace")
    }

    boolean lockfilesNeedRebase(DockerCommandRunner dcr) {
        //TODO: Implement the script below and make this the first stage in all jenkinsfiles
        String result = dcr.run("python ~/scripts/lockfiles_need_rebase.py " +
                " --conanfile_dir=workspace" +
                " ${base.args.asMap['conanRemoteUploadName']}", true)
        boolean lockfilesNeedRebase = (result.trim() == "True")
        base.currentBuild.echo("conanLockfilesNeedRebase() condition is returning value of ${lockfilesNeedRebase}")
        return lockfilesNeedRebase
    }
}