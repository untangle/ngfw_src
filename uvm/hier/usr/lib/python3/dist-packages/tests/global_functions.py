"""ngfw test utilities"""
import subprocess
import time
import re
import socket
import fcntl
import struct
import datetime
import random
import string
import html
import inspect
import sys

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

# special Untangle box configured as WireGuard VPN server
WG_VPN_SERVER_IP = "10.113.150.117"
WG_VPN_SERVICE_INFO = {
        "hostname":"untangle-ats-wireguard",
        "publicKey":"fupwK1yQLvtBOFpW8nHxjIYjSDAzkpCwYGYL2rS5xUU=",
        "endpointHostname":"10.113.150.117",
        "endpointPort":51820,
        "peerAddress":"172.31.53.1",
        "networks":"192.168.20.0/24"
}

#  special box within WG_VPN_SERVER_IP's network
WG_VPN_SERVER_LAN_IP = "192.168.20.170"

# Servers running remote syslog
LIST_SYSLOG_SERVER = '10.112.56.23'

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

def get_public_ip_address(base_URL=TEST_SERVER_HOST,extra_options="",localcall=False):
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
                result = subprocess.check_output(build_wget_command(output_file="-", uri=f"{base_URL}/cgi-bin/myipaddress.py", all_parameters=True, extra_arguments=extra_options), shell=True)
            except:
                pass
        else:
            result = remote_control.run_command(build_wget_command(output_file="-", uri=f"{base_URL}/cgi-bin/myipaddress.py", all_parameters=True, extra_arguments=extra_options), stdout=True)
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
    global uvmContext
    subprocess.call(["/etc/init.d/untangle-vm","restart"],stdout=subprocess.PIPE,stderr=subprocess.PIPE)

    uvmContext = None
    max_tries = 60
    while max_tries > 0:
        try:
            uvmContext = Uvm().getUvmContext(timeout=240)
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
        report=remote_control.run_command("iperf -c " + targetip + " -u -p 5000 -b " + targetRate + " -t 10 -fK", host=senderip, stdout=True)

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
        result = remote_control.run_command(build_wget_command(quiet=False, log_file="/dev/stdout", output_file="/dev/null", timeout=60, uri="http://" + download_server + f"/{meg}MB.zip") +  " 2>&1 | tail -2", stdout=True)
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

def get_events( eventEntryCategory, eventEntryTitle, conditions, limit ):
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
    Retreive events from event report and look for matching events
    """
    events = get_events(report_category, report_title, report_conditions, event_limit)
    assert events != None, f"{prefix} total events found"
    print(events)
    found = check_events( events.get('list'), check_num_events, matches)
    assert found, f"{prefix} matches found"


def is_in_office_network(wan_ip):
    office_networks = overrides.get("OFFICE_NETWORKS")
    if office_networks is None:
        office_networks = OFFICE_NETWORKS
    for office_network_test in office_networks:
        if ipaddr.IPv4Address(wan_ip) in ipaddr.IPv4Network(office_network_test):
            return True
    return False

def is_bridged(wan_ip):
    result = remote_control.run_command("ip -o -f inet addr show",stdout=True)
    match = re.search(r'\d{1,3}.\d{1,3}.\d{1,3}.\d{1,3}\/\d{1,3} brd', result)
    hostname_cidr = (match.group()).replace(' brd','')
    if ipaddr.IPv4Address(wan_ip) in ipaddr.IPv4Network(hostname_cidr):
        return True
    return False

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
       smtpObj = smtplib.SMTP(mailhost)
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
                wanTup = (wanIndex,wan_ip,wanExtip,wanGateway)
                # wanTup = (wanIndex,wan_ip,wanExtip,wanGateway)
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
        time.sleep(1)
        ncresult = subprocess.call("netcat -n -z 127.0.0.1 3310 >/dev/null 2>&1", shell=True)
        if ncresult == 0:
            break
    print("Number of sleep cycles waiting: %d" % i)

    return True

def build_wget_command(uri=None, tries=2, timeout=5, log_file=None, output_file=None, cookies_save_file=None, cookies_load_file=None, header=None, user_agent=None, post_data=None, override_arguments=None, extra_arguments=None, ignore_certificate=True, user=None, password=None, quiet=True, all_parameters=False, content_on_error=False):
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

    if extra_arguments is not None:
        optional_arguments.append(extra_arguments)

    command = f"wget {' '.join(arguments)} {' '.join(optional_arguments)} '{uri}'"
    print(f"{sys._getframe().f_code.co_name}: {command}" )
    return command

def build_curl_command(uri=None, connect_timeout=10, max_time=20, output_file=None, override_arguments=None, extra_arguments=None, location=False, range=None, trace_ascii_file=None, user_agent=None, user=None, password=None, request=None, form=None):
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
