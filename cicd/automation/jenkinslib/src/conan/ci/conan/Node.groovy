package conan.ci.conan

import com.cloudbees.groovy.cps.NonCPS
import net.sf.json.JSONObject

class Node implements Comparable<Node> {
    String pRef
    String options
    String path
    Integer id
    List<Integer> requires

    Node(JSONObject json, String id) {
        this.pRef = json['pref'] as String
        this.options = json['options'] as String
        this.path = json['path'] as String
        this.id = id.toInteger()
        this.requires = json['requires'].collect{String requiresId -> requiresId.toInteger()}
    }

    @NonCPS
    @Override
    int compareTo(Node o) {
        if (this.id == o.id)
            return 0;
        else if (this.id > o.id)
            return 1;
        else
            return -1;
    }
}
