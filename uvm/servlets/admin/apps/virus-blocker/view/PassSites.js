Ext.define('Ung.apps.virusblocker.view.PassSites', {
    extend: 'Ung.cmp.Grid',
    alias: 'widget.app-virus-blocker-passsites',
    itemId: 'passsites',
    title: 'Pass Sites'.t(),

    dockedItems: [{
        xtype: 'toolbar',
        dock: 'top',
        items: [{
            xtype: 'tbtext',
            padding: '8 5',
            style: { fontSize: '12px', fontWeight: 600 },
            html: 'Do not scan traffic to the specified sites.  Use caution!'.t()
        }]
    }, {
        xtype: 'toolbar',
        dock: 'top',
        items: ['@add', '->', '@import', '@export']
    }],

    recordActions: ['edit', 'delete'],

    listProperty: 'settings.passSites.list',
    emptyRow: {
        string: '',
        enabled: true,
        description: '',
        javaClass: 'com.untangle.uvm.app.GenericRule'
    },

    bind: '{passSites}',

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
