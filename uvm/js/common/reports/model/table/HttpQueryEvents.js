/**
 * HttpQueryEvents model definition
 * matching the http_query_events sql table fields
 */
Ext.define ('Ung.model.http_query_events', {
    extend: 'Ext.data.Model',
    proxy: {
        type: 'memory',
        reader: {
            type: 'json'
        }
    },
    fields: [
        { name: 'event_id', type: 'string' },
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
        { name: 'policy_id', type: 'integer', convert: Converter.policy },
        { name: 'username', type: 'string' },
        { name: 'hostname', type: 'string' },
        { name: 'request_id', type: 'string' },
        { name: 'method', type: 'string' },
        { name: 'uri', type: 'string' },
        { name: 'term', type: 'string' },
        { name: 'host', type: 'string' },
        { name: 'c2s_content_length', type: 'integer' },
        { name: 's2c_content_length', type: 'integer' },
        { name: 's2c_content_type', type: 'string' },
        { name: 'blocked', type: 'boolean' },
        { name: 'flagged', type: 'boolean' }
    ]
});
