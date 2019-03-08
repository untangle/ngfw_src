Ext.define('Ung.apps.webfilter.view.PassClients', {
    extend: 'Ung.cmp.Grid',
    alias: 'widget.app-web-filter-passclients',
    itemId: 'pass-clients',
    title: 'Pass Clients'.t(),


    dockedItems: [{
        xtype: 'toolbar',
        dock: 'top',
        items: [{
            xtype: 'tbtext',
            padding: '8 5',
            style: { fontSize: '12px', fontWeight: 600 },
            html: 'Allow access for client networks regardless of matching block policies.'.t()
        }]
    }, {
        xtype: 'toolbar',
        dock: 'top',
        items: ['@add', '->', '@import', '@export']
    }],

    recordActions: ['edit', 'delete'],

    emptyText: 'No Pass Clients defined'.t(),

    listProperty: 'settings.passedClients.list',
    emptyRow: {
        string: '1.2.3.4',
        enabled: true,
        description: '',
        javaClass: 'com.untangle.uvm.app.GenericRule'
    },

    bind: '{passedClients}',

    columns: [{
        header: 'IP address/range'.t(),
        width: Renderer.networkWidth,
        flex: 1,
        dataIndex: 'string',
        editor: {
            xtype: 'textfield',
            emptyText: '[enter IP address/range]'.t(),
            vtype: 'ipMatcher',
            allowBlank: false
        }
    }, {
        xtype: 'checkcolumn',
        width: Renderer.booleanWidth,
        header: 'Pass'.t(),
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
        fieldLabel: 'IP address/range'.t(),
        emptyText: '[enter IP address/range]'.t(),
        vtype: 'ipMatcher',
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

});
