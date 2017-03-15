Ext.define ('Ung.model.NodeProperty', {
    extend: 'Ext.data.Model' ,
    fields: [
        { name: 'autoLoad', type: 'bool' },
        { name: 'autoStart', type: 'bool' },
        { name: 'displayName', type: 'string' },
        { name: 'hasPowerButton', type: 'bool' },
        { name: 'invisible', type: 'bool' },
        { name: 'name', type: 'string' },
        { name: 'nodeBase', type: 'string' },
        { name: 'parents', type: 'auto' },
        { name: 'supportedArchitectures', type: 'auto' },
        { name: 'type', type: 'string' },
        { name: 'viewPosition', type: 'int' },
        { name: 'className', type: 'string' },
        { name: 'javaClass', type: 'string', defaultValue: 'com.untangle.uvm.node.AppProperties' }
    ]
});
