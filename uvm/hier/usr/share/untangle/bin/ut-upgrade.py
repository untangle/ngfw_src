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
    """
    Simulate dist-upgrade.
    Returns:
      0  = clean, ok to proceed
      1  = packages kept back (caller decides whether to abort)
      >1 = apt-get -s returned a real error
    """
    p = subprocess.Popen(["sh","-c","apt-get -s dist-upgrade %s 2>&1" % UPGRADE_OPTS], stdout=subprocess.PIPE, text=True)
    kept_back = False
    for line in iter(p.stdout.readline, ''):
        if re.search('.*been kept back.*', line):
            log( "Packages have been kept back.\n" )
            kept_back = True
    p.wait()
    if p.returncode != 0:
        return p.returncode if p.returncode > 1 else 2
    if kept_back:
        return 1
    return 0

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

# ---- Trixie upgrade helpers ---- #

def is_trixie_upgrade():
    """
    Detect if apt sources point to trixie while the system is still on
    bookworm kernel (6.1.x) — or just rebooted into trixie kernel (6.12.x)
    with post-upgrade fixups not yet completed.
    Returns True only when the upgrade target is trixie and fixups are needed.
    """
    sources_have_trixie = False
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
                    if 'trixie' in line and not line.strip().startswith('#'):
                        sources_have_trixie = True
                        break
        except:
            pass
        if sources_have_trixie:
            break

    if not sources_have_trixie:
        return False

    # Pre-reboot: bookworm 6.1.x kernel still running, dist-upgrade needed
    running_kernel = platform.release()
    if running_kernel.startswith("6.1.") or running_kernel.startswith("5.") or running_kernel.startswith("4."):
        log("Trixie upgrade detected: sources point to trixie, running kernel %s" % running_kernel)
        return True

    # Post-reboot: trixie 6.12.x kernel active, check whether fixups already ran
    fixup_done_flag = "/var/lib/untangle-vm/.trixie-upgrade-fixups-done"
    if not os.path.exists(fixup_done_flag):
        log("Trixie post-upgrade fixups needed: flag file missing on kernel %s" % running_kernel)
        return True

    log("Trixie upgrade: system appears fully migrated on kernel %s" % running_kernel)
    return False

def pre_upgrade_cleanup_trixie():
    """
    Pre-upgrade fixups for bookworm->trixie:
    - Pre-install openjdk-21-jre-headless. trixie untangle-vm needs JDK21 for
      SSL Inspector compat (NGFW-15749 afe4c8650a) but untangle-vm's Depends
      doesn't hard-pull it in, so apt would otherwise keep stale openjdk-17.
    - Preserve wizard-complete flag so the setup wizard doesn't re-appear.
    """
    log("Pre-upgrade: Trixie target detected -- installing prerequisites")

    log("Pre-upgrade: pre-installing openjdk-21-jre-headless (required for SSL Inspector JDK21 compat)")
    cmd_to_log("apt-get install -y --no-install-recommends openjdk-21-jre-headless")

    wizard_flag = "/usr/share/untangle/conf/wizard-complete"
    if os.path.exists(wizard_flag):
        log("Pre-upgrade: wizard-complete flag exists, will be preserved")
    else:
        log("Pre-upgrade: creating wizard-complete flag")
        try:
            os.makedirs(os.path.dirname(wizard_flag), exist_ok=True)
            with open(wizard_flag, "w") as f:
                f.write("upgrade\n")
        except:
            log("Pre-upgrade: WARNING - could not create wizard-complete flag")

