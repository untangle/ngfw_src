Ext.define('Ung.apps.openvpn.view.Client', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-openvpn-client',
    itemId: 'client',
    title: 'Client'.t(),
    viewModel: true,

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
        padding: '20 20 20 20',
        width: 800,
        height: 400
    },{
        xtype: 'displayfield',
        padding: '20 0 0 10',
        value: '<STRONG>' + 'Configure a new Remote Server connection'.t() + '</STRONG>'
    },{
        fieldLabel: 'Configuration File'.t(),
        labelWidth: 160,
        width: 400,
        xtype: 'textfield'
    },{
        xtype: 'button',
        align: 'center',
        width: 100,
        text: 'Submit'.t()
    }]
});

Ext.define('Ung.apps.openvpn.view.ClientTabs', {
    extend: 'Ext.tab.Panel',
    alias: 'widget.app-openvpn-client-tab-panel',
    itemId: 'client-tab-panel',
    viewModel: true,
    width: 800,
    height: 400,

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
    itemId: 'remote-servers-grid',

    dockedItems: [{
        xtype: 'toolbar',
        dock: 'top',
        html: 'Remote Servers'.t(),
    }],

    recordActions: ['edit', 'delete'],
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
    }],

    editorFields: [{
        xtype: 'checkbox',
        bind: '{record.enabled}',
        fieldLabel: 'Enabled'.t()
    }, {
        xtype: 'textfield',
        bind: '{record.name}',
        fieldLabel: 'Server Name'.t()
    }]

});
