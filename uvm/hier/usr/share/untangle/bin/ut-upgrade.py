#!/usr/bin/python3 -u
# $Id$#!/usr/bin/python3

import getopt
import sys
import os
import os.path
import signal
import re
import subprocess
import tempfile
import time
import logging
import platform
from uvm import Uvm
from uvm import disk_health

# set noninteractive mode for all apt-get calls
os.environ['DEBIAN_FRONTEND'] = 'noninteractive'
# set the path (in case its run from cron)
os.environ['PATH'] = '/usr/local/bin:/usr/local/sbin:/usr/bin:/usr/sbin:/bin:/sbin:' + os.environ['PATH']

# apt-get options for various commands
INSTALL_OPTS = " -o DPkg::Options::=--force-confnew -o DPkg::Options::=--force-confmiss --yes --force-yes --fix-broken --purge "
UPGRADE_OPTS = " -o DPkg::Options::=--force-confnew -o DPkg::Options::=--force-confmiss --yes --force-yes --fix-broken --purge -o Debug::Pkgproblemresolver=1 -o Debug::Pkgpolicy=1 "
UPDATE_OPTS = " --yes --force-yes --allow-releaseinfo-change"
AUTOREMOVE_OPTS = " --yes --force-yes --purge "
QUIET=False

# Ignore SIGHUP from parent (this is in case we get launched by the UVM, and then it exits)
# This isn't enough because this doesn't modify sigprocmask so children of this process still get it
signal.signal( signal.SIGHUP, signal.SIG_IGN )
# Detach from parent (so it won't send us signals like SIGHUP)
# This should shield us and children from SIGHUPs
os.setpgrp()

def printUsage():
    sys.stderr.write( """\
%s Usage:
  optional args:
    -q   : quiet
""" % (sys.argv[0]) )
    sys.exit(1)


upgrade_log = open("/var/log/uvm/upgrade.log", "a")

try:
     opts, args = getopt.getopt(sys.argv[1:], "q", ['quiet'])
except getopt.GetoptError as err:
     print(str(err))
     printUsage()
     sys.exit(2)

for opt in opts:
     k, v = opt
     if k == '-q' or k == '--quiet':
         QUIET = True

def log(str):
    # wrap all attempts in try/except
    # if the parent dies, stdout might product a sigpipe
    # see we need to be careful with "print"
    try:
        upgrade_log.write(str + "\n")
        upgrade_log.flush()
    except:
        pass
    try:
        if not QUIET:
            print(str)
    except:
        pass

def log_date( cmd ):
    p = subprocess.Popen(["date","+%Y-%m-%d %H:%M"], stdout=subprocess.PIPE, text=True)
    for line in iter(p.stdout.readline, ''):
        log( line.strip() + " " + cmd)
    p.wait()
    return p.returncode

def cmd_to_log(cmd):
    stdin=open(os.devnull, 'rb')
    p = subprocess.Popen(["sh","-c","%s 2>&1" % (cmd)], stdout=subprocess.PIPE, stdin=stdin, text=True)
    for line in iter(p.stdout.readline, ''):
        log( line.strip() )
    p.wait()
    return p.returncode

def update():
    log("apt-get update %s" % UPDATE_OPTS)
    p = subprocess.Popen(["sh","-c","apt-get update %s 2>&1" % UPDATE_OPTS], stdout=subprocess.PIPE, text=True)
    for line in iter(p.stdout.readline, ''):
        if not re.search('^W: (Conflicting distribution|You may want to run apt-get update to correct these problems)', line):
            log( line.strip() )
    p.wait()
    return p.returncode

def check_upgrade():
    p = subprocess.Popen(["sh","-c","apt-get -s dist-upgrade %s 2>&1" % UPGRADE_OPTS], stdout=subprocess.PIPE, text=True)
    for line in iter(p.stdout.readline, ''):
        if re.search('.*been kept back.*', line):
            log( "Packages have been kept back.\n" )
            return 1
    p.wait()
    return p.returncode

def upgrade():
    log("apt-get dist-upgrade %s" % UPGRADE_OPTS)
    return cmd_to_log("apt-get dist-upgrade %s" % UPGRADE_OPTS)

def autoremove():
    log("apt-get autoremove %s" % AUTOREMOVE_OPTS)
    return cmd_to_log("apt-get autoremove %s" % AUTOREMOVE_OPTS)

def clean():
    log("apt-get clean")
    return cmd_to_log("apt-get clean")

def depmod():
    """
    Call depmod -a to ensure we have up-to-date module information
    for any kernels we may have just installed
    """
    log("depmod -a")
    return cmd_to_log("depmod -a")

