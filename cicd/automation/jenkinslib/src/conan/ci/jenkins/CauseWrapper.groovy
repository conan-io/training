package conan.ci.jenkins

import com.cloudbees.groovy.cps.NonCPS
import hudson.model.Cause
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper

class CauseWrapper {

    def currentBuild

    CauseWrapper(def currentBuild){
        this.currentBuild = currentBuild
    }

    @NonCPS
    Cause.UpstreamCause getFirstUpstreamCause() {
        RunWrapper runWrapper = currentBuild.currentBuild as RunWrapper
        List<Cause> causes = runWrapper.getRawBuild().getCauses()
        Cause.UpstreamCause upstreamCause = causes
                .find{Cause cause -> cause instanceof Cause.UpstreamCause} as Cause.UpstreamCause
        return upstreamCause
    }

    @NonCPS
    int getFirstUpstreamBuild() {
        Cause.UpstreamCause upstreamCause = firstUpstreamCause
        int upstreamBuild = upstreamCause.upstreamBuild
        return upstreamBuild
    }

    @NonCPS
    String getFirstUpstreamProject() {
        Cause.UpstreamCause upstreamCause = firstUpstreamCause
        String upstreamProject = upstreamCause.upstreamProject
        return upstreamProject
    }

    @NonCPS
    String getLastCauseName() {
        RunWrapper runWrapper = currentBuild.currentBuild as RunWrapper
        Cause causeType = runWrapper.getRawBuild().getCauses().last()
        return causeType.getClass().name
//        switch (causeType) {
//            case Cause.RemoteCause:
//                break
//            case Cause.UpstreamCause:
//                break
//            case Cause.UserIdCause:
//                break
//            case TimerTrigger.TimerTriggerCause:
//                break
//            case SCMTrigger.SCMTriggerCause:
//                break
//            case ReplayCause:
//                break
//            case RestartDeclarativePipelineCause:
//                break
//            default:
//                String errorMessage = "Don't know how to process job caused by: ${causeType}"
//                currentBuild.error(errorMessage)
//        }

    }


}
