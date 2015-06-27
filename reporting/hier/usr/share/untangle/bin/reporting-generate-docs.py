#!/usr/bin/python -u

import subprocess
import sys
import copy

dict = {};

generic = {
    'event_id' : 'The unique event ID',
    'time_stamp' : 'The time of the event',
    'session_id' : 'The session',
    'client_intf' : 'The client interface',
    'server_intf' : 'The server interface',
    'c_client_addr' : 'The client-side client IP address',
    's_client_addr' : 'The server-side client IP address',
    'c_server_addr' : 'The client-side server IP address',
    's_server_addr' : 'The server-side server IP address',
    'c_client_port' : 'The client-side client port',
    's_client_port' : 'The server-side client port',
    'c_server_port' : 'The client-side server port',
    's_server_port' : 'The server-side server port',
    'policy_id' : 'The policy',
    'username' : 'The username',
    'hostname' : 'The hostname',
    'c2s_content_length' : 'The client-to-server content length',
    's2c_content_length' : 'The server-to-client content length',
    's2c_content_type' : 'The server-to-client content type',
    'rx_bytes' : 'The number of bytes received from the client in this connection',
    'tx_bytes' : 'The number of bytes sent to the client in this connection',
    'virus_blocker_lite_clean' : 'The cleanliness of the file according to Virus Blocker Lite',
    'virus_blocker_lite_name' : 'The name of the malware according to Virus Blocker Lite',
    'virus_blocker_clean' : 'The cleanliness of the file according to Virus Blocker',
    'virus_blocker_name' : 'The name of the malware according to Virus Blocker',
}

dict['openvpn_stats'] = copy.deepcopy(generic)
dict['openvpn_stats'].update({
    'table_description' : 'This table stores "status" events from OpenVPN connections.',
    'client_name': 'The name of the client',
    'start_time' : 'The time the OpenVPN session started',
    'end_time'   : 'The time the OpenVPN session ended',
    'rx_bytes'   : 'The total bytes received from the client during this session',
    'tx_bytes'   : 'The total bytes sent to the client during this session',
    'remote_address' : 'The remote IP address of the client',
    'remote_port'    : 'The remote port of the client',
    'pool_address'   : 'The pool IP address of the client',
})

dict['openvpn_events'] = copy.deepcopy(generic)
dict['openvpn_events'].update({
    'table_description' : 'This table stores client events from OpenVPN connections.',
    'remote_address' : 'The remote IP address of the client',
    'pool_address'   : 'The pool IP address of the client',
    'client_name': 'The name of the client',
    'type': 'The type of the event (CONNECT/DISCONNECT)',
})

dict['ipsec_user_events'] = copy.deepcopy(generic)
dict['ipsec_user_events'].update({
    'table_description' : 'This table stores IPsec stats for client (L2TP/Xauth) each connection.',
    'connect_stamp' : 'The time the connection started',
    'goodbye_stamp' : 'The time the connection ended',
    'client_address' : 'The remote IP address of the client',
    'client_protocol' : 'The protocol the client used to connect',
    'client_username' : 'The username of the client',
    'net_process' : 'The PID of the PPP process for L2TP connections or the connection ID for Xauth connections',
    'net_interface' : 'The PPP interface for L2TP connections or the client interface for Xauth connections',
    'elapsed_time' : 'The total time the client was connected',
})

dict['smtp_tarpit_events'] = copy.deepcopy(generic)
dict['smtp_tarpit_events'].update({
    'table_description' : 'This table stores SMTP tarpit events.',
    'ipaddr' : 'The client IP address',
    'vendor_name' : 'The \"vendor name\" of the app that logged the event',
})

dict['server_events'] = copy.deepcopy(generic)
dict['server_events'].update({
    'table_description' : 'This table stores server status events.',
    'mem_free' : 'The number of free bytes of memory',
    'mem_cache' : 'The number of bytes of memory used for cache',
    'mem_buffers' : 'The number of bytes of memory used for buffers',
    'load_1' : 'The 1-minute CPU load',
    'load_5' : 'The 5-minute CPU load',
    'load_15' : 'The 15-minute CPU load',
    'cpu_user' : 'The user CPU percent utilization',
    'cpu_system' : 'The system CPU percent utilization',
    'disk_total' : 'The total disk size in bytes',
    'disk_free' : 'The free disk space in bytes',
    'swap_total' : 'The total swap size in bytes',
    'swap_free' : 'The free disk swap in bytes',
})

