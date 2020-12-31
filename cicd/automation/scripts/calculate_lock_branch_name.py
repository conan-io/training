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
    '--source_branch_name_file', 
    default='source_branch_name.txt',
    help='directory to look for text file with name of source branch')
parser.add_argument(
    '--output_file',
    default='lock_branch_name.txt',
    help='text file to write value to')
            
args = parser.parse_args()

if os.path.isfile(args.source_branch_name_file):
    # This condition exists for promotion pipeline, where branch name written in file
    with open(args.source_branch_name_file, 'r') as file:
        source_branch_name = file.read().strip()


start_dir = os.getcwd()
os.chdir(args.conanfile_dir)

if source_branch_name is None:
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
