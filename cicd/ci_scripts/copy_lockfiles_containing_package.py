import json 
import argparse
import os
import shutil

parser = argparse.ArgumentParser()
parser.add_argument(
    'package_ref', 
    help='package reference to search for')
parser.add_argument(
    'src_dir',
    help='source directory to search')
parser.add_argument(
    'dst_dir',
    help='udestination directory to copy to')
            
args = parser.parse_args()

for src_root, dirs, files in os.walk(args.src_dir):
    dst_root = src_root.replace(args.src_dir, args.dst_dir)
    for _file in files:
        if _file == "conan.lock":
            file_full = os.path.join(src_root, _file)
            with open(file_full, 'r') as file:
                lockfile = json.load(file)
                for node in lockfile["graph_lock"]["nodes"].values():
                    if args.package_ref in node["ref"]:
                        print("copying: %s -> %s" % (src_root, dst_root))
                        shutil.copytree(src_root, dst_root)
