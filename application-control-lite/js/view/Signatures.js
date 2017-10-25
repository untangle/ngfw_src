Ext.define('Ung.apps.applicationcontrollite.view.Signatures', {
    extend: 'Ung.cmp.Grid',
    alias: 'widget.app-application-control-lite-signatures',
    itemId: 'signatures',
    title: 'Signatures'.t(),

    dockedItems: [{
        xtype: 'toolbar',
        dock: 'top',
        items: ['@add', '->', '@import', '@export']
    }],

    recordActions: ['edit', 'delete'],
    listProperty: 'settings.patterns.list',
    emptyRow: {
        javaClass: 'com.untangle.app.application_control_lite.ApplicationControlLitePattern',
        'protocol': '',
        'description': '',
        'category': '',
        'definition': '',
        'quality': '',
        'blocked': false,
        'alert': false,
        'log': false,
        },

    bind: '{signatureList}',

    emptyText: 'No Signatures Defined'.t(),

    columns: [{
        header: 'Protocol'.t(),
        width: Renderer.messageWidth,
        flex: 1,
        dataIndex: 'protocol'
    }, {
        header: 'Category'.t(),
        width: Renderer.messageWidth,
        flex: 1,
        dataIndex: 'category'
    }, {
        xtype: 'checkcolumn',
        header: 'Block'.t(),
        width: Renderer.booleanWidth,
        dataIndex: 'blocked'
    }, {
        xtype: 'checkcolumn',
        header: 'Log'.t(),
        width: Renderer.booleanWidth,
        dataIndex: 'log',
    }, {
        header: 'Description'.t(),
        width: Renderer.messageWidth,
        flex: 2,
        dataIndex: 'description',
    }],

    editorFields: [{
        xtype: 'textfield',
        bind: '{record.protocol}',
        fieldLabel: 'Protocol'.t()
    }, {
        xtype: 'textfield',
        bind: '{record.category}',
        fieldLabel: 'Category'.t()
    }, {
        xtype: 'checkbox',
        bind: '{record.blocked}',
        fieldLabel: 'Block'.t()
    }, {
        xtype: 'checkbox',
        bind: '{record.log}',
        fieldLabel: 'Log'.t()
    }, {
        xtype: 'textarea',
        bind: '{record.description}',
        fieldLabel: 'Description'.t()
    }, {
        xtype: 'textarea',
        bind: '{record.definition}',
        fieldLabel: 'Signature'.t()
    }]
});
