import os
import argparse
import shutil

# searches dev lockfile directories and find those that exist in prod
# copies them to prod for promotion and produces a list in a text file

parser = argparse.ArgumentParser()
parser.add_argument(
    'prod_dir',
    help='root directory containing production lockfiles')
parser.add_argument(
    'dev_dir',
    help='root directory containing modified lockfiles')
parser.add_argument(
    '--output_file',
    default="lockfile_names.txt",
    help='path of file to save list to.')

args = parser.parse_args()

max_depth = 6 # Only capture lockfiles in root level package folders (skip lockfiles in deeper subdirs)

prod_lockfiles = []
promotion_list = []
for root, dirs, files in os.walk(args.dev_dir):
    if root[len(args.dev_dir):].count(os.sep) < max_depth:
        for _file in files:
            if _file == "conan-new.lock":
                pkg_dir = "/".join(root.split("/")[-5:-3])
                lockfile_dir = "/".join(root.split("/")[-3:])
                if lockfile_dir.startswith(pkg_dir):
                    # Then this is a lockfile_dir of a product (app1) as opposed to 
                    # a lockfile in an lockfile_dir of an intermediate lib (libd)
                    prod_lockfiles.append(lockfile_dir)
                    dev_lockfile_path = os.path.join(args.dev_dir, pkg_dir, lockfile_dir, "conan-new.lock")
                    prod_lockfile_path = os.path.join(args.prod_dir, lockfile_dir, "conan.lock")
                    print("copying {} -> {}".format(dev_lockfile_path, prod_lockfile_path))
                    shutil.copyfile(dev_lockfile_path, prod_lockfile_path)

with open(args.output_file, 'w') as file:
    file.write('\n'.join(prod_lockfiles))

with open(args.output_file, 'r') as file:
    print(file.read())
