import conan.ci.step.ConanWorkflowSteps
import org.jenkinsci.plugins.workflow.cps.CpsScript

def call(Map config=null) {
    ConanWorkflowSteps.conanRebaseLockfiles(this as CpsScript, config)
}


