import json 
import argparse
import os
import shutil
import subprocess


parser = argparse.ArgumentParser()

parser.add_argument(
    'package_id',
    help='package_id to install lockfiles for.')

parser.add_argument(
    'lockfiles_root',
    help='directory to look for conanfile')

parser.add_argument(
    '--conanfile_dir',
    default='.',
    help='directory to look for conanfile')
    
parser.add_argument(
    '--package_id_map_file',
    default="package_id_map.json",
    help='file containing mapping of package_id to list of lockfiles.')

args = parser.parse_args()

with open(args.package_id_map_file, 'r') as file:
    package_id_map = json.load(file)
    
target_lockfile_dirs = package_id_map[args.package_id]

user = os.getenv('TRAINING_CONAN_USER')
channel = os.getenv('TRAINING_CONAN_CHANNEL')

pkg_name = subprocess.check_output(
    "conan inspect {} --raw name".format(args.conanfile_dir), 
    universal_newlines=True, 
    shell=True)
    
pkg_version = subprocess.check_output(
    "conan inspect {} --raw version".format(args.conanfile_dir), 
    universal_newlines=True, 
    shell=True)

for lockfile_dir in target_lockfile_dirs:
    lockfile_dir_full = os.path.join(args.lockfiles_root, pkg_name, pkg_version, lockfile_dir)
    lock_pkg_name, lock_pkg_version, _ = lockfile_dir.split("/")
    lock_pkg_ref = "{}/{}".format(lock_pkg_name, lock_pkg_version)
    if user is not None and channel is not None:
        lock_pkg_ref += "@{}/{}".format(user, channel)
    
    cmd = "conan install {} --lockfile={}".format(lock_pkg_ref, lockfile_dir_full)
    print("Running command: " + cmd)
    output = subprocess.check_output(cmd, universal_newlines=True, shell=True)
    print(output)