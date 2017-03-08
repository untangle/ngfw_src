Ext.define('Ung.apps.webfilter.view.PassSites', {
    extend: 'Ung.cmp.Grid',
    alias: 'widget.app-web-filter-passsites',
    itemId: 'passsites',
    title: 'Pass Sites'.t(),

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

    recordActions: ['edit', 'delete'],

    listProperty: 'settings.passedUrls.list',
    emptyRow: {
        string: '',
        enabled: true,
        description: '',
        javaClass: 'com.untangle.uvm.node.GenericRule'
    },

    bind: '{passedUrls}',

    columns: [{
        header: 'Site'.t(),
        width: 200,
        dataIndex: 'string',
        editor: {
            xtype: 'textfield',
            emptyText: '[enter site]'.t(),
            allowBlank: false,
            validator: Util.urlValidator
        }
    }, {
        xtype: 'checkcolumn',
        width: 55,
        header: 'Pass'.t(),
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

});
