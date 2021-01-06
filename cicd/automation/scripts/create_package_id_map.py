import json 
import argparse
import os
import shutil
import subprocess

# This script relies on the PACKAGE_NAME_AND_VERSION being specified as a job parameter or env vars
# This scripts is intended to be run in the "from_upstream" stage of the product pipeline.
#
# This script is part of the strategy for handling multiple product lockfiles which share dependencies.
# With our pipeline strategy, we rebuild each package each time the source code or recipe changes.
# The goal is to ensure we rebuild the dependency once for each "effective profile" that is needed. 
# By needed, we mean, "needed by one or more product lockfiles". 
# Thus, we need to evaluate each product lockfile at the start of the build to create that list. 
# Each "effective profile" used in a lockfile corresponds to a single "package_id". 
# Very often, multiple product lockfiles will reference the same "package_id" for a shared dependency.
# So, this script distills out the list of unique "package_ids" from all lockfiles. 
# It produces a text file with a mapping from each "package_id" to a list of lockfiles which reference it. 
# CI launches one stage/build per-package_id, and then "updates" all the lockfiles in the map afterward.
# Prior to implementing this mapping strategy, we built each package once-per-lockfile. 
# This has several problems when dependencies are shared by multiple product lockfiles, especially at scale.
# In summary, it's both inefficient, and can cause errors. It's best to only build once-per-package-id.

parser = argparse.ArgumentParser()
parser.add_argument(
    'src_dir',
    help='source directory to search')
parser.add_argument(
    '--conanfile_dir', 
    default='.',
    help='directory to look for conanfile')
parser.add_argument(
    '--output_file',
    default="package_id_map.txt",
    help='path of file to save list to.')

args = parser.parse_args()


# This condition exists for "from_upstream" part of product pipeline
name_and_version = os.getenv("PACKAGE_NAME_AND_VERSION")
start_dir = os.getcwd()

# This condition exists for package pipeline:
if name_and_version == "auto" or name_and_version is "" or name_and_version is None:
    os.chdir(args.conanfile_dir)
    name = subprocess.check_output(
        "conan inspect . --raw name", 
        universal_newlines=True, 
        shell=True)
        
    version = subprocess.check_output(
        "conan inspect . --raw version", 
        universal_newlines=True, 
        shell=True)

    name_and_version = "{}/{}".format(name, version)
    os.chdir(start_dir)

package_ids = {}
max_depth = 3 # Only list lockfiles within the current package (skip lockfiles in deeper subdirs)

os.chdir(os.path.join(args.src_dir, name_and_version))
for src_root, dirs, files in os.walk("."):
    if src_root[len(args.src_dir):].count(os.sep) < max_depth:
        for _file in files:
            if _file == "conan.lock":
                file_full = os.path.join(src_root, _file)
                lockfile_dir = src_root.replace("./", "")
                with open(file_full, 'r') as file:
                    lockfile = json.load(file)
                for node in lockfile["graph_lock"]["nodes"].values():
                    if name_and_version in node["ref"]:
                        if node["package_id"] not in package_ids.keys():
                            package_ids[node["package_id"]] = [lockfile_dir]
                        else:
                            package_ids[node["package_id"]].append(lockfile_dir)

os.chdir(start_dir)

with open(args.output_file, 'w') as file:
    file.write('\n'.join([k + ":" + ','.join(v) for k,v in package_ids.items()]))

with open(args.output_file, 'r') as file:
    print(file.read())
