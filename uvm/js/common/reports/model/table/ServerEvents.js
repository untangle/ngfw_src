/**
 * ServerEvents model definition
 * matching the server-events sql table fields
 */
Ext.define ('Ung.model.server_events', {
    extend: 'Ext.data.Model',
    proxy: {
        type: 'memory',
        reader: {
            type: 'json'
        }
    },
    fields: [
        { name: 'time_stamp', type: 'auto', convert: Converter.timestamp },
        { name: 'load_1',       type: 'number'},
        { name: 'load_5',       type: 'number'},
        { name: 'load_15',      type: 'number'},
        { name: 'cpu_user',     type: 'number'},
        { name: 'cpu_system',   type: 'number'},
        { name: 'mem_total',    type: 'integer'},
        { name: 'mem_free',     type: 'integer'},
        { name: 'disk_total',   type: 'integer'},
        { name: 'disk_free',    type: 'integer'},
        { name: 'swap_total',   type: 'integer'},
        { name: 'swap_free',    type: 'integer'},
        { name: 'active_hosts', type: 'integer'}
    ]
});
