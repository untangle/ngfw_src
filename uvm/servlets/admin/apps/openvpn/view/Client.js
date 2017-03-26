Ext.define('Ung.apps.openvpn.view.Client', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-openvpn-client',
    itemId: 'openvpn-client',
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
        padding: '20 20 0 20',
        width: 800,
        height: 400
    },{
        xtype: 'app-openvpn-client-submit-form',
        padding: '20 20 0 20',
        border: false
    }]
});

Ext.define('Ung.apps.openvpn.view.ClientTabs', {
    extend: 'Ext.tab.Panel',
    alias: 'widget.app-openvpn-client-tab-panel',
    itemId: 'openvpn-client-tab-panel',
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

Ext.define('Ung.apps.openvpn.view.ClientSubmitForm', {
    extend: 'Ext.form.Panel',
    alias: 'widget.app-openvpn-client-submit-form',
    itemId: 'openvpn-client-submit-form',
    flex: 0,
    margin: '5 0 0 0',
    border: false,
    viewModel: true,

    defaults: {
        padding: '10 0 0 10'
    },

    items: [{
        xtype: 'fieldset',
        title: 'Configure a new Remote Server connection'.t(),
        border: false,
        items: [{
            xtype: 'filefield',
            name: 'uploadConfigFileName',
            fieldLabel: 'Configuration File'.t(),
            allowBlank: false,
            validateOnBlur: false,
            labelWidth: 150,
            width: 600
        }, {
            xtype: 'button',
            id: 'submitUpload',
            text: 'Submit'.t(),
            name: "Submit",
            handler: function(btn) {
                var form = Ext.ComponentQuery.query('#openvpn-client-submit-form')[0];
                var filename = Ext.ComponentQuery.query('textfield[name=uploadConfigFileName]')[0].value;
                if ( filename == null || filename.length === 0 ) {
                    Ext.MessageBox.alert('Select File'.t(), 'Please choose a file to upload.'.t());
                    return;
                }
                form.submit({
                    url: "/openvpn/uploadConfig",
                    success: Ext.bind(function( form, action, handler ) {
                        Ext.MessageBox.alert('Success'.t(), 'The configuration has been imported.'.t());
                        var grid = Ext.ComponentQuery.query('#openvpn-remote-servers-grid')[0];
                        grid.store.reload();
                        }, this),
                    failure: Ext.bind(function( form, action ) {
                        Ext.MessageBox.alert('Failure'.t(), 'Import failure'.t() + ": " + action.result.code);
                    }, this)
                });
            }
        }]
    }]

});
