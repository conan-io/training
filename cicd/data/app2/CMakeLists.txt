cmake_minimum_required(VERSION 3.0.2)
project(conan-ci-cd-training)

include(${CMAKE_CURRENT_BINARY_DIR}/conanbuildinfo.cmake)
conan_basic_setup(TARGETS)

set(SOURCES src/main.cpp)

add_executable(app2 ${SOURCES})
target_link_libraries(app2 PRIVATE CONAN_PKG::libc)

install(TARGETS app2
        RUNTIME DESTINATION ${CMAKE_INSTALL_PREFIX}/bin
        LIBRARY DESTINATION ${CMAKE_INSTALL_PREFIX}/lib
        ARCHIVE DESTINATION ${CMAKE_INSTALL_PREFIX}/lib)

