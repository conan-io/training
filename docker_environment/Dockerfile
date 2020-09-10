FROM conanio/gcc7:1.28.1

USER root

RUN apt-get -qq update \
    && apt-get -qq install -y --no-install-recommends \
       vim \
       nano

RUN git clone https://github.com/conan-io/training

USER conan

WORKDIR /home/conan/training