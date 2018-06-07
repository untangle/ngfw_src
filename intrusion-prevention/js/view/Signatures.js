Ext.define('Ung.apps.intrusionprevention.view.Signatures', {
    extend: 'Ung.cmp.Grid',
    alias: 'widget.app-intrusion-prevention-signatures',
    itemId: 'signatures',
    title: 'Signatures'.t(),
    scrollable: true,

    controller: 'unintrusionsignaturesgrid',

    name: 'signatures',

    region: 'center',

    sortableColumns: true,
    plugins: [
        'gridfilters'
    ],
    features: [{
        ftype: 'grouping',
        groupHeaderTpl: '{columnName}: {name} ({rows.length} signature{[values.rows.length > 1 ? "s" : ""]})',
        startCollapsed: true
     }],

    bind: '{signatures}',

    listeners: {
        reconfigure: 'signaturesReconfigure'
    },

    tbar: ['@add', '->', '@import', '@export'],
    recordActions: ['edit', 'copy', 'delete'],
    copyId: 'sid',
    copyIdPreserve: true,
    copyAppendField: 'msg',

    bbar: [ 'Search'.t(), {
        xtype: 'textfield',
        name: 'searchFilter',
        listeners: {
            change: 'filterSearch'
        }
    },{
        xtype: 'checkbox',
        name: 'searchLog',
        boxLabel: 'Log'.t(),
        listeners: {
            change: 'filterLog'
        }
    }, {
        xtype: 'checkbox',
        name: 'searchBlock',
        boxLabel: 'Block'.t(),
        listeners: {
            change: 'filterBlock'
        }
    },{
        xtype: 'tbtext',
        name: 'searchStatus',
        html: 'Loading...'.t(),
        listeners: {
            afterrender: 'updateSearchStatusBar'
        }
    }],

    emptyRow: {
        "classtype": "unknown",
        "category": "app-detect",
        "msg" : "new signature",
        "sid": "1999999",
        "log": true,
        "block": false,
        "signature": "alert tcp any any -> any any ( msg:\"new signature\"; classtype:unknown; sid:1999999; content:\"matchme\"; nocase;)"
    },

    columns: [{
        header: "Sid".t(),
        dataIndex: 'sid',
        width: Renderer.idWidth,
        renderer: 'sidRenderer'
    },{
        header: "Classtype".t(),
        dataIndex: 'classtype',
        width: Renderer.messageWidth,
        renderer: 'classtypeRenderer'
    },{
        header: "Category".t(),
        dataIndex: 'category',
        width: Renderer.messageWidth,
        renderer: 'categoryRenderer'
    },{
        header: "Msg".t(),
        dataIndex: 'msg',
        width: Renderer.messageWidth,
        flex:3,
    },{
        header: "Reference".t(),
        dataIndex: 'signature',
        width: Renderer.messageWidth,
        renderer: 'referenceRenderer'
    },{
        xtype:'checkcolumn',
        header: "Log".t(),
        dataIndex: 'log',
        width: Renderer.booleanWidth,
        listeners: {
            beforecheckchange: 'logBeforeCheckChange'
        },
        checkAll: {
            handler: 'logCheckAll'
        }
    },{
        xtype:'checkcolumn',
        header: "Block".t(),
        dataIndex: 'block',
        width: Renderer.booleanWidth,
        listeners: {
            beforecheckchange: 'blockBeforeCheckChange'
        },
        checkAll: {
            handler: 'blockCheckAll'
        }
    }],

    editorFields: [{
        fieldLabel: 'Classtype'.t(),
        editable: false,
        xtype: 'combo',
        queryMode: 'local',
        bind:{
            value: '{record.classtype}',
            store: '{classtypes}',
        },
        valueField: 'name',
        displayField: 'name',
        forceSelection: true,
        listeners: {
            change: 'editorClasstypeChange'
        }
    },{
        fieldLabel: 'Category'.t(),
        bind:{
            value: '{record.category}',
            store: '{categories}',
        },
        emptyText: "[enter category]".t(),
        allowBlank: false,
        xtype: 'combo',
        queryMode: 'local',
        valueField: 'name',
        displayField: 'name'
    },{
        xtype:'textfield',
        bind: '{record.msg}',
        fieldLabel: 'Msg'.t(),
        emptyText: "[enter mg]".t(),
        allowBlank: false,
        listeners: {
            change: 'editorMsgChange'
        }
    },{
        xtype:'numberfield',
        bind: '{record.sid}',
        fieldLabel: 'Sid'.t(),
        emptyText: '[enter sid]'.t(),
        allowBlank: false,
        hideTrigger: true,
        listeners:{
            change: 'editorSidChange'
        }
     },{
         xtype:'checkbox',
         bind: '{record.log}',
         fieldLabel: 'Log'.t(),
         listeners: {
             change: 'editorLogChange'
         }
     },{
         xtype:'checkbox',
         bind: '{record.block}',
         fieldLabel: 'Block'.t(),
         listeners: {
             change: 'editorBlockChange'
         }
     },{
        xtype:'textareafield',
        bind: '{record.signature}',
        fieldLabel: 'Signature'.t(),
        emptyText: "[enter signature]".t(),
        allowBlank: false,
        height: 100,
        listeners:{
            change: 'editorSignatureChange'
        }
    }]
});
