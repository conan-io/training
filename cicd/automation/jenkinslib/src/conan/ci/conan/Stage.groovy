package conan.ci.conan

class Stage {

    def currentBuild
    String fullPackageRef
    Lockfile lockfile
    String dockerImage

    Stage(def currentBuild, String fullPackageRef, Lockfile lockfile, String dockerImage) {
        this.currentBuild = currentBuild
        this.fullPackageRef = fullPackageRef
        this.lockfile = lockfile
        this.dockerImage = dockerImage
    }

    String getPackageId (){
        return this.fullPackageRef.split(":")[1].split("#")[0]
    }

}