def post_upgrade_fixups_trixie():
    """
    Post-upgrade fixups for bookworm->trixie:
    - dpkg --configure -a to finish any half-installed packages.
    - sync-settings to regenerate trixie-specific configs.
    - Wait for deferred postinst daemon-reload cascade to settle (~30s).
      Multiple package postinsts each invoke `systemctl daemon-reload`,
      and the cumulative effect auto-restarts untangle-vm. That restart
      hits a JDK21+jabsorb parallel-load race where MarshallingModeContext.pop()
      throws NoSuchElementException and ~3 apps fail to init (observed:
      tunnel-vpn, intrusion-prevention). A clean stop+start cures it.
    - Mark fixups done so subsequent ut-upgrade.py runs skip them.
    """
    log("Post-upgrade: Trixie runtime configuration")

    log("Post-upgrade: configuring pending packages")
    cmd_to_log("dpkg --configure -a")

    log("Post-upgrade: regenerating runtime configs via sync-settings")
    cmd_to_log("sync-settings || true")

    log("Post-upgrade: refreshing PG collation metadata (glibc 2.36 -> 2.41 on trixie changes collation version)")
    # Ensure PostgreSQL is up before REFRESH. dpkg --configure / sync-settings can transition
    # postgresql.service through stop/start during trixie postinst; if we hit it mid-restart the
    # psql calls fail with "connection to server on socket failed: No such file or directory".
    cmd_to_log("systemctl start postgresql || true")
    cmd_to_log("for i in $(seq 1 30); do pg_isready -q && break; sleep 1; done")
    cmd_to_log("su - postgres -c \"psql -d uvm -c 'REINDEX DATABASE uvm;'\" 2>&1 | tail -5 || true")
    cmd_to_log("su - postgres -c \"psql -d uvm -c 'ALTER DATABASE uvm REFRESH COLLATION VERSION;'\" 2>&1 | tail -3 || true")
    cmd_to_log("su - postgres -c \"psql -d postgres -c 'ALTER DATABASE postgres REFRESH COLLATION VERSION;'\" 2>&1 | tail -3 || true")
    cmd_to_log("su - postgres -c \"psql -d template1 -c 'ALTER DATABASE template1 REFRESH COLLATION VERSION;'\" 2>&1 | tail -3 || true")

    log("Post-upgrade: waiting 30s for systemd postinst cascade to settle")
    time.sleep(30)

    log("Post-upgrade: clean restart of untangle-vm to clear JDK21/jabsorb parallel-load race")
    cmd_to_log("systemctl stop untangle-vm")
    time.sleep(5)
    cmd_to_log("systemctl start untangle-vm")

    fixup_done_flag = "/var/lib/untangle-vm/.trixie-upgrade-fixups-done"
    try:
        os.makedirs(os.path.dirname(fixup_done_flag), exist_ok=True)
        with open(fixup_done_flag, "w") as f:
            f.write("trixie upgrade fixups completed at %s\n" % time.strftime("%Y-%m-%d %H:%M:%S"))
        log("Post-upgrade: marked fixups complete at %s" % fixup_done_flag)
    except:
        log("Post-upgrade: WARNING - could not write fixup done flag")

    log("Post-upgrade: Trixie fixups complete")

