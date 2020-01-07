/**
 * All Fields
 * might replace all the other table model files
 */
Ext.define ('Ung.model.Fields', {
    extend: 'Ext.data.Model',
    fields: [
        { name: 'session_id', type: 'string' },
        { name: 'time_stamp', type: 'auto', convert: Converter.timestamp },
        { name: 'end_time', type: 'auto', convert: Converter.timestamp },
        { name: 'bypassed', type: 'boolean' },
        { name: 'entitled', type: 'boolean' },
        { name: 'protocol', type: 'integer', convert: Converter.protocol },
        { name: 'icmp_type', type: 'integer' },
        { name: 'hostname', type: 'string' },
        { name: 'username', type: 'string' },
        { name: 'policy_id', type: 'integer', convert: Converter.policy },
        { name: 'policy_rule_id', type: 'integer' },
        { name: 'local_address', type: 'string' },
        { name: 'remote_address', type: 'string' },
        { name: 'c_client_addr', type: 'string' },
        { name: 'c_server_addr', type: 'string' },
        { name: 'c_client_port', type: 'integer' },
        { name: 'c_server_port', type: 'integer' },
        { name: 's_client_addr', type: 'string' },
        { name: 's_server_addr', type: 'string' },
        { name: 's_client_port', type: 'integer' },
        { name: 's_server_port', type: 'integer' },
        { name: 'client_intf', type: 'integer', convert: Converter.interface },
        { name: 'server_intf', type: 'integer', convert: Converter.interface },
        { name: 'client_country', type: 'string', convert: Converter.country },
        { name: 'client_latitude', type: 'string' },
        { name: 'client_longitude', type: 'string' },
        { name: 'server_country', type: 'string', convert: Converter.country },
        { name: 'server_latitude', type: 'string' },
        { name: 'server_longitude', type: 'string' },
        { name: 'c2p_bytes', type: 'integer' },
        { name: 'p2c_bytes', type: 'integer' },
        { name: 's2p_bytes', type: 'integer' },
        { name: 'p2s_bytes', type: 'integer' },
        { name: 'filter_prefix', type: 'string' },

        { name: 'firewall_blocked', type: 'boolean' },
        { name: 'firewall_flagged', type: 'boolean' },
        { name: 'firewall_rule_index', type: 'integer' },

        { name: 'threat_prevention_blocked', type: 'boolean' },
        { name: 'threat_prevention_flagged', type: 'boolean' },
        { name: 'threat_prevention_reason', type: 'string' },
        { name: 'threat_prevention_rule_id', type: 'integer' },
        { name: 'threat_prevention_client_reputation', type: 'integer' },
        { name: 'threat_prevention_client_categories', type: 'integer' },
        { name: 'threat_prevention_server_reputation', type: 'integer' },
        { name: 'threat_prevention_server_categories', type: 'integer' },
        { name: 'threat_prevention_reputation', type: 'integer' },
        { name: 'threat_prevention_categories', type: 'integer' },


        { name: 'application_control_lite_protocol', type: 'string' },
        { name: 'application_control_lite_blocked', type: 'boolean' },

        { name: 'captive_portal_blocked', type: 'boolean' },
        { name: 'captive_portal_rule_index', type: 'integer' },

        { name: 'application_control_application', type: 'string' },
        { name: 'application_control_protochain', type: 'string' },
        { name: 'application_control_category', type: 'string' },
        { name: 'application_control_blocked', type: 'boolean' },
        { name: 'application_control_flagged', type: 'boolean' },
        { name: 'application_control_confidence', type: 'integer' },
        { name: 'application_control_ruleid', type: 'integer' },
        { name: 'application_control_detail', type: 'string' },

        { name: 'bandwidth_control_priority', type: 'integer' },
        { name: 'bandwidth_control_rule', type: 'integer' },

        { name: 'ssl_inspector_ruleid', type: 'integer' },
        { name: 'ssl_inspector_status', type: 'string' },
        { name: 'ssl_inspector_detail', type: 'string' },

        { name: 'tags', type: 'string' },

        { name: 'login', type: 'string' },
        { name: 'local', type: 'boolean' },
        { name: 'client_addr', type: 'string' },
        { name: 'succeeded', type: 'boolean' },
        { name: 'reason', type: 'string' },


        { name: 'description', type: 'string' },
        { name: 'summary_text', type: 'string' },
        { name: 'json', type: 'string' },

        { name: 'mac_address', type: 'string' },
        { name: 'key', type: 'string' },
        { name: 'value', type: 'string' },
        { name: 'old_value', type: 'string' },

        { name: 'event_id', type: 'string' },
        { name: 'request_id', type: 'string' },
        { name: 'method', type: 'string' },
        { name: 'uri', type: 'string' },

        { name: 'virus_blocker_lite_clean', type: 'boolean' },
        { name: 'virus_blocker_lite_name', type: 'string' },
        { name: 'virus_blocker_clean', type: 'boolean' },
        { name: 'virus_blocker_name', type: 'string' },

        { name: 'address', type: 'string' },

        { name: 'host', type: 'string' },
        { name: 'domain', type: 'string' },
        { name: 'referer', type: 'string' },
        { name: 'c2s_content_length', type: 'integer' },
        { name: 's2c_content_length', type: 'integer' },
        { name: 's2c_content_type', type: 'string' },
        { name: 's2c_content_filename', type: 'string' },
        { name: 'ad_blocker_cookie_ident', type: 'string' },
        { name: 'ad_blocker_action', type: 'string' },

        { name: 'web_filter_reason', type: 'string', convert: Converter.webReason },
        { name: 'web_filter_category_id', type: 'integer', convert: Converter.webCategory },
        { name: 'web_filter_rule_id', type: 'integer' },
        { name: 'web_filter_blocked', type: 'boolean' },
        { name: 'web_filter_flagged', type: 'boolean' },


        { name: 'term', type: 'string' },
        { name: 'blocked', type: 'boolean' },
        { name: 'flagged', type: 'boolean' },



        { name: 'msg_id', type: 'string' },
        { name: 'subject', type: 'string' },
        { name: 'addr', type: 'string' },
        { name: 'addr_name', type: 'string' },
        { name: 'addr_kind', type: 'string' },


        { name: 'sender', type: 'string' },

        { name: 'spam_blocker_lite_score', type: 'number' },
        { name: 'spam_blocker_lite_is_spam', type: 'boolean' },
        { name: 'spam_blocker_lite_action', type: 'string' },
        { name: 'spam_blocker_lite_tests_string', type: 'string' },
        { name: 'spam_blocker_score', type: 'number' },
        { name: 'spam_blocker_is_spam', type: 'boolean' },
        { name: 'spam_blocker_action', type: 'string' },
        { name: 'spam_blocker_tests_string', type: 'string' },

        { name: 'phish_blocker_score', type: 'number' },
        { name: 'phish_blocker_is_spam', type: 'boolean' },
        { name: 'phish_blocker_tests_string', type: 'string' },
        { name: 'phish_blocker_action',  type: 'string' },



        { name: 'load_1',       type: 'number'},
        { name: 'load_5',       type: 'number'},
        { name: 'load_15',      type: 'number'},
        { name: 'cpu_user',     type: 'number'},
        { name: 'cpu_system',   type: 'number'},
        { name: 'mem_total',    type: 'integer'},
        { name: 'mem_free',     type: 'integer'},
        { name: 'disk_total',   type: 'integer'},
        { name: 'disk_free',    type: 'integer'},
        { name: 'swap_total',   type: 'integer'},
        { name: 'swap_free',    type: 'integer'},
        { name: 'active_hosts', type: 'integer'},

        { name: 'c2s_bytes', type: 'integer' },
        { name: 's2c_bytes', type: 'integer' },
        { name: 'start_time', type: 'auto', convert: Converter.timestamp },
        { name: 'local_addr', type: 'string' },
        { name: 'remote_addr', type: 'string' },




        { name: 'settings_file', type: 'string' },



        { name: 'ipaddr', type: 'string' },
        { name: 'vendor_name', type: 'string' },
    ]
});
