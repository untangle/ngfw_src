/**
 * HttpEvents model definition
 * matching the http-events sql table fields
 */
Ext.define ('Ung.model.host_table_updates', {
    extend: 'Ext.data.Model',
    proxy: {
        type: 'memory',
        reader: {
            type: 'json'
        }
    },
    fields: [
        { name: 'time_stamp', type: 'auto', convert: Converter.timestamp },
        { name: 'address', type: 'string' },
        { name: 'key', type: 'string' },
        { name: 'value', type: 'string' },
        { name: 'old_value', type: 'string' }
    ]
});
