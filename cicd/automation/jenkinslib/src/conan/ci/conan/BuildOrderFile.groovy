package conan.ci.conan

import net.sf.json.JSONArray
import org.jenkinsci.plugins.pipeline.utility.steps.fs.FileWrapper
class BuildOrderFile {
    def currentBuild
    FileWrapper fileWrapper
    JSONArray json

    static BuildOrderFile construct(def currentBuild, FileWrapper fileWrapper) {
        BuildOrderFile buildOrderFile = new BuildOrderFile(currentBuild)
        buildOrderFile.json = currentBuild.readJSON(file: fileWrapper.path) as JSONArray
        buildOrderFile.fileWrapper = fileWrapper
        return buildOrderFile
    }

    private BuildOrderFile(def currentBuild) {
        this.currentBuild = currentBuild
    }

}