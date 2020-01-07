/**
 * UserTableUpdates model definition
 * matching the user_table_updates sql table fields
 */
Ext.define ('Ung.model.user_table_updates', {
    extend: 'Ext.data.Model',
    proxy: {
        type: 'memory',
        reader: {
            type: 'json'
        }
    },
    fields: [
        { name: 'time_stamp', type: 'auto', convert: Converter.timestamp },
        { name: 'username', type: 'string' },
        { name: 'key', type: 'string' },
        { name: 'value', type: 'string' },
        { name: 'old_value', type: 'string' }
    ]
});
