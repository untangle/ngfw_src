Ext.define('Ung.apps.openvpn.view.Advanced', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-openvpn-advanced',
    itemId: 'advanced',
    title: 'Advanced'.t(),
    viewModel: true,
    withValidation: true,
    bodyPadding: 10,
    scrollable: true,

    tbar: [{
        xtype: 'tbtext',
        padding: '8 5',
        style: { fontSize: '12px', fontWeight: 600 },
        html: '<i class="fa fa-exclamation-triangle" style="color: red;"></i> ' +
              'Advanced settings require careful configuration.<br>' +
              '<i class="fa fa-exclamation-triangle" style="color: red;"></i> ' +
              'Misconfiguration can compromise the proper operation and security of your server.<br>' +
              '<i class="fa fa-exclamation-triangle" style="color: red;"></i> ' +
              'Changes made on this tab are not officially supported.<br>'.t()
    }],

    items: [{
        xtype: 'container',
        layout: 'column',
        items: [{
            xtype: 'combo',
            fieldLabel: 'Protocol'.t(),
            labelWidth: 180,
            bind: '{settings.protocol}',
            store: [['udp','UDP'],['tcp','TCP']],
            editable: false,
        }, {
            xtype: 'displayfield',
            margin: '0 0 0 10',
            value: '(default = UDP)'.t()
        }]
    },{
        xtype: 'container',
        layout: 'column',
        items: [{
            xtype: 'textfield',
            fieldLabel: 'Port'.t(),
            fieldIndex: 'listenPort',
            labelWidth: 180,
            bind: '{settings.port}'
        }, {
            xtype: 'displayfield',
            margin: '0 0 0 10',
            value: '(default = 1194)'.t()
        }]
    },{
        xtype: 'container',
        layout: 'column',
        items: [{
            xtype: 'textfield',
            fieldLabel: 'Cipher'.t(),
            labelWidth: 180,
            bind: '{settings.cipher}',
        }, {
            xtype: 'displayfield',
            margin: '0 0 0 10',
            value: '(default = AES-128-CBC)'.t()
        }]
    },{
        xtype: 'container',
        layout: 'column',
        items: [{
            xtype: 'checkbox',
            fieldLabel: 'Client To Client Allowed'.t(),
            labelWidth: 180,
            bind: '{settings.clientToClient}'
        }, {
            xtype: 'displayfield',
            margin: '0 0 0 10',
            value: '(default = checked)'.t()
        }]
    },{
        xtype: 'fieldset',
        title: 'Server Configuration'.t(),

        layout: {
            type: 'vbox',
            align: 'stretch'
        },

        items: [{
            xtype: 'app-openvpn-config-editor-grid',
            listProperty: 'settings.serverConfiguration.list',
            itemId: 'server-config-editor-grid',
            bind: '{serverConfiguration}'
        }]
    },{
        xtype: 'fieldset',
        title: 'Client Configuration'.t(),

        layout: {
            type: 'vbox',
            align: 'stretch'
        },

        items: [{
            xtype: 'app-openvpn-config-editor-grid',
            listProperty: 'settings.clientConfiguration.list',
            itemId: 'client-config-editor-grid',
            bind: '{clientConfiguration}'
        }]
    }]
});

Ext.define('Ung.apps.openvpn.view.ConfigEditorGrid', {
    extend: 'Ung.cmp.Grid',
    alias: 'widget.app-openvpn-config-editor-grid',

    dockedItems: [{
        xtype: 'toolbar',
        dock: 'top',
        items: ['@addInline', '->', '@import', '@export']
    }],

    recordActions: ['delete'],
    topInsert: true,

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
        width: Renderer.messageWidth,
        flex: 1,
        dataIndex: 'optionName',
        editable: false,
        editor: {
            xtype: 'textfield',
            bind: '{record.optionName}',
        },
    }, {
        header: 'Option Value'.t(),
        width: Renderer.messageWidth,
        flex: 1,
        dataIndex: 'optionValue',
        editor: {
            xtype: 'textfield',
            bind: '{record.optionValue}'
        }
    }, {
        header: 'Option Type'.t(),
        width: Renderer.actionWidth + 20,
        dataIndex: 'readOnly',
        resizable: false,
        renderer: function(val) {
            return(val ? 'default' : 'custom');
        }
    }, {
        xtype: 'checkcolumn',
        header: 'Exclude'.t(),
        width: Renderer.booleanWidth + 10,
        dataIndex: 'excludeFlag',
        resizable: false,
    }]

});
