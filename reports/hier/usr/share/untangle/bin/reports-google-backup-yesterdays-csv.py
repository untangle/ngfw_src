#! /usr/bin/python3

import argparse
import os
import subprocess
import datetime
import socket
import re
import sys
import uvm

uvm = uvm.Uvm().getUvmContext()

def parse_args():
    parser = argparse.ArgumentParser(description="Upload daily csv report to Google Drive.")
    parser.add_argument('-d', '--dir', required=True, help='Remote Google Drive directory')
    parser.add_argument('-f', '--filename', help='Optional filename to upload')
    return parser.parse_args()

def sanitize_hostname():
    hostname = None
    # Try reading from /etc/hostname
    try:
        with open("/etc/hostname", "r") as f:
            hostname = f.read().strip()
    except FileNotFoundError:
        pass

    # Fallback to socket if /etc/hostname missing or empty
    if not hostname:
        hostname = socket.gethostname()
    hostname = re.sub(r'\s+', '_', hostname)  # replace whitespace with _
    hostname = hostname.replace('.', '_')     # replace . with _
    return hostname


def main():
    args = parse_args()

    yesterday = (datetime.datetime.now() - datetime.timedelta(days=1)).strftime('%Y_%m_%d')

    if args.filename:
        filename = args.filename
    else:
        hostname = sanitize_hostname()
        filename = f"{hostname}-reports_csv-{yesterday}.zip"

    filepath = f"/tmp/{filename}"

    print(f"Creating zip: {filepath}")
    cmd = ["/usr/share/untangle/bin/reports-create-csv.sh", "-r", yesterday, "-f", filepath, "-d", yesterday]
    print("Running:", " ".join(cmd))
    subprocess.run(cmd, check=True)

    # Upload to Google Drive
    code = uvm.googleManager().uploadToDrive(filepath, args.dir)

    # Clean up
    if os.path.exists(filepath):
        os.remove(filepath)
        print(f"Deleted: {filepath}")

    sys.exit(code)

if __name__ == "__main__":
    main()
