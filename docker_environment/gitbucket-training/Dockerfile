FROM gitbucket/gitbucket:nightly

RUN apt-get -qq update \
    && apt-get -qq install -y --no-install-recommends \
       git

ENV DEMO_GIT_CREDS_USR=root
ENV DEMO_GIT_CREDS_PSW=root
ENV GITBUCKET_HOME=/gitbucket-data
ENV GITBUCKET_BASE_URL=http://gitbucket
ENV GITBUCKET_PORT=80
EXPOSE 80

RUN mkdir -p $GITBUCKET_HOME
