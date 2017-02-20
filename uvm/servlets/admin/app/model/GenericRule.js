Ext.define ('Ung.model.GenericRule', {
    extend: 'Ext.data.Model' ,
    fields: [
        { name: 'name', type: 'string', defaultValue: null },
        { name: 'string', type: 'string', defaultValue: '' },
        { name: 'blocked', type: 'boolean', defaultValue: true },
        { name: 'flagged', type: 'boolean', defaultValue: true },
        { name: 'category', type: 'string', defaultValue: null },
        { name: 'description', type: 'string', defaultValue: '' },
        { name: 'enabled', type: 'boolean', defaultValue: true },
        { name: 'id', defaultValue: null },
        { name: 'readOnly', type: 'boolean', defaultValue: null },
        { name: 'javaClass', type: 'string', defaultValue: 'com.untangle.uvm.node.GenericRule' }
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
