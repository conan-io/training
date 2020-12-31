import json 
import argparse
import os
import subprocess

parser = argparse.ArgumentParser()
parser.add_argument(
    '--git_dir', 
    default='.',
    help='directory to look for conanfile')
parser.add_argument(
    '--output_file',
    default='source_branch_name.txt',
    help='text file to write value to')
            
args = parser.parse_args()

start_dir = os.getcwd()
os.chdir(args.git_dir)

current_branch = os.getenv('GIT_BRANCH') or \
    subprocess.check_output(
    "git rev-parse --abbrev-ref HEAD"
    , universal_newlines=True, shell=True).strip()

recent_commits = subprocess.check_output(
    "git log -100 --format=format:%H"
    , universal_newlines=True, shell=True).strip().split("\n")

source_branch = None
for commit in recent_commits:
    containing_branches = subprocess.check_output(
        "git branch -a --contains {} --format='%(refname:lstrip=3)'".format(commit)
        , universal_newlines=True, shell=True).strip().split("\n")

    for branch in containing_branches:
        if branch == "":
            continue
        if branch == "develop":
            continue
        if branch.startswith("PR-"):
            continue
        if branch.startswith("HEAD"):
            continue
        if branch.startswith("(HEAD"):
            continue
        source_branch = branch
        break
    if source_branch is not None:
        break
        
os.chdir(start_dir)

with open(args.output_file, 'w') as file:
    file.write(source_branch)

with open(args.output_file, 'r') as file:
    print(file.read())
