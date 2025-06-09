import copy
import pytest
import time

from tests.common import NGFWTestCase
from tests.global_functions import uvmContext
import tests.global_functions as global_functions
import runtests.test_registry as test_registry
import runtests.remote_control as remote_control
import runtests.overrides as overrides


DNS_HOST = overrides.get("DNS_HOST", default="0.0.0.0")
DNS_HOST_NAME = overrides.get("DNS_HOST", default="boxbackup.edge.arista.com")

def create_alert_rule(description, field, operator, value, field2, operator2, value2, thresholdEnabled=False, thresholdLimit=None, thresholdTimeframeSec=None, thresholdGroupingField=None, sendEmail=False):
    return {
        "email": sendEmail,
        "emailLimitFrequency": False,
        "emailLimitFrequencyMinutes": 60,
        "thresholdEnabled": thresholdEnabled,
        "thresholdLimit": thresholdLimit,
        "thresholdTimeframeSec": thresholdTimeframeSec,
        "thresholdGroupingField": thresholdGroupingField,
        "description": description,
        "enabled": True,
        "javaClass": "com.untangle.uvm.event.AlertRule",
        "log": True,
        "conditions": {
            "javaClass": "java.util.LinkedList",
            "list": [{
                "javaClass": "com.untangle.uvm.event.EventRuleCondition",
                "comparator": operator,
                "field": field,
                "fieldValue": value
            }, {
                "javaClass": "com.untangle.uvm.event.EventRuleCondition",
                "comparator": operator2,
                "field": field2,
                "fieldValue": value2
            }]
        },
        "ruleId": 1
    }

def createDNSRule( networkAddr, name):
    return {
        "address": networkAddr,
        "javaClass": "com.untangle.uvm.network.DnsStaticEntry",
        "name": name
    }

@pytest.mark.about_tests
class AlertTests(NGFWTestCase):

    not_an_app= True

    @staticmethod
    def module_name():
        return "alerts"
    
    def test_050_alert_rule(self):
        settings = uvmContext.eventManager().getSettings()
        orig_settings = copy.deepcopy(settings)
        new_rule = create_alert_rule("test alert rule", "class", "=", "*SessionEvent*", "localAddr", "=", "*"+remote_control.client_ip+"*")
        settings['alertRules']['list'].append( new_rule )
        uvmContext.eventManager().setSettings( settings )

        result = remote_control.is_online()
        time.sleep(4)

        events = global_functions.get_events('Events','Alert Events',None,10)
        found = global_functions.check_events( events.get('list'), 5,
                                            'description', 'test alert rule' )
        uvmContext.eventManager().setSettings( orig_settings )
        assert(events != None)
        assert ( found )

    @pytest.mark.failure_in_podman
    def test_060_customized_email_alert(self):
        """Create custom email template and verify alert email is received correctly"""
        #get settings, backup original settings
        email_settings = uvmContext.eventManager().getSettings()
        orig_email_settings = copy.deepcopy(email_settings)
        admin_settings = uvmContext.adminManager().getSettings()
        orig_admin_settings = copy.deepcopy(admin_settings)

        #change admin email to verify sent email
        new_admin_email = global_functions.random_email()
        admin_settings["users"]["list"][0]["emailAddress"] = new_admin_email
        uvmContext.adminManager().setSettings(admin_settings)

        #set custom email template subject and body
        new_email_subject = "NEW EMAIL SUBJECT TEST"
        new_email_body = "NEW EMAIL BODY TEST"
        email_settings["emailSubject"] = new_email_subject
        email_settings["emailBody"] = new_email_body

        #set new alert rule for easy trigger of email
        new_rule = create_alert_rule("test alert rule", "class", "=", "*SessionEvent*", "localAddr", "=", "*"+remote_control.client_ip+"*", sendEmail=True)
        email_settings['alertRules']['list'].append(new_rule)
        
        #set new settings
        uvmContext.eventManager().setSettings(email_settings)
        
        #send a session
        remote_control.is_online()
        time.sleep(4)

        #check email sent is correct
        emailFound = False
        timeout = 40
        alertEmail = ""
        while not emailFound and timeout > 0:
            timeout -= 1
            time.sleep(1)
            # alertEmail = remote_control.run_command("wget -q --timeout=5 -O - http://test.untangle.com/cgi-bin/getEmail.py?toaddress=" + new_admin_email + " 2>&1 | grep TEST" ,stdout=True)
            alertEmail = remote_control.run_command(global_functions.build_wget_command(output_file="-", uri=f"http://test.untangle.com/cgi-bin/getEmail.py?toaddress={new_admin_email}") + " 2>&1 | grep TEST", stdout=True)
            if (alertEmail != ""):
                emailFound = True
        
        #set settings back
        uvmContext.eventManager().setSettings(orig_email_settings)
        uvmContext.adminManager().setSettings(orig_admin_settings)
        
        assert(emailFound)

    def test_070_configuration_backup_fail_alert(self):
        """Verify alert on configuration backup failure"""

        # backup original network settings
        network_settings = uvmContext.networkManager().getNetworkSettings()
        orig_network_settings = copy.deepcopy(network_settings)

        # instantiate configuration backup app
        app_conf_backup = AlertTests.get_app("configuration-backup")

        # create DNS rule
        newRule = createDNSRule(DNS_HOST,DNS_HOST_NAME)
        network_settings['dnsSettings']['staticEntries']['list'] = [newRule]
        uvmContext.networkManager().setNetworkSettings(network_settings)

        # trigger configuration backup
        app_conf_backup.sendBackup()
        time.sleep(10)

        # verify configuration backup failure event log
        events = global_functions.get_events('Events','Alert Events',None,10)
        found = global_functions.check_events( events.get('list'), 5,
                                            'description', 'Configuration backup failed' )
        
        uvmContext.networkManager().setNetworkSettings(orig_network_settings)

        assert(events != None)
        assert(found)

test_registry.register_module("alerts", AlertTests)
