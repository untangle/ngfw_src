Ext.define('Ung.apps.ipsecvpn.view.IpsecOptions', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-ipsec-vpn-ipsecoptions',
    itemId: 'ipsec_options',
    title: 'IPsec Options'.t(),

    bodyPadding: 10,

    tbar: [{
        xtype: 'tbtext',
        padding: '8 5',
        style: { fontSize: '12px', fontWeight: 600 },
        html: 'The IPsec Options tab contains common settings that apply to all IPsec traffic.'.t()
    }],

    items: [{
        xtype: 'checkbox',
        margin: '0 0 0 20',
        boxLabel: 'Bypass all IPsec traffic'.t(),
        bind: {
            value: '{settings.bypassflag}'
        },
    }]
});
