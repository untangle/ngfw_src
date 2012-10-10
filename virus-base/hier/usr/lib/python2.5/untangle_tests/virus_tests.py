import unittest
import time
from datetime import datetime
import sys
import os
from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from uvm import Manager
from uvm import Uvm
from untangle_tests import ClientControl

uvmContext = Uvm().getUvmContext()
defaultRackId = 1
clientControl = ClientControl()
node = None

def flushEvents():
    reports = uvmContext.nodeManager().node("untangle-node-reporting")
    if (reports != None):
        reports.flushEvents()

class VirusTests(unittest.TestCase):

    @staticmethod
    def nodeName():
        return "untangle-base-virus"

    @staticmethod
    def vendorName():
        return "untangle"

    def setUp(self):
        global node
        if node == None:
            # download eicar before installing virus blocker
            result = clientControl.runCommand("wget http://test.untangle.com/virus/00_eicar.com -O /tmp/eicar -o /dev/null 2>&1")
            assert (result == 0)

            if (uvmContext.nodeManager().isInstantiated(self.nodeName())):
                print "ERROR: Node %s already installed" % self.nodeName();
                raise Exception('node %s already instantiated' % self.nodeName())
            node = uvmContext.nodeManager().instantiateAndStart(self.nodeName(), defaultRackId)
            flushEvents()

    # verify client is online
    def test_010_clientIsOnline(self):
        result = clientControl.runCommand("wget -o /dev/null http://google.com/")
        assert (result == 0)

    # test that client can download zip
    def test_011_httpNonVirusNotBlocked(self):
        result = clientControl.runCommand("wget -q -O - http://test.untangle.com/test/test.zip 2>&1 | grep -q text123")
        assert (result == 0)

    # test that client can download zip
    def test_012_httpVirusBlocked(self):
        result = clientControl.runCommand("wget -q -O - http://test.untangle.com/test/eicar.zip 2>&1 | grep -q blocked")
        assert (result == 0)

    def test_100_eventlog_httpVirus(self):
        fname = sys._getframe().f_code.co_name
        result = clientControl.runCommand("wget -q -O - http://test.untangle.com/test/eicar.zip?arg=%s 2>&1 | grep -q blocked" % fname)
        assert (result == 0)
        flushEvents()
        query = None;
        for q in node.getWebEventQueries():
            if q['name'] == 'Infected Web Events': query = q;
        assert(query != None)
        events = uvmContext.getEvents(query['query'],defaultRackId,1)
        assert(events != None)
        assert(events['list'] != None)
        assert(len(events['list']) > 0)
        print "Event:" + str(events['list'][0])
        assert(events['list'][0]['host'] == "test.untangle.com")
        assert(events['list'][0]['uri'] == ("/test/eicar.zip?arg=%s" % fname))
        assert(events['list'][0]['virus_' + self.vendorName() + '_name'] != None)
        assert(events['list'][0]['virus_' + self.vendorName() + '_clean'] == False)

    def test_101_eventlog_httpNonVirus(self):
        fname = sys._getframe().f_code.co_name
        result = clientControl.runCommand("wget -q -O - http://test.untangle.com/test/test.zip?arg=%s 2>&1 | grep -q text123" % fname)
        assert (result == 0)
        flushEvents()
        query = None;
        for q in node.getWebEventQueries():
            if q['name'] == 'Clean Web Events': query = q;
        assert(query != None)
        events = uvmContext.getEvents(query['query'],defaultRackId,1)
        assert(events != None)
        assert(events['list'] != None)
        assert(len(events['list']) > 0)
        print "Event:" + str(events['list'][0])
        assert(events['list'][0]['host'] == "test.untangle.com")
        assert(events['list'][0]['uri'] == ("/test/test.zip?arg=%s" % fname))
        assert(events['list'][0]['virus_' + self.vendorName() + '_clean'] == True)

    def test_102_eventlog_smtpVirus(self):
        startTime = datetime.now()
        fname = sys._getframe().f_code.co_name
        # download the email script
        result = clientControl.runCommand("wget -O /tmp/email_script.py http://metaloft.com/test/email_script.py 1>/dev/null 2>&1")
        assert (result == 0)
        result = clientControl.runCommand("chmod 775 /tmp/email_script.py")
        assert (result == 0)
        # email the file
        result = clientControl.runCommand("/tmp/email_script.py --server=74.123.29.140 --from=junk@test.untangle.com --to=junk@test.untangle.com --subject='%s' --body='body' --file=/tmp/eicar" % (fname))
        assert (result == 0)
        flushEvents()
        query = None;
        for q in node.getMailEventQueries():
            if q['name'] == 'Infected Email Events': query = q;
        assert(query != None)
        events = uvmContext.getEvents(query['query'],defaultRackId,1)
        assert(events != None)
        assert(events['list'] != None)
        print "startTime: " + str(startTime)
        assert(len(events['list']) > 0)
        print "Event:" + str(events['list'][0])
        assert(events['list'][0]['addr'] == "junk@test.untangle.com")
        assert(events['list'][0]['subject'] == str(fname))
        assert(events['list'][0]['virus_' + self.vendorName() + '_clean'] == False)
        assert(datetime.fromtimestamp((events['list'][0]['time_stamp']['time'])/1000) > startTime)

    def test_103_eventlog_smtpNonVirus(self):
        startTime = datetime.now()
        fname = sys._getframe().f_code.co_name
        result = clientControl.runCommand("echo '%s' > /tmp/attachment-%s" % (fname, fname))
        assert (result == 0)
        # download the email script
        result = clientControl.runCommand("wget -O /tmp/email_script.py http://metaloft.com/test/email_script.py 1>/dev/null 2>&1")
        assert (result == 0)
        result = clientControl.runCommand("chmod 775 /tmp/email_script.py")
        assert (result == 0)
        # email the file
        result = clientControl.runCommand("/tmp/email_script.py --server=74.123.29.140 --from=junk@test.untangle.com --to=junk@test.untangle.com --subject='%s' --body='body' --file=/tmp/attachment-%s" % (fname, fname))
        assert (result == 0)
        flushEvents()
        query = None;
        for q in node.getMailEventQueries():
            if q['name'] == 'Clean Email Events': query = q;
        assert(query != None)
        events = uvmContext.getEvents(query['query'],defaultRackId,1)
        assert(events != None)
        assert(events['list'] != None)
        print "startTime: " + str(startTime)
        assert(len(events['list']) > 0)
        print "Event:" + str(events['list'][0])
        assert(events['list'][0]['addr'] == "junk@test.untangle.com")
        assert(events['list'][0]['subject'] == str(fname))
        assert(events['list'][0]['virus_' + self.vendorName() + '_clean'] == True)
        assert(datetime.fromtimestamp((events['list'][0]['time_stamp']['time'])/1000) > startTime)

    def test_999_finalTearDown(self):
        global node
        uvmContext.nodeManager().destroy( node.getNodeSettings()["id"] )
        node = None
        








