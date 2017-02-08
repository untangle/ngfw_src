Ext.define('Ung.config.network.DnsServer', {
    extend: 'Ext.panel.Panel',
    xtype: 'ung.config.network.dnsserver',

    viewModel: true,

    title: 'DNS Server'.t(),

    layout: 'border',

    items: [{
        xtype: 'rules',
        region: 'center',

        title: 'Static DNS Entries'.t(),

        columnFeatures: ['delete'], // which columns to add
        recordActions: ['@delete'],

        dataProperty: 'staticRoutes',

        // },

        bind: {
            store: {
                data: '{settings.dnsSettings.staticEntries.list}'
            }
        },

        columns: [{
            header: 'Name'.t(),
            dataIndex: 'name',
            flex: 1,
            editor: {
                xtype: 'textfield',
                allowBlank: false,
                emptyText: '[enter name]'.t()
            }
        }, {
            header: 'Address'.t(),
            width: 200,
            dataIndex: 'address',
            editor: {
                xtype: 'textfield',
                emptyText: '[enter address]'.t(),
                allowBlank: false,
                vtype: 'ipall',
            }
        }],
    }, {
        xtype: 'rules',
        region: 'south',

        height: '50%',
        split: true,



        title: 'Domain DNS Servers'.t(),

        columnFeatures: ['delete'], // which columns to add
        recordActions: ['@delete'],

        dataProperty: 'staticRoutes',

        // },

        bind: {
            store: {
                data: '{settings.dnsSettings.localServers.list}'
            }
        },

        columns: [{
            header: 'Domain'.t(),
            dataIndex: 'domain',
            flex: 1,
            editor: {
                xtype: 'textfield',
                allowBlank: false,
                emptyText: '[enter domain]'.t()
            }
        }, {
            header: 'Server'.t(),
            width: 200,
            dataIndex: 'localServer',
            editor: {
                xtype: 'textfield',
                emptyText: '[enter DNS server]'.t(),
                allowBlank: false,
                vtype: 'ipall',
            }
        }],
    }]
});