def check_dpkg():
    dpkg_avail = '/var/lib/dpkg/available'

    log("Checking if " + dpkg_avail + " exists...")
    if os.path.exists(dpkg_avail) :
        log(dpkg_avail + " exists.")
        return
    else:
        log(dpkg_avail + " is missing, attempting to create...")
        dpkg_avail_create = "cat /var/lib/apt/lists/*_Packages >"+dpkg_avail
        dpkg_config = "dpkg --configure -a"
        log(dpkg_avail_create)
        log(dpkg_config)
        cmd_to_log(dpkg_avail_create)
        cmd_to_log(dpkg_config)
        return

def check_disk_health():
    try:
        system_manager = Uvm().getUvmContext().systemManager()
        if not system_manager:
            log("System Manager is not available. Proceeding with upgrade.")
            return
        skip_health_check = system_manager.isSkipDiskCheck()
        if skip_health_check:
            log("Skipping drive health checks. Proceeding with upgrade.")
            return

        health_status = disk_health.check_smart_health()
        log(f"disk health status: {health_status}")
        if "fail" in health_status:
            system_manager.logDiskCheckFailure(str(health_status))
            log("Disk health check failed, Aborting Upgrade.\n")
            sys.exit(1)
    except Exception as e:
        log(f"Disk health check encountered an error {e}, proceeding with upgrade.")

# ---- Bookworm upgrade helpers ---- #

def is_bookworm_upgrade():
    """
    Detect if apt sources point to bookworm while the running system
    is still bullseye (or older). This means a major version upgrade
    is about to happen or has just happened.
    Returns True only when the upgrade target is bookworm.
    """
    # Check if any apt source references bookworm
    sources_have_bookworm = False
    sources_dirs = ["/etc/apt/sources.list.d/"]
    sources_files = ["/etc/apt/sources.list"]
    for d in sources_dirs:
        if os.path.isdir(d):
            for f in os.listdir(d):
                fp = os.path.join(d, f)
                if os.path.isfile(fp):
                    sources_files.append(fp)
    for sf in sources_files:
        try:
            with open(sf) as fh:
                for line in fh:
                    if 'bookworm' in line and not line.strip().startswith('#'):
                        sources_have_bookworm = True
                        break
        except:
            pass
        if sources_have_bookworm:
            break

    if not sources_have_bookworm:
        return False

    # If we already booted into a 6.x kernel, the major upgrade has
    # already completed (post-reboot run) — still run the fixups in
    # case they didn't finish last time.
    running_kernel = platform.release()  # e.g. "5.10.0-27-untangle-amd64"
    if running_kernel.startswith("5.") or running_kernel.startswith("4."):
        log("Bookworm upgrade detected: sources point to bookworm, running kernel %s" % running_kernel)
        return True

    # Already on 6.x kernel — check if post-upgrade fixups are still needed
    # (e.g. first boot after dist-upgrade, sync-settings hasn't run yet)
    if not os.path.exists("/usr/sbin/nft"):
        log("Bookworm post-upgrade fixups needed: nft binary missing on kernel %s" % running_kernel)
        return True

    # Check if nft ruleset has the expected tables
    try:
        result = subprocess.run(["/usr/sbin/nft", "list", "tables"],
                                capture_output=True, text=True, timeout=5)
        if "inet tune" not in result.stdout or "bridge broute" not in result.stdout:
            log("Bookworm post-upgrade fixups needed: nft tables incomplete on kernel %s" % running_kernel)
            return True
    except:
        pass

    log("Bookworm upgrade: system appears fully migrated on kernel %s" % running_kernel)
    return False

def pre_upgrade_cleanup():
    """
    Remove packages that conflict with Bookworm before dist-upgrade.
    This prevents dpkg errors during the main upgrade.
    """
    log("Pre-upgrade: removing Bullseye packages that conflict with Bookworm")

    # wireguard-dkms: BUILD_EXCLUSIVE mismatch with kernel 6.1
    # Bookworm kernel has wireguard built-in, DKMS module not needed
    cmd_to_log("dpkg --list wireguard-dkms 2>/dev/null | grep -q '^ii' && apt-get purge -y wireguard-dkms || true")

    # Ensure nftables is available before the upgrade so post-install
    # scripts that reference nft don't fail
    log("Pre-upgrade: installing nftables")
    cmd_to_log("apt-get install -y nftables")

    # Preserve wizard-complete flag — on the Bullseye system the wizard
    # was already completed; the upgrade must not force the user through
    # the setup wizard again.
    wizard_flag = "/usr/share/untangle/conf/wizard-complete"
    if os.path.exists(wizard_flag):
        log("Pre-upgrade: wizard-complete flag exists, will be preserved")
    else:
        # If running from a configured system the flag should exist.
        # Create it now so the upgraded system doesn't show the wizard.
        log("Pre-upgrade: creating wizard-complete flag")
        try:
            os.makedirs(os.path.dirname(wizard_flag), exist_ok=True)
            with open(wizard_flag, "w") as f:
                f.write("upgrade\n")
        except:
            log("Pre-upgrade: WARNING - could not create wizard-complete flag")

