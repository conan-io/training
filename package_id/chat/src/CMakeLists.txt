cmake_minimum_required(VERSION 2.8)
project(MyHello CXX)

include(${CMAKE_BINARY_DIR}/conanbuildinfo.cmake)
conan_basic_setup()

add_library(chat chat.cpp)
target_link_libraries(chat PRIVATE ${CONAN_LIBS})
