/**
 * SettingsChanges model definition
 * matching the settings_changes sql table fields
 */
Ext.define ('Ung.model.settings_changes', {
    extend: 'Ext.data.Model',
    proxy: {
        type: 'memory',
        reader: {
            type: 'json'
        }
    },
    fields: [
        { name: 'time_stamp', type: 'auto', convert: Converter.timestamp },
        { name: 'settings_file', type: 'string' },
        { name: 'username', type: 'string' },
        { name: 'hostname', type: 'string' }
    ]
});
