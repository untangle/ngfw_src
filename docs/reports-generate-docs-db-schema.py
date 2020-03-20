#!/usr/bin/python -u

import subprocess
import sys
import copy


human_names = {
'action': 'Action',
'active_hosts': 'Active Hosts',
'ad_blocker_action': 'Ad Blocker ' + 'Action',
'ad_blocker_cookie_ident': 'Ad Blocker ' + 'Cookie',
'addr': 'Address',
'addr_kind': 'Address Kind',
'addr_name': 'Address Name',
'address': 'Address',
'application_control_application': 'Application Control ' + 'Application',
'application_control_blocked': 'Application Control ' + 'Blocked',
'application_control_category': 'Application Control ' + 'Category',
'application_control_confidence': 'Application Control ' + 'Confidence',
'application_control_detail': 'Application Control ' + 'Detail',
'application_control_flagged': 'Application Control ' + 'Flagged',
'application_control_lite_blocked': 'Application Control Lite ' + 'Blocked',
'application_control_lite_protocol': 'Application Control Lite ' + 'Protocol',
'application_control_protochain': 'Application Control ' + 'Protochain',
'application_control_ruleid': 'Application Control ' + 'Rule ID',
'auth_type': 'Authorization Type',
'bandwidth_control_priority': 'Bandwidth Control ' + 'Priority',
'bandwidth_control_rule': 'Bandwidth Control ' + 'Rule ID',
'blocked': 'Blocked',
'bypassed': 'Bypassed',
'bypasses': 'Bypasses',
'c2p_bytes': 'From-Client Bytes',
'c2s_bytes': 'From-Client Bytes',
'c2s_content_length': 'Client-to-server Content Length',
'c_client_addr': 'Client-side Client Address',
'c_client_port': 'Client-side Client Port',
'c_server_addr': 'Client-side Server Address',
'c_server_port': 'Client-side Server Port',
'captive_portal_blocked': 'Captive Portal ' + 'Blocked',
'captive_portal_rule_index': 'Captive Portal ' + 'Rule ID',
'category': 'Category',
'class_id': 'Classtype ID',
'classtype': 'Classtype',
'client_addr': 'Client Address',
'client_address': 'Client Address',
'client_country': 'Client Country',
'client_intf': 'Client Interface',
'client_latitude': 'Client Latitude',
'client_longitude': 'Client Longitude',
'client_name': 'Client Name',
'client_protocol': 'Client Protocol',
'client_username': 'Client Username',
'connect_stamp': 'Connect Time',
'cpu_system': 'CPU System Utilization',
'cpu_user': 'CPU User Utilization',
'description': 'Text detail of the event',
'dest_addr': 'Destination Address',
'dest_port': 'Destination Port',
'destination': 'Destination',
'disk_free': 'Disk Free',
'disk_total': 'Disk Size',
'domain': 'Domain',
'elapsed_time': 'Elapsed Time',
'end_time': 'End Time',
'entitled': 'Entitled',
'entity': 'Entity',
'event_id': 'Event ID',
'event_info': 'Event Type',
'event_type': 'Event Type',
'filter_prefix': 'Filter Block',
'firewall_blocked': 'Firewall ' + 'Blocked',
'firewall_flagged': 'Firewall ' + 'Flagged',
'firewall_rule_index': 'Firewall ' + 'Rule ID',
'flagged': 'Flagged',
'gen_id': 'Grouping ID',
'goodbye_stamp': 'End Time',
'hit_bytes': 'Hit Bytes',
'hits': 'Hits',
'host': 'Host',
'hostname': 'Hostname',
'icmp_type': 'ICMP Type',
'in_bytes': 'In Bytes',
'interface_id': 'Interface ID',
'ipaddr': 'Client Address',
'json': 'JSON Text',
'key': 'Key',
'load_1': 'CPU load (1-min)',
'load_15': 'CPU load (15-min)',
'load_5': 'CPU load (5-min)',
'local': 'Local',
'local_addr': 'Local Address',
'local_address': 'Local Address',
'login': 'Login',
'login_name': 'Login Name',
'login_type': 'Login Type',
'mac_address': 'MAC Address',
'mem_buffers': 'Memory Buffers',
'mem_cache': 'Memory Cache',
'mem_free': 'Memory Free',
'mem_total': 'Total Memory',
'method': 'Method',
'miss_bytes': 'Miss Bytes',
'misses': 'Misses',
'msg': 'Message',
'msg_id': 'Message ID',
'name': 'Interface Name',
'net_interface': 'Net Interface',
'net_process': 'Net Process',
'old_value': 'Old Value',
'os_name': 'Interface O/S Name',
'out_bytes': 'Out Bytes',
'p2c_bytes': 'To-Client Bytes',
'p2s_bytes': 'To-Server Bytes',
'phish_blocker_action': 'Phish Blocker ' + 'Action',
'phish_blocker_is_spam': 'Phish Blocker ' + 'Phish',
'phish_blocker_score': 'Phish Blocker ' + 'Score',
'phish_blocker_tests_string': 'Phish Blocker ' + 'Tests',
'policy_id': 'Policy ID',
'policy_rule_id': 'Policy Rule ID',
'pool_address': 'Pool Address',
'protocol': 'Protocol',
'reason': 'Reason',
'receiver': 'Receiver',
'referer': 'Referer',
'remote_addr': 'Remote Address',
'remote_address': 'Remote Address',
'remote_port': 'Remote Port',
'request_id': 'Request ID',
'rid': 'Rule ID',
'rule_id': 'Rule ID',
'rx_bytes': 'Bytes Received',
'rx_rate': 'Rx Rate',
's2c_bytes': 'From-Server Bytes',
's2c_content_length': 'Server-to-client Content Length',
's2c_content_type': 'Server-to-client Content Type',
's2c_content_filename': 'Server-to-client Content Disposition Filename',
's2p_bytes': 'From-Server Bytes',
's_client_addr': 'Server-side Client Address',
's_client_port': 'Server-side Client Port',
's_server_addr': 'Server-side Server Address',
's_server_port': 'Server-side Server Port',
'sender': 'Sender',
'server_address': 'Server IP Address',
'server_country': 'Server Country',
'server_intf': 'Server Interface',
'server_latitude': 'Server Latitude',
'server_longitude': 'Server Longitude',
'session_id': 'Session ID',
'settings_file': 'Settings File',
'sig_id': 'Signature ID',
'size': 'Size',
'source_addr': 'Source Address',
'source_port': 'Source Port',
'spam_blocker_action': 'Spam Blocker ' + 'Action',
'spam_blocker_is_spam': 'Spam Blocker ' + 'Spam',
'spam_blocker_lite_action': 'Spam Blocker Lite ' + 'Action',
'spam_blocker_lite_is_spam': 'Spam Blocker Lite ' + 'Spam',
'spam_blocker_lite_score': 'Spam Blocker Lite ' + 'Score',
'spam_blocker_lite_tests_string': 'Spam Blocker Lite ' + 'Tests',
'spam_blocker_score': 'Spam Blocker ' + 'Score',
'spam_blocker_tests_string': 'Spam Blocker ' + 'Tests',
'ssl_inspector_detail': 'SSL Inspector ' + 'Detail',
'ssl_inspector_ruleid': 'SSL Inspector ' + 'Rule ID',
'ssl_inspector_status': 'SSL Inspector ' + 'Status',
'start_time': 'Start Time',
'subject': 'Subject',
'succeeded': 'Succeeded',
'success': 'Success',
'summary_text': 'Summary Text',
'swap_free': 'Swap Free',
'swap_total': 'Swap Size',
'systems': 'System bypasses',
'tags': 'Tags',
'term': 'Search Term',
'time_stamp': 'Timestamp',
'threat_prevention_blocked': 'Threat Prevention ' + 'Blocked',
'threat_prevention_flagged': 'Threat Prevention ' + 'Flagged',
'threat_prevention_reason': 'Threat Prevention ' + 'Reason',
'threat_prevention_rule_id': 'Threat Prevention ' + 'Rule Id',
'threat_prevention_reputation': 'Threat Prevention ' + 'Reputation',
'threat_prevention_client_reputation': 'Threat Prevention ' + 'Client Reputation',
'threat_prevention_server_reputation': 'Threat Prevention ' + 'Server Reputation',
'threat_prevention_categories': 'Threat Prevention ' + 'Categories',
'threat_prevention_client_categories': 'Threat Prevention ' + 'Client Categories',
'threat_prevention_server_categories': 'Threat Prevention ' + 'Server Categories',
'tunnel_description': 'Tunnel Description',
'tunnel_name': 'Tunnel Name',
'tx_bytes': 'Bytes Sent',
'tx_rate': 'Tx Rate',
'type': 'Type',
'uri': 'URI',
'username': 'Username',
'value': 'Value',
'vendor_name': 'Vendor Name',
'virus_blocker_clean': 'Virus Blocker ' + 'Clean',
'virus_blocker_lite_clean': 'Virus Blocker Lite ' + 'Clean',
'virus_blocker_lite_name': 'Virus Blocker Lite ' + 'Name',
'virus_blocker_name': 'Virus Blocker ' + 'Name',
'web_filter_blocked': 'Web Filter ' + 'Blocked',
'web_filter_category': 'Web Filter ' + 'Category',
'web_filter_category_id': 'Web Filter ' + 'Category Id',
'web_filter_rule_id': 'Web Filter ' + 'Rule Id',
'web_filter_flagged': 'Web Filter ' + 'Flagged',
'web_filter_reason': 'Web Filter ' + 'Reason',
}

