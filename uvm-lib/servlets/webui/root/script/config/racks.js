if (!Ung.hasResource["Ung.Racks"]) {
    Ung.hasResource["Ung.Racks"] = true;

    Ung.Racks = Ext.extend(Ung.ConfigWin, {
        panelPolicyManagement : null,
        gridRacks : null,
        gridRules : null,
        policyStore : null,
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
        },
        getPolicyConfiguration : function(forceReload) {
            if (forceReload || this.rpc.policyConfiguration === undefined) {
                this.rpc.policyConfiguration = rpc.policyManager.getPolicyConfiguration();
            }
            return this.rpc.policyConfiguration;
        },
        getPolicyManagerValidator : function(forceReload) {
            if (forceReload || this.rpc.policyManagerValidator === undefined) {
                this.rpc.policyManagerValidator = rpc.policyManager.getValidator();
            }
            return this.rpc.policyManagerValidator;
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
                items : [this.gridRacks, this.gridRules]
            });

        },
        buildRacks : function() {
            this.gridRacks = new Ung.EditorGrid({
                settingsCmp : this,
                anchor :"100% 50%",
                name : 'Racks',
                height : 250,
                bodyStyle : 'padding-bottom:15px;',
                autoScroll : true,
                parentId : this.getId(),
                title : this.i18n._('Racks'),
                recordJavaClass : "com.untangle.uvm.policy.Policy",
                emptyRow : {
                    "default" : false,
                    "name" : this.i18n._("[no name]"),
                    "notes" : this.i18n._("[no description]")
                },
                data : this.getPolicyConfiguration().policies,
                dataRoot : 'list',
                paginated : false,
                //autoExpandColumn: "name",
                fields : [{
                    name : 'id'
                }, {
                    name : 'default'
                }, {
                    name : 'name'
                }, {
                    name : 'notes'
                }],
                columns : [{
                    header : this.i18n._("name"),
                    width : 200,
                    sortable : true,
                    dataIndex : 'name',
                    editor : new Ext.form.TextField({
                        allowBlank : false
                    })
                }, {
                    header : this.i18n._("description"),
                    width : 200,
                    sortable : true,
                    dataIndex : 'notes',
                    editor : new Ext.form.TextField({
                        allowBlank : false
                    })

                }],
                rowEditorInputLines : [{
                    xtype : "textfield",
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
                    xtype : "textfield",
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
            this.policyStoreData = [];
            this.policyStoreData.push({
                key : null,
                name : this.i18n._("> No rack"),
                policy : null
            });
            var policiesList = this.getPolicyConfiguration().policies.list;
            for (var i = 0; i < this.getPolicyConfiguration().policies.list.length; i++) {
                this.policyStoreData.push({
                    key : policiesList[i].name,
                    name : policiesList[i].name,
                    policy : policiesList[i]
                });
            }
            this.policyStore = new Ext.data.JsonStore({
                fields : ['key', 'name', 'policy'],
                data : this.policyStoreData
            });
            var liveColumn = new Ext.grid.CheckColumn({
                header : "<b>" + this.i18n._("live") + "</b>",
                dataIndex : 'live',
                width : 25,
                fixed : true
            });
            var usersColumn=new Ext.grid.UsersColumn({
                dataIndex : 'user'
            });
            this.gridRules = new Ung.EditorGrid({
                settingsCmp : this,
                name : 'Policies',
                height : 250,
                anchor :"100% 50%",
                autoScroll : true,
                parentId : this.getId(),
                title : this.i18n._('Policies'),
                recordJavaClass : "com.untangle.uvm.policy.UserPolicyRule",
                hasReorder : true,
                emptyRow : {
                    "live" : true,
                    "policy" : null,
                    "clientIntf" : "any",
                    "serverIntf" : "any",
                    "protocol" : "TCP & UDP",
                    "clientAddr" : "any",
                    "serverAddr" : "any",
                    "clientPort" : null,
                    "serverPort" : "any",
                    "user" : "[any]",
                    "startTime" : {"time":-7200000,"javaClass":"java.sql.Time"},
                    "endTime" : {"time":79140000,"javaClass":"java.sql.Time"},
                    "startTimeFormatted" : "00:00",
                    "endTimeFormatted" : "23:59",
                    "dayOfWeek" : "any",
                    "description" : this.i18n._('[no description]')
                },
                // autoExpandColumn : 'notes',
                data : this.getPolicyConfiguration().userPolicyRules,
                dataRoot : 'list',
                paginated : false,
                fields : [{
                    name : 'id'
                }, {
                    name : 'live'
                }, {
                    name : 'policy'
                }, {
                    name : 'policyName',
                    mapping : 'policy',
                    convert : function(val, rec) {
                    	return val==null?null:val.name;
                    }
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
                    name : 'startTimeFormatted',
                    mapping: 'startTime',
                    convert : Ung.Util.formatTime
                }, {
                    name : 'endTimeFormatted',
                    mapping: 'endTime',
                    convert : Ung.Util.formatTime
                }, {
                    name : 'dayOfWeek'
                }, {
                    name : 'description'
                }],
                columns : [liveColumn, {
                    header : this.i18n._("<b>Use this rack</b> when the <br/>next colums are matched..."),
                    width : 140,
                    sortable : true,
                    dataIndex : 'policyName',
                    renderer : function(value, metadata, record) {
                        var result = ""
                        var store = this.policyStore;
                        if (store) {
                            var index = store.findBy(function(record, id) {
                                if (record.data.key == value) {
                                    return true;
                                } else {
                                    return false;
                                }
                            });
                            if (index >= 0) {
                                result = store.getAt(index).get("name");
                                record.data.policy = store.getAt(index).get("policy");
                            }
                        }
                        return result;
                    }.createDelegate(this),
                    editor : new Ext.form.ComboBox({
                        store : this.policyStore,
                        displayField : 'name',
                        valueField : 'key',
                        editable : false,
                        mode : 'local',
                        triggerAction : 'all',
                        listClass : 'x-combo-list-small'
                    })
                }, {
                    header : this.i18n._("client <br/>interface"),
                    width : 75,
                    sortable : true,
                    dataIndex : 'clientIntf',
                    renderer : function(value) {
                        var result = ""
                        var store = Ung.Util.getInterfaceStore();
                        if (store) {
                            var index = store.find("key", value)
                            if (index >= 0) {
                                result = store.getAt(index).get("name");
                            }
                        }
                        return result;
                    },
                    editor : new Ung.Util.InterfaceCombo({})

                }, {
                    header : this.i18n._("server <br/>interface"),
                    width : 75,
                    sortable : true,
                    dataIndex : 'serverIntf',
                    renderer : function(value) {
                        var result = ""
                        var store = Ung.Util.getInterfaceStore();
                        if (store) {
                            var index = store.find("key", value)
                            if (index >= 0) {
                                result = store.getAt(index).get("name");
                            }
                        }
                        return result;
                    },
                    editor : new Ung.Util.InterfaceCombo({})

                }, {
                    header : this.i18n._("protocol"),
                    width : 75,
                    sortable : true,
                    dataIndex : 'protocol',
                    renderer : function(value) {
                        var result = ""
                        var store = Ung.Util.getProtocolStore();
                        if (store) {
                            var index = store.find("key", value)
                            if (index >= 0) {
                                result = store.getAt(index).get("name");
                            }
                        }
                        return result;
                    },
                    editor : new Ung.Util.ProtocolCombo({})

                }, {
                    header : this.i18n._("client <br/>address"),
                    width : 70,
                    sortable : true,
                    dataIndex : 'clientAddr',
                    editor : new Ext.form.TextField({
                        allowBlank : false
                    })

                }, {
                    header : this.i18n._("server <br/>address"),
                    width : 70,
                    sortable : true,
                    dataIndex : 'serverAddr',
                    editor : new Ext.form.TextField({
                        allowBlank : false
                    })

                }, {
                    header : this.i18n._("server<br/>port"),
                    width : 45,
                    sortable : true,
                    dataIndex : 'serverPort',
                    editor : new Ext.form.TextField({
                        allowBlank : false
                    })

                },  
                usersColumn,
                {
                    header : this.i18n._("start time"),
                    width : 55,
                    sortable : true,
                    dataIndex : 'startTimeFormatted',
                    renderer : function(value, metadata, record) {
                        var dt = Date.parseDate(value, "H:i");
                        record.data.startTime = {"time":dt.getTime(),"javaClass":"java.sql.Time"};
                        return value;
                    }.createDelegate(this),
                    editor : new Ext.form.TimeField({
                        format : "H:i",
                        allowBlank : false
                    })
                }, {
                    header : this.i18n._("end time"),
                    width : 55,
                    sortable : true,
                    dataIndex : 'endTimeFormatted',
                    renderer : function(value, metadata, record) {
                        var dt = Date.parseDate(value, "H:i");
                        record.data.endTime = {"time":dt.getTime(),"javaClass":"java.sql.Time"};
                        return value;
                    }.createDelegate(this),
                    editor : new Ext.form.TimeField({
                        format : "H:i",
                        allowBlank : false
                    })
                }, {
                    header : this.i18n._("dayOfWeek"),
                    width : 65,
                    sortable : true,
                    dataIndex : 'dayOfWeek',
                    renderer : function(value, metadata, record) {
                    	var out=[];
                    	if(value!=null) {
                    		var arr=value.split(",");
                    		for(var i=0;i<arr.length;i++) {
                    			out.push(this.i18n._(arr[i]));
                    		}
                    	}
                    	return out.join(",");
                    }.createDelegate(this)/*,
                    editor : new Ext.form.TextField({
                        allowBlank : false
                    })*/
                }, {
                    header : this.i18n._("description"),
                    width : 75,
                    sortable : true,
                    dataIndex : 'description',
                    editor : new Ext.form.TextField({
                        allowBlank : false
                    })
                }],
                plugins : [liveColumn,usersColumn],

                initComponent : function() {
                    this.rowEditor = new Ung.RowEditorWindow({
                        grid : this,
                        sizeToRack : true,
                        title : this.settingsCmp.i18n._("Policy Wizard"),
                        inputLines : this.customInputLines,
                        populate : function(record, addMode) {
                            this.addMode = addMode;
                            this.record = record;
                            this.initialRecordData = Ext.encode(record.data);
                            Ext.getCmp("gridRules_rowEditor_protocol").setValue(record.data.protocol);
                            Ext.getCmp("gridRules_rowEditor_client_interface").setValue(record.data.clientIntf);
                            Ext.getCmp("gridRules_rowEditor_server_interface").setValue(record.data.serverIntf);
                            Ext.getCmp("gridRules_rowEditor_client_address").setValue(record.data.clientAddr);
                            Ext.getCmp("gridRules_rowEditor_server_address").setValue(record.data.serverAddr);
                            Ext.getCmp("gridRules_rowEditor_server_port").setValue(record.data.serverPort);
                            Ext.getCmp("gridRules_rowEditor_user").setValue(record.data.user);
                            Ext.getCmp("gridRules_rowEditor_start_time").setValue(record.data.startTimeFormatted);
                            Ext.getCmp("gridRules_rowEditor_end_time").setValue(record.data.endTimeFormatted);
                            Ext.getCmp("gridRules_rowEditor_sunday").setValue(record.data.dayOfWeek == "any"
                                    || record.data.dayOfWeek.indexOf("Sunday") >= 0);
                            Ext.getCmp("gridRules_rowEditor_monday").setValue(record.data.dayOfWeek == "any"
                                    || record.data.dayOfWeek.indexOf("Monday") >= 0);
                            Ext.getCmp("gridRules_rowEditor_tuesday").setValue(record.data.dayOfWeek == "any"
                                    || record.data.dayOfWeek.indexOf("Tuesday") >= 0);
                            Ext.getCmp("gridRules_rowEditor_wednesday").setValue(record.data.dayOfWeek == "any"
                                    || record.data.dayOfWeek.indexOf("Wednesday") >= 0);
                            Ext.getCmp("gridRules_rowEditor_thursday").setValue(record.data.dayOfWeek == "any"
                                    || record.data.dayOfWeek.indexOf("Thursday") >= 0);
                            Ext.getCmp("gridRules_rowEditor_friday").setValue(record.data.dayOfWeek == "any"
                                    || record.data.dayOfWeek.indexOf("Friday") >= 0);
                            Ext.getCmp("gridRules_rowEditor_saturday").setValue(record.data.dayOfWeek == "any"
                                    || record.data.dayOfWeek.indexOf("Saturday") >= 0);
                            Ext.getCmp("gridRules_rowEditor_rack").setValue(record.data.policyName);
                            Ext.getCmp("gridRules_rowEditor_description").setValue(record.data.description);
                            Ext.getCmp("gridRules_rowEditor_live").setValue(record.data.live);
                        },
                        isFormValid : function() {
                            return true;
                        },
                        updateAction : function() {
                            if (this.isFormValid()) {
                                if (this.record !== null) {
                                    this.record.set("protocol", Ext.getCmp("gridRules_rowEditor_protocol").getValue());
                                    this.record.set("clientIntf", Ext.getCmp("gridRules_rowEditor_client_interface").getValue());
                                    this.record.set("serverIntf", Ext.getCmp("gridRules_rowEditor_server_interface").getValue());
                                    this.record.set("clientAddr", Ext.getCmp("gridRules_rowEditor_client_address").getValue());
                                    this.record.set("serverAddr", Ext.getCmp("gridRules_rowEditor_server_address").getValue());
                                    this.record.set("serverPort", Ext.getCmp("gridRules_rowEditor_server_port").getValue());
                                    this.record.set("user", Ext.getCmp("gridRules_rowEditor_user").getValue());
                                    this.record.set("startTimeFormatted", Ext.getCmp("gridRules_rowEditor_start_time").getValue());
                                    this.record.set("endTimeFormatted", Ext.getCmp("gridRules_rowEditor_end_time").getValue());
                                    var dayOfWeek = "";
                                    if (Ext.getCmp("gridRules_rowEditor_sunday").getValue()
                                            && Ext.getCmp("gridRules_rowEditor_monday").getValue()
                                            && Ext.getCmp("gridRules_rowEditor_tuesday").getValue()
                                            && Ext.getCmp("gridRules_rowEditor_wednesday").getValue()
                                            && Ext.getCmp("gridRules_rowEditor_thursday").getValue()
                                            && Ext.getCmp("gridRules_rowEditor_friday").getValue()
                                            && Ext.getCmp("gridRules_rowEditor_saturday").getValue()) {
                                       dayOfWeek="any" 	

                                    } else {
                                        var out = [];
                                        if (Ext.getCmp("gridRules_rowEditor_sunday").getValue()) {
                                            out.push("Sunday");
                                        }
                                        if (Ext.getCmp("gridRules_rowEditor_monday").getValue()) {
                                            out.push("Monday");
                                        }
                                        if (Ext.getCmp("gridRules_rowEditor_tuesday").getValue()) {
                                            out.push("Tuesday");
                                        }
                                        if (Ext.getCmp("gridRules_rowEditor_wednesday").getValue()) {
                                            out.push("Wednesday");
                                        }
                                        if (Ext.getCmp("gridRules_rowEditor_thursday").getValue()) {
                                            out.push("Thursday");
                                        }
                                        if (Ext.getCmp("gridRules_rowEditor_friday").getValue()) {
                                            out.push("Friday");
                                        }
                                        if (Ext.getCmp("gridRules_rowEditor_saturday").getValue()) {
                                            out.push("Saturday");
                                        }
                                        dayOfWeek=out.join(",");
                                    }
                                    this.record.set("dayOfWeek", dayOfWeek);
                                    this.record.set("policyName", Ext.getCmp("gridRules_rowEditor_rack").getValue());
                                    this.record.set("description", Ext.getCmp("gridRules_rowEditor_description").getValue());
                                    this.record.set("live", Ext.getCmp("gridRules_rowEditor_live").getValue());

                                    if (this.addMode) {
                                        this.grid.getStore().insert(0, [this.record]);
                                        this.grid.updateChangedData(this.record, "added");
                                    }
                                }
                                this.hide();
                            } else {
                                Ext.MessageBox.alert(i18n._('Warning'), i18n._("The form is not valid!"));
                            }
                        },
                        show : function() {
                            Ung.UpdateWindow.superclass.show.call(this);
                        }
                    });
                    
                    this.usersWindow= new Ung.UsersWindow({
                    	grid : this
                    });
                    //this.usersWindow.renderTo('container');
                    
                    Ung.EditorGrid.prototype.initComponent.call(this);
                },
                customInputLines : [{
                    xtype : 'fieldset',
                    autoHeight : true,
                    title : this.i18n._("Protocol"),
                    items : [{
                        bodyStyle : 'padding:0px 0px 5px 5px;',
                        border : false,
                        html : this.i18n._("The protocol you would like this policy to handle.")
                    }, new Ung.Util.ProtocolCombo({
                        id : 'gridRules_rowEditor_protocol',
                        xtype : 'combo',
                        fieldLabel : this.i18n._("Protocol")
                    })]
                }, {
                    xtype : 'fieldset',
                    autoHeight : true,
                    title : this.i18n._("Interface"),
                    items : [{
                        bodyStyle : 'padding:0px 0px 5px 5px;',
                        border : false,
                        html : this.i18n._("The ethernet interface (NIC) you would like this policy to handle.")
                    }, new Ung.Util.InterfaceCombo({
                        name : 'Client',
                        id : 'gridRules_rowEditor_client_interface',
                        fieldLabel : this.i18n._("Client"),
                        editable : false,
                        store : Ung.Util.getInterfaceStore(),
                        width : 350

                    }), new Ung.Util.InterfaceCombo({
                        name : 'Server',
                        id : 'gridRules_rowEditor_server_interface',
                        fieldLabel : this.i18n._("Server"),
                        width : 350

                    })]
                }, {
                    xtype : 'fieldset',
                    autoHeight : true,
                    title : this.i18n._("Address"),
                    items : [{
                        bodyStyle : 'padding:0px 0px 5px 5px;',
                        border : false,
                        html : this.i18n._("The IP address which you would like this policy to handle.")
                    }, {
                        xtype : 'textfield',
                        name : 'Client',
                        id : 'gridRules_rowEditor_client_address',
                        fieldLabel : this.i18n._("Client"),
                        allowBlank : false
                    }, {
                        xtype : 'textfield',
                        name : 'Server',
                        id : 'gridRules_rowEditor_server_address',
                        fieldLabel : this.i18n._("Server"),
                        allowBlank : false
                    }]
                }, {
                    xtype : 'fieldset',
                    autoHeight : true,
                    title : this.i18n._("Port"),
                    items : [{
                        bodyStyle : 'padding:0px 0px 5px 5px;',
                        border : false,
                        html : this.i18n._("The port which you would like this policy to handle.")
                    }, {
                        xtype : 'textfield',
                        name : 'Server',
                        id : 'gridRules_rowEditor_server_port',
                        fieldLabel : this.i18n._("Server"),
                        allowBlank : false
                    }]
                }, {
                    xtype : 'fieldset',
                    autoHeight : true,
                    title : this.i18n._("Users"),
                    items : [{
                        bodyStyle : 'padding:0px 0px 5px 5px;',
                        border : false,
                        html : this.i18n._("The users you would like to apply this policy to.")
                    }, {
                        xtype : 'textfield',
                        name : 'Users',
                        width : 200,
                        readOnly : true,
                        id : 'gridRules_rowEditor_user',
                        fieldLabel : this.i18n._("Users"),
                        allowBlank : false
                    }, {
                        xtype: "button",
                        name : 'Change Users',
                        text : i18n._("Change Users"),
                        handler : function() {
                            this.gridRules.usersWindow.show();
                            this.gridRules.usersWindow.populate(this.gridRules.rowEditor.record,function() {
                                Ext.getCmp("gridRules_rowEditor_user").setValue(this.gridRules.rowEditor.record.data.user);
                            }.createDelegate(this));
                        }.createDelegate(this)
                    }]
                }, {
                    xtype : 'fieldset',
                    autoHeight : true,
                    title : this.i18n._("Time of Day"),
                    items : [{
                        bodyStyle : 'padding:0px 0px 5px 5px;',
                        border : false,
                        html : this.i18n._("The time of day you would like this policy active.")
                    }, {
                        xtype : 'timefield',
                        name : 'Start Time',
                        format : "H:i",
                        id : 'gridRules_rowEditor_start_time',
                        fieldLabel : this.i18n._("Start Time"),
                        allowBlank : false
                    }, {
                        xtype : 'timefield',
                        name : 'End Time',
                        id : 'gridRules_rowEditor_end_time',
                        format : "H:i",
                        fieldLabel : this.i18n._("End Time"),
                        allowBlank : false
                    }]
                }, {
                    xtype : 'fieldset',
                    autoHeight : true,
                    title : this.i18n._("Days of Week"),
                    items : [{
                        bodyStyle : 'padding:0px 0px 5px 5px;',
                        border : false,
                        html : this.i18n._("The time of day you would like this policy active.")
                    }, {
                        xtype : 'checkbox',
                        name : 'Sunday',
                        id : 'gridRules_rowEditor_sunday',
                        boxLabel : this.i18n._('Sunday'),
                        hideLabel : true,
                        checked : true
                    }, {
                        xtype : 'checkbox',
                        name : 'Monday',
                        id : 'gridRules_rowEditor_monday',
                        boxLabel : this.i18n._('Monday'),
                        hideLabel : true,
                        checked : true
                    }, {
                        xtype : 'checkbox',
                        name : 'Tuesday',
                        id : 'gridRules_rowEditor_tuesday',
                        boxLabel : this.i18n._('Tuesday'),
                        hideLabel : true,
                        checked : true
                    }, {
                        xtype : 'checkbox',
                        name : 'Wednesday',
                        id : 'gridRules_rowEditor_wednesday',
                        boxLabel : this.i18n._('Wednesday'),
                        hideLabel : true,
                        checked : true
                    }, {
                        xtype : 'checkbox',
                        name : 'Thursday',
                        id : 'gridRules_rowEditor_thursday',
                        boxLabel : this.i18n._('Thursday'),
                        hideLabel : true,
                        checked : true
                    }, {
                        xtype : 'checkbox',
                        name : 'Friday',
                        id : 'gridRules_rowEditor_friday',
                        boxLabel : this.i18n._('Friday'),
                        hideLabel : true,
                        checked : true
                    }, {
                        xtype : 'checkbox',
                        name : 'Saturday',
                        id : 'gridRules_rowEditor_saturday',
                        boxLabel : this.i18n._('Saturday'),
                        hideLabel : true,
                        checked : true
                    }]
                }, {
                    xtype : 'fieldset',
                    autoHeight : true,
                    title : this.i18n._("Rack"),
                    items : [{
                        bodyStyle : 'padding:0px 0px 5px 5px;',
                        border : false,
                        html : this.i18n._("The rack you would like to use to handle this policy.")
                    }, {
                        xtype : 'combo',
                        name : 'Rack',
                        id : 'gridRules_rowEditor_rack',
                        fieldLabel : this.i18n._("Rack"),
                        editable : false,
                        store : this.policyStore,
                        displayField : 'name',
                        valueField : 'key',
                        width : 200,
                        mode : 'local',
                        triggerAction : 'all',
                        listClass : 'x-combo-list-small'

                    }, {
                        xtype : 'textfield',
                        name : 'Description',
                        width : 200,
                        id : 'gridRules_rowEditor_description',
                        fieldLabel : this.i18n._("Description"),
                        allowBlank : false
                    }]
                }, {
                    xtype : 'fieldset',
                    autoHeight : true,
                    items : [{
                        xtype : 'checkbox',
                        name : 'Enable this Policy',
                        id : 'gridRules_rowEditor_live',
                        boxLabel : this.i18n._('Enable this Policy'),
                        hideLabel : true,
                        checked : true
                    }]
                }]
            });
        },
        validateClient : function() {
            var rackList=this.gridRacks.getFullSaveList();
            if(rackList.length==0) {
                Ext.MessageBox.alert(this.i18n._("There must always be at least one available rack."));
                return false;
            }
            for(var i=0;i<rackList.length;i++) {
            	for(var j=i+1;j<rackList.length;j++) {
            	   if(rackList[i].name==rackList[j].name) {
            	   	   Ext.MessageBox.alert(i18n._("Failed"), i18n.sprintf(this.i18n._("The rack named %s already exists."),rackList[i].name));
            	       return false;
            	   }
            	}
            }
            var rulesList=this.gridRules.getFullSaveList();
            var rackDeletedList=this.gridRacks.getDeletedList();
            for(var i=0;i<rulesList.length;i++) {
                if(rulesList[i].policy!=null) {
                	for(var j=0;j<rackDeletedList.length;j++) {
                		if(rulesList[i].policy.id==rackDeletedList[j].id) {
                			Ext.MessageBox.alert(i18n._("Failed"), i18n.sprintf(this.i18n._('The rack named %s cannot be removed because it is currently being used in "Custom Policies".'),rackDeletedList[j].name));
                            return false;
                		}
                	}
                }
            }
            return true;
        },
        validateServer : function() {
        	var rackDeletedList=this.gridRacks.getDeletedList();
        	for(var i=0;i<rackDeletedList.length;i++)
        	try {
                var result = rpc.nodeManager.nodeInstances(rackDeletedList[i]);
                if (result.list.length>0) {
                    Ext.MessageBox.alert(i18n._("Failed"), i18n.sprintf(this.i18n._("The rack named %s cannot be removed because it is not empty.  Please remove all products first."),rackDeletedList[i].name));
                    return false;
                }
            } catch (e) {
                Ext.MessageBox.alert(i18n._("Failed"), e.message);
                return false;
            }
            
            var passedAddresses = this.gridRules.getFullSaveList();
            var clientAddrList = [];
            var serverAddrList = [];
            var serverPortList = [];
            var dstPortList = [];
            for (var i = 0; i < passedAddresses.length; i++) {
                clientAddrList.push(passedAddresses[i]["clientAddr"]);
                serverAddrList.push(passedAddresses[i]["serverAddr"]);
                serverPortList.push(passedAddresses[i]["serverPort"]);
            }
            if (clientAddrList.length > 0) {
                try {
                    var result = this.getPolicyManagerValidator().validate({
                        map : { "type"      : "IP", 
                                "values"    : {"javaClass" : "java.util.ArrayList", list : clientAddrList}
                        },
                        "javaClass" : "java.util.HashMap"
                    });
                    if (!result.valid) {
                        Ext.MessageBox.alert(this.i18n._("Validation failed for Client Address"), this.i18n._(result.message) + ": " + result.cause);
                        return false;
                    }
                } catch (e) {
                    Ext.MessageBox.alert(i18n._("Failed"), e.message);
                    return false;
                }
            }
            if (serverAddrList.length > 0) {
                try {
                    var result = this.getPolicyManagerValidator().validate({
                        map : { "type"      : "IP", 
                                "values"    : {"javaClass" : "java.util.ArrayList", list : serverAddrList}
                        },
                        "javaClass" : "java.util.HashMap"
                    });
                    if (!result.valid) {
                        Ext.MessageBox.alert(this.i18n._("Validation failed for Server Address"), this.i18n._(result.message) + ": " + result.cause);
                        return false;
                    }
                } catch (e) {
                    Ext.MessageBox.alert(i18n._("Failed"), e.message);
                    return false;
                }
            }
            if (serverPortList.length > 0) {
                try {
                    var result = this.getPolicyManagerValidator().validate({
                        map : { "type"      : "Port", 
                                "values"    : {"javaClass" : "java.util.ArrayList", list : serverPortList}
                        },
                        "javaClass" : "java.util.HashMap"
                    });
                    if (!result.valid) {
                        Ext.MessageBox.alert(this.i18n._("Validation failed for Server Port"), this.i18n._(result.message) + ": " + result.cause);
                        return false;
                    }
                } catch (e) {
                    Ext.MessageBox.alert(i18n._("Failed"), e.message);
                    return false;
                }
            }
            
            return true;
        },
        // save function
        saveAction : function() {
            if (this.validate()) {
                this.getPolicyConfiguration().policies.list=this.gridRacks.getFullSaveList();
                this.getPolicyConfiguration().userPolicyRules.list=this.gridRules.getFullSaveList();
                Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
                rpc.policyManager.setPolicyConfiguration(function(result, exception) {
                    if (exception) {
                        Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                        return;
                    }
                    Ext.MessageBox.hide();
                this.cancelAction();
                }.createDelegate(this), this.getPolicyConfiguration());
            }
        }

    });
}
