Ext.define('Ung.apps.webfilter.view.BlockSites', {
    extend: 'Ung.cmp.Grid',
    alias:  'widget.app-webfilter-blocksites',
    itemId: 'blocksites',
    title:  'Block Sites'.t(),

    dockedItems: [{
        xtype: 'toolbar',
        dock: 'top',
        items: [{
            xtype: 'tbtext',
            padding: '8 5',
            style: { fontSize: '12px', fontWeight: 600 },
            html: 'Block or flag access to sites associated with the specified category.'.t()
        }]
    }, {
        xtype: 'toolbar',
        dock: 'top',
        items: ['@add', '->', '@import', '@export']
    }],

    recordActions: ['edit', 'delete'],

    listProperty: 'settings.blockedUrls.list',
    emptyRow: {
        string: '',
        blocked: true,
        flagged: true,
        description: '',
        javaClass: 'com.untangle.uvm.node.GenericRule'
    },

    bind: '{blockedUrls}',

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
        header: 'Block'.t(),
        dataIndex: 'blocked',
        resizable: false
    }, {
        xtype: 'checkcolumn',
        width: 55,
        header: 'Flag'.t(),
        dataIndex: 'flagged',
        resizable: false,
        tooltip: 'Flag as Violation'.t()
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
        bind: '{record.blocked}',
        fieldLabel: 'Block'.t()
    }, {
        xtype: 'checkbox',
        bind: '{record.flagged}',
        fieldLabel: 'Flag'.t(),
        tooltip: 'Flag as Violation'.t()
    }, {
        xtype: 'textarea',
        bind: '{record.description}',
        fieldLabel: 'Description'.t(),
        emptyText: '[no description]'.t(),
        width: 400,
        height: 60
    }]
});
