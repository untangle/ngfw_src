Ext.define('Ung.apps.openvpn.view.Client', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-openvpn-client',
    itemId: 'openvpn-client',
    title: 'Client'.t(),
    viewModel: true,
    autoScroll: true,

    tbar: [{
        xtype: 'tbtext',
        padding: '8 5',
        style: { fontSize: '12px', fontWeight: 600 },
        html: 'The Client tab is used to configure the servers that OpenVPN will connect as a client.'.t()
    }],

    defaults: {
        padding: '0 0 0 10'
    },

    items: [{
        xtype: 'app-openvpn-client-tab-panel',
        padding: '20 20 0 20',
    },{
        xtype: 'form',
        name: 'upload_form',
        border: false,
        margin: '10 10 10 10',
        items: [{
            xtype: 'fileuploadfield',
            name: 'uploadConfigFileName',
            anchor: '100%',
            buttonOnly: true,
            buttonConfig: {
                iconCls: 'fa fa-upload',
                width: 300,
                height: 40,
                text: 'Upload Remote Server Configuration File'.t(),
            },
            listeners: { 'change': 'uploadFile' }
        }]
    }]
});

Ext.define('Ung.apps.openvpn.view.ClientTabs', {
    extend: 'Ext.tab.Panel',
    alias: 'widget.app-openvpn-client-tab-panel',
    itemId: 'openvpn-client-tab-panel',
    viewModel: true,
    layout: 'fit',

    items: [{
        title: 'Remote Servers'.t(),
        items: [
            { xtype: 'app-openvpn-remote-servers-grid' }
            ]
    }]

});

Ext.define('Ung.apps.openvpn.view.RemoteServersGrid', {
    extend: 'Ung.cmp.Grid',
    alias: 'widget.app-openvpn-remote-servers-grid',
    itemId: 'openvpn-remote-servers-grid',

    recordActions: ['delete'],
    listProperty: 'settings.remoteServers.list',
    emptyRow: {
        javaClass: 'com.untangle.app.openvpn.OpenVpnRemoteServer',
        'enabled': true,
        'name': '',
        },

    bind: '{remoteServers}',

    columns: [{
        xtype: 'checkcolumn',
        header: 'Enabled'.t(),
        width: 80,
        dataIndex: 'enabled',
        resizable: false
    }, {
        header: 'Server Name'.t(),
        width: 150,
        flex: 1,
        dataIndex: 'name',
        editor: {
            xtype: 'textfield',
            bind: '{record.name}'
        }
    }],
});
