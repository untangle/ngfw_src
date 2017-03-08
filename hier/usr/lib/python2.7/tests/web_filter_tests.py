import unittest2
import time
import sys
import re
import datetime
import socket

from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from global_functions import uvmContext
from uvm import Manager
from uvm import Uvm
from tests.web_filter_base_tests import WebFilterBaseTests
import remote_control
import test_registry
import global_functions

defaultRackId = 1
node = None

#
# Just extends the web filter base tests
#
class WebFilterTests(WebFilterBaseTests):

    @staticmethod
    def nodeName():
        return "untangle-node-web-filter"

    @staticmethod
    def shortNodeName():
        return "web-filter"

    @staticmethod
    def eventNodeName():
        return "web_filter"

    @staticmethod
    def displayName():
        return "Web Filter"

    @staticmethod
    def initialSetUp(self):
        global node
        if (uvmContext.nodeManager().isInstantiated(self.nodeName())):
            raise Exception('node %s already instantiated' % self.nodeName())
        node = uvmContext.nodeManager().instantiate(self.nodeName(), defaultRackId)
        nodemetrics = uvmContext.metricManager().getMetrics(node.getNodeSettings()["id"])
        self.node = node

    def test_016_block_url(self):
        """verify basic URL blocking the the url block list"""
        pre_events_scan = global_functions.get_app_metric_value(self.node, "scan")
        pre_events_block = global_functions.get_app_metric_value(self.node, "block")
        self.block_url_list_add("test.untangle.com/test/testPage1.html")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.block_url_list_clear()
        assert ( result == 0 )
        # verify the faceplate counters have incremented.
        post_events_scan = global_functions.get_app_metric_value(self.node, "scan")
        post_events_block = global_functions.get_app_metric_value(self.node, "block")
        assert(pre_events_scan < post_events_scan)
        assert(pre_events_block < post_events_block)

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
                                            self.eventNodeName() + '_blocked', True,
                                            self.eventNodeName() + '_flagged', True )
        assert( found )

    def test_200_pass_referer_disabled(self):
        """disable pass referer and verify that a page with content that would be blocked is blocked."""
        self.block_url_list_add("test.untangle.com/test/refererPage.html")
        self.pass_url_list_add("test.untangle.com/test/testPage1.html")
        settings = self.node.getSettings()
        settings["passReferers"] = False
        self.node.setSettings(settings)
        resultReferer = remote_control.run_command("wget -q --header 'Referer: http://test.untangle.com/test/testPage1.html' -O - http://test.untangle.com/test/refererPage.html 2>&1 | grep -q 'Welcome to the referer page.'");
        print "result %s passReferers %s" % (resultReferer,settings["passReferers"])

        self.block_url_list_clear()
        self.pass_url_list_clear()
        assert( resultReferer == 1 )

    def test_201_pass_referer_enabled(self):
        """disable pass referer and verify that a page with content that would be blocked is allowed."""
        self.block_url_list_add("test.untangle.com/test/refererPage.html")
        self.pass_url_list_add("test.untangle.com/test/testPage1.html")
        settings = self.node.getSettings()
        settings["passReferers"] = True
        self.node.setSettings(settings)
        resultReferer = remote_control.run_command("wget -q --header 'Referer: http://test.untangle.com/test/testPage1.html' -O - http://test.untangle.com/test/refererPage.html 2>&1 | grep -q 'Welcome to the referer page.'");
        print "result %s passReferers %s" % (resultReferer,settings["passReferers"])

        self.block_url_list_clear()
        self.pass_url_list_clear()
        assert( resultReferer == 0 )

    def test_700_safe_search_enabled(self):
        """Check google safe search"""
        settings = self.node.getSettings()
        settings["enforceSafeSearch"] = False
        self.node.setSettings(settings)
        result_without_safe = remote_control.run_command("wget -q -O - '$@' -U 'Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.7) Gecko/20040613 Firefox/0.8.0+)' 'http://www.google.com/search?hl=en&q=boobs&safe=off' | grep -q 'safe=off'");

        settings["enforceSafeSearch"] = True
        self.node.setSettings(settings)
        result_with_safe = remote_control.run_command("wget -q -O - '$@' -U 'Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.7) Gecko/20040613 Firefox/0.8.0+)' 'http://www.google.com/search?hl=en&q=boobs&safe=off' | grep -q 'safe=active'");

        assert( result_without_safe == 0 )
        assert( result_with_safe == 0 )

    def test_701_unblock_option(self):
        """verify that a block page is shown but unblock button option is available."""
        self.block_url_list_add("test.untangle.com/test/testPage1.html")
        settings = self.node.getSettings()
        settings["unblockMode"] = "Host"
        self.node.setSettings(settings)
        # this test URL should be blocked but allow
        remote_control.run_command("rm -f /tmp/web_filter_base_test_120.log")
        result = remote_control.run_command("wget -4 -t 2 --timeout=5 -a /tmp/web_filter_base_test_120.log -O /tmp/web_filter_base_test_120.out http://test.untangle.com/test/testPage1.html")
        resultButton = remote_control.run_command("grep -q 'unblock' /tmp/web_filter_base_test_120.out")
        resultBlock = remote_control.run_command("grep -q 'blockpage' /tmp/web_filter_base_test_120.out")

        # get the IP address of the block page
        ipfind = remote_control.run_command("grep 'Location' /tmp/web_filter_base_test_120.log", stdout=True)
        # print 'ipFind %s' % ipfind
        ip = re.findall( r'[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}(?:[0-9:]{0,6})', ipfind )
        blockPageIP = ip[0]
        # print 'Block page IP address is %s' % blockPageIP
        blockParamaters = re.findall( r'\?(.*)\s', ipfind )
        paramaters = blockParamaters[0]
        # Use unblock button.
        unBlockParameters = "global=false&"+ paramaters + "&password="
        # print "unBlockParameters %s" % unBlockParameters
        print "wget -q -O /dev/null --post-data=\'" + unBlockParameters + "\' http://" + blockPageIP + "/" + self.shortNodeName() + "/unblock"
        remote_control.run_command("wget -q -O /dev/null --post-data=\'" + unBlockParameters + "\' http://" + blockPageIP + "/" + self.shortNodeName() + "/unblock")
        resultUnBlock = remote_control.run_command("wget -q -O - http://test.untangle.com/test/testPage1.html 2>&1 | grep -q text123")

        self.block_url_list_clear()
        self.node.flushAllUnblockedSites()

        print "block %s button %s unblock %s" % (resultBlock,resultButton,resultUnBlock)
        assert (resultBlock == 0)
        assert (resultButton == 0)
        assert (resultUnBlock == 0)

    def test_701_unblock_option_with_password(self):
        """verify that a block page is shown but unblock if correct password."""
        fname = sys._getframe().f_code.co_name
        self.block_url_list_add("test.untangle.com/test/testPage2.html")
        settings = self.node.getSettings()
        settings["unblockMode"] = "Host"
        settings["unblockPassword"] = "atstest"
        settings["unblockPasswordEnabled"] = True
        self.node.setSettings(settings)

        # this test URL should be blocked but allow
        remote_control.run_command("rm -f /tmp/%s.log"%fname)
        result = remote_control.run_command("wget -4 -t 2 --timeout=5 -a /tmp/%s.log -O /tmp/%s.out http://test.untangle.com/test/testPage2.html"%(fname,fname))
        resultButton = remote_control.run_command("grep -q 'unblock' /tmp/%s.out"%fname)
        resultBlock = remote_control.run_command("grep -q 'blockpage' /tmp/%s.out"%fname)

        # get the IP address of the block page
        ipfind = remote_control.run_command("grep 'Location' /tmp/%s.log"%fname,stdout=True)
        print 'ipFind %s' % ipfind
        ip = re.findall( r'[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}(?:[0-9:]{0,6})', ipfind )
        blockPageIP = ip[0]
        # print 'Block page IP address is %s' % blockPageIP
        blockParamaters = re.findall( r'\?(.*)\s', ipfind )
        paramaters = blockParamaters[0]
        # Use unblock button.
        unBlockParameters = "global=false&"+ paramaters + "&password=atstest"
        # print "unBlockParameters %s" % unBlockParameters
        remote_control.run_command("wget -q -O /dev/null --post-data=\'" + unBlockParameters + "\' http://" + blockPageIP + "/" + self.shortNodeName() + "/unblock")
        resultUnBlock = remote_control.run_command("wget -O - http://test.untangle.com/test/testPage2.html 2>&1 | grep -q text123")

        settings = self.node.getSettings()
        settings["unblockMode"] = "None"
        settings["unblockPassword"] = ""
        settings["unblockPasswordEnabled"] = False

        self.node.setSettings(settings)
        self.block_url_list_clear()
        print "block %s button %s unblock %s" % (resultBlock,resultButton,resultUnBlock)
        assert (resultBlock == 0 and resultButton == 0 and resultUnBlock == 0 )

    def test_010_0000_rule_condition_src_addr(self):
        "test SRC_ADDR"
        self.rule_add("SRC_ADDR",remote_control.clientIP)
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
        self.rule_add("DST_INTF",remote_control.interfaceExternal)
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.rules_clear()
        assert (result == 0)

    def test_010_0000_rule_condition_dst_intf_any(self):
        "test DST_INTF any"
        self.rule_add("DST_INTF","any")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
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
        assert (result == 0)
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
        uvmContext.hostTable().decrementQuota( remote_control.clientIP, 10000 )
        self.rule_add("HOST_QUOTA_EXCEEDED",remote_control.get_hostname())
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        assert (result == 0)
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
        uvmContext.hostTable().decrementQuota( remote_control.clientIP, 10000 )
        self.rule_add("CLIENT_QUOTA_EXCEEDED",remote_control.get_hostname())
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        assert (result == 0)
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
        self.rule_add("USER_QUOTA_EXCEEDED",remote_control.get_hostname())
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        assert (result == 0)
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
        
    @staticmethod
    def finalTearDown(self):
        global node
        if node != None:
            uvmContext.nodeManager().destroy( node.getNodeSettings()["id"] )
            node = None

test_registry.registerNode("web-filter", WebFilterTests)
