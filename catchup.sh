#!/bin/bash

curdir=$(pwd)
RED='\033[0;51;30m'
STD='\033[0;0;39m'

consumer() {
   echo "performing Exercise 2 (consumer, with CMake)"
   cd consumer
   rm -rf build
   mkdir -p build
   cd build
   conan install ..
   cmake .. -DCMAKE_BUILD_TYPE=Release
   cmake --build .
   cd bin
   ./timer
   conan search
   conan search zlib/1.2.11@conan/stable
}

consumer_debug() {
   echo "performing Exercise 3 (consumer, with build_type Debug)"
   cd consumer
   rm -rf build
   mkdir -p build
   cd build
   conan install .. -s build_type=Debug
   cmake .. -DCMAKE_BUILD_TYPE=Debug
   cmake --build .
   cd bin
   ./timer
   conan search
   conan search zlib/1.2.11@conan/stable
}

consumer_gcc() {
   echo "performing Exercise 4 (consumer, with GCC)"
   cd consumer_gcc
   conan install . -g gcc
   g++ timer.cpp @conanbuildinfo.gcc -o timer --std=c++11
   ./timer
}

consumer_cmake_modern() {
   echo "performing Exercise 5 (consumer, with CMake modern)"
   cd consumer
   sed -i 's/conan_basic_setup()/conan_basic_setup(NO_OUTPUT_DIRS TARGETS)/g' CMakeLists.txt
   sed -i 's/${CONAN_LIBS}/CONAN_PKG::Poco CONAN_PKG::boost/g' CMakeLists.txt
   rm -rf build
   mkdir -p build
   cd build
   conan install ..
   cmake .. -DCMAKE_BUILD_TYPE=Release
   cmake --build .
   ./timer
}

consumer_cmake_find() {
   echo "performing Exercise 6 (consumer, with cmake_find_package)"
   cd consumer_cmake_find
   rm -rf build
   mkdir -p build
   cd build
   conan install .. -g cmake_find_package
   cmake .. -DCMAKE_BUILD_TYPE=Release
   cmake --build .
   ./timer
}

create() {
   echo "performing Exercise 7 (Create a Conan Package)"
   cd create
   conan new hello/0.1
   conan create . user/testing
   conan search
   conan search hello/0.1@user/testing
   conan create . user/testing -s build_type=Debug
   conan search hello/0.1@user/testing
}

consume_hello() {
   echo "performing Exercise 8 (Consume the hello package)"
   cd consumer
   sed -i "s#\[requires\]#\[requires\]\nhello/0.1@user/testing#g" conanfile.txt
   sed -i 's/CONAN_PKG::Poco/CONAN_PKG::Poco CONAN_PKG::hello/g' CMakeLists.txt
   sed -i 's/TimerExample example;/TimerExample example;\nhello();/g' timer.cpp
   sed -i 's/#include <iostream>/#include <iostream>\n#include "hello.h"/g' timer.cpp
   rm -rf build
   mkdir -p build
   cd build
   conan install ..
   cmake .. -DCMAKE_BUILD_TYPE=Release
   cmake --build .
   ./timer
}

create_test() {
   echo "performing Exercise 9 (Create a Conan Package)"
   cd create
   conan new hello/0.1 -t
   conan create . user/testing
   conan create . user/testing -s build_type=Debug
}

create_sources() {
   echo "performing Exercise 10 (Create Package with sources)"
   cd create_sources
   conan new hello/0.1 -t -s
   conan create . user/testing
   conan create . user/testing -s build_type=Debug
}

upload_artifactory() {
   echo "performing Exercise 11 (Upload packages to artifactory)"
   conan upload hello/0.1@user/testing -r artifactory --all
   conan search -r=artifactory
   conan search hello/0.1@user/testing -r=artifactory
   conan upload "*" -r=artifactory --all --confirm
}

consume_artifactory() {
   echo "performing Exercise 12 (Consume packages from artifactory)"
   # remove everything from local cache
   conan remove "*" -f
   cd ../consumer/build
   conan install .. -r=artifactory
   cmake .. -DCMAKE_BUILD_TYPE=Release
   cmake --build .
   ./timer
}

test_artifactory() {
   echo "performing Exercise 13 (conan test command)"
   cd create_sources
   conan test test_package hello/0.1@user/testing
   conan test test_package hello/0.1@user/testing -s build_type=Debug
}

cross_build_hello(){
   cd create_sources
   conan create . user/testing -pr=../profile_arm/arm_gcc_debug.profile
   conan search
   conan search Hello/0.1@user/testing
}

