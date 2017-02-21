Ext.define('Ung.config.network.view.DhcpServer', {
    extend: 'Ext.panel.Panel',

    alias: 'widget.config.network.dhcpserver',
    viewModel: true,

    title: 'DHCP Server'.t(),

    layout: 'border',

    items: [{
        xtype: 'ungrid',
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
            Fields.macAddress,
            Fields.ipAddress,
            Fields.description
        ]
    }, {
        xtype: 'grid',
        itemId: 'dhcpLeases',
        title: 'Current DHCP Leases'.t(),
        region: 'south',
        height: '50%',
        split: true,

        viewConfig: {
            emptyText: '<p style="text-align: center; margin: 0; line-height: 2;"><i class="fa fa-exclamation-triangle fa-2x"></i> <br/>No Data!</p>',
        },

        tbar: [{
            text: 'Refresh'.t(),
            iconCls: 'fa fa-refresh',
            handler: 'refreshDhcpLeases'
        }],

        store: {
            data: [] // todo: handle this store when available data
        },

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
