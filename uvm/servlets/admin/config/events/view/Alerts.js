Ext.define('Ung.config.events.view.Alerts', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config.events.alerts',

    title: 'Alerts'.t(),

    bodyPadding: 10,

    items: [{
        xtype: 'uneventgrid',
        title: 'Alert Rules'.t(),
        region: 'center',

        controller: 'uneventsgrid',

        listProperty: 'settings.alertRules.list',
        tbar: ['@add'],
        // !!! add copy action
        recordActions: ['edit', 'copy', 'delete', 'reorder'],

        ruleJavaClass: 'com.untangle.uvm.event.EventRuleCondition',
        bind:{
            store: '{alertRules}',
            conditions: '{conditions}'
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
            email: true,
            alertLimitFrequency: false,
            alertLimitFrequencyMinutes: 0,
        },

        columns: [
            Column.ruleId,
            Column.enabled,
            Column.description,
        {
            xtype:'checkcolumn',
            header: 'Log'.t(),
            dataIndex: 'log',
            width:55
        }],

        editorFields: [
            Field.enableRule(),
            Field.description,
            Field.conditions,
        {
            xtype: 'fieldset',
            title: 'And the following conditions:'.t(),
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
                    hidden: '{record.thresholdEnabled != true}',
                    disabled: '{record.thresholdEnabled != true}'
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
                        bind: '{record.thresholdTimeframeSec}',
                        allowDecimals: false,
                        // allowBlank: false,
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
                    bind: '{record.alertLimitFrequency}'
                },{
                    xtype: 'container',
                    layout: 'column',
                    margin: '0 0 5 0',
                    items: [{
                        xtype: 'numberfield',
                        fieldLabel: 'To once per'.t(),
                        labelWidth: 160,
                        bind: '{record.alertLimitFrequencyMinutes}',
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
