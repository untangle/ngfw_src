/**
 * AdminLogins model definition
 * matching the admin_logins sql table fields
 */
Ext.define ('Ung.model.admin_logins', {
    extend: 'Ext.data.Model',
    proxy: {
        type: 'memory',
        reader: {
            type: 'json'
        }
    },
    fields: [
        { name: 'time_stamp', type: 'auto', convert: Converter.timestamp },
        { name: 'login', type: 'string' },
        { name: 'local', type: 'boolean' },
        { name: 'client_addr', type: 'string' },
        { name: 'succeeded', type: 'boolean' },
        { name: 'reason', type: 'string' }
    ]
});
