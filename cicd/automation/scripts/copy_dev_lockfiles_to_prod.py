import os
import argparse
import shutil

# searches dev lockfile directories and find those that exist in prod
# copies them to prod for promotion and produces a list in a text file

parser = argparse.ArgumentParser()
parser.add_argument(
    'prod_dir',
    help='root directory containing production lockfiles')
parser.add_argument(
    'dev_dir',
    help='root directory containing modified lockfiles')
parser.add_argument(
    '--lockfile_names_file',
    default="lockfile_names.txt",
    help='file with list of lockfile names to be copied')

args = parser.parse_args()

with open(args.lockfile_names_file, 'r') as file:
    prod_lockfiles_names = file.read().split("\n")

for prod_lockfiles_name in prod_lockfiles_names:
    print(prod_lockfiles_name)
    pkg_name_and_ver = "/".join(prod_lockfiles_name.split("/")[:-1])
    dev_lockfile_path = os.path.join(args.dev_dir, pkg_name_and_ver, prod_lockfiles_name, "conan-new.lock")
    prod_lockfile_path = os.path.join(args.prod_dir, prod_lockfiles_name, "conan.lock")
    print("copying {} -> {}".format(dev_lockfile_path, prod_lockfile_path))
    shutil.copyfile(dev_lockfile_path, prod_lockfile_path)

