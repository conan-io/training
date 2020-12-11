import json 
import argparse

# Script takes multiple build order files and
# correctly reconciles them to a single build order

parser = argparse.ArgumentParser()
parser.add_argument(
    'build_order_files', 
    nargs='+',
    help='build order files')
            
args = parser.parse_args()
# read build order files into list of dictionaries
build_orders = []
for _file in args.build_order_files:
    with open(_file, 'r') as file:
        build_orders.append(json.load(file))

final_nodes = []
for build_order in build_orders:
    final_nodes.append(build_order[-1][0][1].split('#')[0])

for node in set(final_nodes):
    print(node)