def post_upgrade_fixups():
    """
    After dist-upgrade to Bookworm, set up components that the upgrade
    doesn't handle automatically:
    - IFB device (replaces IMQ, kernel 6.1 doesn't auto-create)
    - sync-settings (regenerates nft rules, bridge pipeline, etc.)
    - Validate nft ruleset
    """
    log("Post-upgrade: configuring Bookworm runtime")

    # Configure any half-installed packages left over from upgrade
    log("Post-upgrade: configuring pending packages")
    cmd_to_log("dpkg --configure -a")

    # IFB device: kernel 6.1 modprobe ifb doesn't auto-create devices.
    # Also ensure the module loads on boot so IFB survives reboot.
    log("Post-upgrade: ensuring IFB module loads on boot")
    try:
        ifb_in_modules = False
        if os.path.exists("/etc/modules"):
            with open("/etc/modules") as f:
                for line in f:
                    if line.strip() == "ifb":
                        ifb_in_modules = True
                        break
        if not ifb_in_modules:
            with open("/etc/modules", "a") as f:
                f.write("ifb\n")
            log("Post-upgrade: added 'ifb' to /etc/modules")
        else:
            log("Post-upgrade: 'ifb' already in /etc/modules")
    except:
        log("Post-upgrade: WARNING - could not update /etc/modules")

    log("Post-upgrade: creating IFB device")
    cmd_to_log("modprobe ifb 2>/dev/null || true")
    cmd_to_log("ip link add ifb0 type ifb 2>/dev/null || true")
    cmd_to_log("ip link set ifb0 up 2>/dev/null || true")

    # Regenerate all network rules (iptables, nftables bridge/broute/tune)
    log("Post-upgrade: running sync-settings to regenerate rules")
    cmd_to_log("sync-settings || true")

    # Validate nft ruleset
    log("Post-upgrade: validating nftables ruleset")
    cmd_to_log("/usr/sbin/nft list ruleset 2>&1 | head -50 || true")

    # Check for required nft tables
    result = subprocess.run("nft list tables 2>/dev/null || true",
                            shell=True, capture_output=True, text=True)
    for table in ["bridge broute", "bridge mangle", "inet tune"]:
        if table in result.stdout:
            log("Post-upgrade: OK - %s present" % table)
        else:
            log("Post-upgrade: WARNING - %s missing" % table)

    # Flag that reboot is needed if still on old kernel
    running_kernel = platform.release()
    if running_kernel.startswith("5.") or running_kernel.startswith("4."):
        log("")
        log("=" * 60)
        log("REBOOT REQUIRED")
        log("Bookworm packages installed but still running kernel %s" % running_kernel)
        log("Reboot to activate kernel 6.1 and complete the migration.")
        log("=" * 60)
        log("")
        # Write a flag file so the UI/admin can detect this
        try:
            with open("/tmp/.bookworm-reboot-required", "w") as f:
                f.write("Bookworm upgrade completed, reboot required\n")
        except:
            pass

# ---- Main flow ---- #

log_date( os.path.basename( sys.argv[0]) )

log("")
check_disk_health()
log("")

check_dpkg()
log("")

update()

if "2.6.32" in platform.platform():
    log("Upgrade(s) are not allowed on the 2.6.32 kernel. Please reboot and select a newer kernel.")
    sys.exit(1)

log_date("")
log("")

# Detect if this is a Bookworm major version upgrade
bookworm_upgrade = is_bookworm_upgrade()

if bookworm_upgrade:
    log("")
    log("=" * 60)
    log("MAJOR VERSION UPGRADE: Bullseye -> Bookworm detected")
    log("=" * 60)
    log("")
    pre_upgrade_cleanup()
    log("")

r = check_upgrade();
if r != 0:
    log("apt-get -s dist-upgrade returned an error (%i). Abort." % r)
    sys.exit(1)

r = upgrade()

log_date("")
log("")

if r != 0:
    log("dist-upgrade returned error (%i), attempting to fix..." % r)
    # Try to configure any partially installed packages
    cmd_to_log("dpkg --configure -a")
    # Retry once
    r = upgrade()
    if r != 0:
        log("dist-upgrade retry also failed (%i). Check upgrade.log for details." % r)
        # Continue anyway — autoremove/depmod may still help

autoremove()

log_date("")
log("")

if bookworm_upgrade:
    post_upgrade_fixups()
    log("")

clean()

log_date("")
log("")

depmod()

log_date("")
log("")

log_date( os.path.basename( sys.argv[0]) + " done." )

sys.exit(0)
