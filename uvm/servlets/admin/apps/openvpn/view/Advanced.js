Ext.define('Ung.apps.openvpn.view.Advanced', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-openvpn-advanced',
    itemId: 'advanced',
    title: 'Advanced'.t(),
    viewModel: true,

    tbar: [{
        xtype: 'tbtext',
        padding: '8 5',
        style: { fontSize: '12px', fontWeight: 600 },
        html: 'The Advanced tab is used to configure advanced OpenVPN options.'.t()
    }],

    defaults: {
        labelWidth: 180,
        padding: '0 0 0 10'
    },

    items: [{
        xtype: 'combo',
        fieldLabel: 'Protocol'.t(),
        bind: '{settings.protocol}',
        store: [['udp','UDP'],['tcp','TCP']],
        editable: false,
        padding: '10 0 0 10'
    },{
        fieldLabel: 'Port'.t(),
        xtype: 'textfield',
        bind: '{settings.port}'
    },{
        fieldLabel: 'Cipher'.t(),
        xtype: 'textfield',
        bind: '{settings.cipher}',
    },{
        fieldLabel: 'Client To Client Allowed'.t(),
        xtype: 'checkbox',
        bind: '{settings.clientToClient}'
    },{
        title: 'Server Configuration'.t(),
        xtype: 'app-openvpn-config-editor-grid',
        padding: '20 20 20 20',
        width: 800,
        height: 300,
        listProperty: 'settings.serverConfiguration.list',
        itemId: 'server-config-editor-grid',
        bind: '{serverConfiguration}'
    },{
        title: 'Client Configuration'.t(),
        xtype: 'app-openvpn-config-editor-grid',
        padding: '20 20 20 20',
        width: 800,
        height: 300,
        listProperty: 'settings.clientConfiguration.list',
        itemId: 'client-config-editor-grid',
        bind: '{clientConfiguration}'
    }]
});

Ext.define('Ung.apps.openvpn.view.ConfigEditorGrid', {
    extend: 'Ung.cmp.Grid',
    alias: 'widget.app-openvpn-config-editor-grid',

    dockedItems: [{
        xtype: 'toolbar',
        dock: 'top',
        items: ['@addInline']
    }],

    recordActions: ['delete'],

    emptyRow: {
        javaClass: 'com.untangle.app.openvpn.OpenVpnConfigItem',
        'optionName': null,
        'optionValue': null,
        'excludeFlag': false,
        'readOnly': false,
    },

    listeners: {
        cellclick: function(grid,td,cellIndex,record,tr,rowIndex,e,eOpts)  {
            // not allowed to edit default options
            if (record.data.readOnly) { return(false); }
        }
    },

    columns: [{
        header: 'Option Name'.t(),
        width: 250,
        dataIndex: 'optionName',
        editable: false,
        editor: {
            xtype: 'textfield',
            bind: '{record.optionName}',
        },
    }, {
        header: 'Option Value'.t(),
        width: 250,
        dataIndex: 'optionValue',
        editor: {
            xtype: 'textfield',
            bind: '{record.optionValue}'
        }
    }, {
        header: 'Option Type'.t(),
        width: 80,
        dataIndex: 'readOnly',
        resizable: false,
        renderer: function(val) {
            return(val ? 'default' : 'custom');
        }
    }, {
        xtype: 'checkcolumn',
        header: 'Exclude'.t(),
        width: 80,
        dataIndex: 'excludeFlag',
        resizable: false,
    }]

});
