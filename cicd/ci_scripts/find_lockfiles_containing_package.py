import json 
import argparse
import os
import shutil

# Script takes a reference and a directory and recursively
# removes all lockfiles which don't contain the reference

parser = argparse.ArgumentParser()
parser.add_argument(
    'package_ref', 
    help='package reference to search for')
parser.add_argument(
    'upstreams',
    help='upstream package names to search for')
            
args = parser.parse_args()

upstreams = args.upstreams.strip('][').split(', ')

for upstream in upstreams:
    upstream_root = os.path.join(root, upstream)

for root, dirs, files in os.walk(args.src_dir):
    for _file in files:
        src_path_joined = os.path.join(root, _file)
        dst_path_joined = src_path_joined.replace(args.src_dir,args.dst_dir)
        if _file == "conan-new.lock":
            os.makedirs(os.path.dirname(dst_path_joined))
            shutil.copy2(src_path_joined, dst_path_joined)
            with open(dst_path_joined, 'r') as file:
               lockfile = json.load(file)