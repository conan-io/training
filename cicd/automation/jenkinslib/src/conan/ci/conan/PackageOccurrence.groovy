package conan.ci.conan

import com.cloudbees.groovy.cps.NonCPS
import net.sf.json.JSONObject

class PackageOccurrence implements Comparable<PackageOccurrence> {
    PackageReferenceFull pRef
    Integer position
    String id

    static PackageOccurrence construct(Integer index, Tuple2<String, String> occurrence){
        PackageOccurrence po = new PackageOccurrence()
        po.position = index
        po.id = occurrence.first
        PackageReferenceString prs = new PackageReferenceString(occurrence.second)
        po.pRef = PackageReferenceFull.construct(prs)
        return po
    }

    static PackageOccurrence construct(JSONObject json){
        PackageOccurrence po = new PackageOccurrence()
        po.position = json['position'] as Integer
        po.id = json['id'] as String
        po.pRef = PackageReferenceFull.construct(json['pRef'] as JSONObject)
        return po
    }

    private PackageOccurrence() {
    }

    @NonCPS
    @Override
    int compareTo(PackageOccurrence o) {
        return this.position <=> o.position
    }
}
