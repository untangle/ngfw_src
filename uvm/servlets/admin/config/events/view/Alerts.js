Ext.define('Ung.config.events.view.Alerts', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config-events-alerts',
    itemId: 'alerts',
    title: 'Alerts'.t(),
    scrollable: true,

    bodyPadding: 10,

    items: [{
        xtype: 'ungrid',
        title: 'Alert Rules'.t(),
        region: 'center',

        listProperty: 'settings.alertRules.list',
        tbar: ['@add', '->', '@import', '@export'],
        recordActions: ['edit', 'copy', 'delete', 'reorder'],
        copyId: 'ruleId',
        copyAppendField: 'description',

        bind:{
            store: '{alertRules}'
        },

        emptyRow: {
            javaClass: 'com.untangle.uvm.event.AlertRule',
            conditions: {
                javaClass: 'java.util.LinkedList',
                list: [{
                    comparator: '=',
                    field: 'class',
                    fieldValue: '*SystemStatEvent*',
                    javaClass: 'com.untangle.uvm.event.EventRuleCondition'
                }]
            },
            ruleId: -1,
            enabled: true,
            thresholdEnabled: false,
            thresholdTimeframeSec: 60,
            thresholdGroupingField: null,
            email: true,
            emailLimitFrequency: false,
            emailLimitFrequencyMinutes: 0,
        },

        columns: [
            Column.ruleId,
            Column.enabled,
            Column.description,
            Ung.config.events.MainController.conditionsClass,
            Ung.config.events.MainController.conditions,
        {
            xtype:'checkcolumn',
            header: 'Log'.t(),
            dataIndex: 'log',
            width: Renderer.booleanWidth,
            sortable: false
        },{
            xtype:'checkcolumn',
            header: 'Email'.t(),
            dataIndex: 'email',
            width: Renderer.booleanWidth
        }],

        editorFields: [
            Field.enableRule(),
            Field.description,
            // Field.conditions,
        {
            xtype: 'eventconditionseditor',
            bind: '{record.conditions}',
            fields: {
                type: 'field',
                comparator: 'comparator',
                value: 'fieldValue',
            }
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
                xtype:'checkbox',
                fieldLabel: 'Log'.t(),
                labelWidth: 160,
                bind: '{record.log}'
            }, {
                xtype:'checkbox',
                fieldLabel: 'Send Email'.t(),
                labelWidth: 160,
                bind: '{record.email}',
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
                    hidden: '{record.email == false}',
                    disabled: '{record.email == false}'
                },
                items: [{
                    xtype:'checkbox',
                    fieldLabel: 'Limit Send Frequency'.t(),
                    labelWidth: 160,
                    bind: '{record.emailLimitFrequency}'
                },{
                    xtype: 'container',
                    layout: 'column',
                    margin: '0 0 5 0',
                    items: [{
                        xtype: 'numberfield',
                        fieldLabel: 'To once per'.t(),
                        labelWidth: 160,
                        bind: '{record.emailLimitFrequencyMinutes}',
                        allowDecimals: false,
                        allowBlank: false,
                        minValue: 0,
                        maxValue: 24*60*7, // 1 weeks
                    }, {
                        xtype: 'label',
                        html: '(minutes)'.t(),
                        cls: 'boxlabel'
                    }]
                }]
            }]
        }]
    }]
});
