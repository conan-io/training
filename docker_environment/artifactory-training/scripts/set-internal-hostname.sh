#!/bin/bash
set -eo pipefail

curl -m 5 -f -X PATCH \
-u ${TRAINING_ART_CREDS_USR}:${TRAINING_ART_CREDS_PSW} \
-H 'Content-Type: application/yaml' \
http://localhost:8082/artifactory/api/system/configuration \
-d '
urlBase: http://jfrog-artifactory-training:8082
localRepositories: #Local repositories configuration
    conan-develop: #The local repository name
        type: conan #The package type
    conan-temp: #The local repository name
        type: conan #The package type
'
# we create the repositories here for the ci/cd training
# but it also 