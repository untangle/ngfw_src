#! /usr/bin/python3

import argparse
import os
import subprocess
import datetime
import re
import socket
import sys
import tempfile
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

    hostname = re.sub(r'[^A-Za-z0-9_-]+', '_', hostname)
    return hostname

def parse_args():
    parser = argparse.ArgumentParser(description="Upload data backup to Google Drive.")
    parser.add_argument('-d', '--dir', required=True, help='Remote Google Drive directory')
    parser.add_argument('-f', '--filename', help='Optional .rbackup filename')
    return parser.parse_args()

def main():
    args = parse_args()
    yesterday = (datetime.datetime.now() - datetime.timedelta(days=1)).strftime('%Y_%m_%d')

    if args.filename:
        filename = args.filename
        if (os.path.basename(filename) != filename or
                not re.fullmatch(r"[A-Za-z0-9._-]+\.rbackup", filename, re.IGNORECASE)):
            raise ValueError("The backup filename must be a basename ending with .rbackup")
    else:
        hostname = sanitize_hostname()
        filename = f"{hostname}-reports_data-{yesterday}.rbackup"

    integrity_helper = "/usr/share/untangle/bin/reports-backup-integrity.py"
    backup_script = "/usr/share/untangle/bin/reports-create-backup-pgdump.sh"

    # TemporaryDirectory creates a private, unique directory. The raw archive
    # never leaves it; only the authenticated package is sent to Google Drive.
    with tempfile.TemporaryDirectory(prefix="reports-backup-") as temp_dir:
        raw_archive = os.path.join(temp_dir, "reports.pgdump")
        package = os.path.join(temp_dir, filename)

        print(f"Creating authenticated backup: {package}")
        subprocess.run([
            backup_script, "-r", f"*{yesterday}*", "-f", raw_archive,
        ], check=True)
        subprocess.run([
            integrity_helper, "create", "--archive", raw_archive,
            "--output", package,
        ], check=True)

        # Preserve the existing Google Drive result code for the caller.
        code = uvm.googleManager().uploadToDrive(package, args.dir)

    sys.exit(code)

if __name__ == "__main__":
    main()
