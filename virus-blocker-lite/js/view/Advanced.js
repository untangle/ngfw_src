Ext.define('Ung.apps.virusblockerlite.view.Advanced', {
    extend: 'Ext.tab.Panel',
    alias: 'widget.app-virus-blocker-lite-advanced',
    itemId: 'advanced',
    title: 'Advanced'.t(),
    scrollable: true,

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
        itemId: 'file_extenions',
        emptyText: 'No File Extensions defined'.t(),

        listProperty: 'settings.httpFileExtensions.list',
        emptyRow: {
            string: '',
            enabled: true,
            description: '',
            javaClass: 'com.untangle.uvm.app.GenericRule'
        },

        bind: '{fileExtensions}',

        columns: [{
            header: 'File Type'.t(),
            width: Renderer.idWidth,
            dataIndex: 'string',
            editor: {
                xtype: 'textfield',
                emptyText: '[enter file type]'.t(),
                allowBlank: false
            }
        }, {
            xtype: 'checkcolumn',
            width: Renderer.booleanWidth,
            header: 'Scan'.t(),
            dataIndex: 'enabled',
            resizable: false
        }, {
            header: 'Description'.t(),
            width: Renderer.messageWidth,
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
        itemId: 'mime_types',
        emptyText: 'No MIME Types defined'.t(),

        listProperty: 'settings.httpMimeTypes.list',
        emptyRow: {
            string: '',
            enabled: true,
            description: '',
            javaClass: 'com.untangle.uvm.app.GenericRule'
        },

        bind: '{mimeTypes}',

        columns: [{
            header: 'MIME Type'.t(),
            width: Renderer.messageWidth,
            flex: 1,
            dataIndex: 'string',
            editor: {
                xtype: 'textfield',
                emptyText: '[enter MIME type]'.t(),
                allowBlank: false
            }
        }, {
            xtype: 'checkcolumn',
            width: Renderer.booleanWidth,
            header: 'Scan'.t(),
            dataIndex: 'enabled',
            resizable: false
        }, {
            header: 'Description'.t(),
            width: Renderer.messageWidth,
            flex: 2,
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
