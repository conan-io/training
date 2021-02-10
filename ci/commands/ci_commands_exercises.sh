
##################################################################################### 
# For libb, clone repo, create branch, modify source, commit and push 
cd ~/workspace
python ~/scripts/reset_workspace_and_cache.py


git clone http://root:root@gitbucket/git/root/libb.git
cd libb
git checkout -b cool_feature

echo "\n// modify libb source" >> src/libb.cpp

git commit -a -m "modify libb source"
git push -u origin cool_feature


##################################################################################### 
cd ~/workspace
python ~/scripts/reset_workspace_and_cache.py

# Boilerplate CI process clones the repo and checks out the new branch
git clone http://root:root@gitbucket/git/root/libb.git -b cool_feature
# We also clone the locks repo
git clone http://root:root@gitbucket/git/root/locks.git

# Create a unique branch in the lockfile repo to hold the copied/modified lockfiles
# Branch name should be globally unique because lockfile repo used for all package changes
# The script below combines feature branch name with package name and version
lock_branch=$(python ~/scripts/calculate_lock_branch_name.py --conanfile_dir=libb)
echo $lock_branch # -> libb/1.0/cool_feature

# Create the new branch for the copied lockfiles
cd locks
git checkout -B $lock_branch



##################################################################################### 
cd ~/workspace

# We make copies of all lockfiles which reference libb so we can work on them safely
# A script is provided to find those lockfiles and copy the directory of each 
python ~/scripts/copy_lockfiles_containing_package.py --conanfile_dir=libb locks/prod locks/dev
# copying: locks/prod/app1/1.0/release-gcc7-app1 ->  locks/dev/libb/1.0/app1/1.0/release-gcc7-app1

# Now we must add/commit/push the new lockfiles for use in build stages
cd locks
git add . && git commit -m "update lockfiles for $lock_branch"
git push --set-upstream origin $lock_branch

# At this point, CI will typically branch into one-stage-per-lockfile
# In Jenkins, we use the “parallel” stages block to create multiple branches
# A script is provided to enumerate the lockfiles which were just copied
cd ~/workspace
lockfile_dirs=$(python ~/scripts/list_lockfile_names.py locks/dev)
echo $lockfile_dirs # -> app1/1.0/release-gcc7-app1

# We then spawn the stages with the stage name equal to the lockfile directory name
#  example: app1/1.0/release-gcc7-app1


##################################################################################### 
 
# As stated earlier, each lockfile has a corresponding build environment, such as:
# - docker image name
# - virtual machine image name
# - physical build server name
# - CI tags
# - etc
#
# For this training, we specify a docker image name
# 
# Jenkins will read this from the text file beside each lockfile named “ci_build_env_tag.txt”
#
# Example Jenkins code for reading the file and invoking docker can be found in Jenkinsfile
#
# All of the steps above should be trivial in any professional CI application
# 
# The the following slides show the commands for a single lockfile in a single clean build environment


##################################################################################### 
cd ~/workspace
python ~/scripts/reset_workspace_and_cache.py

# Now we are in a new stage created to operate on a single lockfile
# We now need to get source code into our clean per-lockfile build environment
# For a docker container, this can be achieved multiple ways depending on CI 
# Here, we do simplest approach which is to checkout the sources manually in the container

git clone http://root:root@gitbucket/git/root/libb.git -b cool_feature

# We also clone the locks repo again inside the container
# Again, we deduce the lock branch name and check it out
lock_branch=$(python ~/scripts/calculate_lock_branch_name.py --conanfile_dir=libb)
echo $lock_branch # -> libb/1.0/cool_feature

git clone http://root:root@gitbucket/git/root/locks.git -b $lock_branch



##################################################################################### 

cd ~/workspace
# The unique lockfile directory is available in the form of the env.STAGE_NAME environment variable
lockfile_dir=app1/1.0/release-gcc7-app1 # We set it here manually for this exercise

# Install packages from each lockfiles to populate the cache with all locked versions
conan install app1/1.0@ci/stable --lockfile locks/dev/libb/1.0/$lockfile_dir

# Create a lockfile with only libb and it’s dependencies, but with the full profile of app1
conan lock create libb/conanfile.py --user=ci --channel=stable \
    --lockfile=locks/dev/libb/1.0/$lockfile_dir/conan.lock \
    --lockfile-out=locks/dev/libb/1.0/$lockfile_dir/temp1.lock

