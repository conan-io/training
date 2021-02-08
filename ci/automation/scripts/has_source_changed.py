import os
import json 
import argparse
import subprocess

# This script takes the local conanfile from source control and performs
# the `conan export` command to obtain the current revision number.  It 
# then searches for that revision in a given remote, to determine if the
# revision needs to be built or not. 

parser = argparse.ArgumentParser()

parser.add_argument(
    'remote',
    default='',
    help='directory to look for conanfile')

parser.add_argument(
    '--conanfile_dir',
    default='.',
    help='directory to look for conanfile')

args = parser.parse_args()

start_dir = os.getcwd()
os.chdir(args.conanfile_dir)

name = subprocess.check_output(
    "conan inspect . --raw name", 
    universal_newlines=True, 
    shell=True)
    
version = subprocess.check_output(
    "conan inspect . --raw version", 
    universal_newlines=True, 
    shell=True)

user = os.getenv('TRAINING_CONAN_USER')
channel = os.getenv('TRAINING_CONAN_CHANNEL')
reference = "{}/{}".format(name, version)
if user is not None and channel is not None:
    reference += "@{}/{}".format(user, channel)


remote_result = subprocess.check_output(
    "conan search {} --revisions --remote {}".format(reference, args.remote), 
    universal_newlines=True, 
    shell=True)
    
remote_revisions = [line.split()[0] for line in remote_result.rstrip().split("\n")[1:]]
   
subprocess.check_output(
    "conan export . {}/{}".format(user, channel), 
    universal_newlines=True, 
    shell=True)
    
local_result = subprocess.check_output(
    "conan search {} --revisions".format(reference), 
    universal_newlines=True, 
    shell=True)

local_revision = local_result.split("\n")[1].split()[0]
print(local_revision not in remote_revisions)


