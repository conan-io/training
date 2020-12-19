#!/bin/bash

TRAINING_GIT_CREDS_USR=${TRAINING_GIT_CREDS_USR:-root}
TRAINING_GIT_CREDS_PSW=${TRAINING_GIT_CREDS_PSW:-root}
TRAINING_GIT_URL=${TRAINING_GIT_SERVER:-http://$TRAINING_GIT_CREDS_USR:$TRAINING_GIT_CREDS_PSW@gitbucket}
TRAINING_JENKINS_URL=${TRAINING_JENKINS_URL:-http://jenkins:8080}

git config --global user.email "you@example.com"
git config --global user.name "Your Name"

JENKINSLIB=~/training/cicd/automation/jenkinslib
SCRIPTS=~/training/cicd/automation/scripts
DATA=~/training/cicd/data

initialize_repo(){
    echo "--- creating GIT repository on server: ${1}"
    curl --user "${TRAINING_GIT_CREDS_USR}:${TRAINING_GIT_CREDS_PSW}" -X POST -d '{"name":"'$1'"}' "${TRAINING_GIT_URL}/api/v3/user/repos"
    
    echo "--- creating webhook pointing to jenkins for GIT repository: ${1}"
    
    curl --user "${TRAINING_GIT_CREDS_USR}:${TRAINING_GIT_CREDS_PSW}" \
    -X POST "${TRAINING_GIT_URL}/api/v3/repos/${TRAINING_GIT_CREDS_USR}/${1}/hooks" \
    -d '{"name":"jenkins","config":{"url":"'"${TRAINING_JENKINS_URL}/github-webhook/"'"},"events":["create", "push", "pull_request"]}'
    pushd $1
    echo "--- creating GIT repo locally: ${1}"
    git init 
    git checkout -b develop
    git add . 
    git commit -m "initial commit"
    echo "--- pushing GIT repository: ${1}"
    git remote add origin "${TRAINING_GIT_URL}/git/${TRAINING_GIT_CREDS_USR}/${1}.git"
    git push origin --mirror -f
    git checkout -b conan_from_upstream
    git push origin --mirror -f
    popd
}

cd ~/training
initialize_repo $(basename $SCRIPTS)
initialize_repo $(basename $JENKINSLIB)

cd ~/training/cicd/data
for repo in $(ls $WORKSPACE); do
    initialize_repo $repo $WORKSPACE
done

