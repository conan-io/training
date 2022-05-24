from conans import ConanFile, CMake


class HelloConan(ConanFile):
    name = "hello"
    version = "0.1"
    settings = "os", "compiler", "build_type", "arch"
    generators = "cmake"
    exports_sources = "*"
    requires = "gtest/1.8.1"

    def build(self):
        cmake = CMake(self)
        cmake.configure()
        cmake.build()
        self.run("bin/runUnitTests")

    def package(self):
        self.copy("*.h", dst="include")
        self.copy("*.a", dst="lib", keep_path=False)

    def package_info(self):
        self.cpp_info.libs = ["hello"]
