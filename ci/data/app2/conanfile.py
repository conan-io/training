from conans import ConanFile, CMake

class app2(ConanFile):
    name = "app2"
    version = "1.0"
    url = "https://github.com/conan-io/training"
    license = "MIT"
    description = "training"
    settings = "os", "arch", "compiler", "build_type"

    generators = "cmake"

    scm = {"type": "git",
           "url": "http://gitbucket/root/app2.git",
           "revision": "auto"}

    def requirements(self):
        self.requires("libc/1.0@ci/stable")

    def build(self):
        cmake = CMake(self)
        cmake.configure()
        cmake.build()
        cmake.install()

    def package(self):
        self.copy("LICENSE", dst="licenses")
