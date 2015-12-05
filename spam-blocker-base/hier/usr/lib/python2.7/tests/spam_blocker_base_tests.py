import unittest2
import time
import subprocess
import sys
import os
import socket
import smtplib
import re
import system_properties
import global_functions
from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from uvm import Manager
from uvm import Uvm
import remote_control
import ipaddr

uvmContext = Uvm().getUvmContext()
defaultRackId = 1
node = None
nodeData = None
nodeSSL = None
nodeSSLData = None
canRelay = True
smtpServerHost = 'test.untangle.com'
listFakeSmtpServerHosts = [('10.112.56.30','16'),('10.111.56.84','16')]
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
    except smtplib.SMTPException, e:
       print "Error: unable to send email" + str(e)
       return 0

def getLatestMailSender():
    remote_control.runCommand("rm -f mailpkg.tar") # remove all previous mail packages
    results = remote_control.runCommand("wget -q http://test.untangle.com/test/mailpkg.tar")
    # print "Results from getting mailpkg.tar <%s>" % results
    results = remote_control.runCommand("tar -xvf mailpkg.tar")
    # print "Results from untaring mailpkg.tar <%s>" % results

def sendSpamMail(host=smtpServerHost):
    remote_control.runCommand("python mailsender.py --from=test@example.com --to=qa@example.com ./spam-mail/ --host=" + host + " --reconnect --series=30:0,150,100,50,25,0,180")

