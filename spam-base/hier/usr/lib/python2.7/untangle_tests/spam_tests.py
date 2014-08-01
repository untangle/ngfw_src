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
from untangle_tests import ClientControl

uvmContext = Uvm().getUvmContext()
defaultRackId = 1
clientControl = ClientControl()
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
    clientControl.runCommand("rm -f mailpkg.tar") # remove all previous mail packages
    results = clientControl.runCommand("wget -o /dev/null http://test.untangle.com/test/mailpkg.tar")
    # print "Results from getting mailpkg.tar <%s>" % results
    results = clientControl.runCommand("tar -xvf mailpkg.tar >/dev/null 2>&1")
    # print "Results from untaring mailpkg.tar <%s>" % results

def sendSpamMail():
    results = clientControl.runCommand("python mailsender.py --from=test@example.com --to=qa@example.com ./spam-mail/ --host="+smtpServerHost+" --reconnect --series=30:0,150,100,50,25,0,180 >/dev/null 2>&1")

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
        result = clientControl.isOnline()
        assert (result == 0)

    def test_020_smtpTest(self):
        if (not canRelay):
            raise unittest2.SkipTest('Unable to relay through test.untangle.com')
        nodeData['smtpConfig']['scanWanMail'] = True
        nodeData['smtpConfig']['strength'] = 30
        node.setSettings(nodeData)
        # Get the IP address of test.untangle.com
        result = clientControl.runCommand("host "+smtpServerHost, True)
        match = re.search(r'\d{1,3}.\d{1,3}.\d{1,3}.\d{1,3}', result)
        ip_address_testuntangle = match.group()

        sendSpamMail()
        query = None;
        for q in node.getEventQueries():
            if q['name'] == 'Quarantined Events': query = q;
        assert(query != None)
        events = uvmContext.getEvents(query['query'],defaultRackId,1)
        # print events['list'][0]
        # Verify Quarantined events occurred..
        assert(events['list'][0]['c_server_addr'] == ip_address_testuntangle)
        assert(events['list'][0]['s_server_port'] == 25)
        assert(events['list'][0]['addr'] == 'qa@example.com')
        assert(events['list'][0]['c_client_addr'] == ClientControl.hostIP)
        if (not 'spamblocker_score' in events['list'][0]):
            assert(events['list'][0]['spamassassin_score'] >= 3.0)
        else:
            assert(events['list'][0]['spamblocker_score'] >= 3.0)
        assert(events['list'][0]['c_client_addr'] == ClientControl.hostIP)

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
        for q in node.getEventQueries():
            if q['name'] == 'Quarantined Events': query = q;
        # print query
        if (query == None):
            raise unittest2.SkipTest('Unable to run user quarantine since there are no quarantine events')
        # Get user quarantine list of email addresses
        addressFound = False
        curQuarantine = nodeSP.getQuarantineUserView()
        curQuarantineList = curQuarantine.getInboxRecords('qa@example.com')
        #print curQuarantineList
        assert(len(curQuarantineList['list']) > 0)

    def test_050_userQuarantinePurge(self):
        if (not canRelay):
            raise unittest2.SkipTest('Unable to relay through test.untangle.com')
        for q in node.getEventQueries():
            if q['name'] == 'Quarantined Events': query = q;
        # print query
        if (query == None):
            raise unittest2.SkipTest('Unable to run user quarantine since there are no quarantine events')
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
        for q in node.getEventQueries():
            if q['name'] == 'Quarantined Events': query = q;
        # print query
        if (query == None):
            raise unittest2.SkipTest('Unable to run admin quarantine since there are no quarantine events')
        # Get adminstrative quarantine list of email addresses
        addressFound = False
        curQuarantine = nodeSP.getQuarantineMaintenenceView()
        curQuarantine.deleteInbox('qa@example.com')
        curQuarantineList = curQuarantine.listInboxes()
        for checkAddress in curQuarantineList['list']:
            if (checkAddress['address'] == 'qa@example.com') and (checkAddress['totalMails'] > 0): addressFound = True
        assert(not addressFound)

    @staticmethod
    def finalTearDown(self):
        global node
        if node != None:
            uvmContext.nodeManager().destroy( node.getNodeSettings()["id"] )
            node = None
