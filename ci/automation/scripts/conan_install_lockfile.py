import json 
import argparse
import os
import shutil
import subprocess

parser = argparse.ArgumentParser()

parser.add_argument(
    'lockfile_dir',
    help='lockfile_name to install lockfiles for.')

parser.add_argument(
    'lockfiles_root',
    help='directory to look for conanfile')

parser.add_argument(
    '--conanfile_dir',
    default='.',
    help='directory to look for conanfile')
    
args = parser.parse_args()

user = os.getenv('TRAINING_CONAN_USER')
channel = os.getenv('TRAINING_CONAN_CHANNEL')

lock_pkg_name, lock_pkg_version, _ = args.lockfile_dir.split("/")
lock_pkg_ref = "{}/{}".format(lock_pkg_name, lock_pkg_version)
if user is not None and channel is not None:
    lock_pkg_ref += "@{}/{}".format(user, channel)

lockfile_dir_full = os.path.join(args.lockfiles_root, lock_pkg_name, lock_pkg_version, args.lockfile_dir)

cmd = "conan install {} --lockfile={}".format(lock_pkg_ref, lockfile_dir_full)
print("Running command: " + cmd)
output = subprocess.check_output(cmd, universal_newlines=True, shell=True)
print(output)