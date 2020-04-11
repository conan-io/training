from conans import ConanFile, CMake


class AppConan(ConanFile):
    name = "app"
    version = "0.1"
    settings = "os", "compiler", "build_type", "arch"
    generators = "cmake"
    requires = "chat/1.0@user/testing"
    exports_sources = "src/*"

    def build(self):
        cmake = CMake(self)
        cmake.configure(source_folder="src")
        cmake.build()
        self.run("bin/app")
