from conans import ConanFile, CMake

class libc(ConanFile):
    name = "libc"
    version = "1.0"
    url = "https://github.com/conan-io/training"
    license = "MIT"
    description = "training"
    settings = "os", "arch", "compiler", "build_type"
    options = {"shared": [True, False]}
    default_options = {"shared": False}

    generators = "cmake"

    scm = {"type": "git",
           "url": "http://gitbucket/root/libc.git",
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
        self.cpp_info.libs = ["libc",]

