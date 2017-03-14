import unittest2
import time
import subprocess
import sys
import os
import subprocess
import socket
import smtplib
import re

from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from global_functions import uvmContext
from uvm import Manager
from uvm import Uvm
import remote_control
import test_registry
import global_functions

defaultRackId = 1
node = None
nodeData = None
nodeSSL = None
canRelay = True
smtpServerHost = global_functions.testServerHost

def getLatestMailSender():
    remote_control.run_command("rm -f mailpkg.tar*") # remove all previous mail packages
    results = remote_control.run_command("wget -q -t 1 --timeout=3 http://test.untangle.com/test/mailpkg.tar")
    # print "Results from getting mailpkg.tar <%s>" % results
    results = remote_control.run_command("tar -xvf mailpkg.tar")
    # print "Results from untaring mailpkg.tar <%s>" % results

def sendPhishMail(mailfrom="test", host=smtpServerHost, useTLS=False):
    mailResult = None
    if useTLS:
        mailResult = remote_control.run_command("python mailsender.py --from=" + mailfrom + "@test.untangle.com --to=qa@test.untangle.com ./phish-mail/ --host=" + host + " --reconnect --series=30:0,150,100,50,25,0,180 --starttls", stdout=False, nowait=False)
    else:
        mailResult = remote_control.run_command("python mailsender.py --from=" + mailfrom + "@test.untangle.com --to=qa@test.untangle.com ./phish-mail/ --host=" + host + " --reconnect --series=30:0,150,100,50,25,0,180")
    return mailResult

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

    @staticmethod
    def nodeNameSSLInspector():
        return "untangle-casing-ssl-inspector"

    @staticmethod
    def initialSetUp(self):
        global node, nodeData, nodeSP, nodeDataSP, nodeSSL, canRelay
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
        try:
            canRelay = global_functions.send_test_email(mailhost=smtpServerHost)
        except Exception,e:
            canRelay = False
        getLatestMailSender()
        
    def setUp(self):
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
        timeout = 180
        result = 1
        while (result and timeout > 0):
            time.sleep(5)
            timeout -= 5
            result = os.system("pidof clamd >/dev/null 2>&1")
        assert (result == 0)

    # verify client is online
    def test_010_clientIsOnline(self):
        result = remote_control.is_online()
        assert (result == 0)

    def test_020_smtpQuarantinedPhishBlockerTest(self):
        if (not canRelay):
            raise unittest2.SkipTest('Unable to relay through' + smtpServerHost)
        pre_events_quarantine = global_functions.get_app_metric_value(node,"quarantine")

        nodeData['smtpConfig']['scanWanMail'] = True
        nodeData['smtpConfig']['strength'] = 5
        node.setSettings(nodeData)
        # Get the IP address of test.untangle.com
        result = remote_control.run_command("host "+smtpServerHost, stdout=True)
        match = re.search(r'\d{1,3}.\d{1,3}.\d{1,3}.\d{1,3}', result)
        ip_address_testuntangle = match.group()

        # sometimes the load is very high >7 and sending mail will fail
        # sleep for a while for the load to go down
        try:
            if float(file("/proc/loadavg","r").readline().split(" ")[0]) > 3:
                time.sleep(30)
        except:
            pass
                
        timeout = 12
        found = False
        email_index = 20;
        while (not found and timeout > 0):
            time.sleep(3)
            email_index += 1
            from_address = "test0" + str(email_index)
            sendPhishMail(mailfrom=from_address)

            events = global_functions.get_events('Phish Blocker','All Phish Events',None,1)
            assert(events != None)
            # print events['list'][0]
            found = global_functions.check_events( events.get('list'), 5,
                                                'c_server_addr', ip_address_testuntangle,
                                                's_server_port', 25,
                                                'addr', 'qa@test.untangle.com',
                                                'c_client_addr', remote_control.clientIP,
                                                'phish_blocker_action', 'Q')
            timeout -= 1
            
        assert( found )
            
        # Check to see if the faceplate counters have incremented. 
        post_events_quarantine = global_functions.get_app_metric_value(node,"quarantine")
        assert(pre_events_quarantine < post_events_quarantine)
        
    def test_030_smtpMarkPhishBlockerTest(self):
        if (not canRelay):
            raise unittest2.SkipTest('Unable to relay through' + smtpServerHost)
        nodeData['smtpConfig']['scanWanMail'] = True
        nodeData['smtpConfig']['strength'] = 5
        nodeData['smtpConfig']['msgAction'] = "MARK"
        node.setSettings(nodeData)
        # Get the IP address of test.untangle.com
        ip_address_testuntangle = socket.gethostbyname(smtpServerHost)

        timeout = 12
        found = False
        email_index = 20;
        while (not found and timeout > 0):
            time.sleep(3)
            email_index += 1
            from_address = "test0" + str(email_index)
            sendPhishMail(mailfrom=from_address)

            events = global_functions.get_events('Phish Blocker','All Phish Events',None,1)
            assert(events != None)
            found = global_functions.check_events( events.get('list'), 5,
                                                'c_server_addr', ip_address_testuntangle,
                                                's_server_port', 25,
                                                'addr', 'qa@test.untangle.com',
                                                'c_client_addr', remote_control.clientIP,
                                                'phish_blocker_action', 'M')
            timeout -= 1

        assert( found )

    def test_040_smtpDropPhishBlockerTest(self):
        if (not canRelay):
            raise unittest2.SkipTest('Unable to relay through' + smtpServerHost)
        nodeData['smtpConfig']['scanWanMail'] = True
        nodeData['smtpConfig']['strength'] = 5
        nodeData['smtpConfig']['msgAction'] = "DROP"
        node.setSettings(nodeData)
        # Get the IP address of test.untangle.com
        ip_address_testuntangle = socket.gethostbyname(smtpServerHost)
        sendPhishMail(mailfrom="test040")

        events = global_functions.get_events('Phish Blocker','All Phish Events',None,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            'c_server_addr', ip_address_testuntangle,
                                            's_server_port', 25,
                                            'addr', 'qa@test.untangle.com',
                                            'c_client_addr', remote_control.clientIP,
                                            'phish_blocker_action', 'D')
        assert( found )

    def test_050_checkTLSBypass(self):
        if (not canRelay):
            raise unittest2.SkipTest('Unable to relay through' + smtpServerHost)
        tlsSMTPResult = sendPhishMail(host=smtpServerHost, useTLS=True)
        # print "TLS  : " + str(tlsSMTPResult)
        assert(tlsSMTPResult == 0)
       
    def test_060_checkTLSwSSLInspector(self):
        global nodeSSL
        if (not canRelay):
            raise unittest2.SkipTest('Unable to relay through' + smtpServerHost)
        ip_address_testuntangle = socket.gethostbyname(smtpServerHost)
        nodeSSL.start()
        tlsSMTPResult = sendPhishMail(mailfrom="test060", host=smtpServerHost, useTLS=True)
        # print "TLS  : " + str(tlsSMTPResult)
        nodeSSL.stop()
        assert(tlsSMTPResult == 0)

        events = global_functions.get_events('Phish Blocker','All Phish Events',None,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            'c_server_addr', ip_address_testuntangle,
                                            's_server_port', 25,
                                            'addr', 'qa@test.untangle.com',
                                            'c_client_addr', remote_control.clientIP,
                                            'phish_blocker_action', 'D')
    
    @staticmethod
    def finalTearDown(self):
        global node, nodeSSL
        if node != None:
            uvmContext.nodeManager().destroy( node.getNodeSettings()["id"] )
            node = None
        if nodeSSL != None:
            uvmContext.nodeManager().destroy( nodeSSL.getNodeSettings()["id"] )
            nodeSSL = None

test_registry.registerNode("phish-blocker", PhishBlockerTests)