dict = {};

generic = {
    'event_id' : 'The unique event ID',
    'time_stamp' : 'The time of the event',
    'session_id' : 'The session',
    'client_intf' : 'The client interface',
    'client_country' : 'The client Country',
    'client_latitude' : 'The client Latitude',
    'client_longitude' : 'The client Longitude',
    'server_intf' : 'The server interface',
    'server_country' : 'The server Country',
    'server_latitude' : 'The server Latitude',
    'server_longitude' : 'The server Longitude',
    'c_client_addr' : 'The client-side client IP address',
    's_client_addr' : 'The server-side client IP address',
    'c_server_addr' : 'The client-side server IP address',
    's_server_addr' : 'The server-side server IP address',
    'c_client_port' : 'The client-side client port',
    's_client_port' : 'The server-side client port',
    'c_server_port' : 'The client-side server port',
    's_server_port' : 'The server-side server port',
    'policy_id' : 'The policy',
    'username' : 'The username associated with this session',
    'hostname' : 'The hostname of the local address',
    'filter_prefix' : 'The network filter that blocked the connection (filter,shield,invalid)',
    'c2s_content_length' : 'The client-to-server content length',
    's2c_content_length' : 'The server-to-client content length',
    's2c_content_type' : 'The server-to-client content type',
    's2c_content_filename' : 'The server-to-client content disposition filename',
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
    'type': 'The type of the event (CONNECT,DISCONNECT)',
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

dict['ipsec_tunnel_stats'] = copy.deepcopy(generic)
dict['ipsec_tunnel_stats'].update({
    'table_description' : 'This table stores IPsec tunnel statistics.',
    'tunnel_name' : 'The name of the IPsec tunnel',
    'in_bytes' : 'The number of bytes received during this time frame',
    'out_bytes' : 'The number of bytes transmitted during this time frame',
})

dict['ipsec_vpn_events'] = copy.deepcopy(generic)
dict['ipsec_vpn_events'].update({
    'table_description' : 'This table stores IPsec tunnel connection events.',
    'local_address' : 'The local address of the tunnel',
    'remote_address' : 'The remote address of the tunnel',
    'tunnel_description' : 'The description of the tunnel',
    'event_type': 'The type of the event (CONNECT,DISCONNECT)',
})

dict['tunnel_vpn_stats'] = copy.deepcopy(generic)
dict['tunnel_vpn_stats'].update({
    'table_description' : 'This table stores Tunnel VPN tunnel statistics.',
    'tunnel_name' : 'The name of the Tunnel VPN tunnel',
    'in_bytes' : 'The number of bytes received during this time frame',
    'out_bytes' : 'The number of bytes transmitted during this time frame',
})

dict['tunnel_vpn_events'] = copy.deepcopy(generic)
dict['tunnel_vpn_events'].update({
    'table_description' : 'This table stores Tunnel VPN connection events.',
    'server_address' : 'The address of the remote server',
    'local_address' : 'The local address assigned the client',
    'tunnel_name' : 'The name the tunnel',
    'type': 'The type of the event (CONNECT,DISCONNECT)',
    'event_type': 'The type of the event (CONNECT,DISCONNECT)',
})

dict['wireguard_vpn_stats'] = copy.deepcopy(generic)
dict['wireguard_vpn_stats'].update({
    'table_description' : 'This table stores Wireguard VPN traffic statistics.',
    'tunnel_name' : 'The name of the Wireguard tunnel',
    'peer_address' : 'The IP address of the tunnel peer',
    'in_bytes' : 'The number of bytes received during this time frame',
    'out_bytes' : 'The number of bytes transmitted during this time frame',
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
    'mem_total' : 'The total bytes of memory',
    'active_hosts' : 'The number of active hosts',
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

dict['web_cache_stats'] = copy.deepcopy(generic)
dict['web_cache_stats'].update({
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
    'destination' : 'The location of the backup',
})

dict['captive_portal_user_events'] = copy.deepcopy(generic)
dict['captive_portal_user_events'].update({
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
    'domain' : 'The HTTP domain (shortened host)',
    'method' : 'The HTTP method',
    'uri' : 'The HTTP URI',
    'host' : 'The HTTP host',
    'referer' : 'The Referer URL',
    'ad_blocker_cookie_ident' : 'This name of cookie blocked by Ad Blocker',
    'ad_blocker_action' : 'This action of Ad Blocker on this request',
    'web_filter_reason' : 'This reason Web Filter blocked/flagged this request',
    'web_filter_category' : 'This category according to Web Filter',
    'web_filter_category_id' : 'This numeric category according to Web Filter',
    'web_filter_rule_id' : 'This numeric rule according to Web Filter',
    'web_filter_blocked' : 'If Web Filter blocked this request',
    'web_filter_flagged' : 'If Web Filter flagged this request',
    'threat_prevention_blocked' : 'If Threat Prevention blocked this request',
    'threat_prevention_flagged' : 'If Threat Prevention flagged this request',
    'threat_prevention_rule_id' : 'This numeric rule according to Threat Prevention',
    'threat_prevention_reputation' : 'This numeric threat reputation',
    'threat_prevention_categories' : 'This bitmask of threat categories',
})

dict['http_query_events'] = copy.deepcopy(generic)
dict['http_query_events'].update({
    'table_description' : 'This table stores search engine (google, etc) queries.',
    'request_id' : 'The HTTP request ID',
    'method' : 'The HTTP method',
    'uri' : 'The HTTP URI',
    'term' : 'The search term',
    'host' : 'The HTTP host',
    'web_filter_reason' : 'This reason Web Filter blocked/flagged this request',
    'blocked' : 'If Web Filter blocked this search term',
    'flagged' : 'If Web Filter flagged this search term',
})

dict['wan_failover_action_events'] = copy.deepcopy(generic)
dict['wan_failover_action_events'].update({
    'table_description' : 'This table stores WAN Failover events. There is one row for each WAN status change.',
    'interface_id' : 'This interface ID',
    'action' : 'This action (CONNECTED,DISCONNECTED)',
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
    'login_type' : 'The login type',
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
    'category' : 'The application specific grouping for the signature',
    'classtype' : 'The generalized threat signature grouping (unrelated to gen_id)',
    'msg' : 'The "title" or "description" of the signature',
    'rid' : 'The rule id',
    'rule_id' : 'The rule id',
})

dict['alerts'] = copy.deepcopy(generic)
dict['alerts'].update({
    'table_description' : 'This table stores Alert events.',
    'description' : 'The description from the alert rule.',
    'summary_text' : 'The summary text of the alert',
    'json' : 'The summary JSON representation of the event causing the alert',
})

dict['syslog'] = copy.deepcopy(generic)
dict['syslog'].update({
    'table_description' : 'This table stores Syslog events.',
    'description' : 'The description from the alert rule.',
    'summary_text' : 'The summary text of the alert',
    'json' : 'The summary JSON representation of the event causing the alert',
})

dict['host_table_updates'] = copy.deepcopy(generic)
dict['host_table_updates'].update({
    'table_description' : 'This table stores Host Table metadata updates',
    'address' : 'The IP address of the host',
    'key' : 'The key being updated',
    'old_value' : 'The old value for the key',
    'value' : 'The new value for the key',
})

dict['device_table_updates'] = copy.deepcopy(generic)
dict['device_table_updates'].update({
    'table_description' : 'This table stores Device Table metadata updates',
    'mac_address' : 'The MAC address of the device',
    'key' : 'The key being updated',
    'old_value' : 'The old value for the key',
    'value' : 'The new value for the key',
})

dict['user_table_updates'] = copy.deepcopy(generic)
dict['user_table_updates'].update({
    'table_description' : 'This table stores Device Table metadata updates',
    'username' : 'The username',
    'key' : 'The key being updated',
    'old_value' : 'The old value for the key',
    'value' : 'The new value for the key',
})

dict['quotas'] = copy.deepcopy(generic)
dict['quotas'].update({
    'table_description' : 'This table stores Quota events',
    'entity' : 'The IP entity given the quota (address/username)',
    'action' : 'The action (1=Quota Given, 2=Quota Exceeded)',
    'size' : 'The size of the quota',
    'reason' : 'The reason for the action',
})

dict['sessions'] = copy.deepcopy(generic)
dict['sessions'].update({
    'table_description' : 'This table stores all scanned TCP/UDP sessions.',
    'protocol' : 'The IP protocol of session',
    'policy_rule_id' : 'The ID of the matching policy rule (0 means none)',
    'icmp_type' : 'The ICMP type of session if ICMP',
    'end_time' : 'The time the session ended',
    'bypassed' : 'True if the session was bypassed, false otherwise',
    'entitled' : 'True if the session is entitled to premium functionality',
    'local_addr' : 'The IP address of the local participant',
    'remote_addr' : 'The IP address of the remote participant',
    'c2p_bytes' : 'The number of bytes the client sent to Untangle (client-to-pipeline)',
    'p2c_bytes' : 'The number of bytes Untangle sent to client (pipeline-to-client)',
    's2p_bytes' : 'The number of bytes the server sent to Untangle (client-to-pipeline)',
    'p2s_bytes' : 'The number of bytes Untangle sent to server (pipeline-to-client)',
    'firewall_blocked' : 'True if Firewall blocked the session, false otherwise',
    'firewall_flagged' : 'True if Firewall flagged the session, false otherwise',
    'firewall_rule_index' : 'The matching rule in Firewall (if any)',
    'application_control_lite_protocol' : 'The application protocol according to Application Control Lite',
    'application_control_lite_blocked' : 'True if Application Control Lite blocked the session',
    'captive_portal_blocked' : 'True if Captive Portal blocked the session',
    'captive_portal_rule_index' : 'The matching rule in Captive Portal (if any)',
    'application_control_application' : 'The application according to Application Control',
    'application_control_protochain' : 'The protochain according to Application Control',
    'application_control_category' : 'The category according to Application Control',
    'application_control_blocked' : 'True if Application Control blocked the session',
    'application_control_flagged' : 'True if Application Control flagged the session',
    'application_control_confidence' : 'True if Application Control confidence of this session\'s identification',
    'application_control_ruleid' : 'The matching rule in Application Control (if any)',
    'application_control_detail' : 'The text detail from the Application Control engine',
    'bandwidth_control_priority' : 'The priority given to this session',
    'bandwidth_control_rule' : 'The matching rule in Bandwidth Control rule (if any)',
    'ssl_inspector_ruleid' : 'The matching rule in SSL Inspector rule (if any)',
    'ssl_inspector_status' : 'The status/action of the SSL session (INSPECTED,IGNORED,BLOCKED,UNTRUSTED,ABANDONED)',
    'ssl_inspector_detail' : 'Additional text detail about the SSL connection (SNI, IP Address)',
    'tags' : 'The tags on this session',
    'threat_prevention_blocked' : 'If Threat Prevention blocked',
    'threat_prevention_flagged' : 'If Threat Prevention flagged',
    'threat_prevention_reason' : 'Threat Prevention reason',
    'threat_prevention_rule_id' : 'Numeric rule id of Threat Prevention',
    'threat_prevention_client_reputation' : 'Numeric client reputation of Threat Prevention',
    'threat_prevention_client_categories' : 'Bitmask client categories of Threat Prevention',
    'threat_prevention_server_reputation' : 'Numeric server reputation of Threat Prevention',
    'threat_prevention_server_categories' : 'Bitmask server categories of Threat Prevention',
})

dict['session_minutes'] = copy.deepcopy(dict['sessions'])
dict['session_minutes'].update({
    'start_time' : 'The start time of the session',
    'c2s_bytes' : 'The number of bytes the client sent',
    's2c_bytes' : 'The number of bytes the server sent',
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

dict['settings_changes'] = copy.deepcopy(generic)
dict['settings_changes'].update({
    'table_description' : 'This table stores settings changes.',
    'settings_file' : 'The name of the file changed',
    'username' : 'The username logged in at the time of the change',
    'hostname' : 'The remote hostname',
})

dict['interface_stat_events'] = copy.deepcopy(generic)
dict['interface_stat_events'].update({
    'table_description' : 'This table stores stats for interfaces.',
    'interface_id' : 'The interface ID',
    'rx_rate' : 'The RX rate (bytes/s)',
    'tx_rate' : 'The TX rate (bytes/s)',
})



print("= Database Tables =")

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
        print("\nMissing documentation for table \"%s\"" % ( table_name  ))
        sys.exit(1)
    if table_dict.get('table_description') == None:
        print("\nMissing description for table \"%s\"" % ( table_name  ))
        sys.exit(1)

    print("")
    print("== %s == " % table_name)
    print("<section begin='%s' />" % table_name)
    print("")
    print("{| border=\"1\" cellpadding=\"2\" width=\"90%%\" align=\"center\"")
    print("!Column Name")
    print("!Human Name")
    print("!Type")
    print("!Description")
    print("|-")

    p2 = subprocess.Popen(["sh","-c","psql -A -t -U postgres uvm -c \"\\d+ reports.%s\"" % table_name], stdout=subprocess.PIPE)
    for line2 in iter(p2.stdout.readline, ''):
        parts = line2.split("|")
        column = parts[0]
        type = parts[1]
        description = None
        try:
            human_name = human_names[column]
        except:
            print("\nMissing human_name for column \"%s\" in table \"%s\"" % ( column, table_name  ))
            sys.exit(1)

        try:
            description = dict[table_name][column]
        except:
            print("\nMissing description for column \"%s\" in table \"%s\"" % ( column, table_name  ))
            sys.exit(1)

        if description == None:
            print("\nMissing description for column \"%s\" in table \"%s\"" % ( column, table_name  ))
            sys.exit(1)

        print("|%s" % column)
        print("|%s" % human_name)
        print("|%s" % type)
        print("|%s" % description)
        print("|-")

    print("|}")
    print("<section end='%s' />" % table_name)
    print()