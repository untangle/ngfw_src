"""threat_prevention tests"""
import datetime
import pytest
import runtests
import subprocess
import unittest
import sys

from tests.common import NGFWTestCase
from tests.global_functions import uvmContext
import runtests.remote_control as remote_control
import runtests.test_registry as test_registry
import tests.global_functions as global_functions

default_policy_id = 1
app = None

@pytest.mark.threat_prevention
class ThreatpreventionTests(NGFWTestCase):

    force_start = True

    @staticmethod
    def module_name():
        # cheap trick to force class variable _app into global namespace as app
        global app
        app = ThreatpreventionTests._app
        return "threat-prevention"

    @staticmethod
    def eventAppName():
        return "threat_prevention"

    @staticmethod
    def displayName():
        return "Threat Prevention"

    @classmethod
    def initial_extra_setup(cls):
        global appData
        appData = cls._app.getSettings()

    @classmethod
    def initial_extra_setup(cls):
        global_functions.get_latest_client_test_pkg("web")

    def rule_add(self, conditionType, conditionData, action="block", flagged=True, description="description"):
        newRule =  {
            "flag": flagged,
            "enabled": True,
            "description": description,
            "action": action,
            "javaClass": "com.untangle.app.threat_prevention.ThreatPreventionRule",
                "conditions": {
                    "javaClass": "java.util.LinkedList",
                    "list": [
                        {
                            "conditionType": conditionType,
                            "invert": False,
                            "javaClass": "com.untangle.app.threat_prevention.ThreatPreventionRuleCondition",
                            "value": conditionData
                        }
                    ]
                }
            }
        rules = self._app.getRules()
        rules["list"].append(newRule)
        self._app.setRules(rules)

    def multiple_rule_add(self, conditionType, conditionData, conditionType2, conditionData2, action="block", flagged=True, description="description"):
        newRule =  {
            "flag": flagged,
            "enabled": True,
            "description": description,
            "action": action,
            "javaClass": "com.untangle.app.threat_prevention.ThreatPreventionRule",
                "conditions": {
                    "javaClass": "java.util.LinkedList",
                    "list": [
                        {
                            "conditionType": conditionType,
                            "invert": False,
                            "javaClass": "com.untangle.app.threat_prevention.ThreatPreventionRuleCondition",
                            "value": conditionData
                        },
                        {
                            "conditionType": conditionType2,
                            "invert": False,
                            "javaClass": "com.untangle.app.threat_prevention.ThreatPreventionRuleCondition",
                            "value": conditionData2
                        }
                    ]
                }
            }
        rules = self._app.getRules()
        rules["list"].append(newRule)
        self._app.setRules(rules)

    def rules_clear(self):
        rules = self._app.getRules()
        rules["list"] = []
        self._app.setRules(rules)

    def test_010_clientIsOnline(self):
        result = remote_control.is_online()
        assert (result == 0)
            
    def test_011_license_valid(self):
        assert(uvmContext.licenseManager().isLicenseValid(self.module_name()))

    @pytest.mark.failure_behind_pihole
    def test_020_basic_block(self):
        output_file_name=global_functions.get_new_output_filename()
        result = remote_control.run_command(global_functions.build_wget_command(output_file=output_file_name, uri="http://account.paypal-inc.tribesiren.com"))
        assert (result == 0)
        search = remote_control.run_command(f"grep -q 'blocked' {output_file_name}")
        assert (search == 0)

        events = global_functions.get_events(self.displayName(),'Blocked Web Events',None,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            "host","account.paypal-inc.tribesiren.com",
                                            self.eventAppName() + '_blocked', True,
                                            self.eventAppName() + '_flagged', True )
        assert( found )
        
    def test_030_test_untangle_com_reachable(self):
        output_file_name=global_functions.get_new_output_filename()
        result = remote_control.run_command(global_functions.build_wget_command(output_file=output_file_name, uri="http://test.untangle.com/test/testPage1.html"))
        assert (result == 0)
        search = remote_control.run_command(f"grep -q 'text123' {output_file_name}")
        assert (search == 0)

        events = global_functions.get_events(self.displayName(),'All Web Events',None,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            "host","test.untangle.com",
                                            self.eventAppName() + '_blocked', False,
                                            self.eventAppName() + '_flagged', False )
        assert( found )
        
    def test_031_block_by_IP(self):
        self.rules_clear()
        self.rule_add("DST_ADDR",global_functions.test_server_ip)

        output_file_name=global_functions.get_new_output_filename()
        result = remote_control.run_command(global_functions.build_wget_command(output_file=output_file_name, uri="http://test.untangle.com/test/testPage1.html"))
        assert (result == 0)
        search = remote_control.run_command(f"grep -q blocked {output_file_name}")
        assert (search == 0)

        events = global_functions.get_events(self.displayName(),'Blocked Web Events',None,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            "host","test.untangle.com",
                                            self.eventAppName() + '_blocked', True,
                                            self.eventAppName() + '_flagged', True )
        assert( found )
        
    def test_032_block_by_Port(self):
        self.rules_clear()
        self.rule_add("DST_PORT","80")

        output_file_name=global_functions.get_new_output_filename()
        result = remote_control.run_command(global_functions.build_wget_command(output_file=output_file_name, uri="http://test.untangle.com/test/testPage1.html"))
        assert (result == 0)
        search = remote_control.run_command(f"grep -q blocked {output_file_name}")
        assert (search == 0)

        events = global_functions.get_events(self.displayName(),'Blocked Web Events',None,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            "host","test.untangle.com",
                                            self.eventAppName() + '_blocked', True,
                                            self.eventAppName() + '_flagged', True )
        assert( found )
        
    def test_033_block_by_mac_address(self):
        entry = uvmContext.hostTable().getHostTableEntry( remote_control.client_ip )
        self.rules_clear()
        self.rule_add("DST_PORT","53",action="pass")  # allow DNS otherwise bridged configs fail
        self.rule_add("SRC_MAC",entry['macAddress'])

        output_file_name=global_functions.get_new_output_filename()
        result = remote_control.run_command(global_functions.build_wget_command(output_file=output_file_name, uri="http://test.untangle.com/test/testPage1.html"))
        assert (result == 0)
        search = remote_control.run_command(f"grep -q blocked {output_file_name}")
        assert (search == 0)

        events = global_functions.get_events(self.displayName(),'Blocked Web Events',None,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            "hostname",entry['hostname'],
                                            self.eventAppName() + '_blocked', True,
                                            self.eventAppName() + '_flagged', True )
        assert( found )
        
    def test_034_check_rule_precedence(self):
        self.rules_clear()
        self.rule_add("SRC_ADDR",remote_control.client_ip,action="pass",flagged=False) # 1st rule
        self.rule_add("DST_PORT","80",action="block")  # 2nd rule

        output_file_name=global_functions.get_new_output_filename()
        result = remote_control.run_command(global_functions.build_wget_command(output_file=output_file_name))
        assert (result == 0)

        events = global_functions.get_events(self.displayName(),'All Web Events',None,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            "c_client_addr",remote_control.client_ip,
                                            self.eventAppName() + '_blocked', False,
                                            self.eventAppName() + '_flagged', False )
        assert( found )

    @pytest.mark.failure_behind_pihole
    def test_035_check_rule_over_category(self):
        self.rules_clear()
        self.rule_add("SRC_ADDR",remote_control.client_ip,action="pass",flagged=False)

        output_file_name=global_functions.get_new_output_filename()
        result = remote_control.run_command(global_functions.build_wget_command(output_file=output_file_name, uri="http://account.paypal-inc.tribesiren.com"))
        assert (result == 0)

        events = global_functions.get_events(self.displayName(),'All Web Events',None,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            "c_client_addr",remote_control.client_ip,
                                            self.eventAppName() + '_blocked', False,
                                            self.eventAppName() + '_flagged', False )
        assert( found )

    def test_080_multiple_rule_conditions_matching(self):
        """Verify that conditions are AND logic"""
        self.rules_clear()
        print(f"test_server_ip={global_functions.test_server_ip}")
        self.multiple_rule_add("DST_ADDR",global_functions.test_server_ip,"DST_PORT","443")

        output_file_name=global_functions.get_new_output_filename()
        result = remote_control.run_command(global_functions.build_wget_command(output_file=output_file_name))
        assert (result == 0)
        search = remote_control.run_command(f"grep -q Hi {output_file_name}")
        assert (search == 0)

        output_file_name=global_functions.get_new_output_filename()
        result = remote_control.run_command(global_functions.build_curl_command(output_file=output_file_name, uri="https://test.untangle.com"))
        assert (result == 0)
        search = remote_control.run_command(f"grep -q blocked {output_file_name}")
        assert (search == 0)

        self.rules_clear()
        events = global_functions.get_events(self.displayName(),'All Web Events',None,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 1,
                                            "host","test.untangle.com",
                                            self.eventAppName() + '_blocked', True,
                                            self.eventAppName() + '_flagged', True )
        assert( found )

    def test_551_https_with_sni_packet_split(self):
        """ Verify no exceptions with split Hello TLS packets"""
        if runtests.quick_tests_only:
            raise unittest.SkipTest('Skipping a time consuming test')

        sni_domain = "docs.google.com"
        app_id = self._app.getAppSettings()["id"]
        log_file = f"/var/log/uvm/app-{app_id}.log"

        count = 0
        exceptions = 0

        # Not the entire packet, but a little bit beyond the position of the SNI server record.
        max_index=1600
        # Hit as many places as we could be out of bounds with split
        packet_split_iteration = 2

        for index in range(2, max_index,packet_split_iteration):
            last_log_line = subprocess.check_output(f"wc -l {log_file} | cut -d' ' -f1", shell=True).decode("utf-8").strip()
            last_log_line = int(last_log_line) + 1
            result = remote_control.run_command(f"./web/https_client.py -i {index}")

            log_invalid_exception = subprocess.check_output(f"awk 'NR >= {last_log_line} && /WARN  Exception calling extractSNIhostname/{{ print NR, $0 }}' {log_file}", shell=True).decode("utf-8")
            print(log_invalid_exception)
            for log in log_invalid_exception.split("\n"):
                if len(log) == 0:
                    continue
                exceptions += 1
                break

            count += 1

        print(f"count={count}, exceptions={exceptions}")
        assert exceptions == 0, "exceptions"

    @classmethod
    def final_extra_tear_down(cls):
        pass
        
test_registry.register_module("threat-prevention", ThreatpreventionTests)
