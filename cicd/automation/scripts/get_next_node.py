import json 
import argparse

# Script takes multiple build order files and
# correctly reconciles them to a single build order

parser = argparse.ArgumentParser()
parser.add_argument(
    'combined_build_order_file', 
    help='combined_build_order file')
            
args = parser.parse_args()

#Print packages in next level, 1 line per package

with open(args.combined_build_order_file, 'r') as file:
    combined_build_order = json.load(file)  

for key, values in combined_build_order.items():
    for value in values:
        print(value.split('@')[0])
    exit(0)
