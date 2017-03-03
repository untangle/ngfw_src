Ext.define ('Ung.model.NodeMetric', {
    extend: 'Ext.data.Model' ,
    fields: [
        { name: 'displayName', type: 'string' },
        { name: 'name', type: 'string' },
        { name: 'value', type: 'int' },
        { name: 'javaClass', type: 'string', defaultValue: 'com.untangle.uvm.node.NodeMetric' }
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
