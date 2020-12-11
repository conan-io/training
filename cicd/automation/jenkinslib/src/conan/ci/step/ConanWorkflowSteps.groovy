package conan.ci.step

import conan.ci.step.workflow.ConanPackagePipeline

class ConanWorkflowSteps {

    static void conanPackagePipeline(Object currentBuild) {
        new ConanPackagePipeline(currentBuild).run()
    }

}
