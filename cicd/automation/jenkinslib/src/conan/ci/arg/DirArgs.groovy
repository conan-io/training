package conan.ci.arg

class DirArgs {
    def currentBuild
    def env
    private String _workingDir
    private String _workingSubDir

    DirArgs(def currentBuild, Map config = [:]) {
        this.currentBuild = currentBuild
        this.env = currentBuild.env
        if (config?.workingDir) {
            _workingDir = config.workingDir
        }
        if (config?.workingSubDir) {
            _workingSubDir = config.workingSubDir
        }
    }

    String getWorkingDir() {
        return _workingDir ?: env["DEMO_WORKING_DIR"] ?: ""
    }

    String getWorkingSubDir() {
        return _workingSubDir ?: env["DEMO_WORKING_SUBDIR"] ?: ""
    }

    String getFullWorkingDir() {
        String fullWorkingDir
        if (getWorkingDir()) {
            if (getWorkingSubDir()) {
                fullWorkingDir = "${getWorkingDir()}/${getWorkingSubDir()}"
            } else {
                fullWorkingDir = getWorkingDir()
            }
        } else {
            fullWorkingDir = "."
        }
        return fullWorkingDir
    }


}
