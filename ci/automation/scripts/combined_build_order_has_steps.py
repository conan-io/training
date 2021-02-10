import os
import json 
import argparse

# This script just returns true if the combined build order has any steps at all
# It is used as the condition to determine whether or not to run the Product pipeline

parser = argparse.ArgumentParser()

parser.add_argument(
    'lockfile_base_dir',
    help='package base directory to start walking for lockfiles')
    
parser.add_argument(
    '--combined_build_order_file',
    default='combined_build_order.json',
    help='combined build order json file to read.')


args = parser.parse_args()

cbo_file_full = os.path.join(args.lockfile_base_dir, args.combined_build_order_file)
with open(cbo_file_full, 'r') as json_file:
    combined_build_order = json.load(json_file)

print(str(bool(combined_build_order)))
