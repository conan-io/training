from conans import ConanFile


def mymsg(conanfile):
    print("MyTool working cool message!!! %s" % conanfile.name)

class ToolConan(ConanFile):
    name = "mytools"
    version = "0.1"

