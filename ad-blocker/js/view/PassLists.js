Ext.define('Ung.apps.ad-blocker.view.PassLists', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-ad-blocker-passlists',
    itemId: 'pass-lists',
    title: 'Pass Lists'.t(),
    scrollable: true,

    layout: 'border',
    border: false,

    defaults: {
        xtype: 'ungrid',
    },

    items: [{
        region: 'center',
        title: 'Passed Sites'.t(),

        tbar: ['@add', '->', '@import', '@export'],
        recordActions: ['edit', 'delete'],

        listProperty: 'settings.passedUrls.list',
        bind: '{passedUrls}',

        emptyText: 'No Passed Sites Defined'.t(),

        emptyRow: {
            string: '',
            enabled: true,
            description: '',
            javaClass: 'com.untangle.uvm.app.GenericRule'
        },

        columns: [{
            header: 'Site'.t(),
            width: Renderer.messagwWidth,
            dataIndex: 'string',
            editor:{
                xtype: 'textfield',
                allowBlank: false,
                emptyText: '[enter site]'.t(),
                validator: Util.urlValidator,
                blankText: 'Invalid URL specified'.t()
            }
        }, {
            xtype:'checkcolumn',
            header: 'Pass'.t(),
            dataIndex: 'enabled',
            resizable: false,
            width: Renderer.booleanWidth
        }, {
            header: 'Description'.t(),
            width: Renderer.messagwWidth,
            dataIndex: 'description',
            flex: 1,
            editor: {
                xtype: 'textfield',
                emptyText: '[no description]'.t()
            }
        }],

        editorFields: [{
            xtype: 'textfield',
            bind: '{record.string}',
            fieldLabel: 'Site'.t(),
            emptyText: '[enter site]'.t(),
            allowBlank: false,
            width: 400,
            validator: Util.urlValidator
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
        region: 'south',
        height: '50%',
        split: true,

        title: 'Passed Client IP Addresses'.t(),

        tbar: ['@add', '->', '@import', '@export'],
        recordActions: ['edit', 'delete'],

        listProperty: 'settings.passedClients.list',
        bind: '{passedClients}',

        emptyText: 'No Passed Client IP Addresses Defined'.t(),

        emptyRow: {
            string: '1.2.3.4',
            enabled: true,
            description: '',
            javaClass: 'com.untangle.uvm.app.GenericRule'
        },

        columns: [{
            header: 'IP Address/Range'.t(),
            width: Renderer.networkWidth,
            dataIndex: 'string',
            editor:{
                xtype: 'textfield',
                allowBlank: false,
                emptyText: '[enter ip]'.t()
            }
        }, {
            xtype:'checkcolumn',
            header: 'Pass'.t(),
            dataIndex: 'enabled',
            resizable: false,
            width: Renderer.booleanWidth
        }, {
            header: 'Description'.t(),
            width: Renderer.messagwWidth,
            dataIndex: 'description',
            flex: 1,
            editor: {
                xtype: 'textfield',
                emptyText: '[no description]'.t()
            }
        }],

        editorFields: [{
            xtype: 'textfield',
            bind: '{record.string}',
            fieldLabel: 'IP address/range'.t(),
            emptyText: '[enter ip]'.t(),
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