profile_arm_compiler() {
   cd profile_arm
   rm -rf build
   mkdir -p build
   cd build
   conan install .. --profile ../arm_gcc_debug.profile
   conan install .. -pr=../arm_gcc_debug.profile --build missing
   conan search zlib/1.2.11@conan/stable
   conan build ..
   ls bin/example && echo "Example built ok!"
}

package_header_only(){
    cd header_only
    conan new picojson/1.3.0 -i -t
    cp example.cpp test_package

    echo 'from conans import ConanFile

class PicojsonConan(ConanFile):
    name = "picojson"
    version = "1.3.0"
    license = "The 2-Clause BSD License"
    url = "https://github.com/kazuho/picojson"
    # No settings/options are necessary, this is header only

    def source(self):
        self.run("git clone https://github.com/kazuho/picojson.git")

    def package(self):
        self.copy("*.h", "include")' > conanfile.py

    conan create . user/testing
}

gtest() {
    conan remote add conan-center https://conan.bintray.com
    cd gtest/package
    conan create . user/testing
    cd ../consumer
    conan install .
    conan remove "gtest*" -f
    conan install .
}

gtest_build_require() {
    cd gtest/package
    conan create . user/testing
    conan remove "gtest*" -f
    cd ../consumer
    conan install .
}

cmake_build_require() {
    cd gtest/package
    echo 'message(STATUS "CMAKE VERSION ${CMAKE_VERSION}")' >> CMakeLists.txt
    conan create . user/testing
    echo 'include(default)
[build_requires]
cmake_installer/3.3.2@conan/stable' > myprofile
    conan create . user/testing -pr=myprofile
}

python_requires(){
	cd python_requires/mytools
	conan export . user/testing
	cd ../consumer
	conan create . user/testing
}

hooks(){
	conan config install hooks
	cd hooks
	conan new Hello/0.1
	conan create . user/testing
	rm conanfile.py
}

version_ranges(){
    mkdir version_ranges && cd version_ranges
    conan remove Hello* -f
    conan new hello/0.1 -s
    conan create . user/testing
    conan install "hello/[>0.0 <1.0]@user/testing"
    conan new hello/0.2 -s
    conan create . user/testing
    conan search
    conan install "hello/[>0.0 <1.0]@user/testing"
}

revisions(){                                          
       mkdir revisions && cd revisions
       conan remove hello* -f
       conan new hello/0.1 -s                         
       conan config set general.revisions_enabled=True
       conan create . user/testing                    
       conan create . user/testing -s build_type=Debug
       conan upload hello* --all -r=artifactory --confirm
       echo "#comment" >> conanfile.py                
       conan create . user/testing                    
       conan create . user/testing -s build_type=Debug
       conan upload hello* --all -r=artifactory --confirm
       conan search hello/0.1@user/testing        
}                                                     

read_options(){
    local choice
    cd ${curdir}
    read -p "Enter choice: " choice
    case $choice in
            2) consumer ;;
            3) consumer_debug ;;
            4) consumer_gcc ;;
            5) consumer_cmake_modern ;;
            6) consumer_cmake_find ;;
            7) create ;;
            8) consume_hello ;;
            9) create_test ;;
            10) create_sources ;;
            11) upload_artifactory ;;
            12) consume_artifactory ;;
            13) test_artifactory ;;
            
            -1) exit 0 ;;
            *) echo -e "${RED}Not valid option! ${STD}" && sleep 2
    esac
}


# function to display menus
show_menus() {
        echo "~~~~~~~~~~~~~~~~~~~~~~~~~~"
        echo " Automation Catch Up Menu "
        echo "~~~~~~~~~~~~~~~~~~~~~~~~~~"
        echo "2. Consume with CMake"
        echo "3. Consume with CMake, with different build_type, Debug"
        echo "4. Consume with GCC"
        echo "5. Consume with CMake, modern targets"
        echo "6. Consume with CMake find_package"
        echo "7. Create a conan 'hello' package"
        echo "8. Consume the 'hello' package"
        echo "9. Create & test the 'hello' package with test_package"
        echo "10. Create a conan 'hello' package recipe in-source"
        echo "11. Upload packages to Artifactory"
        echo "12. Consume packages from Artifactory"
        echo "13. Test packages with 'conan test'"

        echo "8. Cross build to ARM - RPI"
        echo "9. Cross build zlib dependency to ARM"
        echo "10. Use Gtest as a require"
        echo "11. Use Gtest as a build_require"
        echo "12. CMake as build require"
        echo "13. Create a package for a header only library"
	    echo "14. Python requires"
	    echo "15. Hooks"
        echo "16. Version ranges"
        echo "17. Revisions"
        echo "-1. Exit"
}

while true
do
        show_menus
        read_options
done
