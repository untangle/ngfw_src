Ext.define('Ung.config.email.view.SafeList', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config.email.safelist',
    itemId: 'safelist',

    viewModel: true,
    title: 'Safe List'.t(),

    layout: 'border',

    items: [{
        xtype: 'ungrid',
        region: 'center',

        title: 'Global Safe List'.t(),

        tbar: ['@add'],
        recordActions: ['@delete'],

        // listProperty: 'settings.dnsSettings.staticEntries.list',

        emptyRow: {
            emailAddress: 'email@' + rpc.hostname + '.com',
            // javaClass: 'com.untangle.uvm.network.DnsStaticEntry'
        },

        bind: '{globalSL}',

        columns: [{
            header: 'Email Address'.t(),
            dataIndex: 'emailAddress',
            flex: 1,
            editor: {
                xtype: 'textfield',
                allowBlank: false,
                bind: '{record.emailAddress}',
                emptyText: '[enter email]'.t(),
                vtype: 'email'
            }
        }]
    }, {
        xtype: 'grid',
        region: 'south',

        height: '50%',
        split: true,

        title: 'Per User Safe Lists'.t(),

        // tbar: ['@add'],

        // bind: '{localServers}',

        columns: [{
            header: 'Account Address'.t(),
            dataIndex: 'emailAddress',
            flex: 1
        }, {
            header: 'Safe List Size'.t(),
            width: 150,
            dataIndex: 'count',
            align: 'right'
        }],
    }]

});