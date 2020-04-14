cmake_minimum_required(VERSION 2.8)
project(MyHello CXX)

include(${CMAKE_BINARY_DIR}/conanbuildinfo.cmake)
conan_basic_setup()

option(WITH_ZIP "Use zlib library")

add_library(hello hello.cpp)
if(WITH_ZIP)
   message("Add WITH_ZIP definition")
   target_compile_definitions(hello PRIVATE WITH_ZIP)
endif()
