Ext.define('Webui.untangle-node-protofilter.settings', {
    extend: 'Ung.NodeWin',
    panelStatus: null,
    gridProtocolList: null,
    gridEventLog: null,
    initComponent: function() {
        this.buildStatus();
        this.buildProtocolList();
        this.buildEventLog();
        // builds the tab panel with the tabs
        this.buildTabPanel([this.panelStatus, this.gridProtocolList, this.gridEventLog]);
        this.callParent(arguments);
    },
    // Status Panel
    buildStatus: function() {
        this.panelStatus = Ext.create('Ext.panel.Panel',{
            name: 'Status',
            helpSource: 'application_control_lite_status',
            isDirty: function() {
                return false;
            },
            title: this.i18n._('Status'),
            cls: 'ung-panel',
            autoScroll: true,
            items: [{
                xtype: 'fieldset',
                title: this.i18n._('Status'),
                html: Ext.String.format(this.i18n._("Application Control Lite logs and blocks sessions using custom signatures on the session content."))
            }, {
                xtype: 'fieldset',
                defaults: {
                    xtype: "displayfield",
                    labelWidth: 200
                },
                items: [{
                    fieldLabel: this.i18n._('Total Signatures Available'),
                    name: 'Total Signatures Available',
                    value: this.getRpcNode().getPatternsTotal()
                }, {
                    fieldLabel: this.i18n._('Total Signatures Logging'),
                    name: 'Total Signatures Logging',
                    value: this.getRpcNode().getPatternsLogged()
                }, {
                    fieldLabel: this.i18n._('Total Signatures Blocking'),
                    name: 'Total Signatures Blocking',
                    value: this.getRpcNode().getPatternsBlocked()
                }]
            }, {
                xtype: 'fieldset',
                title: this.i18n._('Note'),
                html: Ext.String.format(this.i18n._("Caution and discretion is advised in configuring Application Control Lite at the the risk of harmful false positives."))
            }]
        });
    },
    // Protocol list grid
    buildProtocolList: function() {
        this.gridProtocolList = Ext.create('Ung.grid.Panel',{
            settingsCmp: this,
            name: 'Signatures',
            helpSource: 'application_control_lite_signatures',
            title: this.i18n._("Signatures"),
            dataProperty: "patterns",
            recordJavaClass: "com.untangle.node.protofilter.ProtoFilterPattern",
            emptyRow: {
                "protocol": "",
                "category": "",
                "log": false,
                "blocked": false,
                "description": "",
                "definition": ""
            },
            sortField: 'category',
            fields: [{
                name: 'id'
            },{
                name: 'alert',
                type: 'boolean'
            },{
                name: 'quality',
                type: 'string'
            },{
                name: 'protocol',
                type: 'string'
            },{
                name: 'category',
                type: 'string'
            }, {
                name: 'log',
                type: 'boolean'
            }, {
                name: 'blocked',
                type: 'boolean'
            }, {
                name: 'description',
                type: 'string'
            }, {
                name: 'definition',
                type: 'string'
            }],
            columns: [{
                header: this.i18n._("Protocol"),
                width: 200,
                dataIndex: 'protocol',
                editor: {
                    xtype:'textfield',
                    emptyText: this.i18n._("[enter protocol]"),
                    allowBlank:false
                }
            }, {
                header: this.i18n._("Category"),
                width: 200,
                dataIndex: 'category',
                editor: {
                    xtype:'textfield',
                    emptyText: this.i18n._("[enter category]"),
                    allowBlank:false
                }
            }, {
                xtype:'checkcolumn',
                header: "<b>" + this.i18n._("Block") + "</b>",
                dataIndex: 'blocked',
                resizable: false,
                width:55
            },  {
                xtype:'checkcolumn',
                header: "<b>" + this.i18n._("Log") + "</b>",
                dataIndex: 'log',
                resizable: false,
                width:55
            }, {
                header: this.i18n._("Description"),
                width: 200,
                dataIndex: 'description',
                flex: 1,
                editor:{
                    xtype:'textfield',
                    emptyText: this.i18n._("[no description]")
                }
            }],
            rowEditorInputLines: [{
                xtype:'textfield',
                name: "Protocol",
                dataIndex: "protocol",
                fieldLabel: this.i18n._("Protocol"),
                emptyText: this.i18n._("[enter protocol]"),
                allowBlank: false,
                width: 400
            }, {
                xtype:'textfield',
                name: "Category",
                dataIndex: "category",
                fieldLabel: this.i18n._("Category"),
                emptyText: this.i18n._("[enter category]"),
                allowBlank: false,
                width: 400
            },  {
                xtype:'checkbox',
                name: "Block",
                dataIndex: "blocked",
                fieldLabel: this.i18n._("Block")
            }, {
                xtype:'checkbox',
                name: "Log",
                dataIndex: "log",
                fieldLabel: this.i18n._("Log")
            }, {
                xtype:'textarea',
                name: "Description",
                dataIndex: "description",
                fieldLabel: this.i18n._("Description"),
                emptyText: this.i18n._("[no description]"),
                width: 400,
                height: 60
            }, {  
                xtype:'textarea',
                name: "Signature",
                dataIndex: "definition",
                fieldLabel: this.i18n._("Signature"),
                emptyText: this.i18n._("[enter signature]"),
                allowBlank: false,
                width: 400,
                height: 60
            }]
        });
    },
    buildEventLog: function() {
        this.gridEventLog = Ung.CustomEventLog.buildSessionEventLog (this, 'EventLog', i18n._('Event Log'),
            'application_control_lite_event_log',
            ['time_stamp','application_control_lite_blocked','c_client_addr','username','c_server_addr','application_control_lite_protocol'], this.getRpcNode().getEventQueries);
    },
    beforeSave: function(isApply, handler) {
        this.getSettings().patterns.list = this.gridProtocolList.getList();
        handler.call(this, isApply);
    }
});
//# sourceURL=protofilter-settings.js