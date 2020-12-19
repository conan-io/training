FROM docker.bintray.io/jfrog/artifactory-cpp-ce:7.6.2

COPY scripts/* /scripts/

USER root

#Set executable permission
RUN chmod +x /scripts/*

#Remove windows EOL character
RUN sed -i "s/\r//g" /scripts/*

RUN chown -hR artifactory:artifactory /scripts

USER artifactory

ENV TRAINING_ART_CREDS_USR=admin
ENV TRAINING_ART_CREDS_PSW=password

RUN /scripts/bootstrap-artifactory-config.sh
