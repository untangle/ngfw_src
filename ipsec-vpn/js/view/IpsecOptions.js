Ext.define('Ung.apps.ipsecvpn.view.IpsecOptions', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-ipsec-vpn-ipsecoptions',
    itemId: 'ipsec-options',
    title: 'IPsec Options'.t(),
    scrollable: true,

    bodyPadding: 10,

    tbar: [{
        xtype: 'tbtext',
        padding: '8 5',
        style: { fontSize: '12px', fontWeight: 600 },
        html: 'The IPsec Options tab contains common settings that apply to all IPsec traffic.'.t()
    }],

    items: [{
        xtype:'combobox',
        fieldLabel: "Unique ID's".t(),
        labelWidth: 180,
        editable: false,
        width: 400,
        bind: '{settings.uniqueIds}',
        store: [['yes','Yes'],['no','No'],['never','Never'],['keep','Keep']]
    }, {
        xtype: 'component',
        margin: '0 0 20 10',
        html: 'The recommended default value is Yes unless you have a specific reason to change this setting.'.t(),
    }, {
        xtype: 'checkbox',
        fieldLabel: 'Bypass all IPsec traffic'.t(),
        labelWidth: 180,
        bind: {
            value: '{settings.bypassflag}'
        },
    }, {
        xtype: 'component',
        margin: '0 0 20 10',
        html: 'When enable, IPsec traffic will bypass processing by all Apps and Service Apps'.t(),
    }]
});
