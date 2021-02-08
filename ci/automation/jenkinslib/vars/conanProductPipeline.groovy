import conan.ci.step.ConanWorkflowSteps
import org.jenkinsci.plugins.workflow.cps.CpsScript

def call(Map config=null) {
    ConanWorkflowSteps.conanProductPipeline(this as CpsScript, config)
}


