package conan.ci.conan

import com.cloudbees.groovy.cps.NonCPS
import net.sf.json.JSONObject

class PackageReferenceFull {
    PackageReferenceShort pRefShort
    BinaryReference bRef
    String rRev

    String getName() { return pRefShort.name }

    String getVersion() { return pRefShort.version }

    String getUser() { return pRefShort.user }

    String getChannel() { return pRefShort.channel }

    String getPid() { return bRef.pId }

    String getPrev() { return bRef.pRev }

    @NonCPS
    static PackageReferenceFull construct(PackageReferenceString reference) {
        return new PackageReferenceFull(*reference.toListString())
    }

    static PackageReferenceFull construct(URI uri) {
        return new PackageReferenceFull(uri)
    }

    static PackageReferenceFull construct(JSONObject json) {
        PackageReferenceFull pRefFull = new PackageReferenceFull()
        pRefFull.rRev = json['rRev'] as String
        pRefFull.bRef = new BinaryReference(json['bRef'] as JSONObject)
        pRefFull.pRefShort = new PackageReferenceShort(json['pRefShort'] as JSONObject)
        return pRefFull
    }

    private PackageReferenceFull() {
    }

    private PackageReferenceFull(URI uri) {
        def (user, name, version, channel, rRev, _, pId, pRev) = uri.toString().split("/")
        this.rRev = rRev
        this.pRefShort = new PackageReferenceShort(name, version, user, channel)
        this.bRef = new BinaryReference(pId, pRev)
    }

    // Do not delete, this is actually used, intellisense construct() method
    private PackageReferenceFull(
            String name, String version, String user, String channel, String rRev, String pId, String pRev=""
    ) {
        this.rRev = rRev
        this.pRefShort = new PackageReferenceShort(name, version, user, channel)
        this.bRef = new BinaryReference(pId, pRev)
    }

    String getArtifactoryPath() {
        String path = "${user}/${name}/${version}/${channel}/${rRev}/package/${pid}"
        if(prev){
            path += ("/${prev}")
        }
        return path
    }

    String toString(){
        String ref = "${name}/${version}@${user}/${channel}#${rRev}:${pid}"
        if(prev){
            ref.concat("#${prev}")
        }
        return ref
    }
}