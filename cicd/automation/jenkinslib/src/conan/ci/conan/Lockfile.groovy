package conan.ci.conan

import net.sf.json.JSONObject
import org.jenkinsci.plugins.pipeline.utility.steps.fs.FileWrapper

class Lockfile {
    def currentBuild
    String lockfilePath
    JSONObject json
    List<Node> nodes

    static Lockfile construct(Object currentBuild, FileWrapper lockfileInfo){
        return construct(currentBuild, lockfileInfo.path)
    }

    static Lockfile construct(Object currentBuild, String lockfilePath){
        Lockfile lockfile = new Lockfile(currentBuild, lockfilePath)
        lockfile.json = currentBuild.readJSON(file: lockfilePath) as JSONObject
        JSONObject nodesJson = lockfile.json['graph_lock']['nodes'] as JSONObject
        lockfile.nodes = nodesJson.collect { String id, JSONObject nodeJson -> new Node(nodeJson, id) }
        return lockfile
    }

    private Lockfile(Object currentBuild, String lockfilePath) {
        this.currentBuild = currentBuild
        this.lockfilePath = lockfilePath
    }

    String getProfile() {
        return json['profile_host']
    }

    String getRootPackageName() {
        return rootPackageReference.split("/")[0]
    }

    PackageReferenceString getRootPackageReference() {
        return new PackageReferenceString(nodes[0].pRef)
    }

    String getPath() {
        return lockfilePath
    }

    File getFile() {
        return new File(lockfilePath)
    }

    String getParent() {
        return new File(path).parent
    }

    Node getNodeForPackage(String packageRef){
        return nodes.find { Node n -> n.pRef.startsWith(packageRef) }
    }

    boolean containsPackageReference(String packageRef) {
        return nodes.any { Node n -> n.pRef.startsWith(packageRef) }
    }
}
