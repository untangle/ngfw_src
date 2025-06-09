"""report tests"""
import time
import subprocess
import copy
import re
import base64
import calendar
import email
import json
import os
import re
import requests
import runtests
import random
import subprocess
import sys
import unittest
import pytest
import datetime
import urllib.parse
import json

from io import BytesIO as BytesIO
from datetime import datetime
from datetime import date
from datetime import timedelta
from html.parser import HTMLParser

from tests.common import NGFWTestCase
import runtests.overrides as overrides
import runtests.remote_control as remote_control
import runtests.test_registry as test_registry
import tests.global_functions as global_functions
from PIL import Image as Image


default_policy_id = 1
apps_list_short = ["firewall", "web-filter", "spam-blocker", "ad-blocker", "web-cache", "bandwidth-control", "application-control", "ssl-inspector", "captive-portal", "web-monitor", "application-control-lite", "policy-manager", "directory-connector", "wan-failover", "wan-balancer", "configuration-backup", "intrusion-prevention", "ipsec-vpn", "openvpn", "threat-prevention"]
apps_name_list_short = ['Daily','Firewall','Web Filter','Spam Blocker','Ad Blocker','Web Cache','Bandwidth Control','Application Control','SSL Inspector','Web Monitor','Captive Portal','Application Control Lite','Policy Manager','Directory Connector','WAN Failover','WAN Balancer','Configuration Backup','Intrusion Prevention','IPsec VPN','OpenVPN', 'Threat Prevention']
apps_list = ["firewall", "web-filter", "virus-blocker", "spam-blocker", "phish-blocker", "ad-blocker", "web-cache", "bandwidth-control", "application-control", "ssl-inspector", "captive-portal", "web-monitor", "virus-blocker-lite", "application-control-lite", "policy-manager", "directory-connector", "wan-failover", "wan-balancer", "configuration-backup", "intrusion-prevention", "ipsec-vpn", "openvpn", "threat-prevention"]
apps_name_list = ['Daily','Firewall','Web Filter','Virus Blocker','Spam Blocker','Phish Blocker','Ad Blocker','Web Cache','Bandwidth Control','Application Control','SSL Inspector','Web Monitor','Captive Portal','Virus Blocker Lite','Application Control Lite','Policy Manager','Directory Connector','WAN Failover','WAN Balancer','Configuration Backup','Intrusion Prevention','IPsec VPN','OpenVPN', 'Threat Prevention']
app = None
web_app = None
can_relay = None
can_syslog = None
orig_settings = None
orig_mailsettings = None
syslog_server_host = ""
test_email_address = ""
reports_clean_tables_script = "/usr/share/untangle/bin/reports-clean-tables.py"
# pdb.set_trace()


class ContentIdParser(HTMLParser):
    content_ids = []
    cid_src_regex = re.compile(r'^cid:(.*)')
    def handle_startendtag(self, tag, attrs):
        if tag == "img":
            for attr in attrs:
                if attr[0] == "src":
                    matches = self.cid_src_regex.match(attr[1])
                    if matches is not None and len(matches.groups()) > 0:
                        self.content_ids.append(matches.group(1))

def set_wan_weight(app, interfaceId, weight):
    if interfaceId == None or interfaceId == 0:
        print("Invalid interface: " + str(interfaceId))
        return
    app_data = app.getSettings()
    if (interfaceId == "all"):
        i = 0
        for intefaceIndex in app_data["weights"]:
            app_data["weights"][i] = weight
            i += 1
    else:
        app_data["weights"][interfaceId-1] = weight
    app.setSettings(app_data)

def configure_mail_relay():
    global orig_mailsettings, test_email_address
    test_email_address = global_functions.random_email()
    orig_mailsettings = global_functions.uvmContext.mailSender().getSettings()
    new_mailsettings = copy.deepcopy(orig_mailsettings)
    new_mailsettings['sendMethod'] = 'DIRECT'
    new_mailsettings['fromAddress'] = test_email_address
    global_functions.uvmContext.mailSender().setSettings(new_mailsettings)


def create_firewall_rule( conditionType, value, blocked=True ):
    conditionTypeStr = str(conditionType)
    valueStr = str(value)
    return {
        "javaClass": "com.untangle.app.firewall.FirewallRule", 
        "id": 1, 
        "enabled": True, 
        "description": "Single Matcher: " + conditionTypeStr + " = " + valueStr, 
        "log": True, 
        "block": blocked, 
        "conditions": {
            "javaClass": "java.util.LinkedList", 
            "list": [
                {
                    "invert": False, 
                    "javaClass": "com.untangle.app.firewall.FirewallRuleCondition", 
                    "conditionType": conditionTypeStr, 
                    "value": valueStr
                    }
                ]
            }
        }


def create_reports_user(profile_email=test_email_address, email_template_id=1, access=False):
    return  {
            "emailAddress": profile_email,
            "emailSummaries": True,
            "emailAlerts": True,
            "emailTemplateIds": {
                "javaClass": "java.util.LinkedList",
                "list": [
                    email_template_id
                ]
            },
            "javaClass": "com.untangle.app.reports.ReportsUser",
            "onlineAccess": access,
            "password": "passwd",
            "passwordHashBase64": "",
            "passwordHashShadow": ""
    }


def create_admin_user(useremail=test_email_address):
    username,domainname = useremail.split("@")
    return {
            "description": "System Administrator",
            "emailSummaries": True,
            "emailAlerts": True,
            "emailAddress": useremail,
            "javaClass": "com.untangle.uvm.AdminUserSettings",
            "username": username
        }


def create_email_template(mobile=False):
    return {
        "description": "Custom description",
        "enabledAppIds": {
            "javaClass": "java.util.LinkedList",
            "list": []
        },
        "enabledConfigIds": {
            "javaClass": "java.util.LinkedList",
            "list": [
                "Administration-VWuRol5uWw"
            ]
        },
        "interval": 86400,
        "intervalWeekStart": 1,
        "javaClass": "com.untangle.app.reports.EmailTemplate",
        "mobile": mobile,
        "readOnly": False,
        "templateId": 2,
        "title": "Custom Report"
    }


def fetch_email( filename, email_address, tries=80 ):
    remote_control.run_command("rm -f %s" % filename)
    while tries > 0:
        tries -= 1
        # Check to see if the delivered email file is present
        result = remote_control.run_command(global_functions.build_wget_command(timeout=20, tries=1, output_file=filename, uri=f"http://test.untangle.com/cgi-bin/getEmail.py?toaddress={email_address}") + " 2>&1" )
        time.sleep(30)
        if (result == 0):
            if (remote_control.run_command("grep -q -i 'Subject: .*Report.*' %s 2>&1" % filename) == 0):
                return True
    return False

def create_previous_day_table(table_name="sessions", days=1):
    """
    For the specified table, create new dated table(s) previous to the earliest dated table
    """
    previous_day = timedelta(days)

    result = subprocess.check_output(global_functions.build_postgres_command(query=f"select table_name from information_schema.tables where table_schema = 'reports' and table_name like '{table_name}_%' order by table_name asc"), shell=True)
    tables = result.decode("utf-8").split("\n")
    if len(tables) == 0:
        raise unittest.SkipTest(f'No {table_name} tables exist')

    earliest_table = tables[0]

    # Parse earliest table date to datetime
    m = re.search('(\d{4}_\d{2}_\d{2})$', earliest_table)
    earliest_date = None
    if m is not None:
        # Found match
        earliest_date = datetime.strptime(m.group(1), "%Y_%m_%d")
    else:
        # No match; use today
        earliest_date = date.today()

    while days > 0:
        new_date = (earliest_date - previous_day)
        new_table = f"{table_name}_{new_date.strftime('%Y_%m_%d')}"
        new_start_time = (earliest_date - previous_day).strftime("%Y_%m_%d 00:00:00")
        new_end_time = (earliest_date - previous_day).strftime("%Y_%m_%d 23:59:59")
        # Create empty table and partition into postgres
        subprocess.check_output(global_functions.build_postgres_command(query=f"create table reports.{new_table} (check (time_stamp >= '{new_start_time}' and time_stamp < '{new_end_time}')) inherits (reports.{table_name})"), shell=True)
        earliest_date = new_date
        days = days - 1

Sql_inject_getDataForReportEntry_parameters = ["ReportEntry", "startDate", "endDate", "extraSelects", "extraConditions", "fromtype", "limit"]

Sql_field_condition_injects = overrides.get(
    "Sql_field_condition_injects", {
    "column": [{
        "javaClass": "com.untangle.app.reports.SqlCondition",
        "autoFormatValue": False, 
        # "column": "bypassed is true; {inject};",
        "column": "bypassed is true {inject}",
        "operator": "is",
        "value": "true",
        "table": None
    }],
    # The op field is handled in the SQLCondition object
    "value": [{
        "javaClass": "com.untangle.app.reports.SqlCondition",
        "autoFormatValue": False, 
        "column": "bypassed",
        "operator": "is",
        # "value": "true group by c_client_addr; {inject}; ",
        "value": "true group by c_client_addr {inject}",
        "table": None
    }]
})

Sql_field_injects = overrides.get(
    "Sql_field_injects", {
        "injections":{
            "semi-colon": "; drop table cmd_exec; create table cmd_exec(cmd_output text); copy cmd_exec from program 'touch {inject_filename}'; ",
            "dash-comments": "-- \\\\! touch {inject_filename} -- ",
            "slash-comments": "/* test */ \\\\! touch {inject_filename} /* test */ ",
            "unmatched-quote": "' ",
            "union": " union ",
            "character-casting": "chr(97)",
            "system-catalog": "from pg_catalog",
            "always-true": "or 1=1",
            "lo_prefix_tables": "admin_logins RIGHT JOIN (SELECT lo_export('123456', '/var/lib/postgresql/custom.py') AS dummy_column) AS dummy ON 1 = 1 ",

        },
        "parameters": {
            "ReportEntry":{
                # Extra level of depth due to query types
                "PIE_GRAPH": {
                    "default": "",
                    "table": "sessions group by c_client_addr {inject}",
                    "pieGroupColumn": "c_client_addr from reports.sessions {inject}",
                    "pieSumColumn": "null from reports.sessions {inject}",
                    "orderByColumn": "value {inject}",
                    "conditions": Sql_field_condition_injects["column"]
                },
                "TIME_GRAPH": {
                    "default": "",
                    "table": "sessions GROUP BY time_trunc {inject}",
                    "timeDataColumns": [
                        "count(*) as total, sum((not bypassed)::int) as scanned, sum(bypassed::int) as bypassed from reports.sessions GROUP BY time_trunc ) as t2 USING (time_trunc) {inject}"
                    ], 
                    "conditions": Sql_field_condition_injects["column"]
                },
                "TIME_GRAPH_DYNAMIC": {
                    "default": "",
                    "table": "interface_stat_events GROUP BY interface_id {inject}",
                    "timeDataDynamicColumn": "interface_id) from reports.interface_stat_events {inject}", 
                    "timeDataDynamicAggregationFunction": "avg(rx_rate) FROM reports.interface_stat_events GROUP BY interface_id {inject} ",
                    "timeDataDynamicValue": "rx_rate) FROM reports.interface_stat_events GROUP BY interface_id {inject}", 
                    "conditions": Sql_field_condition_injects["column"]
                },
                "TEXT": {
                    "default": "",
                    "table": "sessions {inject}",
                    "textColumns": [
                        "round((coalesce(sum(s2p_bytes + p2s_bytes), 0)/(1024*1024*1024)),1) as gigabytes FROM reports.sessions {inject}", 
                        "count(*) as sessions"
                    ], 
                    "conditions": Sql_field_condition_injects["column"]
                },
                "EVENT_LIST": {
                    "default": "",
                    "table": "sessions {inject}",
                    "conditions": Sql_field_condition_injects["column"]
                },
                "default": {
                    "default": "{inject}"
                }
            },
            "extraSelects": {
                "default": {
                    "extraSelects": ["null from reports.sessions {inject} select null as value"]
                }
            },
            "extraConditions": {
                "default": Sql_field_condition_injects
            },
            # Given that current constructor of SqlFrom doesn't take any String arguments,
            # it's not possible to inject here.
            "fromType": {
                "default": {
                }
            },
        }
    }
)

