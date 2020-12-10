#!/bin/bash

DEMO_GIT_CREDS_USR=${DEMO_GIT_CREDS_USR:-root}
DEMO_GIT_CREDS_PSW=${DEMO_GIT_CREDS_PSW:-root}

git config --global user.email "you@example.com"
git config --global user.name "Your Name"

git clone $GIT_URL -b $GIT_BRANCH

# We also clone the locks repo
git clone $LOCKS_REPO
cd locks

# Make a unique branch in the lockfile repo, combining package and feature branch info
# Unique string for lock branch may need to be customized, including user/channel, etc…
feature_branch=$GIT_BRANCH
package_name=$JOB_NAME
package_version=$(conan inspect . --raw version)
lock_branch=$package_name/$package_version/$GIT_BRANCH
git checkout -b $lock_branch

if [ ! -d locks/dev/$package_name/$package_version ]; then
    # We make copies of all lockfiles which reference this package so we can work on them safely
    # A script is provided to find those lockfiles and copy the directory of each 
    python ~/ci_scripts/copy_lockfiles_containing_package.py $package_name/$package_version locks/prod locks/dev/libb/1.0
  
    # copying: locks/prod/app1/1.0/release-gcc7-app1 ->  locks/dev/libb/1.0/app1/1.0/release-gcc7-app1

    # Now we commit and push the lockfiles to “cache”
    # Builds of future commits to this branch will re-use these “cached” lockfiles.
    # This guarantees such builds will only be testing the new commits, not new lockfiles
    cd locks
    git add .
    git commit -m "initialize lockfile directories for new branch $lock_branch"
    git push -u origin $lock_branch
fi

# We use the find command here to enumerate the lockfiles 

lockfile_dirs=$(find locks/dev/libb/1.0 -mindepth 3 -maxdepth 3 -type d -printf "%P\n" | sort)

echo $lockfile_dirs 

