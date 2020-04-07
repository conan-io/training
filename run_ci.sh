#!/bin/bash

set -e && ./catchup.sh 1
set -e && ./catchup.sh 2
set -e && ./catchup.sh 3
set -e && ./catchup.sh 4
set -e && ./catchup.sh 5
set -e && ./catchup.sh 6
set -e && ./catchup.sh 7
set -e && ./catchup.sh 8
set +e && ./catchup.sh 9    # requires Artifactory
set -e && ./catchup.sh 10
set +e && ./catchup.sh 11   # requires Artifactory
set -e && ./catchup.sh 12
set -e && ./catchup.sh 13
set +e && ./catchup.sh 14   # option error
set +e && ./catchup.sh 15   # requires xbuilding
set +e && ./catchup.sh 16   # requires xbuilding
set +e && ./catchup.sh 17   # requires conflict
set -e && ./catchup.sh 18
set -e && ./catchup.sh 19
set -e && ./catchup.sh 20
set -e && ./catchup.sh 21
set -e && ./catchup.sh 22
set -e && ./catchup.sh 23
set -e && ./catchup.sh 24
set -e && ./catchup.sh 25
set -e && ./catchup.sh 26