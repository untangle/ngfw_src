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
import remote_control
import test_registry
import global_functions

uvmContext = Uvm().getUvmContext()
defaultRackId = 1
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
            node = uvmContext.nodeManager().instantiate(self.nodeName(), defaultRackId)
            node.start() # must be called since ips doesn't auto-start
            flushEvents()

    # verify client is online
    def test_010_clientIsOnline(self):
        result = remote_control.isOnline()
        assert (result == 0)

    def test_011_testblock(self):
        nukeRules()
        addRule("GET content test", "tcp any any -> any any (msg:\"FOO\"; content:\"GET\"; sid:1900; rev:10;)", category="aaaaaaa", description="aaaaaaa")
        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        nukeRules()
        assert (result != 0)

    def test_100_eventlog(self):
        nukeRules()
        randomName = "".join( [random.choice(string.letters) for i in xrange(8)] )
        addRule("GET content test", "tcp any any -> any any (msg:\"FOO\"; content:\"GET\"; sid:1900; rev:10;)", sid=random.randint(10000,99999999), category=randomName, description=randomName)
        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)
        time.sleep(1);
        flushEvents()
        query = None;
        for q in node.getEventQueries():
            if q['name'] == 'All Events': query = q;
        assert(query != None)
        events = uvmContext.getEvents(query['query'],defaultRackId,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            'ips_description', randomName)
        assert( found )

    @staticmethod
    def finalTearDown(self):
        global node
        if node == None:
            return
        uvmContext.nodeManager().destroy( node.getNodeSettings()["id"] )
        node = None
        

test_registry.registerNode("ips", IpsTests)
