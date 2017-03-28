Ext.define('Ung.apps.intrusionprevention.view.Rules', {
    extend: 'Ung.cmp.Grid',
    alias: 'widget.app-intrusion-prevention-rules',
    itemId: 'rules',
    title: 'Rules'.t(),

    controller: 'unintrusionrulesgrid',

    name: 'rules',

    region: 'center',
    // helpSource: 'intrusion_prevention_rules',
    sortableColumns: true,
    plugins: [
        'gridfilters'
    ],
    features: [{
        ftype: 'grouping',
        groupHeaderTpl: '{columnName}: {name} ({rows.length} rule{[values.rows.length > 1 ? "s" : ""]})',
        startCollapsed: true
     }],

    bind: '{rules}',

    listeners: {
        reconfigure: 'rulesReconfigure'
    },

    tbar: ['@add', '->', '@import', '@export'],
    recordActions: ['edit', 'copy', 'delete'],

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
        "msg" : "new rule",
        "sid": "1999999",
        "log": true,
        "block": false,
        "rule": "alert tcp any any -> any any ( msg:\"new rule\"; classtype:unknown; sid:1999999; content:\"matchme\"; nocase;)"
    },

    columns: [{
        header: "Sid".t(),
        dataIndex: 'sid',
        width: 70, 
        renderer: 'sidRenderer'
    },{
        header: "Classtype".t(),
        dataIndex: 'classtype',
        width: 100,
        renderer: 'classtypeRenderer'
    },{
        header: "Category".t(),
        dataIndex: 'category',
        width: 100,
        renderer: 'categoryRenderer'
    },{
        header: "Msg".t(),
        dataIndex: 'msg',
        width: 200,
        flex:3,
    },{
        header: "Reference".t(),
        dataIndex: 'rule',
        width: 100,
        renderer: 'referenceRenderer'
    },{
        xtype:'checkcolumn',
        header: "Log".t(),
        dataIndex: 'log',
        width:55,
        listeners: {
            beforecheckchange: 'logBeforeCheckChange'
        },
        // checkAll: {
        //     handler: function(checkbox, checked) {
        //         console.log("do checkall");
        //         // Ext.MessageBox.wait(checked?i18n._("Checking All ..."):i18n._("Unchecking All ..."), i18n._("Please wait"));
        //         // Ext.Function.defer(function() {
        //         //     var grid=checkbox.up("grid");
        //         //     var records=grid.getStore().getRange();
        //         //     grid.getStore().suspendEvents(true);
        //         //     for(var i=0; i<records.length; i++) {
        //         //         records[i].set('log', checked);
        //         //         if(!checked) {
        //         //             records[i].set('block', false);
        //         //         }
        //         //         grid.updateRule(records[i], null );
        //         //     }
        //         //     grid.getStore().resumeEvents();
        //         //     grid.getStore().getFilters().notify('endupdate');
        //         //     Ext.MessageBox.hide();
        //         // }, 100, this);
        //     }
        // }
    },{
        xtype:'checkcolumn',
        header: "Block".t(),
        dataIndex: 'block',
        width:55,
        listeners: {
            beforecheckchange: 'blockBeforeCheckChange'
        }
        // checkAll: {
        //     handler: function(checkbox, checked) {
        //         Ext.MessageBox.wait(checked?i18n._("Checking All ..."):i18n._("Unchecking All ..."), i18n._("Please wait"));
        //         Ext.Function.defer(function() {
        //             var grid=checkbox.up("grid");
        //             var records=grid.getStore().getRange();
        //             grid.getStore().suspendEvents(true);
        //             for(var i=0; i<records.length; i++) {
        //                 records[i].set('block', checked);
        //                 if(checked) {
        //                     records[i].set('log', true);
        //                 }
        //                 grid.updateRule(records[i], null );
        //             }
        //             grid.getStore().resumeEvents();
        //             grid.getStore().getFilters().notify('endupdate');
        //             Ext.MessageBox.hide();
        //         }, 100, this);
        //     }
        // }
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
        bind: '{record.rule}',
        fieldLabel: 'Rule'.t(),
        emptyText: "[enter rule]".t(),
        allowBlank: false,
        height: 100,
        listeners:{
            change: 'editorRuleChange'
        }
    }]
});
