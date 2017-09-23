import os
import sys
import subprocess
import time
import re
import socket
import fcntl
import struct
import commands
import datetime
import random
import string

import remote_control
import ipaddr
import smtplib
import json

from uvm import Uvm

officeNetworks = ('10.111.0.0/16','10.112.0.0/16');
iperfServers = [('10.111.0.0/16','10.111.5.20'), # Office network
                #('10.112.0.0/16','10.112.56.44')
                ] # ATS VM
iperfServer = ""
radiusServer = "10.111.56.28"
adServer = "10.111.56.46"

# special Untangle box configured as a OpenVPN server
vpnServerVpnIP = "10.111.56.96"

# special box within vpnServerVpnIP's network
vpnServerVpnLanIP = "192.168.235.96"

# special box with testshell in the sudoer group  - used to connect to vpn as client
vpnClientVpnIP = "10.111.5.20"  

testServerHost = 'test.untangle.com'
testServerIp = socket.gethostbyname(testServerHost)
ftpServer = socket.gethostbyname(testServerHost)

# Servers running remote syslog
listSyslogServer = '10.111.5.20'

accountFileServer = "10.111.56.29"
accountFile = "/tmp/account_login.json"

uvmContext = Uvm().getUvmContext(timeout=120)
uvmContextLongTimeout = Uvm().getUvmContext(timeout=300)
prefix = "@PREFIX@"

test_start_time = None

def get_public_ip_address(base_URL="test.untangle.com",extra_options="",localcall=False):
    timeout = 4
    result = ""
    while result == "" and timeout > 0:
        timeout -= 1
        if localcall:
            result = subprocess.check_output("wget --timeout=4 " + extra_options + " -q -O - \"$@\" test.untangle.com/cgi-bin/myipaddress.py", shell=True)
        else:
            result = remote_control.run_command("wget --timeout=4 " + extra_options + " -q -O - \"$@\" " + base_URL + "/cgi-bin/myipaddress.py",stdout=True)
    return result
    
def verify_iperf_configuration(wanIP):
    # https://iperf.fr/
    global iperfServer
    # check if there is an iperf server on the same network
    for iperfServerSet in iperfServers:
        if ipaddr.IPv4Address(wanIP) in ipaddr.IPv4Network(iperfServerSet[0]):
            iperfServer = iperfServerSet[1]
            break
    if iperfServer == "":
        print "No iperf server in the same network"
        return False
    # Check to see if iperf endpoint is reachable
    iperfServerReachable = subprocess.call(["ping -c 1 " + iperfServer + " >/dev/null 2>&1"],shell=True,stdout=None,stderr=None)
    if iperfServerReachable != 0:
        print "iperf Server is unreachable."
        return False
    # Check to see if some other test is using iperf for UDP testing
    iperfRunning = remote_control.run_command("pidof iperf", host=iperfServer)
    if iperfRunning == 0:
        print "iperf is already running on server."
        return False
    # Check that the client has iperf
    clientHasIperf = remote_control.run_command("test -x /usr/bin/iperf")
    if clientHasIperf != 0:
        print "iperf not installed on client."
        return False
    return True

def find_syslog_server(wan_IP):
    syslog_IP = ""
    if is_in_office_network(wan_IP):
        syslog_IP = listSyslogServer
    return syslog_IP

def get_udp_download_speed( receiverIP, senderIP, targetIP=None, targetRate=None ):
    if targetIP == None:
        targetIP = receiverIP
    if targetRate == None:
        targetRate = "50M"

    # Use iperf to get UDP speed.  Returns number the udp speed
    # start iperf receivier on server
    remote_control.run_command("iperf -s -p 5000 -u >/dev/null 2>&1 &", host=receiverIP)
    # start the UDP generator on the client behind the Untangle.
    iperf_tries = 5
    while iperf_tries > 0:  # try iperf a few times if it fails to send UDP packets correctly.
        report=remote_control.run_command("iperf -c " + targetIP + " -u -p 5000 -b " + targetRate + " -t 10 -fK", host=senderIP, stdout=True)
        if '%' in report:
            break
        else:
            iperf_tries -= 1
    # kill iperf receiver    
    remote_control.run_command("pkill iperf", host=receiverIP)

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

