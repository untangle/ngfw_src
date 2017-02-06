Ext.define('Webui.config.events', {
    extend: 'Ung.ConfigWin',
    displayName: 'Events',
    hasReports: true,
    reportCategory: 'Events',
    panelEvent: null,
    panelSyslog: null,
    initComponent: function() {
        this.breadcrumbs = [{
            title: i18n._("Configuration"),
            action: Ext.bind(function() {
                this.cancelAction();
            }, this)
        }, {
            title: i18n._('Events')
        }];

        this.buildEventRules();
        this.buildSyslog();

        this.buildTabPanel([this.panelEventRules, this.panelSyslog]);

        this.tabs.setActiveTab(this.panelEventRules);
        this.callParent(arguments);
    },
    // get event settings
    getEventSettings: function(forceReload) {
        if (forceReload || this.rpc.eventSettings === undefined) {
            try {
                this.rpc.eventSettings = rpc.eventManager.getSettings();
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }
        }
        return this.rpc.eventSettings;
    },
    getEventRuleConditions: function () {
        return [
            {name:"FIELD_CONDITION",displayName: i18n._("Field condition"), type: "editor", editor: Ext.create('Ung.FieldConditionWindow',{}), visible: true, disableInvert: true, allowMultiple: true, allowBlank: false, formatValue: function(value) {
                var result= "";
                if(value) {
                    result = value.field + " " + value.comparator + " " + value.value;
                }
                return result;
            }}
        ];
    },
    // EventRules Panel
    buildEventRules: function() {
        this.panelEventRules = Ext.create('Ext.panel.Panel',{
            name: 'eventRules',
            helpSource: 'reports_event_rules',
            title: i18n._('Event Rules'),
            layout: { type: 'vbox', align: 'stretch' },
            cls: 'ung-panel',
            items: [{
                xtype: 'fieldset',
                title: i18n._('Note'),
                flex: 0,
                html: " " + i18n._("<b>Event Rules</b> process all events to log and/or event administrators when special or noteworthy events occur.")
            },  this.gridEventRules= Ext.create('Ung.grid.Panel',{
                flex: 1,
                name: 'Event Rules',
                settingsCmp: this,
                hasReorder: true,
                addAtTop: false,
                title: i18n._("Event Rules"),
                dataExpression: "getEventSettings().eventRules.list",
                recordJavaClass: "com.untangle.uvm.event.EventRule",
                hasCopy: true,
                copyNameField: 'description',
                copyIdField: 'ruleId',
                emptyRow: {
                    "ruleId": null,
                    "enabled": true,
                    "log": false,
                    "email": false,
                    "limitFrequency": false,
                    "limitFrequencyMinutes": 0,
                    "description": ""
                },
                fields: [{
                    name: 'ruleId'
                }, {
                    name: 'enabled'
                }, {
                    name: 'log'
                }, {
                    name: 'email'
                }, {
                    name: 'limitFrequency'
                }, {
                    name: 'limitFrequencyMinutes'
                }, {
                    name: 'thresholdEnabled'
                }, {
                    name: 'thresholdLimit'
                }, {
                    name: 'thresholdTimeframeSec'
                }, {
                    name: 'thresholdGroupingField'
                }, {
                    name: 'conditions'
                },{
                    name: 'description'
                }, {
                    name: 'javaClass'
                }],
                columns: [{
                    header: i18n._("Rule Id"),
                    width: 50,
                    dataIndex: 'ruleId',
                    renderer: function(value) {
                        if (value < 0) {
                            return i18n._("new");
                        } else {
                            return value;
                        }
                    }
                }, {
                    xtype:'checkcolumn',
                    header: i18n._("Enable"),
                    dataIndex: 'enabled',
                    resizable: false,
                    width:55
                }, {
                    header: i18n._("Description"),
                    width: 200,
                    dataIndex: 'description',
                    flex: 1,
                    editor: {
                        xtype:'textfield',
                        emptyText: i18n._("[no description]"),
                        allowBlank: false,
                        blankText: i18n._("The description cannot be blank.")
                    },
                }, {
                    xtype:'checkcolumn',
                    header: i18n._("Log Locally"),
                    dataIndex: 'log',
                    width:150
                }, {
                    xtype:'checkcolumn',
                    header: i18n._("Log Remotely"),
                    dataIndex: 'remoteLog',
                    width:150
                }, {
                    xtype:'checkcolumn',
                    header: i18n._("Send Email"),
                    dataIndex: 'email',
                    width:150
                }]
            })]
        });
        this.gridEventRules.setRowEditor( Ext.create('Ung.RowEditorWindow',{
            inputLines: [{
                xtype:'checkbox',
                name: "Enable Rule",
                dataIndex: "enabled",
                fieldLabel: i18n._("Enable Rule")
            }, {
                xtype:'textfield',
                name: "Description",
                dataIndex: "description",
                fieldLabel: i18n._("Description"),
                emptyText: i18n._("[no description]"),
                width: 500
            }, {
                xtype:'fieldset',
                title: i18n._("If all of the following conditions are met:"),
                items:[{
                    xtype:'rulebuilder',
                    settingsCmp: this,
                    javaClass: "com.untangle.uvm.event.EventRuleCondition",
                    dataIndex: "conditions",
                    conditions: this.getEventRuleConditions()
                }]
            }, {
                xtype: 'fieldset',
                title: i18n._('And the following conditions:'),
                items:[{
                    xtype:'checkbox',
                    labelWidth: 160,
                    dataIndex: "thresholdEnabled",
                    fieldLabel: i18n._("Enable Thresholds"),
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, newValue) {
                                this.gridEventRules.rowEditor.syncComponents();
                            }, this)
                        }
                    }
                },{
                    xtype:'fieldset',
                    collapsible: false,
                    items: [{
                        xtype:'numberfield',
                        labelWidth: 160,
                        width: 230,
                        dataIndex: "thresholdLimit",
                        fieldLabel: i18n._("Exceeds Threshold Limit")
                    },{
                        xtype: 'container',
                        layout: 'column',
                        margin: '0 0 5 0',
                        items: [{
                            xtype: 'numberfield',
                            labelWidth: 160,
                            width: 230,
                            dataIndex: "thresholdTimeframeSec",
                            allowDecimals: false,
                            allowBlank: false,
                            minValue: 60,
                            maxValue: 60*24*60*7, // 1 week
                            fieldLabel: i18n._('Over Timeframe')
                        }, {
                            xtype: 'label',
                            html: i18n._("(seconds)"),
                            cls: 'boxlabel'
                        }]
                    },{
                        xtype:'textfield',
                        labelWidth: 160,
                        dataIndex: "thresholdGroupingField",
                        fieldLabel: i18n._("Grouping Field")
                    }]
                }]
            }, {
                xtype: 'fieldset',
                title: i18n._('Perform the following action(s):'),
                items:[{
                    xtype:'checkbox',
                    labelWidth: 160,
                    dataIndex: "log",
                    fieldLabel: i18n._("Log Locally")
                }, {
                    xtype:'checkbox',
                    labelWidth: 160,
                    dataIndex: "remoteLog",
                    fieldLabel: i18n._("Log Remotely")
                }, {
                    xtype:'checkbox',
                    labelWidth: 160,
                    dataIndex: "email",
                    fieldLabel: i18n._("Send Email"),
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, newValue) {
                                this.gridEventRules.rowEditor.syncComponents();
                            }, this)
                        }
                    }
                },{
                    xtype:'fieldset',
                    collapsible: false,
                    items: [{
                        xtype:'checkbox',
                        labelWidth: 160,
                        dataIndex: "limitFrequency",
                        fieldLabel: i18n._("Limit Send Frequency")
                    },{
                        xtype: 'container',
                        layout: 'column',
                        margin: '0 0 5 0',
                        items: [{
                            xtype: 'numberfield',
                            labelWidth: 160,
                            width: 230,
                            dataIndex: "limitFrequencyMinutes",
                            allowDecimals: false,
                            allowBlank: false,
                            minValue: 0,
                            maxValue: 24*60*7, // 1 weeks
                            fieldLabel: i18n._('To once per')
                        }, {
                            xtype: 'label',
                            html: i18n._("(minutes)"),
                            cls: 'boxlabel'
                        }]
                    }]
                }]
            }],
            syncComponents: function () {
                var sendEmail=this.down('checkbox[dataIndex=email]').getValue();
                this.down('checkbox[dataIndex=limitFrequency]').setDisabled(!sendEmail);
                this.down('numberfield[dataIndex=limitFrequencyMinutes]').setDisabled(!sendEmail);

                var thresholdEnabled=this.down('checkbox[dataIndex=thresholdEnabled]').getValue();
                this.down('numberfield[dataIndex=thresholdLimit]').setDisabled(!thresholdEnabled);
                this.down('numberfield[dataIndex=thresholdTimeframeSec]').setDisabled(!thresholdEnabled);
                this.down('textfield[dataIndex=thresholdGroupingField]').setDisabled(!thresholdEnabled);
            }
        }));
    },
    // syslog panel
    buildSyslog: function() {
        this.panelSyslog = Ext.create('Ext.panel.Panel',{
            name: 'Syslog',
            helpSource: 'reports_syslog',
            title: i18n._('Syslog'),
            cls: 'ung-panel',
            autoScroll: true,
            defaults: {
                xtype: 'fieldset'
            },
            items: [{
                title: i18n._('Syslog'),
                height: 350,
                items: [{
                    xtype: 'component',
                    margin: '0 0 10 0',
                    html: i18n._('If enabled logged events will be sent in real-time to a remote syslog for custom processing.')
                }, {
                    xtype: 'component',
                    html: i18n._('Warning: Syslog logging can be computationally expensive for servers processing millions of events. Caution is advised.'),
                    cls: 'warning',
                    margin: '0 0 10 10'
                }, {
                    xtype: 'radio',
                    boxLabel: Ext.String.format(i18n._('{0}Disable{1} Syslog Events. (This is the default setting.)'), '<b>', '</b>'),
                    hideLabel: true,
                    name: 'syslogEnabled',
                    checked: !this.getEventSettings().syslogEnabled,
                    listeners: {
                        "change": {
                            fn: Ext.bind( function(elem, checked) {
                                this.getEventSettings().syslogEnabled = !checked;
                                if (checked) {
                                    this.panelSyslog.down("textfield[name=syslogHost]").disable();
                                    this.panelSyslog.down("numberfield[name=syslogPort]").disable();
                                    this.panelSyslog.down("combo[name=syslogProtocol]").disable();
                                }
                            }, this)
                        }
                    }
                },{
                    xtype: 'radio',
                    boxLabel: Ext.String.format(i18n._('{0}Enable{1} Syslog Events.'), '<b>', '</b>'),
                    hideLabel: true,
                    name: 'syslogEnabled',
                    checked: this.getEventSettings().syslogEnabled,
                    listeners: {
                        "change": {
                            fn: Ext.bind( function(elem, checked) {
                                this.getEventSettings().syslogEnabled = checked;
                                if (checked) {
                                    this.panelSyslog.down("textfield[name=syslogHost]").enable();
                                    this.panelSyslog.down("numberfield[name=syslogPort]").enable();
                                    this.panelSyslog.down("combo[name=syslogProtocol]").enable();
                                }
                            }, this)
                        }
                    }
                }, {
                    xtype: 'container',
                    margin: '0 0 0 40',
                    items: [{
                        xtype: 'textfield',
                        fieldLabel: i18n._('Host'),
                        name: 'syslogHost',
                        width: 300,
                        value: this.getEventSettings().syslogHost,
                        toValidate: true,
                        allowBlank: false,
                        blankText: i18n._("A Host must be specified."),
                        disabled: !this.getEventSettings().syslogEnabled,
                        validator: Ext.bind( function( value ){
                            if( value == '127.0.0.1' ||
                                value == 'localhost' ){
                                return i18n._("Host cannot be localhost address.");
                            }
                            return true;
                        }, this)
                    },{
                        xtype: 'numberfield',
                        fieldLabel: i18n._('Port'),
                        name: 'syslogPort',
                        width: 200,
                        value: this.getEventSettings().syslogPort,
                        toValidate: true,
                        allowDecimals: false,
                        minValue: 0,
                        allowBlank: false,
                        blankText: i18n._("You must provide a valid port."),
                        vtype: 'port',
                        disabled: !this.getEventSettings().syslogEnabled
                    },{
                        xtype: 'combo',
                        name: 'syslogProtocol',
                        editable: false,
                        fieldLabel: i18n._('Protocol'),
                        queryMode: 'local',
                        store: [["UDP", i18n._("UDP")],
                                ["TCP", i18n._("TCP")]],
                        value: this.getEventSettings().syslogProtocol,
                        disabled: !this.getEventSettings().syslogEnabled
                    }]
                }]
            }]
        });
    },
    beforeSave: function(isApply, handler) {
        handler.call(this, isApply);
    },
    save: function(isApply) {
        this.saveSemaphore = 1;
        Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));

        this.getEventSettings().eventRules.list=this.gridEventRules.getList();
        this.getEventSettings().syslogHost = this.panelSyslog.down("textfield[name=syslogHost]").getValue();
        this.getEventSettings().syslogPort = this.panelSyslog.down("numberfield[name=syslogPort]").getValue();
        this.getEventSettings().syslogProtocol = this.panelSyslog.down("combo[name=syslogProtocol]").getValue();

        rpc.eventManager.setSettings(Ext.bind(function(result, exception) {
            this.afterSave(exception, isApply);
        }, this), this.getEventSettings());
    },
    afterSave: function(exception, isApply) {
        if(Ung.Util.handleException(exception)) return;
        this.saveSemaphore--;
        if (this.saveSemaphore == 0) {
            if(isApply) {
                this.getEventSettings(true);
                this.clearDirty();
                Ext.MessageBox.hide();
            } else {
                Ext.MessageBox.hide();
                this.closeWindow();
            }
        }
    }
});
//# sourceURL=event.js
