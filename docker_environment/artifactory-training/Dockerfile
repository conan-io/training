FROM docker.bintray.io/jfrog/artifactory-cpp-ce:7.6.2

COPY scripts/* /scripts/

USER root

#Set executable permission
RUN chmod +x /scripts/*

#Remove windows EOL character
RUN sed -i "s/\r//g" /scripts/*

RUN chown -hR artifactory:artifactory /scripts

# Remove symlink from base image which mapped var to a VOLUME-backed directory
RUN rm /opt/jfrog/artifactory/var

USER artifactory

ENV DEMO_ART_CREDS_USR=admin
ENV DEMO_ART_CREDS_PSW=password

RUN /scripts/bootstrap-artifactory-config.sh
