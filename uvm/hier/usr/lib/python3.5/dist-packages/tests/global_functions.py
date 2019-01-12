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

import runtests.remote_control as remote_control
import runtests
from . import ipaddr
import smtplib
import json
import urllib.request, urllib.parse, urllib.error

from uvm import Uvm

# ATS Global Constants
OFFICE_NETWORKS = ('10.111.0.0/16','10.112.0.0/16','10.113.0.0/16')
IPERF_SERVERS = [('10.111.0.0/16','10.111.56.23'),] # SJ Office network
RADIUS_SERVER = "10.111.56.28"
RADIUS_SERVER_PASSWORD = "chakas"
RADIUS_USER = "normal"
RADIUS_PASSWORD = "passwd"
AD_SERVER = "10.111.56.46"
AD_ADMIN = "ATSadmin"
AD_PASSWORD = "passwd"
AD_DOMAIN = "adtest.adtesting.int"
AD_USER = "user_28004"
TEST_SERVER_HOST = 'test.untangle.com'
ACCOUNT_FILE_SERVER = "10.111.56.29"
ACCOUNT_FILE = "/tmp/account_login.json"

# special Untangle box configured as a OpenVPN server
VPN_SERVER_IP = "10.111.56.96"

# special box within VPN_SERVER_IP's network
VPN_SERVER_LAN_IP = "192.168.235.96"

# special Untangle box configured as a OpenVPN server with User/Pass authentication enabled
VPN_SERVER_USER_PASS_IP = "10.111.56.91"

# special box within VPN_SERVER_USER_PASS_IP's network
VPN_SERVER_USER_PASS_LAN_IP = "192.168.235.91"

# special box with testshell in the sudoer group  - used to connect to vpn as client
VPN_CLIENT_IP = "10.111.56.23"  

# Servers running remote syslog
LIST_SYSLOG_SERVER = '10.111.56.23'

uvmContext = Uvm().getUvmContext(timeout=240)
uvmContextLongTimeout = Uvm().getUvmContext(timeout=300)
prefix = "@PREFIX@"

test_server_ip = socket.gethostbyname(TEST_SERVER_HOST)
ftp_server = test_server_ip
iperf_server = ""

def get_public_ip_address(base_URL=TEST_SERVER_HOST,extra_options="",localcall=False):
    timeout = 4
    result = ""
    while result == "" and timeout > 0:
        timeout -= 1
        time.sleep(1)
        if localcall:
            try:
                result = subprocess.check_output("wget --timeout=4 " + extra_options + " -q -O - \"$@\" test.untangle.com/cgi-bin/myipaddress.py", shell=True)
            except:
                pass
        else:
            result = remote_control.run_command("wget --timeout=4 " + extra_options + " -q -O - \"$@\" " + base_URL + "/cgi-bin/myipaddress.py",stdout=True)
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
    
def verify_iperf_configuration(wan_ip):
    # https://iperf.fr/
    global iperf_server
    # check if there is an iperf server on the same network
    for iperf_server_pair in IPERF_SERVERS:
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
    # start iperf receivier on server
    remote_control.run_command("iperf -s -p 5000 -u >/dev/null 2>&1 &", host=receiverip)
    # start the UDP generator on the client behind the Untangle.
    iperf_tries = 5
    while iperf_tries > 0:  # try iperf a few times if it fails to send UDP packets correctly.
        report=remote_control.run_command("iperf -c " + targetip + " -u -p 5000 -b " + targetRate + " -t 10 -fK", host=senderip, stdout=True)
        if '%' in report:
            break
        else:
            iperf_tries -= 1

    # kill iperf receiver and verify
    iperfRunning = 0
    timeout = 60
    while iperfRunning == 0 and timeout > 0:
        timeout -= 1
        remote_control.run_command("pkill iperf", host=receiverip)
        time.sleep(1)
        iperfRunning = remote_control.run_command("pidof iperf", host=receiverip)

    lines = report.split("\n")
    udp_speed = None
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
        result = remote_control.run_command("wget -t 3 --timeout=60 -O /dev/null -o /dev/stdout http://" + download_server + "/%iMB.zip 2>&1 | tail -2"%meg, stdout=True)
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
        print("Invalid argument length")
        return None
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
            elif type(time_stamp) is int:
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
        for i in range(0, int(len(args)/2)):
            key = args[i*2]
            expectedValue = args[i*2+1]
            actualValue = event.get(key)
            alternateValue = expectedValue
            # If the type is a boolean, accept 1/0 also
            if type(expectedValue) is bool:
                if expectedValue:
                    alternateValue = 1
                else:
                    alternateValue = 0
            print(("key %s expectedValue %s actualValue %s " % ( key, str(expectedValue), str(actualValue) )))
            if str(expectedValue) != str(actualValue) and str(alternateValue) != str(actualValue):
                print(("mismatch event[%s] expectedValue %s != actualValue %s " % ( key, str(expectedValue), str(actualValue) )))
                allMatched = False
                break

        if allMatched:
            return event
    return None

def check_events( events, num_events, *args, **kwargs):
    return (find_event( events, num_events, *args, **kwargs) != None)

def is_in_office_network(wan_ip):
    for office_network_test in OFFICE_NETWORKS:
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
    # Tries to file account password file and returns account and password if available
    ACCOUNT_FILE_SERVERPing = subprocess.call(["ping","-c","1",ACCOUNT_FILE_SERVER],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
    if ACCOUNT_FILE_SERVERPing != 0:
        return ("message",ACCOUNT_FILE_SERVER + " not available")
    # result_ping = subprocess.check_output("ping -c 1 " + ACCOUNT_FILE_SERVER, shell=True)
    # remove old file if it exist
    account_url = "http://" + ACCOUNT_FILE_SERVER + "/account_login.json"
    response = urllib.request.urlopen(account_url)
    accounts = json.loads(response.read())

    for account in accounts: #i is each student's name, class, and number
        if account[0] == accounttype:
            return (account[1], account[2])
    return ("message",accounttype + " account not found")

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
