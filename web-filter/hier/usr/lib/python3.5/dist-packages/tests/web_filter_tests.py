"""web_filter tests"""
import time
import sys
import re
import datetime
import calendar
import socket
import subprocess

import unittest
from tests.global_functions import uvmContext
import runtests.remote_control as remote_control
import runtests.test_registry as test_registry
import tests.global_functions as global_functions
import tests.ipaddr as ipaddr
from uvm import Uvm

from tests.web_filter_base_tests import WebFilterBaseTests

default_policy_id = 1
app = None

def setHttpHttpsPorts(httpPort, httpsPort):
    netsettings = uvmContext.networkManager().getNetworkSettings()
    netsettings['httpPort'] = httpPort
    netsettings['httpsPort'] = httpsPort
    uvmContext.networkManager().setNetworkSettings(netsettings)

#
# Just extends the web filter base tests
#
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
    def initial_setup(self):
        global app
        if (uvmContext.appManager().isInstantiated(self.module_name())):
            raise Exception('app %s already instantiated' % self.module_name())
        app = uvmContext.appManager().instantiate(self.module_name(), default_policy_id)
        appmetrics = uvmContext.metricManager().getMetrics(app.getAppSettings()["id"])
        self.app = app

    def test_016_block_url(self):
        """verify basic URL blocking the the url block list"""
        pre_events_scan = global_functions.get_app_metric_value(self.app, "scan")
        pre_events_block = global_functions.get_app_metric_value(self.app, "block")
        self.block_url_list_add("test.untangle.com/test/testPage1.html")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.block_url_list_clear()
        assert ( result == 0 )
        # verify the faceplate counters have incremented.
        post_events_scan = global_functions.get_app_metric_value(self.app, "scan")
        post_events_block = global_functions.get_app_metric_value(self.app, "block")
        assert(pre_events_scan < post_events_scan)
        assert(pre_events_block < post_events_block)

    def test_019_porn_is_blocked_alt_port(self):
        setHttpHttpsPorts(8081,443)
        result = self.get_web_request_results(url="http://playboy.com/", expected="blockpage")
        setHttpHttpsPorts(80,443)
        assert (result == 0)
        found = self.check_events("playboy.com", "/", True)
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
        result1 = remote_control.run_command("wget -q -O - http://test.untangle.com/test/testPage1.html?arg=%s 2>&1 >/dev/null" % fname)
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
        settings = self.app.getSettings()
        settings["passReferers"] = False
        self.app.setSettings(settings)
        resultReferer = remote_control.run_command("wget -q --header 'Referer: http://test.untangle.com/test/testPage1.html' -O - http://test.untangle.com/test/refererPage.html 2>&1 | grep -q 'Welcome to the referer page.'");
        print("result %s passReferers %s" % (resultReferer,settings["passReferers"]))

        self.block_url_list_clear()
        self.pass_url_list_clear()
        assert( resultReferer == 1 )

    def test_201_pass_referer_enabled(self):
        """disable pass referer and verify that a page with content that would be blocked is allowed."""
        self.block_url_list_add("test.untangle.com/test/refererPage.html")
        self.pass_url_list_add("test.untangle.com/test/testPage1.html")
        settings = self.app.getSettings()
        settings["passReferers"] = True
        self.app.setSettings(settings)
        resultReferer = remote_control.run_command("wget -q --header 'Referer: http://test.untangle.com/test/testPage1.html' -O - http://test.untangle.com/test/refererPage.html 2>&1 | grep -q 'Welcome to the referer page.'");
        print("result %s passReferers %s" % (resultReferer,settings["passReferers"]))

        self.block_url_list_clear()
        self.pass_url_list_clear()
        assert( resultReferer == 0 )

    def test_300_block_ip_only_hosts(self):
        """Enable 'block IP only hosts', then check that traffic to an IP is blocked"""
        settings = self.app.getSettings()
        settings["blockAllIpHosts"]=True
        self.app.setSettings(settings)
        result = self.get_web_request_results(url=global_functions.test_server_ip, expected="blockpage")
        assert(result == 0)

    def test_301_block_QUIC(self):
        """Enable 'block QUIC (UDP port 443)' setting then check that UDP traffic over 443 is blocked (using netcat server/client)"""
        #check for passwordless sudo access for the host first, if not, skip test
        if(remote_control.run_command("sudo ls -l",stdout=False,nowait=True) != 0):
            raise unittest.SkipTest('no passwordless sudo access')
        ping_result = subprocess.call(["ping","-c","1",global_functions.LIST_SYSLOG_SERVER ],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        if (ping_result != 0):
            raise unittest.SkipTest("server not available")

        #set block to false first to verify netcat works
        settings = self.app.getSettings()
        settings["blockQuic"]=False
        self.app.setSettings(settings)

        serverHost = global_functions.LIST_SYSLOG_SERVER

        #set up netcat server/client connection and check that it works first
        remote_control.run_command("sudo pkill nc",host=serverHost) # kill previous running nc
        remote_control.run_command("sudo rm -f /tmp/nc_quic_false.txt",host=serverHost)
        remote_control.run_command("sudo nc -l -u -w 2 %s 443 >/tmp/nc_quic_false.txt" % serverHost,host=serverHost,stdout=False,nowait=True)
        remote_control.run_command("echo TEST | nc -u -w 1 %s 443 | sleep 2" % serverHost)
        first_result =remote_control.run_command("grep TEST /tmp/nc_quic_false.txt",host=serverHost)
        assert(first_result == 0)

        #set block to true
        settings["blockQuic"]=True
        self.app.setSettings(settings)

        #retry netcat connection, verify it fails correctly
        remote_control.run_command("sudo rm -f /tmp/nc_quic_true.txt",host=serverHost)
        remote_control.run_command("sudo nc -l -u -w 2 %s 443 >/tmp/nc_quic_true.txt" % serverHost,host=serverHost,stdout=False,nowait=True)
        remote_control.run_command("echo TEST | sudo nc -u -w 1 %s 443 | sleep 2" % serverHost)
        second_result =remote_control.run_command("grep TEST /tmp/nc_quic_true.txt",host=serverHost)
        assert(second_result != 0)
        remote_control.run_command("sudo pkill nc",host=serverHost) # kill running nc


    def test_700_safe_search_enabled(self):
        """Check google safe search"""
        settings = self.app.getSettings()
        settings["enforceSafeSearch"] = False
        self.app.setSettings(settings)
        result_without_safe = remote_control.run_command("wget -q -O - '$@' -U 'Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.7) Gecko/20040613 Firefox/0.8.0+)' 'http://www.google.com/search?hl=en&q=boobs&safe=off' | grep -q 'safe=off'");

        settings["enforceSafeSearch"] = True
        self.app.setSettings(settings)
        result_with_safe = remote_control.run_command("wget -q -O - '$@' -U 'Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.7) Gecko/20040613 Firefox/0.8.0+)' 'http://www.google.com/search?hl=en&q=boobs&safe=off' | grep -q 'safe=strict'");

        assert( result_without_safe == 0 )
        assert( result_with_safe == 0 )

    def test_701_unblock_option(self):
        """verify that a block page is shown but unblock button option is available."""
        self.block_url_list_add("test.untangle.com/test/testPage1.html")
        settings = self.app.getSettings()
        settings["unblockMode"] = "Host"
        self.app.setSettings(settings)
        # this test URL should be blocked but allow
        remote_control.run_command("rm -f /tmp/web_filter_base_test_120.log")
        result = remote_control.run_command("wget -4 -t 2 --timeout=5 -a /tmp/web_filter_base_test_120.log -O /tmp/web_filter_base_test_120.out http://test.untangle.com/test/testPage1.html")
        resultButton = remote_control.run_command("grep -q 'unblock' /tmp/web_filter_base_test_120.out")
        resultBlock = remote_control.run_command("grep -q 'blockpage' /tmp/web_filter_base_test_120.out")

        # get the IP address of the block page
        ipfind = remote_control.run_command("grep 'Location' /tmp/web_filter_base_test_120.log", stdout=True)
        # print('ipFind %s' % ipfind)
        ip = re.findall( r'[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}(?:[0-9:]{0,6})', ipfind )
        blockPageIP = ip[0]
        # print('Block page IP address is %s' % blockPageIP)
        blockParamaters = re.findall( r'\?(.*)\s', ipfind )
        paramaters = blockParamaters[0]
        # Use unblock button.
        unBlockParameters = "global=false&"+ paramaters + "&password="
        # print("unBlockParameters %s" % unBlockParameters)
        print("wget -q -O /dev/null --post-data=\'" + unBlockParameters + "\' http://" + blockPageIP + "/" + self.shortAppName() + "/unblock")
        remote_control.run_command("wget -q -O /dev/null --post-data=\'" + unBlockParameters + "\' http://" + blockPageIP + "/" + self.shortAppName() + "/unblock")
        resultUnBlock = remote_control.run_command("wget -q -O - http://test.untangle.com/test/testPage1.html 2>&1 | grep -q text123")

        self.block_url_list_clear()
        self.app.flushAllUnblockedSites()

        print("block %s button %s unblock %s" % (resultBlock,resultButton,resultUnBlock))
        assert (resultBlock == 0)
        assert (resultButton == 0)
        assert (resultUnBlock == 0)

    def test_701_unblock_option_with_password(self):
        """verify that a block page is shown but unblock if correct password."""
        fname = sys._getframe().f_code.co_name
        self.block_url_list_add("test.untangle.com/test/testPage2.html")
        settings = self.app.getSettings()
        settings["unblockMode"] = "Host"
        settings["unblockPassword"] = "atstest"
        settings["unblockPasswordEnabled"] = True
        self.app.setSettings(settings)

        # this test URL should be blocked but allow
        remote_control.run_command("rm -f /tmp/%s.log"%fname)
        result = remote_control.run_command("wget -4 -t 2 --timeout=5 -a /tmp/%s.log -O /tmp/%s.out http://test.untangle.com/test/testPage2.html"%(fname,fname))
        resultButton = remote_control.run_command("grep -q 'unblock' /tmp/%s.out"%fname)
        resultBlock = remote_control.run_command("grep -q 'blockpage' /tmp/%s.out"%fname)

        # get the IP address of the block page
        ipfind = remote_control.run_command("grep 'Location' /tmp/%s.log"%fname,stdout=True)
        print('ipFind %s' % ipfind)
        ip = re.findall( r'[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}(?:[0-9:]{0,6})', ipfind )
        blockPageIP = ip[0]
        # print('Block page IP address is %s' % blockPageIP)
        blockParamaters = re.findall( r'\?(.*)\s', ipfind )
        paramaters = blockParamaters[0]
        # Use unblock button.
        unBlockParameters = "global=false&"+ paramaters + "&password=atstest"
        # print("unBlockParameters %s" % unBlockParameters)
        remote_control.run_command("wget -q -O /dev/null --post-data=\'" + unBlockParameters + "\' http://" + blockPageIP + "/" + self.shortAppName() + "/unblock")
        resultUnBlock = remote_control.run_command("wget -O - http://test.untangle.com/test/testPage2.html 2>&1 | grep -q text123")

        settings = self.app.getSettings()
        settings["unblockMode"] = "None"
        settings["unblockPassword"] = ""
        settings["unblockPasswordEnabled"] = False

        self.app.setSettings(settings)
        self.block_url_list_clear()
        print("block %s button %s unblock %s" % (resultBlock,resultButton,resultUnBlock))
        assert (resultBlock == 0 and resultButton == 0 and resultUnBlock == 0 )

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
        uvmContext.hostTable().decrementQuota( remote_control.client_ip, 10000 )
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
        uvmContext.hostTable().decrementQuota( remote_control.client_ip, 10000 )
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
        uvmContext.userTable().decrementQuota( remote_control.get_hostname(), 10000 )
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
        uvmContext.hostTable().decrementQuota( remote_control.client_ip, 10000 )
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
        uvmContext.hostTable().decrementQuota( remote_control.client_ip, 10000 )
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
        uvmContext.userTable().decrementQuota( remote_control.get_hostname(), 10000 )
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
        entry = uvmContext.hostTable().getHostTableEntry( remote_control.client_ip )
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
        entry = uvmContext.hostTable().getHostTableEntry( remote_control.client_ip )
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
        entry = uvmContext.hostTable().getHostTableEntry( socket.gethostbyname("test.untangle.com") )
        if entry != None:
            raise unittest.SkipTest('Entry exists')
        self.rule_add("SERVER_MAC_VENDOR",'*')
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_host_mac_vendor(self):
        "test HOST_MAC_VENDOR"
        entry = uvmContext.hostTable().getHostTableEntry( remote_control.client_ip )
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
        entry = uvmContext.hostTable().getHostTableEntry( remote_control.client_ip )
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
        entry = uvmContext.hostTable().getHostTableEntry( socket.gethostbyname("test.untangle.com") )
        if entry != None:
            raise unittest.SkipTest('Entry exists')
        self.rule_add("SERVER_MAC_VENDOR",'*')
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_host_mac(self):
        "test HOST_MAC"
        entry = uvmContext.hostTable().getHostTableEntry( remote_control.client_ip )
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
        entry = uvmContext.hostTable().getHostTableEntry( remote_control.client_ip )
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
        entry = uvmContext.hostTable().getHostTableEntry( socket.gethostbyname("test.untangle.com") )
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
        entry = uvmContext.hostTable().getHostTableEntry( remote_control.client_ip )
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
        result = self.get_web_request_results(url="http://playboy.com", expected="blockpage")
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
        result = self.get_web_request_results(url="http://playboy.com", expected="blockpage")
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
        
    @staticmethod
    def final_tear_down(self):
        global app
        if app != None:
            uvmContext.appManager().destroy( app.getAppSettings()["id"] )
            app = None

test_registry.register_module("web-filter", WebFilterTests)
