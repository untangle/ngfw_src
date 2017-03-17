Ext.define('Ung.apps.ipsecvpn.view.GreNetworks', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-ipsec-vpn-grenetworks',
    itemId: 'grenetworks',
    title: 'GRE Networks'.t(),
    viewModel: true,

    tbar: [{
        xtype: 'tbtext',
        padding: '8 5',
        style: { fontSize: '12px', fontWeight: 600 },
        html: 'The GRE Networks tab contains configuration options for connecting this server to other servers and networks using the GRE protocol.'.t()
    }],

    items: [{
        xtype: 'textfield',
        bind: '{settings.virtualNetworkPool}',
        vtype: 'cidrBlock',
        allowBlank: false,
        labelWidth: 200,
        padding: '10 0 0 10',
        fieldLabel: 'GRE Address Pool'.t()
    }, {
        xtype: 'displayfield',
        padding: '10 0 0 10',
        value: 'Each Remote Network will have a corresponding GRE interface created on this server, with each interface being assigned an IP address from this pool.'.t()
    }, {
        xtype: 'ungrid',
        region: 'center',
        padding: '10 20 10 20',
        tbar: ['@addInline'],
        recordActions: ['delete'],
        listProperty: 'settings.networks.list',
        emptyRow: {
            javaClass: 'com.untangle.node.ipsec_vpn.IpsecVpnNetwork',
            active: true,
            localAddress: '0.0.0.0',
            remoteAddress: '0.0.0.0',
            remoteNetworks: '0.0.0.0/24',
            description: ''
        },

        bind: '{networkList}',

        columns: [{
            header: 'Enabled'.t(),
            dataIndex: 'active',
            xtype: 'checkcolumn',
            width: 80
        },{
            header: 'Local IP'.t(),
            dataIndex: 'localAddress',
            width: 200,
            editor: {
                bind: '{record.localAddress}',
                xtype: 'textfield',
                vtype: 'ipAddress'
            }
        },{
            header: 'Remote Host'.t(),
            dataIndex: 'remoteAddress',
            width: 200,
            editor: {
                bind: '{record.remoteAddress}',
                xtype: 'textfield',
                vtype: 'ipAddress'
            }
        },{
            header: 'Remote Networks'.t(),
            dataIndex: 'remoteNetworks',
            width: 300,
            editor: {
                bind: '{record.remoteNetworks}',
                xtype: 'textfield',
                vtype: 'cidrBlock'
            }
        },{
            header: 'Description'.t(),
            dataIndex: 'description',
            width: 200,
            flex: 1,
            editor: {
                bind: '{record.description}',
                xtype: 'textfield',
            }
        }],
    }]
});
