Ext.define('Ung.config.events.view.Triggers', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config-events-triggers',
    itemId: 'triggers',
    title: 'Triggers'.t(),
    scrollable: true,

    bodyPadding: 10,
    layout: { type: 'vbox', align: 'stretch' },

    items: [{
        xtype: 'ungrid',
        title: 'Trigger Rules'.t(),
        region: 'center',

        bind:{
            store: '{triggerRules}'
        },

        listProperty: 'settings.triggerRules.list',
        tbar: ['@add', '->', '@import', '@export'],
        recordActions: ['edit', 'copy', 'delete', 'reorder'],
        copyId: 'ruleId',
        copyAppendField: 'description',

        emptyRow: {
            javaClass: 'com.untangle.uvm.event.TriggerRule',
            conditions: {
                javaClass: 'java.util.LinkedList',
                list: [{
                    comparator: '=',
                    field: 'class',
                    fieldValue: '*SystemStatEvent*',
                    javaClass: 'com.untangle.uvm.event.EventRuleCondition'
                }]
            },
            action: 'TAG_HOST',
            tagName: 'tag',
            tagTarget: 'activeHosts',
            tagLifetimeSec: 300,
            ruleId: -1,
            thresholdEnabled: false,
            thresholdTimeframeSec: 60,
            thresholdGroupingField: null,
            enabled: true
        },

        columns: [
            Column.ruleId,
            Column.enabled,
            Column.description,
            Ung.config.events.MainController.conditionsClass,
            Ung.config.events.MainController.conditions,
            {
                header: 'Action'.t(),
                dataIndex: 'action',
                width: Renderer.messagwWidth,
                renderer: function (value, metaData, record) {
                    if (typeof value === 'undefined') {
                        return 'Unknown action'.t();
                    }
                    switch(value) {
                      case 'TAG_HOST': return 'Tag Host'.t();
                      case 'UNTAG_HOST': return 'Untag Host'.t();
                      case 'TAG_USER':return 'Tag User'.t();
                      case 'UNTAG_USER':return 'Untag User'.t();
                      case 'TAG_DEVICE':return 'Tag Device'.t();
                      case 'UNTAG_DEVICE':return 'Untag Device'.t();
                    default: return 'Unknown Action'.t() + ': ' + value;
                    }
                }
            }],

        editorFields: [
            Field.enableRule(),
            Field.description,
            {
            xtype: 'eventconditionseditor',
            bind: '{record.conditions}',
            fields: {
                type: 'field',
                comparator: 'comparator',
                value: 'fieldValue',
            },
            },{
                xtype: 'fieldset',
                title: 'As well as the following conditions:'.t(),
                items:[{
                    xtype:'checkbox',
                    labelWidth: 160,
                    bind: "{record.thresholdEnabled}",
                    fieldLabel: 'Enable Thresholds'.t(),
                    listeners: {
                        disable: function (ck) {
                            ck.setValue(false);
                        }
                    }
                },{
                    xtype:'fieldset',
                    collapsible: false,
                    hidden: true,
                    disabled: true,
                    bind: {
                        hidden: '{!record.thresholdEnabled}',
                        disabled: '{!record.thresholdEnabled}'
                    },
                    items: [{
                        xtype:'numberfield',
                        fieldLabel: 'Exceeds Threshold Limit'.t(),
                        labelWidth: 160,
                        bind: '{record.thresholdLimit}'
                    },{
                        xtype: 'container',
                        layout: 'column',
                        margin: '0 0 5 0',
                        items: [{
                            xtype: 'numberfield',
                            fieldLabel: 'Over Timeframe'.t(),
                            labelWidth: 160,
                            disabled: true,
                            bind: {
                                value: '{record.thresholdTimeframeSec}',
                                disabled: '{!record.thresholdEnabled}'
                            },
                            allowDecimals: false,
                            allowBlank: false,
                            minValue: 60,
                            maxValue: 60*24*60*7, // 1 week
                        }, {
                            xtype: 'label',
                            html: '(seconds)'.t(),
                            cls: 'boxlabel'
                        }]
                    },{
                        xtype:'textfield',
                        fieldLabel: 'Grouping Field'.t(),
                        labelWidth: 160,
                        bind: '{record.thresholdGroupingField}'
                    }]
                }]
            }, {
                xtype: 'fieldset',
                title: 'Perform the following action(s):'.t(),
                items:[{
                    xtype: 'combo',
                    reference: 'actionType',
                    publishes: 'value',
                    fieldLabel: 'Action Type'.t(),
                    bind: '{record.action}',
                    allowBlank: false,
                    editable: false,
                    labelWidth: 160,
                    store: [
                        ['TAG_HOST', 'Tag Host'.t()],
                        ['UNTAG_HOST', 'Untag Host'.t()],
                        ['TAG_USER', 'Tag User'.t()],
                        ['UNTAG_USER', 'Untag User'.t()],
                        ['TAG_DEVICE', 'Tag Device'.t()],
                        ['UNTAG_DEVICE', 'Untag Device'.t()]
                    ],
                    queryMode: 'local'
                }, {
                    xtype: 'combo',
                    itemId: 'target',
                    fieldLabel: 'Target'.t(),
                    labelWidth: 160,
                    editable: true,
                    forceSelection: false,
                    queryMode: 'local',
                    width: 350,
                    bind:{
                        value: '{record.tagTarget}',
                        store: '{targetFields}'
                    },
                    valueField: 'name',
                    displayField: 'description'
                }, {
                    xtype: 'textfield',
                    bind: {
                        value: '{record.tagName}'
                    },
                    labelWidth: 160,
                    fieldLabel: 'Tag Name'.t(),
                    allowBlank: false
                },{
                    xtype: 'container',
                    layout: 'column',
                    hidden: true,
                    bind: {
                        hidden: '{record.action !== "TAG_HOST" && record.action !== "TAG_USER" && record.action !== "TAG_DEVICE"}'
                    },
                    margin: '0 0 5 0',
                    items: [{
                        xtype: 'numberfield',
                        fieldLabel: 'Tag Lifetime'.t(),
                        labelWidth: 160,
                        disabled: true,
                        bind: {
                            value: '{record.tagLifetimeSec}',
                            disabled: '{record.action !== "TAG_HOST" && record.action !== "TAG_USER" && record.action !== "TAG_DEVICE"}'
                        },
                        allowBlank: false
                    }, {
                        xtype: 'label',
                        html: '(seconds)'.t(),
                        cls: 'boxlabel'
                    }]
                }]
            }]
    }]
});
