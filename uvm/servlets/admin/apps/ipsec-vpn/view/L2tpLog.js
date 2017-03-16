Ext.define('Ung.apps.ipsecvpn.view.L2tpLog', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-ipsec-vpn-l2tplog',
    itemId: 'l2tplog',
    title: 'L2TP Log'.t(),

    items: [{
        xtype: 'component',
        padding: 0,
        border: false,
        tpl: '<textarea style="width: 100%; height: 100%; border: 0; resize: none;" readonly>{log}</textarea>',
        data: 'This needs to call and display the data from the app function getVirtualLogFile'
    }]
});
