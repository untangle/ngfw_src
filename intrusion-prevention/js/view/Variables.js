Ext.define('Ung.apps.intrusionprevention.view.Variables', {
    extend: 'Ung.cmp.Grid',
    alias: 'widget.app-intrusion-prevention-variables',
    itemId: 'variables',
    scrollable: true,

    controller: 'unintrusionvariablesgrid',

    name: 'variables',

    region: 'center',

    title: "Variables".t(),
    sortableColumns: true,

    bind: '{variables}',

    tbar: ['@add', '->', '@import', '@export'],
    recordActions: ['edit', 'copy', 'delete'],
    copyAppendField: 'description',

    emptyRow: {
        name: '',
        value: '',
        javaClass: 'com.untangle.app.intrusion_prevention.IntrusionPreventionVariable',
    },

    columns: [{
        header: 'Name'.t(),
        width: Renderer.messageWidth,
        dataIndex: 'name'
    },{
        header: 'Value'.t(),
        width: Renderer.messageWidth,
        flex: 1,
        dataIndex: 'value',
        editor: {
            xtype:'textfield',
            emptyText: "[enter value]".t(),
            allowBlank: false
        }
    }],

    editorFields:[{
        xtype: 'container',
        layout: 'column',
        margin: '0 0 4 80',
        items: [{
            xtype:'textfield',
            bind: '{record.name}',
            fieldLabel: 'Name'.t(),
            emptyText: '[enter name]'.t(),
            labelAlign: 'right',
            labelWdith: 150,
            allowBlank: false,
            listeners: {
                change: 'editorVariableChange'
            }
        }, {
            xtype: 'label',
            name: 'activeVariable',
            hidden: true,
            margin: '5 0 0 10',
            html: 'Variable is used by one or more signatures.'.t(),
            cls: 'boxlabel'
        }]
    },{
        xtype:'textfield',
        bind: '{record.value}',
        fieldLabel: 'Value'.t(),
        emptyText: '[enter value]'.t(),
        allowBlank: false
    }]
});
