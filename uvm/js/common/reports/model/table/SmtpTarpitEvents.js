/**
 * SmtpTarpitEvents model definition
 * matching the smtp_tarpit_events sql table fields
 */
Ext.define ('Ung.model.smtp_tarpit_events', {
    extend: 'Ext.data.Model',
    proxy: {
        type: 'memory',
        reader: {
            type: 'json'
        }
    },
    fields: [
        { name: 'time_stamp', type: 'auto', convert: Converter.timestamp },
        { name: 'ipaddr', type: 'string' },
        { name: 'hostname', type: 'string' },
        { name: 'policy_id', type: 'integer', convert: Converter.policy },
        { name: 'vendor_name', type: 'string' },
        { name: 'event_id', type: 'string' }
    ]
});
