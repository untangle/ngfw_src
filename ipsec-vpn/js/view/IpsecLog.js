Ext.define('Ung.apps.ipsecvpn.view.IpsecLog', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-ipsec-vpn-ipseclog',
    itemId: 'ipsec-log',
    title: 'IPsec Log'.t(),
    layout: 'fit',

    tbar: [{
        xtype: 'button',
        iconCls: 'fa fa-refresh',
        text: 'Refresh',
        target: 'ipsecSystemLog',
        handler: 'refreshTextArea'
    }],

    items: [{
        xtype: 'textarea',
        itemId: 'ipsecSystemLog',
        spellcheck: false,
        padding: 0,
        border: false,
        bind: '{ipsecSystemLog}',
        fieldStyle: {
            'fontFamily'   : 'courier new',
            'fontSize'     : '12px'
        }
    }]
});
