#!/bin/bash

curdir=$(pwd)
RED='\033[0;51;30m'
STD='\033[0;0;39m'

consumer() {
    echo "performing Exercise 1 (consumer, with CMake)"
    cd consumer
    #sed -i 's/booost/boost/g' conanfile.txt
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
    #sed -i 's/cmake_find_pakcage/cmake_find_package/g' conanfile.txt
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
    conan search hello/0.1@user/testing
}

create_options_greet() {
    echo "performing Exercise 13 (Custom options: language)"
    cd create_options
    #sed -i 's/self.copy2/self.copy/g' conanfile.py
    conan create . user/testing -o greet:language=English
    conan create . user/testing -o greet:language=Spanish
}

configuration_values() {
    echo "performing Exercise 14 (Configuration values and errors)"
    cd create_options
    set +e
    conan create . user/testing -o greet:language=Italian
    set -e
    conan inspect greet/0.1@user/testing
    conan inspect zlib/1.2.11@
    conan get zlib/1.2.11@

    set +e
    conan create . user/testing -s compiler=unknown
    conan create . user/testing -s compiler.version=200
    set -e
    cat ~/.conan/settings.yml
}

cross_build_hello() {
    echo "performing Exercise 15 (Cross building hello to RPI)"
    cd cross_build
    #sed -i 's/Linus/Linux/g' rpi_armv7
    conan create . user/testing -pr=rpi_armv7
    conan search
    conan search hello/0.1@user/testing
}

requires() {
   echo "performing Exercise 16 (Transitive requires)"
   cd requires
   #sed -i 's/ZLib/zlib/g' conanfile.py
   conan create . user/testing
   set +e
   conan create . user/testing -pr=rpi_armv7
   set -e
   conan create . user/testing -pr=rpi_armv7 --build=missing
}

requires_conflict() {
   echo "performing Exercise 17 (Transitive requires conflict)"
   cd requires_conflict
   conan create lib_a user/testing
   conan create lib_b user/testing
   set +e
   conan install .
   set -e
   sed -i "s#\[requires\]#\[requires\]\nzlib/1.2.11#g" conanfile.txt
   conan install .
}

requires_conditional() {
   echo "performing Exercise 18 (Conditional requires)"
   cd requires_conditional
   conan create . user/testing -o hello:zip=False
   sed -i 's#self.requires#if self.options.zip:\n            self.requires#g' conanfile.py
   conan create . user/testing -o hello:zip=False
}

gtest_require() {
   echo "performing Exercise 19 (Requiring gtest)"
   cd gtest/hello
   #sed -i "s#import ConanFile#import ConanFile, CMake#g" conanfile.py
   conan create . user/testing
   cd ../consumer
   conan install .
}

gtest_build_require() {
   echo "performing Exercise 20 (Requiring gtest as build_require)"
   cd gtest/hello
   sed -i 's/requires =/build_requires = /g' conanfile.py
   conan create . user/testing
   cd ../consumer
   conan install .
}

cmake_build_require() {
    echo "performing Exercise 21 (cmake build_requires)"
    cd gtest/hello
    conan create . user/testing
    echo 'include(default)
[build_requires]
cmake/3.16.3' > myprofile
    cmake --version
    conan create . user/testing -pr=myprofile
    cmake --version
}

running_apps() {
   echo "performing Exercise 22 (Running apps)"
	cd running_apps
   conan install cmake/3.16.3@ -g deploy
   cmake/bin/cmake --version
   rm -rf cmake

   conan install cmake/3.16.3@ -g virtualrunenv
   cmake --version
   source activate_run.sh
   cmake --version
   source deactivate_run.sh 
   cmake --version
}

python_requires() {
    echo "performing Exercise 23 (python_requires)"
    cd python_requires/mytools
    conan export . user/testing
    cd ../consumer
    #sed -i 's/mymsg()/mymsg(self)/g' conanfile.py
    conan create . user/testing
}

version_ranges() {
    echo "performing Exercise 24 (version ranges)"
    cd version_ranges
    conan create hello hello/0.1@user/testing
    conan create chat user/testing
    sed -i 's/World/World **** 0.2 ****/g' hello/src/hello.cpp
    # generate a new hello/0.2 version
    conan create hello hello/0.2@user/testing
    # the chat package will use it because it is inside its valid range
    conan create chat user/testing
}


