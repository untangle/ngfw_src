Ext.define('Ung.config.network.view.DnsServer', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config-network-dns-server',
    itemId: 'dns-server',
    scrollable: true,

    viewModel: true,

    title: 'DNS Server'.t(),

    layout: 'border',

    items: [{
        xtype: 'ungrid',
        region: 'center',

        title: 'Static DNS Entries'.t(),

        tbar: ['@addInline', '->', '@import', '@export'],
        recordActions: ['delete'],

        emptyText: 'No Static DNS Entries defined'.t(),

        listProperty: 'settings.dnsSettings.staticEntries.list',

        emptyRow: {
            name: '',
            address: '1.2.3.4',
            javaClass: 'com.untangle.uvm.network.DnsStaticEntry'
        },

        bind: '{staticDnsEntries}',

        columns: [{
            header: 'Name'.t(),
            dataIndex: 'name',
            width: Renderer.hostnameWidth,
            flex: 1,
            editor: {
                xtype: 'textfield',
                allowBlank: false,
                bind: '{record.name}',
                maskRe: /[a-zA-Z0-9\-_.]/,
                emptyText: '[enter name]'.t()
            }
        }, {
            header: 'Address'.t(),
            width: Renderer.ipWidth,
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

        tbar: ['@addInline', '->', '@import', '@export'],
        recordActions: ['delete'],

        emptyText: 'No Domain DNS Servers defined'.t(),

        listProperty: 'settings.dnsSettings.localServers.list',

        emptyRow: {
            domain: '',
            localServer: '1.2.3.4',
            javaClass: 'com.untangle.uvm.network.DnsLocalServer'
        },

        bind: '{localServers}',

        columns: [{
            header: 'Domain'.t(),
            dataIndex: 'domain',
            width: Renderer.hostnameWidth,
            flex: 1,
            editor: {
                xtype: 'textfield',
                allowBlank: false,
                emptyText: '[enter domain]'.t(),
                maskRe: /[a-zA-Z0-9\-_.]/,
                bind: '{record.domain}',
            }
        }, {
            header: 'Server'.t(),
            width: Renderer.ipWidth,
            dataIndex: 'localServer',
            editor: {
                xtype: 'textfield',
                emptyText: '[enter DNS server]'.t(),
                allowBlank: false,
                bind: '{record.localServer}',
                vtype: 'ipAddress',
            }
        }],

        // responsive plugin is added inside ungrid
        responsiveConfig: {
            wide: {
                region: 'east',
                width: '50%'
            },
            tall: {
                region: 'south',
                height: '50%'
            }
        }
    }]
});
