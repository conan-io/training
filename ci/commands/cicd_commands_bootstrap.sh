##################################################################################### 

# Initialize GIT and artifactory for CI/CD
~/training/cicd/bootstrap/bootstrap.sh  

cd ~/training/cicd/data
conan config install settings
conan config set general.revisions_enabled=True
conan config set general.default_package_id_mode=recipe_revision_mode 
conan user -p password -r conan-develop admin
conan user -p password -r conan-temp admin

conan export liba ci/stable
conan export libb ci/stable
conan export libc ci/stable
conan export libd ci/stable

conan create app1 ci/stable -pr release-gcc7-app1 --build=missing

conan upload '*' --all -r conan-develop --confirm

##################################################################################### 
cd ~/training/cicd/data

# Create lockfiles for each profile and product/application
conan lock create --reference app1/1.0@ci/stable -pr release-gcc7-app1 \
  --lockfile-out locks/prod/app1/1.0/release-gcc7-app1/conan.lock

# CI jobs will create and execute stages dynamically based on lockfiles available at runtime
# Each lockfile typically implies a specific build environment (docker or vm image, build agent, etc)
# CI will typically need to choose the correct build environment identifier for each lockfile it builds
# Thus we store a text file containing the build environment identifier alongside each lockfile for CI to use
echo 'conanio/gcc7:1.31.2' > locks/prod/app1/1.0/release-gcc7-app1/ci_build_env_tag.txt

# Commit and push lockfiles to locks repo
cd locks
git add .
git commit -m "bootstrap lockfiles for apps"
git push -u origin develop

# We will use the following script repeatedly to make our local environment mimic a clean CI build instance
cd ~/workspace
python ~/scripts/reset_workspace_and_cache.py
