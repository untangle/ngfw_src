Ext.define('Ung.apps.openvpn.view.Server', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-openvpn-server',
    itemId: 'server',
    title: 'Server'.t(),
    viewModel: true,

    tbar: [{
        xtype: 'tbtext',
        padding: '8 5',
        style: { fontSize: '12px', fontWeight: 600 },
        html: 'The Server tab is used to configure OpenVPN to operate as a server for remote clients'.t()
    }],

    defaults: {
        labelWidth: 180,
        padding: '0 0 0 10'
    },

    items: [{
        fieldLabel: 'Site Name'.t(),
        xtype: 'textfield',
        bind: '{settings.siteName}',
        allowBlank: false,
        padding: '10 0 0 10'
    },{
        fieldLabel: 'Site URL'.t(),
        xtype: 'displayfield',
        bind: '{getSiteUrl}'
    },{
        fieldLabel: 'Server Enabled'.t(),
        xtype: 'checkbox',
        bind: '{settings.serverEnabled}'
    },{
        fieldLabel: 'Address Space'.t(),
        xtype: 'textfield',
        vtype: 'cidrBlock',
        bind: '{settings.addressSpace}'
    },{
        fieldLabel: 'NAT OpenVPN Traffic'.t(),
        xtype: 'checkbox',
        bind: '{settings.natOpenVpnTraffic}'
    }]
});
