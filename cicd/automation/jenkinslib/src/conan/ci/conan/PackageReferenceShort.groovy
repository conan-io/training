package conan.ci.conan

import net.sf.json.JSONObject

class PackageReferenceShort {
    String name, version, user, channel

    PackageReferenceShort(String name, String version, String user, String channel) {
        this.name = name
        this.version = version
        this.user = user
        this.channel = channel
    }

    PackageReferenceShort(JSONObject json) {
        this.name = json['name']
        this.version = json['version']
        this.user = json['user']
        this.channel = json['channel']
    }

    String toString(){
        List<String> ref = []
        ref.add(name)
        ref.add("/")
        ref.add(version)
        ref.add("@")
        if(user && channel){
            ref.add(user)
            ref.add("/")
            ref.add(channel)
        }
        return ref.join("")
    }

}