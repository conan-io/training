PROJECT(MyHello)
cmake_minimum_required(VERSION 2.8)

include(${CMAKE_BINARY_DIR}/conanbuildinfo.cmake)
conan_basic_setup()

message(STATUS "CMAKE VERSION ${CMAKE_VERSION}")
add_library(hello hello.cpp)

add_executable(runUnitTests test.cpp)
target_link_libraries(runUnitTests hello ${CONAN_LIBS})
add_test(runUnitTests runUnitTests)
