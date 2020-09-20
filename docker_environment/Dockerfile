FROM conanio/gcc7:1.28.1

USER root

RUN apt-get -qq update \
    && apt-get -qq install -y --no-install-recommends \
       vim \
       nano \
       less \
       g++-arm-linux-gnueabihf

USER conan

RUN git clone https://github.com/conan-io/training

WORKDIR /home/conan/training