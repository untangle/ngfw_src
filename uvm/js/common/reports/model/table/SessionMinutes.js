/**
 * SessionMinutes model definition
 * matching the session_minutes sql table fields
 */
Ext.define ('Ung.model.session_minutes', {
    extend: 'Ext.data.Model',
    proxy: {
        type: 'memory',
        reader: {
            type: 'json'
        }
    },
    fields: [
        { name: 'session_id', type: 'string' },
        { name: 'time_stamp', type: 'auto', convert: Converter.timestamp },
        { name: 'c2s_bytes', type: 'integer' },
        { name: 's2c_bytes', type: 'integer' },
        { name: 'start_time', type: 'auto', convert: Converter.timestamp },
        { name: 'end_time', type: 'auto', convert: Converter.timestamp },
        { name: 'bypassed', type: 'boolean' },
        { name: 'entitled', type: 'boolean' },
        { name: 'protocol', type: 'integer', convert: Converter.protocol },
        { name: 'icmp_type', type: 'integer' },
        { name: 'hostname', type: 'string' },
        { name: 'username', type: 'string' },
        { name: 'policy_id', type: 'integer', convert: Converter.policy },
        { name: 'policy_rule_id', type: 'integer' },
        { name: 'local_addr', type: 'string' },
        { name: 'remote_addr', type: 'string' },
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

        { name: 'tags', type: 'string' }
    ]
});
