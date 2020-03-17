from conans import ConanFile, CMake


class LibBConan(ConanFile):
    name = "lib_b"
    version = "0.1"
    requires = "zlib/1.2.8"
