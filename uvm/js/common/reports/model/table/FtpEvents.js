/**
 * FtpEvents model definition
 * matching the ftp-events sql table fields
 */
Ext.define ('Ung.model.ftp_events', {
    extend: 'Ext.data.Model',
    proxy: {
        type: 'memory',
        reader: {
            type: 'json'
        }
    },
    fields: [
        { name: 'time_stamp', type: 'auto', convert: Converter.timestamp },
        { name: 'event_id', type: 'string' },
        { name: 'session_id', type: 'string' },
        { name: 'client_intf', type: 'integer', convert: Converter.interface },
        { name: 'server_intf', type: 'integer', convert: Converter.interface },
        { name: 'c_client_addr', type: 'string' },
        { name: 's_client_addr', type: 'string' },
        { name: 'c_server_addr', type: 'string' },
        { name: 's_server_addr', type: 'string' },
        { name: 'policy_id', type: 'integer', convert: Converter.policy },
        { name: 'username', type: 'string' },
        { name: 'hostname', type: 'string' },
        { name: 'request_id', type: 'string' },
        { name: 'method', type: 'string' },
        { name: 'uri', type: 'string' },

        { name: 'virus_blocker_lite_clean', type: 'boolean' },
        { name: 'virus_blocker_lite_name', type: 'string' },
        { name: 'virus_blocker_clean', type: 'boolean' },
        { name: 'virus_blocker_name', type: 'string' }
    ]
});
