import unittest2
import time
import sys
import datetime

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

def addCookieEnabled(url, enabled=True, description="description"):
    global node
    newRule =  { "enabled": enabled, "description": description, "javaClass": "com.untangle.uvm.node.GenericRule", "string": url }
    rules = node.getCookies()
    rules["list"].append(newRule)
    node.setCookies(rules)
    
def addCookieBlockedEnabled(url, enabled=True, description="description"):
    global node
    newRule =  { "enabled": enabled, "blocked": "", "description": description, "javaClass": "com.untangle.uvm.node.GenericRule", "string": url }
    rules = node.getCookies()
    rules["list"].append(newRule)
    node.setCookies(rules)
    
def addRule(url, enabled=True, description="description", blocked=True):
    global node
    newRule =  { "enabled": enabled, "description": description, "javaClass": "com.untangle.uvm.node.GenericRule", "string": url, "blocked": blocked }
    
    settings = node.getSettings()
    userList = settings["userRules"]
    userList["list"] = [newRule]
    settings["userRules"] = userList
       
    node.setSettings(settings) 
    
def nukeRules(listName):
    settings = node.getSettings()
    userList = settings[listName]
    userList["list"] = []
    settings[listName] = userList
       
    node.setSettings(settings) 
    
def addPassRule(url, enabled, listName):
    global node
    newRule =  { "enabled": enabled, "description": "description", "javaClass": "com.untangle.uvm.node.GenericRule", "string": url }
    
    settings = node.getSettings()
    passList = settings[listName]
    passList["list"] = [newRule]
    settings[listName] = passList
           
    node.setSettings(settings)
    
