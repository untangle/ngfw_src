Ext.define('Ung.config.events.view.Syslog', {
    extend: 'Ext.form.Panel',
    alias: 'widget.config-events-syslog',
    itemId: 'syslog',
    title: 'Syslog'.t(),
    scrollable: true,

    bodyPadding: 10,
    withValidation: true,

    items:[
    {
        xtype: 'ungrid',
        title: 'Servers'.t(),
        itemId: 'syslogservers',
        region: 'center',

        restrictedRecords: {
            keyMatch: 'reserved',
            valueMatch: true
        },

        padding: '0 0 10 0',
        bind: {
            store: '{syslogServers}',
        },

        listProperty: 'settings.syslogServers.list',
        tbar: ['@add'],
        recordActions: ['edit', 'delete'],

        emptyText: 'No Servers Configured'.t(),

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
            Column.description,
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
        },{
            header: 'Tag'.t(),
            width: Renderer.tagsWidth,
            dataIndex: 'tag',
            renderer: function(value, column, record) {
                if(!value) {
                    return Ext.String.format('uvm-to-{0}', record.get('host'));
                }
                return value;
            }
		}],

        editorFields: [
            Field.enableRule(),
            {
                xtype: 'textfield',
                fieldLabel: 'Description'.t(),
                bind: '{record.description}',
                emptyText: '[no description]'.t(),
                allowBlank: false,
                validator: function(value) {
                    var store = this.up('#syslogservers').getStore(),
                        currentServerId = this.up('window').getViewModel().get('record.serverId');
                
                    // Check if a record with the same description exists in the store
                    var isDescUnique = store.findBy(function(record) {
                        if(currentServerId == -1) {
                            return record.get('description') === value;
                        } else {
                            return record.get('serverId') !== currentServerId && record.get('description') === value;
                        }
                    }) === -1;
                    return (isDescUnique) ? true : 'Duplicate description.'.t();
                }
            },
            {
                xtype: 'textfield',
                fieldLabel: 'Host'.t(),
                bind: '{record.host}',
                emptyText: '[no host]'.t(),
                allowBlank: false,
                blankText: 'This field is required'.t()
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
            },{
                xtype: 'textfield',
                fieldLabel: 'Tag'.t(),
                bind: '{record.tag}',
                emptyText: '[default tag: uvm-to-{host}]'.t(),
                validator: function(value) {
                    var store = this.up('#syslogservers').getStore(),
                        currentServerId = this.up('window').getViewModel().get('record.serverId');
                
                    // Check if a record with the same tag exists in the store
                    var isTagUnique = store.findBy(function(record) {
                        if(currentServerId == -1) {
                            return record.get('tag') === value;
                        } else {
                            return record.get('serverId') !== currentServerId && record.get('tag') === value;
                        }                        
                    }) === -1;
                    return (isTagUnique) ? true : 'Tag already exists.'.t();
                }
            }
        ]
    },{
        xtype: 'container',
        padding: '8 5',
        style: { fontSize: '12px', background: '#DADADA'},
        html: '<i class="fa fa-info-circle" style="color: orange;"></i> ' + 'Save the New/Modified/Deleted records in Syslog Server grid to enable Syslog Rules'.t(),
        hidden: true,
        bind: {
            hidden: '{!syslogRuleGridDisabled}'
        }
    },{
        xtype: 'ungrid',
        controller: 'uneventssyslogrulesgrid',
        title: 'Rules'.t(),
        itemId: 'syslogrules',
        region: 'center',

        disabled: false,
        bind: {
            store: '{syslogRules}',
            disabled: '{syslogRuleGridDisabled || syslogServersGridEmpty}'
        },

        listProperty: 'settings.syslogRules.list',
        tbar: ['@add'],
        recordActions: ['edit', 'copy', 'delete', 'reorder'],
        copyId: 'ruleId',
        copyAppendField: 'description',

        emptyText: 'No Rules Configured'.t(),

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
            syslog: true,
            syslogServers: {
                "javaClass": "java.util.LinkedList",
                "list": []
            }
        },

        columns: [
            Column.ruleId,
            Column.enabled,
            Column.description,
            Ung.config.events.MainController.conditionsClass,
            Ung.config.events.MainController.conditions,
        {
            header: 'Syslog Servers'.t(),
            width: Renderer.conditionsWidth,
            flex: 2,
            dataIndex: 'syslogServers',
            renderer: 'sysLogServersRenderer'
        }],

        editorXtype: 'ung.cmp.unsyslogruleseditor',
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
            itemId: 'actioncontainer'
        }]
    }]
});