dict['webcache_stats'] = copy.deepcopy(generic)
dict['webcache_stats'].update({
    'table_description' : 'This table stores web cache statistics.',
    'hits' : 'The number of cache hits during this time frame',
    'misses' : 'The number of cache misses during this time frame',
    'bypasses' : 'The number of cache user bypasses during this time frame',
    'systems' : 'The number of cache system bypasses during this time frame',
    'hit_bytes' : 'The number of bytes saved from cache hits',
    'miss_bytes' : 'The number of bytes not saved from cache misses',
})

dict['configuration_backup_events'] = copy.deepcopy(generic)
dict['configuration_backup_events'].update({
    'table_description' : 'This table stores configuration backup events to the untangle cloud',
    'success' : 'The result of the backup (true if the backup succeeded, false otherwise)',
    'description' : 'Text detail of the event',
})

dict['capture_user_events'] = copy.deepcopy(generic)
dict['capture_user_events'].update({
    'table_description' : 'This table stores Captive Portal events',
    'login_name' : 'The login username',
    'event_info' : 'The type of event (LOGIN, FAILED, TIMEOUT, INACTIVE, USER_LOGOUT, ADMIN_LOGOUT)',
    'auth_type' : 'The authorization type for this event',
    'client_addr' : 'The remote IP address of the client',
})

dict['ftp_events'] = copy.deepcopy(generic)
dict['ftp_events'].update({
    'table_description' : 'This table stores FTP download events',
    'request_id' : 'The FTP request ID',
    'method' : 'The FTP method',
    'uri' : 'The FTP URI',
    'virus_blocker_lite_clean' : 'The cleanliness of the file according to Virus Blocker Lite',
    'virus_blocker_lite_name' : 'The name of the malware according to Virus Blocker Lite',
    'virus_blocker_clean' : 'The cleanliness of the file according to Virus Blocker',
    'virus_blocker_name' : 'The name of the malware according to Virus Blocker',
})

dict['mail_addrs'] = copy.deepcopy(generic)
dict['mail_addrs'].update({
    'table_description' : 'This table stores mail events by address. There is one row for each address involved in an SMTP session.',
    'msg_id' : 'The message ID',
    'subject' : 'The email subject',
    'addr' : 'The address of this event',
    'addr_name' : 'The name for this address',
    'addr_kind' : 'The type for this address (F=From, T=To, C=CC, G=Envelope From, B=Envelope To, X=Unknown)',
    'sender' : 'The address of the sender',
    'spam_blocker_lite_score' : 'The score of the email according to Spam Blocker Lite',
    'spam_blocker_lite_is_spam' : 'The spam status of the email according to Spam Blocker Lite',
    'spam_blocker_lite_action' : 'The action taken by Spam Blocker Lite',
    'spam_blocker_lite_tests_string' : 'The tess results for Spam Blocker Lite',
    'spam_blocker_score' : 'The score of the email according to Spam Blocker',
    'spam_blocker_is_spam' : 'The spam status of the email according to Spam Blocker',
    'spam_blocker_action' : 'The action taken by Spam Blocker',
    'spam_blocker_tests_string' : 'The tess results for Spam Blocker',
    'phish_blocker_score' : 'The score of the email according to Phish Blocker',
    'phish_blocker_is_spam' : 'The phish status of the email according to Phish Blocker',
    'phish_blocker_action' : 'The action taken by Phish Blocker',
    'phish_blocker_tests_string' : 'The tess results for Phish Blocker',
})

dict['mail_msgs'] = copy.deepcopy(generic)
dict['mail_msgs'].update({
    'table_description' : 'This table stores mail events. There is one row for each SMTP session.',
    'msg_id' : 'The message ID',
    'subject' : 'The email subject',
    'receiver' : 'The address of the receiver',
    'sender' : 'The address of the sender',
    'spam_blocker_lite_score' : 'The score of the email according to Spam Blocker Lite',
    'spam_blocker_lite_is_spam' : 'The spam status of the email according to Spam Blocker Lite',
    'spam_blocker_lite_action' : 'The action taken by Spam Blocker Lite',
    'spam_blocker_lite_tests_string' : 'The tess results for Spam Blocker Lite',
    'spam_blocker_score' : 'The score of the email according to Spam Blocker',
    'spam_blocker_is_spam' : 'The spam status of the email according to Spam Blocker',
    'spam_blocker_action' : 'The action taken by Spam Blocker',
    'spam_blocker_tests_string' : 'The tess results for Spam Blocker',
    'phish_blocker_score' : 'The score of the email according to Phish Blocker',
    'phish_blocker_is_spam' : 'The phish status of the email according to Phish Blocker',
    'phish_blocker_action' : 'The action taken by Phish Blocker',
    'phish_blocker_tests_string' : 'The tess results for Phish Blocker',
})

