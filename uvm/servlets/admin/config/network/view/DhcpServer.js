Ext.define('Ung.config.network.view.DhcpServer', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config-network-dhcpserver',
    itemId: 'dhcp-server',

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
        xtype: 'grid',
        itemId: 'dhcpLeases',
        title: 'Current DHCP Leases'.t(),
        region: 'south',
        height: '50%',
        split: true,
        enableColumnHide: false,
        enableColumnMove: false,

        viewConfig: {
            emptyText: '<p style="text-align: center; margin: 0; line-height: 2;"><i class="fa fa-exclamation-triangle fa-2x"></i> <br/>No Data!</p>',
        },

        tbar: [{
            text: 'Refresh'.t(),
            iconCls: 'fa fa-refresh',
            handler: 'refreshDhcpLeases'
        }],

        store: { data: [] },

        columns: [{
            header: 'MAC Address'.t(),
            dataIndex:'macAddress',
            width: 150
        },{
            header: 'Address'.t(),
            dataIndex: 'address',
            flex: 1
        },{
            header: 'Hostname'.t(),
            dataIndex: 'hostname',
            width: 200
        },{
            header: 'Expiration Time'.t(),
            dataIndex: 'date',
            width: 180,
            renderer: function(value) { return Util.timestampFormat(value*1000); }
        }, {
            xtype: 'actioncolumn',
            header: 'Add Static'.t(),
            align: 'center',
            iconCls: 'fa fa-plus',
            sortable: false,
            resizable: false,
            handler: 'addStaticDhcpLease'
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
