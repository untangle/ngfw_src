Ext.define('Ung.apps.intrusionprevention.view.Variables', {
    extend: 'Ung.cmp.Grid',
    alias: 'widget.app-intrusion-prevention-variables',
    itemId: 'variables',

    controller: 'unintrusionvariablesgrid',

    name: 'variables',

    region: 'center',

    title: "Variables".t(),
    sortableColumns: true,

    bind: '{variables}',

    // listeners: {
    //     reconfigure: 'rulesReconfigure'
    // },

    tbar: ['@add', '->', '@import', '@export'],
    recordActions: ['edit', 'copy', 'delete'],
    copyAppendField: 'description',

    emptyRow: {
        'variable': '',
        'definition': '',
        'description': ''
    },

    columns: [{
        header: 'Name'.t(),
        width: 140,
        dataIndex: 'variable'
    },{
        header: 'Definition'.t(),
        width: 170,
        dataIndex: 'definition',
        editor: {
            xtype:'textfield',
            emptyText: "[enter definition]".t(),
            allowBlank: false
        }
    },{
        header: 'Description'.t(),
        dataIndex: 'description',
        flex:1,
        editor: {
            xtype:'textfield',
            emptyText: "[enter description]".t(),
            allowBlank: false
        }
    }],

    editorFields:[{
        xtype: 'container',
        layout: 'column',
        margin: '0 0 4 80',
        items: [{
            xtype:'textfield',
            bind: '{record.variable}',
            fieldLabel: 'Name'.t(),
            emptyText: '[enter name]'.t(),
            labelAlign: 'right',
            labelWdith: 180,
            allowBlank: false,
            listeners: {
                change: 'editorVariableChange'
            }
        }, {
            xtype: 'label',
            name: 'activeVariable',
            hidden: true,
            margin: '5 0 0 10',
            html: 'Variable is used by one or more rules.'.t(),
            cls: 'boxlabel'
        }]
    },{
        xtype:'textfield',
        bind: '{record.definition}',
        fieldLabel: 'Definition'.t(),
        emptyText: '[enter definition]'.t(),
        allowBlank: false
    },{
        xtype:'textfield',
        bind: '{record.description}',
        fieldLabel: 'Description'.t(),
        emptyText: '[enter description]'.t(),
        allowBlank: false
    }]
});
