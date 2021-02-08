package conan.ci.runner


import org.jenkinsci.plugins.workflow.cps.CpsScript

class NativeCommandRunnerFactory {
    CpsScript currentBuild

    static NativeCommandRunnerFactory construct(CpsScript currentBuild) {
        def it = new NativeCommandRunnerFactory()
        it.currentBuild = currentBuild
        return it
    }

    NativeCommandRunner get(Boolean isUnix){
        NativeCommandRunner it = isUnix
                ? NativeCommandRunnerLinux.construct(currentBuild)
                : NativeCommandRunnerWindows.construct(currentBuild)
        return it
    }

    NativeCommandRunner get(){
        return get(currentBuild.isUnix() as Boolean)
    }
}
