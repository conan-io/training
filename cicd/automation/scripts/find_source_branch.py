import json 
import argparse
import os
import subprocess


# This script uses the current commit hash to identify the original feature branch name
# This script is designed specifically to be used in the promotion pipeline. 
# 
# When a PR is merged into develop, there is no CI mechanism to get the original feature branch name.
# Our promotion process depends on knowing the feature branch name at that time.
# Fortunately, it's actually fairly simple with GIT to identify the original feature branch name. 
# We use git log to go back through some number of commits (100 for our demo)
# For each commit, we go through all the branches that have that commit in their history
# Once we find a commit in a branch which isn't develop, PR-*, or HEAD, we break. 
# That is the name of the feature branch which preceded the PR.
#
# When complete, this script writes a text file with the original feature branch name. 
# That file is used by "calculate_lock_branch_name.py"

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
        if branch == "conan_from_upstream":
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
    else:
        source_branch = current_branch
        
os.chdir(start_dir)

with open(args.output_file, 'w') as file:
    file.write(source_branch)

with open(args.output_file, 'r') as file:
    print(file.read())