# Rebuild libb and create a new lockfile with libb fully updated
conan create libb ci/stable \
    --lockfile=locks/dev/libb/1.0/$lockfile_dir/temp1.lock \
    --lockfile-out=locks/dev/libb/1.0/$lockfile_dir/temp2.lock

# Create a new lockfile for app based on the new libb lockfile
conan lock create --reference app1/1.0@ci/stable \
    --lockfile=locks/dev/libb/1.0/$lockfile_dir/temp2.lock \
    --lockfile-out=locks/dev/libb/1.0/$lockfile_dir/conan-new.lock



##################################################################################### 
# Upload new revision to conan-temp
cd ~/workspace
conan upload 'libb/1.0@ci/stable' --all -r conan-temp --confirm

# Add/commit/push the updated lockfiles
cd locks
git add . && git commit -m "update lockfiles for $lock_branch"
git push --set-upstream origin libb/1.0/cool_feature

# Multiple CI stages updating different lockfiles can 
# commit in parallel and asynchronously and don’t conflict

cd ~/workspace
python ~/scripts/reset_workspace_and_cache.py

# Create branch to simulate submitting a PR
# Base it off cool_feature and use branch named with prefix PR- 
# Branches starting with PR- will trigger product pipeline

git clone http://root:root@gitbucket/git/root/libb.git
cd libb
git checkout -b PR-01 origin/cool_feature
git push origin PR-01


##################################################################################### 

cd ~/workspace
python ~/scripts/reset_workspace_and_cache.py

# Standard clone of libb repository and checkout of PR-01 branch
git clone http://root:root@gitbucket/git/root/libb.git -b PR-01

# We use the script below to obtain the source branch the PR has come from
source_branch=$(python ~/scripts/find_source_branch.py --git_dir=libb)  
echo $source_branch # -> cool_feature

# When our CI jobs detect that the branch is a PR’s, we don’t actually even run a build
# The new binaries were already built under new revisions in the feature branch.
# Instead, we run the Product pipeline which is simply the name of a function in the CI job. 
# This function runs Conan to calculate build order from lockfiles, then triggers downstreams.
# All we really need is to clone lockfiles repo and checkout the appropriate branch.

lock_branch=$(python ~/scripts/calculate_lock_branch_name.py --conanfile_dir=libb)
echo $lock_branch # -> libb/1.0/cool_feature
git clone http://root:root@gitbucket/git/root/locks.git -b $lock_branch


##################################################################################### 

cd ~/workspace
# We use the find command here to enumerate the lockfiles 
lockfile_dirs=$(python ~/scripts/list_lockfile_names.py locks/dev/libb/1.0)
echo $lockfile_dirs # -> app1/1.0/release-gcc7-app1

# Extract build-order json files from all lockfiles used. Then use provided script to consolidate them.
conan lock build-order \
   --json=locks/dev/libb/1.0/app1/1.0/release-gcc7-app1/build-order.json \
   locks/dev/libb/1.0/app1/1.0/release-gcc7-app1/conan-new.lock 

combined_build_order=$(python ~/scripts/create_combined_build_order.py locks/dev/libb/1.0)

echo $combined_build_order
# {
#    "0": [
#        "libd/1.0@ci/stable#c7e9584bf06809a97df22f7953bd1224"
#    ],
#    "1": [
#        "app1/1.0@ci/stable#e00bedd1d7055f91c5920e796c278a27"
#    ]
#}


##################################################################################### 

cd ~/workspace

#Add/commit/push the build order json files. They’re used in later steps, and are helpful for debug.
cd locks
git add . && git commit -m "add build-order json lockfiles for libb/1.0 on $lock_branch"
git push



##################################################################################### 

# At this point, the current CI job will need to trigger other CI jobs 
#
# CI-Specific code will be needed for some things:
#   - Read combined_build_order.json
#   - Associate package refs from combined_build_order.json to CI Jobs
#   - Trigger those CI jobs in the order specified
#
# In our case, we chose CI job names which match the package names to make it easy
#
# When multiple packages appear in a single level in combined_build_order.json,
# that indicates that those builds can be done in parallel (from Conan’s perspective).
# As we’ll see, our lockfile storage/convention can supports parallel jobs as well.
# In Jenkins, we can once again use the “parallel” block to trigger multiple downstreams at once.
# In this example, we don’t have a parallel case, but we plan to demonstrate one soon.
#
# Finally, the downstream job must be able to accept a string parameter as an input
# We will pass the dedicated branch name for this feature branch in the lockfile repo
#   libb/1.0/cool_feature
# 
# On the condition that the downstream has received such a string parameter the downstream 
# will understand it’s being invoked as part of the package pipeline, and act accordingly


