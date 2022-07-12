#!/usr/bin/python3 -u

import subprocess
import sys
import copy

class I18N:
    def _(self, str):
        if not str[0].isupper():
            print("Invalid string: " + str)
            sys.exit(1)
        return '\'' + str + '\'.t()'

i18n = I18N()

dict = {
    'event_id' : i18n._('Event ID'),
    'time_stamp' : i18n._('Timestamp'),
    'tags' : i18n._('Tags'),
    'entity' : i18n._('Entity'),
    'old_value' : i18n._('Old Value'),
    'entitled' : i18n._('Entitled'),
    'session_id' : i18n._('Session ID'),
    'client_intf' : i18n._('Client Interface'),
    'client_country' : i18n._('Client Country'),
    'client_latitude' : i18n._('Client Latitude'),
    'client_longitude' : i18n._('Client Longitude'),
    'server_country' : i18n._('Server Country'),
    'server_latitude' : i18n._('Server Latitude'),
    'server_longitude' : i18n._('Server Longitude'),
    'server_intf' : i18n._('Server Interface'),
    'local_addr' : i18n._('Local Address'),
    'remote_addr' : i18n._('Remote Address'),
    'c_client_addr' : i18n._('Client-side Client Address'),
    's_client_addr' : i18n._('Server-side Client Address'),
    'c_server_addr' : i18n._('Client-side Server Address'),
    's_server_addr' : i18n._('Server-side Server Address'),
    'c_client_port' : i18n._('Client-side Client Port'),
    's_client_port' : i18n._('Server-side Client Port'),
    'c_server_port' : i18n._('Client-side Server Port'),
    's_server_port' : i18n._('Server-side Server Port'),
    'policy_id' : i18n._('Policy ID'),
    'policy_rule_id' : i18n._('Policy Rule ID'),
    'username' : i18n._('Username'),
    'hostname' : i18n._('Hostname'),
    'filter_prefix' : i18n._('Filter Block'),
    'c2s_content_length' : i18n._('Client-to-server Content Length'),
    's2c_content_length' : i18n._('Server-to-client Content Length'),
    's2c_content_type' : i18n._('Server-to-client Content Type'),
    's2c_content_filename' : i18n._('Server-to-client Content Disposition Filename'),
    'rx_bytes' : i18n._('Bytes Received'),
    'tx_bytes' : i18n._('Bytes Sent'),
    'client_name': i18n._('Client Name'),
    'start_time' : i18n._('Start Time'),
    'end_time'   : i18n._('End Time'),
    'remote_address' : i18n._('Remote Address'),
    'remote_port'    : i18n._('Remote Port'),
    'pool_address'   : i18n._('Pool Address'),
    'type': i18n._('Type'),
    'connect_stamp' : i18n._('Connect Time'),
    'goodbye_stamp' : i18n._('End Time'),
    'client_address' : i18n._('Client Address'),
    'client_protocol' : i18n._('Client Protocol'),
    'client_username' : i18n._('Client Username'),
    'net_process' : i18n._('Net Process'),
    'net_interface' : i18n._('Net Interface'),
    'elapsed_time' : i18n._('Elapsed Time'),
    'ipaddr' : i18n._('Client Address'),
    'vendor_name' : i18n._('Vendor Name'),
    'mem_free' : i18n._('Memory Free'),
    'mem_cache' : i18n._('Memory Cache'),
    'mem_buffers' : i18n._('Memory Buffers'),
    'load_1' : i18n._('CPU load (1-min)'),
    'load_5' : i18n._('CPU load (5-min)'),
    'load_15' : i18n._('CPU load (15-min)'),
    'cpu_user' : i18n._('CPU User Utilization'),
    'cpu_system' : i18n._('CPU System Utilization'),
    'disk_total' : i18n._('Disk Size'),
    'disk_free' : i18n._('Disk Free'),
    'swap_total' : i18n._('Swap Size'),
    'swap_free' : i18n._('Swap Free'),
    'hits' : i18n._('Hits'),
    'misses' : i18n._('Misses'),
    'bypasses' : i18n._('Bypasses'),
    'bypassed' : i18n._('Bypassed'),
    'systems' : i18n._('System bypasses'),
    'hit_bytes' : i18n._('Hit Bytes'),
    'miss_bytes' : i18n._('Miss Bytes'),
    'success' : i18n._('Success'),
    'description' : i18n._('Text detail of the event'),
    'login_name' : i18n._('Login Name'),
    'event_info' : i18n._('Event Type'),
    'auth_type' : i18n._('Authorization Type'),
    'client_addr' : i18n._('Client Address'),
    'request_id' : i18n._('Request ID'),
    'method' : i18n._('Method'),
    'uri' : i18n._('URI'),
    'referer' : i18n._('Referer'),
    'msg_id' : i18n._('Message ID'),
    'subject' : i18n._('Subject'),
    'mac_address' : i18n._('MAC Address'),
    'addr' : i18n._('Address'),
    'addr_name' : i18n._('Address Name'),
    'addr_kind' : i18n._('Address Kind'),
    'sender' : i18n._('Sender'),
    'msg_id' : i18n._('Message ID'),
    'receiver' : i18n._('Receiver'),
    'host' : i18n._('Host'),
    'term' : i18n._('Search Term'),
    'interface_id' : i18n._('Interface ID'),
    'action' : i18n._('Action'),
    'os_name' : i18n._('Interface O/S Name'),
    'name' : i18n._('Interface Name'),
    'interface_id' : i18n._('Interface ID'),
    'domain' : i18n._('Domain'),
    'sig_id' : i18n._('Signature ID'),
    'gen_id' : i18n._('Grouping ID'),
    'class_id' : i18n._('Classtype ID'),
    'source_addr' : i18n._('Source Address'),
    'source_port' : i18n._('Source Port'),
    'dest_addr' : i18n._('Destination Address'),
    'dest_port' : i18n._('Destination Port'),
    'protocol' : i18n._('Protocol'),
    'icmp_type' : i18n._('ICMP Type'),
    'blocked' : i18n._('Blocked'),
    'category' : i18n._('Category'),
    'classtype' : i18n._('Classtype'),
    'msg' : i18n._('Message'),
    'summary_text' : i18n._('Summary Text'),
    'json' : i18n._('JSON Text'),
    'address' : i18n._('Address'),
    'key' : i18n._('Key'),
    'value' : i18n._('Value'),
    'size' : i18n._('Size'),
    'reason' : i18n._('Reason'),
    'c2p_bytes' : i18n._('From-Client Bytes'),
    'p2c_bytes' : i18n._('To-Client Bytes'),
    's2p_bytes' : i18n._('From-Server Bytes'),
    'p2s_bytes' : i18n._('To-Server Bytes'),
    'c2s_bytes' : i18n._('From-Client Bytes'),
    's2c_bytes' : i18n._('From-Server Bytes'),
    'login' : i18n._('Login'),
    'local' : i18n._('Local'),
    'succeeded' : i18n._('Succeeded'),
    'firewall_blocked' : '\'Firewall \' + ' + i18n._('Blocked'),
    'firewall_flagged' : '\'Firewall \' + ' + i18n._('Flagged'),
    'firewall_rule_index' : '\'Firewall \' + ' + i18n._('Rule ID'),
    'application_control_lite_protocol' : '\'Application Control Lite \' + ' + i18n._('Protocol'),
    'application_control_lite_blocked' : '\'Application Control Lite \' + ' + i18n._('Blocked'),
    'captive_portal_blocked' : '\'Captive Portal \' + ' + i18n._('Blocked'),
    'captive_portal_rule_index' : '\'Captive Portal \' + ' + i18n._('Rule ID'),
    'application_control_application' : '\'Application Control \' + ' + i18n._('Application'),
    'application_control_protochain' : '\'Application Control \' + ' + i18n._('Protochain'),
    'application_control_category' : '\'Application Control \' + ' + i18n._('Category'),
    'application_control_blocked' : '\'Application Control \' + ' + i18n._('Blocked'),
    'application_control_flagged' : '\'Application Control \' + ' + i18n._('Flagged'),
    'application_control_confidence' : '\'Application Control \' + ' + i18n._('Confidence'),
    'application_control_ruleid' : '\'Application Control \' + ' + i18n._('Rule ID'),
    'application_control_detail' : '\'Application Control \' + ' + i18n._('Detail'),
    'bandwidth_control_priority' : '\'Bandwidth Control \' + ' + i18n._('Priority'),
    'bandwidth_control_rule' : '\'Bandwidth Control \' + ' + i18n._('Rule ID'),
    'ssl_inspector_ruleid' : '\'SSL Inspector \' + ' + i18n._('Rule ID'),
    'ssl_inspector_status' : '\'SSL Inspector \' + ' + i18n._('Status'),
    'ssl_inspector_detail' : '\'SSL Inspector \' + ' + i18n._('Detail'),
    'virus_blocker_lite_clean' : '\'Virus Blocker Lite \' + ' + i18n._('Clean'),
    'virus_blocker_lite_name' : '\'Virus Blocker Lite \' + ' + i18n._('Name'),
    'virus_blocker_clean' : '\'Virus Blocker \' + ' + i18n._('Clean'),
    'virus_blocker_name' : '\'Virus Blocker \' + ' + i18n._('Name'),
    'spam_blocker_lite_score' : '\'Spam Blocker Lite \' + ' + i18n._('Score'),
    'spam_blocker_lite_is_spam' : '\'Spam Blocker Lite \' + ' + i18n._('Spam'),
    'spam_blocker_lite_action' : '\'Spam Blocker Lite \' + ' + i18n._('Action'),
    'spam_blocker_lite_tests_string' : '\'Spam Blocker Lite \' + ' + i18n._('Tests'),
    'spam_blocker_score' : '\'Spam Blocker \' + ' + i18n._('Score'),
    'spam_blocker_is_spam' : '\'Spam Blocker \' + ' + i18n._('Spam'),
    'spam_blocker_action' : '\'Spam Blocker \' + ' + i18n._('Action'),
    'spam_blocker_tests_string' : '\'Spam Blocker \' + ' + i18n._('Tests'),
    'phish_blocker_score' : '\'Phish Blocker \' + ' + i18n._('Score'),
    'phish_blocker_is_spam' : '\'Phish Blocker \' + ' + i18n._('Phish'),
    'phish_blocker_action' : '\'Phish Blocker \' + ' + i18n._('Action'),
    'phish_blocker_tests_string' : '\'Phish Blocker \' + ' + i18n._('Tests'),
    'ad_blocker_cookie_ident' : '\'Ad Blocker \' + ' + i18n._('Cookie'),
    'ad_blocker_action' : '\'Ad Blocker \' + ' + i18n._('Action'),
    'web_filter_reason' : '\'Web Filter \' + ' + i18n._('Reason'),
    'web_filter_category' : '\'Web Filter \' + ' + i18n._('Category'),
    'web_filter_blocked' : '\'Web Filter \' + ' + i18n._('Blocked'),
    'web_filter_flagged' : '\'Web Filter \' + ' + i18n._('Flagged'),
    'settings_file' : i18n._('Settings File'),
    'tunnel_name' : i18n._('Tunnel Name'),
    'in_bytes' : i18n._('In Bytes'),
    'out_bytes' : i18n._('Out Bytes'),
    'destination' : i18n._('Destination'),
    'mem_total' : i18n._('Total Memory'),
    'active_hosts' : i18n._('Active Hosts'),
    'rx_rate' : i18n._('Rx Rate'),
    'tx_rate' : i18n._('Tx Rate'),
}


