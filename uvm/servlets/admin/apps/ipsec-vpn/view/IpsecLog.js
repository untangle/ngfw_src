Ext.define('Ung.apps.ipsecvpn.view.IpsecLog', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-ipsec-vpn-ipseclog',
    itemId: 'ipseclog',
    title: 'IPsec Log'.t(),

    items: [{
        xtype: 'component',
        padding: 0,
        border: false,
        tpl: '<textarea style="width: 100%; height: 100%; border: 0; resize: none;" readonly>{log}</textarea>',
        data: 'This needs to call and display the data from the app function getLogFile'
    }]
});
