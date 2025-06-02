"""ngfw test utilities"""
import subprocess
import time
import re
import socket
import fcntl
import struct
import datetime
import random
import shutil
import string
import html
import inspect
import sys
import unittest

import runtests.remote_control as remote_control
import runtests.overrides as overrides
import runtests
from . import ipaddr
import smtplib
import json
import urllib.request, urllib.parse, urllib.error

from uvm import Uvm

# ATS Global Constants
OFFICE_NETWORKS = ['10.111.0.0/16','10.112.0.0/16','10.113.0.0/16']
IPERF_SERVERS = overrides.get("IPERF_SERVERS", default=[('10.112.0.0/16','10.112.56.23'),])
RADIUS_SERVER = "10.112.56.28"
RADIUS_SERVER_PASSWORD = "chakas"
RADIUS_USER = "normal"
RADIUS_PASSWORD = "passwd"
AD_SERVER = "10.112.56.46"
AD_ADMIN = "ATSadmin"
AD_PASSWORD = "passwd"
AD_DOMAIN = "adtest.adtesting.int"
AD_USER = "user_28004"
TEST_SERVER_HOST = overrides.get("TEST_SERVER_HOST", default='test.untangle.com')
ACCOUNT_FILE_SERVER = "ats-iqd.untangle.int"
ACCOUNT_FILE = "/test/account_login.json"

# special Untangle box configured as a OpenVPN server
VPN_SERVER_IP = "10.112.56.96"

# special box within VPN_SERVER_IP's network
VPN_SERVER_LAN_IP = "192.168.235.96"

# special Untangle box configured as a OpenVPN server with User/Pass authentication enabled
VPN_SERVER_USER_PASS_IP = "10.112.56.93"

# special box within VPN_SERVER_USER_PASS_IP's network
VPN_SERVER_USER_PASS_LAN_IP = "192.168.235.83"

# special box with testshell in the sudoer group  - used to connect to vpn as client
VPN_CLIENT_IP = "10.112.56.23"  

# Servers running remote syslog
LIST_SYSLOG_SERVER = '10.112.56.23'

Smtp_timeout_seconds = overrides.get("Smtp_timeout_seconds", default=30)

uvmContext = Uvm().getUvmContext(timeout=240)
uvmContextLongTimeout = Uvm().getUvmContext(timeout=300)
prefix = "@PREFIX@"

# get the IP address of the test.untangle.com if not use static value.
try:
    test_server_ip = socket.gethostbyname(TEST_SERVER_HOST)
except socket.error:
    print("Using default IP as DNS failed")
    test_server_ip = "35.153.140.77"

ftp_server = overrides.get('ftp_server', test_server_ip)
iperf_server = ""

def get_broadcast_address(interface):
    """
    Extracts broadcast address from 'ip addr' command by searching for the input interface. Returns None if the input interface is not present
    """
    if interface is None:
        print("Input interface is not present")
        return None
    return subprocess.check_output("ip addr | grep " + interface + "  | grep inet | awk '{printf $4}'", shell=True).decode("utf-8")

def get_public_ip_address(base_URL=TEST_SERVER_HOST,url_path="cgi-bin/myipaddress.py", extra_options="",localcall=False):
    if base_URL.startswith("http") is False:
        # Add schema
        base_URL = f"http://{base_URL}"
    timeout = 4
    result = ""
    while result == "" and timeout > 0:
        timeout -= 1
        time.sleep(1)
        if localcall:
            try:
                result = subprocess.check_output(build_wget_command(output_file="-", uri=f"{base_URL}/{url_path}", all_parameters=True, extra_arguments=extra_options), shell=True)
            except:
                pass
        else:
            result = remote_control.run_command(build_wget_command(output_file="-", uri=f"{base_URL}/{url_path}", all_parameters=True, extra_arguments=extra_options), stdout=True)
    if type(result) is bytes:
        result = result.decode("utf-8")
    result = result.rstrip()
    return result
    
def get_hostname_ip_address(resolver="8.8.8.8", hostname=TEST_SERVER_HOST):
    # get the IP for the hostname from DNS server 'resolver'
    hostname_ip = "0.0.0.0"
    found = False
    timeout = 4
    while timeout > 0 and not found:
        timeout -= 1
        try:
            hostname_ip = subprocess.check_output("dig +short @" + resolver +  " " + hostname, shell=True)
        except subprocess.CalledProcessError:
            found = False
        else:
            found = True
    hostname_ip = hostname_ip.rstrip()
    return hostname_ip
    
def verify_iperf_configuration(wan_ip, ignore_running=False):
    # https://iperf.fr/
    global iperf_server

    # check if there is an iperf server on the same network
    iperf_servers = overrides.get("IPERF_SERVERS")
    if iperf_servers is None:
        iperf_servers = IPERF_SERVERS
    for iperf_server_pair in iperf_servers:
        if ipaddr.IPv4Address(wan_ip) in ipaddr.IPv4Network(iperf_server_pair[0]):
            iperf_server = iperf_server_pair[1]
            break
    if iperf_server == "":
        print("No iperf server in the same network")
        return False
    # Check to see if iperf endpoint is reachable
    iperf_serverReachable = subprocess.call(["ping -c 1 " + iperf_server + " >/dev/null 2>&1"],shell=True,stdout=None,stderr=None)
    if iperf_serverReachable != 0:
        print("iperf Server is unreachable.")
        return False
    if ignore_running is False:
        # Check to see if some other test is using iperf for UDP testing
        iperfRunning = remote_control.run_command("pidof iperf", host=iperf_server)
        if iperfRunning == 0:
            print("iperf is already running on server.")
            return False
    # Check that the client has iperf
    clientHasIperf = remote_control.run_command("test -x /usr/bin/iperf")
    if clientHasIperf != 0:
        print("iperf not installed on client.")
        return False
    return True

# For parsing final iperf results:
# [  3] 0.0000-10.0006 sec  9.59 GBytes  8.24 Gbits/sec
# Also, transferred/rate can be non-float:
# [  3] 0.0000-5.0005 sec   952 MBytes  1.60 Gbits/sec"
iperf_results_re = re.compile('^\[\s+\d+\]\s+([0-9]*[.][0-9]*)\-([0-9]*[.][0-9]*)\s+sec\s+([0-9]*[.]*[0-9]*)\s+([^\s]+)\s+([0-9]*[.]*[0-9]*)\s+([^/]+)/(.+)')
def get_iperf_results(duration=30):
    """
    Run iperf and return results

    Returns a dictionary with the following keys:
    duration: duration of the test in seconds
    transferred: number of bytes transferred
    throughput: number of bytes per second
    """
    results = {
        "duration": 0,
        "transferred": 0,
        "throughput": 0,
    }
    result = 0
    start_time = None
    end_time = None
    transferred_amount = None
    transferred_size = None
    rate_amount = None
    rate_size = None
    rate_interval = None

    iperf_results = remote_control.run_command(build_iperf_command(duration=duration), stdout=True)

    last_line = iperf_results.splitlines()[-1]
    matches = iperf_results_re.match(last_line)
    transferred_value = 0
    rate_value = 0
    if matches is not None:
        # If we don't get here either:
        # - Something fundamentally wrong went wrong with the iperf command
        # - Our regex failed
        start_time = float(matches.group(1))
        stop_time = float(matches.group(2))
        results["duration"] = stop_time - start_time 

        transferred_amount = float(matches.group(3))
        transferred_size = matches.group(4)
        results["transferred"] = from_si_prefix(f"{transferred_amount}{transferred_size}")

        throughput_amount = float(matches.group(5))
        throughput_size = matches.group(6)
        # Expecting this to be seconds.
        throughput_interval = matches.group(7)

        results["throughput"] = from_si_prefix(f"{throughput_amount}{throughput_size}")

    return results

