from conans import ConanFile, CMake


class LibAConan(ConanFile):
    name = "lib_a"
    version = "0.1"
    requires = "zlib/1.2.11"
