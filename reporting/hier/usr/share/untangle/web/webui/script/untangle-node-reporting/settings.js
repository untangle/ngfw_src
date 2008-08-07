if (!Ung.hasResource["Ung.Reporting"]) {
    Ung.hasResource["Ung.Reporting"] = true;
    Ung.Settings.registerClassName('untangle-node-reporting', 'Ung.Reporting');

    Ung.Reporting = Ext.extend(Ung.Settings, {
        panelStatus : null,
        panelGeneration : null,
        gridRecipients : null,
        gridIpMap : null,
        // called when the component is rendered
        onRender : function(container, position) {
            // call superclass renderer first
            Ung.Reporting.superclass.onRender.call(this, container, position);
            // builds the 3 tabs
            this.buildStatus();
            this.buildGeneration();
            this.buildIpMap();
            // builds the tab panel with the tabs
            this.buildTabPanel([this.panelStatus/*, this.panelGeneration, this.gridIpMap*/]);
        },
        // Status Panel
        buildStatus : function() {
            this.panelStatus = new Ext.Panel({
                title : this.i18n._('Status'),
                name : 'Status',
                layout : "form",
                autoScroll : true,
                bodyStyle : 'padding:5px 5px 0px 5px;',
                items: [{
                    title : this.i18n._('Status'),
                    xtype : 'fieldset',
                    autoHeight : true,
                    items : [{
                        buttonAlign : 'center',
                        footer : false,
                        border : false,
                        buttons: [{
                            xtype : 'button',
                            text : this.i18n._('View Reports'),
                            name : 'View Reports',
                            iconCls : 'actionIcon',
                            handler : function() {
                                rpc.adminManager.generateAuthNonce(function (result, exception) {
                                    if(exception) { 
                                        Ext.MessageBox.alert(i18n._("Failed"),exception.message); 
                                        return;
                                    }
                                    var viewReportsUrl = "../reports/?" + result;
                                    window.open(viewReportsUrl);
                                }.createDelegate(this));
                            }.createDelegate(this)
                        }]
                    }, {
                        buttonAlign : 'center',
                        footer : false,
                        border : false,
                        buttons: [{
                            xtype : 'button',
                            text : this.i18n._('Archived Reports'),
                            name : 'Archived Reports',
                            iconCls : 'actionIcon',
                            handler : function() {
                                rpc.adminManager.generateAuthNonce(function (result, exception) {
                                    if(exception) { 
                                        Ext.MessageBox.alert(i18n._("Failed"),exception.message); 
                                        return;
                                    }
                                    var viewReportsUrl = "../reports/archive?" + result;
                                    window.open(viewReportsUrl);
                                }.createDelegate(this));
                            }.createDelegate(this)
                        }]
                    }]
                }]
            });
        },
        // Generation panel
        buildGeneration : function() {
        },
        // IP Map grid
        buildIpMap : function() {
//            // enable is a check column
//            var enableColumn = new Ext.grid.CheckColumn({
//                header : this.i18n._("enable"),
//                dataIndex : 'live',
//                fixed : true
//            });
//
//            var deviderData = [[5, 5 + ' ' + this.i18n._("users")], [25, 25 + ' ' + this.i18n._("users")],
//                    [40, 50 + ' ' + this.i18n._("users")], [75, 100 + ' ' + this.i18n._("users")], [-1, this.i18n._("unlimited")]];
//
//            this.gridExceptions = new Ung.EditorGrid({
//                settingsCmp : this,
//                name : 'Exceptions',
//                // the total records is set from the base settings
//                // shieldNodeRulesLength field
//                totalRecords : this.getBaseSettings().shieldNodeRulesLength,
//                emptyRow : {
//                    "live" : true,
//                    "address" : "1.2.3.4",
//                    "divider" : 5,
//                    "description" : i18n._("[no description]")
//                },
//                title : this.i18n._("Exceptions"),
//                // the column is autoexpanded if the grid width permits
//                autoExpandColumn : 'description',
//                recordJavaClass : "com.untangle.node.shield.ShieldNodeRule",
//                // this is the function used by Ung.RpcProxy to retrive data
//                // from the server
//                proxyRpcFn : this.getRpcNode().getShieldNodeRules,
//
//                // the list of fields
//                fields : [{
//                    name : 'id'
//                }, {
//                    name : 'live'
//                }, {
//                    name : 'address'
//                }, {
//                    name : 'divider'
//                },
//                        // this field is internationalized so a converter was
//                        // added
//                        {
//                            name : 'description',
//                            convert : function(v) {
//                                return this.i18n._(v)
//                            }.createDelegate(this)
//                        },],
//                // the list of columns for the column model
//                columns : [{
//                    id : 'id',
//                    dataIndex : 'id',
//                    hidden : true
//                }, enableColumn, {
//                    id : 'address',
//                    header : this.i18n._("address"),
//                    width : 200,
//                    dataIndex : 'address',
//                    // this is a simple text editor
//                    editor : new Ext.form.TextField({
//                        allowBlank : false,
//                        vtype : 'ipAddress'
//                    })
//                }, {
//                    id : 'divider',
//                    header : this.i18n._("user") + "<br>" + this.i18n._("count"),
//                    width : 100,
//                    dataIndex : 'divider',
//                    editor : new Ext.form.ComboBox({
//                        store : new Ext.data.SimpleStore({
//                            fields : ['dividerValue', 'dividerName'],
//                            data : deviderData
//                        }),
//                        displayField : 'dividerName',
//                        valueField : 'dividerValue',
//                        typeAhead : true,
//                        mode : 'local',
//                        triggerAction : 'all',
//                        listClass : 'x-combo-list-small',
//                        selectOnFocus : true
//                    }),
//                    renderer : function(value) {
//                        for (var i = 0; i < deviderData.length; i++) {
//                            if (deviderData[i][0] == value) {
//                                return deviderData[i][1];
//                            }
//                        }
//                        return value;
//                    }
//                }, {
//                    id : 'description',
//                    header : this.i18n._("description"),
//                    width : 200,
//                    dataIndex : 'description',
//                    editor : new Ext.form.TextField({
//                        allowBlank : false
//                    })
//                }],
//                // sortField: 'address',
//                columnsDefaultSortable : true,
//                plugins : [enableColumn],
//                // the row input lines used by the row editor window
//                rowEditorInputLines : [new Ext.form.Checkbox({
//                    name : "Enable",
//                    dataIndex : "live",
//                    fieldLabel : this.i18n._("Enable")
//                }), new Ext.form.TextField({
//                    name : "Address",
//                    dataIndex : "address",
//                    fieldLabel : this.i18n._("Address"),
//                    allowBlank : false,
//                    width : 200,
//                    vtype : 'ipAddress'
//                }), new Ext.form.ComboBox({
//                    name : "User Count",
//                    dataIndex : "divider",
//                    fieldLabel : this.i18n._("User Count"),
//                    store : new Ext.data.SimpleStore({
//                        fields : ['dividerValue', 'dividerName'],
//                        data : deviderData
//                    }),
//                    displayField : 'dividerName',
//                    valueField : 'dividerValue',
//                    typeAhead : true,
//                    mode : 'local',
//                    triggerAction : 'all',
//                    listClass : 'x-combo-list-small',
//                    selectOnFocus : true
//                }), new Ext.form.TextArea({
//                    name : "Description",
//                    dataIndex : "description",
//                    fieldLabel : this.i18n._("Description"),
//                    width : 200,
//                    height : 60
//                })]
//            });
        },
        // save function
        save : function() {
            Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
            this.getRpcNode().updateAll(function(result, exception) {
                Ext.MessageBox.hide();
                if (exception) {
                    Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                    return;
                }
                // exit settings screen
                this.cancelAction();
            }.createDelegate(this), this.gridExceptions.getSaveList());
        }
    });
}