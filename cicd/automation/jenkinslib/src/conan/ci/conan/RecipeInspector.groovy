package conan.ci.conan


import conan.ci.docker.DockerContainerInspector
import conan.ci.runner.CommandRunnerSelector
class RecipeInspector {
    def currentBuild
    String detectedWorkingDir
    String fullWorkingDir
    conan.ci.arg.ConanArgs conanArgs
    def commandRunner

    static RecipeInspector construct(def currentBuild) {
        RecipeInspector cri = new RecipeInspector(currentBuild)
        cri.commandRunner = CommandRunnerSelector.select(currentBuild)
        cri.detectedWorkingDir = new DockerContainerInspector(currentBuild).detectWorkingDirectory()
        cri.fullWorkingDir = new conan.ci.arg.DirArgs(currentBuild, [workingDirectory: cri.detectedWorkingDir]).fullWorkingDir
        cri.conanArgs = new conan.ci.arg.ConanArgs(currentBuild)
        return cri
    }

    RecipeInspector(def currentBuild) {
        this.currentBuild = currentBuild
    }

    String detectRecipeName(String conanfileDir = "") {
        String command = this.conanArgs.getInspectAttributeCommand("name", conanfileDir ?: conanArgs.conanfileDir)
        String recipeName = this.commandRunner.run(command, fullWorkingDir)
        if (recipeName) {
            return recipeName.trim()
        } else {
            return recipeName
        }
    }

    String detectRecipeVersion(String conanfileDir = "") {
        String command = this.conanArgs.getInspectAttributeCommand("version", conanfileDir ?: conanArgs.conanfileDir)
        String recipeVersion = this.commandRunner.run(command, fullWorkingDir)
        if (recipeVersion) {
            return recipeVersion.trim()
        } else {
            return recipeVersion
        }
    }

}
