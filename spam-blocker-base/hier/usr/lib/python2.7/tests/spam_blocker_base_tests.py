import unittest2
import time
import subprocess
import sys
import os
import socket
import re
import global_functions
from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from global_functions import uvmContext
from uvm import Manager
from uvm import Uvm
import remote_control
import ipaddr

defaultRackId = 1
node = None
nodeData = None
nodeSSL = None
nodeSSLData = None
canRelay = True
smtpServerHost = global_functions.testServerHost

def getLatestMailSender():
    remote_control.run_command("rm -f mailpkg.tar") # remove all previous mail packages
    results = remote_control.run_command("wget -q http://test.untangle.com/test/mailpkg.tar")
    # print "Results from getting mailpkg.tar <%s>" % results
    results = remote_control.run_command("tar -xvf mailpkg.tar")
    # print "Results from untaring mailpkg.tar <%s>" % results

def sendSpamMail(host=smtpServerHost, useTLS=False):
    mailResult = None
    randomAddress = global_functions.random_email()
    print "randomAddress: " + randomAddress
    if useTLS:
        mailResult = remote_control.run_command("python mailsender.py --from=atstest@test.untangle.com --to=" + randomAddress + " ./spam-mail/ --host=" + host + " --reconnect --series=30:0,150,100,50,25,0,180 --starttls", stdout=False, nowait=False)
    else:
        mailResult = remote_control.run_command("python mailsender.py --from=atstest@test.untangle.com --to=" + randomAddress + " ./spam-mail/ --host=" + host + " --reconnect --series=30:0,150,100,50,25,0,180")
    return mailResult, randomAddress

def createSSLInspectRule(port="25"):
    return {
        "action": {
            "actionType": "INSPECT",
            "flag": False,
            "javaClass": "com.untangle.app.ssl_inspector.SslInspectorRuleAction"
        },
        "conditions": {
            "javaClass": "java.util.LinkedList",
            "list": [
                {
                    "conditionType": "PROTOCOL",
                    "invert": False,
                    "javaClass": "com.untangle.app.ssl_inspector.SslInspectorRuleCondition",
                    "value": "TCP"
                },
                {
                    "conditionType": "DST_PORT",
                    "invert": False,
                    "javaClass": "com.untangle.app.ssl_inspector.SslInspectorRuleCondition",
                    "value": port
                }
            ]
        },
        "description": "Inspect" + port,
        "javaClass": "com.untangle.app.ssl_inspector.SslInspectorRule",
        "live": True,
        "ruleId": 1
    };

