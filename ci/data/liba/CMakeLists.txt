cmake_minimum_required(VERSION 3.0.2)
project(conan-ci-cd-training)

include(${CMAKE_CURRENT_BINARY_DIR}/conanbuildinfo.cmake)
conan_basic_setup(TARGETS)

set(SOURCES src/liba.cpp)
file(GLOB HEADERS include/liba/*)

add_library(liba ${SOURCES} ${HEADERS})

target_include_directories(
    liba PUBLIC
    $<BUILD_INTERFACE:${CMAKE_CURRENT_SOURCE_DIR}/include/>
    $<INSTALL_INTERFACE:${CMAKE_INSTALL_INCLUDEDIR}>)

install(TARGETS liba
        RUNTIME DESTINATION ${CMAKE_INSTALL_PREFIX}/bin
        LIBRARY DESTINATION ${CMAKE_INSTALL_PREFIX}/lib
        ARCHIVE DESTINATION ${CMAKE_INSTALL_PREFIX}/lib)
install(FILES ${HEADERS}
        DESTINATION ${CMAKE_INSTALL_PREFIX}/include/liba
        PERMISSIONS OWNER_WRITE OWNER_READ GROUP_READ WORLD_READ)
