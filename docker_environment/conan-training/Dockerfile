FROM conanio/gcc7:1.42.1

USER root

RUN apt-get -qq update \
    && apt-get -qq install -y --no-install-recommends \
       vim \
       nano \
       less \
       g++-arm-linux-gnueabihf \
       cmake

USER conan

RUN git clone https://github.com/conan-io/training
RUN conan profile new default --detect
RUN conan profile update settings.compiler.libcxx=libstdc++11 default

WORKDIR /home/conan/training
