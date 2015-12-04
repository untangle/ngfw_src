import unittest2
import time
import subprocess
import sys
import os
import subprocess
import socket
import smtplib
import re
import system_properties

from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from uvm import Manager
from uvm import Uvm
import remote_control
import test_registry
import global_functions

uvmContext = Uvm().getUvmContext()
defaultRackId = 1
node = None
nodeData = None
canRelay = True
smtpServerHost = 'test.untangle.com'
tlsSmtpServerHost = '10.112.56.44' # Vcenter VM Debian-ATS-TLS 

def sendTestmessage():
    sender = 'test@example.com'
    receivers = ['qa@example.com']
    
    message = """From: Test <test@example.com>
    To: Test Group <qa@example.com>
    Subject: SMTP e-mail test
    
    This is a test e-mail message.
    """
    
    try:
       smtpObj = smtplib.SMTP(smtpServerHost)
       smtpObj.sendmail(sender, receivers, message)         
       print "Successfully sent email"
       return 1
    except smtplib.SMTPException:
       print "Error: unable to send email"
       return 0

def getLatestMailSender():
    remote_control.runCommand("rm -f mailpkg.tar*") # remove all previous mail packages
    results = remote_control.runCommand("wget -q -t 1 --timeout=3 http://test.untangle.com/test/mailpkg.tar")
    # print "Results from getting mailpkg.tar <%s>" % results
    results = remote_control.runCommand("tar -xvf mailpkg.tar")
    # print "Results from untaring mailpkg.tar <%s>" % results

def sendPhishMail(mailfrom="test"):
    results = remote_control.runCommand("python mailsender.py --from=" + mailfrom + "@example.com --to=\"qa@example.com\" ./phish-mail/ --host="+smtpServerHost+" --reconnect --series=30:0,150,100,50,25,0,180")

