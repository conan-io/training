import json 
import argparse
import os
import shutil

# Copies all lockfiles which contain the given reference

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
                    if position == 0: # Only evalute the "first" position
                        for pref, prev, _context, _id in level:
                            pref_short = pref.split('@')[0]
                            if args.package_ref == pref_short:
                                upstream_package_ref = "/".join(src_root.split("/")[2:4])
                                lockfile_dir = "/".join(src_root.split("/")[4:]) 
                                dst_root = os.path.join(args.dst_dir, lockfile_dir, upstream_package_ref) 
                                print("copying: %s -> %s" % (src_root, dst_root))
                                shutil.copytree(src_root, dst_root)
