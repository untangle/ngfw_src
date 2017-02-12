Ext.define('Ung.config.network.view.DhcpServer', {
    extend: 'Ext.panel.Panel',

    alias: 'widget.config.network.dhcpserver',
    viewModel: true,

    title: 'DHCP Server'.t(),

    layout: 'border',

    items: [{
        xtype: 'rules',
        region: 'center',

        title: 'Static DHCP Entries'.t(),

        tbar: ['@add'],
        recordActions: ['@delete'],

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
                xtype: 'textfield',
                fieldLabel: 'MAC Address'.t(),
                allowBlank: false,
                bind: '{record.macAddress}',
                emptyText: '[enter MAC name]'.t(),
                maskRe: /[a-fA-F0-9:]/
            }
        }, {
            header: 'Address'.t(),
            width: 200,
            dataIndex: 'address',
            editor: {
                xtype: 'textfield',
                fieldLabel: 'Address'.t(),
                emptyText: '[enter address]'.t(),
                bind: '{record.address}',
                allowBlank: false,
                vtype: 'ipall',
            }
        }, {
            header: 'Description'.t(),
            flex: 1,
            dataIndex: 'description',
            editor: {
                xtype: 'textfield',
                fieldLabel: 'Description'.t(),
                bind: '{record.description}',
                emptyText: '[enter description]'.t(),
                allowBlank: false,
            }
        }],
    }, {
        xtype: 'grid',
        title: 'Current DHCP Leases'.t(),
        region: 'south',

        height: '50%',
        split: true,

        tbar: [{
            text: 'Refresh'.t(),
            iconCls: 'fa fa-refresh',
            // handler: 'refreshDhcpLeases'
        }],

        columns: [{
            header: 'MAC Address'.t(),
            dataIndex:'macAddress',
            width: 150
        },{
            header: 'Address'.t(),
            dataIndex:'address',
            width: 200
        },{
            header: 'Hostname'.t(),
            dataIndex:'hostname',
            width: 200
        },{
            header: 'Expiration Time'.t(),
            dataIndex:'date',
            width: 180,
            // renderer: function(value) { return i18n.timestampFormat(value*1000); }
        }, {
            xtype: 'actioncolumn',
            header: 'Add Static'.t(),
            iconCls: 'fa fa-plus',
            handler: function () {
                alert('to add');
            }
        }]

    }]
});