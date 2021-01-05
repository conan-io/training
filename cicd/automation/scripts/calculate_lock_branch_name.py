import json 
import argparse
import os
import subprocess

# This script one of several methods to derive the branch name in the lockfile repository
# This script is used in package, product, and promotion pipelines
#
# In the package pipeline, the lockfile branch name is derived from conanfile and git
# In the first part of the product pipeline, it is the same as above.
# In the "from_upstream" part of the product pipeline, the source branch is passed as a CI job parameter.
#      Note: from_upstream does not use this script, it just takes the value from CI directly.
# In promotion pipeline, deriving the lockfile branch name is fairly tricky. 
# The "find_source_branch.py" script must be run prior to this script. 
#      Note: See that script for an explanation of how it works. 
# That script writes a text file with the the original feature branch name of the package repo. 
# This script then reads that file and combines it with conan inspect to derive the lock branch name.

parser = argparse.ArgumentParser()
parser.add_argument(
    '--conanfile_dir', 
    default='.',
    help='directory to look for conanfile')
parser.add_argument(
    '--source_branch_name_file', 
    default='source_branch_name.txt',
    help='directory to look for text file with name of source branch')
parser.add_argument(
    '--output_file',
    default='lock_branch_name.txt',
    help='text file to write value to')
            
args = parser.parse_args()

source_branch_name = None

if os.path.isfile(args.source_branch_name_file):
    # This condition exists for promotion pipeline, where branch name written in file
    with open(args.source_branch_name_file, 'r') as file:
        source_branch_name = file.read().strip()


start_dir = os.getcwd()
os.chdir(args.conanfile_dir)

if source_branch_name is None:
    # This condition exists for package pipeline where source branch is current branch
    source_branch_name = os.getenv('GIT_BRANCH') or subprocess.check_output(
                                                    "git rev-parse --abbrev-ref HEAD"
                                                    , universal_newlines=True, shell=True).strip()

name = subprocess.check_output(
    "conan inspect . --raw name", 
    universal_newlines=True, 
    shell=True)
    
version = subprocess.check_output(
    "conan inspect . --raw version", 
    universal_newlines=True, 
    shell=True)

name_and_version = "{}/{}".format(name, version)
lock_branch_name = "{}/{}".format(name_and_version, source_branch_name)

os.chdir(start_dir)

with open(args.output_file, 'w') as file:
    file.write(lock_branch_name)

with open(args.output_file, 'r') as file:
    print(file.read())
