Ext.define('Ung.apps.virusblocker.view.Advanced', {
    extend: 'Ext.tab.Panel',
    alias: 'widget.app-virus-blocker-advanced',
    itemId: 'advanced',
    title: 'Advanced'.t(),

    dockedItems: [{
        xtype: 'toolbar',
        dock: 'top',
        padding: 5,
        layout: {
            type: 'vbox',
            align: 'stretch'
        },
        items: [{
            xtype: 'checkbox',
            boxLabel: 'Enable ScoutIQ&trade; Cloud Scan'.t(),
            bind: '{settings.enableCloudScan}'
        }, {
            xtype: 'checkbox',
            boxLabel: 'Enable BitDefender&reg; Scan'.t(),
            bind: '{settings.enableLocalScan}'
        }]
    }],

    defaults: {
        xtype: 'ungrid',
        sortableColumns: true,
        tbar: ['@add', '->', '@import', '@export'],
        recordActions: ['edit', 'delete'],
    },

    items: [{
        title: 'File Extensions'.t(),

        listProperty: 'settings.httpFileExtensions.list',
        emptyRow: {
            string: '',
            enabled: true,
            description: '',
            javaClass: 'com.untangle.uvm.node.GenericRule'
        },

        bind: '{fileExtensions}',

        columns: [{
            header: 'File Type'.t(),
            width: 100,
            dataIndex: 'string',
            editor: {
                xtype: 'textfield',
                emptyText: '[enter file type]'.t(),
                allowBlank: false
            }
        }, {
            xtype: 'checkcolumn',
            width: 55,
            header: 'Scan'.t(),
            dataIndex: 'enabled',
            resizable: false
        }, {
            header: 'Description'.t(),
            width: 200,
            flex: 1,
            dataIndex: 'description',
            editor: {
                xtype: 'textfield',
                emptyText: '[no description]'.t()
            }
        }],
        editorFields: [{
            xtype: 'textfield',
            bind: '{record.string}',
            fieldLabel: 'File Type'.t(),
            emptyText: '[enter file type]'.t(),
            allowBlank: false,
            width: 400
        }, {
            xtype: 'checkbox',
            bind: '{record.enabled}',
            fieldLabel: 'Pass'.t()
        }, {
            xtype: 'textarea',
            bind: '{record.description}',
            fieldLabel: 'Description'.t(),
            emptyText: '[no description]'.t(),
            width: 400,
            height: 60
        }]
    }, {
        title: 'MIME Types'.t(),

        listProperty: 'settings.httpMimeTypes.list',
        emptyRow: {
            string: '',
            enabled: true,
            description: '',
            javaClass: 'com.untangle.uvm.node.GenericRule'
        },

        bind: '{mimeTypes}',

        columns: [{
            header: 'MIME Type'.t(),
            width: 100,
            dataIndex: 'string',
            editor: {
                xtype: 'textfield',
                emptyText: '[enter MIME type]'.t(),
                allowBlank: false
            }
        }, {
            xtype: 'checkcolumn',
            width: 55,
            header: 'Scan'.t(),
            dataIndex: 'enabled',
            resizable: false
        }, {
            header: 'Description'.t(),
            width: 200,
            flex: 1,
            dataIndex: 'description',
            editor: {
                xtype: 'textfield',
                emptyText: '[no description]'.t()
            }
        }],
        editorFields: [{
            xtype: 'textfield',
            bind: '{record.string}',
            fieldLabel: 'MIME Type'.t(),
            emptyText: '[enter MIME type]'.t(),
            allowBlank: false,
            width: 400
        }, {
            xtype: 'checkbox',
            bind: '{record.enabled}',
            fieldLabel: 'Pass'.t()
        }, {
            xtype: 'textarea',
            bind: '{record.description}',
            fieldLabel: 'Description'.t(),
            emptyText: '[no description]'.t(),
            width: 400,
            height: 60
        }]

    }]
});
