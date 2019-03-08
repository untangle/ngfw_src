Ext.define('Ung.apps.ipsecvpn.view.Log', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-tunnel-vpn-log',
    itemId: 'log',
    title: 'Log'.t(),
    layout: 'fit',
    scrollable: true,

    tbar: [{
        xtype: 'button',
        iconCls: 'fa fa-refresh',
        text: 'Refresh',
        target: 'tunnelLog',
        handler: 'getTunnelLog'
    }],

    items: [{
        xtype: 'textarea',
        itemId: 'tunnelLog',
        spellcheck: false,
        padding: 0,
        border: false,
        bind: '{tunnelLog}',
        fieldStyle: {
            'fontFamily'   : 'courier new',
            'fontSize'     : '12px'
        }
    }]
});
