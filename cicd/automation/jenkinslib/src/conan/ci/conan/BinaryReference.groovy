package conan.ci.conan

import net.sf.json.JSONObject

class BinaryReference {
    String pId, pRev

    BinaryReference(String pId, String pRev) {
        this.pId = pId
        this.pRev = pRev
    }

    BinaryReference(JSONObject json){
        this.pId = json['pId']
        this.pRev = json['pRev']
    }
}