class SpamBlockerBaseTests(unittest2.TestCase):

    @staticmethod
    def shortName():
        return "untangle"

    @staticmethod
    def nodeNameSpamCase():
        return "untangle-casing-smtp"

    @staticmethod
    def nodeNameSSLInspector():
        return "untangle-casing-ssl-inspector"

    @staticmethod
    def initialSetUp(self):
        global node, nodeData, nodeSP, nodeDataSP, nodeSSL, nodeSSLData, canRelay
        if (uvmContext.nodeManager().isInstantiated(self.nodeName())):
            raise unittest2.SkipTest('node %s already instantiated' % self.nodeName())
        node = uvmContext.nodeManager().instantiate(self.nodeName(), defaultRackId)
        nodeData = node.getSettings()
        nodeSP = uvmContext.nodeManager().node(self.nodeNameSpamCase())
        nodeDataSP = nodeSP.getSmtpNodeSettings()
        if uvmContext.nodeManager().isInstantiated(self.nodeNameSSLInspector()):
            raise Exception('node %s already instantiated' % self.nodeNameSSLInspector())
        nodeSSL = uvmContext.nodeManager().instantiate(self.nodeNameSSLInspector(), defaultRackId)
        # nodeSSL.start() # leave node off. node doesn't auto-start
        nodeSSLData = nodeSSL.getSettings()
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

    def setUp(self):
        pass

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
        test_untangle_IP = socket.gethostbyname("test.untangle.com")

        sendSpamMail()
        
        events = global_functions.get_events(self.displayName(),'Quarantined Events',None,1)
        assert( events != None )
        assert( events.get('list') != None )

        print events['list'][0]
        found = global_functions.check_events( events.get('list'), 10,
                                               's_server_addr', test_untangle_IP,
                                               's_server_port', 25,
                                               'addr', 'qa@example.com',
                                               'c_client_addr', remote_control.clientIP)
        assert( found ) 

    def test_030_adminQuarantine(self):
        if (not canRelay):
            raise unittest2.SkipTest('Unable to relay through test.untangle.com')
        events = global_functions.get_events(self.displayName(),'Quarantined Events',None,1)
        if (events == None):
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
        # find local SMTP sender
        fakeSmtpServerHost = "";
        for smtpServerHostIP in listFakeSmtpServerHosts:
            interfaceNet = smtpServerHostIP[0] + "/" + str(smtpServerHostIP[1])
            if ipaddr.IPAddress(wan_IP) in ipaddr.IPv4Network(interfaceNet):
                fakeSmtpServerHost = smtpServerHostIP[0]
        if (fakeSmtpServerHost == ""):
            raise unittest2.SkipTest("No local SMTP server")
        externalClientResult = subprocess.call(["ping","-c","1",fakeSmtpServerHost],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        if (externalClientResult != 0):
            raise unittest2.SkipTest("Fake SMTP client is unreachable, skipping smtp headers check")
        nodeData['smtpConfig']['blockSuperSpam'] = False
        nodeData['smtpConfig']['scanWanMail'] = True
        nodeData['smtpConfig']['addSpamHeaders'] = True
        nodeData['smtpConfig']['msgAction'] = "MARK"
        node.setSettings(nodeData)
        # remove previous smtp log file
        remote_control.runCommand("sudo pkill -INT python",host=fakeSmtpServerHost)
        remote_control.runCommand("sudo rm -f /tmp/test_070_checkForSMTPHeaders.log /tmp/qa@example.com.*", host=fakeSmtpServerHost)
        # Start mail sink
        remote_control.runCommand("sudo python fakemail.py --host " + fakeSmtpServerHost + " --log /tmp/test_070_checkForSMTPHeaders.log --port 25 --path /tmp/ --background", host=fakeSmtpServerHost, stdout=False, nowait=True)
        sendSpamMail(host=fakeSmtpServerHost)
        # check for email file if there is no timeout
        emailFound = False
        timeout = 60
        while not emailFound and timeout > 0:
            timeout -= 1
            time.sleep(1)
            # Check to see if the delivered email file is present
            email_file = remote_control.runCommand("test -f /tmp/qa@example.com.1",host=fakeSmtpServerHost)
            if (email_file == 0):
                emailFound = True
                
        # Either found email file or timed out so kill mail sink
        remote_control.runCommand("sudo pkill -INT python",host=fakeSmtpServerHost)
        nodeData['smtpConfig']['msgAction'] = "QUARANTINE"
        node.setSettings(nodeData)
        assert (timeout != 0)
        # look for added header in delivered email
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
        wan_IP = uvmContext.networkManager().getFirstWanAddress()
        if not global_functions.isInOfficeNetwork(wan_IP):
            raise unittest2.SkipTest("Not on office network, skipping")
        externalClientResult = subprocess.call(["ping -c 1 " + tlsSmtpServerHost + " >/dev/null 2>&1"],shell=True,stdout=None,stderr=None)            
        if (externalClientResult != 0):
            raise unittest2.SkipTest("TLS SMTP server is unreachable, skipping TLS Allow check")
        # Get latest TLS test command file
        testCopyResult = subprocess.call(["scp -3 -o 'StrictHostKeyChecking=no' -i " + system_properties.getPrefix() + "/usr/lib/python2.7/tests/testShell.key testshell@" + tlsSmtpServerHost + ":/home/testshell/test-tls.py testshell@" + remote_control.clientIP + ":/home/testshell/"],shell=True,stdout=None,stderr=None)
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
    
    def test_090_checkTLSwSSLInspector(self):
        wan_IP = uvmContext.networkManager().getFirstWanAddress()
        if not global_functions.isInOfficeNetwork(wan_IP):
            raise unittest2.SkipTest("Not on office network, skipping")
        externalClientResult = subprocess.call(["ping -c 1 " + tlsSmtpServerHost + " >/dev/null 2>&1"],shell=True,stdout=None,stderr=None)            
        if (externalClientResult != 0):
            raise unittest2.SkipTest("TLS SMTP server is unreachable, skipping TLS Allow check")
        # Get latest TLS test command file
        testCopyResult = subprocess.call(["scp -3 -o 'StrictHostKeyChecking=no' -i " + system_properties.getPrefix() + "/usr/lib/python2.7/tests/testShell.key testshell@" + tlsSmtpServerHost + ":/home/testshell/test-tls.py testshell@" + remote_control.clientIP + ":/home/testshell/"],shell=True,stdout=None,stderr=None)
        assert(testCopyResult == 0)
        nodeData['smtpConfig']['scanWanMail'] = True
        nodeData['smtpConfig']['allowTls'] = False
        node.setSettings(nodeData)
        # Turn on SSL Inspector
        nodeSSL.start()
        # print "TLS 1 : " + str(tlsSMTPResult)
        tlsSMTPResult = remote_control.runCommand("python test-tls.py", stdout=False, nowait=False)
        nodeSSL.stop()
        # print "TLS 2 : " + str(tlsSMTPResult)
        assert(tlsSMTPResult == 0)
            
    @staticmethod
    def finalTearDown(self):
        global node,nodeSSL
        if node != None:
            uvmContext.nodeManager().destroy( node.getNodeSettings()["id"] )
            node = None
        if nodeSSL != None:
            uvmContext.nodeManager().destroy( nodeSSL.getNodeSettings()["id"] )
            nodeSSL = None
