Ext.define('Webui.untangle-node-ips.settings', {
    extend:'Ung.NodeWin',
    panelStatus: null,
    panelRules: null,
    gridRules: null,
    gridVariables: null,
    gridEventLog: null,
    statistics: null,
    // called when the component is rendered
    initComponent: function() {
        this.statistics = this.getRpcNode().getStatistics();
        this.buildStatus();
        this.buildRules();
        this.buildEventLog();
        // builds the tab panel with the tabs
        this.buildTabPanel([this.panelStatus, this.panelRules, this.gridEventLog]);
        this.callParent(arguments);
    },
    // Status Panel
    buildStatus: function() {
        this.panelStatus = Ext.create('Ext.panel.Panel',{
            name: 'Status',
            helpSource: 'intrusion_prevention_status',
            parentId: this.getId(),
            title: this.i18n._('Status'),
            cls: 'ung-panel',
            autoScroll: true,
            defaults: {
                xtype: 'fieldset',
                buttonAlign: 'left'
            },
            items: [{
                title: this.i18n._('Statistics'),
                labelWidth: 230,
                defaults: {
                    xtype: "textfield",
                    disabled: true
                },
                items: [{
                    fieldLabel: this.i18n._('Total Signatures Available'),
                    name: 'Total Signatures Available',
                    labelWidth:200,
                    labelAlign:'left',
                    value: this.statistics.totalAvailable
                }, {
                    fieldLabel: this.i18n._('Total Signatures Logging'),
                    name: 'Total Signatures Logging',
                    labelWidth:200,
                    labelAlign:'left',
                    value: this.statistics.totalLogging
                }, {
                    fieldLabel: this.i18n._('Total Signatures Blocking'),
                    name: 'Total Signatures Blocking',
                    labelWidth:200,
                    labelAlign:'left',
                    value: this.statistics.totalBlocking
                }]
            }, {
                title: this.i18n._('Note'),
                cls: 'description',
                html: Ext.String.format(this.i18n._("{0} continues to maintain the default signature settings through automatic updates. You are free to modify and add signatures, however it is not required."),
                        rpc.companyName)
            }]
        });
    },

    // Rules Panel
    buildRules: function() {
        this.panelRules = Ext.create('Ext.panel.Panel',{
            name: 'panelRules',
            helpSource: 'intrusion_prevention_rules',
            parentId: this.getId(),
            title: this.i18n._('Rules'),
            border: false,
            layout: { type: 'vbox', align: 'stretch' },
            cls: 'ung-panel',
            items: [this.gridRules = Ext.create('Ung.EditorGrid', {
                flex: 1,
                style: "margin-bottom:10px;",
                name: 'Rules',
                settingsCmp: this,
                emptyRow: {
                    "category": "",
                    "name": "",
                    "text": "",
                    "sid": "0",
                    "live": true,
                    "log": true,
                    "description": ""
                },
                title: this.i18n._("Rules"),
                recordJavaClass: "com.untangle.node.ips.IpsRule",
                dataProperty: 'rules',
                paginated: false,
                fields: [{
                    name: 'id'
                }, {
                    name: 'text'
                }, {
                    name: 'sid'
                }, {
                    name: 'name'
                }, {
                    name: 'category',
                    type: 'string'
                }, {
                    name: 'classification'
                }, {
                    name: 'URL'
                }, {
                    name: 'live'
                }, {
                    name: 'log'
                }, {
                    name: 'description',
                    type: 'string'
                }],
                columns: [{
                    header: this.i18n._("category"),
                    width: 180,
                    dataIndex: 'category',
                    editor: {
                        xtype:'texfield',
                        emptyText: this.i18n._("[enter category]"),
                        allowBlank: false
                    }
                },
                {
                    xtype:'checkcolumn',
                    header: this.i18n._("block"),
                    dataIndex: 'live',
                    resizable: false,
                    width:55
                },
                {
                    xtype:'checkcolumn',
                    header: this.i18n._("log"),
                    dataIndex: 'log',
                    resizable: false,
                    width:55
                },
                {
                    header: this.i18n._("description"),
                    width: 200,
                    dataIndex: 'description',
                    flex:1,
                    editor: null,
                    renderer: function(value, metadata, record) {
                        var description = "";
                        if (record.data.classification != null)
                        {
                            description += record.data.classification + " ";
                        }
                        if (record.data.description != null) {
                            description += "(" + record.data.description + ")";
                        }
                        return description;
                    }
                }, {
                    header: this.i18n._("id"),
                    width: 70,
                    dataIndex: 'sid',
                    editor: null
                }, {
                    header: this.i18n._("info"),
                    width: 70,
                    dataIndex: 'URL',
                    editor: null,
                    sortable: false,
                    renderer: function(value) {
                        return (value == null || value.length == 0) ? "no info": "<a href='" + value + "' target='_blank'>info</a>";
                    }
                }],
                sortField: 'category',
                columnsDefaultSortable: true,
                rowEditorInputLines: [
                {
                    xtype:'textfield',
                    name: "Category",
                    dataIndex: "category",
                    fieldLabel: this.i18n._("Category"),
                    emptyText: this.i18n._("[enter category]"),
                    allowBlank: false,
                    width: 400
                },
                {
                    xtype:'textfield',
                    name: "Signature",
                    dataIndex: "text",
                    fieldLabel: this.i18n._("Signature"),
                    emptyText: this.i18n._("[enter signature]"),
                    allowBlank: false,
                    width: 450
                },
                {
                    xtype:'textfield',
                    name: "Name",
                    dataIndex: "name",
                    fieldLabel: this.i18n._("Name"),
                    emptyText: this.i18n._("[enter name]"),
                    allowBlank: false,
                    width: 300
                },
                {
                    xtype:'textfield',
                    name: "SID",
                    dataIndex: "sid",
                    fieldLabel: this.i18n._("SID"),
                    allowBlank: false,
                    width: 150
                },
                {
                    xtype:'checkbox',
                    name: "Block",
                    dataIndex: "live",
                    fieldLabel: this.i18n._("Block")
                },
                {
                    xtype:'checkbox',
                    name: "Log",
                    dataIndex: "log",
                    fieldLabel: this.i18n._("Log")
                },
                {
                    xtype:'textfield',
                    name: "Description",
                    dataIndex: "description",
                    fieldLabel: this.i18n._("Description"),
                    emptyText: this.i18n._("[enter description]"),
                    allowBlank: false,
                    width: 500
                }]
            }),
            this.gridVariables = Ext.create('Ung.EditorGrid', {
                flex: 1,
                name: 'Variables',
                settingsCmp: this,
                emptyRow: {
                    "variable": "",
                    "definition": "",
                    "description": ""
                },
                title: this.i18n._("Variables"),
                recordJavaClass: "com.untangle.node.ips.IpsVariable",
                dataProperty: 'variables',
                fields: [{
                    name: 'id'
                }, {
                    name: 'variable',
                    type: 'string'
                }, {
                    name: 'definition',
                    type: 'string'
                }, {
                    name: 'description',
                    type: 'string'
                }],
                columns: [{
                    header: this.i18n._("name"),
                    width: 170,
                    dataIndex: 'variable',
                    editor: {
                        xtype:'textfield',
                        emptyText: this.i18n._("[enter name]"),
                        allowBlank: false
                        }
                    },
                    {
                    id: 'definition',
                    header: this.i18n._("pass"),
                    width: 300,
                    dataIndex: 'definition',
                    editor: {
                        xtype:'textfield',
                        emptyText: this.i18n._("[enter definition]"),
                        allowBlank: false
                    }
                }, {
                    header: this.i18n._("description"),
                    width: 300,
                    dataIndex: 'description',
                    flex:1,
                    editor: {
                        xtype:'textfield',
                        emptyText: this.i18n._("[enter description]"),
                        allowBlank: false
                    }
                }],
                sortField: 'variable',
                columnsDefaultSortable: true,
                rowEditorInputLines: [{
                    xtype:'textfield',
                    name: "Name",
                    dataIndex: "variable",
                    fieldLabel: this.i18n._("Name"),
                    emptyText: this.i18n._("[enter name]"),
                    allowBlank: false,
                    width: 300
                },
                {
                    xtype:'textfield',
                    name: "Pass",
                    dataIndex: "definition",
                    fieldLabel: this.i18n._("Pass"),
                    emptyText: this.i18n._("[enter definition]"),
                    allowBlank: false,
                    width: 400
                },
                {
                    xtype:'textfield',
                    name: "Description",
                    dataIndex: "description",
                    fieldLabel: this.i18n._("Description"),
                    emptyText: this.i18n._("[enter description]"),
                    allowBlank: false,
                    width: 400
                }]
            })
        ]});
    },
    // Event Log
    buildEventLog: function() {
        this.gridEventLog = Ung.CustomEventLog.buildSessionEventLog (this, 'EventLog', i18n._('Event Log'),
            'intrusion_prevention_event_log',
            ['time_stamp','username','c_client_addr','s_server_addr','ips_blocked','ips_ruleid','ips_description'],
            this.getRpcNode().getEventQueries);
    },
    beforeSave: function(isApply,handler) {
        this.settings.rules.list = this.gridRules.getPageList();
        this.settings.variables.list = this.gridVariables.getPageList();
        handler.call(this, isApply);
    },
    afterSave: function() {
        this.statistics = this.getRpcNode().getStatistics();
    }
});
//# sourceURL=ips-settings.js