# check for all names
p = subprocess.Popen(["sh","-c","psql -A -t -U postgres uvm -c \"SELECT table_name FROM information_schema.tables where table_schema = 'reports' and table_name not like '%0%'\""], stdout=subprocess.PIPE, text=True)
for line in iter(p.stdout.readline, ''):
    table_name = line.strip()

    if table_name == "reports_state":
        continue
    if table_name == "report_data_days":
        continue
    if table_name == "table_updates":
        continue
    if "totals" in table_name:
        continue
    if "counts" in table_name:
        continue
    p2 = subprocess.Popen(["sh","-c","psql -A -t -U postgres uvm -c \"\\d+ reports.%s\"" % table_name], stdout=subprocess.PIPE, text=True)
    for line2 in iter(p2.stdout.readline, ''):
        parts = line2.split("|")
        column = parts[0]
        type = parts[1]
        description = None

        try:
            description = dict[column]
        except:
            print("\nMissing description for column \"%s\" in table \"%s\"" % ( column, table_name  ))
            sys.exit(1)
        if description == None:
            print("\nMissing description for column \"%s\" in table \"%s\"" % ( column, table_name  ))
            sys.exit(1)



print("{")
for key, value in sorted(dict.items()):
    if value == None:
        print("\nBad Value for key: " + key)
        sys.exit(1)

    print(key + ": " + value + ",")

print("}")

