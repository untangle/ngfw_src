Ext.define('Ung.config.events.view.Syslog', {
    extend: 'Ext.form.Panel',
    alias: 'widget.config-events-syslog',
    itemId: 'syslog',
    title: 'Syslog'.t(),
    scrollable: true,

    bodyPadding: 10,
    withValidation: true,

    items:[{
        title: 'Remote Syslog Configuration'.t(),
        items: [{
            xtype: 'component',
            html: 'If enabled logged events will be sent in real-time to a remote syslog for custom processing.'.t()
        }, {
            xtype:'checkbox',
            labelWidth: 160,
            bind: "{settings.syslogEnabled}",
            fieldLabel: 'Enable Remote Syslog'.t(),
            listeners: {
                disable: function (ck) {
                    ck.setValue(false);
                }
            }
        }]
    },{
        xtype: 'ungrid',
        title: 'Syslog Servers'.t(),
        itemId: 'syslogservers',
        region: 'center',

        hidden: true,
        disabled: true,
        padding: '20 0',
        bind: {
            store: '{syslogServers}',
            hidden: '{settings.syslogEnabled == false}',
            disabled: '{settings.syslogEnabled == false}'
        },

        listProperty: 'settings.syslogServers.list',
        tbar: ['@add'],
        recordActions: ['edit', 'delete'],

        emptyRow: {
            javaClass: 'com.untangle.uvm.event.SyslogServer',
            serverId: -1,
            enabled: true,
            port: 514,
            protocol: "UDP"
        },

        columns: [
            Column.serverId,
            Column.enabled,
        {
            header: 'Host'.t(),
            dataIndex: 'host',
            flex: 1,
            width: Renderer.hostnameWidth
        }, {
            header: 'Port'.t(),
            width: Renderer.portWidth,
            dataIndex: 'port'
        },{
            header: 'Protocol'.t(),
            width: Renderer.protocolWidth,
            dataIndex: 'protocol'
        }],

        editorFields: [
            Field.enableRule(),
            {
                xtype: 'textfield',
                fieldLabel: 'Host'.t(),
                bind: '{record.host}',
                emptyText: '[no host]'.t(),
                allowBlank: false
            },{
                xtype: 'numberfield',
                fieldLabel: 'Port'.t(),
                bind: '{record.port}',
                toValidate: true,
                allowDecimals: false,
                minValue: 0,
                allowBlank: false,
                blankText: 'You must provide a valid port.'.t(),
                vtype: 'port'
            },{
                xtype: 'combo',
                editable: false,
                fieldLabel: 'Protocol'.t(),
                bind: '{record.protocol}',
                queryMode: 'local',
                store: [["UDP", 'UDP'.t()],
                        ["TCP", "TCP".t()]]
            }
        ]
    },{
        xtype: 'ungrid',
        title: 'Syslog Rules'.t(),
        region: 'center',

        hidden: true,
        disabled: true,
        bind: {
            store: '{syslogRules}',
            hidden: '{settings.syslogEnabled == false}',
            disabled: '{settings.syslogEnabled == false}'
        },

        listProperty: 'settings.syslogRules.list',
        tbar: ['@add'],
        recordActions: ['edit', 'copy', 'delete', 'reorder'],
        copyId: 'ruleId',
        copyAppendField: 'description',

        emptyRow: {
            javaClass: 'com.untangle.uvm.event.SyslogRule',
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
            syslog: true
        },

        columns: [
            Column.ruleId,
            Column.enabled,
            Column.description,
            Ung.config.events.MainController.conditionsClass,
            Ung.config.events.MainController.conditions,
        {
            xtype:'checkcolumn',
            header: 'Remote Syslog'.t(),
            dataIndex: 'syslog',
            width: Renderer.booleanWidth + 30
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
            allowAllClasses: true
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
                fieldLabel: 'Remote Syslog'.t(),
                labelWidth: 160,
                bind: '{record.syslog}',
                listeners: {
                    disable: function (ck) {
                        ck.setValue(false);
                    }
                }
            }]
        }]
    }]
});