dict['http_events'] = copy.deepcopy(generic)
dict['http_events'].update({
    'table_description' : 'This table stores HTTP events. There is one row for each HTTP request.',
    'request_id' : 'The HTTP request ID',
    'method' : 'The HTTP method',
    'uri' : 'The HTTP URI',
    'host' : 'The HTTP host',
    'ad_blocker_cookie_ident' : 'This name of cookie blocked by Ad Blocker',
    'ad_blocker_action' : 'This action of Ad Blocker on this request',
    'web_filter_lite_reason' : 'This reason Web Filter Lite blocked/flagged this request',
    'web_filter_lite_category' : 'This category according to Web Filter Lite',
    'web_filter_lite_blocked' : 'If Web Filter Lite blocked this request',
    'web_filter_lite_flagged' : 'If Web Filter Lite flagged this request',
    'web_filter_reason' : 'This reason Web Filter blocked/flagged this request',
    'web_filter_category' : 'This category according to Web Filter',
    'web_filter_blocked' : 'If Web Filter blocked this request',
    'web_filter_flagged' : 'If Web Filter flagged this request',
})

dict['http_query_events'] = copy.deepcopy(generic)
dict['http_query_events'].update({
    'table_description' : 'This table stores search engine (google, etc) queries.',
    'request_id' : 'The HTTP request ID',
    'method' : 'The HTTP method',
    'uri' : 'The HTTP URI',
    'term' : 'The search term',
    'host' : 'The HTTP host',
})

dict['wan_failover_action_events'] = copy.deepcopy(generic)
dict['wan_failover_action_events'].update({
    'table_description' : 'This table stores WAN Failover events. There is one row for each WAN status change.',
    'interface_id' : 'This interface ID',
    'action' : 'This action (CONNECTED/DISCONNECTED)',
    'os_name' : 'This O/S name of the interface',
    'name' : 'This name of the interface',
})

dict['wan_failover_test_events'] = copy.deepcopy(generic)
dict['wan_failover_test_events'].update({
    'table_description' : 'This table stores WAN Failover test events. There is one row for each WAN Failover test performed.',
    'interface_id' : 'This interface ID',
    'name' : 'This name of the interface',
    'description' : 'The description from the test rule',
    'success' : 'The result of the test (true if the test succeeded, false otherwise)',
})

dict['directory_connector_login_events'] = copy.deepcopy(generic)
dict['directory_connector_login_events'].update({
    'table_description' : 'This table stores Directory Connector username events. There is one row for each status update of an IP/username.',
    'login_name' : 'The login name',
    'domain' : 'The AD domain',
    'type' : 'The type of event (I=Login,U=Update,O=Logout)',
    'client_addr' : 'The client IP address',
})

dict['intrusion_prevention_events'] = copy.deepcopy(generic)
dict['intrusion_prevention_events'].update({
    'table_description' : 'This table stores Intrusion Prevention events. There is one row for each detection.',
    'sig_id' : 'This ID of the rule',
    'gen_id' : 'The grouping ID for the rule, The gen_id + sig_id specify the rule\'s unique identifier',
    'class_id' : 'The numeric ID for the classtype',
    'source_addr' : 'The source IP address of the packet',
    'source_port' : 'The source port of the packet (if applicable)',
    'dest_addr' : 'The destination IP address of the packet',
    'dest_port' : 'The destination port of the packet (if applicable)',
    'protocol' : 'The protocol of the packet',
    'blocked' : 'If the packet was blocked/dropped',
    'category' : 'The application specific grouping',
    'classtype' : 'The generalized threat rule grouping (unrelated to gen_id)',
    'msg' : 'The "title" or "description" of the rule',
})

dict['alerts'] = copy.deepcopy(generic)
dict['alerts'].update({
    'table_description' : 'This table stores Reporting Alert events.',
    'description' : 'The description from the alert rule.',
    'summary_text' : 'The summary text of the alert',
    'json' : 'The summary JSON representation of the event causing the alert',
})

dict['host_table_updates'] = copy.deepcopy(generic)
dict['host_table_updates'].update({
    'table_description' : 'This table stores Host Table metadata updates',
    'address' : 'The IP address of the host',
    'key' : 'The key being updated',
    'value' : 'The new value for the key',
})

dict['quotas'] = copy.deepcopy(generic)
dict['quotas'].update({
    'table_description' : 'This table stores Quota events',
    'address' : 'The IP address of the host',
    'action' : 'The action (1=Quota Given, 2=Quota Exceeded)',
    'size' : 'The size of the quota',
    'reason' : 'The reason for the action',
})

