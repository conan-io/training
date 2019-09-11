cmake_minimum_required(VERSION 2.8)
project(MyHello CXX)

include(${CMAKE_BINARY_DIR}/conanbuildinfo.cmake)
conan_basic_setup()

add_library(hello hello.cpp)
target_compile_definitions(hello PRIVATE GREET_LANGUAGE=${GREET_LANGUAGE})