"""ssl_inspector tests"""
import datetime

import unittest
from tests.global_functions import uvmContext
import runtests.remote_control as remote_control
import runtests.test_registry as test_registry
import tests.global_functions as global_functions
import tests.ipaddr as ipaddr
from uvm import Uvm

default_policy_id = 1
app = None
appWeb = None
pornServerName="www.pornhub.com"
testedServerName="news.ycombinator.com"
testedServerURLParts = testedServerName.split(".")
testedServerDomainWildcard = "*" + testedServerURLParts[-2] + "." + testedServerURLParts[-1]
dropboxIssuer="/C=US/ST=California/L=San Francisco/O=Dropbox"

def createSSLInspectRule(url=testedServerDomainWildcard):
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
                    "conditionType": "SSL_INSPECTOR_SNI_HOSTNAME",
                    "invert": False,
                    "javaClass": "com.untangle.app.ssl_inspector.SslInspectorRuleCondition",
                    "value": url
                }
            ]
        },
        "description": url,
        "javaClass": "com.untangle.app.ssl_inspector.SslInspectorRule",
        "enabled": True,
        "ruleId": 1
    };

def findRule(target_description):
    found = False
    for rule in appData['ignoreRules']['list']:
        if rule['description'] == target_description:
            found = True
            break
    return found
    
def addBlockedUrl(url, blocked=True, flagged=True, description="description"):
    newRule = { "blocked": blocked, "description": description, "flagged": flagged, "javaClass": "com.untangle.uvm.app.GenericRule", "string": url }
    rules = appWeb.getBlockedUrls()
    rules["list"].append(newRule)
    appWeb.setBlockedUrls(rules)

def nukeBlockedUrls():
    rules = appWeb.getBlockedUrls()
    rules["list"] = []
    appWeb.setBlockedUrls(rules)

def search_term_rule_add(termWords, blocked=True, flagged=True, description="description"):
    newTerm =  {
        "blocked": blocked,
        "flagged": flagged,
        "description": description,
        "javaClass": "com.untangle.uvm.app.GenericRule",
        "string": termWords,
        }
    webSettings = appWeb.getSettings()
    webSettings["searchTerms"]["list"].append(newTerm)
    appWeb.setSettings(webSettings)

def search_term_rules_clear():
    webSettings = appWeb.getSettings()
    webSettings["searchTerms"]["list"] = []
    appWeb.setSettings(webSettings)

