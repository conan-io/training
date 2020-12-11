import json 
import argparse

# Script takes multiple build order files and
# correctly reconciles them to a single build order

parser = argparse.ArgumentParser()
parser.add_argument(
    'build_order_files', 
    nargs='+',
    help='build order file')

parser.add_argument(
    '--output_file',
    help='output_file')

args = parser.parse_args()

# read build order files into list of dictionaries
build_orders = []
for _file in args.build_order_files:
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

with open(args.output_file, 'w') as file:
    json.dump(combined_order, file, indent=4)
