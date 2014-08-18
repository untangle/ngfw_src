import os
import sys
import subprocess
import time
import datetime
import re

import remote_control
import system_properties

iperfServer = "10.111.56.32"

def verifyIperf():
    # https://iperf.fr/
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

def getUDPSpeed( receiverIP, senderIP, targetIP=None, targetRate=None ):
    if targetIP == None:
        targetIP = receiverIP
    if targetRate == None:
        targetRate = "50M"

    # Use iperf to get UDP speed.  Returns number the udp speed
    # start iperf receivier on server
    remote_control.runCommand("iperf -s -p 5000 -u >/dev/null 2>&1 &", host=receiverIP)
    # start the UDP generator on the client behind the Untangle.
    report=remote_control.runCommand("iperf -c " + targetIP + " -u -p 5000 -b " + targetRate + " -t 10 -fK", host=senderIP, stdout=True)
    # kill iperf receiver    
    remote_control.runCommand("pkill iperf", host=receiverIP)

    lines = report.split("\n")
    udp_speed = None
    for line in lines:
        if '%' in line: # results line contains a '%'
            match = re.search(r'([0-9.]+) KBytes/sec', line)
            udp_speed =  match.group(1)
            break
    return float(udp_speed)

def getDownloadSpeed():
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

def check_events( events, num_events, *args, **kwargs):
    if events == None:
        return False
    if num_events == 0:
        return False
    if kwargs.get('min_date') == None:
        min_date = datetime.datetime.now()-datetime.timedelta(minutes=10)
    else:
        min_date = kwargs.get('min_date')
    if (len(args) % 2) != 0:
        print "Invalid argument length"
        return False
    num_checked = 0
    while num_checked < num_events:
        if len(events) <= num_checked:
            break
        event = events[num_checked]
        num_checked += 1

        # if event has a date and its too old - ignore the event
        if event.get('time_stamp') != None and datetime.datetime.fromtimestamp((event['time_stamp']['time'])/1000) < min_date:
            continue

        # check each expected value
        # if one doesn't match continue to the next event
        # if all match, return True
        allMatched = True
        for i in range(0, len(args)/2):
            key=args[i*2]
            expectedValue=args[i*2+1]
            actualValue = event.get(key)
            #print "key %s expectedValue %s actualValue %s " % ( key, str(expectedValue), str(actualValue) )
            if str(expectedValue) != str(actualValue):
                print "mismatch event[%s] expectedValue %s != actualValue %s " % ( key, str(expectedValue), str(actualValue) )
                allMatched = False
                break

        if allMatched:
            return True
    return False

