/**
 * Alerts model definition
 * matching the alerts sql table fields
 */
Ext.define ('Ung.model.alerts', {
    extend: 'Ext.data.Model',
    proxy: {
        type: 'memory',
        reader: {
            type: 'json'
        }
    },
    fields: [
        { name: 'time_stamp', type: 'auto', convert: Converter.timestamp },
        { name: 'description', type: 'string' },
        { name: 'summary_text', type: 'string' },
        { name: 'json', type: 'string' }
    ]
});