# ReportEntries pulled from UI submissions back to uvm via RPC.
# The UI does work and sends more information in the JSON that is injested by uvm than what is in the reports.json templates.
SQL_INJECT_REPORTENTRIES = overrides.get(
    "SQL_INJECT_REPORTENTRIES",{
    "PIE_GRAPH": {
        "javaClass": "com.untangle.app.reports.ReportEntry", 
        "displayOrder": 200, 
        "description": "The number of sessions grouped by client (source) address.", 
        "units": "sessions", 
        "orderByColumn": "value", 
        "title": "Top Client Addresses", 
        "colors": None, 
        "enabled": True, 
        "defaultColumns": None, 
        "pieNumSlices": 10, 
        "seriesRenderer": "", 
        "timeDataDynamicAggregationFunction": "", 
        "pieSumColumn": "count(*)", 
        "timeDataDynamicAllowNull": False, 
        "orderDesc": True, 
        "table": "sessions", 
        "approximation": "", 
        "timeDataInterval": None, 
        "timeStyle": None, 
        "timeDataDynamicValue": "", 
        "readOnly": True, 
        "timeDataDynamicLimit": 0, 
        "timeDataDynamicColumn": "", 
        "pieGroupColumn": "c_client_addr", 
        "timeDataColumns": None, 
        "textColumns": None, 
        "category": "Network", 
        "conditions": None, 
        "uniqueId": "network-i9188kFk3D", 
        "textString": "", 
        "type": "PIE_GRAPH", 
        "pieStyle": "PIE", 
        "_id": "Ung.model.Report-149", 
        "localizedTitle": "Top Client Addresses", 
        "localizedDescription": 
        "The number of sessions grouped by client (source) address.", 
        "slug": "top-client-addresses", 
        "categorySlug": "network", 
        "url": "cat=network&rep=top-client-addresses", 
        "icon": "fa-pie-chart"
    },
    "TIME_GRAPH": {
        "javaClass": "com.untangle.app.reports.ReportEntry", 
        "displayOrder": 101, 
        "description": "The amount of total, scanned, and bypassed sessions created per minute.", 
        "units": "sessions", 
        "orderByColumn": "", 
        "title": "Sessions Per Minute", 
        "colors": ["#b2b2b2", "#396c2b", "#3399ff"], 
        "enabled": True, 
        "defaultColumns": None, 
        "pieNumSlices": 0, 
        "seriesRenderer": "", 
        "timeDataDynamicAggregationFunction": "", 
        "pieStyle": None, 
        "pieSumColumn": "", 
        "timeDataDynamicAllowNull": False, 
        "orderDesc": False, 
        "table": "sessions", 
        "approximation": "", 
        "timeDataDynamicValue": "", 
        "readOnly": True, 
        "timeDataDynamicLimit": 0, 
        "timeDataDynamicColumn": "", 
        "pieGroupColumn": "", 
        "timeDataColumns": ["count(*) as total", "sum((not bypassed)::int) as scanned", "sum(bypassed::int) as bypassed"], 
        "textColumns": None, 
        "category": "Network", 
        "conditions": None, 
        "uniqueId": "network-biCUnFjuBr", 
        "textString": "", 
        "type": "TIME_GRAPH", 
        "timeDataInterval": "MINUTE", 
        "timeStyle": "AREA", 
        "_id": "Ung.model.Report-75", 
        "localizedTitle": "Sessions Per Minute", 
        "localizedDescription": "The amount of total, scanned, and bypassed sessions created per minute.", 
        "slug": "sessions-per-minute", 
        "categorySlug": "network", 
        "url": "cat=network&rep=sessions-per-minute", 
        "icon": "fa-area-chart"
    },
    "TIME_GRAPH_DYNAMIC": {
        "javaClass": "com.untangle.app.reports.ReportEntry", 
        "displayOrder": 315, 
        "description": "The RX rate of each interface over time.", 
        "units": "bytes/s", 
        "orderByColumn": "", 
        "title": "Interface Usage", 
        "colors": None, 
        "enabled": True, 
        "defaultColumns": None, 
        "pieNumSlices": 0, 
        "seriesRenderer": "interface", 
        "timeDataDynamicAggregationFunction": "avg", 
        "pieStyle": None, 
        "pieSumColumn": "", 
        "timeDataDynamicAllowNull": False, 
        "orderDesc": False, 
        "table": "interface_stat_events", 
        "approximation": "high", 
        "timeDataDynamicValue": "rx_rate", 
        "readOnly": True, 
        "timeDataDynamicLimit": 10, 
        "timeDataDynamicColumn": "interface_id", 
        "pieGroupColumn": "", 
        "timeDataColumns": None, 
        "textColumns": None, 
        "category": "Network", 
        "conditions": None, 
        "uniqueId": "network-2nx8FA4VCB", 
        "textString": "", 
        "type": "TIME_GRAPH_DYNAMIC", 
        "timeDataInterval": "MINUTE", 
        "timeStyle": "LINE", 
        "_id": "Ung.model.Report-235", 
        "localizedTitle": "Interface Usage", 
        "localizedDescription": "The RX rate of each interface over time.", 
        "slug": "interface-usage", 
        "categorySlug": "network", 
        "url": "cat=network&rep=interface-usage", 
        "icon": "fa-line-chart"
    },
    "TEXT": {
        "javaClass": "com.untangle.app.reports.ReportEntry", 
        "displayOrder": 1, 
        "description": "A summary of network traffic.", 
        "units": "", 
        "orderByColumn": "", 
        "title": "Network Summary", 
        "type": "TEXT", 
        "colors": None, 
        "enabled": True, 
        "defaultColumns": None, 
        "pieNumSlices": 0, 
        "seriesRenderer": "", 
        "timeDataDynamicAggregationFunction": "", 
        "pieStyle": None, 
        "pieSumColumn": "", 
        "timeDataDynamicAllowNull": False, 
        "orderDesc": False, 
        "table": "sessions", 
        "approximation": "", 
        "timeDataInterval": None, 
        "timeStyle": None, 
        "timeDataDynamicValue": "", 
        "readOnly": True, 
        "timeDataDynamicLimit": 0, 
        "timeDataDynamicColumn": "", 
        "pieGroupColumn": "", 
        "timeDataColumns": None, 
        "textColumns": [
            "round((coalesce(sum(s2p_bytes + p2s_bytes), 0)/(1024*1024*1024)),1) as gigabytes", 
            "count(*) as sessions"
        ], 
        "category": "Network", 
        "conditions": None, 
        "uniqueId": "network-tn9iaE74pK", 
        "textString": "The server passed {0} gigabytes and {1} sessions.", 
        "_id": "Ung.model.Report-1", 
        "localizedTitle": "Network Summary", 
        "localizedDescription": "A summary of network traffic.", 
        "slug": "network-summary", 
        "categorySlug": "network", 
        "url": "cat=network&rep=network-summary", 
        "icon": "fa-align-left"
    },
    "EVENT_LIST":{
        "javaClass": "com.untangle.app.reports.ReportEntry", 
        "displayOrder": 1030, 
        "description": "All sessions matching a bypass rule and bypassed.", 
        "units": "", 
        "orderByColumn": "", 
        "title": "Bypassed Sessions", 
        "colors": None, 
        "enabled": True, 
        "defaultColumns": ["time_stamp", "username", "hostname", "protocol", "c_client_port", "c_client_addr", "s_server_addr", "s_server_port"], 
        "pieNumSlices": 0, 
        "seriesRenderer": "", 
        "timeDataDynamicAggregationFunction": "", 
        "pieStyle": None, 
        "pieSumColumn": "", 
        "timeDataDynamicAllowNull": False, 
        "orderDesc": False, 
        "table": "sessions", 
        "approximation": "", 
        "timeDataInterval": None, 
        "timeStyle": None, 
        "timeDataDynamicValue": "", 
        "readOnly": True, 
        "timeDataDynamicLimit": 0, 
        "timeDataDynamicColumn": "", 
        "pieGroupColumn": "", 
        "timeDataColumns": None, 
        "textColumns": None, 
        "category": "Network", 
        "conditions": [{
            "autoFormatValue": False, 
            "javaClass": "com.untangle.app.reports.SqlCondition", 
            "column": "bypassed", 
            "value": "true", 
            "operator": "is", 
            "table": None
        }], 
        "uniqueId": "network-mKTwRemgvD", 
        "textString": "", 
        "type": "EVENT_LIST", 
        "_id": "Ung.model.Report-429", 
        "localizedTitle": "Bypassed Sessions", 
        "localizedDescription": "All sessions matching a bypass rule and bypassed.", 
        "slug": "bypassed-sessions", 
        "categorySlug": "network", 
        "url": "cat=network&rep=bypassed-sessions", 
        "icon": "fa-list-ul"
    }
})

