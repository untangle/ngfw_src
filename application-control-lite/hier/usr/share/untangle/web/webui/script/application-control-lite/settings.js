Ext.define('Webui.application-control-lite.settings', {
    extend: 'Ung.AppWin',
    gridProtocolList: null,
    gridEventLog: null,
    getAppSummary: function() {
        return i18n._("Application Control Lite identifies, logs, and blocks sessions based on the session content using custom signatures.");
    },
    initComponent: function() {
        this.buildProtocolList();
        this.buildTabPanel([this.gridProtocolList]);
        this.callParent(arguments);
    },
    // Status Panel
    buildStatus: function() {
        this.panelStatus = Ext.create('Ung.panel.Status',{
            settingsCmp: this,
            helpSource: 'application_control_lite_status',
            itemsToAppend: [{
                xtype: 'fieldset',
                title: i18n._("Signatures"),
                defaults: {
                    xtype: "displayfield",
                    labelWidth: 200
                },
                items: [{
                    fieldLabel: i18n._('Total Signatures Available'),
                    name: 'Total Signatures Available',
                    value: this.getRpcApp().getPatternsTotal()
                }, {
                    fieldLabel: i18n._('Total Signatures Logging'),
                    name: 'Total Signatures Logging',
                    value: this.getRpcApp().getPatternsLogged()
                }, {
                    fieldLabel: i18n._('Total Signatures Blocking'),
                    name: 'Total Signatures Blocking',
                    value: this.getRpcApp().getPatternsBlocked()
                }]
            }, {
                title: i18n._('Note'),
                html: Ext.String.format(i18n._("Caution and discretion is advised in configuring Application Control Lite at the risk of harmful false positives."))
            }]
        });
    },
    // Protocol list grid
    buildProtocolList: function() {
        this.gridProtocolList = Ext.create('Ung.grid.Panel',{
            settingsCmp: this,
            name: 'Signatures',
            helpSource: 'application_control_lite_signatures',
            title: i18n._("Signatures"),
            dataProperty: "patterns",
            recordJavaClass: "com.untangle.app.application_control_lite.ApplicationControlLitePattern",
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
                header: i18n._("Protocol"),
                width: 200,
                dataIndex: 'protocol',
                editor: {
                    xtype:'textfield',
                    emptyText: i18n._("[enter protocol]"),
                    allowBlank:false
                }
            }, {
                header: i18n._("Category"),
                width: 200,
                dataIndex: 'category',
                editor: {
                    xtype:'textfield',
                    emptyText: i18n._("[enter category]"),
                    allowBlank:false
                }
            }, {
                xtype:'checkcolumn',
                header: "<b>" + i18n._("Block") + "</b>",
                dataIndex: 'blocked',
                resizable: false,
                width:55,
                checkAll: {}
            },  {
                xtype:'checkcolumn',
                header: "<b>" + i18n._("Log") + "</b>",
                dataIndex: 'log',
                resizable: false,
                width:55,
                checkAll: {}
            }, {
                header: i18n._("Description"),
                width: 200,
                dataIndex: 'description',
                flex: 1,
                editor:{
                    xtype:'textfield',
                    emptyText: i18n._("[no description]")
                }
            }],
            rowEditorInputLines: [{
                xtype:'textfield',
                name: "Protocol",
                dataIndex: "protocol",
                fieldLabel: i18n._("Protocol"),
                emptyText: i18n._("[enter protocol]"),
                allowBlank: false,
                width: 400
            }, {
                xtype:'textfield',
                name: "Category",
                dataIndex: "category",
                fieldLabel: i18n._("Category"),
                emptyText: i18n._("[enter category]"),
                allowBlank: false,
                width: 400
            },  {
                xtype:'checkbox',
                name: "Block",
                dataIndex: "blocked",
                fieldLabel: i18n._("Block")
            }, {
                xtype:'checkbox',
                name: "Log",
                dataIndex: "log",
                fieldLabel: i18n._("Log")
            }, {
                xtype:'textarea',
                name: "Description",
                dataIndex: "description",
                fieldLabel: i18n._("Description"),
                emptyText: i18n._("[no description]"),
                width: 400,
                height: 60
            }, {  
                xtype:'textarea',
                name: "Signature",
                dataIndex: "definition",
                fieldLabel: i18n._("Signature"),
                emptyText: i18n._("[enter signature]"),
                allowBlank: false,
                width: 400,
                height: 60
            }]
        });
    },
    beforeSave: function(isApply, handler) {
        this.getSettings().patterns.list = this.gridProtocolList.getList();
        handler.call(this, isApply);
    }
});
//# sourceURL=application-control-lite-settings.js