revisions() {
    echo "performing Exercise 25 (revisions)"
    conan config set general.revisions_enabled=True
    conan remove hello* -f
    conan remote add artifactory http://localhost:8081/artifactory/api/conan/conan-local
    cd revisions
    conan create hello user/testing
    conan upload hello* --all -r=artifactory --confirm
    sed -i 's/World/World IMPROVED/g' hello/src/hello.cpp
    conan create hello user/testing
    conan upload hello* --all -r=artifactory --confirm
    conan search hello/0.1@user/testing --revisions
    conan search hello/0.1@user/testing --revisions -r=artifactory
}

lockfiles() {
    echo "performing Exercise 26 (lockfiles)"
    cd lockfiles
    conan remove hello* -f
    conan create hello hello/0.1@user/testing
    # will generate a conan.lock file
    conan lock create chat/conanfile.py --user=user --channel=testing --lockfile-out=conan.lock
    sed -i 's/World/World **** 0.2 ****/g' hello/src/hello.cpp
    conan create hello hello/0.2@user/testing
    # NOT locked: This will use the latest 0.2
    conan create chat user/testing
    # LOCKED: the chat package will NOT use 0.2 it is locked to 0.1
    conan create chat user/testing --lockfile conan.lock
}

package_id() {
    echo "performing Exercise 27 (package_id)"
    cd package_id
    conan remove "*" -f
    conan create hello hello/1.0@user/testing
    conan create chat user/testing
    conan create app user/testing

    sed -i 's/World/World **** 1.1 ****/g' hello/src/hello.h
    conan create hello hello/1.1@user/testing
    conan create app user/testing

    conan config set general.default_package_id_mode=full_version_mode
    set +e
    conan create app user/testing
    set -e
    conan create app user/testing --build=missing
    conan search chat/1.0@user/testing
}

hooks_config_install() {
    echo "performing Exercise 28 (Hooks and conan config install)"
    conan config install myconfig
    cd hooks
    conan new Hello-Pkg/0.1 -s
    set +e
    conan export . user/testing
    set -e
    conan new hello-pkg/0.1 -s
    conan export . user/testing
    conan remove hello-pkg* -f
    sed -i "s/#TODO/if '-' in ref:\n        raise Exception('Use _ instead of -')/g" ../myconfig/hooks/check_name.py
    conan config install ../myconfig
    set +e
    conan export . user/testing
    set -e
    rm conanfile.py
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

run_option() {
    set -e

    case $1 in
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
         14) configuration_values ;;
         15) cross_build_hello ;;
         16) requires ;;
         17) requires_conflict ;;
         18) requires_conditional ;;
         19) gtest_require ;;
         20) gtest_build_require ;;
         21) cmake_build_require ;;
         22) running_apps ;;
         23) python_requires ;;
         24) version_ranges ;;    
         25) revisions ;;
         26) lockfiles ;;
         27) package_id ;;
         28) hooks_config_install ;;

         -1) exit 0 ;;
         *) echo -e "${RED}Not valid option! ${STD}" && sleep 2
    esac
}


# function to display menus
show_menu() {
    echo "~~~~~~~~~~~~~~~~~~~~~~~~~~"
    echo " Automation Catch Up Menu "
    echo "~~~~~~~~~~~~~~~~~~~~~~~~~~"
    echo "============== Conan Essentials ================="
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
    echo "14. Configuration values and errors"
    echo "15. Cross-build 'hello' pkg for RPI-armv7"
    echo "=============== Conan Advanced ==================="
    echo "16. 'hello' transitive requires 'zlib'"
    echo "17. Transitive requirements conflicts"
    echo "18. Conditional requirements"
    echo "19. requires 'gtest'"
    echo "20. build-requires 'gtest'"
    echo "21. build-requires 'cmake'"
    echo "22. Running apps"
    echo "23. python-requires"
    echo "24. Version ranges"
    echo "25. Package revisions"
    echo "26. Lockfiles"
    echo "27. package_id"
    echo "28. Hooks and conan config install"
    echo "-1. Exit"
}


if [[ $1 ]]
then
    run_option $1
else
    while true
    do
        show_menu
        cd ${curdir}
        echo -n "Enter choice: "
        read choice
        run_option $choice
    done
fi
