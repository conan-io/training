FROM docker.bintray.io/jfrog/artifactory-cpp-ce:7.6.2

COPY scripts/* /scripts/

USER root

RUN chmod +x /scripts/*

RUN chown -hR artifactory:artifactory /scripts

# Remove symlink from base image which mapped var to a VOLUME-backed directory
RUN rm /opt/jfrog/artifactory/var

USER artifactory

ENV DEMO_ART_CREDS_USR=admin
ENV DEMO_ART_CREDS_PSW=password

RUN /scripts/bootstrap-artifactory-config.sh