def build_iperf_command(mode="client", server_address=None, override_arguments=None, extra_arguments=None, fork=False, duration=30):
    """
    Build iperf command

    Default arguments should be evident, but of particular note are:
    override_arguments  If you really want to ignore the standard arguments and options, use it.  For example, if you really wanted to use hsts.
    extra_arguments     Additional arguments not otherwise processed.
    """
    if mode == "client":
        if server_address is None:
            server_address = iperf_server

    arguments = []
    if override_arguments is not None:
        # Allow completely custom arguments
        arguments = override_arguments
    else:
        if mode == "client":
            if duration is not None:
                arguments.append(f"--time {duration}")

    optional_arguments = []
    if fork:
        optional_arguments.append(" >/dev/null 2>&1 &")

    if extra_arguments is not None:
        optional_arguments.append(extra_arguments)

    if mode == "client":
        command = f"iperf -c {server_address} {' '.join(arguments)} {' '.join(optional_arguments)}"
    elif mode == "server":
        command = f"iperf -s {' '.join(arguments)} {' '.join(optional_arguments)}"
    else:
        command = f"iperf {' '.join(arguments)} {' '.join(optional_arguments)}"

    print(f"{sys._getframe().f_code.co_name}: {command}" )
    return command

# For parsing final wrk results:
#   73197 requests in 30.04s, 59.33MB read
#   Socket errors: connect 0, read 0, write 1, timeout 0
# Requests/sec:   2436.31
# Transfer/sec:      1.97MB
# iperf_results_re = re.compile('^\[\s+\d+\]\s+([0-9]*[.][0-9]*)\-([0-9]*[.][0-9]*)\s+sec\s+([0-9]*[.]*[0-9]*)\s+([^\s]+)\s+([0-9]*[.]*[0-9]*)\s+([^/]+)/(.+)')
wrk_requests_result_re = re.compile('^\s*(\d+) requests in ([0-9]*[.]*[0-9]*)([^,]+),\s+([0-9]*[.]*[0-9]*)([^,]+)([^\s]+) read')
wrk_errors_result_re = re.compile('^\s+Socket errors: connect (\d+), read (\d+), write (\d+), timeout (\d+)')
wrk_rate_result_re = re.compile('^Requests/sec:\s+([0-9]*[.]*[0-9]*)')
wrk_transfer_result_re = re.compile('^Transfer/sec:\s+([0-9]*[.]*[0-9]*)([^\s]+)')
def get_wrk_results(duration=30):
    """
    Run iperf and return results
    """
    results = {
        "requests": 0,
        "transferred": 0,
        "rate": 0,
        "throughput": 0,
        "errors": {
            "connect": 0,
            "read": 0,
            "write": 0,
            "timeout": 0,
        }
    }
    wrk_results = remote_control.run_command(build_wrk_command(duration=duration), stdout=True)

    for wrk_result in wrk_results.split("\n"):
        print(f"wrk_result: {wrk_result}")
        matches = wrk_requests_result_re.match(wrk_result)
        if matches is not None:
            results["requests"] = matches.group(1)
            results["duration"] = float(matches.group(2))
            # results["transferred"] = float(matches.group(4))
            results["transferred"] = from_si_prefix(f"{matches.group(4)}{matches.group(5)}")
            continue

        matches = wrk_errors_result_re.match(wrk_result)
        if matches is not None:
            results["errors"]["connect"] = matches.group(1)
            results["errors"]["read"] = matches.group(2)
            results["errors"]["write"] = matches.group(3)
            results["errors"]["timeout"] = matches.group(4)
            continue

        matches = wrk_rate_result_re.match(wrk_result)
        if matches is not None:
            request_value = float(matches.group(1))
            results["rate"] = request_value
            continue

        matches = wrk_transfer_result_re.match(wrk_result)
        if matches is not None:
            throughput_amount = float(matches.group(1))
            throughput_size = matches.group(2)
            throughput_value = from_si_prefix(f"{throughput_amount}{throughput_size}")
            results["throughput"] = throughput_value
            continue

    return results

def build_wrk_command(uri=None, override_arguments=None, extra_arguments=None, threads=100, connections=700, duration=30):
    """
    Build wrk command

    Default arguments should be evident, but of particular note are:
    override_arguments  If you really want to ignore the standard arguments and options, use it.  For example, if you really wanted to use hsts.
    extra_arguments     Additional arguments not otherwise processed.
    """
    if uri is None:
        uri = f"http://{TEST_SERVER_HOST}/"

    arguments = []
    if override_arguments is not None:
        # Allow completely custom arguments
        arguments = override_arguments
    else:
        if duration is not None:
            arguments.append(f"--duration {duration}")
        if threads is not None:
            arguments.append(f"--threads {threads}")
        if connections is not None:
            arguments.append(f"--connections {connections}")

    optional_arguments = []

    if extra_arguments is not None:
        optional_arguments.append(extra_arguments)

    command = f"wrk {' '.join(arguments)} {' '.join(optional_arguments)} {uri}"

    print(f"{sys._getframe().f_code.co_name}: {command}" )
    return command

def verify_wrk_configuration():
    """
    Verify wrk is ready
    """
    wrk_available = remote_control.run_command(f"which wrk 1> /dev/null 2>&1")
    if wrk_available != 0:
        print("wrk not available")
        return False
    return True

def verify_kerberos():
    """
    Verify kerberos is installed
    """
    wrk_available = remote_control.run_command(f"which kinit 1> /dev/null 2>&1")
    if wrk_available != 0:
        print("init not available")
        return False
    return True

def kerberos_authenticate(user=None, password=None):
    """
    Perform kerberos authentication.

    Retrns True if success, False otherwise.
    """
    return remote_control.run_command(build_kerberos_command(user, password)) == 0


def build_kerberos_command(user=None, password=None, override_arguments=None, extra_arguments=None):
    """
    Build kerberos kinit command

    Default arguments should be evident, but of particular note are:
    override_arguments  If you really want to ignore the standard arguments and options, use it.  For example, if you really wanted to use hsts.
    extra_arguments     Additional arguments not otherwise processed.
    """
    if user is None:
        user = "USER"

    if password is not None:
        password_command = f"echo '{password}' | "
    else:
        password_command = ""

    arguments = []
    if override_arguments is not None:
        # Allow completely custom arguments
        arguments = override_arguments

    optional_arguments = []

    if extra_arguments is not None:
        optional_arguments.append(extra_arguments)

    command = f"{password_command}kinit {' '.join(arguments)} {' '.join(optional_arguments)} \"{user}\""

    print(f"{sys._getframe().f_code.co_name}: {command}" )
    return command

def wait_for_event_queue_drain(queue_size_key="eventQueueSize"):
    """
    Run wrk and wait for specified queue to drain below a minimum amount

    NOTE: To generate the maximum number of events, enable an HTTP processing app web-filter.
    """
    # Don't penalize if an event happens to come through
    min_events = 10
    # Time to wait for queue to drain to min_events or less
    max_watch_seconds = 60

    wrk_available = verify_wrk_configuration()
    if not wrk_available:
        raise unittest.SkipTest("wrk not on client")

    wrk_results = get_wrk_results()
    # wrk output can be useful for debugging
    print(wrk_results)

    # It's not uncommon for the queue size to be empty at this point.
    # It would be more "visually" interesting if we forked the wrk process
    # and monitor the queue in that way. 
    queue_size = 0
    max_time = datetime.datetime.now() + datetime.timedelta(seconds=max_watch_seconds)
    while True:
        queue_size = uvmContext.metricManager().getStats()["systemStats"][queue_size_key]
        print(f"queue_size: {queue_size}")
        sys.stdout.flush()
        if queue_size < min_events:
            break
        if datetime.datetime.now() > max_time:
            break
        time.sleep(10)

    assert queue_size <= min_events, f"{queue_size_key} queue drained below {min_events}"

