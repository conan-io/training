import json 
import argparse

# Script takes a path created from a reference
# and and extracts the reference

parser = argparse.ArgumentParser()
parser.add_argument(
    'reference', 
    help='reference')
            
args = parser.parse_args()
root_path = "/".join(args.reference.split("/")[0:4])
print(root_path)
