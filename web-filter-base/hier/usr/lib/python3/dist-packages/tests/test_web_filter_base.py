"""web_filter_base tests"""
import sys
import datetime

from tests.common import NGFWTestCase
from tests.global_functions import uvmContext
import runtests.remote_control as remote_control
import tests.global_functions as global_functions


class WebFilterBaseTests(NGFWTestCase):

    @staticmethod
    def module_name():
        return "untangle-base-web-filter"

    @staticmethod
    def shortAppName():
        return "web-filter"

    @staticmethod
    def eventAppName():
        return "web_filter"

    @staticmethod
    def displayName():
        return "Web Filter"

    def block_url_list_add(self, url, blocked=True, flagged=True, description="description"):
        app_name = self._app.getAppName()
        if ("monitor" in app_name):
            newRule = { "blocked": False, "description": description, "flagged": flagged, "javaClass": "com.untangle.uvm.app.GenericRule", "string": url }
        else:
            newRule = { "blocked": blocked, "description": description, "flagged": flagged, "javaClass": "com.untangle.uvm.app.GenericRule", "string": url }
        rules = self._app.getBlockedUrls()
        rules["list"].append(newRule)
        self._app.setBlockedUrls(rules)

    def block_url_list_clear(self):
        rules = self._app.getBlockedUrls()
        rules["list"] = []
        self._app.setBlockedUrls(rules)

    def pass_url_list_add(self, url, enabled=True, description="description"):
        newRule =  { "enabled": enabled, "description": description, "javaClass": "com.untangle.uvm.app.GenericRule", "string": url }
        rules = self._app.getPassedUrls()
        rules["list"].append(newRule)
        self._app.setPassedUrls(rules)

    def pass_url_list_clear(self):
        rules = self._app.getPassedUrls()
        rules["list"] = []
        self._app.setPassedUrls(rules)

    def rule_add(self, conditionType, conditionData, blocked=True, flagged=True, description="description"):
        newRule =  {
            "blocked": blocked,
            "flagged": flagged,
            "enabled": True,
            "description": description,
            "javaClass": "com.untangle.app.web_filter.WebFilterRule",
                "conditions": {
                    "javaClass": "java.util.LinkedList",
                    "list": [
                        {
                            "conditionType": conditionType,
                            "invert": False,
                            "javaClass": "com.untangle.app.web_filter.WebFilterRuleCondition",
                            "value": conditionData
                        }
                    ]
                }
            }
        rules = self._app.getFilterRules()
        rules["list"].append(newRule)
        self._app.setFilterRules(rules)

    def multiple_rule_add(self, conditionType, conditionData, conditionType2, conditionData2, blocked=True, flagged=True, description="description"):
        newRule =  {
            "blocked": blocked,
            "flagged": flagged,
            "enabled": True,
            "description": description,
            "javaClass": "com.untangle.app.web_filter.WebFilterRule",
                "conditions": {
                    "javaClass": "java.util.LinkedList",
                    "list": [
                        {
                            "conditionType": conditionType,
                            "invert": False,
                            "javaClass": "com.untangle.app.web_filter.WebFilterRuleCondition",
                            "value": conditionData
                        },
                        {
                            "conditionType": conditionType2,
                            "invert": False,
                            "javaClass": "com.untangle.app.web_filter.WebFilterRuleCondition",
                            "value": conditionData2
                        }
                    ]
                }
            }
        rules = self._app.getFilterRules()
        rules["list"].append(newRule)
        self._app.setFilterRules(rules)

    def rules_clear(self):
        rules = self._app.getFilterRules()
        rules["list"] = []
        self._app.setFilterRules(rules)
        
    def search_term_rule_add(self, termWords, blocked=True, flagged=True, description="description"):
        newTerm =  {
            "blocked": blocked,
            "flagged": flagged,
            "description": description,
            "javaClass": "com.untangle.uvm.app.GenericRule",
            "string": termWords,
            }
        webSettings = self._app.getSettings()
        webSettings["searchTerms"]["list"].append(newTerm)
        self._app.setSettings(webSettings)

    def search_term_rules_clear(self):
        webSettings = self._app.getSettings()
        webSettings["searchTerms"]["list"] = []
        self._app.setSettings(webSettings)

    def get_web_request_results(self, url="http://test.untangle.com", expected=None, extra_options=""):
        app_name = self._app.getAppName()
        if ("https" in url) or ("playboy" in url):
            extra_options += "--no-check-certificate "
        if ((expected == None) or (("monitor" in app_name) and (expected == "blockpage"))):
            result = remote_control.run_command("wget -q -O /dev/null -4 -t 2 --timeout=5 " + extra_options + " " +  url)
        else:
            print("wget -4 -t 2 -q -O - " + extra_options + url + " 2>&1 | grep -q " + expected)
            result = remote_control.run_command("wget -q -4 -t 2 -O - " + extra_options + " " + url + " 2>&1 | grep -q " + expected)
        return result

    def check_events(self, host="", uri="", blocked=True, flagged=None):
        app_display_name = self._app.getAppTitle()
        if flagged == None:
            flagged = blocked
        if (("Monitor" in app_display_name) and blocked):
            blocked = False
        if (blocked):
            event_list = "Blocked Web Events"
        elif (flagged):
            event_list = "Flagged Web Events"
        else:
            event_list = "All Web Events"
        events = global_functions.get_events(app_display_name, event_list, None, 10)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 10,
                                            "host", host,
                                            "uri", uri,
                                            'web_filter_blocked', blocked,
                                            'web_filter_flagged', flagged )
        return found
    
    def test_000_client_is_online(self):
        result = remote_control.is_online()
        assert (result == 0)

    def test_011_license_valid(self):
        assert(uvmContext.licenseManager().isLicenseValid(self.module_name()))

    def test_012_test_untangle_com_reachable(self):
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="text123")
        assert (result == 0)
        found = self.check_events("test.untangle.com", "/test/testPage1.html", False)
        assert( found )

    def test_013_porn_is_blocked_by_default(self):
        result = self.get_web_request_results(url="http://playboy.com/", expected="blockpage")
        assert (result == 0)
        found = self.check_events("playboy.com", "/", True)
        assert( found )

    def test_014_porn_subdomain_is_blocked_by_default(self):
        result = self.get_web_request_results(url="http://www.playboy.com/", expected="blockpage")
        assert (result == 0)
        found = self.check_events("www.playboy.com", "/", True)
        assert( found )

    def test_015_porn_subdomain_and_url_is_blocked_by_default(self):
        result = self.get_web_request_results(url="http://www.playboy.com/about", expected="blockpage")
        assert (result == 0)
        found = self.check_events("www.playboy.com", "/about", True)
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
        result = remote_control.run_command("wget -q -O - http://test.untangle.com/test/testPage1.html 2>&1 | grep -q text123")
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
        result = remote_control.run_command("wget -q -O - http://test.untangle.com/test/testPage1.html 2>&1 | grep -q text123")
        self.block_url_list_clear()
        assert (result == 0)
        found = self.check_events("test.untangle.com", "/test/testPage1.html", False)
        assert( found )

    def test_045_blockflag_url_order(self):
        """verify that a the action in taken from the first rule"""
        self.block_url_list_add("test.untangle.com/test/testPage1.html", blocked=False, flagged=False)
        self.block_url_list_add("test.untangle.com", blocked=True, flagged=True)
        # this test URL should NOT be blocked
        result = remote_control.run_command("wget -q -O - http://test.untangle.com/test/testPage1.html 2>&1 | grep -q text123")
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
        found1 = self.check_events("playboy.com", "/", False)
        result2 = self.get_web_request_results(url="http://www.playboy.com/")
        found2 = self.check_events("www.playboy.com", "/", False)
        self.block_url_list_clear()
        assert (result1 == 0)
        assert (result2 == 0)
        assert( found1 )
        assert( found2 )

    def test_051_pass_url_overrides_blocked_url(self):
        """verify that an entry in the pass list overrides a blocked category"""
        self.block_url_list_add("test.untangle.com")
        self.pass_url_list_add("test.untangle.com/test/")
        # this test URL should NOT be blocked
        result = remote_control.run_command("wget -q -O - http://test.untangle.com/test/testPage1.html 2>&1 | grep -q text123")
        self.block_url_list_clear()
        self.pass_url_list_clear()
        assert (result == 0)
        found = self.check_events("test.untangle.com", "/test/testPage1.html", False)
        assert( found )

    def test_052_pass_url_overrides_rule_content_type(self):
        """verify that an entry in the pass list overrides a blocked category"""
        self.rules_clear()
        self.rule_add("HTTP_CONTENT_TYPE","text/plain")
        self.pass_url_list_add("test.untangle.com/test/")
        # this test URL should NOT be blocked
        result = remote_control.run_command("wget -q -O - http://test.untangle.com/test/test.txt 2>&1 | grep -q text123")
        self.block_url_list_clear()
        self.pass_url_list_clear()
        assert (result == 0)

    def test_053_pass_url_overrides_url_extension(self):
        """verify that an entry in the pass list overrides a blocked category"""
        self.rules_clear()
        self.rule_add("HTTP_REQUEST_FILE_EXTENSION","txt")
        self.pass_url_list_add("test.untangle.com/test/")
        # this test URL should NOT be blocked
        result = remote_control.run_command("wget -q -O - http://test.untangle.com/test/test.txt 2>&1 | grep -q text123")
        self.block_url_list_clear()
        self.pass_url_list_clear()
        assert (result == 0)
        found = self.check_events("test.untangle.com", "/test/test.txt", False)
        assert( found )

    def test_060_rule_condition_response_content_type(self):
        """verify that HTTP_CONTENT_TYPE matches"""
        self.rules_clear()
        self.rule_add("HTTP_CONTENT_TYPE","text/plain")
        # this test URL should be blocked
        result = self.get_web_request_results(url="http://test.untangle.com/test/test.txt", expected="blockpage")
        self.rules_clear()
        assert (result == 0)
        found = self.check_events("test.untangle.com", "/test/test.txt", True)
        assert( found )

    def test_061_rule_condition_response_content_type_inverse(self):
        """verify that HTTP_CONTENT_TYPE does not overmatch"""
        self.rules_clear()
        self.rule_add("HTTP_CONTENT_TYPE","text/plain")
        # this test URL should NOT be blocked (its text/html not text/plain)
        result = remote_control.run_command("wget -q -O - http://test.untangle.com/test/test.html 2>&1 | grep -q text123")
        self.rules_clear()
        assert (result == 0)
        found = self.check_events("test.untangle.com", "/test/test.html", False)
        assert( found )

    def test_070_rule_condition_request_file_extension(self):
        """verify that HTTP_REQUEST_FILE_EXTENSION matches"""
        self.rules_clear()
        self.rule_add("HTTP_REQUEST_FILE_EXTENSION","txt")
        # this test URL should be blocked
        result = self.get_web_request_results(url="http://test.untangle.com/test/test.txt", expected="blockpage")
        self.rules_clear()
        assert (result == 0)
        found = self.check_events("test.untangle.com", "/test/test.txt", True)
        assert( found )

    def test_071_rule_condition_request_file_extension_inverse(self):
        """verify that HTTP_REQUEST_FILE_EXTENSION does not overmatch"""
        self.rules_clear()
        self.rule_add("HTTP_REQUEST_FILE_EXTENSION","txt")
        # this test URL should NOT be blocked (its text/html not text/plain)
        result = remote_control.run_command("wget -q -O - http://test.untangle.com/test/test.html 2>&1 | grep -q text123")
        self.rules_clear()
        assert (result == 0)
        found = self.check_events("test.untangle.com", "/test/test.html", False)
        assert( found )

    def test_072_rule_condition_request_file_extension_anchored(self):
        """verify that HTTP_REQUEST_FILE_EXTENSION does not overmatch by assuming a ."""
        self.rules_clear()
        self.rule_add("HTTP_REQUEST_FILE_EXTENSION","tml") # not this should only block ".tml" not ".html"
        # this test URL should NOT be blocked (its text/html not text/plain)
        result = remote_control.run_command("wget -q -O - http://test.untangle.com/test/test.html 2>&1 | grep -q text123")
        self.rules_clear()
        assert (result == 0)
        found = self.check_events("test.untangle.com", "/test/test.html", False)
        assert( found )

    def test_073_rule_condition_request_file_extension_with_arguments(self):
        """verify that HTTP_REQUEST_FILE_EXTENSION matches with arguments"""
        self.rules_clear()
        self.rule_add("HTTP_REQUEST_FILE_EXTENSION","txt")
        # this test URL should be blocked
        result = self.get_web_request_results(url="http://test.untangle.com/test/test.txt?argument", expected="blockpage")
        self.rules_clear()
        assert (result == 0)
        found = self.check_events("test.untangle.com", "/test/test.txt?argument", True)
        assert( found )

    def test_080_multiple_rule_conditions_matching(self):
        """Verify that conditions are AND logic"""
        self.rules_clear()
        self.multiple_rule_add("DST_ADDR",global_functions.test_server_ip,"DST_PORT","443")
        result = self.get_web_request_results(url="http://test.untangle.com/", expected="'Hi'")
        assert (result == 0)
        result = self.get_web_request_results(url="https://test.untangle.com/", expected="blockpage")
        self.rules_clear()
        assert (result == 0)
        found = self.check_events("test.untangle.com", "/", True)
        assert( found )

    def test_101_reports_flagged_url(self):
        """check the Flagged Web Events report"""
        fname = sys._getframe().f_code.co_name
        self.block_url_list_clear();
        self.block_url_list_add("test.untangle.com/test/testPage1.html", blocked=False, flagged=True)
        # specify an argument so it isn't confused with other events
        result1 = remote_control.run_command("wget -q -O - http://test.untangle.com/test/testPage1.html?arg=%s 2>&1 >/dev/null" % fname)
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
        result1 = remote_control.run_command("wget -q -O - http://test.untangle.com/test/testPage1.html?arg=%s 2>&1 >/dev/null" % fname)
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
        result = remote_control.run_command("wget -q -O /dev/null -4 -t 2 --timeout=5 --no-check-certificate -o /dev/null https://test.untangle.com/")
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
        self.pass_url_list_add("pornhub.com")
        # this test URL should NOT be blocked (porn is blocked by default, but playboy.com now on pass list
        result1 = self.get_web_request_results(url="https://pornhub.com/test/testPage1.html", expected="blockpage")
        result2 = self.get_web_request_results(url="https://www.pornhub.com/test/testPage1.html", expected="blockpage")
        self.pass_url_list_clear()
        assert (result1 != 0)
        assert (result2 != 0)

    def test_565_pass_url_overrides_block_with_sni(self):
        """verify that the pass list still overrides the block with SNI"""
        self.block_url_list_add("untangle.com")
        self.pass_url_list_add("test.untangle.com")
        # this test URL should NOT be blocked
        result = remote_control.run_command("wget -q -4 -t 2 --timeout=8 --no-check-certificate -O - https://test.untangle.com/test/testPage1.html 2>&1 | grep -q text123")
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
            result1 = remote_control.run_command("wget -q -O - 'http://%s%s' 2>&1 >/dev/null" % ( t["host"], t["uri"] ) )

            events = global_functions.get_events(self.displayName(),'All Search Events',None,1)
            assert(events != None)
            found = global_functions.check_events( events.get('list'), 5,
                                                   "host", t["host"],
                                                   "term", t["term"])
            assert( found )

    def test_610_web_search_rules(self):
        """check the web rule searches log correctly"""
        host = "www.bing.com"
        term = "boobs"
        uri = "/search?q=%s&qs=n&form=QBRE" % term
        # fname = sys._getframe().f_code.co_name
        app_name = self._app.getAppName()
        if ("monitor" in app_name):
            blocked = False
        else:
            blocked = True
        self.search_term_rule_add(term,blocked)
        eventTime = datetime.datetime.now()
        result = remote_control.run_command("wget -q -O - 'http://%s%s' 2>&1 >/dev/null" % (host, uri) )

        events = global_functions.get_events(self.displayName(),'All Search Events',None,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                               "host", host,
                                               "term", term,
                                               'blocked', blocked,
                                               'flagged', True )
        self.search_term_rules_clear()
        assert( found )
