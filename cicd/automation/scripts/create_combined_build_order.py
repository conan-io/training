import os
import json 
import argparse
import subprocess

# Script takes multiple build order files and
# correctly reconciles them to a single build order

parser = argparse.ArgumentParser()

parser.add_argument(
    'lockfile_base_dir',
    help='package base directory to start walking for lockfiles')

args = parser.parse_args()

max_depth = 6 # Only list lockfiles within the current package (skip lockfiles in deeper subdirs)

new_lockfiles = []
for src_root, dirs, files in os.walk(args.lockfile_base_dir):
    if src_root[len(args.lockfile_base_dir):].count(os.sep) < max_depth:
        for _file in files:
            if _file == "conan-new.lock":
                new_lockfiles.append(os.path.join(src_root, _file))
                
bofs = []
for new_lockfile in new_lockfiles:
    bof = new_lockfile.replace("conan-new.lock", "build-order.json")
    bofs.append(bof)
    subprocess.check_output(
        "conan lock build-order --json {} {}".format(bof, new_lockfile), 
        universal_newlines=True, 
        shell=True)

# read build order files into list of dictionaries
build_orders = []
for _file in bofs:
    with open(_file, 'r') as file:
        build_orders.append(json.load(file))  

po_dict = {}

#Create a dict of pref to a list of positions it was found in
for build_order in build_orders:
    for position, level in enumerate(build_order):
        for occurence in level:
            for pref, prev, _context, _id in level:
                po_dict.setdefault(pref, []).append(position)

#Choose worst of all indexes for each pref
for pref, positions in po_dict.items():
    po_dict[pref] = max(positions)

#Create new combined build order, a dict of level to prefs
combined_order = {}
for pref, position in po_dict.items():
    combined_order.setdefault(position, []).append(pref) 

output_file = os.path.join(args.lockfile_base_dir, "combined_build_order.json")
with open(output_file, 'w') as file:
    json.dump(combined_order, file, indent=4)

with open(output_file, 'r') as file:
    print(file.read())
