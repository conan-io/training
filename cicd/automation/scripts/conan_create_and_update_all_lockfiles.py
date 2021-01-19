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
    default="package_id_map.txt",
    help='file containing mapping of package_id to list of lockfiles.')

args = parser.parse_args()

package_id_map = {}
with open(args.package_id_map_file, 'r') as file:
    package_id_lines = file.readlines()
    for package_id_line in package_id_lines:
        line_split = package_id_line.split(":")
        package_id_map[line_split[0]] = line_split[1].split(",")

target_lockfile_dirs = package_id_map[args.package_id]

user = os.getenv('TRAINING_CONAN_USER')
channel = os.getenv('TRAINING_CONAN_CHANNEL')

target_pkg_name = subprocess.check_output(
    "conan inspect {} --raw name".format(args.conanfile_dir), 
    universal_newlines=True, 
    shell=True)
    
target_pkg_version = subprocess.check_output(
    "conan inspect {} --raw version".format(args.conanfile_dir), 
    universal_newlines=True, 
    shell=True)
    
target_pkg_ref = "{}/{}".format(target_pkg_name, target_pkg_version)
if user is not None and channel is not None:
    target_pkg_ref += "@{}/{}".format(user, channel)
    
conanfile = os.path.join(args.conanfile_dir, "conanfile.py")
target_lockfiles_root = os.path.join(args.lockfiles_root, target_pkg_name, target_pkg_version)
print(target_lockfiles_root)
# Because many lockfiles might share the same package_id for a build, we have special handling.
# We only perform a full rebuild with "conan create" once (on the first lockfile).   
# For subsequent lockfiles, we copy the json diff from the updated lockfiles to the them
# This is safe in the context of our CI because we have a clean and hermetic cache for each execution
for lockfile_dir in target_lockfile_dirs:
    lockfile_dir_full = os.path.join(target_lockfiles_root, lockfile_dir)
    lock_pkg_name, lock_pkg_version, _ = lockfile_dir.split("/")
    lock_pkg_ref = "{}/{}".format(lock_pkg_name, lock_pkg_version)
    if user is not None and channel is not None:
        lock_pkg_ref += "@{}/{}".format(user, channel)
        
    cmd = "conan lock create {conanfile} --user={user} --channel={channel}" \
        " --lockfile={lockfile_dir}/conan.lock" \
        " --lockfile-out={lockfile_dir}/temp1.lock".format(
            conanfile=conanfile, 
            lockfile_dir=lockfile_dir_full, 
            user=user, 
            channel=channel)
    print("Running command: " + cmd)
    output = subprocess.check_output(cmd, universal_newlines=True, shell=True)
        
    cmd = "conan create {conanfile_dir} {user}/{channel}" \
            " --lockfile={lockfile_dir}/temp1.lock" \
            " --lockfile-out={lockfile_dir}/temp2.lock" \
            " --build=outdated".format(
                conanfile_dir=args.conanfile_dir, 
                lockfile_dir=lockfile_dir_full, 
                user=user, 
                channel=channel)
    print("Running command: " + cmd)
    output = subprocess.check_output(cmd, universal_newlines=True, shell=True)
    print(output)

    cmd = "conan lock create --reference {pkg_ref}" \
            " --lockfile={lockfile_dir}/temp2.lock" \
            " --lockfile-out={lockfile_dir}/conan-new.lock".format(
                pkg_ref=lock_pkg_ref, 
                lockfile_dir=lockfile_dir_full)
    print("Running command: " + cmd)
    output = subprocess.check_output(cmd, universal_newlines=True, shell=True)
    print(output)