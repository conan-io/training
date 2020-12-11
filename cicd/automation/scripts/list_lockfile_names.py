import argparse
import os

parser = argparse.ArgumentParser()
parser.add_argument(
    'search_dir',
    help='directory to search')
parser.add_argument(
    '--output_file',
    default="lockfile_names.txt",
    help='path of file to save list to.')
            
args = parser.parse_args()

lockfile_dir_list = []
for root, dirs, files in os.walk(args.search_dir):
    for _file in files:
        if _file == "conan.lock":
            file_full = os.path.join(root, _file)
            lockfile_dir = "/".join(file_full.split("/")[4:7])
            lockfile_dir_list.append(lockfile_dir)

with open(args.output_file, 'w') as file:
    file.writelines(lockfile_dir_list)

with open(args.output_file, 'r') as file:
    print(file.read())
