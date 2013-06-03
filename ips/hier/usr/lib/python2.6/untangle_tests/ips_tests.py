import unittest2
import time
import sys
import datetime
import random
import string
from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from uvm import Manager
from uvm import Uvm
from untangle_tests import ClientControl
from untangle_tests import TestDict

uvmContext = Uvm().getUvmContext()
defaultRackId = 1
clientControl = ClientControl()
node = None

def flushEvents():
    reports = uvmContext.nodeManager().node("untangle-node-reporting")
    if (reports != None):
        reports.flushEvents()

def nukeRules():
    settings = node.getSettings()
    rules = settings["rules"]
    rules["list"] = [];
    settings["rules"] = rules
    node.setSettings(settings)

def addRule(name, sig, sid=12345, blocked=True, log=True, description="description", category="category"):
    newRule = { "id" : 1,
                "live" : True, 
                "category" : category, 
                "description": description, 
                "name" : name, 
                "log": log, 
                "blocked": blocked, 
                "text" : sig,
                "sid" : sid,
                "javaClass": "com.untangle.node.ips.IpsRule"}

    settings = node.getSettings()
    rules = settings["rules"]
    rules["list"].append(newRule)
    settings["rules"] = rules
    node.setSettings(settings)

class IpsTests(unittest2.TestCase):

    @staticmethod
    def nodeName():
        return "untangle-node-ips"

    def setUp(self):
        global node
        if node == None:
            if (uvmContext.nodeManager().isInstantiated(self.nodeName())):
                print "ERROR: Node %s already installed" % self.nodeName();
                raise Exception('node %s already instantiated' % self.nodeName())
            node = uvmContext.nodeManager().instantiateAndStart(self.nodeName(), defaultRackId)
            node.start() # must be called since ips doesn't auto-start
            flushEvents()

    # verify client is online
    def test_010_clientIsOnline(self):
        result = clientControl.runCommand("wget -4 -t 2 --timeout=5 -o /dev/null http://test.untangle.com/")
        assert (result == 0)

    def test_011_testblock(self):
        nukeRules()
        addRule("GET content test", "tcp any any -> any any (msg:\"FOO\"; content:\"GET\"; sid:1900; rev:10;)", category="aaaaaaa", description="aaaaaaa")
        result = clientControl.runCommand("wget -o /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        nukeRules()
        assert (result != 0)

    def test_100_eventlog(self):
        nukeRules()
        randomName = "".join( [random.choice(string.letters) for i in xrange(8)] )
        addRule("GET content test", "tcp any any -> any any (msg:\"FOO\"; content:\"GET\"; sid:1900; rev:10;)", sid=random.randint(10000,99999999), category=randomName, description=randomName)
        result = clientControl.runCommand("wget -o /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)
        time.sleep(1);
        flushEvents()
        query = None;
        for q in node.getEventQueries():
            if q['name'] == 'All Events': query = q;
        assert(query != None)
        events = uvmContext.getEvents(query['query'],defaultRackId,1)
        assert(events != None)
        assert(events['list'] != None)
        assert(len(events['list']) > 0)
        print events['list'][0]
        assert(events['list'][0]['ips_description'] == randomName)

    def test_999_finalTearDown(self):
        global node
        uvmContext.nodeManager().destroy( node.getNodeSettings()["id"] )
        node = None
        

TestDict.registerNode("ips", IpsTests)
