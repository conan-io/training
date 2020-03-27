#!/bin/bash

curdir=$(pwd)
RED='\033[0;51;30m'
STD='\033[0;0;39m'

consumer() {
   echo "performing Exercise 1 (consumer, with CMake)"
   cd consumer
   sed -i 's/booost/boost/g' conanfile.txt
   rm -rf build
   mkdir -p build
   cd build
   conan install ..
   cmake .. -DCMAKE_BUILD_TYPE=Release
   cmake --build .
   cd bin
   ./timer
   conan search
   conan search zlib/1.2.11@
}

consumer_debug() {
   echo "performing Exercise 2 (consumer, with build_type Debug)"
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
   conan search zlib/1.2.11@
}

consumer_gcc() {
   echo "performing Exercise 3 (consumer, with GCC)"
   cd consumer_gcc
   conan install . -g compiler_args
   g++ timer.cpp @conanbuildinfo.args -o timer --std=c++11
   ./timer
}

consumer_cmake_find() {
   echo "performing Exercise 4 (consumer, with cmake_find_package)"
   cd consumer_cmake_find
   sed -i 's/cmake_find_pakcage/cmake_find_package/g' conanfile.txt
   rm -rf build
   mkdir -p build
   cd build
   conan install ..
   cmake .. -DCMAKE_BUILD_TYPE=Release
   cmake --build .
   ./timer
}

create() {
   echo "performing Exercise 5 (Create a Conan Package)"
   cd create
   conan new hello/0.1
   conan create . user/testing
   conan search
   conan search hello/0.1@user/testing
   conan create . user/testing -s build_type=Debug
   conan search hello/0.1@user/testing
}

consume_hello() {
   echo "performing Exercise 6 (Consume the hello package)"
   cd consumer
   sed -i "s#\[requires\]#\[requires\]\nhello/0.1@user/testing#g" conanfile.txt
   sed -i 's/CONAN_PKG::poco/CONAN_PKG::poco CONAN_PKG::hello/g' CMakeLists.txt
   sed -i 's/TimerExample example;/TimerExample example;\nhello();/g' timer.cpp
   sed -i 's/#include <iostream>/#include <iostream>\n#include "hello.h"/g' timer.cpp
   rm -rf build
   mkdir -p build
   cd build
   conan install ..
   cmake .. -DCMAKE_BUILD_TYPE=Release
   cmake --build .
   cd bin
   ./timer
}

create_test() {
   echo "performing Exercise 7 (Create a Conan Package with test_package)"
   cd create
   conan new hello/0.1 -t
   conan create . user/testing
   conan create . user/testing -s build_type=Debug
}

create_sources() {
   echo "performing Exercise 8 (Create Package with sources)"
   cd create_sources
   conan new hello/0.1 -t -s
   conan create . user/testing
   conan create . user/testing -s build_type=Debug
}

upload_artifactory() {
   echo "performing Exercise 9 (Upload packages to artifactory)"
   conan upload hello/0.1@user/testing -r artifactory --all
   conan search -r=artifactory
   conan search hello/0.1@user/testing -r=artifactory
   conan upload "*" -r=artifactory --all --confirm
}

explore_cache() {
   echo "performing Exercise 10 (Explorer cache and remove packages)"
   ls ~/.conan
   ls ~/.conan/data
   ls ~/.conan/data/hello/0.1/user/testing
   conan remove "*" -f
   conan search
   ls ~/.conan/data
}

consume_artifactory() {
   echo "performing Exercise 11 (Consume packages from artifactory)"
   # remove everything from local cache
   conan remove "*" -f
   cd consumer/build
   conan install .. -r=artifactory
   cmake .. -DCMAKE_BUILD_TYPE=Release
   cmake --build .
   cd bin
   ./timer
}

create_options_shared() {
   echo "performing Exercise 12 (Package options: shared)"
   cd create_sources
   conan create . user/testing -o hello:shared=True
   conan create . user/testing -o hello:shared=True -s build_type=Debug
}

create_options_greet() {
   echo "performing Exercise 13 (Custom options: language)"
   cd create_options
   sed -i 's/self.copy2/self.copy/g' conanfile.py
   conan create . user/testing -o greet:language=English
   conan create . user/testing -o greet:language=Spanish
   conan create . user/testing -o greet:language=Italian
}

cross_build_hello(){
   echo "Cross building hello to RPI"
   cd cross_build
   sed -i 's/Linus/Linux/g' rpi_armv7
   conan create . user/testing -pr=rpi_armv7
   conan search
   conan search hello/0.1@user/testing
}

requires() {
   cd requires
   conan create . user/testing
   conan create . user/testing -pr=../cross_build/rpi_armv7
   conan create . user/testing -pr=../cross_build/rpi_armv7 --build=missing
}

requires_conflict() {
   cd requires_conflict
   conan create lib_a user/testing
   conan create lib_b user/testing
   sed -i "s#us\$r#user#g" conanfile.txt
   conan install .
   sed -i "s#\[requires\]#\[requires\]\nzlib/1.2.11#g" conanfile.txt
   conan install .
}

