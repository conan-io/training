cmake_minimum_required(VERSION 2.8.12)
project(conan-ci-cd-training)

include(${CMAKE_CURRENT_BINARY_DIR}/conanbuildinfo.cmake)
conan_basic_setup(TARGETS)

set(SOURCES src/libd.cpp)
file(GLOB HEADERS include/libd/*)

add_library(libd ${SOURCES} ${HEADERS})

target_link_libraries(libd PRIVATE CONAN_PKG::libb CONAN_PKG::libc)

target_include_directories(
    libd PUBLIC
    $<BUILD_INTERFACE:${CMAKE_CURRENT_SOURCE_DIR}/include/>
    $<INSTALL_INTERFACE:${CMAKE_INSTALL_INCLUDEDIR}>)

install(TARGETS libd
        RUNTIME DESTINATION ${CMAKE_INSTALL_PREFIX}/bin
        LIBRARY DESTINATION ${CMAKE_INSTALL_PREFIX}/lib
        ARCHIVE DESTINATION ${CMAKE_INSTALL_PREFIX}/lib)
install(FILES ${HEADERS}
        DESTINATION ${CMAKE_INSTALL_PREFIX}/include/libd
        PERMISSIONS OWNER_WRITE OWNER_READ GROUP_READ WORLD_READ)
