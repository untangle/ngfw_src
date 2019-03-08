Ext.define('Ung.config.network.view.DhcpServer', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config-network-dhcp-server',
    itemId: 'dhcp-server',
    scrollable: true,

    viewModel: true,

    title: 'DHCP Server'.t(),

    layout: 'border',

    items: [{
        xtype: 'ungrid',
        region: 'center',
        itemId: 'dhcpEntries',
        title: 'Static DHCP Entries'.t(),

        tbar: ['@add', '->', '@import', '@export'],
        recordActions: ['delete'],

        emptyText: 'No Static DHCP Entries defined'.t(),

        listProperty: 'settings.staticDhcpEntries.list',

        emptyRow: {
            macAddress: '11:22:33:44:55:66',
            address: '1.2.3.4',
            javaClass: 'com.untangle.uvm.network.DhcpStaticEntry',
            description: '[no description]'.t()
        },

        bind: '{staticDhcpEntries}',

        columns: [{
            header: 'MAC Address'.t(),
            dataIndex: 'macAddress',
            width: 200,
            editor: {
                xtype:'textfield',
                emptyText: '[enter MAC address]'.t(),
                allowBlank: false,
                vtype: 'macAddress',
                maskRe: /[a-fA-F0-9:]/
            }
        }, {
            header: 'Address'.t(),
            width: 200,
            dataIndex: 'address',
            editor: {
                xtype: 'textfield',
                emptyText: '[enter address]'.t(),
                allowBlank: false,
                vtype: 'ipAddress'
            }
        }, {
            header: 'Description'.t(),
            flex: 1,
            dataIndex: 'description',
            editor: {
                xtype: 'textfield',
                emptyText: '[enter description]'.t(),
                allowBlank: false
            }
        }],
        editorFields: [
            Field.macAddress,
            Field.ipAddress,
            Field.description
        ]
    }, {
        xtype: 'ungrid',
        itemId: 'dhcpLeases',
        title: 'Current DHCP Leases'.t(),
        region: 'south',
        split: true,
        enableColumnHide: false,
        enableColumnMove: false,

        emptyText: 'No Current DHCP Leases defined'.t(),

        tbar: [{
            text: 'Refresh'.t(),
            iconCls: 'fa fa-refresh',
            handler: 'externalAction',
            action: 'refreshDhcpLeases'
        }],

        bind: '{dynamicDhcpEntries}',

        columns: [{
            header: 'MAC Address'.t(),
            dataIndex:'macAddress',
            width: Renderer.macWidth
        },{
            header: 'Address'.t(),
            dataIndex: 'address',
            width: Renderer.ipWidth
        },{
            header: 'Hostname'.t(),
            dataIndex: 'hostname',
            width: Renderer.hostnameWidth,
            flex: 1
        },{
            header: 'Expiration Time'.t(),
            dataIndex: 'date',
            width: Renderer.timestampWidth,
            renderer: Renderer.timestamp
        }, {
            xtype: 'actioncolumn',
            width: Renderer.actionColumn,
            header: 'Add Static'.t(),
            align: 'center',
            iconCls: 'fa fa-plus',
            sortable: false,
            resizable: false,
            handler: 'externalAction',
            action: 'addStaticDhcpLease'
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
