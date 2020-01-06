/**
 * DeviceTableUpdates model definition
 * matching the device_table_updates sql table fields
 */
Ext.define ('Ung.model.device_table_updates', {
    extend: 'Ext.data.Model',
    proxy: {
        type: 'memory',
        reader: {
            type: 'json'
        }
    },
    fields: [
        { name: 'time_stamp', type: 'auto', convert: Converter.timestamp },
        { name: 'mac_address', type: 'string' },
        { name: 'key', type: 'string' },
        { name: 'value', type: 'string' },
        { name: 'old_value', type: 'string' }
    ]
});
