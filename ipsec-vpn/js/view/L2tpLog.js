Ext.define('Ung.apps.ipsecvpn.view.L2tpLog', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-ipsec-vpn-l2tplog',
    itemId: 'l2tp-log',
    title: 'L2TP Log'.t(),
    layout: 'fit',
    scrollable: true,

    tbar: [{
        xtype: 'button',
        iconCls: 'fa fa-refresh',
        text: 'Refresh',
        target: 'ipsecVirtualLog',
        handler: 'refreshIpsecVirtualLog'
    }],

    items: [{
        xtype: 'textarea',
        itemId: 'ipsecVirtualLog',
        spellcheck: false,
        padding: 0,
        border: false,
        bind: '{ipsecVirtualLog}',
        fieldStyle: {
            'fontFamily'   : 'courier new',
            'fontSize'     : '12px'
        }
    }]
});
