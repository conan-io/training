import os
import argparse
import shutil

# This script is used in the promotion pipeline. 
# It walks the provided dev_dir to find all lockfile names processed in earlier stages.
# It finds the ones which represent "fully completed product lockfiles". 
# These are easily recognizable by their paths, because the job folder matches the lockfile folder.
# Example:  app1/1.0/app1/1.0/gcc-7/conan-new.lock
# Notice that app1/1.0 appears twice in a row. 
# We write such paths to the text file below, and that will be used later in the pipeline. 

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
