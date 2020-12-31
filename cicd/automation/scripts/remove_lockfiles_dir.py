import os
import argparse
import shutil

parser = argparse.ArgumentParser()
parser.add_argument(
    'lockfile_dir',
    default='dev',
    help='Lockfile directory to be removed')
            
args = parser.parse_args()

shutil.rmtree(args.lockfile_dir)

