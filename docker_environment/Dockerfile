FROM conanio/gcc7:1.28.1

USER root

RUN apt-get -qq update \
    && apt-get -qq install -y --no-install-recommends \
       vim \
       nano \
       less \
       g++-arm-linux-gnueabihf

RUN wget --no-check-certificate --quiet https://cmake.org/files/v3.7/cmake-3.7.2-Linux-x86_64.tar.gz \
    && tar -xzf cmake-3.7.2-Linux-x86_64.tar.gz \
       --exclude=bin/cmake-gui \
       --exclude=doc/cmake \
       --exclude=share/cmake-3.12/Help \
    && cp -fR cmake-3.7.2-Linux-x86_64/* /usr \
    && rm -rf cmake-3.7.2-Linux-x86_64 \
    && rm cmake-3.7.2-Linux-x86_64.tar.gz

USER conan

RUN git clone https://github.com/conan-io/training
RUN conan profile new default --detect
RUN conan profile update settings.compiler.libcxx=libstdc++11 default

WORKDIR /home/conan/training