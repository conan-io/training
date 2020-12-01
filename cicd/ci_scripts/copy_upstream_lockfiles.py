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
    'src_dir',
    help='source directory to search')
parser.add_argument(
    'dst_dir',
    help='destination directory to copy to')
            
args = parser.parse_args()

for src_root, dirs, files in os.walk(args.src_dir):
    for _file in files:
        src_path_joined = os.path.join(src_root, _file)
        if _file == "build-order.json":
            with open(src_path_joined, 'r') as file:
                build_order = json.load(file)
                for position, level in enumerate(build_order):
                    for pref, prev, _context, _id in level:
                        pref_short = pref.split('@')[0]
                        if args.package_ref == pref_short:
                            _suffix = "/".join(src_root.split("/")[4:]) 
                            dst_root = args.dst_dir + "/" + _suffix
                            print("copying: %s -> %s" % (src_root, dst_root))
                            shutil.copytree(src_root, dst_root)