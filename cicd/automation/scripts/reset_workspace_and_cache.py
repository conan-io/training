import argparse
import os

parser = argparse.ArgumentParser()
parser.add_argument(
    '--workspace_path', 
    default=os.path.join(os.path.expanduser("~"),"workspace"),
    help='path of workspace to reset')
            
args = parser.parse_args()
os.chdir(args.workspace_path)
os.system("rm -rf *")
os.system("conan remove '*' -f")