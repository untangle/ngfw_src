"""ad_blocker tests"""
import sys
import datetime
import unittest
import pytest
import subprocess
import os

from tests.common import NGFWTestCase
from tests.global_functions import uvmContext
import runtests.remote_control as remote_control
import runtests.test_registry as test_registry
import tests.global_functions as global_functions


app = None

def addCookieEnabled(url, enabled=True, description="description"):
    global app
    newRule =  { "enabled": enabled, "description": description, "javaClass": "com.untangle.uvm.app.GenericRule", "string": url }
    settings = app.getSettings()
    settings['cookies']['list'].append(newRule)
    app.setSettings(settings)
    
def addCookieBlockedEnabled(url, enabled=True, description="description"):
    global app
    newRule =  { "enabled": enabled, "blocked": "", "description": description, "javaClass": "com.untangle.uvm.app.GenericRule", "string": url }
    settings = app.getSettings()
    settings['cookies']['list'].append(newRule)
    app.setSettings(settings)

def addRule(url, enabled=True, description="description", blocked=True):
    global app
    newRule =  { "enabled": enabled, "description": description, "javaClass": "com.untangle.uvm.app.GenericRule", "string": url, "blocked": blocked }
    
    settings = app.getSettings()
    userList = settings["userRules"]
    userList["list"] = [newRule]
    settings["userRules"] = userList
       
    app.setSettings(settings) 
    
def nukeRules(listName):
    settings = app.getSettings()
    userList = settings[listName]
    userList["list"] = []
    settings[listName] = userList
       
    app.setSettings(settings) 
    
def addPassRule(url, enabled, listName):
    global app
    newRule =  { "enabled": enabled, "description": "description", "javaClass": "com.untangle.uvm.app.GenericRule", "string": url }
    
    settings = app.getSettings()
    passList = settings[listName]
    passList["list"] = [newRule]
    settings[listName] = passList
           
    app.setSettings(settings)
    