class SslInspectorTests(unittest.TestCase):

    @staticmethod
    def module_name():
        return "ssl-inspector"

    @staticmethod
    def appWeb():
        return "web-filter"

    @staticmethod
    def initial_setup(self):
        global app, appData, appWeb, appWebData
        if uvmContext.appManager().isInstantiated(self.module_name()):
            raise Exception('app %s already instantiated' % self.module_name())
        app = uvmContext.appManager().instantiate(self.module_name(), default_policy_id)
        app.start() # must be called since the app doesn't auto-start
        appData = app.getSettings()
        if (uvmContext.appManager().isInstantiated(self.appWeb())):
            raise Exception('app %s already instantiated' % self.appWeb())
        appWeb = uvmContext.appManager().instantiate(self.appWeb(), default_policy_id)
        appWebData = appWeb.getSettings()

        appData['ignoreRules']['list'].insert(0,createSSLInspectRule(testedServerDomainWildcard))
        app.setSettings(appData)
        
    def setUp(self):
        pass

    def test_010_clientIsOnline(self):
        result = remote_control.is_online()
        assert (result == 0)
            
    def test_011_license_valid(self):
        assert(uvmContext.licenseManager().isLicenseValid(self.module_name()))

    def test_012_checkServerCertificate(self):
        result = remote_control.run_command('echo -n | openssl s_client -connect %s:443 -servername %s 2>/dev/null | grep -qi "untangle"' % (testedServerName, testedServerName))
        assert (result == 0)

    def test_015_checkWebFilterBlockInspected(self):
        addBlockedUrl(testedServerName)
        remote_control.run_command('curl -s -4 --connect-timeout 10 --trace-ascii /tmp/ssl_test_015.trace --output /tmp/ssl_test_015.output --insecure https://%s' % (testedServerName))
        nukeBlockedUrls()
        result = remote_control.run_command('grep blockpage /tmp/ssl_test_015.trace')
        assert (result == 0)
        result = remote_control.run_command("wget -q -4 -t 2 --timeout=5 --no-check-certificate -q -O - https://%s 2>&1 | grep -q blockpage" % (pornServerName))
        assert (result == 0)        

    def test_020_checkIgnoreCertificate(self):
        if findRule('Ignore Dropbox'):
            result = remote_control.run_command('echo -n | openssl s_client -connect www.dropbox.com:443 -servername www.dropbox.com 2>/dev/null | grep -q \'%s\'' % (dropboxIssuer))
            assert (result == 0)
        else:
            raise unittest.SkipTest('SSL Inspector does not have Ignore Dropbox rule')

    def test_030_checkSslInspectorInspectorEventLog(self):
        remote_control.run_command('curl -s -4 --connect-timeout 10 --trace /tmp/ssl_test_040.trace --output /tmp/ssl_test_040.output --insecure https://%s' % (testedServerName))
        events = global_functions.get_events('SSL Inspector','All Sessions',None,5)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            "ssl_inspector_status","INSPECTED",
                                            "ssl_inspector_detail",testedServerName)
        assert( found )

    def test_040_checkWebFilterEventLog(self):
        addBlockedUrl(testedServerName)
        remote_control.run_command('curl -s -4 --connect-timeout 10 --trace /tmp/ssl_test_040.trace --output /tmp/ssl_test_040.output --insecure https://%s' % (testedServerName))
        nukeBlockedUrls()

        events = global_functions.get_events('Web Filter','Blocked Web Events',None,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            "host", testedServerName,
                                            "web_filter_blocked", True)
        assert( found )

    # Query eventlog
    def test_060_queryEventLog(self):
        termTests = [{
            "host": "www.bing.com",
            "uri":  "/search?q=oneterm&qs=n&form=QBRE",
            "term": "oneterm"
        },{
            "host": "www.bing.com",
            "uri":  "/search?q=two+terms&qs=n&form=QBRE",
            "term": "two terms"
        },{
            "host": "www.bing.com",
            "uri":  "/search?q=%22quoted+terms%22&qs=n&form=QBRE",
            "term": '"quoted terms"'
        },{
            "host": "search.yahoo.com",
            "uri":  "/search?p=oneterm",
            "term": "oneterm"
        },{
            "host": "search.yahoo.com",
            "uri":  "/search?p=%22quoted+terms%22",
            "term": '"quoted terms"'
        },{
            "host": "search.yahoo.com",
            "uri":  "/search?p=two+terms",
            "term": "two terms"
        }]
        host = "www.bing.com"
        uri = "/search?q=oneterm&qs=n&form=QBRE"
        for t in termTests:
            eventTime = datetime.datetime.now()
            result = remote_control.run_command("curl -s -4 -o /dev/null -A 'Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.8.1.1) Gecko/20061204 Firefox/2.0.0.1' --connect-timeout 10 --insecure 'https://%s%s'" % ( t["host"], t["uri"] ) )
            assert( result == 0 )

            events = global_functions.get_events('Web Filter','All Search Events',None,1)
            assert(events != None)
            found = global_functions.check_events( events.get('list'), 5,
                                                "host", t["host"],
                                                "term", t["term"])
            assert( found )

    def test_610_web_search_rules(self):
        """check the https web rule searches log correctly"""
        term = "boobs"

        termTests = [{
            "host": "www.bing.com",
            "uri":  ("/search?q=%s&qs=n&form=QBRE" % term),
        },{
            "host": "search.yahoo.com",
            "uri": ("/search?p=%s" % term),
        },{
            "host": "www.google.com",
            "uri":  ("/search?hl=en&q=%s" % term),
        }]
        search_term_rule_add(term)
        for t in termTests:
            eventTime = datetime.datetime.now()
            result = remote_control.run_command("curl -s -4 -o /dev/null -A 'Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.8.1.1) Gecko/20061204 Firefox/2.0.0.1' --connect-timeout 10 --insecure 'https://%s%s'" % ( t["host"], t["uri"] ) )
            assert( result == 0 )
            events = global_functions.get_events("Web Filter",'All Search Events',None,1)
            found = global_functions.check_events( events.get('list'), 5,
                                                   "host", t["host"],
                                                   "term", term,
                                                   'blocked', True,
                                                   'flagged', True )
            assert( found )
        search_term_rules_clear()

    @staticmethod
    def final_tear_down(self):
        global app, appWeb
        if app != None:
            uvmContext.appManager().destroy( app.getAppSettings()["id"] )
            app = None
        if appWeb != None:
            uvmContext.appManager().destroy( appWeb.getAppSettings()["id"])
            appWeb = None

test_registry.register_module("ssl-inspector", SslInspectorTests)
