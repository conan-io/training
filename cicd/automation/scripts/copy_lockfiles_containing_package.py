import json 
import argparse
import os
import shutil
import subprocess

parser = argparse.ArgumentParser()
parser.add_argument(
    '--conanfile_dir', 
    default='.',
    help='directory to look for conanfile')
parser.add_argument(
    'src_dir',
    help='source directory to search')
parser.add_argument(
    'dst_dir',
    help='destination directory to copy to')
            
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
dst_root = os.path.join(args.dst_dir, name_and_version)

for src_root, dirs, files in os.walk(args.src_dir):
    dst_full = src_root.replace(args.src_dir, dst_root)
    if not os.path.isdir(dst_full):
        for _file in files:
            if _file == "conan.lock":
                file_full = os.path.join(src_root, _file)
                with open(file_full, 'r') as file:
                    lockfile = json.load(file)
                for node in lockfile["graph_lock"]["nodes"].values():
                    if name_and_version in node["ref"]:
                        print("copying: %s -> %s" % (src_root, dst_full))
                        shutil.copytree(src_root, dst_full)
