Ext.define('Ung.apps.ipsecvpn.view.IpsecPolicy', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-ipsec-vpn-ipsecpolicy',
    itemId: 'ipsec-policy',
    title: 'IPsec Policy'.t(),
    layout: 'fit',

    tbar: [{
        xtype: 'button',
        iconCls: 'fa fa-refresh',
        text: 'Refresh',
        target: 'ipsecPolicyInfo',
        handler: 'refreshTextArea'
    }],

    items: [{
        xtype: 'textarea',
        itemId: 'ipsecPolicyInfo',
        spellcheck: false,
        padding: 0,
        border: false,
        bind: '{ipsecPolicyInfo}',
        fieldStyle: {
            'fontFamily'   : 'courier new',
            'fontSize'     : '12px'
        }
    }]
});