class PhishBlockerTests(unittest2.TestCase):

    @staticmethod
    def nodeName():
        return "untangle-node-phish-blocker"

    @staticmethod
    def vendorName():
        return "untangle"

    @staticmethod
    def nodeNameSpamCase():
        return "untangle-casing-smtp"

    def setUp(self):
        global node, nodeData, nodeSP, nodeDataSP, canRelay
        if node == None:
            if (uvmContext.nodeManager().isInstantiated(self.nodeName())):
                print "ERROR: Node %s already installed" % self.nodeName();
                raise unittest2.SkipTest('node %s already instantiated' % self.nodeName())
            node = uvmContext.nodeManager().instantiate(self.nodeName(), defaultRackId)
            nodeData = node.getSettings()
            nodeSP = uvmContext.nodeManager().node(self.nodeNameSpamCase())
            nodeDataSP = nodeSP.getSmtpNodeSettings()
            try:
                canRelay = sendTestmessage()
            except Exception,e:
                canRelay = False
            getLatestMailSender()

            # flush quarantine.
            curQuarantine = nodeSP.getQuarantineMaintenenceView()
            curQuarantineList = curQuarantine.listInboxes()
            for checkAddress in curQuarantineList['list']:
                if checkAddress['address']:
                    curQuarantine.deleteInbox(checkAddress['address'])
            
    # verify daemon is running
    def test_009_clamdIsRunning(self):
        # wait for freshclam to finish updating sigs
        freshClamResult = os.system("freshclam >/dev/null 2>&1")
        # wait for clam to get ready - trying to fix occasional failure of later tests
        time.sleep(5)
        result = os.system("pidof clamd >/dev/null 2>&1")
        assert (result == 0)

    # verify client is online
    def test_010_clientIsOnline(self):
        result = remote_control.isOnline()
        assert (result == 0)

    def test_020_smtpQuarantinedPhishBlockerTest(self):
        if (not canRelay):
            raise unittest2.SkipTest('Unable to relay through test.untangle.com')
        nodeData['smtpConfig']['scanWanMail'] = True
        nodeData['smtpConfig']['strength'] = 5
        node.setSettings(nodeData)
        # Get the IP address of test.untangle.com
        result = remote_control.runCommand("host "+smtpServerHost, stdout=True)
        match = re.search(r'\d{1,3}.\d{1,3}.\d{1,3}.\d{1,3}', result)
        ip_address_testuntangle = match.group()

        # sometimes the load is very high >7 and sending mail will fail
        # sleep for a while for the load to go down
        try:
            if float(file("/proc/loadavg","r").readline().split(" ")[0]) > 3:
                time.sleep(30)
        except:
            pass
                
        sendPhishMail("test020")
        sendPhishMail("test021")
        sendPhishMail("test022")

        events = global_functions.get_events('Phish Blocker','All Phish Events',None,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            'c_server_addr', ip_address_testuntangle,
                                            's_server_port', 25,
                                            'addr', 'qa@example.com',
                                            'c_client_addr', remote_control.clientIP,
                                            'phish_blocker_action', 'Q')
        assert( found )
            
    def test_030_smtpMarkPhishBlockerTest(self):
        if (not canRelay):
            raise unittest2.SkipTest('Unable to relay through test.untangle.com')
        nodeData['smtpConfig']['scanWanMail'] = True
        nodeData['smtpConfig']['strength'] = 5
        nodeData['smtpConfig']['msgAction'] = "MARK"
        node.setSettings(nodeData)
        # Get the IP address of test.untangle.com
        result = remote_control.runCommand("host "+smtpServerHost, stdout=True)
        match = re.search(r'\d{1,3}.\d{1,3}.\d{1,3}.\d{1,3}', result)
        ip_address_testuntangle = match.group()

        sendPhishMail("test030")

        events = global_functions.get_events('Phish Blocker','All Phish Events',None,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            'c_server_addr', ip_address_testuntangle,
                                            's_server_port', 25,
                                            'addr', 'qa@example.com',
                                            'c_client_addr', remote_control.clientIP,
                                            'phish_blocker_action', 'M')
        assert( found )

    def test_040_smtpDropPhishBlockerTest(self):
        if (not canRelay):
            raise unittest2.SkipTest('Unable to relay through test.untangle.com')
        nodeData['smtpConfig']['scanWanMail'] = True
        nodeData['smtpConfig']['strength'] = 5
        nodeData['smtpConfig']['msgAction'] = "DROP"
        node.setSettings(nodeData)
        # Get the IP address of test.untangle.com
        result = remote_control.runCommand("host "+smtpServerHost, stdout=True)
        match = re.search(r'\d{1,3}.\d{1,3}.\d{1,3}.\d{1,3}', result)
        ip_address_testuntangle = match.group()

        sendPhishMail("test040")

        events = global_functions.get_events('Phish Blocker','All Phish Events',None,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            'c_server_addr', ip_address_testuntangle,
                                            's_server_port', 25,
                                            'addr', 'qa@example.com',
                                            'c_client_addr', remote_control.clientIP,
                                            'phish_blocker_action', 'D')
        assert( found )

    def test_050_checkTLSBypass(self):
        wan_IP = uvmContext.networkManager().getFirstWanAddress()
        if not global_functions.isInOfficeNetwork(wan_IP):
            raise unittest2.SkipTest("Not on office network, skipping")
        externalClientResult = subprocess.call(["ping -c 1 " + tlsSmtpServerHost + " >/dev/null 2>&1"],shell=True,stdout=None,stderr=None)            
        if (externalClientResult != 0):
            raise unittest2.SkipTest("TLS SMTP server is unreachable, skipping TLS Allow check")
        # Get latest TLS test command file
        testCopyResult = subprocess.call(["scp -3 -o 'StrictHostKeyChecking=no' -i " + system_properties.getPrefix() + "/usr/lib/python2.7/tests/testShell.key testshell@" + tlsSmtpServerHost + ":/home/testshell/test-tls.py testshell@" + remote_control.clientIP + ":/home/testshell/"],shell=True,stdout=None,stderr=None)
        assert(testCopyResult == 0)
        tlsSMTPResult = remote_control.runCommand("python test-tls.py", stdout=False, nowait=False)
        # print "TLS  : " + str(tlsSMTPResult)
        assert(tlsSMTPResult == 0)
       
    @staticmethod
    def finalTearDown(self):
        global node
        if node != None:
            uvmContext.nodeManager().destroy( node.getNodeSettings()["id"] )
            node = None

test_registry.registerNode("phish-blocker", PhishBlockerTests)