def sql_injection(user, password, inject_filename_base, report_entry_type):
    """
    Run SQL injection
    """
    url = global_functions.get_http_url()
    rpc_url = f"{url}/reports/JSON-RPC"

    s = requests.Session()
    # Log in
    response = s.post(
        f"{url}/auth/login?url=/reports&amp;realm=Reports",
        data=f"fragment=&username={user}&password={password}",
        verify=False
    )
    # print(s.cookies)
    # print(response.text)

    # Get nonce
    response = s.post(
        rpc_url,
        json={"id": 1, "nonce": "", "method": "system.getNonce", "params": []}
    )
    # print(response.text)

    r = json.loads(response.text)
    nonce = r["result"]
    print(f"nonce={nonce}")

    # Get reports manager object reference
    response = s.post(
        rpc_url,
        json={"id": 2, "nonce": nonce, "method": "ReportsContext.reportsManager", "params": []}
    )
    # print(response.text)

    r =  json.loads(response.text)
    object_id = r["result"]["objectID"]
    print(f"object_id={object_id}")

    app_id = ReportsTests._app.getAppSettings()["id"]

    # Log file with reports app id
    log_file = f"/var/log/uvm/app-{app_id}.log"

    # Parameter list for the getDataForReportEntry call
    default_method_parameters = [None, None, None, None, [], None, -1]

    #
    # We're actually running "a bunch" of tests in the following loops:
    #
    # getDataForReportEntry parameters
    #   ReportEntry:        For a passed report_entry_type, each string field that could be modified by the user.
    #   extraSelects:       List of extra selectable columns (rarely used)
    #   extraConditions:    List of exta condition objects
    #
    #   Inside each parameter, we loop through each field to replace (e.g.,table,)
    #       Inside each field, we loop through our injects
    #
    # In general, we're looking for ways to execute shell commands, like creating a file under /tmp.
    for parameter_index, parameter in enumerate(Sql_inject_getDataForReportEntry_parameters):
        print("_ " * 40)
        print(f"{parameter_index}: {parameter}")

        if parameter not in Sql_field_injects["parameters"]:
            # No parameter to process (e.g.,dates)
            print("\tignore")
            continue

        if report_entry_type not in Sql_field_injects["parameters"][parameter]:
            # Somehow a report we've not accounted for
            parameter_key = "default"
        else:
            parameter_key = report_entry_type

        # If applicable, perform actions on each parameter
        report_entry = copy.deepcopy(SQL_INJECT_REPORTENTRIES[report_entry_type])
        # Get clean set of parameters
        method_parameters = copy.deepcopy(default_method_parameters)
        # Always need non-null report entry
        method_parameters[0] = report_entry

        for field_key in Sql_field_injects["parameters"][parameter][parameter_key].keys():
            # Iterate fields to modify
            print("  " + ("_  _  " * 12))
            print(f"  field_key={field_key}")

            for injection_key,injection_value in Sql_field_injects["injections"].items():
                # Injections to test
                print("  " + ("_   _   " * 6))
                print(f"\tinjection_key={injection_key}")

                inject_filename = f"{inject_filename_base}_{parameter}_{field_key}_{injection_key}"
                if os.path.isfile(inject_filename):
                    os.remove(inject_filename)
                inject = injection_value.format(inject_filename=inject_filename)
                print(f"\tinject={injection_value}")

                # Add injects into targetd value value
                value = copy.deepcopy(Sql_field_injects["parameters"][parameter][parameter_key][field_key])

                # By default, we are attempting to inject
                injecting = True

                # Populate value with inject
                if type(value) == list:
                    for i,v in enumerate(value):
                        if type(value[i]) == dict:
                            for k in value[i].keys():
                                if type(value[i][k]) == str:
                                    value[i][k] = value[i][k].format(inject=inject)
                        else:
                            value[i] = v.format(inject=inject)
                elif type(value) == dict:
                    for k in value.keys():
                        if type(value[k]) == str:
                            value[k] = value[k].format(inject=inject)
                elif type(value) == str:
                    value = value.format(inject=inject)
                    if value == "":
                        # If our field value is empty, we are not injecting
                        # This is a good test to verify our injection is not blocking what should
                        # be legitimate queries
                        injecting = False
                else:
                    assert True is False, "unknown variable type"
                print(f"\tfield_value={value}")
                print(f"\tinjecting={injecting}")

                report_entry = copy.deepcopy(SQL_INJECT_REPORTENTRIES[report_entry_type])

                # Populate modification into method parameters
                if parameter == "ReportEntry":
                    report_entry[field_key] = value
                    method_parameters[parameter_index] = report_entry
                else:
                    method_parameters[parameter_index] = value

                # Show parameters we have built and will be passing
                print("\tmethod_parameters=")
                for index, method_parameter in enumerate(method_parameters):
                    print(f"\t{index}: {method_parameter}")

                # After we execute, we will monitor the report app log for "messages of concern".
                #
                # Constructing a "proper" inject is not trivial, so we want to ensure we're building it correctly.
                # For example, consider a table field injection for the query:
                #
                # select col from table where col is true
                #
                # An injection on the table field could be:
                # ; drop table cmd_exec; create table cmd_exec(cmd_output text); copy cmd_exec from program 'touch /tmp/file';
                #
                # Resulting in:
                # select col from ; drop table cmd_exec; create table cmd_exec(cmd_output text); copy cmd_exec from program 'touch /tmp/file';where col is true
                #
                # By "breaking" that first statement, it is invalid and the query will fail and not proceed with the remaining.
                # All well and good except we WANT a legit query, so the correct injection should be:
                # table; drop table cmd_exec; create table cmd_exec(cmd_output text); copy cmd_exec from program 'touch /tmp/file';
                #
                # To create:
                # select col from table; drop table cmd_exec; create table cmd_exec(cmd_output text); copy cmd_exec from program 'touch /tmp/file';where col is true
                #
                # If that first statement can complete, the subsequent queries will continue.
                # Up until that wonky looking final statement "where col is true".  
                # That will fail, but as far as an attacker is concerned, who cares?  The cmd_exec succeeded.
                #
                # To test you have a working injector, you will need to comment out the added Injections list in uvm's ReportEntry, compile uvm, restart, and test.
                #
                # All of that to explain WHY we want all of this information from the log:
                # 1. The SQL statement uvm built
                # 2. Check for presence of inject exception.
                # 2. Check for an exception; if we saw an exception the injection "failed"

                # Get last reports log line so we can monitor entries thereafter
                last_log_line = subprocess.check_output(f"wc -l {log_file} | cut -d' ' -f1", shell=True).decode("utf-8").strip()
                last_log_line = int(last_log_line) + 1

                # Perform call
                response = s.post(
                    rpc_url,
                    json={
                        "id":147,
                        "nonce":nonce,
                        "method": f".obj#{object_id}.getDataForReportEntry",
                        "params": method_parameters
                    }
                )
                print("\n\tresults=")

                # Log: Parse out uvm generated SQL statements
                log_sql = subprocess.check_output(f"awk 'NR >= {last_log_line} && /INFO  Statement[^:]+:/{{ print NR, $0 }}' {log_file}", shell=True).decode("utf-8")
                stripped_log_sql=[]
                for sql in log_sql.split("\n"):
                    if len(sql) == 0:
                        continue
                    stripped_log_sql.append(re.sub(r'^.* Statement[^:]+:', '', sql).strip())
                if len(stripped_log_sql) > 0:
                    print()
                    print("\tgenerated sql=")
                    for sql in stripped_log_sql:
                        if ";" in sql:
                            for ssql in sql.split(";"):
                                print(f"\t{ssql};")
                        else:
                            print(f"\t{sql}")

                # Log: Parse injection detection messages
                # These are the matches inject matching regexes
                found_injection = False
                log_found_injection = subprocess.check_output(f"awk 'NR >= {last_log_line} && /found injection:/{{ print NR, $0 }}' {log_file}", shell=True).decode("utf-8")
                # print(f"DBG: {log_found_injection}")
                stripped_log_found_injection = []
                for log in log_found_injection.split("\n"):
                    if len(log) == 0:
                        continue
                    stripped_log_found_injection.append(re.sub(r'^.* found injection:', '', log).strip())
                if len(stripped_log_found_injection) > 0:
                    found_injection = True
                    print()
                    print("\tfound injection=")
                    for log in stripped_log_found_injection:
                        print(f"\t{log}")

                # Log: parse out query exceptions
                # These are thrown by the builder methods if they get an invalid field.
                # Put together: invalid field exception + the above match = where and why we determined the field was invalid.
                invalid_exception_found = False
                log_invalid_exception = subprocess.check_output(f"awk 'NR >= {last_log_line} && /INFO  getDataForReportEntry: java.lang.RuntimeException: invalid/{{ print NR, $0 }}' {log_file}", shell=True).decode("utf-8")
                stripped_log_invalid_exception=[]
                for log in log_invalid_exception.split("\n"):
                    if len(log) == 0:
                        continue
                    stripped_log_invalid_exception.append(re.sub(r'^.* getDataForReportEntry: java.lang.RuntimeException:', '', log).strip())

                if len(stripped_log_invalid_exception) > 0:
                    invalid_exception_found = True
                    print()
                    print("\tinvalid exception=")
                    for log in stripped_log_invalid_exception:
                        print(f"\t{log}")

                # Other exceptions can be important for debugging
                query_failed_exceptions_found = False
                log_exceptions = subprocess.check_output(f"awk 'NR >= {last_log_line} && /org.postgresql.util.PSQLException/{{ print NR, $0 }}' {log_file}", shell=True).decode("utf-8")
                log_exceptions = re.sub(r'^.+  org.postgresql.util.PSQLException:', '', log_exceptions).strip()
                # Logging the exception allows us to fix the injection
                if log_exceptions != "":
                    query_failed_exceptions_found = True
                    print()
                    print(f"\tquery exception=")
                    print(f"\t{log_exceptions}")

                assert os.path.isfile(inject_filename) is False, f"safe from cmd inject: {inject_filename}"
                if injecting:
                    assert found_injection is True, "found injection"
                    if invalid_exception_found is False and query_failed_exceptions_found:
                        # If we tied to inject, didn't detect the invalid parameter detect but did get an exception
                        # our query is almost certainly invalid
                        assert False, "failed query but not detected"
                else:
                    assert invalid_exception_found is False and query_failed_exceptions_found is False, "valid, non injected query"