class AdBlockerTests(unittest2.TestCase):

    @staticmethod
    def nodeName():
        return "ad-blocker"

    @staticmethod
    def initialSetUp(self):
        global node
        if (uvmContext.nodeManager().isInstantiated(self.nodeName())):
            raise Exception('node %s already instantiated' % self.nodeName())
        node = uvmContext.nodeManager().instantiate(self.nodeName(), defaultRackId)
        node.start() # must be called since ad blocker doesn't auto-start

    def setUp(self):
        pass

    # verify client is online
    def test_010_clientIsOnline(self):
        result = remote_control.is_online()
        assert (result == 0)

    # check that "ad" is blocked - can't use test.untangle.com because everything *untangle* is ignored
    def test_011_adIsBlocked(self):
        result = remote_control.run_command("wget -4 -q -O /dev/null http://ads.pubmatic.com/AdServer/js/showad.js")
        assert (result != 0)
         
    def test_012_notBlocked(self):
        result = remote_control.run_command("wget -4 -q -O /dev/null http://www.google.com")
        assert (result == 0)
 
    def test_013_eventlog_blockedAd(self):
        fname = sys._getframe().f_code.co_name
        # specify an argument so it isn't confused with other events
        eventTime = datetime.datetime.now()
        result = remote_control.run_command("wget -4 -q -O /dev/null http://ads.pubmatic.com/AdServer/js/showad.js?arg=%s" % fname)
        assert ( result != 0 )

        events = global_functions.get_events('Ad Blocker','Blocked Ad Events',None,1)
        assert( events != None )
        found = global_functions.check_events( events.get('list'), 5,
                                            'host', 'ads.pubmatic.com',
                                            'uri', ("/AdServer/js/showad.js?arg=%s" % fname),
                                            'ad_blocker_action', 'B' )
        assert( found )
        
    def test_014_userDefinedAdIsBlocked(self):
        addRule("google.com", True, "description", True)
        result = remote_control.run_command("wget -4 -q -O /dev/null http://www.google.com")
        nukeRules("userRules")
        assert (result != 0)
        
    def test_015_userDefinedAdNotBlocked(self):
        addRule("showad.js", True, "description", False)
        result = remote_control.run_command("wget -4 -q -O /dev/null http://ads.pubmatic.com/AdServer/js/showad.js")
        nukeRules("userRules")
        assert (result == 0)
           
    def test_016_passSiteDisabled(self):
        addPassRule("ads.pubmatic.com", False, "passedUrls")
        result = remote_control.run_command("wget -4 -q -O /dev/null http://ads.pubmatic.com/AdServer/js/showad.js")
        nukeRules("passedUrls")
        assert (result != 0)
         
    def test_017_passSiteEnabled(self):
        addPassRule("ads.pubmatic.com", True, "passedUrls")
        result = remote_control.run_command("wget -4 -q -O /dev/null http://ads.pubmatic.com/AdServer/js/showad.js")
        nukeRules("passedUrls")
        assert (result == 0)
        
    def test_018_passClientDisabled(self):
        addPassRule(remote_control.clientIP, False, "passedClients")
        result = remote_control.run_command("wget -4 -q -O /dev/null http://ads.pubmatic.com/AdServer/js/showad.js")
        nukeRules("passedClients")
        assert (result != 0)
         
    def test_019_passClientEnabled(self):
        addPassRule(remote_control.clientIP, True, "passedClients")
        result = remote_control.run_command("wget -4 -q -O /dev/null http://ads.pubmatic.com/AdServer/js/showad.js")
        nukeRules("passedClients")
        assert (result == 0)
        
    # verify there is a accuweather cookie
    def test_100_accuweatherCookie(self):
        # remove any previous instance of testcookie.txt
        remote_control.run_command("/bin/rm -f testcookie.txt")
        # see if cookie is downloaded.
        result = remote_control.run_command("rm -f /tmp/testcookie.txt ; wget -4 -q -O /dev/null --save-cookies /tmp/testcookie.txt http://accuweather.com/ ; grep -q accuweather.com /tmp/testcookie.txt")
        assert (result == 0)
 
    # verify a accuweather cookie can be blocked
    def test_101_accuweatherCookieEnabled(self):
        addCookieEnabled("accuweather.com")
        # remove any previous instance of testcookie.txt
        remote_control.run_command("/bin/rm -f /tmp/testcookie.txt")
        # see if cookie is downloaded.
        result = remote_control.run_command("rm -f /tmp/testcookie.txt ; wget -4 -q -O /dev/null --save-cookies /tmp/testcookie.txt http://accuweather.com/ ; grep -q accuweather.com /tmp/testcookie.txt")
        assert (result == 1)
         
    # verify a accuweather cookie can be blocked, but set both "enabled" and "blocked" params
    def test_102_accuweatherCookieBlockedEnabled(self):
        addCookieBlockedEnabled("www.accuweather.com")
        # remove any previous instance of testcookie.txt
        remote_control.run_command("/bin/rm -f /tmp/testcookie.txt")
        # see if cookie is downloaded.
        result = remote_control.run_command("rm -f /tmp/testcookie.txt ; wget -4 -q -O /dev/null --save-cookies /tmp/testcookie.txt http://www.accuweather.com/ ; grep -q www.accuweather.com /tmp/testcookie.txt")
        assert (result == 1)

    # verify update mechanism
    def test_110_updateAdBlockRules(self):
        node.updateList()
        result = node.getListLastUpdate()
        today_str = datetime.datetime.utcnow().strftime("%d %b %Y")
        yesterday_str = (datetime.datetime.utcnow() - datetime.timedelta(days=1)).strftime("%d %b %Y")
        print "Last Update: \"%s\"" % (result)
        print "Today: \"%s\"" % (today_str)
        print "Yesterday: \"%s\"" % (yesterday_str)
        assert((today_str in result) or (yesterday_str in result))

    @staticmethod
    def finalTearDown(self):
        global node
        if node != None:
            uvmContext.nodeManager().destroy( node.getNodeSettings()["id"] )
        node = None
        

test_registry.registerNode("ad-blocker", AdBlockerTests)
