import json 
import argparse
import os
import shutil
import subprocess

# This script relies on the PACKAGE_NAME_AND_VERSION being specified as a job parameter or env vars
# This scripts is intended to be run in the "from_upstream" stage of the product pipeline.
# 
# This script copies all lockfiles of packages which are directly upstream from the provided package.
# It creates/copies the whole directory structure containing each lockfile with it's neighboring files.
# It copies them to a subdirectory under a new sub-directory of "dev" created just for "this job".
# The rest of the job will only look at and use these copies in this subdirectory.
#
# The PACKAGE_NAME_AND_VERSION parameter actually defines the current jobs name and version. 
# There was surprisingly no other reliable way to deduce both automatically without parameter.
# This is because we re-build using "conan install --build", NOT "conan create" from git checkout.
# In other stages, we deduce this with conan inspect on the conanfile.py which was checked out.
# Also, it uses the combined-build-order.json created in the root upstream package directory.
# It uses this to determine the current packages direct upstreams and only copy lockfiles from those.

parser = argparse.ArgumentParser()
parser.add_argument(
    'lockfiles_root',
    help='root directory to search for lockfiles which were modified by direct upstream jobs.')
parser.add_argument(
    'package_name_and_version',
    help='target package name and version to copy upstreams of.')
            
args = parser.parse_args()

name_and_version = args.package_name_and_version
dst_dir = os.path.join(args.lockfiles_root, name_and_version)
shutil.rmtree(dst_dir, ignore_errors=True)
os.makedirs(dst_dir)

for src_root, dirs, files in os.walk(args.lockfiles_root):
    for _file in files:
        if _file == "combined_build_order.json":
            src_path_joined = os.path.join(src_root, _file)
            with open(src_path_joined, 'r') as file:
                build_order = json.load(file)
                root_upstream = src_root.replace(args.lockfiles_root + "/","")
                   
for level, prefs in build_order.items():
    for pref in prefs:
        pref_short = pref.split("@")[0]
        if pref_short == name_and_version:
            int_level = int(level)
            if int_level == 0:
                direct_upstreams = [root_upstream]
            else:
                previous_level = int_level-1
                direct_upstreams = build_order[str(previous_level)]


max_depth = 4 # Only list lockfiles within the current package (skip lockfiles in deeper subdirs)
for direct_upstream in direct_upstreams:
    upstream_short = direct_upstream.split("@")[0]
    upstream_root = os.path.join(args.lockfiles_root, upstream_short)
   
    for src_root, dirs, files in os.walk(upstream_root):
        if src_root[len(upstream_root):].count(os.sep) < max_depth:
            for _file in files:
                if _file == "conan.lock":
                    file_full = os.path.join(src_root, _file)
                    lockfile_dir = "/".join(src_root.split("/")[4:])
                    with open(file_full, 'r') as file:
                        lockfile = json.load(file)
                    for node in lockfile["graph_lock"]["nodes"].values():
                        if name_and_version in node["ref"]:
                            dst_full = os.path.join(dst_dir, lockfile_dir, upstream_short)
                            print("copying: %s -> %s" % (src_root, dst_full))
                            shutil.copytree(src_root, dst_full)