def protect_untangle_packages_from_autoremove():
    """
    Mark untangle-* and related runtime-required packages as manually installed
    BEFORE running autoremove. Defense against autoremove sweeping packages that
    have no formal Debian dependency from a manually-installed package but ARE
    invoked at runtime by NGFW scripts and tooling.

    Two failure modes this protects against:

    1. NGFW package anchor missing: When the meta-package that normally anchors
       all untangle-* (untangle-gateway) is missing or has lost its
       manual-install marker, autoremove flags every untangle-* as an orphan.
       Observed 2026-05-22 during bullseye->trixie attempt on .175: dist-upgrade
       installed untangle-vm-1trixie cleanly, then autoremove --purge flagged
       501 packages (untangle-vm, all untangle-app-*, untangle-libuvm*, etc.)
       and destroyed them.

    2. Runtime tools not formally declared as Depends: NGFW shell scripts call
       binaries like smartctl (disk health check inside this very script), dig,
       wg, etc. that have no formal Debian package dependency from any
       untangle-* package. They were installed historically as recommended
       packages or by other Untangle releases. Autoremove will sweep them when
       the recommending package goes away.

    apt-mark manual on already-manual or already-installed packages is a no-op,
    so safe to run unconditionally on every ut-upgrade.py invocation.
    """
    log("Pre-autoremove: marking untangle-* + critical runtime packages as manually installed (anti-sweep)")

    # Anchor untangle-gateway + untangle-vm explicitly (these are normally the
    # manually-installed roots; re-anchor in case markers got scrambled)
    cmd_to_log("apt-mark manual untangle-vm untangle-gateway 2>&1 | tail -5 || true")

    # Mark ALL currently-installed untangle-* as manual so autoremove won't
    # touch them if the gateway anchor is missing
    cmd_to_log("dpkg -l 'untangle-*' 2>/dev/null | awk '/^ii/ {print $2}' | xargs -r apt-mark manual 2>&1 | tail -10 || true")

    # Runtime tools NGFW scripts invoke but don't formally depend on. apt-mark
    # only marks packages that are actually installed; missing packages are
    # silently skipped.
    runtime_tools = [
        "smartmontools",     # smartctl used by ut-upgrade.py check_disk_health
        "wireguard-tools",   # wg, wg-quick for WireGuard VPN userland
        "lsb-release",       # /usr/bin/lsb_release used by various NGFW scripts
        "dnsutils",          # bullseye name for dig/host/nslookup
        "bind9-dnsutils",    # bookworm/trixie name for same
        "tcpdump",           # network capture (support diagnostics)
        "traceroute",        # routing diagnostics
        "iproute2",          # ip command (used everywhere)
        "bridge-utils",      # brctl (legacy bridge tooling)
        "ethtool",           # NIC inspection
        "iputils-ping",      # ping binary
        "rsyslog",           # logging
        "logrotate",         # log rotation
        "cron",              # scheduled jobs
        "openssh-server",    # SSH access
        "sudo",              # privilege escalation
    ]
    # Note: mtr-tiny intentionally excluded — not in Untangle's curated trixie
    # mirror as of 2026-05-22. If the mirror later adds it, add back here.
    cmd_to_log("apt-mark manual %s 2>&1 | tail -10 || true" % " ".join(runtime_tools))

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

# Detect major version upgrade target (bookworm or trixie)
bookworm_upgrade = is_bookworm_upgrade()
trixie_upgrade = is_trixie_upgrade()

if bookworm_upgrade:
    log("")
    log("=" * 60)
    log("MAJOR VERSION UPGRADE: Bullseye -> Bookworm detected")
    log("=" * 60)
    log("")
    pre_upgrade_cleanup()
    log("")
elif trixie_upgrade:
    log("")
    log("=" * 60)
    log("MAJOR VERSION UPGRADE: Bookworm -> Trixie detected")
    log("=" * 60)
    log("")
    pre_upgrade_cleanup_trixie()
    log("")

r = check_upgrade();
if r > 1:
    log("apt-get -s dist-upgrade returned an error (%i). Abort." % r)
    sys.exit(1)
if r == 1:
    # Packages kept back. Tolerate for trixie major-version upgrade where
    # a transitional library (e.g. libmanette-0.2-0) commonly can't be
    # reconciled by apt's resolver mid-hop; abort otherwise.
    if trixie_upgrade:
        log("Packages kept back during trixie upgrade -- proceeding anyway (may need manual install post-upgrade)")
    else:
        log("Packages have been kept back. Abort.")
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

protect_untangle_packages_from_autoremove()
autoremove()

log_date("")
log("")

if bookworm_upgrade:
    post_upgrade_fixups()
    log("")
elif trixie_upgrade:
    post_upgrade_fixups_trixie()
    log("")

clean()

log_date("")
log("")

depmod()

log_date("")
log("")

log_date( os.path.basename( sys.argv[0]) + " done." )

sys.exit(0)
