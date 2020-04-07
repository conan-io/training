#!/bin/bash

set -e
./catchup.sh 1
./catchup.sh 2
./catchup.sh 3
./catchup.sh 4
./catchup.sh 5
./catchup.sh 6
./catchup.sh 7
./catchup.sh 8
#set +e && ./catchup.sh 9    # requires Artifactory
./catchup.sh 10
#set +e && ./catchup.sh 11   # requires Artifactory
./catchup.sh 12
./catchup.sh 13
#set +e && ./catchup.sh 14   # option error
#set +e && ./catchup.sh 15   # requires xbuilding
#set +e && ./catchup.sh 16   # requires xbuilding
#set +e && ./catchup.sh 17   # requires conflict
./catchup.sh 17  # should fail
./catchup.sh 18
./catchup.sh 19
./catchup.sh 20
./catchup.sh 21
./catchup.sh 22
./catchup.sh 23
./catchup.sh 24
./catchup.sh 25
./catchup.sh 26