@pytest.mark.ad_blocker
class AdBlockerTests(NGFWTestCase):

    force_start = True

    @staticmethod
    def module_name():
        # cheap trick to force class variable _app into global namespace as app
        return "ad-blocker"

    @classmethod
    def initial_extra_setup(cls):
        global app
        app = AdBlockerTests._app
        # download a known list so the test URLs are matching
        if os.path.isdir('/usr/share/untangle/lib/ad-blocker/'):
            result = subprocess.call("rm /usr/share/untangle/lib/ad-blocker/adblock_easylist_2_0.txt", shell=True)
            result_download = subprocess.call(global_functions.build_wget_command(output_file="/usr/share/untangle/lib/ad-blocker/adblock_easylist_2_0.txt", uri="http://" + global_functions.ACCOUNT_FILE_SERVER + "/test/adblock_easylist_2_0.txt"), shell=True)
            if result_download != 0:
                NGFWTestCase.skipTest(cls,"Failed to download stock adblock_list")
        # reload the rules
        app.initializeSettings()
    
    # verify client is online
    def test_010_clientIsOnline(self):
        result = remote_control.is_online()
        assert (result == 0)

    def test_011_license_valid(self):
        assert(uvmContext.licenseManager().isLicenseValid(self.module_name()))

        # check that "ad" is blocked - can't use test.untangle.com because everything *untangle* is ignored
    def test_021_adIsBlocked(self):
        result = remote_control.run_command(global_functions.build_wget_command(output_file="/dev/null", uri="http://ads.pubmatic.com/AdServer/js/showad.js"))
        assert (result != 0)
         
    def test_022_notBlocked(self):
        result = remote_control.run_command(global_functions.build_wget_command(output_file="/dev/null", uri="http://www.google.com"))
        assert (result == 0)
        events = global_functions.get_events('Ad Blocker','All Ad Events',None,1)
        assert( events != None )
        found = global_functions.check_events( events.get('list'), 5,
                                            'host', 'www.google.com',
                                            'uri', ("/"),
                                            'ad_blocker_action', 'P' )
 
    @pytest.mark.failure_behind_pihole
    def test_023_eventlog_blockedAd(self):
        fname = sys._getframe().f_code.co_name
        # specify an argument so it isn't confused with other events
        eventTime = datetime.datetime.now()
        result = remote_control.run_command(global_functions.build_wget_command(output_file="/dev/null", uri=f"http://ads.pubmatic.com/AdServer/js/showad.js?arg={fname}"))
        assert ( result != 0 )

        events = global_functions.get_events('Ad Blocker','Blocked Ad Events',None,1)
        assert( events != None )
        found = global_functions.check_events( events.get('list'), 5,
                                            'host', 'ads.pubmatic.com',
                                            'uri', ("/AdServer/js/showad.js?arg=%s" % fname),
                                            'ad_blocker_action', 'B' )
        assert( found )
        
    def test_024_userDefinedAdIsBlocked(self):
        addRule("google.com", True, "description", True)
        result = remote_control.run_command(global_functions.build_wget_command(output_file="/dev/null", uri="http://www.google.com"))
        nukeRules("userRules")
        assert (result != 0)
        
    @pytest.mark.failure_behind_pihole
    def test_025_userDefinedAdNotBlocked(self):
        addRule("showad.js", True, "description", False)
        result = remote_control.run_command(global_functions.build_wget_command(output_file="/dev/null", uri="http://ads.pubmatic.com/AdServer/js/showad.js"))
        nukeRules("userRules")
        assert (result == 0)
           
    def test_026_passSiteDisabled(self):
        addPassRule("ads.pubmatic.com", False, "passedUrls")
        result = remote_control.run_command(global_functions.build_wget_command(output_file="/dev/null", uri="http://ads.pubmatic.com/AdServer/js/showad.js"))
        nukeRules("passedUrls")
        assert (result != 0)
         
    @pytest.mark.failure_behind_pihole
    def test_027_passSiteEnabled(self):
        addPassRule("ads.pubmatic.com", True, "passedUrls")
        result = remote_control.run_command(global_functions.build_wget_command(output_file="/dev/null", uri="http://ads.pubmatic.com/AdServer/js/showad.js"))
        nukeRules("passedUrls")
        assert (result == 0)
        
    def test_028_passClientDisabled(self):
        addPassRule(remote_control.client_ip, False, "passedClients")
        result = remote_control.run_command(global_functions.build_wget_command(output_file="/dev/null", uri="http://ads.pubmatic.com/AdServer/js/showad.js"))
        nukeRules("passedClients")
        assert (result != 0)
         
    @pytest.mark.failure_behind_pihole
    def test_029_passClientEnabled(self):
        addPassRule(remote_control.client_ip, True, "passedClients")
        result = remote_control.run_command(global_functions.build_wget_command(output_file="/dev/null", uri="http://ads.pubmatic.com/AdServer/js/showad.js"))
        nukeRules("passedClients")
        assert (result == 0)
        
    # verify there is a test.untangle.com cookie
    def test_100_webCookie(self):
        # remove any previous instance of testcookie.txt
        remote_control.run_command("/bin/rm -f testcookie.txt")
        # see if cookie is downloaded.
        result = remote_control.run_command("rm -f /tmp/testcookie.txt ;" + global_functions.build_wget_command(output_file="/dev/null", uri="http://test.untangle.com/mycookie.php", cookies_save_file="/tmp/testcookie.txt") + "; grep -q untangle.com /tmp/testcookie.txt")
        assert (result == 0)
 
    # verify a test.untangle.com cookie can be blocked
    def test_101_webCookieEnabled(self):
        addCookieEnabled("untangle.com")
        # remove any previous instance of testcookie.txt
        remote_control.run_command("/bin/rm -f /tmp/testcookie.txt")
        # see if cookie is downloaded.
        result = remote_control.run_command("rm -f /tmp/testcookie.txt ;" + global_functions.build_wget_command(output_file="/dev/null", uri="http://test.untangle.com/mycookie.php", cookies_save_file="/tmp/testcookie.txt") + "; grep -q untangle.com /tmp/testcookie.txt")
        assert (result == 1)
         
    # verify a test.untangle.com cookie can be blocked, but set both "enabled" and "blocked" params
    def test_102_webCookieBlockedEnabled(self):
        addCookieBlockedEnabled("untangle.com")
        # remove any previous instance of testcookie.txt
        remote_control.run_command("/bin/rm -f /tmp/testcookie.txt")
        # see if cookie is downloaded.
        result = remote_control.run_command("rm -f /tmp/testcookie.txt ;" + global_functions.build_wget_command(output_file="/dev/null", uri="http://test.untangle.com/mycookie.php", cookies_save_file="/tmp/testcookie.txt") + "; grep -q untangle.com /tmp/testcookie.txt")
        assert (result == 1)

    # verify update mechanism
    def test_110_updateAdBlockRules(self):
        app.updateList()
        result = app.getListLastUpdate()
        today_str = datetime.datetime.utcnow().strftime("%d %b %Y")
        yesterday_str = (datetime.datetime.utcnow() - datetime.timedelta(days=1)).strftime("%d %b %Y")
        print("Last Update: \"%s\"" % (result))
        print("Today: \"%s\"" % (today_str))
        print("Yesterday: \"%s\"" % (yesterday_str))
        assert((today_str in result) or (yesterday_str in result))

test_registry.register_module("ad-blocker", AdBlockerTests)
