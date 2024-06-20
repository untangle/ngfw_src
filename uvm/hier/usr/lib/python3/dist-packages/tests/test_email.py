import pytest
import copy
import datetime
import shutil
import subprocess
import time

from tests.common import NGFWTestCase
import tests.global_functions as global_functions
import runtests
import unittest
import runtests.test_registry as test_registry

from uvm import Uvm

@pytest.mark.email_tests
class EmailTests(NGFWTestCase):

    not_an_app= True
    original_mail_settings = None

    @staticmethod
    def module_name():
        return "email-tests"

    @classmethod
    def initial_extra_setup(cls):
        if EmailTests.original_mail_settings is None:
            EmailTests.original_mail_settings = global_functions.uvmContext.mailSender().getSettings()

    @pytest.mark.slow
    def test_010_mail_send_method_modes(self):
        if runtests.quick_tests_only:
            raise unittest.SkipTest('Skipping a time consuming test')

        origMailsettings = global_functions.uvmContext.mailSender().getSettings()
        #Default sendMethod Mode is DIRECT
        assert (origMailsettings['sendMethod'] == 'DIRECT')

        #Upgrade scenario simulating RELAY method 
        newMailsettings = copy.deepcopy(origMailsettings)
        newMailsettings['sendMethod'] = 'RELAY'

        #Updating existing sendMethod mode to RELAY
        global_functions.uvmContext.mailSender().setSettings(newMailsettings)
        time.sleep(10) # give it time for exim to restart

        #Upgrade method scenario is simulated, sendMethod mode set to RELAY
        updatedMailSettings = global_functions.uvmContext.mailSender().getSettings()
        assert (updatedMailSettings['sendMethod'] == 'RELAY')

        #Restart UVM, this should set the sendMethod mode to DIRECT
        uvmContext = global_functions.restart_uvm()
        restartedMailSettings = uvmContext.mailSender().getSettings()
        assert (restartedMailSettings['sendMethod'] == 'DIRECT')
    def test_100_blocked_queue(self):
        """
        Non-blocking flush on network settings change

        This non-standard test works as follows:
        1. Get a new uvm context with a 10 second timeout instead of 240 (non-standard)
        2. Wipe out exim spool directory and restart exim to ensure a "fresh" system condition.
        3. Configure mail sender as relay.
        4. Break relay by setting relay.untangle.com to an unreachable IP address by modifying host name (non-standard)
        5. Create a new email alert for "plenty of disk space"
        6. Wait for event to be generated
        7, Wait for exim to try its first delivery attempt.
        8. Perform network settings change and verify it doesn't take 10 seconds.

        When restarting networking, we attempt to flush mail queue since
        network settings may materially change settings that didn't work previously like WAN.
        However, with some email environments where messages will "never" be delivered,
        this causes network settings to block for the exim timeout period.
        Make sure we don't block for more than an acceptable amount of time. 
        """
        alert_description = "TEST Free disk space is actually fine"
        hosts_filename = "/etc/hosts.dnsmasq"
        orig_hosts_filename = "/tmp/hosts.dnsmasq"
        relay_domain_name = "relay.untangle.com"

        exim_spool_directory = "/var/spool/exim4"
        exim_service = "exim4"
        exim_log_filename = "/var/log/exim4/mainlog"

        tester_email_address = "tester@domain.com"
        orig_mail_settings = EmailTests.original_mail_settings

        # uvm_context_timeout = 60
        uvm_context_timeout = 30
        local_uvm_context = Uvm().getUvmContext(timeout=uvm_context_timeout)

        # Wipe out exim spool directory
        subprocess.call(f"systemctl stop {exim_service}", shell=True)
        subprocess.call(f"killall {exim_service} 2>/dev/null", shell=True)
        shutil.rmtree(f"{exim_spool_directory}/")
        subprocess.call(f"systemctl start {exim_service}", shell=True)

        # Test in relay mode with the relay server non-reachable
        mail_settings = copy.deepcopy(EmailTests.original_mail_settings)
        mail_settings["fromAddress"] = tester_email_address
        mail_settings["sendMethod"] = "RELAY"
        local_uvm_context.mailSender().setSettings(mail_settings)
        
        # Normally we'd set static DNS override in network settings, but since the problem
        # is with network settings...manually update the hosts file
        shutil.copyfile(hosts_filename, orig_hosts_filename)
        with open(hosts_filename, "a", encoding="ascii") as hosts_file:
            hosts_file.write(f"192.168.0.99\t{relay_domain_name}.\n\n")
        subprocess.call(f"systemctl restart dnsmasq", shell=True)
        print(subprocess.check_output(f"host {relay_domain_name}", shell=True))

        last_exim_log_line = subprocess.check_output(f"wc -l {exim_log_filename} | cut -d' ' -f1", shell=True).decode("utf-8").strip()
        print(f"last_exim_log_line={last_exim_log_line}")

        original_event_settings = local_uvm_context.eventManager().getSettings()
        event_settings = copy.deepcopy(original_event_settings)
        event_settings["alertRules"]["list"].append({
               "conditions": {
                    "javaClass": "java.util.LinkedList",
                    "list": [
                        {
                            "comparator": "=",
                            "field": "class",
                            "fieldValue": "*SystemStatEvent*",
                            "javaClass": "com.untangle.uvm.event.EventRuleCondition"
                        },
                        {
                            "comparator": ">",
                            "field": "diskFreePercent",
                            "fieldValue": ".1",
                            "javaClass": "com.untangle.uvm.event.EventRuleCondition"
                        }
                    ]
                },
                "description": alert_description,
                "email": True,
                "emailLimitFrequency": True,
                "emailLimitFrequencyMinutes": 60,
                "enabled": True,
                "javaClass": "com.untangle.uvm.event.AlertRule",
                "log": True,
                "thresholdEnabled": False
        })
        local_uvm_context.eventManager().setSettings(event_settings)

        # Wait for alert event to be generated
        found_event = global_functions.get_wait_for_events(
            prefix="alert events",
            report_category="Events",
            report_title='Alert Events',
            matches={
                "description": alert_description
            },
            tries=60)
        print(f"found={found_event}")

        found_delivery_attempt = False
        if found_event:
            # Wait for exim to try its first attempt
            max_tries = 60
            tries = 0
            while tries < max_tries:
                print(f"awk 'NR >= {last_exim_log_line} && / \<\= {tester_email_address} H=localhost /{{ print NR, $0 }}' {exim_log_filename}")
                log_exim_attempt = subprocess.check_output(f"awk 'NR >= {last_exim_log_line} && / <= {tester_email_address} H=localhost /{{ print NR, $0 }}' {exim_log_filename}", shell=True).decode("utf-8")
                print(f"log_exim_attempt={log_exim_attempt}")
                if log_exim_attempt.strip() != "":
                    found_delivery_attempt = True
                    break
                tries += 1
                time.sleep(1)

        local_uvm_context.eventManager().setSettings(original_event_settings)

        print(f"found_delivery_attempt={found_delivery_attempt}")

        elapsed_time = None
        if found_delivery_attempt:
            # Perform a network settings save
            start_time = datetime.datetime.now()
            network_settings = local_uvm_context.networkManager().getNetworkSettings()
            try:
                network_settings = local_uvm_context.networkManager().setNetworkSettings(network_settings)
            except:
                pass
            elapsed_time = (datetime.datetime.now() - start_time).total_seconds()
            print(f"elaspsed_time={elapsed_time}")

        # ?? verify exim still running properly

        # Restore system
        # Restore hosts
        shutil.copyfile(orig_hosts_filename, hosts_filename)
        # re-wipe out exim spool directory
        subprocess.call(f"systemctl stop {exim_service}", shell=True)
        subprocess.call(f"killall {exim_service} 2>/dev/null", shell=True)
        shutil.rmtree(f"{exim_spool_directory}/")
        subprocess.call(f"systemctl start {exim_service}", shell=True)

        assert found_event, "found event"
        assert found_delivery_attempt, "found delivery attempt"
        assert elapsed_time < uvm_context_timeout, f"network settings completed in under {uvm_context_timeout}s"
        global_functions.uvmContext.mailSender().setSettings(orig_mail_settings)

    @classmethod
    def final_extra_tear_down(cls):
        if EmailTests.original_mail_settings is not None:
            global_functions.uvmContext.mailSender().setSettings(EmailTests.original_mail_settings)

test_registry.register_module("email", EmailTests)