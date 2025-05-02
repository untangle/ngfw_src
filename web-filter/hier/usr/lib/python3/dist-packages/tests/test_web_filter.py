"""web_filter tests"""
import sys
import re
import datetime
import calendar
import socket
import subprocess
import copy

import unittest
import pytest
import glob
import os
import datetime
import time
import runtests.remote_control as remote_control
import runtests.test_registry as test_registry
import tests.global_functions as global_functions

from .test_web_filter_base import WebFilterBaseTests
from uvm import Uvm
from concurrent.futures import ThreadPoolExecutor


def setHttpHttpsPorts(httpPort, httpsPort):
    netsettings = global_functions.uvmContext.networkManager().getNetworkSettings()
    netsettings['httpPort'] = httpPort
    netsettings['httpsPort'] = httpsPort
    global_functions.uvmContext.networkManager().setNetworkSettings(netsettings)

#
# Just extends the web filter base tests
#
@pytest.mark.web_filter
class WebFilterTests(WebFilterBaseTests):

    @staticmethod
    def module_name():
        return "web-filter"

    @staticmethod
    def shortAppName():
        return "web-filter"

    @staticmethod
    def eventAppName():
        return "web_filter"

    @staticmethod
    def displayName():
        return "Web Filter"
    
    @staticmethod
    def policyAppName():
        return "policy-manager"

    @classmethod
    def initial_extra_setup(cls):
        global orig_settings, web_app_1, web_app_2,web_app_1_id, web_app_2_id, secondRackId, thirdRackId, policy_app, primary_ip, secondary_ip, interface, default_policy_id, web_app_3
        default_policy_id = 1
        web_app_3 = None
        interface = None
        primary_ip = None
        secondary_ip = None
        thirdRackId = None
        secondRackId = None
        web_app_2 = None
        web_app_1 = cls._app
        webFilterSettings = cls._app.getSettings()
        orig_settings = copy.deepcopy(webFilterSettings)
        web_app_1_id = web_app_1.getAppSettings()["id"]
        if (global_functions.uvmContext.appManager().isInstantiated(cls.policyAppName())):
            raise Exception('app %s already instantiated' % cls.policyAppName())
        policy_app = global_functions.uvmContext.appManager().instantiate(cls.policyAppName())
        secondRackId = global_functions.addRack(policy_app, name= "Second Rack")
        web_app_2 = global_functions.uvmContext.appManager().instantiate(cls.module_name(), secondRackId)
        #Add secondary IP to client to test blocked/Passed traffic 
        web_app_2_id = web_app_2.getAppSettings()["id"]
        interface = global_functions.get_network_interface()
        if interface:
            # Get the primary IP of the interface
            primary_ip = global_functions.get_primary_ip(interface)
            if primary_ip:
                # Add secondary IP to the remote server
                primary_ip,secondary_ip = global_functions.add_secondary_ip(interface, primary_ip)
        default_policy_rule = global_functions.appendRule(policy_app, global_functions.createPolicySingleConditionRule("SRC_ADDR",primary_ip, default_policy_id))
        second_policy_rule = global_functions.appendRule(policy_app, global_functions.createPolicySingleConditionRule("SRC_ADDR",secondary_ip, secondRackId))


    def worker(self, app_id):
        print(f"Worker thread running for app_id: {app_id}")
        try:
            app = Uvm().getUvmContext(timeout=300).appManager().app(int(app_id))

            if app is None:
                print(f"App with ID {app_id} is not installed.")
                return

            print(app.getAppSettings())  #RPC CALL
            print()
            # Generate dynamic rule name
            rule_base = f"Rule-{app_id}"

            # Build new rule
            newRule = {
                "javaClass": "java.util.LinkedList",
                "list": [
                    {
                        "blocked": True,
                        "flagged": True,
                        "string": f"{rule_base}-B",
                        "javaClass": "com.untangle.uvm.app.GenericRule",
                        "isGlobal": True,
                        "description": "",
                    }
                ]
            }
            app.setBlockedUrls(newRule)  #RPC CALL
        except Exception as e:
            print(f"Exception in app_id {app_id}: {e}")

    def test_016_block_url(self):
        """verify basic URL blocking the the url block list"""
        pre_events_scan = global_functions.get_app_metric_value(self._app, "scan")
        pre_events_block = global_functions.get_app_metric_value(self._app, "block")
        self.block_url_list_add("test.untangle.com/test/testPage1.html")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.block_url_list_clear()
        assert ( result == 0 )
        # verify the faceplate counters have incremented.
        post_events_scan = global_functions.get_app_metric_value(self._app, "scan")
        post_events_block = global_functions.get_app_metric_value(self._app, "block")
        assert(pre_events_scan < post_events_scan)
        assert(pre_events_block < post_events_block)

    def test_019_porn_is_blocked_alt_port(self):
        setHttpHttpsPorts(8081,443)
        result = self.get_web_request_results(url="http://www.pornhub.com/", expected="blockpage")
        setHttpHttpsPorts(80,443)
        assert (result == 0)
        found = self.check_events("www.pornhub.com", "/", True)
        assert( found )

    def test_029_blocked_url_https_http_alt_ports(self):
        setHttpHttpsPorts(8081,4443)
        self.block_url_list_add("test.untangle.com")
        result = self.get_web_request_results(url="https://test.untangle.com/", expected="blockpage")
        self.block_url_list_clear()
        setHttpHttpsPorts(80,443)
        assert (result == 0)

    def test_100_reports_blocked_url(self):
        """check the Blocked Web Events report"""
        fname = sys._getframe().f_code.co_name
        self.block_url_list_clear();
        self.block_url_list_add("test.untangle.com/test/testPage1.html", blocked=True, flagged=True)
        # specify an argument so it isn't confused with other events
        eventTime = datetime.datetime.now()
        result1 = remote_control.run_command(global_functions.build_wget_command(output_file="-", uri=f"http://test.untangle.com/test/testPage1.html?arg={fname}") + " 2>&1 >/dev/null")
        events = global_functions.get_events(self.displayName(),'Blocked Web Events',None,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            "host","test.untangle.com",
                                            "uri", ("/test/testPage1.html?arg=%s" % fname),
                                            self.eventAppName() + '_blocked', True,
                                            self.eventAppName() + '_flagged', True )
        assert( found )

    def test_200_pass_referer_disabled(self):
        """disable pass referer and verify that a page with content that would be blocked is blocked."""
        self.block_url_list_add("test.untangle.com/test/refererPage.html")
        self.pass_url_list_add("test.untangle.com/test/testPage1.html")
        settings = self._app.getSettings()
        settings["passReferers"] = False
        self._app.setSettings(settings)
        resultReferer = remote_control.run_command(global_functions.build_wget_command(output_file="-", uri="http://test.untangle.com/test/refererPage.html", header="Referer: http://test.untangle.com/test/testPage1.html") + " 2>&1 | grep -q 'Welcome to the referer page.'")
        print("result %s passReferers %s" % (resultReferer,settings["passReferers"]))

        self.block_url_list_clear()
        self.pass_url_list_clear()
        assert( resultReferer == 1 )

    def test_201_pass_referer_enabled(self):
        """disable pass referer and verify that a page with content that would be blocked is allowed."""
        self.block_url_list_add("test.untangle.com/test/refererPage.html")
        self.pass_url_list_add("test.untangle.com/test/testPage1.html")
        settings = self._app.getSettings()
        settings["passReferers"] = True
        self._app.setSettings(settings)
        resultReferer = remote_control.run_command(global_functions.build_wget_command(output_file="-", uri="http://test.untangle.com/test/refererPage.html", header="Referer: http://test.untangle.com/test/testPage1.html") + " 2>&1 | grep -q 'Welcome to the referer page.'")
        print("result %s passReferers %s" % (resultReferer,settings["passReferers"]))

        self.block_url_list_clear()
        self.pass_url_list_clear()
        assert( resultReferer == 0 )

    def test_300_block_ip_only_hosts(self):
        """Enable 'block IP only hosts', then check that traffic to an IP is blocked"""
        settings = self._app.getSettings()
        settings["blockAllIpHosts"]=True
        self._app.setSettings(settings)
        result = self.get_web_request_results(url=global_functions.test_server_ip, expected="blockpage")
        assert(result == 0)

    @pytest.mark.failure_outside_corporate_network
    def test_301_block_QUIC(self):
        """Enable 'block QUIC (UDP port 443)' setting then check that UDP traffic over 443 is blocked (using netcat server/client)"""
        #check for passwordless sudo access for the host first, if not, skip test
        if(remote_control.run_command("sudo ls -l",stdout=False,nowait=True) != 0):
            raise unittest.SkipTest('no passwordless sudo access')
        ping_result = subprocess.call(["ping","-c","1",global_functions.LIST_SYSLOG_SERVER ],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        if (ping_result != 0):
            raise unittest.SkipTest("server not available")

        #set block to false first to verify netcat works
        settings = self._app.getSettings()
        settings["blockQuic"]=False
        self._app.setSettings(settings)

        serverHost = global_functions.LIST_SYSLOG_SERVER
        
        #set up netcat server/client connection and check that it works first
        remote_control.run_command("sudo pkill nc",host=serverHost) # kill previous running nc
        remote_control.run_command("sudo rm -f /tmp/nc_quic_false.txt",host=serverHost)
        remote_control.run_command("sudo nc -l -u -w 2 -p 443 >/tmp/nc_quic_false.txt",host=serverHost,stdout=False,nowait=True)
        remote_control.run_command("echo TEST | nc -q 1 -u -w 1 %s 443 | sleep 2" % serverHost)
        first_result =remote_control.run_command("grep TEST /tmp/nc_quic_false.txt",host=serverHost)
        assert(first_result == 0)

        #set block to true
        settings["blockQuic"]=True
        self._app.setSettings(settings)

        #retry netcat connection, verify it fails correctly
        remote_control.run_command("sudo rm -f /tmp/nc_quic_true.txt",host=serverHost)
        remote_control.run_command("sudo nc -l -u -w 2 -p 443 >/tmp/nc_quic_true.txt", host=serverHost,stdout=False,nowait=True)
        remote_control.run_command("echo TEST | sudo nc -q 1 -u -w 1 %s 443 | sleep 2" % serverHost)
        second_result =remote_control.run_command("grep TEST /tmp/nc_quic_true.txt",host=serverHost)
        assert(second_result != 0)
        remote_control.run_command("sudo pkill nc",host=serverHost) # kill running nc


    @pytest.mark.failure_in_podman
    def test_700_safe_search_enabled(self):
        """Check google/bing/yahoo safe search"""
        settings = self._app.getSettings()
        settings["enforceSafeSearch"] = False
        self._app.setSettings(settings)
        google_result_without_safe = remote_control.run_command(global_functions.build_wget_command(output_file="-", uri="http://www.google.com/search?hl=en&q=boobs&safe=off", user_agent="Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.7) Gecko/20040613 Firefox/0.8.0+)") + " | grep -q 'safe=off'")

        settings["enforceSafeSearch"] = True
        self._app.setSettings(settings)
        google_result_with_safe = remote_control.run_command(global_functions.build_wget_command(output_file="-", uri="http://www.google.com/search?hl=en&q=boobs", user_agent="Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.7) Gecko/20040613 Firefox/0.8.0+)") + " | grep -q 'safe=strict'")

        assert( google_result_without_safe == 0 )
        assert( google_result_with_safe == 0 )

        settings["enforceSafeSearch"] = False
        self._app.setSettings(settings)
        bing_result_without_safe = remote_control.run_command(global_functions.build_wget_command(output_file="-", uri="http://www.bing.com/search?q=boobs&adlt=off", user_agent="Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.7) Gecko/20040613 Firefox/0.8.0+)") + " | grep -q 'adlt=off'")

        settings["enforceSafeSearch"] = True
        self._app.setSettings(settings)
        bing_result_with_safe = remote_control.run_command(global_functions.build_wget_command(output_file="-", uri="http://www.bing.com/search?q=boobs", user_agent="Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.7) Gecko/20040613 Firefox/0.8.0+)") + " | grep -q 'adlt=strict'")

        assert(bing_result_without_safe == 0)
        assert(bing_result_with_safe == 0)

        settings["enforceSafeSearch"] = False
        self._app.setSettings(settings)
        yahoo_result_without_safe = remote_control.run_command(global_functions.build_wget_command(output_file="-", uri="http://search.yahoo.com/search?p=boobs&vm=p", user_agent="Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.7) Gecko/20040613 Firefox/0.8.0+)") + " | grep -q 'vm=p'")

        settings["enforceSafeSearch"] = True
        self._app.setSettings(settings)
        yahoo_result_with_safe = remote_control.run_command(global_functions.build_wget_command(output_file="-", uri="http://search.yahoo.com/search?p=boobs", user_agent="Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.7) Gecko/20040613 Firefox/0.8.0+)") + " | grep -q 'vm=r'")

        assert(yahoo_result_with_safe == 0)
        assert(yahoo_result_without_safe == 0)

    def test_701_unblock_option(self):
        """verify that a block page is shown but unblock button option is available."""
        self.block_url_list_add("test.untangle.com/test/testPage1.html")
        settings = self._app.getSettings()
        settings["unblockMode"] = "Host"
        self._app.setSettings(settings)
        # this test URL should be blocked but allow
        remote_control.run_command("rm -f /tmp/web_filter_base_test_120.log")
        result = remote_control.run_command(global_functions.build_wget_command(log_file="/tmp/web_filter_base_test_120.log", output_file="/tmp/web_filter_base_test_120.out", uri="http://test.untangle.com/test/testPage1.html", extra_arguments="--server-response"))
        resultButton = remote_control.run_command("grep -q 'unblock' /tmp/web_filter_base_test_120.out")
        resultBlock = remote_control.run_command("grep -q 'blockpage' /tmp/web_filter_base_test_120.out")

        # get the IP address of the block page
        ipfind = remote_control.run_command("grep 'Location' /tmp/web_filter_base_test_120.log", stdout=True)
        # print('ipFind %s' % ipfind)
        ip = re.findall( r'[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}(?:[0-9:]{0,6})', ipfind )
        blockPageIP = ip[0]
        # print('Block page IP address is %s' % blockPageIP)
        blockParamaters = re.findall( r'\?(.*)', ipfind )
        paramaters = blockParamaters[0]
        # Use unblock button.
        unBlockParameters = "global=false&"+ paramaters + "&password="
        # print("unBlockParameters %s" % unBlockParameters)
        print("wget -q -O /dev/null --post-data=\'" + unBlockParameters + "\' http://" + blockPageIP + "/" + self.shortAppName() + "/unblock")
        remote_control.run_command(global_functions.build_wget_command(output_file="/dev/null", uri="http://" + blockPageIP + "/" + self.shortAppName() + "/unblock", post_data=unBlockParameters))
        resultUnBlock = remote_control.run_command(global_functions.build_wget_command(output_file="-", uri="http://test.untangle.com/test/testPage1.html") + " 2>&1 | grep -q text123")

        self.block_url_list_clear()
        self._app.flushAllUnblockedItems()

        print("block %s button %s unblock %s" % (resultBlock,resultButton,resultUnBlock))
        assert (resultBlock == 0)
        assert (resultButton == 0)
        assert (resultUnBlock == 0)

    def test_701_unblock_option_with_password(self):
        """verify that a block page is shown but unblock if correct password."""
        fname = sys._getframe().f_code.co_name
        self.block_url_list_add("test.untangle.com/test/testPage2.html")
        settings = self._app.getSettings()
        settings["unblockMode"] = "Host"
        settings["unblockPassword"] = "atstest"
        settings["unblockPasswordEnabled"] = True
        self._app.setSettings(settings)

        # this test URL should be blocked but allow
        remote_control.run_command("rm -f /tmp/%s.log"%fname)
        result = remote_control.run_command(global_functions.build_wget_command(log_file=f"/tmp/{fname}.log", output_file=f"/tmp/{fname}.out", uri="http://test.untangle.com/test/testPage2.html", extra_arguments="--server-response"))
        resultButton = remote_control.run_command("grep -q 'unblock' /tmp/%s.out"%fname)
        resultBlock = remote_control.run_command("grep -q 'blockpage' /tmp/%s.out"%fname)

        # get the IP address of the block page
        ipfind = remote_control.run_command("grep 'Location' /tmp/%s.log" % fname,stdout=True)
        print('ipFind %s' % ipfind)
        ip = re.findall( r'[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}(?:[0-9:]{0,6})', ipfind )
        blockPageIP = ip[0]
        # print('Block page IP address is %s' % blockPageIP)
        blockParamaters = re.findall( r'\?(.*)', ipfind )
        paramaters = blockParamaters[0]
        # Use unblock button.
        unBlockParameters = "global=false&"+ paramaters + "&password=atstest"
        # print("unBlockParameters %s" % unBlockParameters)
        remote_control.run_command(global_functions.build_wget_command(output_file="/dev/null", uri="http://" + blockPageIP + "/" + self.shortAppName() + "/unblock", post_data=unBlockParameters))
        resultUnBlock = remote_control.run_command(global_functions.build_wget_command(output_file="-", uri="http://test.untangle.com/test/testPage2.html") + " 2>&1 | grep -q text123")

        settings = self._app.getSettings()
        settings["unblockMode"] = "None"
        settings["unblockPassword"] = ""
        settings["unblockPasswordEnabled"] = False

        self._app.setSettings(settings)
        self.block_url_list_clear()
        print("block %s button %s unblock %s" % (resultBlock,resultButton,resultUnBlock))
        assert (resultBlock == 0 and resultButton == 0 and resultUnBlock == 0 )

    def test_800_site_lookup(self):
        """test site lookup functionality"""
        url = "www.google.com"
        loop = 10
        site_returned = False

        while (loop >= 10) and (site_returned == False):
            site_lookup = self._app.lookupSite(url)
            site_category_id = site_lookup['list'][0]
            site_category = self._app.getSettings()['categories']['list'][site_category_id]
            #print(site_category.get('name'))
            if (site_category.get("id") != 0):
                site_returned = True
            loop -= 1
        assert (site_returned == True)


    def test_202_url_filtering_global_vs_instance_specific_rules_reflection(self):
        """
        Verify that:
        - Global block/pass rules apply across all web apps
        - Instance-specific (non-global) rules apply only to the originating web app
        - Clearing global rules does not remove instance-specific rules
        """

        # Initial state: check that both web app blocked URL lists are empty
        web_app_2_blocked_rules = web_app_2.getBlockedUrls()
        web_app_1_blocked_rules = self._app.getBlockedUrls()
        self.assertEquals(len(web_app_1_blocked_rules['list']),0)
        self.assertEquals(len(web_app_2_blocked_rules['list']), 0)

        # Add blocked URLs
        self.block_url_list_add("http://www.amazon.com", blocked=True, flagged=True, isGlobal=True, description="description")
        self.block_url_list_add("http://www.google.com", blocked=True, flagged=True, description="description")

        # Check the blocked URL lists again
        web_app_2_blocked_rules = web_app_2.getBlockedUrls()
        web_app_1_blocked_rules = self._app.getBlockedUrls()

        self.assertEquals(len(web_app_1_blocked_rules['list']), 2)  # web_app_1 should have 2 blocked URLs
        self.assertEquals(len(web_app_2_blocked_rules['list']), 1)  # web_app_2 should have 1 blocked URL

        # Clear global blocked URLs
        self.block_global_url_list_clear()

        # Check the blocked URL lists after clearing global block
        web_app_2_blocked_rules = web_app_2.getBlockedUrls()
        web_app_1_blocked_rules = self._app.getBlockedUrls()

        self.assertEquals(len(web_app_1_blocked_rules['list']), 1)  # web_app_1 should still have 1 blocked URL
        self.assertEquals(len(web_app_2_blocked_rules['list']), 0)  # web_app_2 should have 0 blocked URLs

        # Revert to original settings
        self.block_url_list_clear()

        # Initial state: check that both web app passed URL lists are empty
        web_app_2_passed_rules = web_app_2.getPassedUrls()
        web_app_1_passed_rules = self._app.getPassedUrls()
        
        # Verify that both lists are empty initially
        self.assertEquals(len(web_app_1_passed_rules['list']), 0)
        self.assertEquals(len(web_app_2_passed_rules['list']), 0)
    
        # Add passed URLs
        self.pass_url_list_add("http://www.amazon.com", enabled=True, isGlobal=True, description="description")
        self.pass_url_list_add("http://www.google.com", enabled=True, isGlobal=False, description="description")

        # Check the passed URL lists again
        web_app_2_passed_rules = web_app_2.getPassedUrls()
        web_app_1_passed_rules = self._app.getPassedUrls()

        # Verify the passed URL counts after addition
        self.assertEquals(len(web_app_1_passed_rules['list']), 2)  # Web App 1 should have 2 passed URLs
        self.assertEquals(len(web_app_2_passed_rules['list']), 1)  # Web App 2 should have 1 passed URL

        # Clear global passed URLs
        self.pass_global_url_list_clear()

        # Check the passed URL lists again after clearing
        web_app_2_passed_rules = web_app_2.getPassedUrls()
        web_app_1_passed_rules = self._app.getPassedUrls()

        # Verify the passed URL counts after clearing global passed URLs
        self.assertEquals(len(web_app_1_passed_rules['list']), 1)  # Web App 1 should still have 1 passed URL
        self.assertEquals(len(web_app_2_passed_rules['list']), 0)  # Web App 2 should have 0 passed URLs

        # Revert to original settings
        self.pass_url_list_clear()

    def test_203_url_filtering_respects_pass_and_block_rules_per_ip(self):
        """
        Verify that a global block rule applies only when a corresponding per-instance pass rule is NOT present.
        - The URL 'test.untangle.com/test/testPage1.html' is globally blocked.
        - A non-global pass rule is added (expected to apply only to the instance with `primary_ip`).
        - Requests from the primary IP (with pass rule) should NOT be blocked.
        - Requests from the secondary IP (without pass rule) SHOULD be blocked.
        """
        global primary_ip, secondary_ip, web_app_1, web_app_2
        print("PRIMARY IP : ",primary_ip)
        print("SECONDARY IP : ",secondary_ip)
        self.block_url_list_add("test.untangle.com/test/testPage1.html", blocked=True, flagged=False, isGlobal=True, description="description")
        self.pass_url_list_add("test.untangle.com/test/testPage1.html", enabled=True, isGlobal=False, description="description")
        # Verify requests from the primary IP (with pass rule) should NOT be blocked.
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage",bind_address=primary_ip)
        assert result == 1, f"Test failed: blockpage detected for primary IP ({primary_ip})"
        # Verify requests from the secondary IP (without pass rule) SHOULD be blocked.
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage",bind_address=secondary_ip)
        assert result == 0, f"Test failed: blockpage NOT detected for secondary IP ({secondary_ip})"
        # Revert to original settings
        self.block_url_list_clear()
        self.pass_url_list_clear()

    def test_204_flagged_logs_only_without_pass(self):
        """
        Verify that a globally flagged (not blocked) URL logs a flagged event only 
        for the instance which does NOT have an explicit pass rule. 
        The instance with a local pass rule for the same URL should NOT log a flagged event.

        - Add a global flagged rule (blocked=False, flagged=True) for a specific URL.
        - Add a local pass rule for the same URL on one instance (primary).
        - Perform web requests to the URL from both primary and secondary IPs.
        - Verify that:
            - The flagged event is NOT logged for the instance with the local pass rule (primary).
            - The flagged event IS logged for the instance without the pass rule (secondary).
        """
        global primary_ip, secondary_ip, web_app_1, web_app_2
        self.block_url_list_add("test.untangle.com/test/testPage1.html", blocked=False, flagged=True, isGlobal=True, description="description")
        self.pass_url_list_add("test.untangle.com/test/testPage1.html", enabled=True, isGlobal=False, description="description")
        self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html",bind_address=primary_ip)
        self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html",bind_address=secondary_ip)
        # get flagged event
        event_list = "Flagged Web Events"
        events = global_functions.get_events(self._app.getAppTitle(), event_list, None, 1)
        assert(events != None)
        # Check flagged event should not logged for the primary IP when pass rule is present
        found = global_functions.check_events( events.get('list'), 3,
                                            "host", "test.untangle.com",
                                            "uri", "/test/testdef test_803_flagged_logs_only_without_pass(self):Page1.html",
                                            'web_filter_blocked', False,
                                            'web_filter_flagged', True,
                                            'c_client_addr', primary_ip)
        assert found == False, "Flagged event was incorrectly logged for primary IP (pass rule present)."
        # Check if a flagged event is logged for the secondary IP when no pass rule is present
        found = global_functions.check_events( events.get('list'), 3,
                                            "host", "test.untangle.com",
                                            "uri", "/test/testPage1.html",
                                            'web_filter_blocked', False,
                                            'web_filter_flagged', True,
                                            'c_client_addr', secondary_ip)
        assert found == True, "Flagged event was not logged for secondary IP (no pass rule present)."
        # Revert to original settings
        self.block_url_list_clear()
        self.pass_url_list_clear()

    def test_205_global_settings_consistency_under_concurrent_updates(self):
        """
        Verifies that during concurrent setSettings calls across multiple web filter instances,
        the global settings persist correctly and the last completed update is reflected.
        Ensures that when multiple threads apply global block/pass rules, 
        the final instance reflects the latest changes.
        """
        global policy_app, web_app_1_id, web_app_2_id, web_app_3, thirdRackId

        # Add initial global rules (block + pass) to be present before concurrent updates
        self.block_url_list_add("test.untangle.com/test/testPage1.html", blocked=True, flagged=True, isGlobal=True, description="description")
        self.pass_url_list_add("http://test.untangle.com/test/test.html", enabled=True, isGlobal=True, description="description")

        # Instantiate a new web app in a new rack to participate in concurrent updates
        thirdRackId = global_functions.addRack(policy_app, name="Third Rack")
        web_app_3 = global_functions.uvmContext.appManager().instantiate(self.module_name(), thirdRackId)
        web_app_3_id = web_app_3.getAppSettings()["id"]

        # Validate initial global rules are present in the new instance
        web_app_3_blocked_rules = web_app_3.getBlockedUrls()
        web_app_3_passed_rules =  web_app_3.getPassedUrls()
        self.assertEquals(len(web_app_3_blocked_rules['list']), 1)
        self.assertEquals(len(web_app_3_passed_rules['list']), 1)

        # Clear global block/pass rules before concurrent modifications
        self.block_url_list_clear()
        self.pass_url_list_clear()

        # Run concurrent workers to apply different rule sets
        app_ids_to_run = [web_app_2_id, web_app_3_id, web_app_1_id]

        with ThreadPoolExecutor(max_workers=20) as executor:
            futures = {
                executor.submit(self.worker, app_id): app_id for app_id in app_ids_to_run
            }
        #Rpc call may take time to complete the process hence wating to finish all rpc call
        time.sleep(60)
        # Check if the blocked rule applied matches the last file id written on disk
        appFolder = "/usr/share/untangle/settings/web-filter"
        latest_file = None
        latest_mtime = 0
        pattern = re.compile(r"settings_(\d+)\.js-version-.*\.js$")

        for fname in os.listdir(appFolder):
            full_path = os.path.join(appFolder, fname)
            if pattern.match(fname) and os.path.isfile(full_path):
                mtime = os.path.getmtime(full_path)
                if mtime > latest_mtime:
                    latest_mtime = mtime
                    latest_file = fname

        if latest_file:
            match = pattern.match(latest_file)
            app_id = match.group(1)
            print(f" \n Last modified settings file: {latest_file} \n  Corresponding app_id: {app_id} \n")
        # Verify web_app_3 received the latest global rule updates
        web_app_3_blocked_rules = web_app_3.getBlockedUrls()
        self.assertEquals(len(web_app_3_blocked_rules['list']), 1)
        print("web_app_3_blocked_rules['list']  ==>   ", web_app_3_blocked_rules['list'])
        expected_prefix = f"Rule-{app_id}"
        match_found = any(rule.get("string", "").startswith(expected_prefix) for rule in web_app_3_blocked_rules['list'])
        assert match_found, f"No rule starting with '{expected_prefix}' found in blocked rules."

        # Revert to original setting
        self.block_url_list_clear()


    def test_206_global_block_rule_ignored_when_web_filter_disabled_in_policy(self):
        """
        Verifies that when Policy Manager routes traffic to a policy that does not have 
        the Web Filter app enabled, the global block rule is not enforced. The traffic 
        bypasses the Web Filter entirely and is not blocked for that specific policy instance,
        even if a global block rule exists.
        """
        global web_app_3
        
        # Ensure web_app_3 exists, otherwise skip the test
        if not web_app_3:
            raise unittest.SkipTest("Test test_205_global_settings_consistency_under_concurrent_updates success required")
        
        # Nuke existing rules and create a new policy rule for a specific condition (SRC_ADDR)
        global_functions.nukeRules(policy_app)
        third_policy_rule = global_functions.appendRule(policy_app, global_functions.createPolicySingleConditionRule("SRC_ADDR", secondary_ip, thirdRackId))

        # Add a global block rule
        self.block_url_list_add("test.untangle.com/test/testPage1.html", blocked=True, flagged=True, isGlobal=True, description="description")
        # Stop the web_app_3
        web_app_3.stop()

        # Verify that the blockpage is detected for the primary IP (should be blocked)
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage", bind_address=primary_ip)
        assert result == 0, f"Test failed: blockpage NOT detected for primary IP ({primary_ip})"

        # Verify that the blockpage is not detected for the secondary IP (bypasses Web Filter)
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage", bind_address=secondary_ip)
        assert result == 1, f"Test failed: blockpage detected for secondary IP ({secondary_ip})"

        # Restart web_app_3 and verify both IPs should be blocked now
        web_app_3.start()
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage", bind_address=primary_ip)
        assert result == 0, f"Test failed: blockpage NOT detected for primary IP ({primary_ip})"
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage", bind_address=secondary_ip)
        assert result == 0, f"Test failed: blockpage NOT detected for secondary IP ({secondary_ip})"

        # Stop web_app_3 again, and destroy it from app manager
        web_app_3.stop()
        global_functions.uvmContext.appManager().destroy(web_app_3.getAppSettings()["id"])

        # Verify that the blockpage is still not detected for the primary IP (no Web Filter app)
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage", bind_address=primary_ip)
        assert result == 0, f"Test failed: blockpage NOT detected for primary IP ({primary_ip})"

        # Verify that the blockpage is not detected for the secondary IP (bypasses Web Filter)
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage", bind_address=secondary_ip)
        assert result == 1, f"Test failed: blockpage detected for secondary IP ({secondary_ip})"
        
        # Revert to original setting
        web_app_3 = None
        self.block_url_list_clear()

    def test_207_backup_includes_global_settings_file(self):
        """
        Verify that the Untangle system backup includes the `globalSettings.js` file from Web Filter configuration.
        """
        globalSettingsFile = "usr/share/untangle/settings/web-filter/globalSettings.js"
        full_path = os.path.join("/tmp/untangleBackup", globalSettingsFile)
        subprocess.call("rm -rf /tmp/untangleBackup*", shell=True)
        result = subprocess.call(global_functions.build_wget_command(output_file='/tmp/untangleBackup.backup', post_data='type=backup', uri="http://localhost/admin/download"), shell=True)
        subprocess.call("mkdir /tmp/untangleBackup", shell=True)
        subprocess.call("tar -xf /tmp/untangleBackup.backup -C /tmp/untangleBackup", shell=True)
        subprocess.call("tar -xf "+glob.glob("/tmp/untangleBackup/files*.tar.gz")[0] + " -C /tmp/untangleBackup", shell=True)
        print(result)
        if os.path.isfile(full_path):
            assert True
        else:
            assert False
    
    def test_010_0000_rule_condition_src_addr(self):
        "test SRC_ADDR"
        self.rule_add("SRC_ADDR",remote_control.client_ip)
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_src_addr_any(self):
        "test SRC_ADDR any"
        self.rule_add("SRC_ADDR","any")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_src_addr_inverse(self):
        "test SRC_ADDR non match"
        self.rule_add("SRC_ADDR","1.2.3.4")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_dst_addr(self):
        "test DST_ADDR"
        self.rule_add("DST_ADDR",socket.gethostbyname("test.untangle.com"))
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_dst_addr_any(self):
        "test DST_ADDR any"
        self.rule_add("DST_ADDR","any")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_dst_addr_inverse(self):
        "test DST_ADDR non match"
        self.rule_add("DST_ADDR","1.2.3.4")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_src_port(self):
        "test SRC_PORT"
        self.rule_add("SRC_PORT","0-70000")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_src_port_any(self):
        "test SRC_PORT any"
        self.rule_add("SRC_PORT","any")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_src_port_inverse(self):
        "test SRC_PORT non match"
        self.rule_add("SRC_PORT","<1")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_dst_port(self):
        "test DST_PORT"
        self.rule_add("DST_PORT","80")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_dst_port_any(self):
        "test DST_PORT any"
        self.rule_add("DST_PORT","any")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_dst_port_inverse(self):
        "test DST_PORT non match"
        self.rule_add("DST_PORT","79")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_src_intf(self):
        "test SRC_INTF"
        self.rule_add("SRC_INTF",remote_control.interface)
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_src_intf_any(self):
        "test SRC_INTF any"
        self.rule_add("SRC_INTF","any")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_src_intf_inverse(self):
        "test SRC_INTF non match"
        self.rule_add("SRC_INTF","123")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()
        assert (result == 0)

    @pytest.mark.failure_in_podman
    def test_010_0000_rule_condition_dst_intf(self):
        "test DST_INTF"
        # check if a multi-wan box.
        indexOfWans = global_functions.get_wan_tuples()
        if (len(indexOfWans) < 2):
            self.rule_add("DST_INTF",remote_control.interface_external)
        else:
            for wanIndexTup in indexOfWans:
                wanIndex = wanIndexTup[0]
                self.rule_add("DST_INTF",wanIndex)
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_dst_intf_any(self):
        "test DST_INTF any"
        self.rule_add("DST_INTF","any")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_dst_intf_wan(self):
        "test DST_INTF wan"
        self.rule_add("DST_INTF","wan")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_dst_intf_non_wan(self):
        "test DST_INTF non-wan"
        self.rule_add("DST_INTF","non_wan")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_dst_intf_inverse(self):
        "test DST_INTF non match"
        self.rule_add("DST_INTF","123")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_protocol(self):
        "test PROTOCOL"
        self.rule_add("PROTOCOL","tcp")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_protocol_any(self):
        "test PROTOCOL any"
        self.rule_add("PROTOCOL","any")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_protocol_inverse(self):
        "test PROTOCOL non match"
        self.rule_add("PROTOCOL","udp")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_username(self):
        "test USERNAME"
        username = remote_control.run_command("hostname -s", stdout=True)
        global_functions.host_username_set( username )
        self.rule_add("USERNAME",username)
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.rules_clear()
        global_functions.host_username_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_username_any(self):
        "test USERNAME any"
        global_functions.host_username_set( remote_control.get_hostname() )
        self.rule_add("USERNAME","*")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.rules_clear()
        global_functions.host_username_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_username_any_null(self):
        "test USERNAME * does not match null (no username)"
        global_functions.host_username_clear()
        self.rule_add("USERNAME","*")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()
        assert (result == 0)
        
    def test_010_0000_rule_condition_username_inverse(self):
        "test USERNAME non match"
        self.rule_add("USERNAME","xyz")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_protocol(self):
        "test PROTOCOL"
        self.rule_add("PROTOCOL","tcp")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_protocol_any(self):
        "test PROTOCOL any"
        self.rule_add("PROTOCOL","any")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_protocol_inverse(self):
        "test PROTOCOL non match"
        self.rule_add("PROTOCOL","udp")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_tagged(self):
        "test TAGGED"
        global_functions.host_tags_add("foobar")
        self.rule_add("TAGGED","foobar")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.rules_clear()
        global_functions.host_tags_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_tagged_any(self):
        "test TAGGED any"
        global_functions.host_tags_add("foobar")
        self.rule_add("TAGGED","foo*")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.rules_clear()
        global_functions.host_tags_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_tagged_inverse(self):
        "test TAGGED non match"
        self.rule_add("TAGGED","xyznevermatch")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_host_tagged(self):
        "test HOST_TAGGED"
        global_functions.host_tags_add("foobar")
        self.rule_add("HOST_TAGGED","foobar")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.rules_clear()
        global_functions.host_tags_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_client_tagged(self):
        "test CLIENT_TAGGED"
        global_functions.host_tags_add("foobar")
        self.rule_add("CLIENT_TAGGED","foobar")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.rules_clear()
        global_functions.host_tags_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_server_tagged(self):
        "test SERVER_TAGGED"
        global_functions.host_tags_add("foobar")
        self.rule_add("SERVER_TAGGED","foobar")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()
        global_functions.host_tags_clear()
        assert (result == 0)
        
    def test_010_0000_rule_condition_host_hostname(self):
        "test HOST_HOSTNAME"
        global_functions.host_hostname_set( remote_control.get_hostname() )
        self.rule_add("HOST_HOSTNAME",remote_control.get_hostname())
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_host_hostname_any(self):
        "test HOST_HOSTNAME any"
        global_functions.host_hostname_set( remote_control.get_hostname() )
        self.rule_add("HOST_HOSTNAME","*")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_host_hostname_inverse(self):
        "test HOST_HOSTNAME non match"
        self.rule_add("HOST_HOSTNAME","xyznevermatch")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_client_hostname(self):
        "test CLIENT_HOSTNAME"
        global_functions.host_hostname_set( remote_control.get_hostname() )
        self.rule_add("CLIENT_HOSTNAME",remote_control.get_hostname())
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_client_hostname_any(self):
        "test CLIENT_HOSTNAME any"
        global_functions.host_hostname_set( remote_control.get_hostname() )
        self.rule_add("CLIENT_HOSTNAME","*")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_client_hostname_inverse(self):
        "test CLIENT_HOSTNAME non match"
        self.rule_add("CLIENT_HOSTNAME","xyznevermatch")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_server_hostname_any(self):
        "test SERVER_HOSTNAME * should not match null"
        self.rule_add("SERVER_HOSTNAME","*")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_server_hostname_inverse(self):
        "test SERVER_HOSTNAME non match"
        self.rule_add("SERVER_HOSTNAME","xyznevermatch")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_host_in_penalty_box(self):
        "test HOST_IN_PENALTY_BOX"
        global_functions.host_tags_add("penalty-box")
        self.rule_add("HOST_IN_PENALTY_BOX",None)
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.rules_clear()
        global_functions.host_tags_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_host_in_penalty_box_inverse(self):
        "test HOST_IN_PENALTY_BOX non match"
        global_functions.host_tags_clear()
        self.rule_add("HOST_IN_PENALTY_BOX",None)
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_client_in_penalty_box(self):
        "test CLIENT_IN_PENALTY_BOX"
        global_functions.host_tags_add("penalty-box")
        self.rule_add("CLIENT_IN_PENALTY_BOX",None)
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.rules_clear()
        global_functions.host_tags_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_client_in_penalty_box_inverse(self):
        "test CLIENT_IN_PENALTY_BOX non match"
        global_functions.host_tags_clear()
        self.rule_add("CLIENT_IN_PENALTY_BOX",None)
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_server_in_penalty_box_inverse(self):
        "test SERVER_IN_PENALTY_BOX non match"
        self.rule_add("SERVER_IN_PENALTY_BOX",None)
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()
        assert (result == 0)
        
    def test_010_0000_rule_condition_host_has_no_quota(self):
        "test HOST_HAS_NO_QUOTA"
        global_functions.host_quota_clear()
        self.rule_add("HOST_HAS_NO_QUOTA",remote_control.get_hostname())
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_host_has_no_quota_inverse(self):
        "test HOST_HAS_NO_QUOTA non match"
        global_functions.host_quota_give( 1000000, 60 )
        self.rule_add("HOST_HAS_NO_QUOTA",None)
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()
        global_functions.host_quota_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_user_has_no_quota(self):
        "test USER_HAS_NO_QUOTA"
        global_functions.host_username_set( remote_control.get_hostname() )
        global_functions.user_quota_clear( remote_control.get_hostname() )
        self.rule_add("USER_HAS_NO_QUOTA",remote_control.get_hostname())
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.rules_clear()
        global_functions.host_username_clear()
        global_functions.user_quota_clear( remote_control.get_hostname() )
        assert (result == 0)

    def test_010_0000_rule_condition_user_has_no_quota_inverse(self):
        "test USER_HAS_NO_QUOTA non match"
        global_functions.host_username_set( remote_control.get_hostname() )
        global_functions.user_quota_give( remote_control.get_hostname(), 100000, 60 )
        self.rule_add("USER_HAS_NO_QUOTA",None)
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()
        global_functions.host_username_clear()
        global_functions.user_quota_clear( remote_control.get_hostname() )
        assert (result == 0)

    def test_010_0000_rule_condition_user_has_no_quota_no_user(self):
        "test USER_HAS_NO_QUOTA non match"
        global_functions.host_username_clear()
        self.rule_add("USER_HAS_NO_QUOTA",None)
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()
        assert (result == 0)
        
    def test_010_0000_rule_condition_client_has_no_quota(self):
        "test CLIENT_HAS_NO_QUOTA"
        global_functions.host_quota_clear()
        self.rule_add("CLIENT_HAS_NO_QUOTA",remote_control.get_hostname())
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_client_has_no_quota_inverse(self):
        "test CLIENT_HAS_NO_QUOTA non match"
        global_functions.host_quota_give( 1000000, 60 )
        self.rule_add("CLIENT_HAS_NO_QUOTA",None)
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()
        global_functions.host_quota_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_server_has_no_quota(self):
        "test SERVER_HAS_NO_QUOTA match"
        global_functions.host_quota_give( 1000000, 60 )
        self.rule_add("SERVER_HAS_NO_QUOTA",None)
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.rules_clear()
        global_functions.host_quota_clear()
        assert (result == 0)
        
    def test_010_0000_rule_condition_host_quota_exceeded(self):
        "test HOST_QUOTA_EXCEEDED"
        global_functions.host_quota_give( 1000, 300 )
        global_functions.uvmContext.hostTable().decrementQuota( remote_control.client_ip, 10000 )
        self.rule_add("HOST_QUOTA_EXCEEDED",None)
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.rules_clear()
        global_functions.host_quota_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_host_quota_exceeded_inverse(self):
        "test HOST_QUOTA_EXCEEDED non match"
        global_functions.host_quota_clear()
        self.rule_add("HOST_QUOTA_EXCEEDED",None)
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_client_quota_exceeded(self):
        "test CLIENT_QUOTA_EXCEEDED"
        global_functions.host_quota_give( 1000, 300 )
        global_functions.uvmContext.hostTable().decrementQuota( remote_control.client_ip, 10000 )
        self.rule_add("CLIENT_QUOTA_EXCEEDED",None)
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.rules_clear()
        global_functions.host_quota_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_client_quota_exceeded_inverse(self):
        "test CLIENT_QUOTA_EXCEEDED non match"
        global_functions.host_quota_clear()
        self.rule_add("CLIENT_QUOTA_EXCEEDED",None)
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_server_quota_exceeded_inverse(self):
        "test SERVER_QUOTA_EXCEEDED non match"
        self.rule_add("SERVER_QUOTA_EXCEEDED",None)
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_user_quota_exceeded(self):
        "test USER_QUOTA_EXCEEDED"
        global_functions.host_username_set( remote_control.get_hostname() )
        global_functions.user_quota_give( remote_control.get_hostname(), 1000, 300 )
        global_functions.uvmContext.userTable().decrementQuota( remote_control.get_hostname(), 10000 )
        self.rule_add("USER_QUOTA_EXCEEDED",None)
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.rules_clear()
        global_functions.user_quota_clear(remote_control.get_hostname())
        global_functions.host_username_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_user_quota_exceeded_inverse(self):
        "test USER_QUOTA_EXCEEDED non match"
        global_functions.host_username_set( remote_control.get_hostname() )
        global_functions.user_quota_give( remote_control.get_hostname(), 100000000, 300 )
        self.rule_add("USER_QUOTA_EXCEEDED",None)
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()
        global_functions.user_quota_clear(remote_control.get_hostname())
        global_functions.host_username_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_user_quota_exceeded_no_quota(self):
        "test USER_QUOTA_EXCEEDED non match"
        global_functions.host_username_set( remote_control.get_hostname() )
        global_functions.user_quota_clear( remote_control.get_hostname() )
        self.rule_add("USER_QUOTA_EXCEEDED",None)
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_user_quota_exceeded_no_user(self):
        "test USER_QUOTA_EXCEEDED non match"
        global_functions.host_username_clear()
        self.rule_add("USER_QUOTA_EXCEEDED",None)
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_host_quota_attainment(self):
        "test HOST_QUOTA_ATTAINMENT"
        global_functions.host_quota_give( 1000, 300 )
        global_functions.uvmContext.hostTable().decrementQuota( remote_control.client_ip, 10000 )
        self.rule_add("HOST_QUOTA_ATTAINMENT",">1.1")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        assert (result == 0)
        self.rules_clear()
        global_functions.host_quota_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_host_quota_attainment_inverse(self):
        "test HOST_QUOTA_ATTAINMENT non match"
        global_functions.host_quota_clear()
        self.rule_add("HOST_QUOTA_ATTAINMENT",">.1")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_client_quota_attainment(self):
        "test CLIENT_QUOTA_ATTAINMENT"
        global_functions.host_quota_give( 1000, 300 )
        global_functions.uvmContext.hostTable().decrementQuota( remote_control.client_ip, 10000 )
        self.rule_add("CLIENT_QUOTA_ATTAINMENT",">1.1")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.rules_clear()
        global_functions.host_quota_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_client_quota_attainment_inverse(self):
        "test CLIENT_QUOTA_ATTAINMENT non match"
        global_functions.host_quota_clear()
        self.rule_add("CLIENT_QUOTA_ATTAINMENT",">.1")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_server_quota_attainment_inverse(self):
        "test SERVER_QUOTA_ATTAINMENT non match"
        self.rule_add("SERVER_QUOTA_ATTAINMENT",">.1")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_user_quota_attainment(self):
        "test USER_QUOTA_ATTAINMENT"
        global_functions.host_username_set( remote_control.get_hostname() )
        global_functions.user_quota_give( remote_control.get_hostname(), 1000, 300 )
        global_functions.uvmContext.userTable().decrementQuota( remote_control.get_hostname(), 10000 )
        self.rule_add("USER_QUOTA_ATTAINMENT",">1.1")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.rules_clear()
        global_functions.user_quota_clear(remote_control.get_hostname())
        global_functions.host_username_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_user_quota_attainment_inverse(self):
        "test USER_QUOTA_ATTAINMENT non match"
        global_functions.host_username_set( remote_control.get_hostname() )
        global_functions.user_quota_give( remote_control.get_hostname(), 100000000, 300 )
        self.rule_add("USER_QUOTA_ATTAINMENT",">.5")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()
        global_functions.user_quota_clear(remote_control.get_hostname())
        global_functions.host_username_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_user_quota_attainment_no_quota(self):
        "test USER_QUOTA_ATTAINMENT non match"
        global_functions.host_username_set( remote_control.get_hostname() )
        global_functions.user_quota_clear( remote_control.get_hostname() )
        self.rule_add("USER_QUOTA_ATTAINMENT",">.1")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_user_quota_attainment_no_user(self):
        "test USER_QUOTA_ATTAINMENT non match"
        global_functions.host_username_clear()
        self.rule_add("USER_QUOTA_ATTAINMENT",">.1")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_host_mac_vendor(self):
        "test HOST_MAC_VENDOR"
        entry = global_functions.uvmContext.hostTable().getHostTableEntry( remote_control.client_ip )
        vendor = entry.get('macVendor')
        if vendor == None:
            raise unittest.SkipTest('No MAC vendor')
        vendor = "*" + vendor.split(None,1)[0] + "*"
        self.rule_add("HOST_MAC_VENDOR",vendor)
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_host_mac_vendor_inverse(self):
        "test HOST_MAC_VENDOR inverse"
        self.rule_add("HOST_MAC_VENDOR","xyznevermatch")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_client_mac_vendor(self):
        "test CLIENT_MAC_VENDOR"
        entry = global_functions.uvmContext.hostTable().getHostTableEntry( remote_control.client_ip )
        vendor = entry.get('macVendor')
        if vendor == None:
            raise unittest.SkipTest('No MAC vendor')
        vendor = "*" + vendor.split(None,1)[0] + "*"
        self.rule_add("CLIENT_MAC_VENDOR",vendor)
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_client_mac_vendor_inverse(self):
        "test CLIENT_MAC_VENDOR inverse"
        self.rule_add("CLIENT_MAC_VENDOR","xyznevermatch")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_server_mac_vendor_inverse(self):
        "test SERVER_MAC_VENDOR inverse"
        self.rule_add("SERVER_MAC_VENDOR","xyznevermatch")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_server_mac_vendor_no_entry(self):
        "test SERVER_MAC_VENDOR if no host entry exists - * should not match null"
        entry = global_functions.uvmContext.hostTable().getHostTableEntry( socket.gethostbyname("test.untangle.com") )
        if entry != None:
            raise unittest.SkipTest('Entry exists')
        self.rule_add("SERVER_MAC_VENDOR",'*')
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_host_mac_vendor(self):
        "test HOST_MAC_VENDOR"
        entry = global_functions.uvmContext.hostTable().getHostTableEntry( remote_control.client_ip )
        vendor = entry.get('macVendor')
        if vendor == None:
            raise unittest.SkipTest('No MAC vendor')
        vendor = "*" + vendor.split(None,1)[0] + "*"
        self.rule_add("HOST_MAC_VENDOR",vendor)
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_host_mac_vendor_inverse(self):
        "test HOST_MAC_VENDOR inverse"
        self.rule_add("HOST_MAC_VENDOR","xyznevermatch")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_client_mac_vendor(self):
        "test CLIENT_MAC_VENDOR"
        entry = global_functions.uvmContext.hostTable().getHostTableEntry( remote_control.client_ip )
        vendor = entry.get('macVendor')
        if vendor == None:
            raise unittest.SkipTest('No MAC vendor')
        vendor = "*" + vendor.split(None,1)[0] + "*"
        self.rule_add("CLIENT_MAC_VENDOR",vendor)
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_client_mac_vendor_inverse(self):
        "test CLIENT_MAC_VENDOR inverse"
        self.rule_add("CLIENT_MAC_VENDOR","xyznevermatch")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_server_mac_vendor_inverse(self):
        "test SERVER_MAC_VENDOR inverse"
        self.rule_add("SERVER_MAC_VENDOR","xyznevermatch")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_server_mac_vendor_no_entry(self):
        "test SERVER_MAC_VENDOR if no host entry exists - * should not match null"
        entry = global_functions.uvmContext.hostTable().getHostTableEntry( socket.gethostbyname("test.untangle.com") )
        if entry != None:
            raise unittest.SkipTest('Entry exists')
        self.rule_add("SERVER_MAC_VENDOR",'*')
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_host_mac(self):
        "test HOST_MAC"
        entry = global_functions.uvmContext.hostTable().getHostTableEntry( remote_control.client_ip )
        mac = entry.get('macAddress')
        if mac == None:
            raise unittest.SkipTest('No MAC address')
        self.rule_add("HOST_MAC",mac)
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_host_mac_inverse(self):
        "test HOST_MAC inverse"
        self.rule_add("HOST_MAC","xy:xy:xy:xy:xy:xy")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_src_mac(self):
        "test SRC_MAC"
        entry = global_functions.uvmContext.hostTable().getHostTableEntry( remote_control.client_ip )
        mac = entry.get('macAddress')
        if mac == None:
            raise unittest.SkipTest('No MAC address')
        self.rule_add("SRC_MAC",mac)
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_src_mac_inverse(self):
        "test SRC_MAC inverse"
        self.rule_add("SRC_MAC","xy:xy:xy:xy:xy:xy")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_dst_mac_inverse(self):
        "test DST_MAC inverse"
        self.rule_add("DST_MAC","xy:xy:xy:xy:xy:xy")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_dst_mac_no_entry(self):
        "test DST_MAC if no host entry exists - * should not match null"
        entry = global_functions.uvmContext.hostTable().getHostTableEntry( socket.gethostbyname("test.untangle.com") )
        if entry != None:
            raise unittest.SkipTest('Entry exists')
        self.rule_add("DST_MAC",'*')
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()
        assert (result == 0)
        
    def test_010_0000_rule_condition_day_of_week_number(self):
        "test DAY_OF_WEEK by day number"
        daynum = datetime.date.today().weekday() # 0 - monday 6 - sunday
        daynum = ((daynum + 1) % 7) + 1 # 1 - sunday, 7 - saturday
        self.rule_add("DAY_OF_WEEK",str(daynum))
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_day_of_week_name(self):
        "test DAY_OF_WEEK by day name"
        dayname = (calendar.day_name[datetime.date.today().weekday()]).lower()
        self.rule_add("DAY_OF_WEEK",dayname)
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_day_of_week_list(self):
        "test DAY_OF_WEEK in a list"
        dayname = (calendar.day_name[datetime.date.today().weekday()]).lower()
        print(dayname)
        self.rule_add("DAY_OF_WEEK","xyz," + dayname + ",zyx")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.rules_clear()
        assert (result == 0)
        
    def test_010_0000_rule_condition_day_of_week_inverse(self):
        "test DAY_OF_WEEK inverse"
        dayname = (calendar.day_name[datetime.date.today().weekday()]).lower()
        self.rule_add("DAY_OF_WEEK","xyznevermatch")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()
        assert (result == 0)
        
    def test_010_0000_rule_condition_time_of_day(self):
        "test TIME_OF_DAY"
        start_time = (datetime.datetime.today() - datetime.timedelta(hours=1)).strftime('%H:%M')
        stop_time = (datetime.datetime.today() + datetime.timedelta(hours=1)).strftime('%H:%M')
        self.rule_add("TIME_OF_DAY",start_time+"-"+stop_time)
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_time_of_day_inverse(self):
        "test TIME_OF_DAY inverse"
        start_time = (datetime.datetime.today() + datetime.timedelta(minutes=5)).strftime('%H:%M')
        stop_time = (datetime.datetime.today() + datetime.timedelta(minutes=6)).strftime('%H:%M')
        self.rule_add("TIME_OF_DAY",start_time+"-"+stop_time)
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_http_host(self):
        "test HTTP_HOST"
        self.rule_add("HTTP_HOST","test.untangle.com")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_http_host_glob(self):
        "test HTTP_HOST glob"
        self.rule_add("HTTP_HOST","*untangle*")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_http_host_inverse(self):
        "test HTTP_HOST"
        self.rule_add("HTTP_HOST","xyznevermatch")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_http_referer(self):
        "test HTTP_REFERER"
        self.rule_add("HTTP_REFERER","*untangle*")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage", extra_options="--header 'Referer: http://test.untangle.com/test/testPage1.html'")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_http_referer_inverse(self):
        "test HTTP_REFERER inverse (no referer so it should not be blocked)"
        self.rule_add("HTTP_REFERER","*untangle*")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_http_uri(self):
        "test HTTP_URI"
        self.rule_add("HTTP_URI","*test*")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage", extra_options="--header 'Uri: http://test.untangle.com/test/testPage1.html'")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_http_uri_search(self):
        "test HTTP_URI"
        self.rule_add("HTTP_URI","*search*")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html?q=searchteam", expected="blockpage", extra_options="--header 'Uri: http://test.untangle.com/test/testPage1.html'")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_http_uri_inverse(self):
        "test HTTP_URI inverse (untangle string is not in URI)"
        self.rule_add("HTTP_URI","*untangle*")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_http_url(self):
        "test HTTP_URL"
        self.rule_add("HTTP_URL","*test*")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_http_url_includes_domain(self):
        "test HTTP_URL includes domain"
        self.rule_add("HTTP_URL","*untangle*")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_http_url_domain_plus_uri(self):
        "test HTTP_URL includes domain"
        self.rule_add("HTTP_URL","*com/test*")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_http_url_inverse(self):
        "test HTTP_URL inverse"
        self.rule_add("HTTP_URL","*xyznevermatch*")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_http_content_type(self):
        "test HTTP_CONTENT_TYPE"
        self.rule_add("HTTP_CONTENT_TYPE","*text/html*")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_http_content_type_inverse(self):
        "test HTTP_CONTENT_TYPE"
        self.rule_add("HTTP_CONTENT_TYPE","*xyznevermatch*")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_http_content_length(self):
        "test HTTP_CONTENT_LENGTH"
        self.rule_add("HTTP_CONTENT_LENGTH",">1")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.rules_clear()
        assert (result == 0)
        
    def test_010_0000_rule_condition_http_content_length_inverse(self):
        "test HTTP_CONTENT_LENGTH"
        self.rule_add("HTTP_CONTENT_LENGTH",">100000000")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_http_user_agent(self):
        "test HTTP_USER_AGENT"
        entry = global_functions.uvmContext.hostTable().getHostTableEntry( remote_control.client_ip )
        if entry.get('httpUserAgent') == None or not('linux' in entry.get('httpUserAgent')):
            raise unittest.SkipTest('No usable user agent')
        self.rule_add("HTTP_USER_AGENT","*linux*")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_http_user_agent_inverse(self):
        "test HTTP_USER_AGENT inverse"
        self.rule_add("HTTP_USER_AGENT","*xyznevermatch*")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_web_filter_category(self):
        "test WEB_FILTER_CATEGORY"
        self.rule_add("WEB_FILTER_CATEGORY","Pornography")
        result = self.get_web_request_results(url="http://pornhub.com", expected="blockpage")
        self.rules_clear()
        assert (result == 0)
        
    def test_010_0000_rule_condition_web_filter_category_inverse(self):
        "test WEB_FILTER_CATEGORY inverse"
        self.rule_add("WEB_FILTER_CATEGORY","xyznevermatch")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()
        assert (result == 0)
        
    def test_010_0000_rule_condition_web_filter_category(self):
        "test WEB_FILTER_CATEGORY"
        raise unittest.SkipTest('Skipping as brightcloud is setting previous category instead of Generative AI ')
        self.rule_add("WEB_FILTER_CATEGORY","Generative AI")
        result = self.get_web_request_results(url="http://chat.openai.com/auth/login", expected="blockpage")
        self.rules_clear()
        assert (result == 0)
        
    def test_010_0000_rule_condition_web_filter_category_inverse(self):
        "test WEB_FILTER_CATEGORY inverse"
        self.rule_add("WEB_FILTER_CATEGORY","xyznevermatch")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_web_filter_category(self):
        "test WEB_FILTER_CATEGORY_DESCRIPTION"
        self.rule_add("WEB_FILTER_CATEGORY_DESCRIPTION","*sexual*")
        result = self.get_web_request_results(url="http://pornhub.com", expected="blockpage")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_web_filter_category_inverse(self):
        "test WEB_FILTER_CATEGORY_DESCRIPTION"
        self.rule_add("WEB_FILTER_CATEGORY_DESCRIPTION","xyznevermatch")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()

    def test_010_0000_rule_condition_web_filter_request_method(self):
        "test WEB_FILTER_REQUEST_METHOD"
        self.rule_add("WEB_FILTER_REQUEST_METHOD","GET")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_web_filter_request_method_inverse(self):
        "test WEB_FILTER_REQUEST_METHOD inverse"
        self.rule_add("WEB_FILTER_REQUEST_METHOD","OPTIONS")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_web_filter_request_file_path(self):
        "test WEB_FILTER_REQUEST_FILE_PATH"
        self.rule_add("WEB_FILTER_REQUEST_FILE_PATH","*test*")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_web_filter_request_file_path_inverse(self):
        "test WEB_FILTER_REQUEST_FILE_PATH inverse"
        self.rule_add("WEB_FILTER_REQUEST_FILE_PATH","*untangle*")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_web_filter_request_file_name(self):
        "test WEB_FILTER_REQUEST_FILE_NAME"
        self.rule_add("WEB_FILTER_REQUEST_FILE_NAME","*testPage*")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_web_filter_request_file_name_inverse(self):
        "test WEB_FILTER_REQUEST_FILE_NAME inverse"
        self.rule_add("WEB_FILTER_REQUEST_FILE_NAME","*test/*")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_web_filter_request_file_extension(self):
        "test WEB_FILTER_REQUEST_FILE_EXTENSION"
        self.rule_add("WEB_FILTER_REQUEST_FILE_EXTENSION","html")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_web_filter_request_file_extension_inverse(self):
        "test WEB_FILTER_REQUEST_FILE_EXTENSION inverse"
        self.rule_add("WEB_FILTER_REQUEST_FILE_EXTENSION","*testPage*")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_web_filter_response_content_type(self):
        "test WEB_FILTER_RESPONSE_CONTENT_TYPE"
        self.rule_add("WEB_FILTER_RESPONSE_CONTENT_TYPE","*html*")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_web_filter_response_content_type_inverse(self):
        "test WEB_FILTER_RESPONSE_CONTENT_TYPE inverse"
        self.rule_add("WEB_FILTER_RESPONSE_CONTENT_TYPE","*pdf*")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_web_filter_response_file_name(self):
        "test WEB_FILTER_RESPONSE_FILE_NAME"
        self.rule_add("WEB_FILTER_RESPONSE_FILE_NAME","*zip*")
        result = self.get_web_request_results(url="http://test.untangle.com/download.php?file=5MB.zip", expected="blockpage")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_web_filter_response_file_name_inverse(self):
        "test WEB_FILTER_RESPONSE_FILE_NAME inverse"
        self.rule_add("WEB_FILTER_RESPONSE_FILE_NAME","*test/*")
        result = self.get_web_request_results(url="http://test.untangle.com/download.php?file=testPage1.html", expected="text123")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_web_filter_response_file_extension(self):
        "test WEB_FILTER_RESPONSE_FILE_EXTENSION"
        self.rule_add("WEB_FILTER_RESPONSE_FILE_EXTENSION","zip")
        result = self.get_web_request_results(url="http://test.untangle.com/download.php?file=5MB.zip", expected="blockpage")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_web_filter_response_file_extension_inverse(self):
        "test WEB_FILTER_RESPONSE_FILE_EXTENSION inverse"
        self.rule_add("WEB_FILTER_RESPONSE_FILE_EXTENSION","zip")
        result = self.get_web_request_results(url="http://test.untangle.com/download.php?file=testPage1.html", expected="text123")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_web_filter_category(self):
        """ Flag option should not be required for Web Filter Pass Rules to take effect"""
        original_settings = self._app.getSettings()
        host = "www.facebook.com"
        # Make copy of settings so we can modify enable set category as block and flagged
        settings = copy.deepcopy(original_settings)
        for category in settings.get("categories").get("list"):
            if category.get("name") == "Social Networking" :
                category['blocked'] = True
                category['flagged'] = True
        self._app.setSettings(settings)
        # Add a rule to pass the above category config , with Blocked as False and Flagged as False
        self.rule_add("WEB_FILTER_CATEGORY","Social Networking", False, False)
        result = self.get_web_request_results(url="http://www.facebook.com/", expected="blockpage")
        assert (result != 0)
        events = global_functions.get_events(self.displayName(),'All Web Events',None,5)
        # Verify the Webfilter rule passes the Social Networking category access and  event is logged
        found = global_functions.check_events( events.get('list'), 5,
                                            "host", host,
                                            "uri", "/",
                                            'web_filter_blocked', False,
                                            'web_filter_flagged', False)

        assert(found)
        # setting back original settings and deleting the previous added ru;e
        self._app.setSettings(original_settings)
        self.rules_clear()

    @classmethod
    def final_extra_tear_down(cls):
        global web_app_2, policy_app, secondRackId, interface, primary_ip, thirdRackId, web_app_3
        if interface and primary_ip:
                # Remove secondary IP from the remote server
                global_functions.remove_secondary_ip(interface, primary_ip)
        if web_app_3 != None:
            global_functions.uvmContext.appManager().destroy(web_app_3.getAppSettings()["id"])
        # Remove all other web instances, policy racks and rules
        if web_app_2 != None and policy_app != None:
            global_functions.uvmContext.appManager().destroy(web_app_2.getAppSettings()["id"])
            global_functions.nukeRules(policy_app)
            global_functions.removeRack(policy_app, secondRackId)
            global_functions.removeRack(policy_app, thirdRackId)
            global_functions.uvmContext.appManager().destroy(policy_app.getAppSettings()["id"])
            web_app_2 = None
            policy_app = None


test_registry.register_module("web-filter", WebFilterTests)
