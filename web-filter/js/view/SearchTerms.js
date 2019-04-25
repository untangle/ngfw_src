Ext.define('Ung.apps.webfilter.view.SearchTerms', {
    extend: 'Ung.cmp.Grid',
    alias:  'widget.app-web-filter-searchterms',
    itemId: 'search-terms',
    title:  'Search Terms'.t(),

    controller: 'unwebfiltersearchtermsgrid',

    import:{
        items: [{
            fieldLabel: 'Format'.t(),
            name: 'fileType',
            editable: false,
            xtype: 'combo',
            queryMode: 'local',
            value: 'COMMA',
            store: [[
                'COMMA', 'Comma delimited'
            ],[
                'NEWLINE', 'Newline delimited'
            ],[
                'JSONArray', 'JSON array'
            ]],
            forceSelection: true
        },{
            fieldLabel: 'Default actions'.t(),
            displayName: 'Default actions'.t(),
            xtype: 'checkboxgroup',
            columns: 1,
            items: [{
                boxLabel: 'Block'.t(),
                name: 'defaultActions',
                inputValue: 'block',
                checked: true
            },{
                boxLabel: 'Flag'.t(),
                name: 'defaultActions',
                inputValue: 'flag',
                checked: true
            }]
        }],
        handler: 'handleImport'
    },

    dockedItems: [{
        xtype: 'toolbar',
        dock: 'top',
        items: [{
            xtype: 'tbtext',
            padding: '8 5',
            style: { fontSize: '12px', fontWeight: 600 },
            html: 'Block or flag searches with the specified term.'.t()
        },{
            xtype: 'ungridfilter'
        }]
    }, {
        xtype: 'toolbar',
        dock: 'top',
        items: ['@add', '->', '@import', '@export']
    }],

    recordActions: ['edit', 'delete'],

    emptyText: 'No Search Terms defined'.t(),

    listProperty: 'settings.searchTerms.list',
    emptyRow: {
        string: '',
        blocked: true,
        flagged: true,
        description: '',
        javaClass: 'com.untangle.uvm.app.GenericRule'
    },

    bind: '{searchTerms}',

    columns: [{
        header: 'Term'.t(),
        width: Renderer.uriWidth,
        flex: 1,
        dataIndex: 'string',
        editor: {
            xtype: 'textfield',
            emptyText: '[enter term]'.t(),
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
        fieldLabel: 'Term'.t(),
        emptyText: '[enter term]'.t(),
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
