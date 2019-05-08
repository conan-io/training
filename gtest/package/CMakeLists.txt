PROJECT(MyHello)
cmake_minimum_required(VERSION 2.8)

include(${CMAKE_BINARY_DIR}/conanbuildinfo.cmake)
conan_basic_setup()
    
add_library(hello STATIC hello.cpp)

add_executable(runUnitTests test.cpp)
target_link_libraries(runUnitTests hello ${CONAN_LIBS})
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++0x")
add_test(runUnitTests runUnitTests)