dict['penaltybox'] = copy.deepcopy(generic)
dict['penaltybox'].update({
    'table_description' : 'This table stores Penalty Box events',
    'address' : 'The IP address of the host',
    'reason' : 'The reason for the action',
    'start_time' : 'The time the client entered the penalty box',
    'end_time' : 'The time the client exited the penalty box',
})

dict['sessions'] = copy.deepcopy(generic)
dict['sessions'].update({
    'table_description' : 'This table stores all scanned TCP/UDP sessions.',
    'end_time' : 'The time the session ended',
    'bypassed' : 'True if the session was bypassed, false otherwise',
    'c2p_bytes' : 'The number of bytes the client sent to Untangle (client-to-pipeline)',
    'p2c_bytes' : 'The number of bytes Untangle sent to client (pipeline-to-client)',
    's2p_bytes' : 'The number of bytes the server sent to Untangle (client-to-pipeline)',
    'p2s_bytes' : 'The number of bytes Untangle sent to server (pipeline-to-client)',
    'shield_blocked' : 'True if the shield blocked the session, false otherwise',
    'firewall_blocked' : 'True if Firewall blocked the session, false otherwise',
    'firewall_flagged' : 'True if Firewall flagged the session, false otherwise',
    'firewall_rule_index' : 'The matching rule in Firewall (if any)',
    'application_control_lite_protocol' : 'The application protocol according to Application Control Lite',
    'application_control_lite_blocked' : 'True if Application Control Lite blocked the session',
    'captive_portal_blocked' : 'True if Captive Portal blocked the session',
    'captive_portal_rule_index' : 'The matching rule in Captive Portal (if any)',
    'application_control_application' : 'The application according to Application Control',
    'application_control_protochain' : 'The protochain according to Application Control',
    'application_control_blocked' : 'True if Application Control blocked the session',
    'application_control_flagged' : 'True if Application Control flagged the session',
    'application_control_confidence' : 'True if Application Control confidence of this session\'s identification',
    'application_control_ruleid' : 'The matching rule in Application Control (if any)',
    'application_control_detail' : 'The text detail from the Application Control engine',
    'bandwidth_control_priority' : 'The priority given to this session',
    'bandwidth_control_rule' : 'The matching rule in Bandwidth Control rule (if any)',
    'ssl_inspector_ruleid' : 'The matching rule in HTTPS Inspector rule (if any)',
    'ssl_inspector_status' : 'The status/action of the SSL session (INSPECTED/IGNORED/BLOCKED/UNTRUSTED/ABANDONED)',
    'ssl_inspector_detail' : 'Additional text detail about the SSL connection (SNI, IP Address)',
})

dict['admin_logins'] = copy.deepcopy(generic)
dict['admin_logins'].update({
    'table_description' : 'This table stores all administrator login attempts.',
    'login' : 'The login name',
    'local' : 'True if it is a login attempt through a local process',
    'client_addr' : 'The client IP address',
    'succeeded' : 'True if the login succeeded, false otherwise',
    'reason' : 'The reason for the login (if applicable)',
})



print "= Database Tables ="

p = subprocess.Popen(["sh","-c","psql -A -t -U postgres uvm -c \"SELECT table_name FROM information_schema.tables where table_schema = 'reports' and table_name not like '%0%'\""], stdout=subprocess.PIPE)
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
    table_dict = dict.get(table_name)
    if table_dict == None:
        print "\nMissing documentation for table \"%s\"" % ( table_name  )
        sys.exit(1)
    if table_dict.get('table_description') == None:
        print "\nMissing description for table \"%s\"" % ( table_name  )
        sys.exit(1)

    print 
    print "== %s == " % table_name
    print 
    print "{| border=\"1\" cellpadding=\"2\""
    print "!Column Name"
    print "!Type"
    print "!Description"
    print "|-"

    p2 = subprocess.Popen(["sh","-c","psql -A -t -U postgres uvm -c \"\\d+ reports.%s\"" % table_name], stdout=subprocess.PIPE)
    for line2 in iter(p2.stdout.readline, ''):
        parts = line2.split("|")
        column = parts[0]
        type = parts[1]
        description = None

        try:
            description = dict[table_name][column]
        except:
            print "\nMissing description for column \"%s\" in table \"%s\"" % ( column, table_name  )
            sys.exit(1)

        if description == None:
            print "\nMissing description for column \"%s\" in table \"%s\"" % ( column, table_name  )
            sys.exit(1)

        print "|%s" % column
        print "|%s" % type
        print "|%s" % description
        print "|-"
        
    print "|}"


