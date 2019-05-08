#!/bin/bash

curdir=$(pwd)
RED='\033[0;51;30m'
STD='\033[0;0;39m'
APIKEY='AKCp2WWshJKjZjguhB3vD2u3RMwHA7gmxWUohWVhs1FqacHBAzKaiL2pp24NNUEhWHm5Dd4JY'

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

create() {
   echo "performing Exercise 5 (Create a Conan Package)"
   cd create
   conan new Hello/0.1
   conan create . user/testing
   conan search
   conan search Hello/0.1@user/testing
   conan create . user/testing -s build_type=Debug
   conan search Hello/0.1@user/testing
   conan new Hello/0.1 -t
   conan create . user/testing
}

create_sources() {
   echo "performing Exercise 6 (Create Package with sources)"
   cd create_sources
   conan new Hello/0.1 -t -s
   conan create . user/testing
   conan create . user/testing -s build_type=Debug
}

upload_artifactory() {
   echo "performing Exercise 7 (Upload packages to artifactory)"
   conan upload Hello/0.1@user/testing -r artifactory --all
   conan search -r=artifactory
   conan search Hello/0.1@user/testing -r=artifactory
   conan remove Hello/0.1@user/testing -f
   cd create_sources
   conan test test_package Hello/0.1@user/testing
   conan test test_package Hello/0.1@user/testing -s build_type=Debug
   conan upload "*" -r=artifactory --all --confirm
   conan remove "*" -f
   cd ../consumer/build
   conan install ..
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

read_options(){
    local choice
    cd ${curdir}
    read -p "Enter choice: " choice
    case $choice in
            2) consumer ;;
            3) consumer_debug ;;
            4) consumer_gcc ;;
            5) create ;;
            6) create_sources ;;
            7) upload_artifactory ;;
            8) cross_build_hello ;;
            9) profile_arm_compiler ;;
            11) gtest ;;
            12) gtest_build_require ;;
            13) cmake_build_require ;;
            14) package_header_only ;;
            -1) exit 0 ;;
            *) echo -e "${RED}Not valid option! ${STD}" && sleep 2
    esac
}


# function to display menus
show_menus() {
        echo "~~~~~~~~~~~~~~~~~~~~~~~~~~"
        echo " Automation Catch Up Menu "
        echo "~~~~~~~~~~~~~~~~~~~~~~~~~~"
        echo "2. Exercise 2 (Consume with CMake)"
        echo "3. Exercise 3 (Consume with CMake, with different build_type, Debug)"
        echo "4. Exercise 4 (Consume with GCC)"
        echo "5. Exercise 5 (Create a conan package)"
        echo "6. Exercise 6 (Create package with sources)"
        echo "7. Exercise 7 (Upload packages to artifactory)"
        echo "8. Exercise 8 (Cross build to ARM - RPI)"
        echo "9. Exercise 9 (Cross build zlib dependency to ARM)"
        echo "11. Exercise 11 (Use Gtest as a require)"
        echo "12. Exercise 12 (Use Gtest as a build_require)"
        echo "13. Exercise 13 (CMake as build require)"
        echo "14. Exercise 14 (Create a package for a header only library)"
        echo "-1. Exit"
}

while true
do
        show_menus
        read_options
done
