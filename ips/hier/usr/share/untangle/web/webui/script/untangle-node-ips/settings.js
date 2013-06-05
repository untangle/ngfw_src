if (!Ung.hasResource["Ung.Ips"]) {
    Ung.hasResource["Ung.Ips"] = true;
    Ung.NodeWin.registerClassName('untangle-node-ips', 'Ung.Ips');

    Ext.define('Ung.Ips', {
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
                helpSource: 'status',
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
                                         main.getBrandingManager().getCompanyName())
                }]
            });
        },

        // Rules Panel
        buildRules: function() {
            this.panelRules = Ext.create('Ext.panel.Panel',{
                name: 'panelRules',
                helpSource: 'rules',
                parentId: this.getId(),
                title: this.i18n._('Rules'),
                autoScroll: true,
                border: false,
                layout: 'anchor',
                cls: 'ung-panel',
                items: [this.gridRules = Ext.create('Ung.EditorGrid', {
                    anchor: "100% 48%",
                    style: "margin-bottom:10px;",
                    name: 'Rules',
                    settingsCmp: this,
                    emptyRow: {
                        "category": this.i18n._("[no category]"),
                        "name": this.i18n._("[no name]"),
                        "text": this.i18n._("[no signature]"),
                        "sid": "0",
                        "live": true,
                        "log": true,
                        "description": this.i18n._("[no description]")
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
                        allowBlank: false,
                        width: 400
                    },
                    {
                        xtype:'textfield',
                        name: "Signature",
                        dataIndex: "text",
                        fieldLabel: this.i18n._("Signature"),
                        allowBlank: false,
                        width: 450
                    },
                    {
                        xtype:'textfield',
                        name: "Name",
                        dataIndex: "name",
                        fieldLabel: this.i18n._("Name"),
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
                        allowBlank: false,
                        width: 500
                    }]
                }),
                this.gridVariables = Ext.create('Ung.EditorGrid', {
                    anchor: "100% 48%",
                    name: 'Variables',
                    settingsCmp: this,
                    emptyRow: {
                        "variable": this.i18n._("[no name]"),
                        "definition": this.i18n._("[no definition]"),
                        "description": this.i18n._("[no description]")
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
                            allowBlank: false
                        }
                    }, {
                        header: this.i18n._("description"),
                        width: 300,
                        dataIndex: 'description',
                        flex:1,
                        editor: {
                            xtype:'textfield',
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
                        allowBlank: false,
                        width: 300
                    },
                    {
                        xtype:'textfield',
                        name: "Pass",
                        dataIndex: "definition",
                        fieldLabel: this.i18n._("Pass"),
                        allowBlank: false,
                        width: 400
                    },
                    {
                        xtype:'textfield',
                        name: "Description",
                        dataIndex: "description",
                        fieldLabel: this.i18n._("Description"),
                        allowBlank: false,
                        width: 400
                    }]
                })
            ]});
        },
        // Event Log
        buildEventLog: function() {
            this.gridEventLog = Ext.create('Ung.GridEventLog',{
                settingsCmp: this,
                fields: [{
                    name: 'time_stamp',
                    sortType: Ung.SortTypes.asTimestamp
                }, {
                    name: 'blocked',
                    mapping: 'ips_blocked'
                }, {
                    name: 'name',
                    mapping: 'ips_ruleid'
                }, {
                    name: 'description',
                    mapping: 'ips_description',
                    type: 'string'
                }, {
                    name: 'client',
                    mapping: 'c_client_addr'
                }, {
                    name: 'server',
                    mapping: 'c_server_addr'
                }, {
                    name: 'server',
                    mapping: 'c_server_addr'
                }, {
                    name: 'uid'
                }],
                columns: [{
                    header: this.i18n._("Timestamp"),
                    width: Ung.Util.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'time_stamp',
                    renderer: function(value) {
                        return i18n.timestampFormat(value);
                    }
                }, {
                    header: this.i18n._("Client"),
                    width: Ung.Util.ipFieldWidth,
                    sortable: true,
                    dataIndex: 'client'
                }, {
                    header: this.i18n._("Username"),
                    width: Ung.Util.usernameFieldWidth,
                    sortable: true,
                    dataIndex: 'uid'
                }, {
                    header: this.i18n._("Blocked"),
                    width: Ung.Util.booleanFieldWidth,
                    sortable: true,
                    dataIndex: 'blocked'
                }, {
                    id: 'ruleName',
                    header: this.i18n._('Rule Id'),
                    width: 60,
                    sortable: true,
                    dataIndex: 'name'
                }, {
                    id: 'ruleDescription',
                    header: this.i18n._('Rule Description'),
                    width: 150,
                    sortable: true,
                    flex:1,
                    dataIndex: 'description'
                }, {
                    header: this.i18n._("Server"),
                    width: Ung.Util.ipFieldWidth,
                    sortable: true,
                    dataIndex: 'server'
                }]
            });
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
}
//@ sourceURL=ips-settings.js
