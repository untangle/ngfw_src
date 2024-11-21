from psutil import disk_partitions
from subprocess import run, CalledProcessError
import re
from collections import defaultdict

# Constants for Messages
ROOT_NOT_FOUND = "Root partition not found."
SMART_UNSUPPORTED_KEYWORDS = ["Unavailable", "not supported", "lacks SMART capability"]
SMART_UNSUPPORTED = "SMART support is: Unavailable - device lacks SMART capability"
SMART_PASS_KEYWORD = "PASSED"
SMART_FAIL_KEYWORD = "FAILED"
SMART_RESULT = "SMART overall-health self-assessment test result:"
SMART_ERROR = "Failed to run smartctl on"

# Health status dictionary
status = defaultdict(list)

def update_status(level, msg):
    """Update status dictionary."""
    status[level].append(msg)

def get_root_disk():
    """Identify the root disk by finding the '/' mount point."""
    root_part = next((part for part in disk_partitions(all=False) if part.mountpoint == '/'), None)
    if not root_part:
        update_status("error", ROOT_NOT_FOUND)
        return None
    return re.sub(r'p?\d+$', '', root_part.device)

def run_smart_check(disk):
    """Perform a SMART health check on the specified disk."""
    try:
        result = run(["smartctl", "-H", disk], capture_output=True, text=True, check=True)
        output = result.stdout
    except CalledProcessError as e:
        output = e.stdout or e.stderr
        msg = SMART_UNSUPPORTED if any(keyword in output for keyword in SMART_UNSUPPORTED_KEYWORDS) else f"{SMART_ERROR} {disk}: {output}"
        update_status("error", msg)
        return

    if SMART_PASS_KEYWORD in output:
        update_status("pass", f"{SMART_RESULT} {SMART_PASS_KEYWORD}")
    elif SMART_FAIL_KEYWORD in output:
        update_status("fail", f"{SMART_RESULT} {SMART_FAIL_KEYWORD}")
    else:
        update_status("error", f"{SMART_RESULT} UNKNOWN")

def check_smart_health():
    """Find and check the SMART health of the root disk."""
    root_disk = get_root_disk()
    if root_disk:
        run_smart_check(root_disk)
    return dict(status)  # Convert defaultdict back to dict for compatibility

if __name__ == "__main__":
    print(check_smart_health())