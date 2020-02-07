from conans import ConanFile, python_requires

mytools = python_requires("mytools/0.1@user/testing")

class ConsumerConan(ConanFile):
    name = "consumer"
    version = "0.1"
    settings = "os", "compiler", "build_type", "arch"

    def build(self):
        mytools.mymsg(self)
