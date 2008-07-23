if (!Ung.hasResource["Ung.Racks"]) {
    Ung.hasResource["Ung.Racks"] = true;

    Ung.Racks = Ext.extend(Ung.ConfigWin, {
        panelPolicyManagement : null,
        gridRacks : null,
        gridPolicies : null,
        initComponent : function() {
            this.breadcrumbs = [{
                title : i18n._("Configuration"),
                action : function() {
                    this.cancelAction();
                }.createDelegate(this)
            }, {
                title : i18n._('Policy Management')
            }];
            Ung.Racks.superclass.initComponent.call(this);
        },

        onRender : function(container, position) {
            // call superclass renderer first
            Ung.Racks.superclass.onRender.call(this, container, position);
            this.initSubCmps.defer(1, this);
            // builds the tabs
        },
        initSubCmps : function() {
            this.buildPolicyManagement();
            // builds the tab panel with the tabs
            this.buildTabPanel([this.panelPolicyManagement]);
            this.tabs.activate(this.panelPolicyManagement);
            // this.loadGridRacks();
        },
        getPolicyConfiguration : function(forceReload) {
            if (forceReload || this.rpc.policyConfiguration === undefined) {
                this.rpc.policyConfiguration = rpc.policyManager.getPolicyConfiguration();
            }
            return this.rpc.policyConfiguration;
        },
        buildPolicyManagement : function() {
            this.buildRacks();
            this.buildPolicies();
            this.panelPolicyManagement = new Ext.Panel({
                // private fields
                name : 'Policy Management',
                parentId : this.getId(),
                title : this.i18n._('Policy Management'),
                layout : "form",
                autoScroll : true,
                items : [
                    this.gridRacks,
                    this.gridPolicies
                ]
            });

        },
        buildRacks : function() {
            this.gridRacks = new Ung.EditorGrid({
                settingsCmp : this,
                name : 'Racks',
                height : 300,
                bodyStyle : 'padding-bottom:20px;',
                autoScroll : true,
                parentId : this.getId(),
                title : this.i18n._('Racks'),
                recordJavaClass : "com.untangle.uvm.policy.Policy",
                emptyRow : {
                    "default" : false,
                    "name" : this.i18n._("[no name]"),
                    "notes" : this.i18n._("[no description]"),
                    "javaClass" : "com.untangle.uvm.policy.Policy"
                },
                //autoExpandColumn : 'notes',
                data : this.getPolicyConfiguration().policies,
                dataRoot: 'list',
                paginated: false,
                fields : [{
                    name : 'id'
                },{
                    name : 'default'
                }, {
                    name : 'name'
                }, {
                    name : 'notes'
                }, {
                    name : 'javaClass'
                }],
                columns : [{
                    header : this.i18n._("name"),
                    width : 200,
                    sortable : true,
                    dataIndex : 'name'
                }, {
                    header : this.i18n._("description"),
                    width : 200,
                    sortable : true,
                    dataIndex : 'notes'

                }],
                rowEditorInputLines : [{
                	xtype: "textfield",
                    name : "Name",
                    dataIndex : "name",
                    fieldLabel : this.i18n._("Name"),
                    allowBlank : false,
                    blankText : this.i18n._("The policy name cannot be blank."),
                    width : 200,
                    editor : new Ext.form.TextField({
                        allowBlank : false
                    })
                }, {
                	xtype: "textfield",
                    name : "Description",
                    dataIndex : "notes",
                    fieldLabel : this.i18n._("Description"),
                    allowBlank : false,
                    width : 200,
                    editor : new Ext.form.TextField({
                        allowBlank : true
                    })
                }]
            });

        },
        buildPolicies : function() {
        	var liveColumn = new Ext.grid.CheckColumn({
                header : "<b>" + this.i18n._("live") + "</b>",
                dataIndex : 'live',
                fixed : true
            });
            this.gridPolicies = new Ung.EditorGrid({
                settingsCmp : this,
                name : 'Policies',
                height : 300,
                bodyStyle : 'padding-bottom:20px;',
                autoScroll : true,
                parentId : this.getId(),
                title : this.i18n._('Policies'),
                recordJavaClass : "com.untangle.uvm.policy.UserPolicyRule",
                emptyRow : {
                    "live" : true,
                    "policy" : null,
                    "clientIntf" : null,
                    "serverIntf" : null,
                    "protocol": null,
                    "clientAddr" : null,
                    "serverAddr" :null,
                    "clientPort" : null,
                    "serverPort" : null,
                    "user" : null,
                    "startTime" : null,
                    "endTime" : null,
                    "dayOfWeek" : null,
                    "description" : null,
                    "javaClass" : "com.untangle.uvm.policy.UserPolicyRule"
                },
                //autoExpandColumn : 'notes',
                data : this.getPolicyConfiguration().userPolicyRules,
                dataRoot: 'list',
                paginated: false,
                fields : [{
                    name : 'id'
                },{
                    name : 'live'
                }, {
                    name : 'policy'
                }, {
                    name : 'clientIntf'
                }, {
                    name : 'serverIntf'
                }, {
                    name : 'protocol'
                }, {
                    name : 'clientAddr'
                }, {
                    name : 'serverAddr'
                }, {
                    name : 'clientPort'
                }, {
                    name : 'serverPort'
                }, {
                    name : 'user'
                }, {
                    name : 'startTime'
                }, {
                    name : 'endTime'
                }, {
                    name : 'dayOfWeek'
                }, {
                    name : 'description'
                }, {
                    name : 'javaClass'
                }],
                columns : [liveColumn,
            	{
                    header : this.i18n._("<b>Use this rack</b> when the <br/>next colums are matched..."),
                    width : 200,
                    sortable : true,
                    dataIndex : 'policy',
                    renderer : function(value) {
                        return value!=null?value.name : this.i18n._("&gt; No rack");
                    }.createDelegate(this)
                }, {
                    header : this.i18n._("client <br/>interface"),
                    width : 150,
                    sortable : true,
                    dataIndex : 'clientIntf'

                }, {
                    header : this.i18n._("server <br/>interface"),
                    width : 150,
                    sortable : true,
                    dataIndex : 'serverIntf'

                }, {
                    header : this.i18n._("protocol"),
                    width : 150,
                    sortable : true,
                    dataIndex : 'protocol'

                }, {
                    header : this.i18n._("clientAddr"),
                    width : 150,
                    sortable : true,
                    dataIndex : 'clientAddr'

                }, {
                    header : this.i18n._("serverAddr"),
                    width : 150,
                    sortable : true,
                    dataIndex : 'serverAddr'

                }, {
                    header : this.i18n._("user"),
                    width : 150,
                    sortable : true,
                    dataIndex : 'user'

                }, {
                    header : this.i18n._("startTime"),
                    width : 100,
                    sortable : true,
                    dataIndex : 'startTime'

                }, {
                    header : this.i18n._("endTime"),
                    width : 100,
                    sortable : true,
                    dataIndex : 'endTime'

                }, {
                    header : this.i18n._("dayOfWeek"),
                    width : 150,
                    sortable : true,
                    dataIndex : 'dayOfWeek'

                }, {
                    header : this.i18n._("description"),
                    width : 150,
                    sortable : true,
                    dataIndex : 'description'

                }],
                plugins : [liveColumn],
                
                
                
                initComponent : function() {
                	this.rowEditor = new Ung.RowEditorWindow({
                        grid : this,
                        sizeToRack: true,
                        title : this.settingsCmp.i18n._("Policy Wizard"),
                        inputLines : this.customInputLines,
                        populate : function(record, addMode) {
                            this.addMode=addMode;
                            this.record = record;
                            this.initialRecordData = Ext.encode(record.data);
                            alert("todo populate");
                        },
                        updateAction : function() {
                            alert("todo updateAction");
                            this.cancelAction();
                        },
                        show : function() {
                            Ung.UpdateWindow.superclass.show.call(this);
                        }
                	});
                    this.rowEditor.render('container');
                    Ung.EditorGrid.prototype.initComponent.call(this);
                },
                customInputLines: [{
                    xtype : 'fieldset',
                    autoHeight : true,
                    title : this.i18n._("Protocol"),
                    items: [{
                        bodyStyle : 'padding:0px 0px 5px 5px;',
                        border : false,
                        html:this.i18n._("The protocol you would like this policy to handle.")
                    }, {
                        xtype : 'combo',
                        name : 'Protocol',
                        fieldLabel: this.i18n._("Protocol"),
                        editable : false,
                        //store : timeZones,
                        width : 350,
                        mode : 'local',
                        triggerAction : 'all',
                        listClass : 'x-combo-list-small',
                        value : ""
                    }]
                }, {
                    xtype : 'fieldset',
                    autoHeight : true,
                    title : this.i18n._("Interface"),
                    items: [{
                        bodyStyle : 'padding:0px 0px 5px 5px;',
                        border : false,
                        html:this.i18n._("The ethernet interface (NIC) you would like this policy to handle.")
                    }, {
                        xtype : 'combo',
                        name : 'Client',
                        fieldLabel: this.i18n._("Client"),
                        editable : false,
                        //store : timeZones,
                        width : 350,
                        mode : 'local',
                        triggerAction : 'all',
                        listClass : 'x-combo-list-small',
                        value : ""
                        
                    }, {
                        xtype : 'combo',
                        name : 'Server',
                        fieldLabel: this.i18n._("Server"),
                        editable : false,
                        //store : timeZones,
                        width : 350,
                        mode : 'local',
                        triggerAction : 'all',
                        listClass : 'x-combo-list-small',
                        value : ""
                        
                    }]
                }, {
                    xtype : 'fieldset',
                    autoHeight : true,
                    title : this.i18n._("Address"),
                    items: [{
                        bodyStyle : 'padding:0px 0px 5px 5px;',
                        border : false,
                        html:this.i18n._("The IP address which you would like this policy to handle.")
                    }, {
                        xtype : 'textfield',
                        name : 'Client',
                        fieldLabel : this.i18n._("Client"),
                        allowBlank : false,
                        value : ""
                    }, {
                        xtype : 'textfield',
                        name : 'Server',
                        fieldLabel : this.i18n._("Server"),
                        allowBlank : false,
                        value : ""
                    }]
                }, {
                    xtype : 'fieldset',
                    autoHeight : true,
                    title : this.i18n._("Port"),
                    items: [{
                        bodyStyle : 'padding:0px 0px 5px 5px;',
                        border : false,
                        html:this.i18n._("The port which you would like this policy to handle.")
                    }, {
                        xtype : 'numberfield',
                        name : 'Server',
                        fieldLabel : this.i18n._("Server"),
                        allowBlank : false,
                        value : "25"
                    }]
                }, {
                    xtype : 'fieldset',
                    autoHeight : true,
                    title : this.i18n._("Users"),
                    items: [{
                        bodyStyle : 'padding:0px 0px 5px 5px;',
                        border : false,
                        html:this.i18n._("The users you would like to apply this policy to.")
                    }, {
                        html: "todo"
                    }]
                }, {
                    xtype : 'fieldset',
                    autoHeight : true,
                    title : this.i18n._("Time of Day"),
                    items: [{
                        bodyStyle : 'padding:0px 0px 5px 5px;',
                        border : false,
                        html:this.i18n._("The time of day you would like this policy active.")
                    }, {
                        xtype : 'timefield',
                        name : 'Start Time',
                        fieldLabel : this.i18n._("Start Time"),
                        allowBlank : false,
                        value : ""
                    }, {
                        xtype : 'timefield',
                        name : 'End Time',
                        fieldLabel : this.i18n._("End Time"),
                        allowBlank : false,
                        value : ""
                    }]
                }, {
                    xtype : 'fieldset',
                    autoHeight : true,
                    title : this.i18n._("Days of Week"),
                    items: [{
                        bodyStyle : 'padding:0px 0px 5px 5px;',
                        border : false,
                        html:this.i18n._("The time of day you would like this policy active.")
                    }, {
                        xtype : 'checkbox',
                        name : 'Sunday',
                        boxLabel : this.i18n._('Sunday'),
                        hideLabel : true,
                        checked : true
                    }, {
                        xtype : 'checkbox',
                        name : 'Monday',
                        boxLabel : this.i18n._('Monday'),
                        hideLabel : true,
                        checked : true
                    }, {
                        xtype : 'checkbox',
                        name : 'Tuesday',
                        boxLabel : this.i18n._('Tuesday'),
                        hideLabel : true,
                        checked : true
                    }, {
                        xtype : 'checkbox',
                        name : 'Wednesday',
                        boxLabel : this.i18n._('Wednesday'),
                        hideLabel : true,
                        checked : true
                    }, {
                        xtype : 'checkbox',
                        name : 'Thursday',
                        boxLabel : this.i18n._('Thursday'),
                        hideLabel : true,
                        checked : true
                    }, {
                        xtype : 'checkbox',
                        name : 'Friday',
                        boxLabel : this.i18n._('Friday'),
                        hideLabel : true,
                        checked : true
                    }, {
                        xtype : 'checkbox',
                        name : 'Saturday',
                        boxLabel : this.i18n._('Saturday'),
                        hideLabel : true,
                        checked : true
                    }]
                }, {
                    xtype : 'fieldset',
                    autoHeight : true,
                    title : this.i18n._("Rack"),
                    items: [{
                        bodyStyle : 'padding:0px 0px 5px 5px;',
                        border : false,
                        html:this.i18n._("The rack you would like to use to handle this policy.")
                    }, {
                        xtype : 'combo',
                        name : 'Rack',
                        fieldLabel: this.i18n._("Rack"),
                        editable : false,
                        //store : timeZones,
                        width : 350,
                        mode : 'local',
                        triggerAction : 'all',
                        listClass : 'x-combo-list-small',
                        value : ""
                        
                    }, {
                        xtype : 'textfield',
                        name : 'Description',
                        fieldLabel : this.i18n._("Description"),
                        allowBlank : false,
                        value : ""
                    }]
                }, {
                    xtype : 'fieldset',
                    autoHeight : true,
                    items: [{
                        xtype : 'checkbox',
                        name : 'Enable this Policy',
                        boxLabel : this.i18n._('Enable this Policy'),
                        hideLabel : true,
                        checked : true
                    }]
                }]
            });

        },
        validateClient : function() {
        	return true;
        },
        // save function
        saveAction : function() {
            if (this.validate()) {
                Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
                this.saveSemaphore = 1;
                // save language settings

                rpc.policyManager.setPolicyConfiguration(function(result, exception) {
                    if (exception) {
                        Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                        return;
                    }
                    this.afterSave();
                }.createDelegate(this), this.getPolicyConfiguration());
            }
        },
        afterSave : function() {
            this.saveSemaphore--;
            if (this.saveSemaphore == 0) {
                Ext.MessageBox.hide();
                this.cancelAction();
            }
        }

    });
}
