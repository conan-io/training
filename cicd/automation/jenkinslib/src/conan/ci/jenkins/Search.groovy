package conan.ci.jenkins

import hudson.model.Result
import jenkins.model.Jenkins
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.job.WorkflowRun

class Search {

    def currentBuild
    String currentJobName
    List<String> siblingJobNames

    Search(def currentBuild, String currentJobName, List<String> siblingJobNames) {
        this.currentBuild = currentBuild
        this.currentJobName = currentJobName
        this.siblingJobNames = siblingJobNames
    }

    List<String> getRelatedSuccessfulBuildNames() {
        List<String> builds = getRelatedBuilds()
                .findAll { WorkflowRun workflowRun -> workflowRun.result == Result.SUCCESS }
                .collect { WorkflowRun workflowRun -> workflowRun.parent.fullName }
        return builds
    }

    List<WorkflowRun> getRelatedBuilds() {
        List<WorkflowJob> allJobs = Jenkins.instance.getAllItems(WorkflowJob)
        List<WorkflowJob> siblingJobs = intersectJobsAndProjectNames(allJobs)

        List<WorkflowRun> siblingBuilds = siblingJobs.collect { WorkflowJob workflowJob ->
            new WorkflowJobWrapper(currentBuild, workflowJob)
        }
        .findAll{WorkflowJobWrapper workflowJobWrapper -> workflowJobWrapper.workflowRunWithSameUpstream}
        .collect{WorkflowJobWrapper workflowJobWrapper -> workflowJobWrapper.workflowRunWithSameUpstream}
        return siblingBuilds
    }

    List<WorkflowJob> intersectJobsAndProjectNames(List<WorkflowJob> workflowJobs) {
        List<WorkflowJob> selectedJobs = siblingJobNames.collectMany { String projectName ->
            return workflowJobs.findAll { WorkflowJob workflowJob -> workflowJob.fullName.endsWith(projectName) }
        }
        return selectedJobs
    }

}
