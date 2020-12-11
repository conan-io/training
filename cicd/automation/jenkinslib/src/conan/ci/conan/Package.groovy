package conan.ci.conan

class Package {
    def currentBuild
    RecipeInspector recipeInspector
    conan.ci.arg.ConanArgs conanArgs

    static Package construct(def currentBuild){
        Package conanPackage = new Package(currentBuild)
        conanPackage.recipeInspector = RecipeInspector.construct(currentBuild)
        conanPackage.conanArgs = new conan.ci.arg.ConanArgs(currentBuild)
        return conanPackage
    }

    private Package(def currentBuild) {
        this.currentBuild = currentBuild
    }

    PackageReferenceShort getPackageReferenceShort(){
        String name = this.recipeInspector.detectRecipeName()
        String version = this.recipeInspector.detectRecipeVersion()
        String user = this.conanArgs.conanUserCmd()
        String channel = this.conanArgs.conanChannelCmd()
        PackageReferenceShort pRef = new PackageReferenceShort(name, version, user, channel)
        return pRef
    }

}
