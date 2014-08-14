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
from uvm import Manager
from uvm import Uvm
import remote_control
import test_registry

uvmContext = Uvm().getUvmContext()
defaultRackId = 1
node = None
nodeData = None
canRelay = True
smtpServerHost = 'test.untangle.com'

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

def sendPhishMail():
    results = remote_control.runCommand("python mailsender.py --from=test@example.com --to=\"qa@example.com\" ./phish-mail/ --host="+smtpServerHost+" --reconnect --series=30:0,150,100,50,25,0,180")

def flushEvents():
    reports = uvmContext.nodeManager().node("untangle-node-reporting")
    if (reports != None):
        reports.flushEvents()

class PhishTests(unittest2.TestCase):

    @staticmethod
    def nodeName():
        return "untangle-node-phish"

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

    def test_020_smtpQuarantinedPhishTest(self):
        if (not canRelay):
            raise unittest2.SkipTest('Unable to relay through test.untangle.com')
        nodeData['smtpConfig']['scanWanMail'] = True
        nodeData['smtpConfig']['strength'] = 5
        node.setSettings(nodeData)
        # Get the IP address of test.untangle.com
        result = remote_control.runCommand("host "+smtpServerHost, True)
        match = re.search(r'\d{1,3}.\d{1,3}.\d{1,3}.\d{1,3}', result)
        ip_address_testuntangle = match.group()

        sendPhishMail()
        flushEvents()
        query = None;
        for q in node.getEventQueries():
            if q['name'] == 'All Phish Events': query = q                
        assert(query != None)
        events = uvmContext.getEvents(query['query'],defaultRackId,1)
        assert(events != None)
        found = remote_control.check_events( events.get('list'), 5,
                                            'c_server_addr', ip_address_testuntangle,
                                            's_server_port', 25,
                                            'addr', 'qa@example.com',
                                            'c_client_addr', remote_control.clientIP,
                                            'phish_action', 'Q')
        assert( found )
            
    def test_030_smtpMarkPhishTest(self):
        if (not canRelay):
            raise unittest2.SkipTest('Unable to relay through test.untangle.com')
        nodeData['smtpConfig']['scanWanMail'] = True
        nodeData['smtpConfig']['strength'] = 5
        nodeData['smtpConfig']['msgAction'] = "MARK"
        node.setSettings(nodeData)
        # Get the IP address of test.untangle.com
        result = remote_control.runCommand("host "+smtpServerHost, True)
        match = re.search(r'\d{1,3}.\d{1,3}.\d{1,3}.\d{1,3}', result)
        ip_address_testuntangle = match.group()

        sendPhishMail()
        flushEvents()
        query = None;
        for q in node.getEventQueries():
            if q['name'] == 'All Phish Events': query = q                
        assert(query != None)
        events = uvmContext.getEvents(query['query'],defaultRackId,1)
        assert(events != None)
        found = remote_control.check_events( events.get('list'), 5,
                                            'c_server_addr', ip_address_testuntangle,
                                            's_server_port', 25,
                                            'addr', 'qa@example.com',
                                            'c_client_addr', remote_control.clientIP,
                                            'phish_action', 'M')
        assert( found )

    def test_040_smtpDropPhishTest(self):
        if (not canRelay):
            raise unittest2.SkipTest('Unable to relay through test.untangle.com')
        nodeData['smtpConfig']['scanWanMail'] = True
        nodeData['smtpConfig']['strength'] = 5
        nodeData['smtpConfig']['msgAction'] = "DROP"
        node.setSettings(nodeData)
        # Get the IP address of test.untangle.com
        result = remote_control.runCommand("host "+smtpServerHost, True)
        match = re.search(r'\d{1,3}.\d{1,3}.\d{1,3}.\d{1,3}', result)
        ip_address_testuntangle = match.group()

        sendPhishMail()
        flushEvents()
        query = None;
        for q in node.getEventQueries():
            if q['name'] == 'All Phish Events': query = q                
        assert(query != None)
        events = uvmContext.getEvents(query['query'],defaultRackId,1)
        assert(events != None)
        found = remote_control.check_events( events.get('list'), 5,
                                            'c_server_addr', ip_address_testuntangle,
                                            's_server_port', 25,
                                            'addr', 'qa@example.com',
                                            'c_client_addr', remote_control.clientIP,
                                            'phish_action', 'D')
        assert( found )
        
    @staticmethod
    def finalTearDown(self):
        global node
        if node != None:
            uvmContext.nodeManager().destroy( node.getNodeSettings()["id"] )
            node = None

test_registry.registerNode("phish", PhishTests)
