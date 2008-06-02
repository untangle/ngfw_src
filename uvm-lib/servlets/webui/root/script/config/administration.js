if (!Ung.hasResource["Ung.Administration"]) {
    Ung.hasResource["Ung.Administration"] = true;

    Ung.Administration = Ext.extend(Ung.ConfigWin, {
        panelAdministration : null,
        panelPublicAddress : null,
        panelCertificates : null,
        panelMonitoring : null,
        panelSkins : null,
        initComponent : function() {
            this.breadcrumbs = [{
                title : i18n._("Configuration"),
                action : function() {
                    this.cancelAction();
                }.createDelegate(this)
            }, {
                title : i18n._('Administration')
            }];
            Ung.Administration.superclass.initComponent.call(this);
        },

        onRender : function(container, position) {
            // call superclass renderer first
            Ung.Administration.superclass.onRender.call(this, container, position);
            this.initSubCmps.defer(1, this);
            // builds the 2 tabs
        },
        initSubCmps : function() {
            this.buildAdministration();
            this.buildPublicAddress();
            this.buildCertificates();
            this.buildMonitoring();
            this.buildSkins();
            // builds the tab panel with the tabs
            this.buildTabPanel([this.panelAdministration, this.panelPublicAddress, this.panelCertificates, this.panelMonitoring,
                    this.panelSkins]);
            this.tabs.activate(this.panelSkins);
            this.panelPublicAddress.disable();
            this.panelCertificates.disable();
            this.panelMonitoring.disable();

        },
        // get base settings object
        getSkinSettings : function(forceReload) {
            if (forceReload || this.rpc.skinSettings === undefined) {
                this.rpc.skinSettings = rpc.skinManager.getSkinSettings();
            }
            return this.rpc.skinSettings;
        },
        // get admin settings
        getAdminSettings : function(forceReload) {
            if (forceReload || this.rpc.adminSettings === undefined) {
                this.rpc.adminSettings = rpc.adminManager.getAdminSettings();
            }
            return this.rpc.adminSettings;
        },
        
        getTODOPanel : function(title) {
            return new Ext.Panel({
                title : this.i18n._(title),
                layout : "form",
                autoScroll : true,
                bodyStyle : 'padding:5px 5px 0px 5px;',
                items : [{
                    xtype : 'fieldset',
                    title : this.i18n._(title),
                    autoHeight : true,
                    items : [{
                        xtype : 'textfield',
                        fieldLabel : 'TODO',
                        name : 'todo',
                        allowBlank : false,
                        value : 'todo',
                        disabled : true
                    }]
                }]
            });
        },
        buildAdministration : function() {
            // read-only is a check column
            var readOnlyColumn = new Ext.grid.CheckColumn({
                header : this.i18n._("read-only"),
                dataIndex : 'readOnly',
                fixed : true
            });

            this.panelAdministration = new Ext.Panel({
                name : 'panelAdministration',
                // private fields
                parentId : this.getId(),

                title : this.i18n._('Administration'),
                layout : "form",
                bodyStyle : 'padding:5px 5px 0px 5px;',
                autoScroll : true,
                defaults : {
                    autoHeight : true
                },
                items : [new Ung.EditorGrid({
                    settingsCmp : this,
                    name : 'gridAdminAccounts',
//                    // the total records is set from the base settings
//                    // patternsLength field
//                    totalRecords : this.getBaseSettings().patternsLength,
                    emptyRow : {
                        "login" : this.i18n._("[no login]"),
                        "name" : this.i18n._("[no description]"),
                        "readOnly" : false,
                        "email" : this.i18n._("[no email]")
//                        ,
//                        "clearPassword" : ""
                    },
                    title : this.i18n._("Admin Accounts"),
                    // the column is autoexpanded if the grid width permits
                    autoExpandColumn : 'name',
                    recordJavaClass : "com.untangle.uvm.security.User",
//                    // this is the function used by Ung.RpcProxy to retrive data
//                    // from the server
//                    proxyRpcFn : this.getRpcNode().getPatterns,
                    
        store: new Ext.data.Store({
            data : this.getAdminSettings().users.set,
//            sortInfo : this.sortField ? {
//                field : this.sortField,
//                direction : "ASC"
//            } : null,
            reader : new Ext.data.JsonReader({
//                totalProperty : "totalRecords",
//                root : 'set',
                fields : [{
                        name : 'id'
                    }, {
                        name : 'login'
                    },
                    // this field is internationalized so a converter was
                    // added
                    {
                        name : 'name',
                        convert : function(v) {
                            return this.i18n._(v)
                        }.createDelegate(this)
                    }, {
                        name : 'readOnly'
                    }, {
                        name : 'email'
//                    }, {
//                        name : 'clearPassword'
                    }]
            })
//            ,

//            remoteSort : true,
//            getPageStart : function() {
//                if (this.lastOptions && this.lastOptions.params) {
//                    return this.lastOptions.params.start
//                } else {
//                    return 0;
//                }
//            },
//            listeners : {
//                "update" : {
//                    fn : function(store, record, operation) {
//                        this.updateChangedData(record, "modified");
//                    }.createDelegate(this)
//                },
//                "load" : {
//                    fn : function(store, records, options) {
//                        this.updateFromChangedData(records, options);
//                    }.createDelegate(this)
//                }
//            }
        }),
    // is grid paginated
    isPaginated : function() {
        return false;
    },
    // load a page
    loadPage : function(pageStart, callback, scope, arg) {
//            this.getStore().load({
//                callback : callback,
//                scope : scope,
//                arg : arg
//            });
    },
        
                    
                    // the list of fields
                    fields : [{
                        name : 'id'
                    }, {
                        name : 'login'
                    },
                    // this field is internationalized so a converter was
                    // added
                    {
                        name : 'name',
                        convert : function(v) {
                            return this.i18n._(v)
                        }.createDelegate(this)
                    }, {
                        name : 'readOnly'
                    }, {
                        name : 'email'
//                    }, {
//                        name : 'clearPassword'
                    }],
                    // the list of columns for the column model
                    columns : [{
                        id : 'id',
                        dataIndex : 'id',
                        hidden : true
                    }, {
                        id : 'login',
                        header : this.i18n._("login"),
                        width : 200,
                        dataIndex : 'login',
                        editor : new Ext.form.TextField({
                            allowBlank : false
                        })
                    }, {
                        id : 'name',
                        header : this.i18n._("description"),
                        width : 200,
                        dataIndex : 'name',
                        editor : new Ext.form.TextField({
                            allowBlank : false
                        })
                    }, readOnlyColumn, {
                        id : 'email',
                        header : this.i18n._("email"),
                        width : 200,
                        dataIndex : 'email',
                        editor : new Ext.form.TextField({
                            allowBlank : false
                        })
                    }
//                    , {
//                        id : 'password',
//                        header : this.i18n._("description"),
//                        width : 200,
//                        dataIndex : 'description',
//                        editor : new Ext.form.TextField({
//                            allowBlank : false
//                        })
//                    }
                    ],
                    sortField : 'login',
                    columnsDefaultSortable : true,
                    plugins : [readOnlyColumn],
                    // the row input lines used by the row editor window
                    rowEditorInputLines : [new Ext.form.TextField({
                        name : "login",
                        fieldLabel : this.i18n._("Login"),
                        allowBlank : false,
                        width : 200
                    }), new Ext.form.TextField({
                        name : "name",
                        fieldLabel : this.i18n._("Description"),
                        allowBlank : false,
                        width : 200
                    }), new Ext.form.Checkbox({
                        name : "readOnly",
                        fieldLabel : this.i18n._("Read-only")
                    }), new Ext.form.TextField({
                        name : "email",
                        fieldLabel : this.i18n._("Email"),
                        width : 200
//                    }), new Ext.form.TextField({
//                        name : "password",
//                        fieldLabel : this.i18n._("Password"),
//                        width : 200
                    })]
                })]
            })
        },
        buildPublicAddress : function() {
            this.panelPublicAddress = this.getTODOPanel("Public Address");
        },
        buildCertificates : function() {
            this.panelCertificates = this.getTODOPanel("Certificates");
        },
        buildMonitoring : function() {
            this.panelMonitoring = this.getTODOPanel("Monitoring");
        },
        buildSkins : function() {
            var skinsStore = new Ext.data.Store({
                proxy : new Ung.RpcProxy(rpc.skinManager.getSkinsList, false),
                reader : new Ung.JsonListReader({
                    root : 'list',
                    fields : ['skinName']
                })
            });

            this.panelSkins = new Ext.Panel({
                name : "panelSkins",
                // private fields
                parentId : this.getId(),
                title : this.i18n._('Skins'),
                layout : "form",
                bodyStyle : 'padding:5px 5px 0px 5px;',
                autoScroll : true,
                defaults : {
                    xtype : 'fieldset',
                    autoHeight : true,
                    buttonAlign : 'left'
                },
                items : [{
                    title : this.i18n._('Administration Skin'),
                    items : [{
                        bodyStyle : 'padding:0px 0px 5px 5px;',
                        border : false,
                        html : this.i18n._("This skin will used in the administration client")
                    }, {
                        xtype : 'combo',
                        name : "administrationClientSkin",
                        store : skinsStore,
                        displayField : 'skinName',
                        valueField : 'skinName',
                        forceSelection : true,
                        value : this.getSkinSettings().administrationClientSkin,
                        typeAhead : true,
                        mode : 'remote',
                        triggerAction : 'all',
                        listClass : 'x-combo-list-small',
                        selectOnFocus : true,
                        hideLabel : true,
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getSkinSettings().administrationClientSkin = newValue;
                                    Ext.MessageBox.alert(this.i18n._("Info"), this.i18n
                                            ._("Please note that you have to relogin after saving for the new skin to take effect."));
                                }.createDelegate(this)
                            }
                        }
                    }]
                }, {
                    title : this.i18n._('Block Page Skin'),
                    items : [{
                        bodyStyle : 'padding:0px 0px 5px 5px;',
                        border : false,
                        html : this.i18n._("This skin will used in the user pages like quarantine and block pages")
                    }, {
                        xtype : 'combo',
                        name : "userPagesSkin",
                        store : skinsStore,
                        displayField : 'skinName',
                        valueField : 'skinName',
                        forceSelection : true,
                        value : this.getSkinSettings().userPagesSkin,
                        typeAhead : true,
                        mode : 'remote',
                        triggerAction : 'all',
                        listClass : 'x-combo-list-small',
                        selectOnFocus : true,
                        hideLabel : true,
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getSkinSettings().userPagesSkin = newValue;
                                }.createDelegate(this)
                            }
                        }
                    }]
                }, {
                    title : this.i18n._('Upload New Skin'),
                    items : {
                        fileUpload : true,
                        xtype : 'form',
                        id : 'upload_skin_form',
                        url : 'upload',
                        border : false,
                        items : [{
                            fieldLabel : 'File',
                            name : 'file',
                            inputType : 'file',
                            xtype : 'textfield',
                            allowBlank : false
                        }, {
                            xtype : 'hidden',
                            name : 'type',
                            value : 'skin'
                        }]
                    },
                    buttons : [{
                        text : this.i18n._("Upload"),
                        handler : function() {
                            this.panelSkins.onUpload();
                        }.createDelegate(this)
                    }]
                }],
                onUpload : function() {
                    var prova = Ext.getCmp('upload_skin_form');
                    var cmp = Ext.getCmp(this.parentId);

                    var form = prova.getForm();
                    form.submit({
                        parentId : cmp.getId(),
                        waitMsg : cmp.i18n._('Please wait while your skin is uploaded...'),
                        success : function(form, action) {
                            skinsStore.load();
                            var cmp = Ext.getCmp(action.options.parentId);
                            Ext.MessageBox.alert(cmp.i18n._("Succeeded"), cmp.i18n._("Upload Skin Succeeded"));
                        },
                        failure : function(form, action) {
                            var cmp = Ext.getCmp(action.options.parentId);
                            if (action.result && action.result.msg) {
                                Ext.MessageBox.alert(cmp.i18n._("Failed"), cmp.i18n._(action.result.msg));
                            } else {
                                Ext.MessageBox.alert(cmp.i18n._("Failed"), cmp.i18n._("Upload Skin Failed"));
                            }
                        }
                    });
                }
            });
        },
        // save function
        saveAction : function() {
            if (this.validate()) {
                Ext.MessageBox.progress(i18n._("Please wait"), i18n._("Saving..."));
                rpc.skinManager.setSkinSettings(function(result, exception) {
                    Ext.MessageBox.hide();
                    if (exception) {
                        Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                        return;
                    }
                    // exit settings screen
                    this.cancelAction();
                }.createDelegate(this), this.getSkinSettings());
            }
        }

    });

}