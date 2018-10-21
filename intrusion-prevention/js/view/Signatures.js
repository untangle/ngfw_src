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
        groupHeaderTpl: ['{columnName}: {name:this.formatName} ({rows.length} signature{[values.rows.length > 1 ? "s" : ""]})',{
            formatName: function(name){
                return name == null ? "general" : Ext.String.trim(name);
            }
        }],
        startCollapsed: true
     }],

    bind: '{signatures}',

    listeners: {
        reconfigure: 'signaturesReconfigure'
    },

    // tbar: ['@add', '->', '@import', '@export'],
    tbar: ['@add'],
    recordActions: ['edit', 'copy', 'delete'],
    copyId: 'sid',
    copyIdPreserve: true,
    copyAppendField: 'msg',
    copyModify: [{
        key: 'reserved',
        value: false,
    },{
        key: 'default',
        value: false,
    }],

    bbar: [ 
    'Search'.t(), {
        xtype: 'combo',
        name: 'searchCondition',
        listeners: {
            change: 'searchConditionChange'
        },
        queryMode: 'local',
        valueField: 'value',
        displayField: 'name',
        bind:{
            store: '{searchConditions}',
            value: '{searchCondition}'
        }
    },{
        xtype: 'combo',
        name: 'searchComparator',
        // listeners: {
        //     change: 'filterSearch'
        // }
        queryMode: 'local',
        valueField: 'value',
        displayField: 'name',
        bind:{
            store: '{searchComparators}',
            value: '{searchComparator}'
        }
    },{
        xtype: 'textfield',
        name: 'searchFilter',
        bind: '{searchFilter}',
        listeners: {
            change: 'filterSearch'
        }
    },{
        xtype: 'button',
        text: 'Create Rule',
        iconCls: 'fa fa-plus-circle',
        listeners: {
            click: 'createRuleFromSearch'
        }
    // },{
    //     xtype: 'checkbox',
    //     name: 'searchLog',
    //     boxLabel: 'Log'.t(),
    //     listeners: {
    //         change: 'filterLog'
    //     }
    // }, {
    //     xtype: 'checkbox',
    //     name: 'searchBlock',
    //     boxLabel: 'Block'.t(),
    //     listeners: {
    //         change: 'filterBlock'
    //     }
    },{
        xtype: 'tbtext',
        name: 'searchStatus',
        html: 'Loading...'.t(),
        listeners: {
            afterrender: 'updateSearchStatusBar'
        }
    }],

    restrictedRecords: {
        keyMatch: 'reserved',
        valueMatch: true
    },

    recordModel: 'Ung.model.intrusionprevention.signature',
    emptyRow: "alert tcp any any -> any any ( msg:\"new signature\"; classtype:unknown; sid:1999999; gid:1; content:\"matchme\"; nocase;)",

    columns: [{
        header: "Gid".t(),
        dataIndex: 'gid',
        width: Renderer.idWidth,
        renderer: Ung.apps.intrusionprevention.MainController.idRenderer
    },{
        header: "Sid".t(),
        dataIndex: 'sid',
        width: Renderer.idWidth,
        renderer: Ung.apps.intrusionprevention.MainController.idRenderer
    },{
        header: "Classtype".t(),
        dataIndex: 'classtype',
        width: Renderer.messageWidth,
        renderer: Ung.apps.intrusionprevention.MainController.classtypeRenderer
    },{
        header: "Category".t(),
        dataIndex: 'category',
        width: Renderer.messageWidth,
        renderer: Ung.apps.intrusionprevention.MainController.categoryRenderer
    },{
        header: "Protocol".t(),
        dataIndex: 'protocol',
        width: Renderer.protocolWidth
    },{
        header: "Msg".t(),
        dataIndex: 'msg',
        width: Renderer.messageWidth,
        flex:3,
    },{
        header: "Reference".t(),
        dataIndex: 'sid',
        width: Renderer.messageWidth,
        renderer: Ung.apps.intrusionprevention.MainController.referenceRenderer
    },{
        header: "Default Action".t(),
        dataIndex: 'defaultAction',
        width: Renderer.messageWidth,
        renderer: Ung.apps.intrusionprevention.MainController.actionRenderer
    }],

    editorXtype: 'ung.cmp.unintrusionsignaturesrecordeditor',
    editorFields: [{
        xtype:'numberfield',
        name: 'gid',
        bind: '{record.gid}',
        fieldLabel: 'Gid'.t(),
        emptyText: '[enter gid]'.t(),
        allowBlank: false,
        hideTrigger: true,
        listeners:{
            change: 'editorGidChange'
        }
    },{
        xtype:'numberfield',
        name: 'sid',
        bind: '{record.sid}',
        fieldLabel: 'Sid'.t(),
        emptyText: '[enter sid]'.t(),
        allowBlank: false,
        hideTrigger: true,
        listeners:{
            change: 'editorSidChange'
        }
     },{
        fieldLabel: 'Classtype'.t(),
        editable: false,
        xtype: 'combo',
        queryMode: 'local',
        bind:{
            value: '{record.classtype}',
        },
        store: Ung.apps.intrusionprevention.Main.classtypes,
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
        },
        store: Ung.apps.intrusionprevention.Main.categories,
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
        emptyText: "[enter msg]".t(),
        allowBlank: false,
        listeners: {
            change: 'editorMsgChange'
        }
     },{
        fieldLabel: 'Default Action'.t(),
        editable: false,
        xtype: 'combo',
        queryMode: 'local',
        bind:{
            value: '{record.defaultAction}',
        },
        store: Ung.apps.intrusionprevention.Main.actions,
        valueField: 'name',
        displayField: 'description',
        forceSelection: true,
        listeners: {
            change: 'editorDefaultActionChange'
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
