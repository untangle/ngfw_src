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
    enableColumnHide: true,
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
    tbar: [
        '@add',
        '-',
    {
        xtype: 'signatureungridfilter'
    }],
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
        width: Renderer.protocolWidth,
        renderer: Ung.apps.intrusionprevention.MainController.signatureRenderer
    },{
        header: "Msg".t(),
        dataIndex: 'msg',
        width: Renderer.messageWidth,
        flex:3,
        renderer: Ung.apps.intrusionprevention.MainController.signatureRenderer
    },{
        header: "Signature".t(),
        dataIndex: 'signature',
        width: Renderer.messageWidth,
        flex:3,
        hidden: true
    },{
        header: "Reference".t(),
        dataIndex: 'sid',
        width: Renderer.messageWidth,
        renderer: Ung.apps.intrusionprevention.MainController.referenceRenderer
    },{
        header: "Recommended Action".t(),
        dataIndex: 'recommendedAction',
        width: Renderer.messageWidth,
        renderer: Ung.apps.intrusionprevention.MainController.recommendedActionRenderer
    },{
        header: "Rule Action".t(),
        dataIndex: 'ruleMatch',
        width: Renderer.messageWidth,
        renderer: Ung.apps.intrusionprevention.MainController.signatureRuleActionRenderer
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
        valueField: 'value',
        displayField: 'value',
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
        valueField: 'value',
        displayField: 'value'
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
        fieldLabel: 'Recommended Action'.t(),
        editable: false,
        xtype: 'combo',
        queryMode: 'local',
        bind:{
            value: '{record.recommendedAction}',
        },
        store: Ung.apps.intrusionprevention.Main.signatureActions,
        valueField: 'value',
        displayField: 'description',
        forceSelection: true,
        listeners: {
            change: 'editorRecommendedActionChange'
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
