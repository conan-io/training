import conan.ci.condition.ConanConditions
import org.jenkinsci.plugins.workflow.cps.CpsScript

def call(Map config=null) {
    ConanConditions.conanBuildOrderHasSteps(this as CpsScript, config)
}


