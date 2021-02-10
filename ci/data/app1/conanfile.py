from conans import ConanFile, CMake

class app1(ConanFile):
    name = "app1"
    version = "1.0"
    url = "https://github.com/conan-io/training"
    license = "MIT"
    description = "training"
    settings = "os", "arch", "compiler", "build_type"

    generators = "cmake"

    scm = {"type": "git",
           "url": "http://gitbucket/root/app1.git",
           "revision": "auto"}

    def requirements(self):
        self.requires("libd/1.0@ci/stable")

    def build(self):
        cmake = CMake(self)
        cmake.configure()
        cmake.build()
        cmake.install()

    def package(self):
        self.copy("LICENSE", dst="licenses")
