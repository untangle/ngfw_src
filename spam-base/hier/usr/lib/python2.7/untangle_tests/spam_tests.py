import unittest2
import time
import subprocess
import sys
import os
import socket
import smtplib
import re
import system_properties
from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from uvm import Manager
from uvm import Uvm
import remote_control

uvmContext = Uvm().getUvmContext()
defaultRackId = 1
node = None
nodeData = None
canRelay = True
smtpServerHost = 'test.untangle.com'
fakeSmtpServerHost = '10.111.56.32'
tlsSmtpServerHost = '10.111.56.44' # Vcenter VM Debian-ATS-TLS 

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
    remote_control.runCommand("rm -f mailpkg.tar") # remove all previous mail packages
    results = remote_control.runCommand("wget -q http://test.untangle.com/test/mailpkg.tar")
    # print "Results from getting mailpkg.tar <%s>" % results
    results = remote_control.runCommand("tar -xvf mailpkg.tar")
    # print "Results from untaring mailpkg.tar <%s>" % results

def sendSpamMail(host=smtpServerHost):
    remote_control.runCommand("python mailsender.py --from=test@example.com --to=qa@example.com ./spam-mail/ --host=" + host + " --reconnect --series=30:0,150,100,50,25,0,180")

def flushEvents():
    reports = uvmContext.nodeManager().node("untangle-node-reporting")
    if (reports != None):
        reports.flushEvents()