##################################################################################### 
cd ~/workspace
python ~/scripts/reset_workspace_and_cache.py

# Build libd which was triggered by libb PR as part of product pipeline
# In Jenkins, environment variables corresponding to the Job Parameters exist
# We use those variables for various purposes in this stage

lock_branch=libb/1.0/cool_feature  # On jenkins:   lock_branch=${params.LOCKFILE_BRANCH}
PACKAGE_NAME_AND_VERSION=libd/1.0  # On jenkins:   lock_branch=${params.PACKAGE_NAME_AND_VERSION}

export PACKAGE_NAME_AND_VERSION    # We export this one because it’s used in upcoming script

git clone http://root:root@gitbucket/git/root/locks.git -b $lock_branch

# Copy lockfiles from packages which are directly upstream from this package (libd/1.0)
# Note that following script distinguish “direct” upstreams with transitive upstreams and neighbors
python ~/scripts/copy_direct_upstream_lockfiles.py locks/dev locks/dev/libd/1.0
# copying:   
# locks/dev/libb/1.0/app1/1.0/release-gcc7-app1 -> locks/dev/libd/1.0/app1/1.0/release-gcc7-app1/libb/1.0



##################################################################################### 
# Before we spawn new stages to build each lockfile, there is something we must do
# For each lockfile directory, we must do some lockfile consolidation to support diamond dependencies
# Each might have multiple subdirectories with lockfiles modified by parallel neighbors upstream
# So for each, we must consolidate all such lockfiles into a single lockfile to pass to conan
# We also copy the ci_build_env_tag.txt along with the first lockfile

lockfile_dirs=$(python ~/scripts/list_lockfile_names.py locks/dev)
echo $lockfile_dirs 
# Should be app1/1.0/release-gcc7-app1

python ~/scripts/consolidate_lockfiles.py \
     --lockfile_base_dir=locks/dev/libd/1.0 \
     --lockfile_names_file=lockfile_names.txt

# Finally, lets commit these new consolidated files so that future stages can use them
cd locks
git add . && git commit -m "add consolidated lockfiles for libd/1.0 on $lock_branch"
git push

# Now, we can to spawn CI build stages for each lockfile



##################################################################################### 

# Now we are in a new build stage for a single lockfile with a build instance with clean workspace
cd ~/workspace
python ~/scripts/reset_workspace_and_cache.py
lock_branch=libb/1.0/cool_feature       # On Jenkins: lock_branch = ${params.LOCKFILE_BRANCH}
git clone http://root:root@gitbucket/git/root/locks.git -b $lock_branch
lockfile_dir=app1/1.0/release-gcc7-app1    # On jenkins: lockfile_dir=${env.STAGE_NAME}

# Create a new lockfile with libd “unlocked” and ready to be rebuilt
conan lock create --reference app1/1.0@ci/stable --build=libd \
  --lockfile=locks/dev/libd/1.0/$lockfile_dir/conan.lock \
  --lockfile-out=locks/dev/libd/1.0/$lockfile_dir/conan-temp1.lock

# Rebuild libd using the new lockfile
conan install libd/1.0@ci/stable --build=libd \
  --lockfile=locks/dev/libd/1.0/$lockfile_dir/conan-temp1.lock \
  --lockfile-out=locks/dev/libd/1.0/$lockfile_dir/conan-new.lock

# Create an updated build order file to satisfy copy_direct_upstream_lockfiles.py in downstream jobs
conan lock build-order \
   --json=locks/dev/libd/1.0/$lockfile_dir/build-order.json \
   locks/dev/libd/1.0/$lockfile_dir/conan-new.lock 


##################################################################################### 

# Upload new revision to conan-temp
conan upload 'libd/1.0@ci/stable' --all -r conan-temp --confirm

# Add/commit/push the updated lockfiles
cd locks
git add . && git commit -m "update lockfiles for $lock_branch/libd/1.0"
git push


