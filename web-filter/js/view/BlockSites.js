Ext.define('Ung.apps.webfilter.view.BlockSites', {
    extend: 'Ung.cmp.Grid',
    alias:  'widget.app-web-filter-blocksites',
    itemId: 'block-sites',
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

    emptyText: 'No Block Sites defined'.t(),

    listProperty: 'settings.blockedUrls.list',
    emptyRow: {
        string: '',
        blocked: true,
        flagged: true,
        description: '',
        javaClass: 'com.untangle.uvm.app.GenericRule'
    },

    bind: '{blockedUrls}',

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
        header: 'Block'.t(),
        dataIndex: 'blocked',
        resizable: false
    }, {
        xtype: 'checkcolumn',
        width: Renderer.booleanWidth,
        header: 'Flag'.t(),
        dataIndex: 'flagged',
        resizable: false,
        tooltip: 'Flag as Violation'.t()
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
