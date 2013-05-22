if (!Ung.hasResource["Ung.Shield"]) {
    Ung.hasResource["Ung.Shield"] = true;
    Ung.NodeWin.registerClassName('untangle-node-shield', 'Ung.Shield');

    Ext.define('Ung.Shield', {
        extend: 'Ung.NodeWin',
        gridExceptions: null,
        gridEventLog: null,
        initComponent: function(container, position) {
            // builds the 3 tabs
            this.buildStatus();
            this.buildExceptions();
            this.buildEventLog();
            // builds the tab panel with the tabs
            this.buildTabPanel([this.statusPanel, this.gridExceptions, this.gridEventLog]);
            this.callParent(arguments);
        },
        // Status Panel
        buildStatus: function() {
            this.statusPanel = Ext.create('Ext.panel.Panel',{
                title: this.i18n._('Status'),
                name: 'Status',
                helpSource: 'status',
                autoScroll: true,
                cls: 'ung-panel',
                items: [{
                    xtype: 'fieldset',
                    title: this.i18n._('Statistics'),
                    items: [{
                        xtype: 'textfield',
                        fieldLabel: this.i18n._('Status'),
                        name: 'Statistics Status',
                        allowBlank: false,
                        style: 'color:green',
                        value: this.node.isRunning() ? this.i18n._('active'): this.i18n._('inactive'),
                        /*disabled: true,*/
                        readOnly: true,
                        cls:'attack-blocker-style-1'

                    }]
                }, {
                    xtype: 'fieldset',
                    title: this.i18n._('Note'),
                    cls: 'description',
                    html: this.i18n
                        ._('Shield is a heuristic based intrusion prevention and requires no configuration. Users can modify the treatment of certain IP and/or networks on the exception tab.')
                }]
            });
        },
        // Exceptions grid
        buildExceptions: function() {
            var deviderData = [[5, 5 + ' ' + this.i18n._("users")], [25, 25 + ' ' + this.i18n._("users")],
                               [40, 50 + ' ' + this.i18n._("users")], [75, 100 + ' ' + this.i18n._("users")], [-1, this.i18n._("unlimited")]];

            this.gridExceptions = Ext.create('Ung.EditorGrid',{
                settingsCmp: this,
                name: 'Exceptions',
                helpSource: 'exceptions',
                emptyRow: {
                    "enabled": true,
                    "address": "1.2.3.4",
                    "divider": 5,
                    "description": i18n._("[no description]")
                },
                title: this.i18n._("Exceptions"),
                // the column is autoexpanded if the grid width permits
                recordJavaClass: "com.untangle.node.shield.ShieldRule",
                dataProperty:'rules',
                // the list of fields
                fields: [{
                    name: 'id'
                }, {
                    name: 'enabled'
                }, {
                    name: 'address'
                }, {
                    name: 'divider'
                },
                         {
                             name: 'description',
                             type: 'string'
                         }],
                // the list of columns for the column model
                columns: [{
                    xtype: 'checkcolumn',
                    header: this.i18n._("Enable"),
                    dataIndex: 'enabled',
                    width: 55,
                    resizable: false
                }, {
                    header: this.i18n._("Address"),
                    width: 200,
                    dataIndex: 'address',
                    // this is a simple text editor
                    editor: {
                        xtype:'textfield',
                        allowBlank: false,
                        vtype: 'ipMatcher'
                    }
                }, {
                    header: this.i18n._("User Count"),
                    width: 100,
                    dataIndex: 'divider',
                    editor: Ext.create('Ext.form.ComboBox',{
                        store: Ext.create('Ext.data.ArrayStore',{
                            fields: ['dividerValue', 'dividerName'],
                            data: deviderData
                        }),
                        displayField: 'dividerName',
                        valueField: 'dividerValue',
                        typeAhead: true,
                        queryMode: 'local',
                        selectOnFocus: true
                    }),
                    renderer: function(value) {
                        for (var i = 0; i < deviderData.length; i++) {
                            if (deviderData[i][0] == value) {
                                return deviderData[i][1];
                            }
                        }
                        return value;
                    }
                }, {
                    header: this.i18n._("description"),
                    width: 200,
                    flex: 1,
                    dataIndex: 'description',
                    editor: {
                        xtype:'textfield',
                        allowBlank:false
                    }
                }],
                sortField: 'address',
                columnsDefaultSortable: true,
                // the row input lines used by the row editor window
                rowEditorInputLines: [{
                    xtype:'checkbox',
                    name: "Enable",
                    dataIndex: "enabled",
                    fieldLabel: this.i18n._("Enable")
                }, {
                    xtype:'textfield',
                    name: "Address",
                    dataIndex: "address",
                    fieldLabel: this.i18n._("Address"),
                    allowBlank: false,
                    width: 400,
                    vtype: 'ipMatcher'
                }, {
                    xtype:'combo',
                    name: "User Count",
                    dataIndex: "divider",
                    fieldLabel: this.i18n._("User Count"),
                    store: Ext.create('Ext.data.ArrayStore',{
                        fields: ['dividerValue', 'dividerName'],
                        data: deviderData
                    }),
                    displayField: 'dividerName',
                    valueField: 'dividerValue',
                    typeAhead: true,
                    queryMode: 'local',
                    selectOnFocus: true
                }, {
                    xtype:'textarea',
                    name: "Description",
                    dataIndex: "description",
                    fieldLabel: this.i18n._("Description"),
                    width: 400,
                    height: 60
                }]
            });
        },
        // Event Log
        buildEventLog: function() {
            this.gridEventLog = Ext.create('Ung.GridEventLog',{
                settingsCmp: this,
                fields: [{
                    name: 'id'
                }, {
                    name: 'time_stamp',
                    sortType: Ung.SortTypes.asTimestamp
                }, {
                    name: 'shield_blocked'
                }, {
                    name: 'username'
                }, {
                    name: 'c_client_addr'
                }, {
                    name: 'c_client_port'
                }, {
                    name: 's_server_addr'
                }, {
                    name: 's_server_port'
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
                    dataIndex: 'c_client_addr'
                }, {
                    header: this.i18n._("Client Port"),
                    width: Ung.Util.portFieldWidth,
                    sortable: true,
                    dataIndex: 'c_client_port'
                }, {
                    header: this.i18n._("Username"),
                    width: Ung.Util.usernameFieldWidth,
                    sortable: true,
                    dataIndex: 'username'
                }, {
                    header: this.i18n._("Blocked"),
                    width: Ung.Util.booleanFieldWidth,
                    sortable: true,
                    dataIndex: 'shield_blocked',
                    renderer: function(value) {
                        if (value == "")
                            return "false";
                        else
                            return value;
                    }
                }, {
                    header: this.i18n._("Server") ,
                    width: Ung.Util.ipFieldWidth + 40, // +40 for column header
                    sortable: true,
                    dataIndex: 's_server_addr'
                }, {
                    header: this.i18n._("Server Port"),
                    width: Ung.Util.portFieldWidth + 40, // +40 for column header
                    sortable: true,
                    dataIndex: 's_server_port'
                }]
            });
        },
        beforeSave: function(isApply, handler) {
            this.gridExceptions.getList(Ext.bind(function(saveList) {
                this.settings.rules = saveList;
                handler.call(this, isApply);
            }, this));
        }
        
    });
}
//@ sourceURL=shield-settings.js
