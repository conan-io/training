package conan.ci.jenkins

import hudson.model.Cause
import org.jenkinsci.plugins.workflow.cps.CpsScript
import org.jenkinsci.plugins.workflow.job.WorkflowRun

class WorkflowRunWrapper {
    WorkflowRun workflowRun
    CauseWrapper causeEvaluator
    boolean hasSameUpstreamCause

    WorkflowRunWrapper(CpsScript currentBuild, WorkflowRun workflowRun) {
        this.workflowRun = workflowRun
        this.causeEvaluator = new CauseWrapper(currentBuild)

        Cause.UpstreamCause firstUpstreamCause = workflowRun.causes.find {
            Cause cause -> cause instanceof Cause.UpstreamCause
        } as Cause.UpstreamCause

        if (firstUpstreamCause) {
            this.hasSameUpstreamCause = (
                    firstUpstreamCause.upstreamRun.url == causeEvaluator.firstUpstreamCause.upstreamRun.url
            )
        } else {
            this.hasSameUpstreamCause = false
        }
    }
}
