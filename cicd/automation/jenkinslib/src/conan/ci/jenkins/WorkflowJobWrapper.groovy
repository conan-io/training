package conan.ci.jenkins

import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.job.WorkflowRun

class WorkflowJobWrapper {
    def currentBuild
    WorkflowJob workflowJob
    WorkflowRun workflowRunWithSameUpstream

    WorkflowJobWrapper(def currentBuild, WorkflowJob workflowJob) {
        this.currentBuild = currentBuild
        this.workflowJob = workflowJob
        workflowRunWithSameUpstream = workflowJob.builds
                .collect { WorkflowRun workflowRun ->
                    new WorkflowRunWrapper(currentBuild, workflowRun)
                }
                .find{WorkflowRunWrapper workflowRunWrapper ->
                    workflowRunWrapper.hasSameUpstreamCause
                }?.workflowRun
    }
}
