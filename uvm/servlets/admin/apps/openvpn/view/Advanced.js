Ext.define('Ung.apps.openvpn.view.Advanced', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-openvpn-advanced',
    itemId: 'advanced',
    title: 'Advanced'.t(),
    viewModel: true,

    tbar: [{
        xtype: 'tbtext',
        padding: '8 5',
        style: { fontSize: '12px', fontWeight: 600 },
        html: 'The Advanced tab is used to configure advanced OpenVPN options.'.t()
    }],

    defaults: {
        labelWidth: 180,
        padding: '0 0 0 10'
    },

    items: [{
        xtype: 'combo',
        fieldLabel: 'Protocol'.t(),
        bind: '{settings.protocol}',
        store: [['udp','UDP'],['tcp','TCP']],
        editable: false,
        padding: '10 0 0 10'
    },{
        fieldLabel: 'Port'.t(),
        xtype: 'textfield',
        bind: '{settings.port}'
    },{
        fieldLabel: 'Cipher'.t(),
        xtype: 'textfield',
        bind: '{settings.cipher}',
    },{
        fieldLabel: 'Client To Client Allowed'.t(),
        xtype: 'checkbox',
        bind: '{settings.clientToClient}'
    }]

});