##################################################################################### 

# At this point, flow control returns to the libb/1.0/PR-01 job
#
# Recall that jenkins job trigger call was based off the combined build order:
#
# libd/1.0
# app1/1.0
# 
# So, next, jenkins will trigger app1 job
# 
# The app1 job will perform the exact same steps as the libd job

##################################################################################### 

cd ~/workspace
python ~/scripts/reset_workspace_and_cache.py

# Build app1 which was triggered by libb PR as part of product pipeline
# In Jenkins, environment variables corresponding to the Job Parameters exist
# We use those variables for various purposes in this stage

lock_branch=libb/1.0/cool_feature  # On jenkins:   lock_branch=${params.LOCKFILE_BRANCH}
PACKAGE_NAME_AND_VERSION=app1/1.0  # On jenkins:   lock_branch=${params.PACKAGE_NAME_AND_VERSION}

export PACKAGE_NAME_AND_VERSION    # We export this one because it’s used in upcoming script

git clone http://root:root@gitbucket/git/root/locks.git -b $lock_branch

# Copy lockfiles from packages which are directly upstream from this package (app1/1.0)
# Note that following script distinguish “direct” upstreams with transitive upstreams and neighbors
python ~/scripts/copy_direct_upstream_lockfiles.py locks/dev locks/dev/app1/1.0
# copying:   
# locks/dev/libd/1.0/app1/1.0/release-gcc7-app1 -> locks/dev/app1/1.0/app1/1.0/release-gcc7-app1/libd/1.0


##################################################################################### 

# Before we spawn new stages to build each lockfile, there is something we must do
# For each lockfile directory, we must do some lockfile consolidation to support diamond dependencies
# Each might have multiple subdirectories with lockfiles modified by parallel neighbors upstream
# So for each, we must consolidate all such lockfiles into a single lockfile to pass to conan
# We also copy the ci_build_env_tag.txt along with the first lockfile

lockfile_dirs=$(python ~/scripts/list_lockfile_names.py locks/dev)
echo $lockfile_dirs 
# Should be app1/1.0/release-gcc7-app1

python ~/scripts/consolidate_lockfiles.py \
     --lockfile_base_dir=locks/dev/app1/1.0 \
     --lockfile_names_file=lockfile_names.txt

# Finally, lets commit these new consolidated files so that future stages can use them
cd locks
git add . && git commit -m "add consolidated lockfiles for app1/1.0 on $lock_branch"
git push

# Now, we can to spawn CI build stages for each lockfile



##################################################################################### 

# Now we are in a new build stage for a single lockfile with a build instance with clean workspace
cd ~/workspace
python ~/scripts/reset_workspace_and_cache.py
lock_branch=libb/1.0/cool_feature       # On Jenkins: lock_branch = ${params.LOCKFILE_BRANCH}
git clone http://root:root@gitbucket/git/root/locks.git -b $lock_branch
lockfile_dir=app1/1.0/release-gcc7-app1    # On jenkins: lockfile_dir=${env.STAGE_NAME}

# Create a new lockfile with libd “unlocked” and ready to be rebuilt
conan lock create --reference app1/1.0@ci/stable --build=app1 \
  --lockfile=locks/dev/app1/1.0/$lockfile_dir/conan.lock \
  --lockfile-out=locks/dev/app1/1.0/$lockfile_dir/conan-temp1.lock

# Rebuild libd using the new lockfile
conan install app1/1.0@ci/stable --build=app1 \
  --lockfile=locks/dev/app1/1.0/$lockfile_dir/conan-temp1.lock \
  --lockfile-out=locks/dev/app1/1.0/$lockfile_dir/conan-new.lock

# Create an updated build order file to satisfy copy_direct_upstream_lockfiles.py in downstream jobs
conan lock build-order \
   --json=locks/dev/app1/1.0/$lockfile_dir/build-order.json \
   locks/dev/app1/1.0/$lockfile_dir/conan-new.lock 

##################################################################################### 

# Upload new revision to conan-temp
conan upload 'app1/1.0@ci/stable' --all -r conan-temp --confirm

# Add/commit/push the updated lockfiles
cd locks
git add . && git commit -m "update lockfiles for $lock_branch/app1/1.0"
git push



##################################################################################### 

