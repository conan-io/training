import os
import json 
import argparse
import subprocess

# This script takes multiple build order files and correctly reconciles them to a single build order.
# It is used by the first part of the "product pipeline", to trigger the downstream builds. 
#
# This script is necessary because build-order.json files are only relevant to a single lockfile.
# CI jobs inherently deal with "groups of lockfiles" (and thus, groups of build-orders) in each run. 
# When triggering downstream jobs, we must trigger all the downstreams for all the lockfiles. 
# They key challenge is that build-orders can be different for different platforms.
# For example, we might end up with the following two build orders at the end of a job:
# For Windows:  liba -> libb -> libc -> libd
# For Linux  :  liba -> libd
# If we trigger libb and libd, right away, we will have a failure in the windows build of libd. 
# We cannot trigger libd until libb and libc have both been built. 
# This script solves that problem. 

parser = argparse.ArgumentParser()

parser.add_argument(
    'lockfile_base_dir',
    help='package base directory to start walking for lockfiles')
parser.add_argument(
    '--output_filename',
    default="combined_build_order.json",
    help='filename to save the new combined build order json to.')

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
                pref_short = pref.split("@")[0]
                po_dict.setdefault(pref_short, []).append(position)

#Choose worst of all indexes for each pref
for pref, positions in po_dict.items():
    po_dict[pref] = max(positions)

#Create new combined build order, a dict of level to prefs
combined_order = {}
for pref, position in po_dict.items():
    combined_order.setdefault(position, []).append(pref) 

os.makedirs(args.lockfile_base_dir, exist_ok=True)
output_file = os.path.join(args.lockfile_base_dir, args.output_filename)
with open(output_file, 'w') as file:
    json.dump(combined_order, file, indent=2)

with open(output_file, 'r') as file:
    print(file.read())
