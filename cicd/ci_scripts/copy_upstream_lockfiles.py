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
    help='udestination directory to copy to')
            
args = parser.parse_args()

for src_root, dirs, files in os.walk(args.src_dir):
    dst_root = src_root.replace(args.src_dir, args.dst_dir)
    for _file in files:
        src_path_joined = os.path.join(src_root, _file)
        if _file == "build-order.json":
            with open(src_path_joined, 'r') as file:
                build_order = json.load(file)
                for position, level in enumerate(build_order):
                    for occurence in level:
                        for pref, prev, _context, _id in level:
                            pref_short = pref.split('@')[0]
                            if args.package_ref == pref_short:
                                print("copying: %s -> %s" % (src_root, dst_root))
                                print("args.dst_dir: %s " % (args.dst_dir))
                                # shutil.copytree(src_root, dst_root)