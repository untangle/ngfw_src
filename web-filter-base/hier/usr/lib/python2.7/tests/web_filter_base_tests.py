import unittest2
import time
import sys
import datetime
import re

from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from global_functions import uvmContext
from uvm import Manager
from uvm import Uvm
import remote_control
import global_functions

class WebFilterBaseTests(unittest2.TestCase):

    @staticmethod
    def nodeName():
        return "untangle-base-web-filter"

    @staticmethod
    def shortNodeName():
        return "web-filter"

    @staticmethod
    def eventNodeName():
        return "web_filter"

    @staticmethod
    def displayName():
        return "Web Filter"

    def block_url_list_add(self, url, blocked=True, flagged=True, description="description"):
        node_name = self.node.getAppName()
        if ("monitor" in node_name):
            newRule = { "blocked": False, "description": description, "flagged": flagged, "javaClass": "com.untangle.uvm.node.GenericRule", "string": url }
        else:
            newRule = { "blocked": blocked, "description": description, "flagged": flagged, "javaClass": "com.untangle.uvm.node.GenericRule", "string": url }
        rules = self.node.getBlockedUrls()
        rules["list"].append(newRule)
        self.node.setBlockedUrls(rules)

    def block_url_list_clear(self):
        rules = self.node.getBlockedUrls()
        rules["list"] = []
        self.node.setBlockedUrls(rules)

    def pass_url_list_add(self, url, enabled=True, description="description"):
        newRule =  { "enabled": enabled, "description": description, "javaClass": "com.untangle.uvm.node.GenericRule", "string": url }
        rules = self.node.getPassedUrls()
        rules["list"].append(newRule)
        self.node.setPassedUrls(rules)

    def pass_url_list_clear(self):
        rules = self.node.getPassedUrls()
        rules["list"] = []
        self.node.setPassedUrls(rules)

    def rule_add(self, conditionType, conditionData, blocked=True, flagged=True, description="description"):
        newRule =  {
            "blocked": blocked,
            "flagged": flagged,
            "enabled": True,
            "description": description,
            "javaClass": "com.untangle.node.web_filter.WebFilterRule",
                "conditions": {
                    "javaClass": "java.util.LinkedList",
                    "list": [
                        {
                            "conditionType": conditionType,
                            "invert": False,
                            "javaClass": "com.untangle.node.web_filter.WebFilterRuleCondition",
                            "value": conditionData
                        }
                    ]
                }
            }
        rules = self.node.getFilterRules()
        rules["list"].append(newRule)
        self.node.setFilterRules(rules)

    def rules_clear(self):
        rules = self.node.getFilterRules()
        rules["list"] = []
        self.node.setFilterRules(rules)

    def get_web_request_results(self, url="http://test.untangle.com", expected=None):
        extra_opts = ""
        node_name = self.node.getAppName()
        if ("https" in url):
            extra_opts = "--no-check-certificate "
        if ((expected == None) or (("monitor" in node_name) and (expected == "blockpage"))):
            result = remote_control.runCommand("wget -q -O /dev/null -4 -t 2 --timeout=5 " + extra_opts +  url)
        else:
            result = remote_control.runCommand("wget -q -O - " + extra_opts + url + " 2>&1 | grep -q " + expected)
        return result

    def check_events(self, host="", uri="", blocked=True, flagged=None):
        node_display_name = self.node.getNodeTitle()
        if flagged == None:
            flagged = blocked
        if (("Monitor" in node_display_name) and blocked):
            blocked = False
        if (blocked):
            event_list = "Blocked Web Events"
        elif (flagged):
            event_list = "Flagged Web Events"
        else:
            event_list = "All Web Events"
        events = global_functions.get_events(node_display_name, event_list, None, 5)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            "host", host,
                                            "uri", uri,
                                            'web_filter_blocked', blocked,
                                            'web_filter_flagged', flagged )
        return found
    
    @staticmethod
    def initialSetUp(self):
        if (uvmContext.nodeManager().isInstantiated(self.nodeName())):
            raise Exception('node %s already instantiated' % self.nodeName())
        node = uvmContext.nodeManager().instantiate(self.nodeName(), defaultRackId)
        nodemetrics = uvmContext.metricManager().getMetrics(node.getNodeSettings()["id"])
        self.node = node

    def setUp(self):
        pass

    def test_010_client_is_online(self):
        result = remote_control.isOnline()
        assert (result == 0)

    def test_011_test_untangle_com_reachable(self):
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        assert (result == 0)
        found = self.check_events("test.untangle.com", "/test/testPage1.html", False)
        assert( found )

    def test_012_porn_is_blocked_by_default(self):
        result = self.get_web_request_results(url="http://playboy.com/", expected="blockpage")
        assert (result == 0)
        found = self.check_events("playboy.com", "/", True)
        assert( found )

    def test_013_porn_subdomain_is_blocked_by_default(self):
        result = self.get_web_request_results(url="http://www.playboy.com/", expected="blockpage")
        assert (result == 0)
        found = self.check_events("www.playboy.com", "/", True)
        assert( found )

    def test_014_porn_subdomain_and_url_is_blocked_by_default(self):
        result = self.get_web_request_results(url="http://www.playboy.com/bunnies.html", expected="blockpage")
        assert (result == 0)
        found = self.check_events("www.playboy.com", "/bunnies.html", True)
        assert( found )

    def test_017_blockflag_url_right_side(self):
        """verify that the right side is anchored with ".*$" """
        self.block_url_list_add("test.untangle.com/test/")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.block_url_list_clear()
        assert (result == 0)
        found = self.check_events("test.untangle.com", "/test/testPage1.html", True)
        assert( found )

    def test_018_blockflag_url_right_side_anchor(self):
        """verify that the right side anchor is not added if a $ is specified"""
        self.block_url_list_add("test.untangle.com/test/$")
        result0 = self.get_web_request_results(url="http://test.untangle.com/testPage1.html", expected="text123")
        result1 = self.get_web_request_results(url="http://test.untangle.com/test/", expected="blockpage")
        self.block_url_list_clear()
        assert (result0 == 0)
        assert (result1 == 0)
        found0 = self.check_events("test.untangle.com", "/testPage1.html", False)
        found1 = self.check_events("test.untangle.com", "/test/", True)
        assert( found0 )
        assert( found1 )

    def test_020_blockflag_url_case_sensitivity(self):
        """verify that a block list entry does not match when the URI capitalization is different"""
        self.block_url_list_add("test.untangle.com/test/testPage1.html")
        # this test URL should NOT be blocked (capitalization is different)
        result = self.get_web_request_results(url="http://test.untangle.com/test/testpage1.html", expected="text123")
        self.block_url_list_clear()
        assert (result == 0)

    def test_021_blockflag_url_case_sensitivity_domain(self):
        """verify that a block list entry does match when the DOMAIN capitalization is different"""
        self.block_url_list_add("test.untangle.com/test/testPage1.html")
        # this test URL should be blocked (capitalization of domain is different, URL captilatization is the same)
        result = self.get_web_request_results(url="http://TEST.untangle.com/test/testPage1.html", expected="blockpage")
        self.block_url_list_clear()
        assert (result == 0)
        
    def test_030_blockflag_url_glob_star(self):
        """verify that a block list glob functions with * at the end"""
        self.block_url_list_add("test.untangle.com/test/test*")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.block_url_list_clear()
        assert (result == 0)
        found = self.check_events("test.untangle.com", "/test/testPage1.html", True)
        assert( found )

    def test_031_blockflag_url_glob_star_multi(self):
        """verify that a block list glob functions with * at the end and at the beginning"""
        self.block_url_list_add("tes*tangle.com/test/test*")
        # this test URL should be blocked
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.block_url_list_clear()
        assert (result == 0)
        found = self.check_events("test.untangle.com", "/test/testPage1.html", True)
        assert( found )

    def test_032_blockflag_url_glob_star_begin_and_end(self):
        """verify that a block list glob functions with * at the end and at the beginning and in the middle"""
        self.block_url_list_add("*est*angle.com/test/test*")
        # this test URL should be blocked
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.block_url_list_clear()
        assert (result == 0)
        found = self.check_events("test.untangle.com", "/test/testPage1.html", True)
        assert( found )

    def test_033_blockflag_url_glob_star_whoreuri(self):
        """verify that a block list glob matches the whole URL"""
        self.block_url_list_add("test.untangle.com*")
        # this test URL should be blocked
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        assert (result == 0)
        self.block_url_list_clear()
        found = self.check_events("test.untangle.com", "/test/testPage1.html", True)
        assert( found )

    def test_034_blockflag_url_glob_star_zerolen(self):
        """verify that a block list glob * matches zero characters"""
        self.block_url_list_add("te*st.untangle.com*")
        # this test URL should NOT be blocked
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.block_url_list_clear()
        assert (result == 0)
        found = self.check_events("test.untangle.com", "/test/testPage1.html", True)
        assert( found )

    def test_035_blockflag_url_glob_star_no_overmatch(self):
        """verify that a block list glob * doesnt overmatch"""
        self.block_url_list_add("test.untangle.com/test/testP*.html")
        # this test URL should NOT be blocked (uri is different)
        self.block_url_list_clear()
        result = self.get_web_request_results(url="http://test.untangle.com/test/test.html", expected="text123")
        assert (result == 0)
        found = self.check_events("test.untangle.com", "/test/test.html", False)
        assert( found )

    def test_036_blockflag_url_glob_question_mark(self):
        """verify that a block list glob ? matches a single character"""
        self.block_url_list_add("te?t.untangle.com/test/testP?ge1.html")
        # this test URL should be blocked
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.block_url_list_clear()
        assert (result == 0)
        found = self.check_events("test.untangle.com", "/test/testPage1.html", True)
        assert( found )

    def test_037_blockflag_url_glob_question_mark_only_one_char(self):
        """verify that a block list glob ? matches ONLY single character (but not two or more)"""
        self.block_url_list_add("metalo?t.com/test/testP?.html")
        # this test URL should NOT be blocked
        result = remote_control.runCommand("wget -q -O - http://test.untangle.com/test/testPage1.html 2>&1 | grep -q text123")
        self.block_url_list_clear()
        assert (result == 0)
        found = self.check_events("test.untangle.com", "/test/testPage1.html", False)
        assert( found )

    def test_038_blockflag_url_glob_argument(self):
        """verify that the full URI is included in the match (even things in arguments) bug #10067"""
        self.block_url_list_add("test.untangle.com/*foo*")
        # this test URL should NOT be blocked
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html?arg=foobar", expected="blockpage")
        self.block_url_list_clear()
        assert (result == 0)
        found = self.check_events("test.untangle.com", "/test/testPage1.html?arg=foobar", True)
        assert( found )

    def test_039_blockflag_url_subdomain(self):
        """verify that untangle.com block rule also blocks test.untangle.com"""
        self.block_url_list_add("untangle.com")
        # this test URL should NOT be blocked
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        assert (result == 0)
        self.block_url_list_clear()
        found = self.check_events("test.untangle.com", "/test/testPage1.html", True)
        assert( found )

    def test_040_blockflag_url_subdomain_no_substring(self):
        """verify that t.untangle.com block rule DOES NOT block test.untangle.com ( it should block foo.t.untangle.com though )"""
        self.block_url_list_add("t.untangle.com")
        # this test URL should NOT be blocked
        result = remote_control.runCommand("wget -q -O - http://test.untangle.com/test/testPage1.html 2>&1 | grep -q text123")
        self.block_url_list_clear()
        assert (result == 0)
        found = self.check_events("test.untangle.com", "/test/testPage1.html", False)
        assert( found )

    def test_045_blockflag_url_order(self):
        """verify that a the action in taken from the first rule"""
        self.block_url_list_add("test.untangle.com/test/testPage1.html", blocked=False, flagged=False)
        self.block_url_list_add("test.untangle.com", blocked=True, flagged=True)
        # this test URL should NOT be blocked
        result = remote_control.runCommand("wget -q -O - http://test.untangle.com/test/testPage1.html 2>&1 | grep -q text123")
        self.block_url_list_clear()
        assert (result == 0)
        found = self.check_events("test.untangle.com", "/test/testPage1.html", blocked=False)
        assert( found )

    def test_046_blockflag_url_rule_order_inverse(self):
        """verify that a the action in taken from the second rule (first rule doesn't match)"""
        self.block_url_list_add("test.untangle.com/test/testPage1.html", blocked=False, flagged=True)
        self.block_url_list_add("test.untangle.com", blocked=True, flagged=True)
        # this test URL should be blocked
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage2.html", expected="blockpage")
        self.block_url_list_clear()
        assert (result == 0)
        found = self.check_events("test.untangle.com", "/test/testPage2.html", True)
        assert( found )

    def test_050_pass_url_overrides_category(self):
        """verify that an entry in the pass list overrides a blocked category"""
        self.pass_url_list_add("playboy.com")
        # this test URL should NOT be blocked (porn is blocked by default, but playboy.com now on pass list
        result1 = self.get_web_request_results(url="http://playboy.com/")
        result2 = self.get_web_request_results(url="http://www.playboy.com/")
        self.block_url_list_clear()
        assert (result1 == 0)
        assert (result2 == 0)
        found1 = self.check_events("playboy.com", "/", False)
        found2 = self.check_events("www.playboy.com", "/", False)
        assert( found1 )
        assert( found2 )

    def test_051_pass_url_overrides_blocked_url(self):
        """verify that an entry in the pass list overrides a blocked category"""
        self.block_url_list_add("test.untangle.com")
        self.pass_url_list_add("test.untangle.com/test/")
        # this test URL should NOT be blocked
        result = remote_control.runCommand("wget -q -O - http://test.untangle.com/test/testPage1.html 2>&1 | grep -q text123")
        self.block_url_list_clear()
        self.pass_url_list_clear()
        assert (result == 0)
        found = self.check_events("test.untangle.com", "/test/testPage1.html", False)
        assert( found )

    def test_052_pass_url_overrides_rule_content_type(self):
        """verify that an entry in the pass list overrides a blocked category"""
        self.rules_clear()
        self.rule_add("WEB_FILTER_RESPONSE_CONTENT_TYPE","text/plain")
        self.pass_url_list_add("test.untangle.com/test/")
        # this test URL should NOT be blocked
        result = remote_control.runCommand("wget -q -O - http://test.untangle.com/test/test.txt 2>&1 | grep -q text123")
        self.block_url_list_clear()
        self.pass_url_list_clear()
        assert (result == 0)

    def test_053_pass_url_overrides_url_extension(self):
        """verify that an entry in the pass list overrides a blocked category"""
        self.rules_clear()
        self.rule_add("WEB_FILTER_REQUEST_FILE_EXTENSION","txt")
        self.pass_url_list_add("test.untangle.com/test/")
        # this test URL should NOT be blocked
        result = remote_control.runCommand("wget -q -O - http://test.untangle.com/test/test.txt 2>&1 | grep -q text123")
        self.block_url_list_clear()
        self.pass_url_list_clear()
        assert (result == 0)
        found = self.check_events("test.untangle.com", "/test/test.txt", False)
        assert( found )

    def test_060_rule_condition_response_content_type(self):
        """verify that WEB_FILTER_RESPONSE_CONTENT_TYPE matches"""
        self.rules_clear()
        self.rule_add("WEB_FILTER_RESPONSE_CONTENT_TYPE","text/plain")
        # this test URL should be blocked
        result = self.get_web_request_results(url="http://test.untangle.com/test/test.txt", expected="blockpage")
        self.rules_clear()
        assert (result == 0)
        found = self.check_events("test.untangle.com", "/test/test.txt", True)
        assert( found )

    def test_061_rule_condition_response_content_type_inverse(self):
        """verify that WEB_FILTER_RESPONSE_CONTENT_TYPE does not overmatch"""
        self.rules_clear()
        self.rule_add("WEB_FILTER_RESPONSE_CONTENT_TYPE","text/plain")
        # this test URL should NOT be blocked (its text/html not text/plain)
        result = remote_control.runCommand("wget -q -O - http://test.untangle.com/test/test.html 2>&1 | grep -q text123")
        self.rules_clear()
        assert (result == 0)
        found = self.check_events("test.untangle.com", "/test/test.html", False)
        assert( found )

    def test_070_rule_condition_request_file_extension(self):
        """verify that WEB_FILTER_REQUEST_FILE_EXTENSION matches"""
        self.rules_clear()
        self.rule_add("WEB_FILTER_REQUEST_FILE_EXTENSION","txt")
        # this test URL should be blocked
        result = self.get_web_request_results(url="http://test.untangle.com/test/test.txt", expected="blockpage")
        self.rules_clear()
        assert (result == 0)
        found = self.check_events("test.untangle.com", "/test/test.txt", True)
        assert( found )

    def test_071_rule_condition_request_file_extension_inverse(self):
        """verify that WEB_FILTER_REQUEST_FILE_EXTENSION does not overmatch"""
        self.rules_clear()
        self.rule_add("WEB_FILTER_REQUEST_FILE_EXTENSION","txt")
        # this test URL should NOT be blocked (its text/html not text/plain)
        result = remote_control.runCommand("wget -q -O - http://test.untangle.com/test/test.html 2>&1 | grep -q text123")
        self.rules_clear()
        assert (result == 0)
        found = self.check_events("test.untangle.com", "/test/test.html", False)
        assert( found )

    def test_072_rule_condition_request_file_extension_anchored(self):
        """verify that WEB_FILTER_REQUEST_FILE_EXTENSION does not overmatch by assuming a ."""
        self.rules_clear()
        self.rule_add("WEB_FILTER_REQUEST_FILE_EXTENSION","tml") # not this should only block ".tml" not ".html"
        # this test URL should NOT be blocked (its text/html not text/plain)
        result = remote_control.runCommand("wget -q -O - http://test.untangle.com/test/test.html 2>&1 | grep -q text123")
        self.rules_clear()
        assert (result == 0)
        found = self.check_events("test.untangle.com", "/test/test.html", False)
        assert( found )

    def test_073_rule_condition_request_file_extension_with_arguments(self):
        """verify that WEB_FILTER_REQUEST_FILE_EXTENSION matches with arguments"""
        self.rules_clear()
        self.rule_add("WEB_FILTER_REQUEST_FILE_EXTENSION","txt")
        # this test URL should be blocked
        result = self.get_web_request_results(url="http://test.untangle.com/test/test.txt?argument", expected="blockpage")
        self.rules_clear()
        assert (result == 0)
        found = self.check_events("test.untangle.com", "/test/test.txt?argument", True)
        assert( found )

    def test_101_reports_flagged_url(self):
        """check the Flagged Web Events report"""
        fname = sys._getframe().f_code.co_name
        self.block_url_list_clear();
        self.block_url_list_add("test.untangle.com/test/testPage1.html", blocked=False, flagged=True)
        # specify an argument so it isn't confused with other events
        result1 = remote_control.runCommand("wget -q -O - http://test.untangle.com/test/testPage1.html?arg=%s 2>&1 >/dev/null" % fname)
        events = global_functions.get_events(self.displayName(),'Flagged Web Events',None,5)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                               "host","test.untangle.com",
                                               "uri", ("/test/testPage1.html?arg=%s" % fname),
                                               'web_filter_blocked', False,
                                               'web_filter_flagged', True )
        assert( found )

    def test_102_reports_all_urls(self):
        """check the All Web Events report"""
        fname = sys._getframe().f_code.co_name
        self.block_url_list_clear();
        # specify an argument so it isn't confused with other events
        result1 = remote_control.runCommand("wget -q -O - http://test.untangle.com/test/testPage1.html?arg=%s 2>&1 >/dev/null" % fname)
        events = global_functions.get_events(self.displayName(),'All Web Events',None,5)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            "host","test.untangle.com",
                                            "uri", ("/test/testPage1.html?arg=%s" % fname),
                                            'web_filter_blocked', False,
                                            'web_filter_flagged', False )
        assert( found )

    def test_510_client_is_online_https(self):
        """verify client is online HTTPS"""
        global remote_control
        result = remote_control.runCommand("wget -q -O /dev/null -4 -t 2 --timeout=5 --no-check-certificate -o /dev/null https://test.untangle.com/")
        assert (result == 0)

    def test_530_https_porn_is_blocked(self):
        """check for block page with HTTPS request"""
        result = self.get_web_request_results(url="https://www.playboy.com/", expected="blockpage")
        assert (result == 0)
        found = self.check_events("www.playboy.com", "/", True)
        assert( found )

    def test_550_https_with_sni(self):
        """Check SNI block list handling"""
        self.block_url_list_add("test.untangle.com")
        result = self.get_web_request_results(url="https://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.block_url_list_clear()
        assert (result == 0)
        found = self.check_events("test.untangle.com", "/", True)
        assert( found )

    def test_560_https_blockflag_glob_sni(self):
        """verify that a block list glob * matches"""
        self.block_url_list_add("*st.untangle.com")
        result = self.get_web_request_results(url="https://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.block_url_list_clear()
        assert (result == 0)
        found = self.check_events("test.untangle.com", "/", True)
        assert( found )

    def test_561_https_blockflag_glob_question_mark(self):
        """verify that a block list glob ? matches a single character"""
        self.block_url_list_add("t?st.untangle.com")
        result = self.get_web_request_results(url="https://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.block_url_list_clear()
        assert (result == 0)
        found = self.check_events("test.untangle.com", "/", True)
        assert( found )

    def test_562_https_blockflag_subdomain(self):
        """verify that untangle.com block rule also blocks test.untangle.com"""
        self.block_url_list_add("untangle.com")
        # this test URL should NOT be blocked
        result = self.get_web_request_results(url="https://test.untangle.com/test/testPage1.html", expected="blockpage")
        assert (result == 0)
        self.block_url_list_clear()
        found = self.check_events("test.untangle.com", "/", True)
        assert( found )

    def test_563_https_blockflag_subdomain_no_substring(self):
        """verify that t.untangle.com block rule DOES NOT block test.untangle.com ( it should block foo.t.untangle.com though )"""
        self.block_url_list_add("t.untangle.com")
        # this test URL should NOT be blocked
        result = self.get_web_request_results(url="https://test.untangle.com/test/testPage1.html", expected="text123")
        self.block_url_list_clear()
        assert (result == 0)
        found = self.check_events("test.untangle.com", "/", False)
        assert( found )

    def test_564_pass_url_works_with_https_sni(self):
        """verify that an entry in the pass list overrides a blocked category"""
        self.pass_url_list_add("playboy.com")
        # this test URL should NOT be blocked (porn is blocked by default, but playboy.com now on pass list
        result1 = remote_control.runCommand("wget -q -4 -t 2 --timeout=8 --no-check-certificate -O - https://playboy.com/ 2>&1 | grep -q blockpage")
        result2 = remote_control.runCommand("wget -q -4 -t 2 --timeout=8 --no-check-certificate -O - https://www.playboy.com/ 2>&1 | grep -q blockpage")
        self.pass_url_list_clear()
        assert (result1 != 0)
        assert (result2 != 0)

    def test_565_pass_url_overrides_block_with_sni(self):
        """verify that the pass list still overrides the block with SNI"""
        self.block_url_list_add("untangle.com")
        self.pass_url_list_add("test.untangle.com")
        # this test URL should NOT be blocked
        result = remote_control.runCommand("wget -q -4 -t 2 --timeout=8 --no-check-certificate -O - https://test.untangle.com/test/testPage1.html 2>&1 | grep -q text123")
        self.block_url_list_clear()
        self.pass_url_list_clear()
        assert (result == 0)

    def test_600_web_searches(self):
        """check the web searches log correctly"""
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
        }]
        host = "www.bing.com"
        uri = "/search?q=oneterm&qs=n&form=QBRE"
        for t in termTests:
            fname = sys._getframe().f_code.co_name
            eventTime = datetime.datetime.now()
            result1 = remote_control.runCommand("wget -q -O - 'http://%s%s' 2>&1 >/dev/null" % ( t["host"], t["uri"] ) )

            events = global_functions.get_events(self.displayName(),'All Query Events',None,1)
            assert(events != None)
            found = global_functions.check_events( events.get('list'), 5,
                                                   "host", t["host"],
                                                   "term", t["term"])
            assert( found )

    @staticmethod
    def finalTearDown(self):
        if self.node != None:
            uvmContext.nodeManager().destroy( node.getNodeSettings()["id"] )
            self.node = None
