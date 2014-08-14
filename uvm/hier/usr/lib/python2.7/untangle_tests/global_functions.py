import os
import sys
import subprocess
import time
import datetime

import remote_control
import system_properties

radiusServer = "10.111.56.71"

# http://www.nrl.navy.mil/itd/ncs/products/mgen
def verifyMgen():
    mGenPresent = False
    # Check to see if mgen endpoint is reachable
    externalClientResult = subprocess.call(["ping","-c","1",radiusServer],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
    # Check to see if some other test is using mgen for UDP testing
    isMgenNotRunning = os.system("ssh -o 'StrictHostKeyChecking=no' -i " + system_properties.getPrefix() + "/usr/lib/python2.7/untangle_tests/testShell.key testshell@" + radiusServer + " \"pidof mgen >/dev/null 2>&1\"")
    # print "externalClientResult <%s>" % externalClientResult
    # print "isMgenNotRunning <%s>" % isMgenNotRunning
    if (externalClientResult == 0 and isMgenNotRunning):
        # RADIUS server, the mgen endpoint is reachable, check other requirements
        mgenResult = remote_control.runCommand("test -x /usr/bin/mgen")
        # print "mgenResult <%s>" % mgenResult
        if (mgenResult == 0):
            # Always get new UDP traffic generator file in case it changes.
            remote_control.runCommand("rm udp-load-ats.mgn")
            mgnFileResult = remote_control.runCommand("wget -q http://test.untangle.com/test/udp-load-ats.mgn")
            if (mgnFileResult == 0):
                mGenPresent = True
    return mGenPresent

def getUDPSpeed():
    # Use mgen to get UDP speed.  Returns number of packets received.
    # start mgen receiver on radius server.
    os.system("rm -f /tmp/mgen_recv.dat")
    os.system("ssh -o 'StrictHostKeyChecking=no' -i " + system_properties.getPrefix() + "/usr/lib/python2.7/untangle_tests/testShell.key testshell@" + radiusServer + " \"rm -f mgen_recv.dat\" >/dev/null 2>&1")
    os.system("ssh -o 'StrictHostKeyChecking=no' -i " + system_properties.getPrefix() + "/usr/lib/python2.7/untangle_tests/testShell.key testshell@" + radiusServer + " \"/home/fnsadmin/MGEN/mgen output mgen_recv.dat port 5000 >/dev/null 2>&1 &\"")
    # start the UDP generator on the client behind the Untangle.
    remote_control.runCommand("mgen input /home/testshell/udp-load-ats.mgn txlog log mgen_snd.log")
    # wait for UDP to finish
    time.sleep(25)
    # kill mgen receiver    
    os.system("ssh -o 'StrictHostKeyChecking=no' -i " + system_properties.getPrefix() + "/usr/lib/python2.7/untangle_tests/testShell.key testshell@" + radiusServer + " \"pkill mgen\"  >/dev/null 2>&1")
    os.system("scp -o 'StrictHostKeyChecking=no' -i " + system_properties.getPrefix() + "/usr/lib/python2.7/untangle_tests/testShell.key testshell@" + radiusServer + ":mgen_recv.dat /tmp/mgen_recv.dat >/dev/null 2>&1")
    wcResults = subprocess.Popen(["wc","-l","/tmp/mgen_recv.dat"], stdout=subprocess.PIPE).communicate()[0]
    print "wcResults " + str(wcResults)
    numOfPackets = wcResults.split(' ')[0]
    return int(numOfPackets)

def sendUDPPackets(targetIP):
    # Use mgen to send UDP packets.  Returns number of packets received.
    # start mgen receiver on client.
    remote_control.runCommand("rm -f mgen_recv.dat")
    remote_control.runCommand("nohup mgen output mgen_recv.dat port 5000", stdout=False, nowait=True)
    # Create a mgen config with client host IP as the target
    os.system("ssh -o 'StrictHostKeyChecking=no' -i " + system_properties.getPrefix() + "/usr/lib/python2.7/untangle_tests/testShell.key testshell@" + radiusServer + " \"rm -f udp-load-send.mgn\" >/dev/null 2>&1")
    os.system("ssh -o 'StrictHostKeyChecking=no' -i " + system_properties.getPrefix() + "/usr/lib/python2.7/untangle_tests/testShell.key testshell@" + radiusServer + " \"sed 's/" + radiusServer + "/" + targetIP + "/' udp-load-ats.mgn > udp-load-send.mgn\" >/dev/null 2>&1")
    # start the UDP generator on the radius server.
    os.system("ssh -o 'StrictHostKeyChecking=no' -i " + system_properties.getPrefix() + "/usr/lib/python2.7/untangle_tests/testShell.key testshell@" + radiusServer + " \"/home/fnsadmin/MGEN/mgen input /home/testshell/udp-load-send.mgn txlog log mgen_snd.log\" >/dev/null 2>&1")
    # wait for UDP to finish
    time.sleep(25)
    # kill mgen receiver    
    remote_control.runCommand("pkill mgen")
    wcResults = remote_control.runCommand("wc -l mgen_recv.dat", stdout=True)
    print "wcResults " + str(wcResults)
    numOfPackets = wcResults.split(' ')[0]
    return int(numOfPackets)
        