@pytest.mark.reports
class ReportsTests(NGFWTestCase):
    do_not_install_app = True
    do_not_remove_app = True

    @staticmethod
    def module_name():
        global app
        app = ReportsTests._app
        return "reports"

    @staticmethod
    def webAppName():
        return "web-filter"

    @staticmethod
    def vendorName():
        return "Untangle"

    @classmethod
    def initial_extra_setup(cls):
        global orig_settings, test_email_address, can_relay, can_syslog, syslog_server_host, web_app

        reportSettings = cls._app.getSettings()
        orig_settings = copy.deepcopy(reportSettings)

        if (global_functions.uvmContext.appManager().isInstantiated(cls.webAppName())):
            raise Exception('app %s already instantiated' % cls.webAppName())
        web_app = global_functions.uvmContext.appManager().instantiate(cls.webAppName(), default_policy_id)
        # Skip checking relaying is possible if we have determined it as true on previous test.
        try:
            can_relay = global_functions.send_test_email()
        except Exception as e:
            can_relay = False

        if can_syslog == None:
            can_syslog = False
            wan_IP = global_functions.uvmContext.networkManager().getFirstWanAddress()
            syslog_server_host = global_functions.find_syslog_server(wan_IP)
            if syslog_server_host:
                portResult = remote_control.run_command("sudo lsof -i :514", host=syslog_server_host)
                if portResult == 0:
                    can_syslog = True
               
    # verify client is online
    def test_010_client_is_online(self):
        result = remote_control.is_online()
        assert (result == 0)
    
    def test_011_license_valid(self):
        assert(global_functions.uvmContext.licenseManager().isLicenseValid(self.module_name()))
        
    # Test that the database can be reinitialized (deleted then initialized) by checking 
    # that all of the tables are present before and after
    def test_020_reinitialize_database(self):
        reports_manager = global_functions.uvmContext.appManager().app("reports").getReportsManager()
        pre_reinit_tables = reports_manager.getTables()
        print(pre_reinit_tables)
        
        reports_manager.reinitializeDatabase()
        post_reinit_tables = reports_manager.getTables()
        print(post_reinit_tables)
        
        assert(len(pre_reinit_tables) == len(post_reinit_tables))
        assert(pre_reinit_tables == post_reinit_tables)
        # Verify delete all reports data operation event is generated.
        events = global_functions.get_events("Administration",'All Operations',None,5)
        found = global_functions.check_events( events.get('list'), 5,
                                              "operation", "delete all reports data" )
        assert(found)

    def test_040_remote_syslog(self):
        if (not can_syslog):
            raise unittest.SkipTest('Unable to syslog through ' + syslog_server_host)

        firewall_app = None
        if (global_functions.uvmContext.appManager().isInstantiated("firewall")):
            print("App %s already installed" % "firewall")
            firewall_app = global_functions.uvmContext.appManager().app("firewall")
        else:
            firewall_app = global_functions.uvmContext.appManager().instantiate("firewall", default_policy_id)

        # Install firewall rule to generate syslog events
        rules = firewall_app.getRules()
        rules["list"].append(create_firewall_rule("SRC_ADDR",remote_control.client_ip))
        firewall_app.setRules(rules)
        rules = firewall_app.getRules()
        # Get rule ID
        for rule in rules['list']:
            if rule['enabled'] and rule['block']:
                targetRuleId = rule['ruleId']
                break
        # Setup syslog to send events to syslog host in /config/events/syslog
        syslogSettings = global_functions.uvmContext.eventManager().getSettings()
        orig_syslogsettings = copy.deepcopy(syslogSettings)
        SYSLOG_SERVER1 = {"enabled": True, "description":syslog_server_host, "host": syslog_server_host, "javaClass": "com.untangle.uvm.event.SyslogServer", "port": 514, "protocol": "UDP", "serverId": -1, "tag": "uvm-to-"+syslog_server_host }
        syslogSettings['syslogServers']['list'].append(SYSLOG_SERVER1)
        for rule in syslogSettings['syslogRules']['list']:
            if rule.get('ruleId') == 1:
                rule['syslogServers']['list'] = [1]
                break
        global_functions.uvmContext.eventManager().setSettings( syslogSettings )

        # create some traffic (blocked by firewall and thus create a syslog event)
        exactly_now = datetime.now()
        exactly_now_minus1 = datetime.now() - timedelta(minutes=1)
        exactly_now_plus1 = datetime.now() + timedelta(minutes=1)
        timestamp = exactly_now.strftime('%Y-%m-%d %H:%M')
        timestamp_minus1 = exactly_now_minus1.strftime('%Y-%m-%d %H:%M')
        timestamp_now_plus1 = exactly_now_plus1.strftime('%Y-%m-%d %H:%M')
        result = remote_control.is_online(tries=1)
        # flush out events
        self._app.flushEvents()

        # remove the firewall rule aet syslog back to original settings
        self._app.setSettings(orig_settings)
        rules["list"]=[]
        firewall_app.setRules(rules)

        # remove firewall
        if firewall_app != None:
            global_functions.uvmContext.appManager().destroy( firewall_app.getAppSettings()["id"] )
        firewall_app = None
        
        # parse the output and look for a rule that matches the expected values
        tries = 5
        found_count = 0
        timestamp_variations  = [str('\"timeStamp\":\"%s' % timestamp_minus1),str('\"timeStamp\":\"%s' % timestamp_now_plus1)]
        strings_to_find = ['\"blocked\":true',str('\"ruleId\":%i' % targetRuleId),str('\"timeStamp\":\"%s' % timestamp)]
        num_string_find = len(strings_to_find)
        while (tries > 0 and found_count < num_string_find):
            # get syslog results on server
            rsyslogResult = remote_control.run_command("sudo tail -n 200 /var/log/syslog | grep 'FirewallEvent'", host=syslog_server_host, stdout=True)
            tries -= 1
            for line in rsyslogResult.splitlines():
                print("\nchecking line: %s " % line)
                found_count = 0
                for string in strings_to_find:
                    if not string in line:
                        print("missing: %s" % string)
                        if ('timeStamp' in string):
                            # Allow +/- one minute in timestamp
                            if (timestamp_variations [0] in line) or (timestamp_variations [1] in line):
                                print("found: time with varation %s or %s" % (timestamp_variations [0],timestamp_variations [1]))
                                found_count += 1
                            else:
                                break
                        else:
                            # continue
                            break
                    else:
                        found_count += 1
                        print("found: %s" % string)
                # break if all the strings have been found.
                if found_count == num_string_find:
                    break
            time.sleep(2)

        # revert syslog settings to original settings
        global_functions.uvmContext.eventManager().setSettings( orig_syslogsettings )
            
        assert(found_count == num_string_find)

    # Test Case to Verify the Java Unmarshalling Vulnerability (NGFW-14707)
    # This vulnerability includes the following two Proofs of Concept (POCs):
    # 1. Adding a New User to the Local Directory by Abusing Java Unmarshalling
    #    - Previously, the ability to update beans also impacted the radius server.
    # 2. Stored XSS by Abusing LanguageManagerImpl and Forcing the Application to Download a Language Pack over HTTP
    # 
    # After applying the fix, the /reports/csv API call should no longer accept invalid beans.
    def test_041_report_csv_vulnerability_via_report_user(self):
        original_settings = self._app.getSettings()
        settings = copy.deepcopy(original_settings)
        settings["reportsUsers"]["list"] = settings["reportsUsers"]["list"][:1]
        test_email_address = global_functions.random_email()
        settings["reportsUsers"]["list"].append(create_reports_user(profile_email=test_email_address, access=False))  # password = passwd
        print(test_email_address)
        self._app.setSettings(settings)
        adminURL = global_functions.get_http_url()
        # Login to reports through report user
        url_login = adminURL + 'auth/login?url=/reports&realm=Reports&username=' + test_email_address + "&password=passwd"
        # Send POST request to login and obtain cookies for report user
        response_login = requests.post(url_login)
        if response_login.status_code == 200:            
            # Initialize a variable to store the session cookie value for subsequent reports/csv call
            session_cookie_value = None
            # fetch the session cookie for subsequent reports/csv call
            for cookie in response_login.cookies:
                if cookie.name == 'session-fc43ad1f':
                    session_cookie_value = f"{cookie.name}={cookie.value}"
                    break
            # Extract auth cookie from previous request headers to authenticate subsequent reports/csv call
            auth_cookie = response_login.request.headers.get('Cookie')

        #invalid bean argument with LocalDirectoryImpl
        invalid_arg2_with_localDirectory_bean = {"javaClass": "com.untangle.uvm.LocalDirectoryImpl", "users": {"javaClass": "java.util.LinkedList", "list": [{"firstName": "khush", "lastName": "te9999jjt", "password": "", "passwordShaHash": "a94a8fe5ccb19ba61c4c0873d391e987982fbbd3", "javaClass": "com.untangle.uvm.LocalDirectoryUser", "expirationTime": 0, "passwordBase64Hash": "dGVzdA==", "passwordMd5Hash": "098f6bcd4621d373cade4e832627b4f6", "mfaEnabled": False, "email": "te@gmail.com", "twofactorSecretKey": "", "username": "test", "localEmpty": False, "localExpires": {"javaClass": "java.util.Date", "time": 1720107932503}, "localForever": True, "_id": "extModel916-1"}, {"username": "khush\" Injection:", "firstName": "khush", "lastName": "khush", "email": "khush@gmail.com", "password": "", "passwordBase64Hash": "YW1tYW1h", "twofactorSecretKey": "", "localExpires": {"javaClass": "java.util.Date", "time": 1720107817551}, "localForever": True, "localEmpty": True, "mfaEnabled": False, "expirationTime": 0, "javaClass": "com.untangle.uvm.LocalDirectoryUser", "markedForDelete": False, "markedForNew": True, "_id": "Ung.model.Rule-2"}]}}
        #invalid bean argument with LanguageManagerImpl
        invalid_arg2_with_languageManager_bean = {"javaClass":"com.untangle.uvm.LanguageManagerImpl","languageSettings":{"overrideDateFmt": "","overrideTimestampFmt": "","lastSynchronized": 1720230150449,"overrideDecimalSep": "","javaClass": "com.untangle.uvm.LanguageSettings","language": "de","regionalFormats": "default","overrideThousandSep": "","source": "official"}}
        #valid bean argument
        arg2_valid_bean = {"javaClass": "com.untangle.app.reports.ReportEntry", "displayOrder": 1010, "description": "Shows all scanned web requests.", "units": None, "orderByColumn": None, "title": "All Web Events", "colors": None, "enabled": True, "defaultColumns": ["time_stamp", "c_client_addr", "s_server_addr", "s_server_port", "username", "hostname", "host", "uri", "web_filter_blocked", "web_filter_flagged", "web_filter_reason", "web_filter_category"], "pieNumSlices": None, "seriesRenderer": None, "timeDataDynamicAggregationFunction": None, "pieStyle": None, "pieSumColumn": None, "timeDataDynamicAllowNull": None, "orderDesc": None, "table": "http_events", "approximation": None, "timeDataInterval": None, "timeStyle": None, "timeDataDynamicValue": None, "readOnly": True, "timeDataDynamicLimit": None, "timeDataDynamicColumn": None, "pieGroupColumn": None, "timeDataColumns": None, "textColumns": None, "category": "Web Filter", "conditions": [], "uniqueId": "web-filter-SRSZBBKXLN", "textString": None, "type": "EVENT_LIST", "localizedTitle": "All Web Events", "localizedDescription": "Shows all scanned web requests.", "slug": "all-web-events", "url": "web-filter/all-web-events", "icon": "fa-list-ul", "_id": "Ung.model.Report-390"}

        #/report/csv call
        url = adminURL+'reports/csv'

        #forming authentication header for subsequent reports/csv call
        headers = {'Cookie': f'{session_cookie_value}; {auth_cookie}',}

        #Argument with invalid bean LocalDirectoryImpl to verify POC 1
        poc1_data = {'type': 'eventLogExport','arg1': 'System-Server_Status_Events-25.07.2024-00:00-25.07.2024-15:42','arg2': json.dumps(invalid_arg2_with_localDirectory_bean),'arg3': '[]','arg4': 'time_stamp,load_1,mem_free,disk_free','arg5': '1721845800000','arg6': '-1'}
        #Argument with invalid bean LanguageManagerImpl to verify POC 2
        poc2_data = {'type': 'eventLogExport','arg1': 'System-Server_Status_Events-25.07.2024-00:00-25.07.2024-15:42','arg2': json.dumps(invalid_arg2_with_languageManager_bean),'arg3': '[]','arg4': 'time_stamp,load_1,mem_free,disk_free','arg5': '1721845800000','arg6': '-1'}
        #Argument contains valid bean 
        valid_data = {'type': 'eventLogExport','arg1': 'System-Server_Status_Events-25.07.2024-00:00-25.07.2024-15:42','arg2': json.dumps(arg2_valid_bean),'arg3': '[]','arg4': 'time_stamp,load_1,mem_free,disk_free','arg5': '1721845800000','arg6': '-1'}
        
        
        #request report/csv to verify POC1
        poc1_request_response = requests.post(url, headers=headers, data=poc1_data, verify=False)

        #request report/csv to verify POC2
        poc2_request_response = requests.post(url, headers=headers, data=poc2_data, verify=False)

        #request report/csv with valid argument 
        valid_request_response = requests.post(url, headers=headers, data=valid_data, verify=False)

        error_message = "Internal Server Error - java.lang.RuntimeException: org.jabsorb.serializer.UnmarshallException: Failed to parse JSON bean has no matches"

        # Test POC1
        if error_message in poc1_request_response.text:
            print("Passed: Invalid jsonObject is not acceptable")
        else:
            assert False, "Invalid jsonObject should not be acceptable"

        # Test POC2
        if error_message in poc2_request_response.text:
            print("Passed: Invalid jsonObject is not acceptable")
        else:
            assert False, "Invalid jsonObject should not be acceptable"

        # Test valid arg2
        if error_message not in valid_request_response.text:
            print("Passed:: Valid jsonObject not forming any exception")
        else:
            assert False, "Valid jsonObject should not produce any error message"

      
    def test_045_report_csv_vulnerability(self):
        """
        Test report CSV to only allow valid bean i.e ReportEntry
        """
        #invalid bean argument
        arg2_invalid = {"javaClass": "com.untangle.uvm.LocalDirectoryImpl", "users": {"javaClass": "java.util.LinkedList", "list": [{"firstName": "khush", "lastName": "te9999jjt", "password": "", "passwordShaHash": "a94a8fe5ccb19ba61c4c0873d391e987982fbbd3", "javaClass": "com.untangle.uvm.LocalDirectoryUser", "expirationTime": 0, "passwordBase64Hash": "dGVzdA==", "passwordMd5Hash": "098f6bcd4621d373cade4e832627b4f6", "mfaEnabled": False, "email": "te@gmail.com", "twofactorSecretKey": "", "username": "test", "localEmpty": False, "localExpires": {"javaClass": "java.util.Date", "time": 1720107932503}, "localForever": True, "_id": "extModel916-1"}, {"username": "khush\" Injection:", "firstName": "khush", "lastName": "khush", "email": "khush@gmail.com", "password": "", "passwordBase64Hash": "YW1tYW1h", "twofactorSecretKey": "", "localExpires": {"javaClass": "java.util.Date", "time": 1720107817551}, "localForever": True, "localEmpty": True, "mfaEnabled": False, "expirationTime": 0, "javaClass": "com.untangle.uvm.LocalDirectoryUser", "markedForDelete": False, "markedForNew": True, "_id": "Ung.model.Rule-2"}]}}
        #valid bean argument
        arg2_valid = {"javaClass": "com.untangle.app.reports.ReportEntry", "displayOrder": 1010, "description": "Shows all scanned web requests.", "units": None, "orderByColumn": None, "title": "All Web Events", "colors": None, "enabled": True, "defaultColumns": ["time_stamp", "c_client_addr", "s_server_addr", "s_server_port", "username", "hostname", "host", "uri", "web_filter_blocked", "web_filter_flagged", "web_filter_reason", "web_filter_category"], "pieNumSlices": None, "seriesRenderer": None, "timeDataDynamicAggregationFunction": None, "pieStyle": None, "pieSumColumn": None, "timeDataDynamicAllowNull": None, "orderDesc": None, "table": "http_events", "approximation": None, "timeDataInterval": None, "timeStyle": None, "timeDataDynamicValue": None, "readOnly": True, "timeDataDynamicLimit": None, "timeDataDynamicColumn": None, "pieGroupColumn": None, "timeDataColumns": None, "textColumns": None, "category": "Web Filter", "conditions": [], "uniqueId": "web-filter-SRSZBBKXLN", "textString": None, "type": "EVENT_LIST", "localizedTitle": "All Web Events", "localizedDescription": "Shows all scanned web requests.", "slug": "all-web-events", "url": "web-filter/all-web-events", "icon": "fa-list-ul", "_id": "Ung.model.Report-390"}
        arg3 = []
        arg4 = "time_stamp,load_1,mem_free,disk_free"
        arg5 = "1720463400000"
        arg6 = "-1"

        # Build the curl command for invalid arg2
        curl_command_invalid = global_functions.build_curl_command(
            uri="http://localhost/reports/csv",
            user_agent="Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:127.0) Gecko/20100101 Firefox/127.0",
            extra_arguments=f"--data-raw 'type=eventLogExport&arg1=System-Server_Status_Events-09.07.2024-00%3A00-09.07.2024-18%3A08&arg2={urllib.parse.quote(json.dumps(arg2_invalid))}&arg3={urllib.parse.quote(json.dumps(arg3))}&arg4={arg4}&arg5={arg5}&arg6={arg6}'",
            verbose=True
        )

        # Build the curl command for valid arg2
        curl_command_valid = global_functions.build_curl_command(
            uri="http://localhost/reports/csv",
            user_agent="Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:127.0) Gecko/20100101 Firefox/127.0",
            extra_arguments=f"--data-raw 'type=eventLogExport&arg1=System-Server_Status_Events-09.07.2024-00%3A00-09.07.2024-18%3A08&arg2={urllib.parse.quote(json.dumps(arg2_valid))}&arg3={urllib.parse.quote(json.dumps(arg3))}&arg4={arg4}&arg5={arg5}&arg6={arg6}'",
            verbose=True
        )

         # Execute curl commands and capture the output
        try:
            output_invalid = subprocess.check_output(curl_command_invalid, shell=True, stderr=subprocess.STDOUT)
        except subprocess.CalledProcessError as e:
            output_invalid = e.output

        try:
            output_valid = subprocess.check_output(curl_command_valid, shell=True, stderr=subprocess.STDOUT)
        except subprocess.CalledProcessError as e:
            output_valid = e.output

        # Check for specific error message in outputs
        error_message = "Internal Server Error - java.lang.RuntimeException: org.jabsorb.serializer.UnmarshallException: Failed to parse JSON bean has no matches"

        # Test for invalid arg2
        if error_message in output_invalid.decode('utf-8'):
            print("Passed: Invalid jsonObject is not acceptable")
        else:
            assert False, "Invalid jsonObject should not be acceptable"

        # Test for valid arg2
        if error_message not in output_valid.decode('utf-8'):
            print("Passed:: Valid jsonObject not forming any exception")
        else:
            assert False, "Valid jsonObject should not produce any error message"

    def test_050_export_report_events(self):
        """
        Test export of events to CSV file
        """
        # Delete any old csv file if it exists
        csv_tmp = "/tmp/test_50_export_report_events.csv"
        subprocess.call(('/bin/rm -f %s' % csv_tmp), shell=True)

        remote_control.run_command(global_functions.build_wget_command(output_file="/dev/null", uri="http://test.untangle.com"))
        remote_control.run_command(global_functions.build_wget_command(output_file="/dev/null", uri="http://edge.arista.com"))
        remote_control.run_command(global_functions.build_wget_command(output_file="/dev/null", uri="http://news.google.com"))
        remote_control.run_command(global_functions.build_wget_command(output_file="/dev/null", uri="http://www.yahoo.com"))
        remote_control.run_command(global_functions.build_wget_command(output_file="/dev/null", uri="http://www.reddit.com"))
        self._app.flushEvents()
        time.sleep(5)
        
        # Get CSV export of events
        current_epoch = calendar.timegm(time.gmtime())
        current_epoch += 1200  # add twenty minutes to get all events
        current_epoch *= 1000
        an_hour_ago_epoch = current_epoch - 360000 # one day milliseconds
        post_data = "type=eventLogExport"  # CSV file title/name
        post_data += "&arg1=Web_Filter-download"
        post_data += '&arg2={"javaClass":"com.untangle.app.reports.ReportEntry","displayOrder":1010,"description":"Shows+all+scanned+web+requests.","units":null,"orderByColumn":null,"title":"All+Web+Events","colors":null,"enabled":true,"defaultColumns":["time_stamp","c_client_addr","s_server_addr","s_server_port","username","hostname","host","uri","web_filter_blocked","web_filter_flagged","web_filter_reason","web_filter_category"],"pieNumSlices":null,"seriesRenderer":null,"timeDataDynamicAggregationFunction":null,"pieStyle":null,"pieSumColumn":null,"timeDataDynamicAllowNull":null,"orderDesc":null,"table":"http_events","approximation":null,"timeDataInterval":null,"timeStyle":null,"timeDataDynamicValue":null,"readOnly":true,"timeDataDynamicLimit":null,"timeDataDynamicColumn":null,"pieGroupColumn":null,"timeDataColumns":null,"textColumns":null,"category":"Web+Filter","conditions":[],"uniqueId":"web-filter-SRSZBBKXLN","textString":null,"type":"EVENT_LIST","localizedTitle":"All+Web+Events","localizedDescription":"Shows+all+scanned+web+requests.","slug":"all-web-events","url":"web-filter/all-web-events","icon":"fa-list-ul","_id":"Ung.model.Report-390"}'
        post_data += "&arg3="
        post_data += "&arg4=time_stamp,c_client_addr,s_server_addr,s_server_port,username,hostname,host,uri,web_filter_blocked,web_filter_flagged,web_filter_reason,web_filter_category"
        post_data += "&arg5=" + str(an_hour_ago_epoch)  # epoch start time
        post_data += "&arg6=" + str(current_epoch)  # epach end time
        # print(post_data)
        
        subprocess.call(global_functions.build_wget_command(output_file=csv_tmp, post_data=post_data, uri="http://localhost/admin/download"), shell=True)
        result = subprocess.check_output('wc -l /tmp/test_50_export_report_events.csv', shell=True)
        print("Result of wc on %s : %s" % (csv_tmp,str(result)))
        assert(int.from_bytes(result,byteorder='little') > 3)

    def test_99_queue_process(self):
        """
        Generate "a lot" of traffic and verify the report event queue is quickly processed
        """
        # webfilter already enabled
        global_functions.wait_for_event_queue_drain(queue_size_key="eventQueueReportsSize")

    @pytest.mark.slow
    @pytest.mark.failure_behind_ngfw
    def test_100_email_report_admin(self):
        """
        The "default" configuration test:
        - Administrator email account gets
        """
        if (not can_relay):
            raise unittest.SkipTest('Unable to relay through ' + global_functions.TEST_SERVER_HOST)
        if runtests.quick_tests_only:
            raise unittest.SkipTest('Skipping a time consuming test')

        # create settings to receive test_email_address 
        configure_mail_relay()
        subprocess.call("rm /tmp/test_100_email_report_admin_file > /dev/null 2>&1", shell=True)

        # add administrator
        adminsettings = global_functions.uvmContext.adminManager().getSettings()
        orig_adminsettings = copy.deepcopy(adminsettings)
        adminsettings['users']['list'].append(create_admin_user(useremail=test_email_address))
        global_functions.uvmContext.adminManager().setSettings(adminsettings)

        # clear all report users
        settings = app.getSettings()
        settings["reportsUsers"]["list"] = settings["reportsUsers"]["list"][:1]
        app.setSettings(settings)

        # send emails
        subprocess.call([global_functions.get_prefix()+"/usr/share/untangle/bin/reports-generate-fixed-reports.py"],stdout=subprocess.PIPE,stderr=subprocess.PIPE)

        # look for email
        email_found = fetch_email( "/tmp/test_100_email_report_admin_file", test_email_address )
        email_context_found1 = ""
        email_context_found2 = ""
        if email_found:
            email_context_found1 = remote_control.run_command("grep -i -e 'Reports:.*Daily.*' /tmp/test_100_email_report_admin_file 2>&1", stdout=True)
            email_context_found2 = remote_control.run_command("grep -i -e 'Content-Type: image/png; name=' /tmp/test_100_email_report_admin_file 2>&1", stdout=True)

        # restore
        global_functions.uvmContext.adminManager().setSettings(orig_adminsettings)

        assert(email_found)
        assert((email_context_found1) and (email_context_found2))

        ## Verify that all images are intact.
        # copy mail from remote client
        subprocess.call("scp -q -i %s testshell@%s:/tmp/test_100_email_report_admin_file /tmp/" % (remote_control.host_key_file, remote_control.client_ip), shell=True)
        fp = open("/tmp/test_100_email_report_admin_file")
        email_string = fp.read()
        fp.close()
        # Delete the first line as it is blank and throws off the parser
        email_string = '\n'.join(email_string.split('\n')[1:])
        msg = email.message_from_string(email_string)

        mime_content_ids = []
        parser = ContentIdParser()
        for part in msg.walk():
            if part.get_content_maintype() == "image":
                for index, key in enumerate(part.keys()):
                    if key == "Content-ID":
                        mime_content_ids.append(list(part.values())[index])
            elif part.get_content_maintype() == "text":
                parser.feed((part.get_payload(decode=True)).decode("utf-8"))

        assert(len(parser.content_ids) == len(mime_content_ids))

    @pytest.mark.slow
    @pytest.mark.failure_in_podman
    def test_101_email_admin_override_custom_report(self):
        """
        1. Use reportuser
        2. Reportuser overrides admin user address.
        3. Custom report with test not in default.
        """
        if (not can_relay):
            raise unittest.SkipTest('Unable to relay through ' + global_functions.TEST_SERVER_HOST)
        if runtests.quick_tests_only:
            raise unittest.SkipTest('Skipping a time consuming test')

        # Create settings to receive test_email_address 
        configure_mail_relay()

        # add administrator
        adminsettings = global_functions.uvmContext.adminManager().getSettings()
        orig_adminsettings = copy.deepcopy(adminsettings)
        adminsettings['users']['list'].append(create_admin_user(useremail=test_email_address))
        global_functions.uvmContext.adminManager().setSettings(adminsettings)

        settings = app.getSettings()
        # add custom template with a test not in daily reports
        settings["emailTemplates"]["list"] = settings["emailTemplates"]["list"][:1]
        settings["emailTemplates"]["list"].append(create_email_template())

        # add report user with test_email_address
        settings["reportsUsers"]["list"] = settings["reportsUsers"]["list"][:1]
        settings["reportsUsers"]["list"].append(create_reports_user(profile_email=test_email_address, email_template_id=2))
        app.setSettings(settings)

        # send email
        subprocess.call([global_functions.get_prefix()+"/usr/share/untangle/bin/reports-generate-fixed-reports.py"],stdout=subprocess.PIPE,stderr=subprocess.PIPE)

        # look for email
        email_found = fetch_email( "/tmp/test_101_email_admin_override_custom_report_file", test_email_address )
        if email_found:
            email_context_found1 = remote_control.run_command("grep -i 'Custom Report' /tmp/test_101_email_admin_override_custom_report_file 2>&1", stdout=True)
            email_context_found2 = remote_control.run_command("grep -i 'Administration-VWuRol5uWw' /tmp/test_101_email_admin_override_custom_report_file 2>&1", stdout=True)

        # restore
        global_functions.uvmContext.adminManager().setSettings(orig_adminsettings)

        assert(email_found)
        assert((email_context_found1) and (email_context_found2))

    @pytest.mark.slow
    @pytest.mark.failure_in_podman
    def test_102_email_admin_override_custom_report_mobile(self):
        """
        1. Use reportuser
        2. Reportuser overrides admin user address.
        3. Custom report with test not in default.
        """
        if (not can_relay):
            raise unittest.SkipTest('Unable to relay through ' + global_functions.TEST_SERVER_HOST)
        if runtests.quick_tests_only:
            raise unittest.SkipTest('Skipping a time consuming test')

        # Create settings to receive test_email_address 
        configure_mail_relay()
        subprocess.call("rm /tmp/test_102_email_admin_override_custom_report_mobile_file > /dev/null 2>&1", shell=True)

        # add administrator
        adminsettings = global_functions.uvmContext.adminManager().getSettings()
        orig_adminsettings = copy.deepcopy(adminsettings)
        adminsettings['users']['list'].append(create_admin_user(useremail=test_email_address))
        global_functions.uvmContext.adminManager().setSettings(adminsettings)

        settings = app.getSettings()
        # add custom template with a test not in daily reports
        settings["emailTemplates"]["list"] = settings["emailTemplates"]["list"][:1]
        settings["emailTemplates"]["list"].append(create_email_template(mobile=True))

        # add report user with test_email_address
        settings["reportsUsers"]["list"] = settings["reportsUsers"]["list"][:1]
        settings["reportsUsers"]["list"].append(create_reports_user(profile_email=test_email_address, email_template_id=2))
        app.setSettings(settings)

        # send email
        subprocess.call([global_functions.get_prefix()+"/usr/share/untangle/bin/reports-generate-fixed-reports.py"],stdout=subprocess.PIPE,stderr=subprocess.PIPE)

        # look for email
        email_found = fetch_email( "/tmp/test_102_email_admin_override_custom_report_mobile_file", test_email_address )
        if email_found:
            email_context_found1 = remote_control.run_command("grep -i 'Custom Report' /tmp/test_102_email_admin_override_custom_report_mobile_file 2>&1", stdout=True)
            email_context_found2 = remote_control.run_command("grep -i 'Administration-VWuRol5uWw' /tmp/test_102_email_admin_override_custom_report_mobile_file 2>&1", stdout=True)

        # restore
        global_functions.uvmContext.adminManager().setSettings(orig_adminsettings)

        assert(email_found)
        assert((email_context_found1) and (email_context_found2))

        # Verify that all images are less than 3502350.
        # copy mail from remote client
        subprocess.call("scp -q -i %s testshell@%s:/tmp/test_102_email_admin_override_custom_report_mobile_file /tmp/" % (remote_control.host_key_file, remote_control.client_ip), shell=True)
        fp = open("/tmp/test_102_email_admin_override_custom_report_mobile_file")
        email_string = fp.read()
        fp.close()
        # Delete the first line as it is blank and throws off the parser
        email_string = '\n'.join(email_string.split('\n')[1:])
        msg = email.message_from_string(email_string)

        mime_content_ids = []
        for part in msg.walk():
            if part.get_content_maintype() == "image":
                # print("Image found")
                for index, key in enumerate(part.keys()):
                    if key == "Content-ID" and "untangle.int" in list(part.values())[index]:
                        email_image = part.get_payload(decode=True)
                        im = Image.open(BytesIO(email_image))
                        (image_width,image_height) = im.size
                        print("Image %s width: %d height: %d" % (list(part.values())[index], image_width, image_height))
                        assert(image_width <= 350 and image_height <= 350)

    @pytest.mark.slow
    @pytest.mark.failure_behind_ngfw
    def test_103_email_report_verify_apps(self):
        """
        1) Install all apps
        2) Generate a report
        3) Verify that the emailed report contains a section for each app
        """
        global app,apps_list,apps_name_list
        if (not can_relay):
            raise unittest.SkipTest('Unable to relay through ' + global_functions.TEST_SERVER_HOST)
        if runtests.quick_tests_only:
            raise unittest.SkipTest('Skipping a time consuming test')

        # create settings to receive test_email_address 
        configure_mail_relay()

        # add administrator
        adminsettings = global_functions.uvmContext.adminManager().getSettings()
        orig_adminsettings = copy.deepcopy(adminsettings)
        adminsettings['users']['list'].append(create_admin_user(useremail=test_email_address))
        global_functions.uvmContext.adminManager().setSettings(adminsettings)

        # clear all report users
        settings = self._app.getSettings()
        settings["reportsUsers"]["list"] = settings["reportsUsers"]["list"][:1]
        self._app.setSettings(settings)
        
        # install all the apps that aren't already installed
        system_stats = global_functions.uvmContext.metricManager().getStats()
        # print system_stats
        system_memory = system_stats['systemStats']['MemTotal']
        if (int(system_memory) < 2200000000):   # don't use high memory apps in devices with 2G or less.
            apps_list = apps_list_short
            apps_name_list = apps_name_list_short
        apps = []
        wan_balancer_app = None
        for name in apps_list:
            if (global_functions.uvmContext.appManager().isInstantiated(name)):
                print("App %s already installed" % name)
            else:
                app = global_functions.uvmContext.appManager().instantiate(name, default_policy_id)
                if "wan-balancer" in name:
                    wan_balancer_app = app
                apps.append(app )
            
        # create some traffic 
        result = remote_control.is_online(tries=1)

        # flush out events
        self._app.flushEvents()

        index_of_wans = global_functions.get_wan_tuples()
        """
        1) This test breaks for Dual wan set up, it checks whether email is present at particular URL
        2) wget is used to check for generated email, it breaks with two wan interfaces possibly due to assymetric replies.
        3) Added a check for dual wan setup and setting up single wan interface to serve 100% traffic.
        4) wan-balancer is uninstalled at end of test, hence restored to default values.
        5) This logic will be applicable only for multiple wan setup
        """
        if (len(index_of_wans) >=2):
            for index, (interface_id, ip1, ip2, ip3, intf_name) in enumerate(index_of_wans):
                #set weight 100 for first and rest 0
                if index == 0:
                    set_wan_weight(wan_balancer_app, interface_id, 100)
                else:
                    set_wan_weight(wan_balancer_app, interface_id, 0)

        # send emails
        subprocess.call([global_functions.get_prefix()+"/usr/share/untangle/bin/reports-generate-fixed-reports.py"],stdout=subprocess.PIPE,stderr=subprocess.PIPE)

        # look for email
        email_found = fetch_email( "/tmp/test_103_email_report_admin_file", test_email_address)

        # look for all the appropriate sections in the report email
        results = []
        if email_found:
            for str in apps_name_list:
                results.append(remote_control.run_command("grep -q -i '%s' /tmp/test_103_email_report_admin_file 2>&1"%str))

        # restore
        global_functions.uvmContext.adminManager().setSettings(orig_adminsettings)

        # remove apps that were installed above
        for a in apps: global_functions.uvmContext.appManager().destroy( a.getAppSettings()["id"] )
        
        assert(email_found)
        for result in results:
            assert(result == 0)


    def test_110_verify_report_users(self):
        # Test report only user can login and report servlet displays 
        # add report user with test_email_address
        original_settings = self._app.getSettings()
        settings = copy.deepcopy(original_settings)
        settings["reportsUsers"]["list"] = settings["reportsUsers"]["list"][:1]
        test_email_address = global_functions.random_email()
        settings["reportsUsers"]["list"].append(create_reports_user(profile_email=test_email_address, access=True))  # password = passwd
        self._app.setSettings(settings)
        adminURL = global_functions.get_http_url()
        print("URL %s" % adminURL)
        resultLoginPage = subprocess.call(global_functions.build_wget_command(output_file="-", uri=adminURL + "reports") + " 2>&1 | grep -q Login", shell=True)
        assert (resultLoginPage == 0)
        
        resultLoginPage = subprocess.call(global_functions.build_wget_command(output_file="-", uri=adminURL + 'auth/login?url=/reports&realm=Reports&username=' + test_email_address + "&password=passwd") + " 2>&1 | grep -q Report", shell=True)
        assert (resultLoginPage == 0)
        # Restore original settings
        self._app.setSettings(original_settings)


    def test_111_data_retention_days(self):
        """
        Day retention removal

        Get the earliest table (if found) and creating the previous day's table, run clean, then compare number of tables.
        The cleaner should remove the new table and the before/after counts should be different.
        """
        # On a new system we won't have more than a day's worth of logs
        settings = self._app.getSettings()
        settings["dbRetention"] = 1
        self._app.setSettings(settings)

        # Create at table eligible for removal
        # NOTE: Create 2 previous tables since running the hourly test will remove yesterday's table
        # not leaving enough tables for this test to be verified.
        create_previous_day_table(days=2)

        # Get current count of tables
        result = subprocess.check_output(global_functions.build_postgres_command(query="select count(*) from information_schema.tables where table_schema = 'reports'"), shell=True)
        start_count = int(result.decode("utf-8"))

        # Create and run clean command 
        clean_command = reports_clean_tables_script + " -d postgresql " + str(settings["dbRetention"]) + "| logger -t uvmreports"
        result = subprocess.check_output(clean_command, shell=True)

        # Get post count of tables
        result = subprocess.check_output(global_functions.build_postgres_command(query="select count(*) from information_schema.tables where table_schema = 'reports'"), shell=True)
        end_count = int(result.decode("utf-8"))

        # After cleaning, expecting to have less tables
        print(f"Pre/post table counts: {start_count} < {end_count}")
        assert(end_count < start_count)

    def test_112_data_retention_hours(self):
        """
        Hour retention removal

        Using the sessions table (which should always be populated with something), copy the last record to be earlier than now.
        Get the earliest table (if found) and creating the previous day's table, run clean, then compare number of records.
        NOTE: This because going back an hour can cause a table constraint violation, we make sure we create the previous day.
        Example: If the earliest timestamp is 2023-01-01 00:10:00 and we go back two hours, that timestamp will be 2022-12-31 22:10:00.
        Attempting to change that timestamp in the reports.sessions_2023_01_01 table will cause a violation.
        So we unconditionally create reports.sessions_2022_12_31, catch the exception violation, 
        and attempt to copy make the same change reports.sessions_2022_12_31. 
        """
        table_name = "sessions"
        settings = self._app.getSettings()
        settings["dbRetentionHourly"] = 1
        self._app.setSettings(settings)

        # For safety, create earlier table in case we need to cross day boundary
        create_previous_day_table()

        # start_count = 0
        # end_count = 0
        # Get the latest timestamp; otherwise we may have more in our post clean query.
        result = subprocess.check_output(global_functions.build_postgres_command(query=f"select time_stamp from reports.{table_name} order by time_stamp desc limit 1"), shell=True)
        # Important: Keep as a string so we have an exact match!
        original_latest_time_stamp_string = result.decode("utf-8").strip()

        # Get earliest timestamp in table
        result = subprocess.check_output(global_functions.build_postgres_command(query=f"select time_stamp from reports.{table_name} order by time_stamp asc limit 1"), shell=True)
        # Important: Keep as a string so we have an exact match!
        original_early_time_stamp_string = result.decode("utf-8").strip()

        original_early_time_stamp = datetime.strptime(original_early_time_stamp_string.split(".")[0], "%Y-%m-%d %H:%M:%S")

        # Table that the timestamp currently belongs
        original_table_name = f"{table_name}_{original_early_time_stamp.strftime('%Y_%m_%d')}"

        # Create adjusted timestamp.  2 hours to guarantee 1 hour age will catch.
        previous_time = timedelta(hours=2)
        new_early_time_stamp = datetime.strptime(original_early_time_stamp_string.split(".")[0], "%Y-%m-%d %H:%M:%S") - previous_time
        # Alternate table name to attempt
        target_table_name = f"{table_name}_{new_early_time_stamp.strftime('%Y_%m_%d')}"

        print(f"original_early_time_stamp={original_early_time_stamp}")
        print(f"new_early_time_stamp={new_early_time_stamp}")
        print(f"target_table={target_table_name}")

        # NGFW-14964: No longer copying an existing record by setting an earlier timestamp as insertion of the new record was failing 
        # due to unique constraint on session_id despite providing a random session_id.
        # This failure was occurring intermittently on either of the ATS environment and on random dates of the execution
        # Hence just modifying an existing record's timestamp and will hope to clean that as part of the data retention test
        try:
            # Modify an existing record's timestamp.
            commands = [
                f"update reports.{original_table_name} set time_stamp = '{new_early_time_stamp}' where time_stamp = '{original_early_time_stamp_string}'"
            ]
            result = subprocess.check_output(global_functions.build_postgres_command(query=commands),shell=True, stderr=subprocess.STDOUT)
        except Exception as e:
            # Unable to modify existing record, do in previous day.
            print(e.output.decode('utf-8'))
            commands = [
                f"update reports.{target_table_name} set time_stamp = '{new_early_time_stamp}' where time_stamp = '{original_early_time_stamp_string}'"
            ]
            result = subprocess.check_output(global_functions.build_postgres_command(query=commands),shell=True, stderr=subprocess.STDOUT)
            pass

        # Get current record count
        result = subprocess.check_output(global_functions.build_postgres_command(query=f"select count(*) from reports.sessions where time_stamp < '{original_latest_time_stamp_string}'"), shell=True)
        start_count = int(result.decode("utf-8"))

        # Create and run clean command 
        clean_command = reports_clean_tables_script + " -d postgresql -h " + str(settings["dbRetentionHourly"]) + " " + str(settings["dbRetentionHourly"])
        result = subprocess.check_output(clean_command, shell=True)

        # Get post clean record count
        result = subprocess.check_output(global_functions.build_postgres_command(query=f"select count(*) from reports.sessions where time_stamp < '{original_latest_time_stamp_string}'"), shell=True)
        end_count = int(result.decode("utf-8"))

        # After cleaning, expecting to have less records
        print(f"Pre/post record counts: {start_count} < {end_count}")
        assert(end_count < start_count)

    # tests for each report type:
    #    case EVENT_LIST:

    def test_500_sql_injection_pie_graph(self):
        """
        """
        original_settings = self._app.getSettings()
        settings = copy.deepcopy(original_settings)
        settings = self._app.getSettings()
        settings["reportsUsers"]["list"] = settings["reportsUsers"]["list"][:1]
        test_email_address = global_functions.random_email()
        settings["reportsUsers"]["list"].append(create_reports_user(profile_email=test_email_address, access=True))  # password = passwd
        self._app.setSettings(settings)

        function_name = sys._getframe().f_code.co_name
        sql_injection(test_email_address, "passwd", f"/tmp/{function_name}", "PIE_GRAPH")
        self._app.setSettings(original_settings)

    def test_501_sql_injection_time_graph(self):
        """
        """
        original_settings = self._app.getSettings()    
        settings = copy.deepcopy(original_settings)
        settings = self._app.getSettings()
        settings["reportsUsers"]["list"] = settings["reportsUsers"]["list"][:1]
        test_email_address = global_functions.random_email()
        settings["reportsUsers"]["list"].append(create_reports_user(profile_email=test_email_address, access=True))  # password = passwd
        self._app.setSettings(settings)

        function_name = sys._getframe().f_code.co_name
        sql_injection(test_email_address, "passwd", f"/tmp/{function_name}", "TIME_GRAPH")
        self._app.setSettings(original_settings)

    def test_502_sql_injection_time_graph_dynamic(self):
        """
        """
        original_settings = self._app.getSettings()    
        settings = copy.deepcopy(original_settings)
        settings = self._app.getSettings()
        settings["reportsUsers"]["list"] = settings["reportsUsers"]["list"][:1]
        test_email_address = global_functions.random_email()
        settings["reportsUsers"]["list"].append(create_reports_user(profile_email=test_email_address, access=True))  # password = passwd
        self._app.setSettings(settings)

        function_name = sys._getframe().f_code.co_name
        sql_injection(test_email_address, "passwd", f"/tmp/{function_name}", "TIME_GRAPH_DYNAMIC")
        self._app.setSettings(original_settings)

    def test_503_sql_injection_text(self):
        """
        """
        original_settings = self._app.getSettings()    
        settings = copy.deepcopy(original_settings)
        settings = self._app.getSettings()
        settings["reportsUsers"]["list"] = settings["reportsUsers"]["list"][:1]
        test_email_address = global_functions.random_email()
        settings["reportsUsers"]["list"].append(create_reports_user(profile_email=test_email_address, access=True))  # password = passwd
        self._app.setSettings(settings)

        function_name = sys._getframe().f_code.co_name
        sql_injection(test_email_address, "passwd", f"/tmp/{function_name}", "TEXT")
        self._app.setSettings(original_settings)

    def test_504_sql_injection_event_list(self):
        """
        """
        original_settings = self._app.getSettings()    
        settings = copy.deepcopy(original_settings)
        settings = self._app.getSettings()
        settings["reportsUsers"]["list"] = settings["reportsUsers"]["list"][:1]
        test_email_address = global_functions.random_email()
        settings["reportsUsers"]["list"].append(create_reports_user(profile_email=test_email_address, access=True))  # password = passwd
        self._app.setSettings(settings)

        function_name = sys._getframe().f_code.co_name
        sql_injection(test_email_address, "passwd", f"/tmp/{function_name}", "EVENT_LIST")
        self._app.setSettings(original_settings)

    def test_600_session_minutes_referral(self):
        """
        Verify that missing session table won't cause dependent inserts to fail

        Test stages:
        initialize:
          -   Change uvm table cache timeout
          -   Set system from NTP to manual
          -   Change system date back to one day before current detention
          -   Updatedatabase, update to begin"on that day"

        pre:
          -   Start long running curl command
          -   Wait until both sessions and sessions_minute are populated with this new session across day boundary
          -   On new day, simulate database rotation by removing old sessions table

        post:
          -   Wait a few conntrack cycles

        verify:
          -   We should have current populated session minutes for the original session id

        Timeline for this test after initialization:
        [        client curl       ]
        [pre events] | [post events]

        """
        if runtests.quick_tests_only:
            raise unittest.SkipTest('Skipping a time consuming test')

        global uvmContext
        # # Safety to ensure database is fully aged out as expected
        # subprocess.call(f"/etc/cron.daily/reports-cron",shell=True)

        # This cache is 5 minutes by default; we need to reduce it to verify
        cache_interval_seconds = 1
        # How long we'll wait in our pre-state
        pre_duration_interval_seconds = 200
        # How long we'll wait in the post state
        # post_duration_interval = 200
        post_duration_interval = 130
        # Session should last a little longer than our waits
        session_duration_interval = pre_duration_interval_seconds + post_duration_interval + 30

        # Query conditions to match our session
        event_query_conditions = [
            {"column": "c_client_addr", "operator":"=", "value":remote_control.client_ip},
            {"column": "s_server_addr", "operator":"=", "value":global_functions.test_server_ip},
            {"column": "s_server_port", "operator":"=", "value":443}
        ]

        # Reports app id
        app_id = ReportsTests._app.getAppSettings()["id"]

        # Log file with reports app id
        log_file = f"/var/log/uvm/app-{app_id}.log"

        # Tables to perform delete operations upon
        delete_tables = ["sessions", "session_minutes", "http_events"]

        # Original settings to restore at end
        original_system_settings = global_functions.uvmContext.systemManager().getSettings()

        # Backip existing untangle-vm.conf
        global_functions.vm_conf_backup()

        # Kill curl from possibly aborted test run
        remote_control.run_command(f"ps awwwux | grep [c]url | cut -d' ' -f3 | xargs kill -9 2>/dev/null")

        # Run our test in a try so that on any failure, we can properly cleanup
        print()
        failures = []
        try:
            print("-" * 80)
            print("stage=initialize")
            print("-" * 80)

            # Make sure we're in day-only retention mode
            report_settings = copy.deepcopy(orig_settings)
            report_settings["dbRetentionHourly"] = 0

            # How many days ago to start
            # days_ago = report_settings["dbRetention"] + 1
            days_ago = 1

            # Disable NTP
            original_system_settings = global_functions.uvmContext.systemManager().getSettings()
            system_settings = copy.deepcopy(original_system_settings)
            system_settings['timeSource'] = 'manual'
            global_functions.uvmContext.systemManager().setSettings(system_settings)

            # Set time to 90 seconds before 1 day before end of day of retention.
            # This gives enough time for both the session and session_minute entries
            initialize_date = subprocess.check_output(f"date -Ins -d '-{days_ago} day 23:58:30'", shell=True).decode("utf-8").strip()
            print(f"initialize_date={initialize_date}")
            subprocess.call(f"date -Ins -s '{initialize_date}' >/dev/null",shell=True)

            # Restart UVM due to date change and while we're at it,
            # change the cacheTableInterval to 1s instead of its 30 min default
            global_functions.uvmContext = global_functions.vm_conf_update(search="reports_cacheTableInterval=", replace=f"reports_cacheTableInterval=\"{cache_interval_seconds}000\"")

            # Run report sync script to add table
            print(f"running reports-cron to setup today's tables")
            subprocess.call(f"/etc/cron.daily/reports-cron",shell=True)
            # Delete from tables
            print("pre-delete all rows from tables:")
            for delete_table in delete_tables:
                print(f"\tdelete_table={delete_table}")
                subprocess.check_output(f"psql -U postgres uvm -c \"delete from reports.{delete_table}\"", shell=True).decode("utf-8").strip()

            print("available session tables")
            for info in subprocess.check_output(f"psql -U postgres uvm -c \"select table_name from information_schema.tables where table_name like 'sessions_%' order by table_name\"", shell=True).decode("utf-8").strip().split("\n"):
                print(f"\t{info}")

            print("-" * 80)
            print("stage=pre")
            print("-" * 80)

            # Start the forked curl command
            # This uses HTTPS to keep the session alive (via keepalive) througout this test which is critical.
            # Since curl doesn't have a way to "wait", we rely on the remote server performing a sleep to "pause" the session
            # IMPORTANT: Assumption is that no other HTTPS traffic from client to test server via HTTPS is occuring during this test
            curl_command=global_functions.build_curl_command(uri=f"https://{global_functions.TEST_SERVER_HOST}/sleep.php?seconds=1&length=1024&[1-{session_duration_interval}]", max_time=None, override_arguments=["--silent"])
            print(f"fork curl_command={curl_command} for {session_duration_interval} seconds")
            remote_control.run_command(f"{curl_command} > /dev/null", nowait=True)

            # We need to wait long enough for:
            # - Session table to be updated with initial packet (quick, within a second or so)
            # - The next day's session minute table to be populated with at least one entry
            # So overall, a little over 2 min to be safe.
            print()
            print(f"* sleep pre_duration_interval_seconds={pre_duration_interval_seconds}")
            sys.stdout.flush()

            # For sessions, ONLY consider sessions that are in our current first day
            pre_start_date_seconds = subprocess.check_output(f"date +%s -d 'today 00:00:00'", shell=True).decode("utf-8").strip()
            pre_end_date_seconds = subprocess.check_output(f"date +%s -d '{pre_duration_interval_seconds} seconds'", shell=True).decode("utf-8").strip()
            # Minutes we are expecting to see something in next day
            pre_session_minutes_end_date_seconds = subprocess.check_output(f"date +%s -d '{pre_duration_interval_seconds} seconds'", shell=True).decode("utf-8").strip()

            time.sleep(pre_duration_interval_seconds)
            now_date = subprocess.check_output(f"date", shell=True).decode("utf-8").strip()
            print(f"ended at {now_date}")

            print()
            # Collect session and session minute ids and their timestamps
            session_id_timestamps = {}
            events = global_functions.get_events("Network", "All Sessions", event_query_conditions, 5,
                start_date={"javaClass": "java.util.Date", "time": int(pre_start_date_seconds) * 1000},
                end_date={"javaClass": "java.util.Date", "time": (int(pre_end_date_seconds) * 1000)}
            )
            print("pre sessions events=")
            if events is not None and "list" in events:
                for event in events["list"]:
                    session_id = event['session_id']
                    time_stamp = datetime.fromtimestamp(int(event['time_stamp']['time'])/1000).strftime('%Y-%m-%d %H:%M:%S')
                    print(f"\t{time_stamp} {session_id} {event['c_client_addr']} {event['s_server_addr']} {event['s_server_port']}")
                    session_id_timestamps[session_id] = [f"pre_sessions-{time_stamp}"]

            events = global_functions.get_events("Network", "All Session Minutes", event_query_conditions, 5,
                start_date={"javaClass": "java.util.Date", "time": int(pre_start_date_seconds) * 1000},
                end_date={"javaClass": "java.util.Date", "time": (int(pre_session_minutes_end_date_seconds) * 1000)}
            )
            print("pre session_minutes events=")
            matching_session_ids = []
            if events is not None and "list" in events:
                for event in events["list"]:
                    session_id = event['session_id']
                    time_stamp = datetime.fromtimestamp(int(event['time_stamp']['time'])/1000).strftime('%Y-%m-%d %H:%M:%S')
                    print(f"\t{time_stamp} {session_id} {event['c_client_addr']} {event['s_server_addr']} {event['s_server_port']}")
                    if session_id in session_id_timestamps:
                        session_id_timestamps[session_id].append(f"pre_session_minutes-{time_stamp}")
                        if session_id not in matching_session_ids:
                            matching_session_ids.append(session_id)

            # We are expecting to find our session_id in sessions and session_minutes
            assert len(matching_session_ids) > 0, "in pre stage, found corresponding sessions/session_minutes session id"
            print(f"matching_session_ids={','.join(map(str, matching_session_ids))}")

            # Running reports-cron won't work the next day (unless we set time to actual current, then back..) so we need to simulate the aging by
            # deleting delete the now-previous day's sessions and session_minutes partition tables.
            drop_tables = []
            drop_tables.append(subprocess.check_output("psql -U postgres uvm -c \"select table_name from information_schema.tables where table_schema = 'reports' and table_name like 'sessions_%' order by table_name limit 1\" | grep sessions_", shell=True).decode("utf-8").strip())
            drop_tables.append(subprocess.check_output("psql -U postgres uvm -c \"select table_name from information_schema.tables where table_schema = 'reports' and table_name like 'session_minutes_%' order by table_name limit 1\" | grep session_minutes_", shell=True).decode("utf-8").strip())
            print()
            print("drop tables:")
            for drop_table in drop_tables:
                print(f"\tdrop_table={drop_table}")
                subprocess.check_output(f"psql -U postgres uvm -c \"drop table reports.{drop_table}\"", shell=True).decode("utf-8").strip()

            print("available session tables (should not have earliest table as before)")
            for info in subprocess.check_output(f"psql -U postgres uvm -c \"select table_name from information_schema.tables where table_name like 'sessions_%' order by table_name\"", shell=True).decode("utf-8").strip().split("\n"):
                print(f"\t{info}")

            # Get last reports log line so we can monitor entries thereafter
            last_log_line = subprocess.check_output(f"wc -l {log_file} | cut -d' ' -f1", shell=True).decode("utf-8").strip()
            last_log_line = int(last_log_line) + 1

            print("-" * 80)
            print("stage=post")
            print("-" * 80)

            post_start_date = subprocess.check_output(f"date +%s", shell=True).decode("utf-8").strip()

            # After day change, give a few (3) minutes and a little extra before going into POST
            # Get the date to start querying after we go into post; we will query for events on this timestamp onward
            # post_start_date = subprocess.check_output(f"date +%s", shell=True).decode("utf-8").strip()
            print()
            print(f"* sleep post_duration_interval={post_duration_interval}")
            sys.stdout.flush()
            time.sleep(post_duration_interval)

            events = None
            events = global_functions.get_events("Network", "All Session Minutes", event_query_conditions, 100,
                start_date={"javaClass": "java.util.Date", "time": int(post_start_date) * 1000},
                end_date={"javaClass": "java.util.Date", "time": (int(post_start_date) + 86400) * 1000}
            )
            print("post session_minutes events=")
            found_sessions = []
            if events is not None and "list" in events:
                for event in events["list"]:
                    session_id = event['session_id']
                    time_stamp = datetime.fromtimestamp(int(event['time_stamp']['time'])/1000).strftime('%Y-%m-%d %H:%M:%S')
                    print(f"\t{time_stamp} {session_id} {event['c_client_addr']} {event['s_server_addr']} {event['s_server_port']}")
                    if session_id in session_id_timestamps:
                        if session_id not in found_sessions:
                            found_sessions.append(session_id)
                        session_id_timestamps[session_id].append(f"post_session_minutes-{time_stamp}")

            # Check for exceptions in logs
            # This may introduce some false positives for other sessions that may be going on that we don't care about
            log_sql = subprocess.check_output(f"awk 'NR >= {last_log_line} && /BatchUpdateException/{{ print NR, $0 }}' {log_file}", shell=True).decode("utf-8")
            if log_sql != "":
                print("update/insert exceptions=")
                print(f"{log_sql}")
            assert log_sql == "", "no sql exceptions found in logs"

            print("-" * 80)
            print("stage=verify")
            print("-" * 80)
            assert len(found_sessions) > 0, "found at least one session candidate" 

            if len(found_sessions) > 0:
                print("finding matching sessions:")
                for session_id in matching_session_ids:
                    pre_sessions = False
                    pre_session_minutes = False
                    post_session_minutes = False
                    print(f"\tsession_id={session_id}")
                    for time_stamp in session_id_timestamps[session_id]:
                        if time_stamp.startswith("pre_sessions-"):
                            pre_sessions = True
                        if time_stamp.startswith("pre_session_minutes-"):
                           pre_session_minutes = True
                        if time_stamp.startswith("post_session_minutes"):
                            post_session_minutes = True
                        print(f"\t\ttime_stamp={time_stamp}")
                    assert pre_sessions and pre_session_minutes and post_session_minutes, f"found all stages for session_id={session_id}"

        except Exception as e:
            failures.append(e)

        # Cleanup

        # Kill lingering curl session (it may not naturally end)
        remote_control.run_command(f"ps awwwux | grep [c]url | cut -d' ' -f3 | xargs kill -9 2>/dev/null")

        # Restore system back to standard working order
        global_functions.uvmContext = global_functions.vm_conf_restore()

        # Change system back to NTP
        global_functions.uvmContext.systemManager().setSettings(original_system_settings)
        global_functions.uvmContext.forceTimeSync()
        global_functions.uvmContext = global_functions.restart_uvm()
        subprocess.call(f"/etc/cron.daily/reports-cron",shell=True)

        assert len(failures) == 0, ", ".join(map(str, failures))

    @classmethod
    def final_extra_tear_down(cls):
        global web_app, orig_settings
        # remove all the apps in case test 103 does not remove them.
        for name in apps_list:
            if (global_functions.uvmContext.appManager().isInstantiated(name)):
                remove_app = global_functions.uvmContext.appManager().app(name)
                global_functions.uvmContext.appManager().destroy(remove_app.getAppSettings()["id"])
        if orig_mailsettings != None:
            global_functions.uvmContext.mailSender().setSettings(orig_mailsettings)

        web_app = None

        
test_registry.register_module("reports", ReportsTests)
