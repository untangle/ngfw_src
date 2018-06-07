Ext.define('Ung.apps.intrusionprevention.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-intrusion-prevention',
    controller: 'app-intrusion-prevention',

    viewModel: {
        data:{
            settings: {}
        },
        stores: {
            signatures: {
                storeId: 'signaturesStore',
                fields: [{
                    name: 'sid',
                    sortType: 'asInt'
                },{
                    name: 'classtype'
                },{
                    name: 'category'
                },{
                    name: 'msg'
                },{
                    name: 'signature'
                },{
                    name: 'path'
                },{
                    name: 'log',
                    type: 'boolean'
                },{
                    name: 'block',
                    type: 'boolean'
                }],
                data: '{settings.signatures.list}',
                groupField: 'classtype',
                sorters: [{
                    property: 'sid',
                    direction: 'ASC'
                }],
                listeners:{
                    datachanged: 'storeDataChanged'
                }
            },
            variables: {
                storeId: 'variablesStore',
                fields: [{
                    name: 'variable',
                },{
                    name: 'definition'
                },{
                    name: 'description'
                }],
                data: '{settings.variables.list}',
                sorters: [{
                    property: 'variable',
                    direction: 'ASC'
                }],
                listeners:{
                    datachanged: 'storeDataChanged'
                }
            }
        }
    },

    items: [
        { xtype: 'app-intrusion-prevention-status' },
        { xtype: 'app-intrusion-prevention-signatures' },
        { xtype: 'app-intrusion-prevention-variables' }
    ]

});
