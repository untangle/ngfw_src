Ext.define('Ung.config.network.view.DnsServer', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config-network-dnsserver',
    itemId: 'dns_server',
    helpSource: 'network_dns_server',
    viewModel: true,

    title: 'DNS Server'.t(),

    layout: 'border',

    items: [{
        xtype: 'ungrid',
        region: 'center',

        title: 'Static DNS Entries'.t(),

        tbar: ['@addInline'],
        recordActions: ['delete'],

        listProperty: 'settings.dnsSettings.staticEntries.list',

        emptyRow: {
            name: '[no name]'.t(),
            address: '1.2.3.4',
            javaClass: 'com.untangle.uvm.network.DnsStaticEntry'
        },

        bind: '{staticDnsEntries}',

        columns: [{
            header: 'Name'.t(),
            dataIndex: 'name',
            flex: 1,
            editor: {
                xtype: 'textfield',
                allowBlank: false,
                bind: '{record.name}',
                emptyText: '[enter name]'.t()
            }
        }, {
            header: 'Address'.t(),
            width: 200,
            dataIndex: 'address',
            editor: {
                xtype: 'textfield',
                emptyText: '[enter address]'.t(),
                bind: '{record.address}',
                allowBlank: false,
                vtype: 'ipAddress',
            }
        }]
    }, {
        xtype: 'ungrid',
        region: 'south',

        height: '50%',
        split: true,

        title: 'Domain DNS Servers'.t(),

        tbar: ['@addInline'],
        recordActions: ['delete'],

        listProperty: 'settings.dnsSettings.localServers.list',

        emptyRow: {
            domain: '[no domain]'.t(),
            localServer: '1.2.3.4',
            javaClass: 'com.untangle.uvm.network.DnsLocalServer'
        },

        bind: '{localServers}',

        columns: [{
            header: 'Domain'.t(),
            dataIndex: 'domain',
            flex: 1,
            editor: {
                xtype: 'textfield',
                allowBlank: false,
                emptyText: '[enter domain]'.t(),
                bind: '{record.domain}',
            }
        }, {
            header: 'Server'.t(),
            width: 200,
            dataIndex: 'localServer',
            editor: {
                xtype: 'textfield',
                emptyText: '[enter DNS server]'.t(),
                allowBlank: false,
                bind: '{record.localServer}',
                vtype: 'ipAddress',
            }
        }],

        plugins: 'responsive',
        responsiveConfig: {
            wide: {
                region: 'east',
                width: '50%'
            },
            tall: {
                region: 'south',
                height: '50%',
            }
        }
    }]
});
