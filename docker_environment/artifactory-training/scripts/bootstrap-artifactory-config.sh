#!/bin/bash
set -eo pipefail

/entrypoint-artifactory.sh &
/scripts/wait-for-artifactory.sh
/scripts/set-internal-hostname.sh &> set-internal-hostname.log
