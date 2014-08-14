import os
import sys
import subprocess
import time
import datetime

import remote_control
import system_properties

iperfServer = "10.111.56.32"

def verifyIperf():
    # https://iperf.fr/
    iperfPresent = False
    # Check to see if iperf endpoint is reachable
    externalClientResult = subprocess.call(["ping","-c","1",iperfServer],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
    # Check to see if some other test is using iperf for UDP testing
    isIperfNotRunning = os.system("ssh -o 'StrictHostKeyChecking=no' -i " + systemProperties.getPrefix() + "/usr/lib/python2.7/untangle_tests/testShell.key testshell@" + iperfServer + " \"pidof iperf >/dev/null 2>&1\"")
    # print "externalClientResult <%s>" % externalClientResult
    # print "isIperfNotRunning <%s>" % isIperfNotRunning
    if (externalClientResult == 0 and isIperfNotRunning):
        # Iperf server, the iperf endpoint is reachable, check other requirements
        iperfResult = remote_control.runCommand("test -x /usr/bin/iperf")
        # print "iperfResult <%s>" % iperfResult
        if (iperfResult == 0):
            iperfPresent = True
    return iperfPresent

def getUDPSpeed():
    # Use iperf to get UDP speed.  Returns number of packets received.
    # start iperf receiver on iperf server.
    os.system("ssh -o 'StrictHostKeyChecking=no' -i " + systemProperties.getPrefix() + "/usr/lib/python2.7/untangle_tests/testShell.key testshell@" + iperfServer + " \"rm -f iperf_recv.dat\" >/dev/null 2>&1")
    os.system("ssh -o 'StrictHostKeyChecking=no' -i " + systemProperties.getPrefix() + "/usr/lib/python2.7/untangle_tests/testShell.key testshell@" + iperfServer + " \"iperf -s -p 5000 -u > iperf_recv.dat &\"")
    # start the UDP generator on the client behind the Untangle.
    remote_control.runCommand("iperf -c " + iperfServer + " -u -p 5000 -b 10M -t 20")
    # wait for UDP to finish
    time.sleep(25)
    # kill iperf receiver    
    os.system("ssh -o 'StrictHostKeyChecking=no' -i " + systemProperties.getPrefix() + "/usr/lib/python2.7/untangle_tests/testShell.key testshell@" + iperfServer + " \"pkill iperf\"  >/dev/null 2>&1")
    os.system("scp -o 'StrictHostKeyChecking=no' -i " + systemProperties.getPrefix() + "/usr/lib/python2.7/untangle_tests/testShell.key testshell@" + iperfServer + ":iperf_recv.dat /tmp/iperf_recv.dat >/dev/null 2>&1")
    f = open('/tmp/iperf_recv.dat')
    results = f.readlines()
    f.close()
    for data in results:
        if 'bits/sec' in data:
            match = re.search(r'([0-9.]+) [KM]Bytes', data)
            udp_speed =  match.group(1)
            break
    return float(udp_speed)

def sendUDPPackets(targetIP):
    # Use iperf to send UDP packets.  Returns number of packets received.
    # start iperf receiver on client.
    remote_control.runCommand("rm -f iperf_recv.dat")
    remote_control.runCommand("iperf -s -p 5000 -u > iperf_recv.dat", False, True)
    # start the UDP generator on the iperf server.
    os.system("ssh -o 'StrictHostKeyChecking=no' -i " + systemProperties.getPrefix() + "/usr/lib/python2.7/untangle_tests/testShell.key testshell@" + iperfServer + " \"iperf -c " + targetIP + " -u -p 5000 -b 10M -t 20\" >/dev/null 2>&1")
    # wait for UDP to finish
    time.sleep(25)
    # kill iperf receiver    
    remote_control.runCommand("pkill iperf")
    f = open('/tmp/iperf_recv.dat')
    results = f.readlines()
    f.close()
    for data in results:
        if 'bits/sec' in data:
            match = re.search(r'([0-9.]+) [KM]Bytes', data)
            numOfPackets =  match.group(1)
            break
    return float(numOfPackets)
        
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

