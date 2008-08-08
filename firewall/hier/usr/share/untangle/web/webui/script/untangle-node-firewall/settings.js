if (!Ung.hasResource["Ung.Firewall"]) {
    Ung.hasResource["Ung.Firewall"] = true;
    Ung.Settings.registerClassName('untangle-node-firewall', 'Ung.Firewall');

    Ung.Firewall = Ext.extend(Ung.Settings, {
        gridRules : null,
        gridEventLog : null,
        // called when the component is rendered
        onRender : function(container, position) {
            // call superclass renderer first
            Ung.Firewall.superclass.onRender.call(this, container, position);
            // builds the tabs
            this.buildRules();
            this.buildEventLog();
            // builds the tab panel with the tabs
            this.buildTabPanel([this.panelRules, this.gridEventLog]);
        },
        // Rules Panel
        buildRules : function() {
            // enable is a check column
            var liveColumn = new Ext.grid.CheckColumn({
                header : this.i18n._("Enable"),
                dataIndex : 'live',
                fixed : true
            });

            var actionData = [[false, this.i18n._('Pass')],[true, this.i18n._('Block')]];

            this.panelRules = new Ext.Panel({
                name : 'panelRules',
                // private fields
                gridRulesList : null,
                parentId : this.getId(),
                title : this.i18n._('Rules'),
                layout : 'form',
                autoScroll : true,
                border : false,
                bodyStyle : 'padding:5px 5px 0px 5px;',
                items : [this.gridRules = new Ung.EditorGrid({
                        name : 'Rules',
                        settingsCmp : this,
                        height : 500,
                        totalRecords : this.getBaseSettings().firewallRulesLength,
                        paginated : false,
                        hasReorder : true,
                        emptyRow : {
                            "live" : true,
                            "action" : this.i18n._('Block'),
                            "log" : false,
                            "protocol" : "TCP & UDP",
                            "srcIntf" : "any",
                            "dstIntf" : "any",
                            "srcAddress" : "1.2.3.4",
                            "dstAddress" : "1.2.3.4",
                            "srcPort" : "any",
                            "dstPort" : "2-5",
                            "name" : this.i18n._("[no name]"),
                            "category" : this.i18n._("[no category]"),
                            "description" : this.i18n._("[no description]"),
                            "javaClass" : "com.untangle.node.firewall.FirewallRule"
                        },
                        title : this.i18n._("Rules"),
                        recordJavaClass : "com.untangle.node.firewall.FirewallRule",
                        proxyRpcFn : this.getRpcNode().getFirewallRuleList,
                        fields : [{
                            name : 'id'
                        }, {
                            name : 'live'
                        }, {
                            name : 'action'
                        }, {
                            name : 'log'
                        }, {
                            name : 'protocol'
                        }, {
                            name : 'srcIntf'
                        }, {
                            name : 'dstIntf'
                        }, {
                            name : 'srcAddress'
                        }, {
                            name : 'dstAddress'
                        }, {
                            name : 'srcPort'
                        }, {
                            name : 'dstPort'
                        }, {
                            name : 'name'
                        }, {
                            name : 'category'
                        }, {
                            name : 'description'
                        }, {
                            name : 'javaClass'
                        }],
                        columns : [liveColumn, {
                            id : 'description',
                            header : this.i18n._("Description"),
                            width : 200,
                            dataIndex : 'description'
                        }],
                        sortField : 'description',
                        columnsDefaultSortable : true,
                        autoExpandColumn : 'description',
                        plugins : [liveColumn],
                        rowEditorInputLines : [new Ext.form.Checkbox({
                            name : "Enable Rule",
                            dataIndex: "live",
                            fieldLabel : this.i18n._("Enable Rule")
                        }), new Ext.form.ComboBox({
                            name : "Action",
                            dataIndex: "action",
                            fieldLabel : this.i18n._("Action"),
	                        store : new Ext.data.SimpleStore({
	                            fields : ['key', 'name'],
	                            data : actionData
	                        }),
	                        displayField : 'name',
	                        valueField : 'key',
                            forceSelection : true,
	                        typeAhead : true,
	                        mode : 'local',
	                        triggerAction : 'all',
	                        listClass : 'x-combo-list-small',
	                        selectOnFocus : true
                        }), new Ext.form.Checkbox({
                            name : "Log",
                            dataIndex: "log",
                            fieldLabel : this.i18n._("Log")
                        }), new Ung.Util.ProtocolCombo({
                            name : "Traffic Type",
                            dataIndex: "protocol",
                            fieldLabel : this.i18n._("Traffic Type"),
                            width : 100
                        }), new Ung.Util.InterfaceCombo({
                            name : "Source Interface",
                            dataIndex: "srcIntf",
                            fieldLabel : this.i18n._("Source Interface"),
                            width : 150
                        }), new Ung.Util.InterfaceCombo({
                            name : "Destination Interface",
                            dataIndex: "dstIntf",
                            fieldLabel : this.i18n._("Destination Interface"),
                            width : 150
                        }), new Ext.form.TextField({
                            name : "Source Address",
                            dataIndex: "srcAddress",
                            fieldLabel : this.i18n._("Source Address"),
                            allowBlank : false,
                            width : 150
                        }), new Ext.form.TextField({
                            name : "Destination Address",
                            dataIndex: "dstAddress",
                            fieldLabel : this.i18n._("Destination Address"),
                            allowBlank : false,
                            width : 150
                        }), new Ext.form.TextField({
                            name : "Source Port",
                            dataIndex: "srcPort",
                            fieldLabel : this.i18n._("Source Port"),
                            width : 150,
                            allowBlank : false
                        }), new Ext.form.TextField({
                            name : "Destination Port",
                            dataIndex: "dstPort",
                            fieldLabel : this.i18n._("Destination Port"),
                            allowBlank : false,
                            width : 150
                        }), new Ext.form.TextField({
                            name : "Category",
                            dataIndex: "category",
                            fieldLabel : this.i18n._("Category"),
                            width : 150
                        }), new Ext.form.TextField({
                            name : "Description",
                            dataIndex: "description",
                            fieldLabel : this.i18n._("Description"),
                            width : 400
                        })]
                    }),{
                        xtype : 'fieldset',
                        autoHeight : true,
                        title : this.i18n._('Default Action'),
                        items : [{
	                        xtype : 'radio',
                            boxLabel : this.i18n._('Block'), 
                            hideLabel : true,
	                        name : 'isDefaultAccept',
	                        checked : !this.getBaseSettings().isDefaultAccept,
	                        listeners : {
	                            "check" : {
	                                fn : function(elem, checked) {
	                                    this.getBaseSettings().isDefaultAccept = !checked;
	                                }.createDelegate(this)
	                            }
	                        }
	                    },{
	                        xtype : 'radio',
	                        boxLabel : this.i18n._('Pass'), 
	                        hideLabel : true,
	                        name : 'isDefaultAccept',
	                        checked : this.getBaseSettings().isDefaultAccept,
	                        listeners : {
	                            "check" : {
	                                fn : function(elem, checked) {
	                                    this.getBaseSettings().isDefaultAccept = checked;
	                                }.createDelegate(this)
	                            }
	                        }
                        }]
                    }
                ]
            });
        },
        // Event Log
        buildEventLog : function() {
            this.gridEventLog = new Ung.GridEventLog({
                settingsCmp : this,
                fields : [{
                    name : 'id'
                }, {
                    name : 'timeStamp'
                }, {
                    name : 'ruleIndex'
                }, {
                    name : 'pipelineEndpoints'
                }, {
                    name : 'wasBlocked'
                }],
                columns : [{
                    header : i18n._("timestamp"),
                    width : 150,
                    sortable : true,
                    dataIndex : 'timeStamp',
                    renderer : function(value) {
                        return i18n.timestampFormat(value);
                    }
                }, {
                    header : i18n._("action"),
                    width : 55,
                    sortable : true,
                    dataIndex : 'wasBlocked',
                    renderer : function(value) {
                        switch (value) {
                            case 1 : // BLOCKED
                                return this.i18n._("blocked");
                            default :
                            case 0 : // PASSED
                                return this.i18n._("passed");
                        }
                    }.createDelegate(this)
                }, {
                    header : i18n._("client"),
                    width : 165,
                    sortable : true,
                    dataIndex : 'pipelineEndpoints',
                    renderer : function(value) {
                        return value === null ? "" : value.CClientAddr.hostAddress + ":" + value.CClientPort;
                    }
                }, {
                    header : i18n.sprintf(this.i18n._('reason for%saction'),'<br>'),
                    width : 150,
                    sortable : true,
                    dataIndex : 'ruleIndex',
                    renderer : function(value, metadata, record) {
                           return this.i18n._("rule #") + value;
					}
                }, {
                    header : i18n._("server"),
                    width : 165,
                    sortable : true,
                    dataIndex : 'pipelineEndpoints',
                    renderer : function(value) {
                        return value === null ? "" : value.SServerAddr.hostAddress + ":" + value.SServerPort;
                    }
                }]
                
            });
        },
        validateServer : function() {
            // ipMaddr list must be validated server side
            var passedAddresses = this.gridRules ? this.gridRules.getFullSaveList() : null;
            if (passedAddresses != null) {
                var srcAddrList = [];
                var dstAddrList = [];
                var srcPortList = [];
                var dstPortList = [];
                for (var i = 0; i < passedAddresses.length; i++) {
                    srcAddrList.push(passedAddresses[i]["srcAddress"]);
                    dstAddrList.push(passedAddresses[i]["dstAddress"]);
                    srcPortList.push(passedAddresses[i]["srcPort"]);
                    dstPortList.push(passedAddresses[i]["dstPort"]);
                }
                var validateData = {
                    map : {},
                    javaClass : "java.util.HashMap"
                }; 
                if (srcAddrList.length > 0) {
                    validateData.map["SRC_ADDR"] = {"javaClass" : "java.util.ArrayList", list : srcAddrList};
                }
                if (dstAddrList.length > 0) {
                    validateData.map["DST_ADDR"] = {"javaClass" : "java.util.ArrayList", list : dstAddrList};
                }
                if (srcPortList.length > 0) {
                    validateData.map["SRC_PORT"] = {"javaClass" : "java.util.ArrayList", list : srcPortList};
                }
                if (dstPortList.length > 0) {
                    validateData.map["DST_PORT"] = {"javaClass" : "java.util.ArrayList", list : dstPortList};
                }
                if (Ung.Util.hasData(validateData.map)) {
                    try {
                        var result = this.getValidator().validate(validateData);
                        if (!result.valid) {
                            var errorMsg = "";
                            switch (result.errorCode) {
                                case 'INVALID_SRC_ADDR' : 
                                    errorMsg = this.i18n._("Invalid address specified for Source Address") + ": " + result.cause;
                                break;
                                case 'INVALID_DST_ADDR' : 
                                    errorMsg = this.i18n._("Invalid address specified for Destination Address") + ": " + result.cause;
                                break;
                                case 'INVALID_SRC_PORT' : 
                                    errorMsg = this.i18n._("Invalid port specified for Source Port") + ": " + result.cause;
                                break;
                                case 'INVALID_DST_PORT' : 
                                    errorMsg = this.i18n._("Invalid port specified for Destination Port") + ": " + result.cause;
                                break;
                                default :
                                    errorMsg = this.i18n._(result.errorCode) + ": " + result.cause;
                            }
                            Ext.MessageBox.alert(this.i18n._("Validation failed"), errorMsg);
                            return false;
                        }
                    } catch (e) {
                        Ext.MessageBox.alert(i18n._("Failed"), e.message);
                        return false;
                    }
                }
            }
            return true;
        },
        // save function
        save : function() {
            if (this.validate()) {
                Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
                this.getRpcNode().updateAll(function(result, exception) {
                    Ext.MessageBox.hide();
                    if (exception) {
                        Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                        return;
                    }
                    // exit settings screen
                    this.cancelAction();
                }.createDelegate(this), this.getBaseSettings(), this.gridRules ? {javaClass:"java.util.ArrayList",list:this.gridRules.getFullSaveList()} : null);
            }
        }
    });
}