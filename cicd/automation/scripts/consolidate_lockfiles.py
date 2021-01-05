import json 
import argparse
import os
import shutil
import subprocess

# To support when diamond dependency CI jobs are "joined" 
# We need to use "conan lock update" command to consolidate 
# changes from multiple lockfiles into a single lockfile

parser = argparse.ArgumentParser()
parser.add_argument(
    '--lockfile_names_file',
    help='text file with a list of lockfile names')
parser.add_argument(
    '--lockfile_base_dir',
    help='package base directory to start walking for lockfiles')
            
args = parser.parse_args()

with open(args.lockfile_names_file, 'r') as file:
    lockfile_names = file.read().split("\n")

for lockfile_name in lockfile_names:
    lockfile_dir = os.path.join(args.lockfile_base_dir, lockfile_name)
    consolidated_lockfile = os.path.join(lockfile_dir, "conan.lock")
    lockfiles = []
    for src_root, dirs, files in os.walk(lockfile_dir):
        if not src_root == lockfile_dir:
            for _file in files:
                if _file == "conan-new.lock":
                    lockfiles.append(os.path.join(src_root, _file))
    if lockfiles:
        first_lock_dir = os.path.dirname(lockfiles[0])
        shutil.copy(lockfiles[0], consolidated_lockfile)
        shutil.copy(os.path.join(first_lock_dir,"ci_build_env_tag.txt"), lockfile_dir)
        for lockfile in lockfiles:
            print("updating {} with info from {}".format(consolidated_lockfile, lockfile))
            subprocess.check_output(
                "conan lock update {} {}".format(consolidated_lockfile, lockfile), 
                universal_newlines=True, 
                shell=True)
