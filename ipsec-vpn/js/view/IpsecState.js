Ext.define('Ung.apps.ipsecvpn.view.IpsecState', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-ipsec-vpn-ipsecstate',
    itemId: 'ipsec-state',
    title: 'IPsec State'.t(),
    layout: 'fit',
    scrollable: true,

    tbar: [{
        xtype: 'button',
        iconCls: 'fa fa-refresh',
        text: 'Refresh',
        target: 'ipsecStateInfo',
        handler: 'refreshIpsecStateInfo'
    }],

    items: [{
        xtype: 'textarea',
        itemId: 'ipsecStateInfo',
        spellcheck: false,
        padding: 0,
        border: false,
        bind: '{ipsecStateInfo}',
        fieldStyle: {
            'fontFamily'   : 'courier new',
            'fontSize'     : '12px'
        }
    }]
});
