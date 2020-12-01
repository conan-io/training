from conans import ConanFile, CMake

class LibB(ConanFile):
    name = "libb"
    version = "1.0"

    settings = "os", "arch", "compiler", "build_type"
    options = {"shared": [True, False]}
    default_options = {"shared": False}

    generators = "cmake"

    scm = {"type": "git",
           "url": "http://gitbucket/root/libb.git",
           "revision": "auto"}

    def requirements(self):
        self.requires("liba/1.0@ci/stable")

    def build(self):
        cmake = CMake(self)
        cmake.configure()
        cmake.build()
        cmake.install()

    def package(self):
        self.copy("LICENSE", dst="licenses")

    def package_info(self):
        self.cpp_info.libs = ["libb",]