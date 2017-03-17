Ext.define('Ung.apps.ipsecvpn.view.IpsecPolicy', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-ipsec-vpn-ipsecpolicy',
    itemId: 'ipsecpolicy',
    title: 'IPsec Policy'.t(),

    items: [{
        xtype: 'component',
        padding: 0,
        border: false,
        tpl: '<textarea style="width: 100%; height: 100%; border: 0; resize: none;" readonly>{log}</textarea>',
        data: 'This needs to call and display the data from the app function getPolicyInfo'
    }]
});
