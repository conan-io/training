import os
import argparse
import shutil

# This script is used at the end of the promotion pipeline to cleanup the temporary working lockfile directories. 

parser = argparse.ArgumentParser()
parser.add_argument(
    'lockfile_dir',
    default='dev',
    help='Lockfile directory to be removed')
            
args = parser.parse_args()

shutil.rmtree(args.lockfile_dir)