class SpamBlockerBaseTests(unittest2.TestCase):

    @staticmethod
    def shortName():
        return "untangle"

    @staticmethod
    def nodeNameSpamCase():
        return "smtp"

    @staticmethod
    def nodeNameSSLInspector():
        return "ssl-inspector"

    @staticmethod
    def initialSetUp(self):
        global node, nodeData, nodeSP, nodeDataSP, nodeSSL, nodeSSLData, canRelay
        if (uvmContext.appManager().isInstantiated(self.nodeName())):
            raise unittest2.SkipTest('node %s already instantiated' % self.nodeName())
        node = uvmContext.appManager().instantiate(self.nodeName(), defaultRackId)
        nodeData = node.getSettings()
        nodeSP = uvmContext.appManager().app(self.nodeNameSpamCase())
        nodeDataSP = nodeSP.getSmtpSettings()
        if uvmContext.appManager().isInstantiated(self.nodeNameSSLInspector()):
            raise Exception('node %s already instantiated' % self.nodeNameSSLInspector())
        nodeSSL = uvmContext.appManager().instantiate(self.nodeNameSSLInspector(), defaultRackId)
        # nodeSSL.start() # leave node off. node doesn't auto-start
        nodeSSLData = nodeSSL.getSettings()
        try:
            canRelay = global_functions.send_test_email(mailhost=smtpServerHost)
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
        result = remote_control.is_online()
        assert (result == 0)

    def test_020_smtpTest(self):
        if (not canRelay):
            raise unittest2.SkipTest('Unable to relay through ' + smtpServerHost)
        nodeData['smtpConfig']['scanWanMail'] = True
        nodeData['smtpConfig']['strength'] = 30
        node.setSettings(nodeData)
        # Get the IP address of test.untangle.com
        test_untangle_IP = socket.gethostbyname(smtpServerHost)

        result, to_address = sendSpamMail()
        
        events = global_functions.get_events(self.displayName(),'Quarantined Events',None,1)
        assert( events != None )
        assert( events.get('list') != None )

        print events['list'][0]
        found = global_functions.check_events( events.get('list'), 10,
                                               's_server_addr', test_untangle_IP,
                                               's_server_port', 25,
                                               'addr', to_address,
                                               'c_client_addr', remote_control.clientIP)
        assert( found ) 

    def test_030_adminQuarantine(self):
        if (not canRelay):
            raise unittest2.SkipTest('Unable to relay through ' + smtpServerHost)
        # send some spam
        nodeData['smtpConfig']['scanWanMail'] = True
        nodeData['smtpConfig']['strength'] = 30
        node.setSettings(nodeData)
        result, to_address = sendSpamMail()
        events = global_functions.get_events(self.displayName(),'Quarantined Events',None,1)
       
        if (events == None):
            raise unittest2.SkipTest('Unable to run admin quarantine since there are no quarantine events')
        
        # Get adminstrative quarantine list of email addresses
        addressFound = False
        curQuarantine = nodeSP.getQuarantineMaintenenceView()
        curQuarantineList = curQuarantine.listInboxes()
        for checkAddress in curQuarantineList['list']:
            print checkAddress
            if (checkAddress['address'] == to_address) and (checkAddress['totalMails'] > 0): addressFound = True
        assert(addressFound)

    def test_040_userQuarantine(self):
        if (not canRelay):
            raise unittest2.SkipTest('Unable to relay through ' + smtpServerHost)
        # send some spam
        nodeData['smtpConfig']['scanWanMail'] = True
        nodeData['smtpConfig']['strength'] = 30
        node.setSettings(nodeData)
        result, to_address = sendSpamMail()
        # Get user quarantine list of email addresses
        addressFound = False
        curQuarantine = nodeSP.getQuarantineUserView()
        curQuarantineList = curQuarantine.getInboxRecords(to_address)
        #print curQuarantineList
        assert(len(curQuarantineList['list']) > 0)

    def test_050_userQuarantinePurge(self):
        if (not canRelay):
            raise unittest2.SkipTest('Unable to relay through ' + smtpServerHost)
        # send some spam
        nodeData['smtpConfig']['scanWanMail'] = True
        nodeData['smtpConfig']['strength'] = 30
        node.setSettings(nodeData)
        result, to_address = sendSpamMail()
        # Get user quarantine list of email addresses
        addressFound = False
        curQuarantine = nodeSP.getQuarantineUserView()

        curQuarantineList = curQuarantine.getInboxRecords(to_address)
        initialLen = len(curQuarantineList['list'])
        mailId = curQuarantineList['list'][0]['mailID'];
        print mailId
        curQuarantine.purge(to_address, [mailId]);

        curQuarantineListAfter = curQuarantine.getInboxRecords(to_address)
        assert(len(curQuarantineListAfter['list']) == initialLen - 1);

    def test_060_adminQuarantineDeleteAccount(self):
        # Get adminstrative quarantine list of email addresses
        if (not canRelay):
            raise unittest2.SkipTest('Unable to relay through ' + smtpServerHost)
        # send some spam
        nodeData['smtpConfig']['scanWanMail'] = True
        nodeData['smtpConfig']['strength'] = 30
        node.setSettings(nodeData)
        result, to_address = sendSpamMail()
        addressFound = False
        curQuarantine = nodeSP.getQuarantineMaintenenceView()
        curQuarantine.deleteInbox(to_address)
        curQuarantineList = curQuarantine.listInboxes()
        for checkAddress in curQuarantineList['list']:
            if (checkAddress['address'] == to_address) and (checkAddress['totalMails'] > 0): addressFound = True
        assert(not addressFound)

    def test_070_checkForSMTPHeaders(self):
        if (not canRelay):
            raise unittest2.SkipTest('Unable to relay through ' + smtpServerHost)

        nodeData['smtpConfig']['blockSuperSpam'] = False
        nodeData['smtpConfig']['scanWanMail'] = True
        nodeData['smtpConfig']['addSpamHeaders'] = True
        nodeData['smtpConfig']['msgAction'] = "MARK"
        node.setSettings(nodeData)

        result, to_address = sendSpamMail()
        # check for email file if there is no timeout
        emailFound = False
        timeout = 5
        emailContext = ""
        while not emailFound and timeout > 0:
            timeout -= 1
            time.sleep(1)
            # Check to see if the delivered email file is present
            emailContext = remote_control.run_command("wget -q --timeout=5 -O - http://test.untangle.com/cgi-bin/getEmail.py?toaddress=" + to_address + " 2>&1" ,stdout=True)
            if (emailContext != ""):
                emailFound = True
                
        # Either found email file or timed out so kill mail sink
        nodeData['smtpConfig']['msgAction'] = "QUARANTINE"
        node.setSettings(nodeData)
        assert (timeout != 0)
        # look for added header in delivered email
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
        if (not canRelay):
            raise unittest2.SkipTest('Unable to relay through ' + smtpServerHost)
        nodeData['smtpConfig']['scanWanMail'] = True
        node.setSettings(nodeData)
        # Make sure SSL Inspector is off
        nodeSSL.stop()
        tlsSMTPResult, to_address = sendSpamMail(useTLS=True)
        # print "TLS 1 : " + str(tlsSMTPResult)
        assert(tlsSMTPResult != 0)
        nodeData['smtpConfig']['allowTls'] = True
        node.setSettings(nodeData)
        tlsSMTPResult, to_address = sendSpamMail(host=global_functions.testServerHost, useTLS=True)
        # print "TLS 2 : " + str(tlsSMTPResult)
        assert(tlsSMTPResult == 0)
        
    
    def test_090_checkTLSwSSLInspector(self):
        if (not canRelay):
            raise unittest2.SkipTest('Unable to relay through ' + smtpServerHost)
        test_untangle_IP = socket.gethostbyname(smtpServerHost)
        nodeData['smtpConfig']['scanWanMail'] = True
        nodeData['smtpConfig']['allowTls'] = False
        nodeData['smtpConfig']['strength'] = 30
        node.setSettings(nodeData)
        # Turn on SSL Inspector
        nodeSSLData['processEncryptedMailTraffic'] = True
        nodeSSLData['ignoreRules']['list'].insert(0,createSSLInspectRule("25"))
        nodeSSL.setSettings(nodeSSLData)
        nodeSSL.start()
        tlsSMTPResult, to_address = sendSpamMail(useTLS=True)
        # print "TLS 090 : " + str(tlsSMTPResult)
        nodeSSL.stop()
        assert(tlsSMTPResult == 0)
        events = global_functions.get_events(self.displayName(),'Quarantined Events',None,1)
        assert( events != None )
        assert( events.get('list') != None )

        print events['list'][0]
        found = global_functions.check_events( events.get('list'), 5,
                                               's_server_addr', test_untangle_IP,
                                               's_server_port', 25,
                                               'addr', to_address,
                                               'c_client_addr', remote_control.clientIP)
        assert( found ) 
            
    @staticmethod
    def finalTearDown(self):
        global node,nodeSSL
        if node != None:
            uvmContext.appManager().destroy( node.getAppSettings()["id"] )
            node = None
        if nodeSSL != None:
            uvmContext.appManager().destroy( nodeSSL.getAppSettings()["id"] )
            nodeSSL = None
