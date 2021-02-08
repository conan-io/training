import argparse
import os

# This script is used in both stages of the product pipeline
# It walks the provided search_dir to find all lockfile names processed in earlier stages.
# It writes the unique list of lockfiles to a text file which is read by other scripts later on.

parser = argparse.ArgumentParser()
parser.add_argument(
    'lockfile_base_dir',
    help='package base directory to start walking for lockfiles')
parser.add_argument(
    '--output_file',
    default="lockfile_names.txt",
    help='path of file to save list to.')
            
args = parser.parse_args()

lockfile_dir_list = []
max_depth = 6 # Only list lockfiles within the current package (skip lockfiles in deeper subdirs)
for root, dirs, files in os.walk(args.lockfile_base_dir):
    if root[len(args.lockfile_base_dir):].count(os.sep) < max_depth:
        for _file in files:
            if _file == "conan.lock":
                file_full = os.path.join(root, _file)
                lockfile_dir = "/".join(file_full.split("/")[4:7])
                if lockfile_dir not in lockfile_dir_list:
                    lockfile_dir_list.append(lockfile_dir)

with open(args.output_file, 'w') as file:
    file.write('\n'.join(lockfile_dir_list))

with open(args.output_file, 'r') as file:
    print(file.read())
