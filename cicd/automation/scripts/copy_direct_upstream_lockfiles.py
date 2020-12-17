import json 
import argparse
import os
import shutil
import subprocess


# Copies all lockfiles of packages which are directly upstream from the provided one

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

max_depth = 6 # Only list lockfiles within the current package (skip lockfiles in deeper subdirs)

shutil.rmtree(args.dst_dir, ignore_errors=True)

for src_root, dirs, files in os.walk(args.src_dir):
    if src_root[len(args.src_dir):].count(os.sep) < max_depth:
        for _file in files:
            src_path_joined = os.path.join(src_root, _file)
            if _file == "build-order.json":
                with open(src_path_joined, 'r') as file:
                    build_order = json.load(file)
                for position, level in enumerate(build_order):
                    if position == 0: # Only evalute the "first" position
                        for pref, prev, _context, _id in level:
                            pref_short = pref.split('@')[0]
                            if name_and_version == pref_short:
                                lockfile_dir = "/".join(src_root.split("/")[4:])
                                dst_dir_suffix = "/".join(src_root.split("/")[2:4])
                                os.makedirs(os.path.join(args.dst_dir, lockfile_dir))
                                dst_full = os.path.join(args.dst_dir, lockfile_dir, dst_dir_suffix)
                                shutil.rmtree(dst_full, ignore_errors=True)
                                print("copying: %s -> %s" % (src_root, dst_full))
                                shutil.copytree(src_root, dst_full)
