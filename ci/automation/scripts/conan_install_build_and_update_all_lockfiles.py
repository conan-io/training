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

name_and_version = os.getenv('PACKAGE_NAME_AND_VERSION')
user = os.getenv('TRAINING_CONAN_USER')
channel = os.getenv('TRAINING_CONAN_CHANNEL')

with open(args.package_id_map_file, 'r') as file:
    package_id_map = json.load(file)

target_lockfile_dirs = package_id_map[args.package_id]
target_pkg_name, target_pkg_version = name_and_version.split("/")
target_pkg_ref = name_and_version
if user is not None and channel is not None:
    target_pkg_ref += "@{}/{}".format(user, channel)

# Because many lockfiles might share the same package_id for a build, we have special handling.
# We only perform a full rebuild with "conan install --build" once (on the first lockfile).
# For subsequent lockfiles, we just re-create the lockfile and they captures the new updates. 

conanfile = os.path.join(args.conanfile_dir, "conanfile.py")
package_locks_root = os.path.join(args.lockfiles_root, name_and_version)
first_lockfile_dir_full = os.path.join(package_locks_root, target_lockfile_dirs[0])
first_lock_pkg_name, first_lock_pkg_version, _ = target_lockfile_dirs[0].split("/")
first_lock_pkg_ref = "{}/{}".format(first_lock_pkg_name, first_lock_pkg_version)
if user is not None and channel is not None:
    first_lock_pkg_ref += "@{}/{}".format(user, channel)

# Only run conan install --build for the first package
cmd = "conan lock create --reference {pkg_ref}" \
    " --lockfile={lockfile_dir}/conan.lock" \
    " --lockfile-out={lockfile_dir}/temp1.lock" \
    " --build={pkg_name}".format(
        pkg_ref=target_pkg_ref, 
        lockfile_dir=first_lockfile_dir_full, 
        pkg_name=target_pkg_name)
print("Running command: " + cmd)
output = subprocess.check_output(cmd, universal_newlines=True, shell=True)
print(output)

cmd = "conan install {pkg_ref}" \
    " --lockfile={lockfile_dir}/temp1.lock" \
    " --lockfile-out={lockfile_dir}/temp2.lock" \
    " --build={pkg_name}".format(
        pkg_ref=target_pkg_ref, 
        lockfile_dir=first_lockfile_dir_full,
        pkg_name=target_pkg_name)
print("Running command: " + cmd)
output = subprocess.check_output(cmd, universal_newlines=True, shell=True)
print(output)


lockfile_to_update_from = os.path.join(first_lockfile_dir_full, "temp2.lock")    
with open(lockfile_to_update_from, 'r') as json_file:
    lock_to_update_from = json.load(json_file)

# Now we go through each lockfile and patch them with the new package_id 
for lockfile_dir in target_lockfile_dirs:
    lockfile_to_be_updated = os.path.join(package_locks_root, lockfile_dir, "conan.lock")
    new_lockfile = os.path.join(package_locks_root, lockfile_dir, "conan-new.lock")

    for id, node in lock_to_update_from["graph_lock"]["nodes"].items():
        if node["ref"].startswith(target_pkg_ref):
            updated_node = node
        
    with open(lockfile_to_be_updated, 'r') as json_file:
        lock_to_be_updated = json.load(json_file)
    
    for id, node in lock_to_be_updated["graph_lock"]["nodes"].items():
        if node["ref"].startswith(target_pkg_ref):
            lock_to_be_updated["graph_lock"]["nodes"][id]["prev"] = updated_node["prev"]
            lock_to_be_updated["graph_lock"]["nodes"][id]["modified"] = updated_node["modified"]
            
    with open(new_lockfile, 'w') as json_file:
        json.dump(lock_to_be_updated, json_file, indent=2)
