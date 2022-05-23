ARG CONAN_VERSION

FROM conanio/gcc10:${CONAN_VERSION}

RUN sudo apt-get -qq update \
    && sudo apt-get -qq install -y --no-install-recommends \
       vim \
       nano \
       less \
       g++-arm-linux-gnueabihf \
       cmake

RUN git clone https://github.com/conan-io/training
RUN conan profile new default --detect --force
RUN conan profile update settings.compiler.libcxx=libstdc++11 default

WORKDIR /home/conan/training
