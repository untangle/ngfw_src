Ext.define('Ung.apps.webfilter.view.PassSites', {
    extend: 'Ung.cmp.Grid',
    alias: 'widget.app-web-filter-passsites',
    itemId: 'pass-sites',
    title: 'Pass Sites'.t(),
    withValidation: false,
    controller: 'passsites',
    dockedItems: [{
        xtype: 'toolbar',
        dock: 'top',
        items: [{
            xtype: 'tbtext',
            padding: '8 5',
            style: { fontSize: '12px', fontWeight: 600 },
            html: 'Allow access to the specified site regardless of matching block policies.'.t()
        }]
    }, {
        xtype: 'toolbar',
        dock: 'top',
        items: ['@add', '->', '@import', '@export']
    }],

    recordActions: ['edit', 'copy', 'delete'],
    copyAppendField: 'description',

    emptyText: 'No Pass Sites defined'.t(),

    importValidationJavaClass: true,

    viewConfig: {
        getRowClass : Ung.util.Util.getGlobalRowClass
    },

    listProperty: 'settings.passedUrls.list',
    emptyRow: {
        string: '',
        enabled: true,
        isGlobal: false,
        description: '',
        javaClass: 'com.untangle.uvm.app.GenericRule'
    },

    bind: '{passedUrls}',

    columns: [{
        header: 'Site'.t(),
        width: Renderer.uriWidth,
        flex: 1,
        dataIndex: 'string',
        editor: {
            xtype: 'textfield',
            emptyText: '[enter site]'.t(),
            allowBlank: false
        }
    }, {
        xtype: 'checkcolumn',
        width: Renderer.booleanWidth,
        header: 'Pass'.t(),
        dataIndex: 'enabled',
        resizable: false
    }, {
        xtype: 'checkcolumn',
        width: Renderer.booleanWidth,
        header: 'Global'.t(),
        dataIndex: 'isGlobal',
        resizable: false,
        listeners: {
            beforecheckchange: Ung.util.Util.canToggleGlobalCheckbox
        },
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
        fieldLabel: 'Site'.t(),
        emptyText: '[enter site]'.t(),
        allowBlank: false,
        width: 400
    }, {
        xtype: 'checkbox',
        bind: '{record.enabled}',
        fieldLabel: 'Pass'.t()
    }, {
        xtype: 'checkbox',
        bind: {
            value: '{record.isGlobal}',
            hidden: '{!isAddAction}'
        }, 
        fieldLabel: 'Global'.t(),
    }, {
        xtype: 'textarea',
        bind: '{record.description}',
        fieldLabel: 'Description'.t(),
        emptyText: '[no description]'.t(),
        width: 400,
        height: 60
    }]

});