# Parse "dhcp client" (nmap with "broadcast-dhcp-discover" script) with successful output like:
# # nmap --script broadcast-dhcp-discover
# Starting Nmap 7.80 ( https://nmap.org ) at 2023-03-15 14:03 EDT
# Pre-scan script results:
# | broadcast-dhcp-discover:
# |   Response 1 of 1:
# |     IP Offered: 192.168.252.117
# |     DHCP Message Type: DHCPOFFER
# |     Server Identifier: 192.168.252.70
# |     IP Address Lease Time: 2m00s
# |     Renewal Time Value: 1m00s
# |     Rebinding Time Value: 1m45s
# |     Broadcast Address: 192.168.252.255
# |     Domain Name: example.com
# |     TFTP Server Name: 1.2.3.4\x00
# |     Domain Name Server: 192.168.252.70
# |     Subnet Mask: 255.255.255.0
# |_    Router: 192.168.252.70
# WARNING: No targets were specified, so 0 hosts scanned.
# Nmap done: 0 IP addresses (0 hosts up) scanned in 3.14 seconds
#
# No result (no DHCP server) looks like:
# # nmap --script broadcast-dhcp-discover
# Starting Nmap 7.80 ( https://nmap.org ) at 2023-03-15 14:08 EDT
# WARNING: No targets were specified, so 0 hosts scanned.
# Nmap done: 0 IP addresses (0 hosts up) scanned in 10.14 seconds
nmap_results_no_privileges_re = re.compile(".+ not running for lack of privileges.*")
nmap_fields = [{
    "id": 1,    "type": "address",  "output": "Subnet Mask"
},{
    "id": 2,    "type": "time",     "output": "Time Offset"
},{
    "id": 3,    "type": "address",  "output": "Router"
},{
    "id": 4,    "type": "address",  "output": "Time Server"
},{
    "id": 6,    "type": "address",  "output": "Domain Name Server"
},{
    "id": 5,    "type": "address",  "output": "Name Server"
},{
    "id": 7,    "type": "address",  "output": "Log Server"
},{
    "id": 8,    "type": "address",  "output": "Cookie Server"
},{
    "id": 9,    "type": "address",  "output": "LPR Server"
},{
    "id": 10,   "type": "address",  "output": "Impress Server"
},{
    "id": 11,   "type": "address",  "output": "Resource Location Server"
},{
    "id": 12,   "type": "string",   "output": "Hostname"
},{
    "id": 13,   "type": "size",     "output": "Boot File Size"
},{
    "id": 14,   "type": "string",   "output": "Merit Dump File"
},{
    "id": 15,   "type": "string",   "output": "Domain Name"
},{
    "id": 16,   "type": "address",  "output": "Swap Address"
},{
    "id": 17,   "type": "string",   "output": "Root Path"
},{
    "id": 18,   "type": "string",   "output": "extensions_path"
},{
    "id": 19,   "type": "boolean",  "output": "IP Forwarding"
},{
    "id": 20,   "type": "boolean",  "output": "Non-local Source Routing"
},{
    "id": 21,   "type": "string",   "output": "Policy Filter"
},{
    "id": 22,   "type": "size",     "output": "Maximum Datagram Reassembly Size"
},{
    "id": 23,   "type": "seconds",  "output": "Default IP TTL"
},{
    "id": 24,   "type": "seconds",  "output": "Path MTU Aging Timeout"
},{
    "id": 25,   "type": "size",     "output": "Path MTU Plateau"
},{
    "id": 26,   "type": "size",     "output": "Interface MTU"
},{
    "id": 27,   "type": "boolean",  "output": "All Subnets are Local"
},{
    "id": 28,   "type": "address",  "output": "All Subnets are Local"
},{
    "id": 29,   "type": "boolean",  "output": "Perform Mask Discovery"
},{
    "id": 30,   "type": "boolean",  "output": "Mask Supplier"
},{
    "id": 31,   "type": "boolean",  "output": "Perform Router Discovery"
},{
    "id": 32,   "type": "address",  "output": "Router Solicitation Address"
},{
    "id": 33,   "type": "route",    "output": "Static Route"
},{
    "id": 34,   "type": "boolean",  "output": "Trailer Encapsulation"
},{
    "id": 35,   "type": "seconds",  "output": "ARP Cache Timeout"
},{
    "id": 36,   "type": "boolean",  "output": "Ethernet Encapsulation"
},{
    "id": 37,   "type": "seconds",  "output": "TCP Default TTL"
},{
    "id": 38,   "type": "seconds",  "output": "TCP Keepalive Interval"
},{
    "id": 39,   "type": "boolean",  "output": "TCP Keepalive Garbage"
},{
    "id": 40,   "type": "string",   "output": "NIS Domain"
},{
    "id": 41,   "type": "address",  "output": "NIS Servers"
},{
    "id": 42,   "type": "address",  "output": "NTP Servers"
},{
    "id": 43,   "type": "string",   "output": "Vendor Specific Information"
},{
    "id": 44,   "type": "address",  "output": "NetBIOS Name Server"
},{
    "id": 45,   "type": "address",  "output": "NetBIOS Datagram Server"
},{
    "id": 46,   "type": "string",   "output": "NetBIOS Node Type"
},{
    "id": 47,   "type": "string",   "output": "NetBIOS Scope"
},{
    "id": 48,   "type": "address",  "output": "X Window Font Server"
},{
    "id": 49,   "type": "address",  "output": "X Window Display Manager"
},{
    "id": 50,   "type": "address",  "output": "Requested IP Address (client)"
},{
    "id": 51,   "type": "seconds",  "output": "IP Address Lease Time"
},{
    "id": 52,   "type": "string",   "output": "Option Overload"
},{
    "id": 53,   "type": "string",   "output": "DHCP Message Type"
},{
    "id": 54,   "type": "address",  "output": "Server Identifier"
},{
    "id": 55,   "type": "string",   "output": "Parameter Request List (client)"
},{
    "id": 56,   "type": "string",   "output": "Error Message"
},{
    "id": 57,   "type": "size",     "output": "Maximum DHCP Message Size"
},{
    "id": 58,   "type": "seconds",  "output": "Renewal Time Value"
},{
    "id": 59,   "type": "seconds",  "output": "Rebinding Time Value"
},{
    "id": 60,   "type": "string",   "output": "Class Identifier"
},{
    "id": 61,   "type": "string",   "output": "Client Identifier (client)"
},{
    "id": 62,   "type": "string",   "output": "TFTP Server Name"
},{
    "id": 67,   "type": "string",   "output": "Bootfile Name"
},{
    "id": 252,  "type": "string",   "output": "WPAD"
},{
    "id": None, "type": "address",  "output": "IP Offered"
}]

def get_dhcp_client_results(duration=30):
    """
    Run dhcp client (nmap)and return results

    Returns a dictionary of options obtained from DHCP query with the following keys:
    dhcp                Dictionary of key/pair valyes.
    no_privileges       Client did not have user privileges to run
    """
    results = {
        "dhcp": {},
        "no_privileges": False
    }

    dhcp_results = remote_control.run_command(build_nmap_command(script="broadcast-dhcp-discover"), stdout=True)
    for dhcp_result in dhcp_results.split("\n"):
        matches = nmap_results_no_privileges_re.match(dhcp_result)
        if matches:
            # Not running with root privileges
            results["no_privileges"] = True
            break

        for nmap_field in nmap_fields:
            matches = re.match(f".+ {nmap_field['output']}: (.+)", dhcp_result)
            if matches:
                field = nmap_field['output'].lower().replace(" ", "_")
                results["dhcp"][field] = matches.group(1)
                break

    return results

def build_nmap_command(sudo=True, override_arguments=None, extra_arguments=None, script=None):
    """
    Build nmap command

    Default arguments should be evident, but of particular note are:
    override_arguments  If you really want to ignore the standard arguments and options, use it.  For example, if you really wanted to use hsts.
    extra_arguments     Additional arguments not otherwise processed.
    """
    if sudo:
        prefix = "sudo "
    else:
        prefix = ""

    arguments = []
    if override_arguments:
        # Allow completely custom arguments
        arguments = override_arguments

    optional_arguments = []

    if extra_arguments:
        optional_arguments.append(extra_arguments)
    if script:
        optional_arguments.append(f"--script {script}")

    command = f"{prefix}nmap {' '.join(arguments)} {' '.join(optional_arguments)}"

    print(f"{sys._getframe().f_code.co_name}: {command}" )
    return command

def get_wait_for_command_result(command=None, success_result=0, tries=10, local=False):
    """
    Wait up to tries for a successful command result

    returns True if ping successful, False otherwise
    """
    if command is None:
        command = build_ping_command(target=TEST_SERVER_HOST)

    result = None
    while result != 0 and tries > 0:
        tries -= 1
        if local is True:
            result = subprocess.run(command, shell=True, stderr=subprocess.PIPE, stdout=subprocess.PIPE)
            result = result.returncode
        else:
            result = remote_control.run_command(command)

        if result == success_result:
            return True
        time.sleep(1)

    return False

