import json 
import argparse

# Reads a combined build order file and outputs the next level, one reference per line

parser = argparse.ArgumentParser()
parser.add_argument(
    'combined_build_order_file', 
    help='combined_build_order file')
parser.add_argument(
    '--output_file',
    default="next_build_order_level.txt",
    help='path of file to save list to.')
            
args = parser.parse_args()

#Print packages in next level, 1 line per package

with open(args.combined_build_order_file, 'r') as file:
    combined_build_order = json.load(file)  

build_order_levels = []
for key, values in combined_build_order.items():
    for value in values:
        if key == "0":
            build_order_levels.append((value.split('@')[0]))

with open(args.output_file, 'w') as file:
    file.writelines(build_order_levels)

with open(args.output_file, 'r') as file:
    print(file.read())
