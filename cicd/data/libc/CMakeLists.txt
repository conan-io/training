cmake_minimum_required(VERSION 2.8.12)
project(conan-ci-cd-training)

include(${CMAKE_CURRENT_BINARY_DIR}/conanbuildinfo.cmake)
conan_basic_setup(TARGETS)

set(SOURCES src/libc.cpp)
file(GLOB HEADERS include/libc/*)

add_library(libc ${SOURCES} ${HEADERS})

target_link_libraries(libc PRIVATE CONAN_PKG::liba)

target_include_directories(
    libc PUBLIC
    $<BUILD_INTERFACE:${CMAKE_CURRENT_SOURCE_DIR}/include/>
    $<INSTALL_INTERFACE:${CMAKE_INSTALL_INCLUDEDIR}>)

install(TARGETS libc
        RUNTIME DESTINATION ${CMAKE_INSTALL_PREFIX}/bin
        LIBRARY DESTINATION ${CMAKE_INSTALL_PREFIX}/lib
        ARCHIVE DESTINATION ${CMAKE_INSTALL_PREFIX}/lib)
install(FILES ${HEADERS}
        DESTINATION ${CMAKE_INSTALL_PREFIX}/include/libc
        PERMISSIONS OWNER_WRITE OWNER_READ GROUP_READ WORLD_READ)
