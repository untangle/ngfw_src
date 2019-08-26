Ext.define('Ung.apps.ipreputation.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-ip-reputation',
    controller: 'app-ip-reputation',

    viewModel: {
        stores: {
            passRules: { data: '{settings.passRules.list}' },
        }
    },

    items: [
        { xtype: 'app-ip-reputation-status' },
        { xtype: 'app-ip-reputation-reputation' },
        { xtype: 'app-ip-reputation-pass' }
    ],

    statics: {
        threatLevels: Ext.create('Ext.data.ArrayStore', {
            fields: [ 'color', 'rangeBegin', 'rangeEnd', 'description', 'details' ],
            sorters: [{
                property: 'rangeBegin',
                direction: 'ASC'
            }],
            data: [
            ]
        }),

        threaLevelsRenderer: function(value, meta, record){
            return Ext.String.format("{0} ({1})".t(), record.get('description'), record.get('details'));
        },

        threats: Ext.create('Ext.data.ArrayStore', {
            fields: [ 'bit', 'description', 'details' ],
            sorters: [{
                property: 'bit',
                direction: 'ASC'
            }],
            data: [
            ]
        }),

        threatsRenderer: function(value, meta, record){
            return record.get('description');
        },

        actions: Ext.create('Ext.data.ArrayStore', {
            fields: [ 'value', 'description'],
            sorters: [{
                property: 'value',
                direction: 'ASC'
            }],
            data: [
                ["block", 'Block'.t()],
                ["pass", 'Pass'.t()]
            ]
        }),
    }

});
