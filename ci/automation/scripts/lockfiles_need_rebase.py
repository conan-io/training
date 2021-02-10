import os
import json 
import argparse
import subprocess

# This script determines if any of the `conan.lock` files which
# contain this package have changed since we branched off develop.
# Of note, this script assumes that its run on a branch other than develop.
# If it is run on develop branch, it will return False

parser = argparse.ArgumentParser()
parser.add_argument(
    '--conanfile_dir', 
    default='.',
    help='directory to look for conanfile')
parser.add_argument(
    'src_dir',
    help='path of prod lockfiles directory')
            
args = parser.parse_args()

name = subprocess.check_output(
    "conan inspect {} --raw name".format(args.conanfile_dir), 
    universal_newlines=True, 
    shell=True)
    
version = subprocess.check_output(
    "conan inspect {} --raw version".format(args.conanfile_dir), 
    universal_newlines=True, 
    shell=True)
    
name_and_version = "{}/{}".format(name, version)

needs_rebase = False
os.chdir(args.src_dir)

for src_root, dirs, files in os.walk("."):
    print(src_root)
    for _file in files:
        if _file == "conan.lock":
            file_full = os.path.join(src_root, _file)
            print(file_full)
            with open(file_full, 'r') as file:
                lockfile = json.load(file)
            for node in lockfile["graph_lock"]["nodes"].values():
                if name_and_version in node["ref"]:
                    lockfile_dir = os.path.dirname(file_full)
                    print(lockfile_dir)
                    # In theory, this could be more simple command: git diff develop:{}
                    # However, this way, it returns false if run on develop branched
                    # Which gives the behavior we want on first-time-job-run
                    output = subprocess.check_output(
                        "git diff origin/develop -- {}".format(lockfile_dir), 
                        universal_newlines=True, 
                        shell=True)
                    print(output)
                    needs_rebase = output.strip()==""
                    
print(str(needs_rebase))