def get_download_speed():
    try:
        # Download file and record the average speed in which the file was download
        result = remote_control.run_command("wget -t 3 --timeout=60 -O /dev/null -o /dev/stdout http://test.untangle.com/5MB.zip 2>&1 | tail -2", stdout=True)
        match = re.search(r'([0-9.]+) [KM]B\/s', result)
        bandwidth_speed =  match.group(1)
        # cast string to float for comparsion.
        bandwidth_speed = float(bandwidth_speed)
        # adjust value if MB or KB
        if "MB/s" in result:
            bandwidth_speed *= 1000
        # print "bandwidth_speed <%s>" % bandwidth_speed
        return bandwidth_speed
    except Exception,e:
        return None

def get_events( eventEntryCategory, eventEntryTitle, conditions, limit ):
    reports = uvmContextLongTimeout.appManager().app("reports")
    if reports == None:
        print "WARNING: reports app not found"
        return None

    reports.flushEvents()

    reportsManager = reports.getReportsManager()

    reportEntry = reportsManager.getReportEntry( eventEntryCategory, eventEntryTitle )
    if reportEntry == None:
        print "WARNING: Event entry not found: %s %s" % (eventEntryCategory, eventEntryTitle)
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
        print "No events in list"
        return None
    if kwargs.get('min_date') == None:
        min_date = test_start_time
    else:
        min_date = kwargs.get('min_date')
    if (len(args) % 2) != 0:
        print "Invalid argument length"
        return None
    num_checked = 0
    while num_checked < num_events:
        if len(events) <= num_checked:
            print "failed to find event checked: %i total: %i" % (num_checked, len(events)) 
            break
        event = events[num_checked]
        num_checked += 1

        # if event has a date and its too old - ignore the event
        if event.get('time_stamp') != None:
            time_stamp = event.get('time_stamp')
            if type(time_stamp) is int:
                ts = datetime.datetime.fromtimestamp((time_stamp/1000)+1)#round up
            elif type(time_stamp) is long:
                ts = datetime.datetime.fromtimestamp((time_stamp/1000)+1)#round up
            else:
                ts = datetime.datetime.fromtimestamp((time_stamp['time']/1000)+1)#round up
            if ts < min_date:
                print "ignoring old event: %s < %s " % (ts.isoformat(),min_date.isoformat())
                continue

        # check each expected value
        # if one doesn't match continue to the next event
        # if all match, return True
        allMatched = True
        for i in range(0, len(args)/2):
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
            #print "key %s expectedValue %s actualValue %s " % ( key, str(expectedValue), str(actualValue) )
            if str(expectedValue) != str(actualValue) and str(alternateValue) != str(actualValue):
                print "mismatch event[%s] expectedValue %s != actualValue %s " % ( key, str(expectedValue), str(actualValue) )
                allMatched = False
                break

        if allMatched:
            return event
    return None

def check_events( events, num_events, *args, **kwargs):
    return (find_event( events, num_events, *args, **kwargs) != None)

def is_in_office_network(wanIP):
    for officeNetworkTest in officeNetworks:
        if ipaddr.IPv4Address(wanIP) in ipaddr.IPv4Network(officeNetworkTest):
            return True
            break
    return False

def is_bridged(wanIP):
    result = remote_control.run_command("ip -o -f inet addr show",stdout=True)
    match = re.search(r'\d{1,3}.\d{1,3}.\d{1,3}.\d{1,3}\/\d{1,3} brd', result)
    hostname_cidr = (match.group()).replace(' brd','')
    if ipaddr.IPv4Address(wanIP) in ipaddr.IPv4Network(hostname_cidr):
        return True
    return False
    
def send_test_email(mailhost=testServerHost):
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
       print "Successfully sent email through " + mailhost
       return 1
    except smtplib.SMTPException, e:
       print "Error: unable to send email through " + mailhost + " " + str(e)
       return 0

def get_app_metric_value(app, label):
    metric = app.getMetric(label)
    if metric == None:
        print "Missing metric: %s"%str(label) 
        return 0
    if metric.get('value') == None:
        print "Missing metric value: %s"%str(label) 
        return 0
    return metric.get('value')