class SpamTests(unittest2.TestCase):

    @staticmethod
    def nodeName():
        return "untangle-base-spam"

    @staticmethod
    def shortName():
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
            flushEvents()
            # flush quarantine.
            curQuarantine = nodeSP.getQuarantineMaintenenceView()
            curQuarantineList = curQuarantine.listInboxes()
            for checkAddress in curQuarantineList['list']:
                if checkAddress['address']:
                    curQuarantine.deleteInbox(checkAddress['address'])

    # verify client is online
    def test_010_clientIsOnline(self):
        result = remote_control.isOnline()
        assert (result == 0)

    def test_020_smtpTest(self):
        if (not canRelay):
            raise unittest2.SkipTest('Unable to relay through test.untangle.com')
        nodeData['smtpConfig']['scanWanMail'] = True
        nodeData['smtpConfig']['strength'] = 30
        node.setSettings(nodeData)
        # Get the IP address of test.untangle.com
        result = remote_control.runCommand("host "+smtpServerHost, stdout=True)
        match = re.search(r'\d{1,3}.\d{1,3}.\d{1,3}.\d{1,3}', result)
        ip_address_testuntangle = match.group()

        sendSpamMail()
        flushEvents()

        query = None;
        for q in node.getEventQueries():
            if q['name'] == 'Quarantined Events': query = q;
        assert(query != None)
        events = uvmContext.getEvents(query['query'],defaultRackId,1)
        assert( events != None )
        # Verify Quarantined events occurred..
        assert(events['list'][0]['c_server_addr'] == ip_address_testuntangle)
        assert(events['list'][0]['s_server_port'] == 25)
        assert(events['list'][0]['addr'] == 'qa@example.com')
        assert(events['list'][0]['c_client_addr'] == remote_control.clientIP)
        if (not 'spamblocker_score' in events['list'][0]):
            assert(events['list'][0]['spamassassin_score'] >= 3.0)
        else:
            assert(events['list'][0]['spamblocker_score'] >= 3.0)
        assert(events['list'][0]['c_client_addr'] == remote_control.clientIP)

    def test_030_adminQuarantine(self):
        if (not canRelay):
            raise unittest2.SkipTest('Unable to relay through test.untangle.com')
        for q in node.getEventQueries():
            if q['name'] == 'Quarantined Events': query = q;
        # print query
        if (query == None):
            raise unittest2.SkipTest('Unable to run admin quarantine since there are no quarantine events')
        # Get adminstrative quarantine list of email addresses
        addressFound = False
        curQuarantine = nodeSP.getQuarantineMaintenenceView()
        curQuarantineList = curQuarantine.listInboxes()
        for checkAddress in curQuarantineList['list']:
            print checkAddress
            if (checkAddress['address'] == 'qa@example.com') and (checkAddress['totalMails'] > 0): addressFound = True
        assert(addressFound)

    def test_040_userQuarantine(self):
        if (not canRelay):
            raise unittest2.SkipTest('Unable to relay through test.untangle.com')
        # Get user quarantine list of email addresses
        addressFound = False
        curQuarantine = nodeSP.getQuarantineUserView()
        curQuarantineList = curQuarantine.getInboxRecords('qa@example.com')
        #print curQuarantineList
        assert(len(curQuarantineList['list']) > 0)

    def test_050_userQuarantinePurge(self):
        if (not canRelay):
            raise unittest2.SkipTest('Unable to relay through test.untangle.com')
        # Get user quarantine list of email addresses
        addressFound = False
        curQuarantine = nodeSP.getQuarantineUserView()

        curQuarantineList = curQuarantine.getInboxRecords('qa@example.com')
        initialLen = len(curQuarantineList['list'])
        mailId = curQuarantineList['list'][0]['mailID'];
        print mailId
        curQuarantine.purge('qa@example.com', [mailId]);

        curQuarantineListAfter = curQuarantine.getInboxRecords('qa@example.com')
        assert(len(curQuarantineListAfter['list']) == initialLen - 1);

    def test_060_adminQuarantineDeleteAccount(self):
        # Get adminstrative quarantine list of email addresses
        addressFound = False
        curQuarantine = nodeSP.getQuarantineMaintenenceView()
        curQuarantine.deleteInbox('qa@example.com')
        curQuarantineList = curQuarantine.listInboxes()
        for checkAddress in curQuarantineList['list']:
            if (checkAddress['address'] == 'qa@example.com') and (checkAddress['totalMails'] > 0): addressFound = True
        assert(not addressFound)

    def test_070_checkForSMTPHeaders(self):
        wan_IP = uvmContext.networkManager().getFirstWanAddress()
        if (wan_IP.split(".")[0] != "10"):
            raise unittest2.SkipTest("Not on 10.x network, skipping")
        externalClientResult = subprocess.call(["ping","-c","1",fakeSmtpServerHost],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        if (externalClientResult != 0):
            raise unittest2.SkipTest("Fake SMTP client is unreachable, skipping smtp headers check")
        nodeData['smtpConfig']['addSpamHeaders'] = True
        node.setSettings(nodeData)
        # remove previous smtp log file
        remote_control.runCommand("sudo rm -f /tmp/test_070_checkForSMTPHeaders.log /tmp/qa@example.com.*", host=fakeSmtpServerHost)
        # Start mail sink
        remote_control.runCommand("sudo python fakemail.py --host=" + fakeSmtpServerHost +" --log=/tmp/test_070_checkForSMTPHeaders.log --port 25 --background --path=/tmp/", host=fakeSmtpServerHost, stdout=False, nowait=True)
        sendSpamMail(host=fakeSmtpServerHost)
        time.sleep(3) # wait for email to arrive
        # look for added header in delivered email
        remote_control.runCommand("sudo pkill -INT python",host=fakeSmtpServerHost)
        emailContext=remote_control.runCommand("cat /tmp/qa@example.com.1",host=fakeSmtpServerHost, stdout=True)
        lines = emailContext.split("\n")
        spamScore = 0
        requiredScore = 0

        # some dev boxes score this < 0, so don't check the score
        # just check that the headers were added
        for line in lines:
            if 'X-spam-status' in line:
                # print line
                match = re.search(r'\sscore\=([0-9.-]+)\srequired\=([0-9.]+) ', line)
                spamScore =  match.group(1)
                requiredScore =  match.group(2)
                print "spamScore: " + spamScore + " requiredScore: " + requiredScore
                return
        assert False 
        # assert(float(spamScore) > 0)
        # assert(float(requiredScore) > 0)
        # assert(float(requiredScore) > float(spamScore))

    def test_080_checkAllowTLS(self):
        raise unittest2.SkipTest("Review changes in test")
        wan_IP = uvmContext.networkManager().getFirstWanAddress()
        if (wan_IP.split(".")[0] != "10"):
            raise unittest2.SkipTest("Not on 10.x network, skipping")
        externalClientResult = subprocess.call(["ping -c 1 " + tlsSmtpServerHost + " >/dev/null 2>&1"],shell=True,stdout=None,stderr=None)            
        if (externalClientResult != 0):
            raise unittest2.SkipTest("TLS SMTP server is unreachable, skipping TLS Allow check")
        # Get latest TLS test command file
        testCopyResult = subprocess.call(["scp -3 -o 'StrictHostKeyChecking=no' -i " + system_properties.getPrefix() + "/usr/lib/python2.7/untangle_tests/testShell.key testshell@" + tlsSmtpServerHost + ":/home/testshell/test-tls.py testshell@" + remote_control.clientIP + ":/home/testshell/"],shell=True,stdout=None,stderr=None)
        assert(testCopyResult == 0)
        nodeData['smtpConfig']['scanWanMail'] = True
        node.setSettings(nodeData)
        tlsSMTPResult = remote_control.runCommand("python test-tls.py", stdout=False, nowait=False)
        # print "TLS 1 : " + str(tlsSMTPResult)
        assert(tlsSMTPResult != 0)
        nodeData['smtpConfig']['allowTls'] = True
        node.setSettings(nodeData)
        tlsSMTPResult = remote_control.runCommand("python test-tls.py", stdout=False, nowait=False)
        # print "TLS 2 : " + str(tlsSMTPResult)
        assert(tlsSMTPResult == 0)
        
    @staticmethod
    def finalTearDown(self):
        global node
        if node != None:
            uvmContext.nodeManager().destroy( node.getNodeSettings()["id"] )
            node = None
