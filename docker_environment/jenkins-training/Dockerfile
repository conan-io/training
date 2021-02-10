FROM jenkins/jenkins:2.263.1-lts-centos

USER root

RUN rm -f /etc/yum.repos.d/github_git-lfs.repo

RUN yum-config-manager \
    --add-repo \
    https://download.docker.com/linux/centos/docker-ce.repo

RUN yum -y update \
    && yum install -y \
        curl \
        git \
        docker-ce \
        docker-ce-cli \
        containerd.io

RUN jenkins-plugin-cli --verbose --plugins \
    configuration-as-code \
    credentials-binding \
    pipeline-model-definition \
    job-dsl \
    cloudbees-folder \
    strict-crumb-issuer \
    copyartifact \
    docker-workflow \
    git-client \
    git-parameter \
    git-changelog \
    github \
    github-api \
    github-branch-source \
    ws-cleanup \
    pipeline-utility-steps \
    http_request \
    pipeline-build-step \
    blueocean

COPY configs/* /home/jenkins/configs/

ENV JAVA_OPTS=-Djenkins.install.runSetupWizard=false
ENV CASC_JENKINS_CONFIG=/home/jenkins/configs