def get_live_account_info(accounttype):
    # Tries to file account password file and returns account and password if available
    accountFileServerPing = subprocess.call(["ping","-c","1",accountFileServer],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
    if accountFileServerPing != 0:
        return ("message",accountFileServer + " not available")
    # result_ping = subprocess.check_output("ping -c 1 " + accountFileServer, shell=True)
    # remove old file if it exist
    subprocess.call("wget -q -4 -t 2 --timeout=5 http://" + accountFileServer + "/account_login.json -O " + accountFile, shell=True)
    if not os.path.isfile(accountFile):
        return ("message",accountFile + " file not available")
    with open(accountFile) as data_file:    
        accounts = json.load(data_file)    
    if os.path.isfile(accountFile):
        os.remove(accountFile)
    for account in accounts: #i is each student's name, class, and number
        if account[0] == accounttype:
            return (account[1], account[2])
    return ("message",accounttype + " account not found")

def get_wan_tuples():
    myWANs = []
    netsettings = uvmContext.networkManager().getNetworkSettings()
    for interface in netsettings['interfaces']['list']:
        wanIP = ""
        wanGateway = ""
        if interface['isWan']:
            if interface['v4ConfigType'] == "STATIC":
                wanIndex =  interface['interfaceId']
                wanIP =  interface['v4StaticAddress']
                wanGateway =  interface['v4StaticGateway']
            elif interface['v4ConfigType'] == "AUTO":
                nicDevice = str(interface['symbolicDev'])
                wanIndex = interface['interfaceId']
                wanIP = __get_ip_address(nicDevice)
                wanGateway = __get_gateway(nicDevice)
            if wanIP:
                wanExtIP = get_public_ip_address(extra_options="--bind-address=" + wanIP,localcall=True)
                wanExtIP = wanExtIP.rstrip()
                wanTup = (wanIndex,wanIP,wanExtIP,wanGateway)
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

def get_test_start_time():
    global test_start_time
    return test_start_time

def set_test_start_time():
    global test_start_time
    test_start_time = datetime.datetime.now()

def set_previous_test_name( name ):
    global previous_test_name
    previous_test_name = name

def get_previous_test_name():
    global previous_test_name
    return previous_test_name
  
def host_username_set(username):
    entry = uvmContext.hostTable().getHostTableEntry( remote_control.clientIP )
    entry['usernameDirectoryConnector'] = username
    uvmContext.hostTable().setHostTableEntry( remote_control.clientIP, entry )

def host_username_clear():
    entry = uvmContext.hostTable().getHostTableEntry( remote_control.clientIP )
    entry['usernameDirectoryConnector'] = None
    uvmContext.hostTable().setHostTableEntry( remote_control.clientIP, entry )

def host_tags_add(str):
    entry = uvmContext.hostTable().getHostTableEntry( remote_control.clientIP )
    entry['tags']['list'].append( {
        "javaClass": "com.untangle.uvm.Tag",
        "name": str,
        "expirationTime": int(round((time.time()+60) * 1000)) #60 seconds from now
    } )
    uvmContext.hostTable().setHostTableEntry( remote_control.clientIP, entry )

def host_tags_clear():
    entry = uvmContext.hostTable().getHostTableEntry( remote_control.clientIP )
    for t in entry['tags']['list']:
        t['expirationTime'] = 1; #expire 1970
    uvmContext.hostTable().setHostTableEntry( remote_control.clientIP, entry )
    uvmContext.hostTable().cleanup()
    
def host_hostname_set(str):
    entry = uvmContext.hostTable().getHostTableEntry( remote_control.clientIP )
    entry['hostnameDhcp'] = str
    uvmContext.hostTable().setHostTableEntry( remote_control.clientIP, entry )

def host_hostname_clear():
    entry = uvmContext.hostTable().getHostTableEntry( remote_control.clientIP )
    entry['hostnameDhcp'] = None
    uvmContext.hostTable().setHostTableEntry( remote_control.clientIP, entry )

def host_quota_clear():
    uvmContext.hostTable().removeQuota( remote_control.clientIP )

def host_quota_give(bytes_size, seconds):
    uvmContext.hostTable().giveHostQuota( remote_control.clientIP, bytes_size, seconds, "test" )

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
   return ''.join(random.choice(string.lowercase) for i in range(length)) + "@" + testServerHost
    
def __get_ip_address(ifname):
    print "ifname <%s>" % ifname
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        ifaddr = socket.inet_ntoa(fcntl.ioctl(
            s.fileno(),
            0x8915,  # SIOCGIFADDR
            struct.pack('256s', ifname[:15])
        )[20:24])
    except IOError: # interface is present in routing tables but does not have any assigned IP
        ifaddr ="0.0.0.0"
    return ifaddr

def __get_gateway(ifname):
    cmd = "route -n | grep '[ \t]" + ifname + "' | grep 'UH[ \t]' | awk '{print $1}'"
    status, output = commands.getstatusoutput(cmd)
    if (not status) and output:
        return output
    else:
        return None
