FROM conanio/gcc7:1.30.2

USER root

RUN apt-get -qq update \
    && apt-get -qq install -y --no-install-recommends \
       vim \
       nano \
       less \
       g++-arm-linux-gnueabihf \
       cmake

USER conan

RUN git clone https://github.com/solvingj/training
WORKDIR training
RUN git checkout add_ci_cd_content
RUN chmod +x cicd/bootstrap/bootstrap_git_repos.sh
RUN conan profile new default --detect
RUN conan profile update settings.compiler.libcxx=libstdc++11 default

WORKDIR /home/conan/training