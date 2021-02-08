import conan.ci.condition.ConanConditions
import org.jenkinsci.plugins.workflow.cps.CpsScript

def call(Map config=null) {
    ConanConditions.conanLockfilesNeedRebase(this as CpsScript, config)
}


