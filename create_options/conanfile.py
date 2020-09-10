from conans import ConanFile, CMake


class GreetConan(ConanFile):
    name = "greet"
    version = "0.1"
    settings = "os", "compiler", "build_type", "arch"
    options = {"language": ["English", "Spanish"]}
    default_options = {"language": "English"}
    generators = "cmake"
    exports_sources = "src/*"

    def build(self):
        cmake = CMake(self)
        if self.options.language == "English":
            cmake.definitions["GREET_LANGUAGE"] = 1
        else:
            cmake.definitions["GREET_LANGUAGE"] = 0
        cmake.configure(source_folder="src")
        cmake.build()

    def package(self):
        self.copy("*.h", dst="include", src="src")
        self.copy("*.lib", dst="lib", keep_path=False)
        self.copy("*.dll", dst="bin", keep_path=False)
        self.copy("*.dylib*", dst="lib", keep_path=False)
        self.copy("*.so", dst="lib", keep_path=False)
        self.copy("*.a", dst="lib", keep_path=False)

    def package_info(self):
        self.cpp_info.libs = ["hello"]
