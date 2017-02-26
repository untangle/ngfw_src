import os
import sys
import subprocess
import time
import datetime
import re

import remote_control
import system_properties
import ipaddr
import smtplib
import json

from uvm import Uvm

officeNetworks = ('10.111.0.0/16','10.112.0.0/16');
iperfServers = [('10.111.0.0/16','10.111.5.20'), # Office network
                ('10.112.0.0/16','10.112.56.44')] # ATS VM
iperfServer = ""

# special box with testshell in the sudoer group  - used to connect to vpn as client
# and ftp server.
ftpServer = vpnClientVpnIP = "10.111.5.20"  

# special Untangle box configured as a OpenVPN server and special DNS config
specialDnsServer= vpnServerVpnIP = "10.111.56.96"

# special box within vpnServerVpnIP's network
vpnServerVpnLanIP = "192.168.235.96"

# special box with testshell in the sudoer group  - used to connect to vpn as client
vpnClientVpnIP = "10.111.5.20"  

smtpServerHost = 'test.untangle.com'
tlsSmtpServerHost = '10.112.56.44'  # Vcenter VM Debian-ATS-TLS 
# DNS MX record on 10.111.56.57 for domains untangletestvm.com and untangletest.com
listFakeSmtpServerHosts = [('10.111.5.20','16','untangletest.com'),# Office network
                            ('10.112.56.30','16','untangletestvm.com')]# ATS VM

accountFileServer = "10.112.56.44"
accountFile = "/tmp/account_login.json"

uvmContext = Uvm().getUvmContext(timeout=120)
uvmContextLongTimeout = Uvm().getUvmContext(timeout=300)

def getIpAddress(base_URL="test.untangle.com",extra_options="",localcall=False):
    timeout = 4
    result = ""
    while result == "" and timeout > 0:
        timeout -= 1
        if localcall:
            result = subprocess.check_output("wget --timeout=4 " + extra_options + " -q -O - \"$@\" test.untangle.com/cgi-bin/myipaddress.py", shell=True)
        else:
            result = remote_control.runCommand("wget --timeout=4 " + extra_options + " -q -O - \"$@\" " + base_URL + "/cgi-bin/myipaddress.py",stdout=True)
    return result
    
def verifyIperf(wanIP):
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
    iperfRunning = remote_control.runCommand("pidof iperf", host=iperfServer)
    if iperfRunning == 0:
        print "iperf is already running on server."
        return False
    # Check that the client has iperf
    clientHasIperf = remote_control.runCommand("test -x /usr/bin/iperf")
    if clientHasIperf != 0:
        print "iperf not installed on client."
        return False
    return True

def findSmtpServer(wan_IP):
    smtp_IP = ""
    smtp_domain = "";
    match = False
    for smtpServerHostIP in listFakeSmtpServerHosts:
        interfaceNet = smtpServerHostIP[0] + "/" + str(smtpServerHostIP[1])
        if ipaddr.IPAddress(wan_IP) in ipaddr.IPv4Network(interfaceNet):
            match = True
            break

        # Verify that it will pass through our WAN network
        result = subprocess.check_output("traceroute -n -w 1 -q 1 " + smtpServerHostIP[0], shell=True)
        space_split = result.split("\n")[1].strip().split()
        if len(space_split) > 1:
            try:
                if ipaddr.IPAddress(space_split[1]) in ipaddr.IPv4Network(wan_IP + "/24"):
                    match = True
                    break
            except Exception,e:
                continue

    if match is True:
        smtp_IP = smtpServerHostIP[0]
        smtp_domain = smtpServerHostIP[2]

    return smtp_IP,smtp_domain

def getUDPSpeed( receiverIP, senderIP, targetIP=None, targetRate=None ):
    if targetIP == None:
        targetIP = receiverIP
    if targetRate == None:
        targetRate = "50M"

    # Use iperf to get UDP speed.  Returns number the udp speed
    # start iperf receivier on server
    remote_control.runCommand("iperf -s -p 5000 -u >/dev/null 2>&1 &", host=receiverIP)
    # start the UDP generator on the client behind the Untangle.
    iperf_tries = 5
    while iperf_tries > 0:  # try iperf a few times if it fails to send UDP packets correctly.
        report=remote_control.runCommand("iperf -c " + targetIP + " -u -p 5000 -b " + targetRate + " -t 10 -fK", host=senderIP, stdout=True)
        if '%' in report:
            break
        else:
            iperf_tries -= 1
    # kill iperf receiver    
    remote_control.runCommand("pkill iperf", host=receiverIP)

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

def getDownloadSpeed():
    try:
        # Download file and record the average speed in which the file was download
        result = remote_control.runCommand("wget -t 3 --timeout=60 -O /dev/null -o /dev/stdout http://test.untangle.com/5MB.zip 2>&1 | tail -2", stdout=True)
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
    reports = uvmContextLongTimeout.nodeManager().node("untangle-node-reports")
    if reports == None:
        print "WARNING: reports app not found"
        return None

    reports.flushEvents()

    reportsManager = reports.getReportsManager()

    reportEntry = reportsManager.getReportEntry( eventEntryCategory, eventEntryTitle )
    if reportEntry == None:
        print "WARNING: Event entry not found: %s %s" % (eventEntryCategory, eventEntryTitle)
        return None

    return reportsManager.getEvents( reportEntry, conditions, limit )

def find_event( events, num_events, *args, **kwargs):
    if events == None:
        return None
    if num_events == 0:
        return None
    if len(events) == 0:
        print "No events in list"
        return None
    if kwargs.get('min_date') == None:
        min_date = datetime.datetime.now()-datetime.timedelta(minutes=12)
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

def isInOfficeNetwork(wanIP):
    for officeNetworkTest in officeNetworks:
        if ipaddr.IPv4Address(wanIP) in ipaddr.IPv4Network(officeNetworkTest):
            return True
            break
    return False

def isBridged(wanIP):
    result = remote_control.runCommand("ip -o -f inet addr show",stdout=True)
    match = re.search(r'\d{1,3}.\d{1,3}.\d{1,3}.\d{1,3}\/\d{1,3} brd', result)
    hostname_cidr = (match.group()).replace(' brd','')
    if ipaddr.IPv4Address(wanIP) in ipaddr.IPv4Network(hostname_cidr):
        return True
    return False
    
def sendTestmessage(mailhost=smtpServerHost):
    sender = 'test@example.com'
    receivers = ['qa@example.com']

    message = """From: Test <test@example.com>
    To: Test Group <qa@example.com>
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

def getStatusValue(node, label):
    metric = node.getMetric(label)
    if metric == None:
        print "Missing metric: %s"%str(label) 
        return 0
    if metric.get('value') == None:
        print "Missing metric value: %s"%str(label) 
        return 0
    return metric.get('value')

def getLiveAccountInfo(accounttype):
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

def foundWans():
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
                wanIndex =  interface['interfaceId']
                wanIP =  system_properties.__get_ip_address(nicDevice)
                wanGateway =  system_properties.__get_gateway(nicDevice)
            if wanIP:
                wanExtIP = getIpAddress(extra_options="--bind-address=" + wanIP,localcall=True)
                wanExtIP = wanExtIP.rstrip()
                wanTup = (wanIndex,wanIP,wanExtIP,wanGateway)
                myWANs.append(wanTup)
    return myWANs
