import json 
import argparse
import os
import subprocess

parser = argparse.ArgumentParser()
parser.add_argument(
    '--conanfile_dir', 
    default='.',
    help='directory to look for conanfile')
parser.add_argument(
    '--output_file',
    default='lock_branch_name.txt',
    help='text file to write value to')
            
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
source_branch_name = os.getenv('CHANGE_BRANCH') or os.getenv('GIT_BRANCH')
branch_name = "{}/{}".format(name_and_version, source_branch_name)

with open(args.output_file, 'w') as file:
    file.write(branch_name)

with open(args.output_file, 'r') as file:
    print(file.read())