gtest_require() {
   cd gtest/package
   sed -i "s#require =#requires =#g" conanfile.py
   conan create . user/testing
   cd ../consumer
   conan install .
   conan remove "gtest*" -f
   conan install .
}

gtest_build_require() {
   cd gtest/package
   sed -i 's/requires =/build_requires = /g' conanfile.py
   conan create . user/testing
   conan remove "gtest*" -f
   cd ../consumer
   conan install .
}

cmake_build_require() {
    cd gtest/package
    conan create . user/testing
    echo 'include(default)
[build_requires]
cmake/3.16.3' > myprofile
    conan create . user/testing -pr=myprofile
}

python_requires() {
	cd python_requires/mytools
	conan export . user/testing
	cd ../consumer
	conan create . user/testing
}

hooks_config_install() {
	conan config install myconfig
	cd hooks
	conan new Hello-Pkg/0.1 -s
	conan export . user/testing
	conan new hello-pkg/0.1 -s
	conan export . user/testing
   conan remove hello-pkg* -f
   sed -i "s/#TODO/if '-' in ref:\n        raise Exception('Use _ instead of -')/g" ../myconfig/hooks/check_name.py
   conan config install ../myconfig
   conan export . user/testing
	rm conanfile.py
}

version_ranges() {
    cd version_ranges
    conan create hello1 user/testing
    conan create chat user/testing
    # generate a new hello/0.2 version
    conan create hello2 user/testing
    # the chat package will use it because it is inside its valid range
    conan create chat user/testing
}

lockfiles() {
    cd version_ranges
    conan remove hello/0.2* -f
    # will generate a conan.lock file
    conan graph lock chat
    conan create hello2 user/testing
    # This will use the 
    conan create chat user/testing
    # the chat package will NOT use 0.2 it is locked to 0.1
    conan create chat user/testing --lockfile
}

revisions() {
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

package_pico_json() {
    cd pico_json
    conan new picojson/1.3.0 -i -t
    cp example.cpp test_package

    echo 'from conans import ConanFile, tools

class PicojsonConan(ConanFile):
    name = "picojson"
    version = "1.3.0"
    no_copy_source = True
    # No settings/options are necessary, this is header only

    def source(self):
        tools.get("https://github.com/kazuho/picojson/archive/v1.3.0.zip")

    def package(self):
        self.copy("*.h", dst="include/picojson", src="picojson-1.3.0")' > conanfile.py

    conan create . user/testing
}

read_options() {
    local choice
    cd ${curdir}
    read -p "Enter choice: " choice
    case $choice in
         1) consumer ;;
         2) consumer_debug ;;
         3) consumer_gcc ;;
         4) consumer_cmake_find ;;
         5) create ;;
         6) consume_hello ;;
         7) create_test ;;
         8) create_sources ;;
         9) upload_artifactory ;;
         10) explore_cache ;;
         11) consume_artifactory ;;
         12) create_options_shared ;;
         13) create_options_greet ;;
         14) cross_build_hello ;;
         15) requires ;;
         16) requires_conflict ;;
         17) gtest_require ;;
         18) gtest_build_require ;;
         19) cmake_build_require ;;
         20) python_requires ;;
         21) hooks_config_install ;;
         22) version_ranges ;;
         23) lockfiles ;;
         24) revisions ;;
         25) package_pico_json ;;

         -1) exit 0 ;;
         *) echo -e "${RED}Not valid option! ${STD}" && sleep 2
    esac
}


# function to display menus
show_menus() {
      echo "~~~~~~~~~~~~~~~~~~~~~~~~~~"
      echo " Automation Catch Up Menu "
      echo "~~~~~~~~~~~~~~~~~~~~~~~~~~"
      echo "1. Consume with CMake"
      echo "2. Consume with CMake, with different build_type, Debug"
      echo "3. Consume with GCC"
      echo "4. Consume with CMake find_package"
      echo "5. Create a conan 'hello' package"
      echo "6. Consume the 'hello' package"
      echo "7. Create & test the 'hello' package with test_package"
      echo "8. Create a conan 'hello' package recipe in-source"
      echo "9. Upload packages to Artifactory"
      echo "10. Explore and remove packages from cache"
      echo "11. Consume packages from Artifactory"
      echo "12. Package options: shared"
      echo "13. Custom package options: language"
      echo "14. Cross-build 'hello' pkg for RPI-armv7"
      echo "15. 'hello' transitive requires 'zlib'"
      echo "16. Transitive requirements conflicts"
      echo "17. requires 'gtest'"
      echo "18. build-requires 'gtest'"
      echo "19. build-requires 'cmake'"
      echo "20. python-requires"
      echo "21. Hooks and conan config install"
      echo "22. Version ranges"
      echo "23. Lockfiles"
      echo "24. Package revisions"
      echo "25. Create a package for Pico-json"
      echo "-1. Exit"
}

while true
do
        show_menus
        read_options
done
