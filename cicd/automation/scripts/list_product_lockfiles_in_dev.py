import os
import argparse
import shutil

# searches dev lockfile directories and find those that exist in prod

parser = argparse.ArgumentParser()
parser.add_argument(
    'dev_dir',
    help='root directory containing modified lockfiles')
parser.add_argument(
    '--output_file',
    default="lockfile_names.txt",
    help='path of file to save list to.')

args = parser.parse_args()

max_depth = 6 # Only evaluate lockfiles in root level package folders (skip lockfiles in deeper subdirs)

prod_lockfiles = []
promotion_list = []
for root, dirs, files in os.walk(args.dev_dir):
    if root[len(args.dev_dir):].count(os.sep) < max_depth:
        for _file in files:
            if _file == "conan-new.lock":
                pkg_dir = "/".join(root.split("/")[-5:-3])
                lockfile_dir = "/".join(root.split("/")[-3:])
                if lockfile_dir.startswith(pkg_dir):
                    prod_lockfiles.append(lockfile_dir)

with open(args.output_file, 'w') as file:
    file.write('\n'.join(prod_lockfiles))

with open(args.output_file, 'r') as file:
    print(file.read())
