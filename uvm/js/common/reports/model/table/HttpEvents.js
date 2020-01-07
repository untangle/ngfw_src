/**
 * HttpEvents model definition
 * matching the http-events sql table fields
 */
Ext.define ('Ung.model.http_events', {
    extend: 'Ext.data.Model',
    proxy: {
        type: 'memory',
        reader: {
            type: 'json'
        }
    },
    fields: [
        { name: 'request_id', type: 'string' },
        { name: 'time_stamp', type: 'auto', convert: Converter.timestamp },
        { name: 'session_id', type: 'string' },
        { name: 'client_intf', type: 'integer', convert: Converter.interface },
        { name: 'server_intf', type: 'integer', convert: Converter.interface },
        { name: 'c_client_addr', type: 'string' },
        { name: 's_client_addr', type: 'string' },
        { name: 'c_server_addr', type: 'string' },
        { name: 's_server_addr', type: 'string' },
        { name: 'c_client_port', type: 'integer' },
        { name: 's_client_port', type: 'integer' },
        { name: 'c_server_port', type: 'integer' },
        { name: 's_server_port', type: 'integer' },
        { name: 'client_country', type: 'string', convert: Converter.country },
        { name: 'client_latitude', type: 'string' },
        { name: 'client_longitude', type: 'string' },
        { name: 'server_country', type: 'string', convert: Converter.country },
        { name: 'server_latitude', type: 'string' },
        { name: 'server_longitude', type: 'string' },
        { name: 'policy_id', type: 'integer', convert: Converter.policy },
        { name: 'username', type: 'string' },
        { name: 'hostname', type: 'string' },
        { name: 'method', type: 'string' },
        { name: 'uri', type: 'string' },
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
        { name: 'virus_blocker_lite_clean', type: 'boolean' },
        { name: 'virus_blocker_lite_name', type: 'string' },
        { name: 'virus_blocker_clean', type: 'boolean' },
        { name: 'virus_blocker_name', type: 'string' },
        { name: 'threat_prevention_blocked', type: 'boolean' },
        { name: 'threat_prevention_flagged', type: 'boolean' },
        { name: 'threat_prevention_rule_id', type: 'integer' },
        { name: 'threat_prevention_reputation', type: 'integer' },
        { name: 'threat_prevention_categories', type: 'integer' }
    ]
});
