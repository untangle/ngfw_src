#! /usr/bin/python3

import argparse
import os
import subprocess
import datetime
import re
import socket
import sys
import uvm

uvm = uvm.Uvm().getUvmContext()

def sanitize_hostname():
    hostname = None

    try:
        with open("/etc/hostname", "r") as f:
            hostname = f.read().strip()
    except FileNotFoundError:
        pass

    if not hostname:
        hostname = socket.gethostname()

    hostname = re.sub(r'\s+', '_', hostname)
    hostname = hostname.replace('.', '_')
    return hostname

def parse_args():
    parser = argparse.ArgumentParser(description="Upload data backup to Google Drive.")
    parser.add_argument('-d', '--dir', required=True, help='Remote Google Drive directory')
    parser.add_argument('-f', '--filename', help='Optional backup filename')
    return parser.parse_args()

def main():
    args = parse_args()
    yesterday = (datetime.datetime.now() - datetime.timedelta(days=1)).strftime('%Y_%m_%d')

    if args.filename:
        filename = args.filename
    else:
        hostname = sanitize_hostname()
        filename = f"{hostname}-reports_data-{yesterday}.sql.gz"

    filepath = f"/tmp/{filename}"

    print(f"Creating backup: {filepath}")
    cmd = ["/usr/share/untangle/bin/reports-create-backup.sh", "-r", f"*{yesterday}*", "-f", filepath]
    print("Running:", " ".join(cmd))
    subprocess.run(cmd, check=True)

    # Upload to Google Drive using the CLI
    code = uvm.googleManager().uploadToDrive(filepath, args.dir)

    # Cleanup
    if os.path.exists(filepath):
        os.remove(filepath)
        print(f"Deleted: {filepath}")

    sys.exit(code)

if __name__ == "__main__":
    main()
