package conan.ci.jenkins

import conan.ci.runner.NativeCommandRunner
import org.jenkinsci.plugins.workflow.cps.CpsScript

abstract class JenkinsAgent {

    CpsScript currentBuild
    Map config
    NativeCommandRunner nativeCommandRunner
    Boolean isUnix

}
