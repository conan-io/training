package conan.ci.conan

import com.cloudbees.groovy.cps.NonCPS

class PackageReferenceString {
    String reference

    PackageReferenceString(String reference) {
        this.reference = reference
    }

    @NonCPS
    List<String> toListString() {
        return this.reference.split("[:/@#]")
    }
}
