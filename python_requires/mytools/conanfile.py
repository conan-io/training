from conans import ConanFile


def mymsg():
    print("MyTool working cool message!!!")


class ToolConan(ConanFile):
    name = "mytools"
    version = "0.1"
