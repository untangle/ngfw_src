if (!Ung.hasResource["Ung.PolicyManager"]) {
    Ung.hasResource["Ung.PolicyManager"] = true;
    
    Ung.PolicyManager = Ext.extend(Ung.ConfigWin, {
        fnCallback : null,
        panelPolicyManagement : null,
        gridRacks : null,
        gridRules : null,
        policyStore : null,
        node:null,
        rackKey : 1,
        initComponent : function()
        {
            this.breadcrumbs = [{
                title : this.i18n._('Policy Manager')
            }];
            if(this.node!=null) {
                this.bbar=['-',{
                    name : "Remove",
                    id : this.getId() + "_removeBtn",
                    iconCls : 'node-remove-icon',
                    text : i18n._('Remove'),
                    handler : function() {
                        if(this.node && this.node.settingsWin) {
                            this.node.settingsWin.removeAction();
                        }
                    }.createDelegate(this)
                },'-',{
                    name : 'Help',
                    id : this.getId() + "_helpBtn",
                    iconCls : 'icon-help',
                    text : i18n._("Help"),
                    handler : function() {
                        this.helpAction();
                    }.createDelegate(this)
                },'->',{
                    name : 'Save',
                    id : this.getId() + "_saveBtn",
                    iconCls : 'save-icon',
                    text : i18n._("OK"),
                    handler : function() {
                        this.saveAction.defer(1, this);
                    }.createDelegate(this)
                },"-",{
                    name : 'Cancel',
                    id : this.getId() + "_cancelBtn",
                    iconCls : 'cancel-icon',
                    text : i18n._("Cancel"),
                    handler : function() {
                        this.cancelAction();
                    }.createDelegate(this)
                },"-",{
                    name : "Apply",
                    id : this.getId() + "_applyBtn",
                    iconCls : 'apply-icon',
                    text : i18n._("Apply"),
                    handler : function() {
                        this.applyAction.defer(1, this,[true]);
                    }.createDelegate(this)
                },"-"];
            }
            
            this.rackDatabase = this.buildRackDatabase( this.getPolicyConfiguration().policies.list );
            this.buildPolicyManagement();
            // builds the tab panel with the tabs
            this.buildTabPanel([this.panelPolicyManagement]);
            this.tabs.activate(this.panelPolicyManagement);
            Ung.PolicyManager.superclass.initComponent.call(this);
        },

        getPolicyConfiguration : function(forceReload) {
            if (forceReload || this.rpc.policyConfiguration === undefined) {
                try {
                    /* Force a reload of the policy manager */
                    var policyManager = rpc.jsonrpc.RemoteUvmContext.policyManager();
                    rpc.policyManager = policyManager;
                    this.rpc.policyConfiguration = rpc.policyManager.getPolicyConfiguration();

                    if ( this.node ) {
                        this.node.hasMultipleRacks = this.rpc.policyConfiguration.policies.list.length > 1;
                    }
                } catch (e) {
                    Ung.Util.rpcExHandler(e);
                }

            }
            return this.rpc.policyConfiguration;
        },
        getPolicyManagerValidator : function(forceReload) {
            if (forceReload || this.rpc.policyManagerValidator === undefined) {
                try {
                    this.rpc.policyManagerValidator = rpc.policyManager.getValidator();
                } catch (e) {
                    Ung.Util.rpcExHandler(e);
                }

            }
            return this.rpc.policyManagerValidator;
        },
        getPolicyManagerLicenseStatus : function(forceReload) {
            if (forceReload || this.rpc.policyManagerLicenseStatus === undefined) {
                try {
                    this.rpc.policyManagerLicenseStatus = main.getLicenseManager().getLicenseStatus("untangle-policy-manager");
                } catch (e) {
                    Ung.Util.rpcExHandler(e);
                }

            }
            return this.rpc.policyManagerLicenseStatus;
        },
        buildPolicyManagement : function() {
            Ung.Util.clearInterfaceStore();

            this.buildInfo();
            this.buildRacks();
            this.buildPolicies();

            var items = [];

            /* This one should always reload policy management */
            if (this.getPolicyConfiguration(true).hasRackManagement) {
                items.push(this.gridRacks);
                items.push( this.gridRules );
            } else {
                items.push(this.infoLabel);
                /**
                 * Grandfather - if you have free and more than one rule still show Rules (bug #7907)
                 */
                if (this.getPolicyConfiguration().userPolicyRules.list.length > 0)
                    items.push( this.gridRules );
            }


            this.panelPolicyManagement = new Ext.Panel({
                // private fields
                anchor: "100% 100%",
                name : 'Policy Management',
                parentId : this.getId(),
                title : this.i18n._('Policy Management'),
                layout : "form",
                autoScroll : true,
                items : items
            });
        },
        buildInfo : function() {
            var items = null;
            
            items = [{
                cls: 'description',
                border: false,
                html : this.i18n._( 'The Policy Manager application is required to create additional policies/racks. Policy Manager allows for the creation and management of different policies for different hosts, users, times of day, etc. ' )
            },{
                xtype : 'button',
                text : this.i18n._( "More Info" ),
                handler : function() {
                    var app = Ung.AppItem.getAppByLibItem("untangle-libitem-policy");
                    if (app != null && app.libItem != null) {
                        Ung.Window.cancelAction( this.isDirty(), function() {app.linkToStoreFn();});
                    }
                }.createDelegate(this)
            }];

            this.infoLabel = new Ext.form.FieldSet({
                title : this.i18n._("Racks / Policies"),
                items : items,
                autoHeight : true
            });
        },
        
        buildRacks : function()
        {   
            this.gridRacks = new Ung.EditorGrid({
                settingsCmp : this,
                anchor :"100% 50%",
                name : "Racks / Policies",
                height : 250,
                bodyStyle : 'padding-bottom:15px;',
                autoScroll : true,
                parentId : this.getId(),
                title : this.i18n._('Racks / Policies'),
                recordJavaClass : "com.untangle.uvm.policy.Policy",
                emptyRow : {
                    "default" : false,
                    "name" : this.i18n._("[no name]"),
                    "notes" : this.i18n._("[no description]"),
                    "parentId" : null
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
                },{
                    name : 'parentId'
                }],
                columns : [{
                    id : "rack-name",
                    header : this.i18n._("name"),
                    width : 200,
                    sortable : true,
                    dataIndex : 'name',
                    editor : new Ext.form.TextField({allowBlank : false})
                }, {
                    header : this.i18n._("description"),
                    width : 200,
                    sortable : true,
                    dataIndex : "notes",
                    editor : new Ext.form.TextField({allowBlank : false})
                },{
                    header : this.i18n._("Parent Rack"),
                    width : 200,
                    id : "parent-rack",
                    sortable : true,
                    dataIndex: "parentId",
                    renderer : function (value,metadata,record)
                    {
                        if ( value == null ) {
                            return this.i18n._("None");
                        }
                        
                        var rack = this.rackDatabase[value];
                        
                        if ( rack == null ) {
                            return this.i18n._("None");
                        }
                        
                        return rack.name;
                    }.createDelegate(this),
                    editor : this.gridParentRackCombo = new Ext.form.ComboBox({
                        store : this.parentStore = new Ext.data.SimpleStore({
                            fields : ["id", "name"],
                            data : [],
                            autoLoad : false
                        }),
                        displayField : "name",
                        valueField : "id",
                        editable : false,
                        mode : "local",
                        triggerAction : "all",
                        listClass : 'x-combo-list-small'
                    })
                }],
                rowEditorInputLines : [new Ext.form.TextField({
                    name : "Name",
                    dataIndex : "name",
                    fieldLabel : this.i18n._("Name"),
                    allowBlank : false,
                    blankText : this.i18n._("The policy name cannot be blank."),
                    width : 200,
                    editor : new Ext.form.TextField({
                        allowBlank : false
                    })
                }), new Ext.form.TextField({
                    name : "Description",
                    dataIndex : "notes",
                    fieldLabel : this.i18n._("Description"),
                    allowBlank : false,
                    width : 200,
                    editor : new Ext.form.TextField({
                        allowBlank : true
                    })
                }),this.editorRackCombo = new Ext.form.ComboBox({
                    name : "Parent Rack",
                    dataIndex : 'parentId',
                    fieldLabel : this.i18n._("Parent Rack"),
                    store : this.parentStore,
                    displayField : 'name',
                    valueField : "id",
                    forceSelection : true,
                    typeAhead : true,
                    mode : 'local',
                    editable : false,
                    triggerAction : 'all',
                    listClass : 'x-combo-list-small',
                    itemCls : 'firewall-spacing-1',
                    selectOnFocus : true
                })],
                listeners : {
                    "beforeedit" : {
                        fn : this.onBeforeEditRackCell,
                        scope : this
                    },
                    "afteredit" : {
                        fn : this.onAfterEditRackCell,
                        scope : this
                    }
                },
                addHandler : function() {
                    if (this.getPolicyManagerLicenseStatus(true).expired){
                        this.showPolicyManagerRequired();
                    } else {
                        this.updateParentRackStore( this.editorRackCombo, new Ext.data.Record([]));
                        Ung.EditorGrid.prototype.addHandler.call(this.gridRacks);
                    }
                }.createDelegate(this),
                editHandler : function(record) {
                    if (this.getPolicyManagerLicenseStatus(true).expired){
                        this.showPolicyManagerRequired();
                    } else {
                        this.updateParentRackStore( this.editorRackCombo, record );
                        Ung.EditorGrid.prototype.editHandler.call(this.gridRacks,record);
                        
                    }
                }.createDelegate(this),
                deleteHandler : function(record) {
                    if (this.getPolicyManagerLicenseStatus(true).expired){
                        this.showPolicyManagerRequired();
                    } else {
                        
                        Ung.EditorGrid.prototype.deleteHandler.call(this.gridRacks,record);
                        this.removeRackFromDatabase( this.rackDatabase, record.data.id);

                        this.gridRacks.getView().refresh(false);
                    }
                }.createDelegate(this)
            });

            this.gridRacks.rowEditor.updateAction = function() 
            {
                Ung.RowEditorWindow.prototype.updateAction.call(this.gridRacks.rowEditor);

                if ( this.gridRacks.rowEditor.record != null ) {
                    var record = this.gridRacks.rowEditor.record;
                    var id = record.get( "id" );
                    if ( id == null ) {
                        return;
                    }
                    
                    var policy = this.rackDatabase[id];
                    if ( policy == null ) {
                        return;
                    }
                    policy.name = record.get("name");
                }

                this.gridRacks.getView().refresh(false);
            }.createDelegate(this);
        },
        showPolicyManagerRequired : function(){
            Ext.MessageBox.show({
                title: this.i18n._('Policy Manager is Required'),
                msg: this.i18n._( 'The Policy Manager application is required to create additional policies/racks. Policy Manager allows for the creation and management of different policies for different hosts, users, times of day, etc. ' ),
                buttons: {ok:this.i18n._('More Info'), cancel:true},
                fn: function(btn){
                    if(btn=='ok'){
                        var app = Ung.AppItem.getAppByLibItem("untangle-libitem-policy");
                        if (app != null && app.libItem != null) {
                            app.linkToStoreFn();
                        }
                    }
                },
                icon: Ext.MessageBox.INFO
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
            
            var userHandler = null;
            if (this.getPolicyConfiguration(false).hasUserManagement) {
                userHandler = function(record) {
                    // populate usersWindow
                    this.grid.usersWindow.show();
                    this.grid.usersWindow.populate(record);
                };
            } else {
                userHandler = function(record) {
                    Ext.MessageBox.show({
                        title: this.i18n._("Directory Connector Feature"),
                        msg: this.i18n._("Creating policies based on Username requires Directory Connector."),
                        buttons: { 
                            ok : true
                        },
                        icon: Ext.MessageBox.INFO
                    });
                }.createDelegate( this );
            }
                
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
                tooltip : this.i18n._("live"),
                dataIndex : 'live',
                width : 25,
                fixed : true
            });
            var usersColumn=new Ext.grid.ButtonColumn({
                width: 80,
                header: this.i18n._("user"),
                dataIndex : 'user',
                handle : userHandler
            });
            this.gridRules = new Ung.EditorGrid({
                settingsCmp : this,
                name : 'Policy Rules',
                height : 250,
                anchor :"100% 50%",
                autoScroll : true,
                parentId : this.getId(),
                title : this.i18n._('Policy Rules'),
                recordJavaClass : "com.untangle.uvm.policy.UserPolicyRule",
                hasReorder : true,
                configReorder:{width:35,fixed:false,tooltip:this.i18n._("Reorder")},
                configDelete:{width:30,fixed:false,tooltip:this.i18n._("Delete")},
                configEdit:{width:25,fixed:false,tooltip:this.i18n._("Edit")},
                emptyRow : {
                    "live" : true,
                    "policy" : null,
                    "clientIntf" : "any",
                    "serverIntf" : "any",
                    "protocol" : "TCP & UDP",
                    "clientAddr" : "any",
                    "serverAddr" : "any",
                    "clientPort" : "any",
                    "serverPort" : "any",
                    "user" : "[any]",
                    "startTimeString" : "00:00",
                    "endTimeString" : "23:59",
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
                    name : 'startTimeString'
                }, {
                    name : 'endTimeString'
                }, {
                    name : 'dayOfWeek'
                }, {
                    name : 'description'
                }],
                columns : [liveColumn, {
                    header : this.i18n._("<b>Use this rack</b> when the <br/>next colums are matched..."),
                    tooltip : this.i18n._("<b>Use this rack</b> when the <br/>next colums are matched..."),
                    width : 140,
                    sortable : true,
                    dataIndex : 'policyName',
                    renderer : function(value, metadata, record) {
                        var result = "";
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
                    tooltip : this.i18n._("client <br/>interface"),
                    width : 75,
                    sortable : true,
                    dataIndex : 'clientIntf',
                    renderer : function(value) {
                        var result = "";
                        var store = Ung.Util.getInterfaceStore();
                        if (store) {
                            var index = store.find("key", value);
                            if (index >= 0) {
                                result = store.getAt(index).get("name");
                            }
                        }
                        return result;
                    },
                    editor : new Ung.Util.InterfaceCombo({})

                }, {
                    header : this.i18n._("server <br/>interface"),
                    tooltip : this.i18n._("server <br/>interface"),
                    width : 75,
                    sortable : true,
                    dataIndex : 'serverIntf',
                    renderer : function(value) {
                        var result = "";
                        var store = Ung.Util.getInterfaceStore();
                        if (store) {
                            var index = store.find("key", value);
                            if (index >= 0) {
                                result = store.getAt(index).get("name");
                            }
                        }
                        return result;
                    },
                    editor : new Ung.Util.InterfaceCombo({})

                }, {
                    header : this.i18n._("protocol"),
                    tooltip : this.i18n._("protocol"),
                    width : 75,
                    sortable : true,
                    dataIndex : 'protocol',
                    renderer : function(value) {
                        var result = "";
                        var store = Ung.Util.getProtocolStore();
                        if (store) {
                            var index = store.find("key", new RegExp("^"+value+"$"));
                            if (index >= 0) {
                                result = store.getAt(index).get("name");
                            }
                        }
                        return result;
                    },
                    editor : new Ung.Util.ProtocolCombo({})

                }, {
                    header : this.i18n._("client <br/>address"),
                    tooltip: this.i18n._("client <br/>address"),
                    width : 70,
                    sortable : true,
                    dataIndex : 'clientAddr',
                    editor : new Ext.form.TextField({
                        allowBlank : false
                    })

                }, {
                    header : this.i18n._("server <br/>address"),
                    tooltip: this.i18n._("server <br/>address"),
                    width : 70,
                    sortable : true,
                    dataIndex : 'serverAddr',
                    editor : new Ext.form.TextField({
                        allowBlank : false
                    })

                }, {
                    header : this.i18n._("server<br/>port"),
                    tooltip: this.i18n._("server<br/>port"),
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
                    tooltip: this.i18n._("start time"),
                    width : 55,
                    sortable : true,
                    dataIndex : 'startTimeString',
                    editor : new Ung.form.TimeField({
                        format : "H:i",
                        allowBlank : false
                    })
                }, {
                    header : this.i18n._("end time"),
                    tooltip: this.i18n._("end time"),
                    width : 55,
                    sortable : true,
                    dataIndex : 'endTimeString',
                    editor : new Ung.form.TimeField({
                        format : "H:i",
                        allowBlank : false,
                        endTime : true
                    })
                }, {
                    header : this.i18n._("day of week"),
                    tooltip: this.i18n._("day of week"),
                    width : 70,
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
                    tooltip: this.i18n._("description"),
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
                            Ext.getCmp("gridRules_rowEditor_start_time").setValue(record.data.startTimeString);
                            Ext.getCmp("gridRules_rowEditor_end_time").setValue(record.data.endTimeString);
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
                                    this.record.set("clientPort", "any");
                                    this.record.set("serverPort", Ext.getCmp("gridRules_rowEditor_server_port").getValue());
                                    this.record.set("user", Ext.getCmp("gridRules_rowEditor_user").getValue());
                                    this.record.set("startTimeString", Ext.getCmp("gridRules_rowEditor_start_time").getValue());
                                    this.record.set("endTimeString", Ext.getCmp("gridRules_rowEditor_end_time").getValue());
                                    var dayOfWeek = "";
                                    if (Ext.getCmp("gridRules_rowEditor_sunday").getValue()
                                            && Ext.getCmp("gridRules_rowEditor_monday").getValue()
                                            && Ext.getCmp("gridRules_rowEditor_tuesday").getValue()
                                            && Ext.getCmp("gridRules_rowEditor_wednesday").getValue()
                                            && Ext.getCmp("gridRules_rowEditor_thursday").getValue()
                                            && Ext.getCmp("gridRules_rowEditor_friday").getValue()
                                            && Ext.getCmp("gridRules_rowEditor_saturday").getValue()) {
                                        dayOfWeek="any";
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
                        isDirty : function() {
                            var initial_record_data = Ext.decode(this.initialRecordData);

                            return Ext.getCmp("gridRules_rowEditor_protocol").getValue() != initial_record_data.protocol
                                || Ext.getCmp("gridRules_rowEditor_client_interface").getValue() != initial_record_data.clientIntf
                                || Ext.getCmp("gridRules_rowEditor_server_interface").getValue() != initial_record_data.serverIntf
                                || Ext.getCmp("gridRules_rowEditor_client_address").getValue() != initial_record_data.clientAddr
                                || Ext.getCmp("gridRules_rowEditor_server_address").getValue() != initial_record_data.serverAddr
                                || Ext.getCmp("gridRules_rowEditor_server_port").getValue() != initial_record_data.serverPort
                                || Ext.getCmp("gridRules_rowEditor_user").getValue() != initial_record_data.user
                                || Ext.getCmp("gridRules_rowEditor_start_time").getValue() != initial_record_data.startTimeString
                                || Ext.getCmp("gridRules_rowEditor_end_time").getValue() != initial_record_data.endTimeString
                                || Ext.getCmp("gridRules_rowEditor_sunday").getValue() !=
                                    (initial_record_data.dayOfWeek == "any" || initial_record_data.dayOfWeek.indexOf("Sunday") >= 0)
                                || Ext.getCmp("gridRules_rowEditor_monday").getValue() !=
                                    (initial_record_data.dayOfWeek == "any" || initial_record_data.dayOfWeek.indexOf("Monday") >= 0)
                                || Ext.getCmp("gridRules_rowEditor_tuesday").getValue() !=
                                    (initial_record_data.dayOfWeek == "any" || initial_record_data.dayOfWeek.indexOf("Tuesday") >= 0)
                                || Ext.getCmp("gridRules_rowEditor_wednesday").getValue() !=
                                    (initial_record_data.dayOfWeek == "any" || initial_record_data.dayOfWeek.indexOf("Wednesday") >= 0)
                                || Ext.getCmp("gridRules_rowEditor_thursday").getValue() !=
                                    (initial_record_data.dayOfWeek == "any" || initial_record_data.dayOfWeek.indexOf("Thursday") >= 0)
                                || Ext.getCmp("gridRules_rowEditor_friday").getValue() !=
                                    (initial_record_data.dayOfWeek == "any" || initial_record_data.dayOfWeek.indexOf("Friday") >= 0)
                                || Ext.getCmp("gridRules_rowEditor_saturday").getValue() !=
                                    (initial_record_data.dayOfWeek == "any" || initial_record_data.dayOfWeek.indexOf("Saturday") >= 0)
                                || Ext.getCmp("gridRules_rowEditor_rack").getValue() != initial_record_data.policyName
                                || Ext.getCmp("gridRules_rowEditor_description").getValue() != initial_record_data.description
                                || Ext.getCmp("gridRules_rowEditor_live").getValue() != initial_record_data.live;
                        },
                        show : function() {
                            Ung.UpdateWindow.superclass.show.call(this);
                        }
                    });

                    this.usersWindow= new Ung.UsersWindow({
                        grid : this,
                        title : i18n._('Select Users'),
                        userDataIndex : "user",
                        sortField : 'UID',
                        loadLocalDirectoryUsers: false
                    });

                    Ung.EditorGrid.prototype.initComponent.call(this);
                },
                customInputLines : [{
                    xtype : 'fieldset',
                    autoHeight : true,
                    title : this.i18n._("Protocols"),
                    items : [{
                        cls: 'description',
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
                        cls: 'description',
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
                        cls: 'description',
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
                        cls: 'description',
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
                        cls: 'description',
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
                },{
                    xtype : 'fieldset',
                    autoHeight : true,
                    title : this.i18n._("Time of Day"),
                    items : [{
                        cls: 'description',
                        border : false,
                        html : this.i18n._("The time of day you would like this policy active.")
                    },{
                        xtype : 'utimefield',
                        name : 'Start Time',
                        id : 'gridRules_rowEditor_start_time',
                        fieldLabel : this.i18n._("Start Time"),
                        allowBlank : false
                    },{
                        xtype : 'utimefield',
                        name : 'End Time',
                        endTime : true,
                        id : 'gridRules_rowEditor_end_time',
                        fieldLabel : this.i18n._("End Time"),
                        allowBlank : false
                    }]
                }, {
                    xtype : 'fieldset',
                    autoHeight : true,
                    title : this.i18n._("Days of Week"),
                    items : [{
                        cls: 'description',
                        border : false,
                        html : this.i18n._("The days of the week you would like this policy active.")
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
                        cls: 'description',
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
            var i,j;
            if(rackList.length==0) {
                Ext.MessageBox.alert(i18n._("Failed"), this.i18n._("There must always be at least one available rack."));
                return false;
            }
            for(i=0;i<rackList.length;i++) {
                for(j=i+1;j<rackList.length;j++) {
                    if(rackList[i].name==rackList[j].name) {
                        Ext.MessageBox.alert(i18n._("Failed"), String.format(this.i18n._("The rack named {0} already exists."),rackList[i].name));
                        return false;
                    }
                }
                if ( this.isChild( rackList[i].id, rackList[i].parentId )) {
                    Ext.MessageBox.alert(i18n._("Failed"), String.format(this.i18n._("The rack named {0} cannot have one its children as its parent."),rackList[i].name));
                    return false;
                }
                var parent = this.convertIDToRack(rackList[i].parentId);
                if ( parent != null && parent.deleted ) {
                    Ext.MessageBox.alert(i18n._("Failed"), String.format(this.i18n._("The rack named {0} cannot have a deleted parent."),rackList[i].name));
                    return false;
                }
            }
            var rulesList=this.gridRules.getFullSaveList();
            var rackDeletedList=this.gridRacks.getDeletedList();
            for(i=0;i<rulesList.length;i++) {
                if(rulesList[i].policy!=null) {
                    for(j=0;j<rackDeletedList.length;j++) {
                        if(rulesList[i].policy.id==rackDeletedList[j].id) {
                            Ext.MessageBox.alert(i18n._("Failed"), String.format(this.i18n._('The rack named {0} cannot be removed because it is currently being used in "Policy Rules".'),rackDeletedList[j].name));
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
            {
                try {
                    try {
                        delete rackDeletedList[i].parentId;
                        var result = rpc.nodeManager.nodeInstances(rackDeletedList[i]);
                    } catch (e) {
                        Ung.Util.rpcExHandler(e);
                    }
                    
                    if (result.list.length>0) {
                        
                        //                var isEmptyPolicy = rpc.nodeManager.isEmptyPolicy(rackDeletedList[i]);
                        //                if (!isEmptyPolicy) {
                        Ext.MessageBox.alert(i18n._("Failed"), String.format(this.i18n._("The rack named {0} cannot be removed because it is not empty.  Please remove all applications first."),rackDeletedList[i].name));
                        return false;
                    }
                } catch (e) {
                    var message = e.message;
                    if (message == null || message == "Unknown") {
                        message = i18n._("Please Try Again");
                    }
                    
                    Ext.MessageBox.alert(i18n._("Failed"), message);
                    return false;
                }
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
            var validateData = {
                map : {},
                javaClass : "java.util.HashMap"
            };
            if (clientAddrList.length > 0) {
                validateData.map["CLIENT_ADDR"] = {"javaClass" : "java.util.ArrayList", list : clientAddrList};
            }
            if (serverAddrList.length > 0) {
                validateData.map["SERVER_ADDR"] = {"javaClass" : "java.util.ArrayList", list : serverAddrList};
            }
            if (serverPortList.length > 0) {
                validateData.map["SERVER_PORT"] = {"javaClass" : "java.util.ArrayList", list : serverPortList};
            }
            if (Ung.Util.hasData(validateData.map)) {
                try {
                    try {
                        var result = this.getPolicyManagerValidator().validate(validateData);
                    } catch (e) {
                        Ung.Util.rpcExHandler(e);
                    }
                    
                    if (!result.valid) {
                        var errorMsg = "";
                        switch (result.errorCode) {
                            case 'INVALID_CLIENT_ADDR' :
                                errorMsg = this.i18n._("Invalid address specified for Client Address") + ": " + result.cause;
                            break;
                            case 'INVALID_SERVER_ADDR' :
                                errorMsg = this.i18n._("Invalid address specified for Server Address") + ": " + result.cause;
                            break;
                            case 'INVALID_SERVER_PORT' :
                                errorMsg = this.i18n._("Invalid port specified for Server Port") + ": " + result.cause;
                            break;
                            default :
                                errorMsg = this.i18n._(result.errorCode) + ": " + result.cause;
                        }
                        Ext.MessageBox.alert(this.i18n._("Validation failed"), errorMsg);
                        return false;
                    }
                } catch (e) {
                    var message = e.message;
                    if (message == null || message == "Unknown") {
                        message = i18n._("Please Try Again");
                    }
                    
                    Ext.MessageBox.alert(i18n._("Failed"), message);
                    return false;
                }
            }
            return true;
        },
        
        saveAction : function()
        {
            this.commitSettings(this.completeSaveAction.createDelegate(this));
        },
        completeSaveAction : function()
        {
            Ext.MessageBox.hide();
            this.closeWindow();
            main.loadPolicies.defer(1,main);
        },
        applyAction : function()        
        {
            this.commitSettings(this.reloadSettings.createDelegate(this));
        },
        reloadSettings : function()
        {
            this.getPolicyConfiguration(true);

            this.rackDatabase = this.buildRackDatabase( this.getPolicyConfiguration().policies.list );
            this.gridRacks.clearChangedData();
            this.gridRacks.store.loadData(this.getPolicyConfiguration().policies,false);
            
            this.gridRules.clearChangedData();
            this.gridRules.store.loadData(this.getPolicyConfiguration().userPolicyRules,false);

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

            this.policyStore.loadData(this.policyStoreData);

            main.loadPolicies.defer(1,main);

            Ext.MessageBox.hide();
        },

        // save function
        commitSettings : function(callback)
        {
            if (!this.validate()) {
                return;
            }
            
            var saveList = this.gridRacks.getFullSaveList();
            for ( var c = 0 ; c < saveList.length ; c++ ) {
                if ( saveList[c].parentId != null ) {
                    saveList[c].parentId = parseInt( saveList[c].parentId );
                }
            }

            this.getPolicyConfiguration().policies.list=saveList;
                        
            this.getPolicyConfiguration().userPolicyRules.list=this.gridRules.getFullSaveList();
            Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
            rpc.policyManager.setPolicyConfiguration(function(result, exception) {
                if(Ung.Util.handleException(exception)) return;
                
                callback();
            }.createDelegate(this), this.getPolicyConfiguration());
        },
        // save function
        closeWindow : function() {
            Ung.PolicyManager.superclass.closeWindow.call(this);
            if(this.fnCallback) {
                this.fnCallback.call();
            }
        },
        isDirty : function() {
            return this.gridRacks.isDirty() || this.gridRules.isDirty();
        },

        /* A map of a random ID -> rack.  This is used to simplify parent / child management. */
        buildRackDatabase : function( racks )
        {
            var rackDatabase = {}, c = 0;

            for ( c =0 ; c < racks.length ; c++ ) {
                this.addRackToDatabase( rackDatabase, racks[c] );
            }
                        
            return rackDatabase;
        },
        removeRackFromDatabase : function( rackDatabase, id )
        {
            var rack = rackDatabase[id],
                isRackDeleted = false;

            if ( rack == null ) {
                return;
            }

            /* Change it to a policy that is marked as deleted.  This
             * is so the user has a change to update the parent policy
             * but still see the names. */
            if(rack.name.match("^Deleted \(.*?\)")){
                isRackDeleted = true;
            }            
            rackDatabase[id] = {
                name : isRackDeleted == true ? rack.name : String.format( this.i18n._( "Deleted ({0})"), rack.name ),
                deleted : true,
                rack : rack
            };

        },
        addRackToDatabase : function( rackDatabase, rack )
        {
            rackDatabase[rack.id] = rack;
        },
        
        onBeforeEditRackCell : function( e )
        {
            if ( e.grid.getColumnModel().getColumnId( e.column ) != "parent-rack" ) {
                return;
            }
            
            this.updateParentRackStore( this.gridParentRackCombo, e.record );
        },
        onAfterEditRackCell : function( e )
        {
            var columnID = e.grid.getColumnModel().getColumnId( e.column );

            /* Change the names of the other columns */
            if ( columnID == "rack-name" ) {
                var id = e.record.get( "id" );
                if ( id == null ) {
                    return;
                }
                
                var policy = this.rackDatabase[id];
                if ( policy == null ) {
                    return;
                }
                policy.name = e.value;
                
                /* After edit is after editing, but before updating the store. */
                this.gridRacks.getView().refresh(false);
            }
        },
        updateParentRackStore : function( combo, record )
        {
            var storeData = [[0,this.i18n._("None")]], id = record.get( "id" ), c = 0, rack;

            /* Default rack can't have a parent. */
            if ( record.get( "default" )) {
                combo.store.loadData(storeData);
                return;
            }

            var parent = record.get("parentId");
            
            for ( rackID in this.rackDatabase ) {
                if ( rackID == id ) {
                    continue;
                }
                
                var rack = this.rackDatabase[rackID];
                
                if ( rack.deleted ) {
                    continue;
                }
                
                /* Have to remove children */
                if ( this.isChild( id, rackID )) {
                    continue;
                }
                
                /* Just indicate that the value has been found */
                if ( rackID == parent ) {
                    parent = null;
                }
                
                storeData.push([rackID,rack.name]);
            }

            combo.store.loadData(storeData);
            
            if ( parent !== null ) {
                record.set("parent",0);
            }
        },

        fetchRackFromStore : function( id )
        {
            if ( id == null ) {
                return null;
            }

            var racks = this.gridRacks.store.query( "id", id );
            if ( racks == null || racks.getCount() == 0 ) {
                return null;
            }
            
            return racks.first().data;
        },
        
        /* Return true if rackB is a child of rackA */
        isChild : function( idA, idB )
        {
            var rackA = this.fetchRackFromStore( idA );
            var rackB = this.fetchRackFromStore( idB );
            
            if ( rackA == null ) {
                /* this happens on a new rack, (it can't have children, so this doesn't matter) */
                return false;
            }

            while ( true ) {
                if ( rackB == null ) {
                    return false;
                }

                if ( rackB.id == rackA.id ) {
                    return true;
                }

                if ( rackB.parentId == null ) {
                    return false;
                }

                rackB = this.fetchRackFromStore( rackB.parentId );
            }
        },

        convertIDToRack : function( id )
        {
            var rack = null;
            if ( id == null ) {
                return null;
            }

            return this.rackDatabase[id];
        }
    });
}
