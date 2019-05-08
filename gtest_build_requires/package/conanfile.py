import os
from conans import ConanFile, CMake


class HelloConan(ConanFile):
    name = "hello"
    version = "1.0"
    settings = "os", "compiler", "build_type", "arch"
    generators = "cmake"
    exports_sources = "*"

    def build(self):
        cmake = CMake(self)

        if os.environ.get("TEST_ENABLED") == "1":
            cmake.definitions["TEST_ENABLED"] = "1"

        cmake.configure()
        cmake.build()

        if os.environ.get("TEST_ENABLED") == "1":
            self.run("bin/runUnitTests")

    def package(self):
        self.copy("*.h", dst="include")
        self.copy("*.a", dst="lib", keep_path=False)

    def package_info(self):
        self.cpp_info.libs = ["hello"]
