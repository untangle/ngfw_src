Ext.define('Ung.config.network.NetworkTest', {
    extend: 'Ext.panel.Panel',
    xtype: 'ung.config.networktest',
    alias: 'widget.networktest',

    tbar: [{
        xtype: 'displayfield',
        padding: '0 10',
        bind: '{description}'
    }],

    layout: 'fit',

    config: {
        command: null
    },

    actions: {
        runTest: {
            text: 'Run Test'.t(),
            handler: 'runTest'
        }
    },

    items: [{
        xtype: 'panel',
        tbar: ['@runTest'],
        layout: 'fit',
        items: [{
            xtype: 'textarea',
            border: false,
            bind: {
                emptyText: '{emptyText}'
            },
            fieldStyle: {
                fontFamily: 'Courier, monospace',
                fontSize: '14px',
                background: '#1b1e26',
                color: 'lime'
            },
            // margin: 10,
            readOnly: true
        }]
    }]

    // initComponent: function () {
    //     this.callParent(arguments);
    // }
});