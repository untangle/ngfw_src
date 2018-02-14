Ext.define('Ung.apps.openvpn.view.Client', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-openvpn-client',
    itemId: 'client',
    title: 'Client'.t(),
    viewModel: true,
    autoScroll: true,
    withValidation: true,

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

    emptyText: 'No Remote Servers defined'.t(),

    recordActions: ['edit','delete'],
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
        width: Renderer.booleanWidth,
        dataIndex: 'enabled',
        resizable: false
    }, {
        header: 'Server Name'.t(),
        width: Renderer.messageWidth,
        flex: 1,
        dataIndex: 'name',
        editor: {
            xtype: 'textfield',
            vtype: 'openvpnName',
            bind: '{record.name}'
        }
    }, {
        header: 'Username',
        width: 200,
        dataIndex: 'authUsername',
        renderer: function(value, meta, record) {
            if (record.data.authUserPass) return(value);
            return('[User/Pass Auth Disabled]'.t());
        }
    }],

    editorFields: [
        Field.enableRule(),
    {
        xtype: 'textfield',
        fieldLabel: 'Server Name'.t(),
        readOnly: true,
        name: 'serverName',
        bind: '{record.name}'
    }, {
        xtype: 'checkbox',
        fieldLabel: 'Username/Password Authentication',
        name: 'authUserPass',
        bind: '{record.authUserPass}'
    }, {
        xtype: 'textfield',
        fieldLabel: 'Username'.t(),
        allowBlank: false,
        bind: {
            value: '{record.authUsername}',
            disabled: '{record.authUserPass == false}',
            hidden: '{record.authUserPass == false}'
        },
    }, {
        xtype: 'textfield',
        fieldLabel: 'Password'.t(),
        allowBlank: false,
        inputType: 'password',
        bind: {
            value: '{record.authPassword}',
            disabled: '{record.authUserPass == false}',
            hidden: '{record.authUserPass == false}'
        },
    }]
});
