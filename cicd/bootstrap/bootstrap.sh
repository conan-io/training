#!/bin/bash

DEMO_GIT_CREDS_USR=${DEMO_GIT_CREDS_USR:-root}
DEMO_GIT_CREDS_PSW=${DEMO_GIT_CREDS_PSW:-root}

git config --global user.email "you@example.com"
git config --global user.name "Your Name"

CI_DATA=~/training/cicd/data
CI_SCRIPTS=~/training/cicd/ci_scripts
WORKSPACE=~/workspace
SCRIPTS=~/scripts

mkdir -p "${WORKSPACE}"
mkdir -p "${SCRIPTS}"

cp -RT "${CI_DATA}" "${WORKSPACE}"
cp -RT "${CI_SCRIPTS}" "${SCRIPTS}"

repos=`ls ${WORKSPACE}`

for repo in `echo $repos`; do

    echo "--- creating $repo on server"
    curl --user "${DEMO_GIT_CREDS_USR}:${DEMO_GIT_CREDS_PSW}" -X POST -d '{"name":"'$repo'"}' "http://gitbucket/api/v3/user/repos"
    
    
    echo "--- creating webhook pointing to jenkins for $repo"
    curl --user "${DEMO_GIT_CREDS_USR}:${DEMO_GIT_CREDS_PSW}" \
    -X POST "http://gitbucket/api/v3/repos/${DEMO_GIT_CREDS_USR}/${repo}/hooks" \
    -d '{"name":"jenkins","config":{"url":"http://jenkins:8080/github-webhook/"},"events":["push", "pull_request"]}'
    
    pushd ${WORKSPACE}/$repo
    echo "--- creating ${WORKSPACE}/$repo locally"
    
    git init 
    git checkout -b develop
    git add . 
    git commit -m "initial commit"
    
    echo "--- pushing $repo"
    git remote add origin "http://${DEMO_GIT_CREDS_USR}:${DEMO_GIT_CREDS_PSW}@gitbucket/git/${DEMO_GIT_CREDS_USR}/${repo}.git"
    git push origin --mirror -f
    
    popd
done


