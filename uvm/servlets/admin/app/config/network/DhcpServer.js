Ext.define('Ung.config.network.DhcpServer', {
    extend: 'Ext.panel.Panel',
    xtype: 'ung.config.network.dhcpserver',

    viewModel: true,

    title: 'DHCP Server'.t(),

    layout: 'border',

    items: [{
        xtype: 'rules',
        region: 'center',

        title: 'Static DHCP Entries'.t(),

        columnFeatures: ['delete'], // which columns to add
        recordActions: ['@delete'],

        dataProperty: 'staticRoutes',

        // },

        bind: {
            store: {
                data: '{settings.staticDhcpEntries.list}'
            }
        },

        columns: [{
            header: 'MAC Address'.t(),
            dataIndex: 'macAddress',
            width: 200,
            editor: {
                xtype: 'textfield',
                allowBlank: false,
                emptyText: '[enter MAC name]'.t(),
                maskRe: /[a-fA-F0-9:]/
            }
        }, {
            header: 'Address'.t(),
            flex: 1,
            dataIndex: 'address',
            editor: {
                xtype: 'textfield',
                emptyText: '[enter address]'.t(),
                allowBlank: false,
                vtype: 'ipall',
            }
        }, {
            header: 'Description'.t(),
            width: 200,
            dataIndex: 'description',
            editor: {
                xtype: 'textfield',
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