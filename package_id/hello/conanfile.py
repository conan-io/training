from conans import ConanFile


class HelloConan(ConanFile):
    name = "hello"
    exports_sources = "src/*"

    def package(self):
        self.copy("*.h", dst="include", src="src")
