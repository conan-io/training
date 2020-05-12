cmake_minimum_required(VERSION 3.0)
project(timer)
add_compile_options(-std=c++11)

# Using the "cmake_find_package" generator
set(CMAKE_MODULE_PATH ${CMAKE_BINARY_DIR} ${CMAKE_MODULE_PATH})
set(CMAKE_PREFIX_PATH ${CMAKE_BINARY_DIR} ${CMAKE_PREFIX_PATH})

find_package(Boost REQUIRED)
find_package(Poco REQUIRED)

add_executable(timer timer.cpp)
target_link_libraries(timer Poco::Poco Boost::Boost)