def get_wait_for_command_output(command=None, success_test=None, tries=10, local=False):
    """
    Wait up to tries for a successful command result

    returns True if ping successful, False otherwise
    """
    if command is None:
        command = build_ping_command(target=TEST_SERVER_HOST)

    result = None
    while result != 0 and tries > 0:
        tries -= 1
        if local is True:
            result = subprocess.run(command, shell=True, stderr=subprocess.STDOUT, stdout=subprocess.PIPE)
            result = result.stdout.decode('utf-8')
        else:
            result = remote_control.run_command(command, stdout=True)

        result = result.strip()
        print(f"result={result}")
        if success_test is None or success_test(result):
            return result
        time.sleep(1)

    return ""

def build_ping_command(target=None, override_arguments=None, extra_arguments=None, count=1, wait=1):
    """
    Build ping command

    Default arguments should be evident, but of particular note are:
    override_arguments  If you really want to ignore the standard arguments and options, use it.  For example, if you really wanted to use hsts.
    extra_arguments     Additional arguments not otherwise processed.
    """
    arguments = []
    if override_arguments:
        # Allow completely custom arguments
        arguments = override_arguments
    else:
        if count is not None:
            arguments.append(f"-c {count}")
        if wait is not None:
            arguments.append(f"-w {wait}")

    optional_arguments = []

    if extra_arguments:
        optional_arguments.append(extra_arguments)

    command = f"ping {' '.join(arguments)} {' '.join(optional_arguments)} {target}"

    print(f"{sys._getframe().f_code.co_name}: {command}" )
    return command

Si_prefixes = {
    # yocto
    'y': {
        "bit": 1e-24,
        "dec": 1e-24
    },
    # zepto
    'z': {
        "bit": 1e-21,
        "dec": 1e-21
    },
    # atto
    'a': {
        "bit": 1e-18,
        "dec": 1e-18
    },
    # femto
    'f': {
        "bit": 1e-15,
        "dec": 1e-15
    },
    # pico
    'p': {
        "bit": 1e-12,
        "dec": 1e-12
    },
    # nano
    'n': {
        "bit": 1e-9,
        "dec": 1e-9
    },
    # micro
    'u': {
        "bit": 1e-6,
        "dec": 1e-6
    },
    # mili
    'm': {
        "bit": 1e-3,
        "dec": 1e-3
    },
    # centi
    'c': {
        "bit": 1e-2,
        "dec": 1e-2
    },
    # deci
    'd': {
        "bit": 1e-1,
        "dec": 1e-1
    },
    # kilo
    'k': {
        "bit": 2<<9,
        "dec": 1e3
    },
    # kilo
    'K': {
        "bit": 2<<9,
        "dec": 1e3
    },
    # mega
    'M': {
        "bit": 2<<19,
        "dec": 1e6
    },
    # giga
    'G': {
        "bit": 2<<19,
        "dec": 1e9
    },
    # tera
    'T': {
        "bit": 2<<29,
        "dec": 1e12
    },
    # peta
    'P': {
        "bit": 2<<39,
        "dec": 1e15
    },
    # exa
    'E': {
        "bit": 2<<49,
        "dec": 1e18
    },
    # zetta
    'Z': {
        "bit": 2<<59,
        "dec": 1e21
    },
    # yotta
    'Y': {
        "bit": 2<<69,
        "dec": 1e24
    }
}
Si_from_match_re=re.compile('^([0-9]*[.][0-9]*)\s*(.+)')

def from_si_prefix(si_value, type="bit"):
    """
    Convert si value into raw non sa value 
    """
    value = None
    matches = Si_from_match_re.match(si_value)
    if matches is not None:
        prefix = matches.group(2)[0]
        if prefix in Si_prefixes:
            value = float(matches.group(1)) * Si_prefixes[prefix][type]
    return value

def to_si_prefix(value, unit="bit", type="bit"):
    """
    Convert value into si prefix with optional unit
    """
    last_si = None
    for si in Si_prefixes:
        if last_si is None:
            last_si = si
        if value < Si_prefixes[si][type]:
            break
        last_si = si

    value = value / Si_prefixes[last_si][type]
    return f"{value:.2f} {last_si}{unit}"

def get_host_hops(host_address):
    """
    Run traceroute to determine number of hops to reach host.
    """
    traceroute_result = remote_control.run_command(f"sudo traceroute -n -I {host_address}", stdout=True)
    # Expecting our last result line to look like:
    # 2  192.168.25.57  1.034 ms * *
    return int(traceroute_result.split("\n")[-1].strip().split(" ")[0])

def restart_uvm():
    """
    Restart uvm.
    IMPORTANT: This changes uvmContext!
    """
    global uvmContext, uvmContextLongTimeout
    subprocess.call(["/etc/init.d/untangle-vm","restart"],stdout=subprocess.PIPE,stderr=subprocess.PIPE)

    uvmContext = None
    max_tries = 60
    while max_tries > 0:
        try:
            uvmContext = Uvm().getUvmContext(timeout=240)
            uvmContextLongTimeout = Uvm().getUvmContext(timeout=300)
        except Exception as e:
            pass
        if uvmContext is not None:
            break
        max_tries -= 1
        time.sleep(1)

    return uvmContext


def find_syslog_server(wan_ip):
    syslog_ip = ""
    if is_in_office_network(wan_ip):
        syslog_ip = LIST_SYSLOG_SERVER
    return syslog_ip

def get_udp_download_speed( receiverip, senderip, targetip=None, targetRate=None ):
    if targetip == None:
        targetip = receiverip
    if targetRate == None:
        targetRate = "50M"

    # Use iperf to get UDP speed.  Returns number the udp speed
    iperf_tries = 5
    while iperf_tries > 0:  # try iperf a few times if it fails to send UDP packets correctly.
        # start iperf receivier on server
        remote_control.run_command("iperf -s -p 5000 -u >/dev/null 2>&1 &", host=receiverip)

        # start the UDP generator on the client behind the Untangle.
        report=remote_control.run_command("iperf -c " + targetip + " -u -p 5000 -b " + targetRate + " -t 20 -fK", host=senderip, stdout=True)

        # kill iperf receiver and verify
        iperfRunning = 0
        timeout = 60
        while iperfRunning == 0 and timeout > 0:
            timeout -= 1
            remote_control.run_command("pkill --signal 9 iperf", host=receiverip)
            time.sleep(1)
            iperfRunning = remote_control.run_command("pidof iperf", host=receiverip)

        if '%' in report:
            break
        else:
            iperf_tries -= 1

    lines = report.split("\n")
    udp_speed = 0
    for line in lines:
        if '%' in line: # results line contains a '%'
            match = re.search(r'([0-9.]+) ([KM])Bytes/sec', line)
            udp_speed =  match.group(1)
            udp_speed = float(udp_speed)
            if match.group(2) == "M":
                udp_speed *= 1000
            break
    return udp_speed