# PR can usually be merged in GIT via the GUI
# Here we do the merge locally and “push” the merge commit for the same effect

cd ~/workspace
python ~/scripts/reset_workspace_and_cache.py

git clone http://root:root@gitbucket/git/root/libb.git
cd libb
git checkout develop
git merge origin/PR-01
git push


##################################################################################### 

# Commits on “develop” branch represent and indicate “promotion” events in this CI
#
# All the binaries have already been built and tested
#
# Rebuilding artifacts now would not only introduce risk, but also massive unnecessary delay
#
# Instead, we “promote” all binaries built during the feature branch and PR to conan-develop
#
# Lockfiles are designed to enable promotion of an entire group of packages like this


##################################################################################### 

cd ~/workspace
python ~/scripts/reset_workspace_and_cache.py

# Boilerplate checkout 
git clone http://root:root@gitbucket/git/root/libb.git -b develop

source_branch=$(python ~/scripts/find_source_branch.py --git_dir=libb)
echo $source_branch # -> cool_feature

lock_branch=$(python ~/scripts/calculate_lock_branch_name.py --conanfile_dir=libb)
echo $lock_branch # -> libb/1.0/cool_feature

git clone http://root:root@gitbucket/git/root/locks.git -b $lock_branch

##################################################################################### 

# We must identify the “final” packages which will have “fully-updated” lockfiles
# We base this on the package folders in dev which match lockfile folder names in prod
# For example, in dev we have libb, libd, and app1, in prod we only have app1 
# So we deduce that this is the only directory under dev which has “fully-updated” lockfiles
cd ~/workspace
product_lockdirs=$(python ~/scripts/list_product_lockfiles_in_dev.py locks/dev)
echo $product_lockdirs  # -> app1/1.0/release-gcc7-app1

# Now, we will spawn separate parallel CI stages to process each lockfile_dir


##################################################################################### 

python ~/scripts/reset_workspace_and_cache.py
cd ~/workspace

# Now we are in a new stage created to promote a binary Conan package
# We repeat the process of deducing the original branch name from the PR
git clone http://root:root@gitbucket/git/root/libb.git -b PR-01
source_branch=$(python ~/scripts/find_source_branch.py --git_dir=libb)
echo $source_branch # -> cool_feature
lock_branch=$(python ~/scripts/calculate_lock_branch_name.py --conanfile_dir=libb)
echo $lock_branch # -> libb/1.0/cool_feature

# We clone the lockfile repo again
git clone http://root:root@gitbucket/git/root/locks.git -b $lock_branch 

##################################################################################### 

cd ~/workspace

# This stage is dedicated to promoting one lockfile
lockfile_dir=app1/1.0/release-gcc7-app1    # On jenkins: lockfile_dir=${env.STAGE_NAME}

# We also extract the product name and version from the stage name
product=app1/1.0

# Now we can install the fully completed lockfile, and promote all of the binaries in it
conan install app1/1.0@ci/stable --lockfile=locks/dev/$product/$lockfile_dir/conan-new.lock

conan upload '*' --all -r conan-develop --confirm

# We do not promote the lockfile here because we will promote them all in one commit


##################################################################################### 


# Once all Conan package promotion stages are done, the job returns to the parent scope
# In Jenkins, we will still have the lockfiles checked out in that scope
# However, with our manual workspace, we need to re-run the commands to re-create the state

python ~/scripts/reset_workspace_and_cache.py

git clone http://root:root@gitbucket/git/root/libb.git -b develop

python ~/scripts/find_source_branch.py --git_dir=libb

lock_branch=$(python ~/scripts/calculate_lock_branch_name.py --conanfile_dir=libb)

git clone http://root:root@gitbucket/git/root/locks.git -b $lock_branch

python ~/scripts/list_product_lockfiles_in_dev.py locks/dev



##################################################################################### 

# We can now promote all the updated lockfiles for all products in one commit
cd locks
git checkout develop
git merge $lock_branch

cd ~/workspace

# This scripts identifies and copies the “fully updated” lockfiles to be promoted
python ~/scripts/copy_dev_lockfiles_to_prod.py locks/dev locks/prod
# We also remove the dev directory 
python ~/scripts/remove_lockfiles_dir.py locks/dev

cd locks
git add . 
git commit -m "Promoting lockfiles for branch $lock_branch"
git push -u origin develop


##################################################################################### 
