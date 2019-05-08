cmake_minimum_required(VERSION 2.8)
project(BoostPoco)
add_compile_options(-std=c++11)

include(${CMAKE_BINARY_DIR}/conanbuildinfo.cmake)
conan_basic_setup()

add_executable(timer timer.cpp)
target_link_libraries(timer ${CONAN_LIBS})


# if(EXISTS ${CMAKE_BINARY_DIR}/conanbuildinfo.cmake)
#   include(${CMAKE_BINARY_DIR}/conanbuildinfo.cmake)
#    conan_basic_setup()
# endif()

# But we could use CMake find_package as well, or link with libraries individually
#find_package(Boost REQUIRED regex)

#if(Boost_FOUND)
#  include_directories(${Boost_INCLUDE_DIRS})
#  target_link_libraries(timer ${Boost_LIBRARIES})
#endif()
	
#target_link_libraries(timer ${CONAN_LIBS_POCO})
