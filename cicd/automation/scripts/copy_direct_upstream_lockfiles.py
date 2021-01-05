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
# This scripts works in the "from_upstream" stage of the product pipeline.
# The PACKAGE_NAME_AND_VERSION parameter actually defines the current jobs name and version. 
# There was surprisingly no other reliable way to deduce both automatically without parameter.
# This is because we re-build using "conan install --build", NOT "conan create" from git checkout.
# In other stages, we deduce this with conan inspect on the conanfile.py which was checked out.
# Also, it relies on the upstream jobs generating and committing build-order.json
# For each build-order.json, we look at the package reference in position 0.
# If this package is there, then we know that build-order is from a lockfile of a direct upstream


parser = argparse.ArgumentParser()
parser.add_argument(
    'src_dir',
    help='source directory to search')
parser.add_argument(
    'dst_dir',
    help='destination directory to copy to')
            
args = parser.parse_args()

name_and_version = os.getenv("PACKAGE_NAME_AND_VERSION")

max_depth = 6 # Only list lockfiles within the current package (skip lockfiles in deeper subdirs)

shutil.rmtree(args.dst_dir, ignore_errors=True)
for src_root, dirs, files in os.walk(args.src_dir):
    if src_root[len(args.src_dir):].count(os.sep) < max_depth:
        for _file in files:
            src_path_joined = os.path.join(src_root, _file)
            if _file == "build-order.json":
                with open(src_path_joined, 'r') as file:
                    build_order = json.load(file)
                for position, level in enumerate(build_order):
                    if position == 0: # Only evalute the "first" position
                        for pref, prev, _context, _id in level:
                            pref_short = pref.split('@')[0]
                            if name_and_version == pref_short:
                                lockfile_dir = "/".join(src_root.split("/")[4:])
                                dst_dir_suffix = "/".join(src_root.split("/")[2:4])
                                if not os.path.isdir(os.path.join(args.dst_dir, lockfile_dir)):
                                    os.makedirs(os.path.join(args.dst_dir, lockfile_dir))
                                dst_full = os.path.join(args.dst_dir, lockfile_dir, dst_dir_suffix)
                                shutil.rmtree(dst_full, ignore_errors=True)
                                print("copying: %s -> %s" % (src_root, dst_full))
                                shutil.copytree(src_root, dst_full)
