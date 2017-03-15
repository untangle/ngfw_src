Ext.define ('Ung.model.AppMetric', {
    extend: 'Ext.data.Model' ,
    fields: [
        { name: 'displayName', type: 'string' },
        { name: 'name', type: 'string' },
        { name: 'value', type: 'int' },
        { name: 'javaClass', type: 'string', defaultValue: 'com.untangle.uvm.node.AppMetric' }
    ],
    proxy: {
        autoLoad: true,
        type: 'memory',
        reader: {
            type: 'json'
            //rootProperty: 'list'
        }
    }
});