def get_download_speed(download_server="",meg=20):
    try:
        # Download file and record the average speed in which the file was download
        # As a default use the office web server if available
        if download_server == "":
            accountFileServerPing = subprocess.call(["ping","-c","1",ACCOUNT_FILE_SERVER],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
            if accountFileServerPing != 0:
                download_server = TEST_SERVER_HOST
            else:
                # Use QA web server in the office for more reliable results
                download_server = ACCOUNT_FILE_SERVER
        print(f"download_server={download_server}")
        result = remote_control.run_command(build_wget_command(quiet=False, log_file="/dev/stdout", output_file="/dev/null", timeout=60, uri="http://" + download_server + f"/{meg}MB.zip") +  " 2>&1 | tail -2", stdout=True)
        print(f"result={result}")

        match = re.search(r'([0-9.]+) [KM]B\/s', result)
        bandwidth_speed =  match.group(1)
        # cast string to float for comparsion.
        bandwidth_speed = float(bandwidth_speed)
        # adjust value if MB or KB
        if "MB/s" in result:
            bandwidth_speed *= 1000
        # print("bandwidth_speed <%s>" % bandwidth_speed)
        return bandwidth_speed
    except Exception as e:
        print(e)
        return None

def get_events( eventEntryCategory, eventEntryTitle, conditions, limit, start_date=None, end_date=None ):
    reports = uvmContextLongTimeout.appManager().app("reports")
    if reports == None:
        print("WARNING: reports app not found")
        return None

    reports.flushEvents()

    reportsManager = reports.getReportsManager()

    reportEntry = reportsManager.getReportEntry( eventEntryCategory, eventEntryTitle )
    if reportEntry == None:
        print(("WARNING: Event entry not found: %s %s" % (eventEntryCategory, eventEntryTitle)))
        return None

    if start_date is not None and end_date is not None:
        events = reportsManager.getDataForReportEntry( reportEntry, start_date, end_date, None, conditions, None, limit )
    else:
        events = reportsManager.getEvents( reportEntry, conditions, limit )
    if events == None:
        return None

    return events
    # FIXME we should return the array instead
    # return events.get('list')

def find_event( events, num_events, *args, **kwargs):
    if events == None:
        return None
    if num_events == 0:
        return None
    if len(events) == 0:
        print("No events in list")
        return None
    if kwargs.get('min_date') == None:
        print("XXX: " + str(runtests.test_start_time))
        min_date = runtests.test_start_time
    else:
        min_date = kwargs.get('min_date')

    if (len(args) % 2) != 0:
        if type(args[0]) == dict:
            # Use first element as a dict for matches
            matches = args[0]
        else:
            # Otherwise, provide a mismatch prevents key/pair matching
            print("Invalid argument length")
            return None
    else:
        # Build matches dictionary
        matches = {}
        for i in range(0, int(len(args)/2)):
            matches[args[i*2]] = args[i*2+1]

    num_checked = 0
    while num_checked < num_events:
        if len(events) <= num_checked:
            print(("failed to find event checked: %i total: %i" % (num_checked, len(events)) ))
            break
        event = events[num_checked]
        num_checked += 1

        # if event has a date and its too old - ignore the event
        if event.get('time_stamp') != None:
            time_stamp = event.get('time_stamp')
            if type(time_stamp) is int:
                ts = datetime.datetime.fromtimestamp((time_stamp/1000)+1)#round up
            else:
                ts = datetime.datetime.fromtimestamp((time_stamp['time']/1000)+1)#round up
            if ts < min_date:
                print(("ignoring old event: %s < %s " % (ts.isoformat(),min_date.isoformat())))
                continue

        # check each expected value
        # if one doesn't match continue to the next event
        # if all match, return True
        allMatched = True

        print("event:")
        for key in matches.keys():
            expectedValue = matches.get(key)
            actualValue = event.get(key)
            # HTML strings are escaped now.
            if type(actualValue) is str:
                actualValue = html.unescape(actualValue)
            alternateValue = expectedValue
            # If the type is a boolean, accept 1/0 also
            if type(expectedValue) is bool:
                if expectedValue:
                    alternateValue = 1
                else:
                    alternateValue = 0
            # mismatch = str(expectedValue) != str(actualValue) and str(alternateValue) != str(actualValue)
            match = str(expectedValue) == str(actualValue) or str(alternateValue) == str(actualValue)
            if match:
                compare = "=="
            else:
                compare = "!="
            print(f"key={key:10} expectedValue={expectedValue:<10} {compare} actualValue={actualValue:<10} {match}")
            if not match:
                allMatched = False
                break

        if allMatched:
            return event
    return None

def check_events( events, num_events, *args, **kwargs):
    return (find_event( events, num_events, *args, **kwargs) != None)

def get_and_check_events(prefix="", report_category="", report_title="", report_conditions=None, event_limit=5, check_num_events=5, matches={}):
    """
    Retrieve events from specified event report and look for matching events
    """
    events = get_events(report_category, report_title, report_conditions, event_limit)
    assert events != None, f"{prefix} total events found"
    print(events)
    found = check_events( events.get('list'), check_num_events, matches)

    assert found, f"{prefix} event matches found"

def get_wait_for_events(prefix="", report_category="", report_title="", report_conditions=None, event_limit=5, check_num_events=5, matches={}, tries=10):
    """
    Retrieve events from specified event report and look for matching events
    """
    found = False
    while found is False and tries > 0:
        tries -= 1
        events = get_events(report_category, report_title, report_conditions, event_limit)
        assert events != None, f"{prefix} total events found"
        found = check_events( events.get('list'), check_num_events, matches)
        if found is True:
            return True
        time.sleep(1)

    return False

def is_in_office_network(wan_ip):
    office_networks = overrides.get("OFFICE_NETWORKS")
    if office_networks is None:
        office_networks = OFFICE_NETWORKS
    for office_network_test in office_networks:
        if ipaddr.IPv4Address(wan_ip) in ipaddr.IPv4Network(office_network_test):
            return True
    return False

def is_device_pppoe():
    netsettings = uvmContext.networkManager().getNetworkSettings()

    # Get enabled WAN interfaces, static, Ethernet
    pppoe_status = False
    for interface in netsettings["interfaces"]["list"]:
        if interface["v4ConfigType"] == "PPPOE":
            pppoe_status = True
            break
    return pppoe_status

def is_bridged(wan_ip):
    result = remote_control.run_command("ip -o -f inet addr show",stdout=True)
    match = re.search(r'\d{1,3}.\d{1,3}.\d{1,3}.\d{1,3}\/\d{1,3} brd', result)
    hostname_cidr = (match.group()).replace(' brd','')
    if ipaddr.IPv4Address(wan_ip) in ipaddr.IPv4Network(hostname_cidr):
        return True
    return False

def is_vm_instance():
    # Check if this is a VMware instance
    hostname_ip = "0.0.0.0"
    is_vm = True
    try:
        vm_result = subprocess.check_output("lscpu | grep VMware", shell=True).decode('utf-8')
        if ("VMware" not in vm_result):
            is_vm = False
    except subprocess.CalledProcessError:
        is_vm = True
    else:
        found = True
    return is_vm

def cidr_to_netmask(cidr):
    """
    From a cidr notation string, return network/netmask.
    """
    network, net_bits = cidr.split('/')
    host_bits = 32 - int(net_bits)
    netmask = socket.inet_ntoa(struct.pack('!I', (1 << 32) - (1 << host_bits)))
    return network, netmask

def send_test_email(mailhost=TEST_SERVER_HOST):
    sender = 'atstest@test.untangle.com'
    receivers = ['atstest@test.untangle.com']

    message = """From: Test <atstest@test.untangle.com>
    To: Test Group <atstest@test.untangle.com>
    Subject: SMTP e-mail test

    This is a test e-mail message.
    """

    try:
       smtpObj = smtplib.SMTP(mailhost,timeout=Smtp_timeout_seconds)
       smtpObj.sendmail(sender, receivers, message)
       print(("Successfully sent email through " + mailhost))
       return 1
    except smtplib.SMTPException as e:
       print(("Error: unable to send email through " + mailhost + " " + str(e)))
       return 0

def get_app_metric_value(app, label):
    metric = app.getMetric(label)
    if metric == None:
        print(("Missing metric: %s"%str(label) ))
        return 0
    if metric.get('value') == None:
        print(("Missing metric value: %s"%str(label) ))
        return 0
    return metric.get('value')

def get_live_account_info(accounttype):
    account_url = "http://" + ACCOUNT_FILE_SERVER + "/test/account_login.json"
    try:
        response = urllib.request.urlopen(account_url,timeout=10)
        accounts = json.loads((response.read()).decode("utf-8"))

        for account in accounts: #i is each student's name, class, and number
            if account[0] == accounttype:
                return (account[1], account[2])
        return ("message",accounttype + " account not found")
    except:
        return (None, None)

def get_wan_tuples():
    myWANs = []
    netsettings = uvmContext.networkManager().getNetworkSettings()
    for interface in netsettings['interfaces']['list']:
        wan_ip = ""
        wanGateway = ""
        if interface['isWan'] and interface['configType'] == "ADDRESSED":
            if interface['v4ConfigType'] == "STATIC":
                wanIndex =  interface['interfaceId']
                wan_ip =  interface['v4StaticAddress']
                wanGateway =  interface['v4StaticGateway']
            elif interface['v4ConfigType'] == "AUTO":
                nicDevice = str(interface['symbolicDev'])
                wanIndex = interface['interfaceId']
                wan_ip = __get_ip_address(nicDevice)
                wanGateway = __get_gateway(nicDevice)

            if wan_ip:
                wanExtip = get_public_ip_address(extra_options="--bind-address=" + wan_ip,localcall=True)
                wanExtip = wanExtip.rstrip()
                wanTup = (wanIndex,wan_ip,wanExtip,wanGateway, str(interface['symbolicDev']))
                myWANs.append(wanTup)
    return myWANs

def get_prefix():
    global prefix
    return prefix

def get_lan_ip():
    ip = uvmContext.networkManager().getInterfaceHttpAddress( remote_control.interface )
    return ip

def get_http_url():
    ip = uvmContext.networkManager().getInterfaceHttpAddress( remote_control.interface )
    httpPort = str(uvmContext.networkManager().getNetworkSettings().get('httpPort'))
    httpAdminUrl = "http://" + ip + ":" + httpPort + "/"
    return httpAdminUrl

def get_https_url():
    ip = uvmContext.networkManager().getInterfaceHttpAddress( remote_control.interface )
    httpsPort = str(uvmContext.networkManager().getNetworkSettings().get('httpsPort'))
    httpsAdminUrl = "https://" + ip + ":" + httpsPort + "/"
    return httpsAdminUrl

def host_username_set(username):
    entry = uvmContext.hostTable().getHostTableEntry( remote_control.client_ip )
    entry['usernameDirectoryConnector'] = username
    uvmContext.hostTable().setHostTableEntry( remote_control.client_ip, entry )

def host_username_clear():
    entry = uvmContext.hostTable().getHostTableEntry( remote_control.client_ip )
    entry['usernameDirectoryConnector'] = None
    uvmContext.hostTable().setHostTableEntry( remote_control.client_ip, entry )

def host_tags_add(str):
    entry = uvmContext.hostTable().getHostTableEntry( remote_control.client_ip )
    entry['tags']['list'].append( {
        "javaClass": "com.untangle.uvm.Tag",
        "name": str,
        "expirationTime": int(round((time.time()+60) * 1000)) #60 seconds from now
    } )
    uvmContext.hostTable().setHostTableEntry( remote_control.client_ip, entry )

def host_tags_clear():
    entry = uvmContext.hostTable().getHostTableEntry( remote_control.client_ip )
    for t in entry['tags']['list']:
        t['expirationTime'] = 1 #expire 1970
    uvmContext.hostTable().setHostTableEntry( remote_control.client_ip, entry )
    uvmContext.hostTable().cleanup()
    
def host_hostname_set(str):
    entry = uvmContext.hostTable().getHostTableEntry( remote_control.client_ip )
    entry['hostnameDhcp'] = str
    uvmContext.hostTable().setHostTableEntry( remote_control.client_ip, entry )

def host_hostname_clear():
    entry = uvmContext.hostTable().getHostTableEntry( remote_control.client_ip )
    entry['hostnameDhcp'] = None
    uvmContext.hostTable().setHostTableEntry( remote_control.client_ip, entry )

def host_quota_clear():
    uvmContext.hostTable().removeQuota( remote_control.client_ip )

def host_quota_give(bytes_size, seconds):
    uvmContext.hostTable().giveHostQuota( remote_control.client_ip, bytes_size, seconds, "test" )

def user_tags_add(username, str):
    entry = uvmContext.userTable().getUserTableEntry( username, True )
    entry['tags']['list'].append( {
        "javaClass": "com.untangle.uvm.Tag",
        "name": str,
        "expirationTime": 0
    } )
    uvmContext.userTable().setUserTableEntry( username, entry )

def user_tags_clear(username):
    entry = uvmContext.userTable().getUserTableEntry( username, True )
    entry['tags']['list'] = []
    uvmContext.userTable().setUserTableEntry( username, entry )

def user_quota_clear(username):
    uvmContext.userTable().removeQuota( username )

def user_quota_give(username, bytes_size, seconds):
    uvmContext.userTable().giveUserQuota( username, bytes_size, seconds, "test" )
    
def random_email(length=10):
   return ''.join(random.choice(string.ascii_lowercase) for i in range(length)) + "@" + TEST_SERVER_HOST
    
def createPolicySingleConditionRule( conditionType, value, targetPolicy, blocked=True ):
    conditionTypeStr = str(conditionType)
    valueStr = str(value)
    return {
        "javaClass": "com.untangle.app.policy_manager.PolicyRule", 
        "ruleId": 1, 
        "enabled": True, 
        "description": "Single Matcher: " + conditionTypeStr + " = " + valueStr, 
        "targetPolicy" : targetPolicy,
        "conditions": {
            "javaClass": "java.util.LinkedList", 
            "list": [
                {
                    "invert": False, 
                    "javaClass": "com.untangle.app.policy_manager.PolicyRuleCondition", 
                    "conditionType": conditionTypeStr, 
                    "value": valueStr
                    }
                ]
            }
        }

def appendRule(app, newRule):
    settings = app.getSettings()
    settings['rules']['list'].append(newRule)
    app.setSettings(settings)

def nukeRules(app):
    settings = app.getSettings()
    settings['rules']['list'] = []
    app.setSettings(settings)

def addRack(app, name="New Rack", description="", parentId=None):
    currentSettings = app.getSettings()
    currentPolicies = currentSettings['policies']
    maxIdFound = 0
    for policy in currentPolicies['list']:
        if policy['policyId'] > maxIdFound:
            maxIdFound = policy['policyId']
    newPolicy = { "javaClass" : "com.untangle.app.policy_manager.PolicySettings", "policyId" : maxIdFound+1, "name": name, "description" : description, "parentId" : parentId }
    currentPolicies['list'].append(newPolicy)
    app.setSettings(currentSettings)
    return newPolicy['policyId']

def removeRack(app, id):
    currentSettings = app.getSettings()
    currentPolicies = currentSettings['policies']
    i = 0
    removed = False
    for policy in currentPolicies['list']:
        if policy['policyId'] == id:
            del currentPolicies['list'][i]
            removed = True
            break
        i = i + 1
    if removed:
        app.setSettings(currentSettings)
    return removed

def  get_network_interface():
    # Run the "ip addr" command remotely 
    output = remote_control.run_command("sudo ip addr", stdout=True)

    interfaces = []
    for line in output.splitlines():
        if 'lo' in line:
            continue  # Skip loopback interface
        if re.match(r'^\d+:', line):
            interface_name = line.split(":")[1].strip()
            interfaces.append(interface_name)

    # Return the first non-loopback interface
    return interfaces[0] if interfaces else None

def get_primary_ip(interface):
    # Get the primary IP address of the interface using "ip addr show"
    output = remote_control.run_command(f"sudo ip addr show {interface}", stdout=True)

    primary_ip = None
    for line in output.splitlines():
        if "inet" in line:
            primary_ip = line.split()[1].split('/')[0]
            break

    return primary_ip

def get_gateway():
    # Get the gatway of remote client
    lan_interface = None
    netsettings = uvmContext.networkManager().getNetworkSettings()
    for interface in netsettings['interfaces']['list']:
        if interface.get("configType") != 'ADDRESSED':
            continue
        if interface.get("isWan"):
            continue
        lan_interface = interface
        break
    gateway = lan_interface["v4StaticAddress"]
    return gateway

def create_route_table_entry(route_table_name):
    # Ensure the route table entry exists in /etc/iproute2/rt_tables on the remote server
    # Read the current contents of the /etc/iproute2/rt_tables file remotely
    command = "cat /etc/iproute2/rt_tables"
    tables = remote_control.run_command(command, stdout=True)

    # Check if the table already exists
    if route_table_name not in tables:
        # Find a unique ID for the new routing table
        next_id = str(len(tables.splitlines()) + 1)  # or use a specific algorithm to avoid collisions
        # Append the new route table entry remotely
        command = f"echo '{next_id} {route_table_name}' | sudo tee -a /etc/iproute2/rt_tables"
        remote_control.run_command(command)

def delete_route_table_entry(route_table_name):
    # Read the existing remote route tables
    command = "cat /etc/iproute2/rt_tables"
    tables = remote_control.run_command(command, stdout=True)
    # Remove the line containing the route_table_name
    updated_lines = []
    for line in tables.splitlines():
        if route_table_name not in line:
            updated_lines.append(line)

    if len(updated_lines) == len(tables.splitlines()):
        return
    # Construct the new content
    new_table_contents = "\n".join(updated_lines)
    #  Push the updated content remotely using a heredoc and sudo tee
    heredoc = f"""sudo tee /etc/iproute2/rt_tables > /dev/null <<EOF
    {new_table_contents}
    """
    remote_control.run_command(heredoc)

def add_secondary_ip(interface, primary_ip):
    # Calculate secondary IP by modifying the last octet of the primary IP
    ip_parts = primary_ip.split('.')
    ip_parts[-1] = str(int(ip_parts[-1]) + 1)  # Add 1 to the last octet for secondary IP
    secondary_ip = ".".join(ip_parts)
    # Check if the secondary IP already exists
    output = remote_control.run_command(f"sudo ip addr show {interface}", stdout=True)
    if secondary_ip in output:
        print("PRIMARY IP : ",primary_ip)
        print("SECONDARY IP : ",secondary_ip)
        return primary_ip, secondary_ip  # Skip adding if the IP already exists
    # Add the secondary IP to the network interface
    command = f"sudo ip addr add {secondary_ip}/24 dev {interface}"
    remote_control.run_command(command)
    route_table_name = f"rt{ip_parts[-1]}"
    create_route_table_entry(route_table_name)
    # Add the rule for the secondary IP
    route_table_command = f"sudo ip rule add from {secondary_ip} table {route_table_name}"
    remote_control.run_command(route_table_command)
    # Check if the route already exists
    check_command = f"sudo ip route show table {route_table_name}"
    route_output = remote_control.run_command(check_command, stdout=True)

    gateway = get_gateway()
    expected_route = f"default via {gateway} dev {interface}"

    if expected_route not in route_output:
        # Add the route since it does not exist
        route_command = f"sudo ip route add default via {gateway} dev {interface} table {route_table_name}"
        remote_control.run_command(route_command)
    print("PRIMARY IP : ",primary_ip)
    print("SECONDARY IP : ",secondary_ip)
    return primary_ip, secondary_ip

def remove_secondary_ip(interface, primary_ip):
    # Calculate secondary IP by modifying the last octet of the primary IP
    ip_parts = primary_ip.split('.')
    ip_parts[-1] = str(int(ip_parts[-1]) + 1)  # Add 1 to the last octet for secondary IP
    secondary_ip = ".".join(ip_parts)
    # Remove the secondary IP from the network interface
    command = f"sudo ip addr del {secondary_ip}/24 dev {interface}"
    remote_control.run_command(command)
    # Remove the secondary IP from the routing table
    route_table_command = f"sudo ip rule del from {secondary_ip} table rt{ip_parts[-1]}"
    remote_control.run_command(route_table_command)
    # Remove the default route from the custom table
    gateway = get_gateway()
    route_table_name = f"rt{ip_parts[-1]}"
    route_command = f"sudo ip route del default via {gateway} dev {interface} table {route_table_name}"
    remote_control.run_command(route_command)
    # Ensure the routing table entry is deleted
    delete_route_table_entry(route_table_name)

def __get_ip_address(ifname):
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        ifaddr = socket.inet_ntoa(fcntl.ioctl(
            s.fileno(),
            0x8915,  # SIOCGIFADDR
            struct.pack('256s', ifname[:15].encode("utf-8"))
        )[20:24])
    except IOError: # interface is present in routing tables but does not have any assigned ip
        ifaddr ="0.0.0.0"
    return ifaddr

def __get_gateway(ifname):
    cmd = "ip route | awk '/" + ifname + "\s+scope link/ {print $1}'"
    status, output = subprocess.getstatusoutput(cmd)
    if (not status) and output:
        return output
    else:
        return None

def check_clamd_ready():
    """
    check_clamd_ready will verify clamd is running and also that signatures are done downloading
    """
    clamdtimeout = 1200

    # call freshclam to update signatures
    freshClamResult = subprocess.call("freshclam >/dev/null 2>&1", shell=True)

    # wait until clamd service is running on 3310
    print("Waiting for server to start...")
    for i in range(clamdtimeout):
        #Increased sleep to ensure clamd is ready
        time.sleep(10)
        ncresult = subprocess.call("netcat -n -z 127.0.0.1 3310 >/dev/null 2>&1", shell=True)
        if ncresult == 0:
            break
    print("Number of sleep cycles waiting: %d" % i)

    return True

def build_wget_command(uri=None, tries=2, timeout=5, log_file=None, output_file=None, cookies_save_file=None, cookies_load_file=None, header=None, user_agent=None, post_data=None, override_arguments=None, extra_arguments=None, ignore_certificate=True, user=None, password=None, quiet=True, all_parameters=False, content_on_error=False, bind_address=None):
    """
    Build wget command

    wget is best for straight http (not https) testing

    Default arguments should be evident, but of particular note are:
    override_arguments  If you really want to ignore the standard arguments and options, use it.  For example, if you really wanted to use hsts.
    extra_arguments     Additional arguments not otherwise processed.
    """
    global Wget_hsts
    if uri is None:
        uri = f"http://{TEST_SERVER_HOST}/"

    arguments = []
    if override_arguments is not None:
        # Allow completely custom arguments
        arguments = override_arguments
    else:
        arguments.append("--no-hsts")
        # We only process ipv4.
        arguments.append("--inet4-only")

        if quiet is True:
            arguments.append("--quiet")
        if tries is not None:
            arguments.append(f"--tries={tries}")
        if timeout is not None:
            arguments.append(f"--timeout={timeout}")

        if ignore_certificate is True and "https" in uri:
            arguments.append(f"--no-check-certificate")

        if all_parameters is True:
            arguments.append(f'"$@"')

    optional_arguments = []
    if log_file is not None:
        optional_arguments.append(f"--output-file={log_file}")
    if output_file is not None:
        optional_arguments.append(f"--output-document={output_file}")
    if cookies_save_file is not None:
        optional_arguments.append(f"--save-cookies={cookies_save_file}")
    if cookies_load_file is not None:
        optional_arguments.append(f"--load-cookies={cookies_load_file}")
    if header is not None:
        optional_arguments.append(f"--header='{header}'")
    if user_agent is not None:
        optional_arguments.append(f"--user-agent='{user_agent}'")
    if post_data is not None:
        optional_arguments.append(f"--post-data='{post_data}'")
    if content_on_error is not False:
        optional_arguments.append(f"--content-on-error")
    if user is not None:
        optional_arguments.append(f"--user='{user}'")
    if password is not None:
        optional_arguments.append(f"--password='{password}'")
    if bind_address is not None:
        optional_arguments.append(f"--bind-address='{bind_address}'")

    if extra_arguments is not None:
        optional_arguments.append(extra_arguments)

    command = f"wget {' '.join(arguments)} {' '.join(optional_arguments)} '{uri}'"
    print(f"{sys._getframe().f_code.co_name}: {command}" )
    return command

def build_curl_command(uri=None, connect_timeout=10, max_time=20, output_file=None, override_arguments=None, extra_arguments=None, location=False, range=None, trace_ascii_file=None, user_agent=None, user=None, password=None, request=None, form=None, head=False, verbose=False):
    """
    Build curl command

    curl is best for straight https testing

    Default arguments should be evident, but of particular note are:
    override_arguments  If you really want to ignore the standard arguments and options, use it.
    extra_arguments     Additional arguments not otherwise processed.
    """
    if uri is None:
        uri = f"https://{TEST_SERVER_HOST}/"

    arguments = []
    if override_arguments is not None:
        # Allow completely custom arguments
        arguments = override_arguments
    else:
        arguments.append("--silent")
        arguments.append("--insecure")
        arguments.append("--location")
        if connect_timeout is not None:
            arguments.append(f"--connect-timeout {connect_timeout}")
        if max_time is not None:
            arguments.append(f"--max-time {max_time}")
        if location is True:
            arguments.append("--location")

    optional_arguments = []
    if output_file is not None:
        optional_arguments.append(f"--output {output_file}")
    if range is not None:
        optional_arguments.append(f"--range {range}")
    if trace_ascii_file is not None:
        optional_arguments.append(f"--trace-ascii {trace_ascii_file}")
    if user_agent is not None:
        optional_arguments.append(f"--user-agent '{user_agent}'")
    if extra_arguments is not None:
        optional_arguments.append(extra_arguments)
    if user is not None:
        if password is not None:
            optional_arguments.append(f"--user '{user}:{password}'")
        else:
            optional_arguments.append(f"--user '{user}'")
    if request is not None:
        optional_arguments.append(f"--request '{request}'")
    if form is not None:
        for key in form.keys():
            optional_arguments.append(f"--form '{key}={form[key]}'")
    if head is True:
        optional_arguments.append(f"--head")
    if verbose is True:
        optional_arguments.append(f"--verbose")

    command = f"curl {' '.join(arguments)} {' '.join(optional_arguments)} '{uri}'"
    print(f"{sys._getframe().f_code.co_name}: {command}" )
    return command

def build_postgres_command(override_arguments=None, extra_arguments=None, user="postgres", database="uvm", pset="pager=off", query=None):
    """
    Build posgres command

    override_arguments  If you really want to ignore the standard arguments and options, use it.
    extra_arguments     Additional arguments not otherwise processed.
    """
    if query is None:
        query = f"select count(*) from information_schema.tables where table_schema = 'reports'"

    arguments = []
    if override_arguments is not None:
        # Allow completely custom arguments
        arguments = override_arguments
    else:
        arguments.append("--tuples-only")
        arguments.append("--no-align")
        arguments.append(f"--username={user}")
        arguments.append(f"--dbname={database}")
        arguments.append(f"--pset={pset}")

    optional_arguments = []
    if extra_arguments is not None:
        optional_arguments.append(extra_arguments)

    if type(query) == list:
        query = ';'.join(query)

    command = f"psql {' '.join(arguments)} {' '.join(optional_arguments)} -c \"{query}\""
    print(f"{sys._getframe().f_code.co_name}: {command}" )
    return command


Output_names = []
def get_new_output_filename(name=None):
    """
    Get a unique, indexed filename into /tmp.

    if name not specified, use calling method's name.
    """
    if name is None:
        name = inspect.stack()[1].function

    index = 1
    base_name = "".join(c for c in name if c.isalnum() or c == '_')

    name = f"{base_name}_{index}"
    while name in Output_names:
        index += 1
        name = f"{base_name}_{index}"

    Output_names.append(name)

    return f"/tmp/{name}.out"

def is_vpn_running(interface=None, route_table=None):
    """
    Perform system checks to verify vpn is running properly
    """
    if interface:
        command = f"ip link show {interface}"
        try:
            result = subprocess.check_output(command, shell=True, stderr=subprocess.STDOUT)
        except subprocess.CalledProcessError as exc:
            result = exc.output
            pass
        if type(result) is bytes:
            result = result.decode("utf-8")
        print(f"command={command}")
        print(f"result={result}")
        if "does not exist" in result:
            return False

    if route_table:
        command = f"ip rule show table {route_table}"
        try:
            result = subprocess.check_output(command, shell=True, stderr=subprocess.STDOUT)
        except subprocess.CalledProcessError as exc:
            result = exc.output
            pass
        if type(result) is bytes:
            result = result.decode("utf-8")
        print(f"command={command}")
        print(f"result={result}")

        if "is invalid" in result:
            return False
        if "from all lookup" not in result:
            return False

    return True

def is_vpn_routing(route_table, expected_route=None):
    """
    Perform system checks to verify vpn is running properly
    """
    command = f"ip route show table {route_table}"
    result = ""
    try:
        result = subprocess.check_output(command, shell=True, stderr=subprocess.STDOUT)
    except subprocess.CalledProcessError as exc:
        result = exc.output
        pass
    if type(result) is bytes:
        result = result.decode("utf-8")
    print(f"command={command}")
    print(f"result={result}")

    if "is invalid" in result:
        return False

    if expected_route:
        # Verify specific route
        if expected_route in result:
            return True
    else:
        # Verify non-empty
        if result.strip() != "":
            return True

    return False

Vm_conf_filename = "/usr/share/untangle/conf/untangle-vm.conf"

def vm_conf_backup(filename=None):
    """
    Make a copy of the current untangle-vm.conf file
    """
    if filename is None:
        filename = f"{Vm_conf_filename}.orig"

    shutil.copyfile(Vm_conf_filename, filename)

    return filename

def vm_conf_restore(filename=None):
    """
    Copy file to untangle-vm.conf and perform uvm restart
    """
    global uvmContext, uvmContextLongTimeout

    if filename is None:
        filename = f"{Vm_conf_filename}.orig"

    shutil.copyfile(filename, Vm_conf_filename)

    return restart_uvm()

def vm_conf_update(search=None, replace=None):
    """
    Update vm_conf (into a temp file).
    """
    temp_filename = f"/tmp/untangle-vm.conf.tmp"

    vm_conf = []
    with open(Vm_conf_filename, "r") as file:
        for line in file:
            if line.startswith(search):
                line = f"{replace}\n"
            vm_conf.append(line)
        file.close()

    with open(temp_filename, "w") as file:
        for line in vm_conf:
            file.write(line)
        file.close()

    return vm_conf_restore(temp_filename)

def get_latest_client_test_pkg(name=None):
    """
    Download and install latest client test package
    """
    if name is not None:
        package_filename=f"{name}pkg.tar"
        remote_control.run_command(f"rm -f {package_filename}*") # remove all previous mail packages
        results = remote_control.run_command(build_wget_command(uri=f"http://test.untangle.com/test/{package_filename}"))
        results = remote_control.run_command(f"tar -xvf {package_filename}")

def is_clamav_receive_ready(data):
    """
    Verify Clamd communication, send message and verify in calling function
    """
    host = "127.0.0.1"
    port = 3310

    with socket.create_connection((host, port)) as sock:
        sock.sendall(b'nINSTREAM\n')
        size = len(data).to_bytes(4, byteorder='big')
        sock.sendall(size + data)
        sock.sendall(b'\x00\x00\x00\x00')  # End of stream
        response = sock.recv(1024)
        return response.decode().strip()


# Method to check if ClamAV is ready to accept connections
def clamav_not_ready_for_connections(log_file_path= "/var/log/clamav/clamav.log", timeout=300):
    start_time = time.time()
    # patterns that indicate ClamAV is ready to accept connections
    connection_patterns = [
        r"TCP: Received AF_INET SOCK_STREAM socket from systemd",
        r"LOCAL: Received AF_UNIX SOCK_STREAM socket from systemd"
    ]
    try:
        # Open the log file in read mode
        with open(log_file_path, 'r') as log_file:
            # Move the file pointer to the end of the file to read new log entries only
            log_file.seek(0, 2)  
            
            while True:
                # Read the new lines from the log file
                line = log_file.readline()
                if line == "":  
                    time.sleep(1)
                    continue
                
                # Check if any line matches the connection patterns
                if any(re.search(pattern, line) for pattern in connection_patterns):
                    print("ClamAV is ready to accept connections.")
                    return False
                
                # Check if the timeout has been exceeded
                if time.time() - start_time > timeout:
                    print("Timed out waiting for ClamAV to be ready to accept connections.")
                    return True
                
                print("Waiting for ClamAV to be ready to accept connections...")
                
    except FileNotFoundError:
        print(f"Error: Log file {log_file_path} not found.")
        return True

def is_apache_listening_on_ipv6_port80():
    """
    Checks if Apache is listening on IPv6 port 80 by parsing `netstat` output.
    Returns True if found, False otherwise.
    """
    try:
        output = subprocess.check_output(['sudo', 'netstat', '-tlnp'], stderr=subprocess.DEVNULL)
        lines = output.decode().splitlines()
        for line in lines:
            if line.startswith('tcp6') and ':::80' in line and 'apache2' in line:
                return True
        return False
    except subprocess.CalledProcessError as e:
        print("Error running netstat:", e)
        return False
    
def restart_apache():
    """
    Restarts the Apache2 service using systemctl.
    """
    try:
        print("Restarting Apache...")
        subprocess.run(['sudo', 'systemctl', 'restart', 'apache2'], check=True)
        # Give Apache time to restart
        time.sleep(5)
    except subprocess.CalledProcessError:
        print("Failed to restart Apache")

