if (!Ung.hasResource["Ung.UserDirectory"]) {
    Ung.hasResource["Ung.UserDirectory"] = true;

    Ung.UserDirectory = Ext.extend(Ung.ConfigWin, {
        panelActiveDirectoryConnector : null,
        panelLocalDirectory : null,
        gridUsers : null,
        initComponent : function() {
            this.breadcrumbs = [{
                title : i18n._("Configuration"),
                action : function() {
                    this.cancelAction();
                }.createDelegate(this)
            }, {
                title : i18n._('User Directory')
            }];
            Ung.UserDirectory.superclass.initComponent.call(this);
        },
        onRender : function(container, position) {
            // call superclass renderer first
            Ung.UserDirectory.superclass.onRender.call(this, container, position);
            this.initSubCmps.defer(1, this);
            // builds the 2 tabs
        },
        initSubCmps : function() {
            this.buildActiveDirectoryConnector();
            this.buildLocalDirectory();
            // builds the tab panel with the tabs
            var pageTabs = [this.panelActiveDirectoryConnector, this.panelLocalDirectory];
            this.buildTabPanel(pageTabs);
            this.tabs.activate(this.panelActiveDirectoryConnector);
        },
        getAddressBookSettings : function(forceReload) {
            if (forceReload || this.rpc.addressBookSettings === undefined) {
                this.rpc.addressBookSettings = main.getAppAddressBook().getAddressBookSettings();
            }
            return this.rpc.addressBookSettings;
        },
        getADRepositorySettings : function(forceReload) {
            if (forceReload || this.rpc.ADRepositorySettings === undefined) {
                this.rpc.ADRepositorySettings = this.getAddressBookSettings().ADRepositorySettings;
            }
            return this.rpc.ADRepositorySettings;
        },
        buildActiveDirectoryConnector : function() {
            var enableAD = this.getAddressBookSettings().addressBookConfiguration == 'AD_AND_LOCAL' ? true : false;
            this.panelActiveDirectoryConnector = new Ext.Panel({
                name : 'Active Directory (AD) Connector',
                parentId : this.getId(),
                title : this.i18n._('Active Directory (AD) Connector'),
                layout : "form",
                bodyStyle : 'padding:5px 5px 0px 5px;',
                autoScroll : true,
                items : [{
                    title : this.i18n._('Active Directory (AD) Server'),
                    name : 'Active Directory (AD) Server',
                    xtype : 'fieldset',
                    autoHeight : true,
                    items: [{
                        html : i18n.sprintf(this.i18n._('This alows your server to connect to an %sActive Directory Server%s in order to recognize various users for use in reporting, firewall, router, policies, etc.'),'<b>','</b>'),
                        border : false
                    }, {
                        html : "<br>",
                        border : false
                    }, {
                        xtype : 'radio',
                        boxLabel : '<b>'+this.i18n._('Disabled')+'</b>', 
                        hideLabel : true,
                        name : 'enableAD',
                        checked : this.getAddressBookSettings().addressBookConfiguration != 'AD_AND_LOCAL',
                        listeners : {
                            "check" : {
                                fn : function(elem, checked) {
                                    if (checked) {
                                        this.getAddressBookSettings().addressBookConfiguration = 'LOCAL_ONLY';
                                        Ext.getCmp('adConnector_LDAPHost').disable();
                                        Ext.getCmp('adConnector_LDAPPort').disable();
                                        Ext.getCmp('adConnector_superuser').disable();
                                        Ext.getCmp('adConnector_superuserPass').disable();
                                        Ext.getCmp('adConnector_domain').disable();
                                        Ext.getCmp('adConnector_OUFilter').disable();
                                        Ext.getCmp('adConnector_ActiveDirectoryTest').disable();
                                        Ext.getCmp('adConnector_ActiveDirectoryUsers').disable();
                                        Ext.getCmp('adConnector_ADUsersTextArea').disable();
                                    }
                                    else {
                                        this.getAddressBookSettings().addressBookConfiguration = 'AD_AND_LOCAL';
                                        Ext.getCmp('adConnector_LDAPHost').enable();
                                        Ext.getCmp('adConnector_LDAPPort').enable();
                                        Ext.getCmp('adConnector_superuser').enable();
                                        Ext.getCmp('adConnector_superuserPass').enable();
                                        Ext.getCmp('adConnector_domain').enable();
                                        Ext.getCmp('adConnector_OUFilter').enable();
                                        Ext.getCmp('adConnector_ActiveDirectoryTest').enable();
                                        Ext.getCmp('adConnector_ActiveDirectoryUsers').enable();
                                        Ext.getCmp('adConnector_ADUsersTextArea').enable();
                                    }
                                }.createDelegate(this)
                            }
                        }
                    }, {
                        xtype : 'radio',
                        boxLabel : '<b>'+this.i18n._('Enabled')+'</b>', 
                        hideLabel : true,
                        name : 'enableAD',
                        checked : this.getAddressBookSettings().addressBookConfiguration == 'AD_AND_LOCAL'
                    }, {
                        border: false,
                        layout:'column',
                        items: [ { 
                            border: false,
                            columnWidth:.7,
                            layout: 'form',
                            items: [ {
		                        xtype : 'textfield',
		                        name : 'AD Server IP or Hostname',
		                        fieldLabel : this.i18n._('AD Server IP or Hostname'),
		                        id : 'adConnector_LDAPHost',
		                        labelStyle : 'text-align: right; width: 250px;',
		                        width : 200,
		                        value : this.getADRepositorySettings().LDAPHost
		                    }, {
		                        xtype : 'textfield',
		                        name : 'Port',
		                        fieldLabel : this.i18n._('Port'),
		                        id : 'adConnector_LDAPPort',
		                        labelStyle : 'text-align: right; width: 250px;',
		                        width : 80,
		                        value : this.getADRepositorySettings().LDAPPort,
		                        vtype : "port"
		                    }, {
		                        xtype : 'textfield',
		                        name : 'Authentication Login',
		                        fieldLabel : this.i18n._('Authentication Login'),
		                        id : 'adConnector_superuser',
		                        labelStyle : 'text-align: right; width: 250px;',
		                        width : 150,
		                        value : this.getADRepositorySettings().superuser
		                    }, {
		                        xtype : 'textfield',
		                        inputType: 'password',
		                        name : 'Authentication Password',
		                        fieldLabel : this.i18n._('Authentication Password'),
		                        id : 'adConnector_superuserPass',
		                        labelStyle : 'text-align: right; width: 250px;',
		                        width : 150,
		                        value : this.getADRepositorySettings().superuserPass
		                    }]
                        }, { 
                            border: false,
                            columnWidth:.3,
                            layout: 'form',
                            items: [ new Ext.Panel({
                                buttonAlign : 'right',
                                bodyStyle : "padding-top: 35px",
                                footer : false,
                                border : false,
                                buttons: [new Ext.Button({
						            name: 'Help',
						            iconCls: 'iconHelp',
						            text: i18n._('Help'),
						            handler: function() {
						                var helpLink = "http://www.untangle.com/docs/get.php?" + "version=" + rpc.version + "&source=active_directory_config";
						                window.open(helpLink);
						            }
						        })]
                            })]
                        }]
                    }, {
                        html : "<hr>",
                        border : false
                    }, {
                        xtype : 'textfield',
                        name : 'Active Directory Domain',
                        fieldLabel : this.i18n._('Active Directory Domain'),
                        id : 'adConnector_domain',
                        labelStyle : 'text-align: right; width: 250px;',
                        width : 200,
                        value : this.getADRepositorySettings().domain
                    }, {
                        xtype : 'textfield',
                        name : 'Active Directory Organization (optional)',
                        fieldLabel : this.i18n._('Active Directory Organization (optional)'),
                        id : 'adConnector_OUFilter',
                        labelStyle : 'text-align: right; width: 250px;',
                        width : 200,
                        value : this.getADRepositorySettings().OUFilter
                    }, {
                        html : "<hr>",
                        border : false
                    }, {
                        html : i18n.sprintf(this.i18n._('The %sActive Directory Test%s can be used to test that your settings above are correct.'),'<b>','</b>'),
                        border : false
                    }, new Ext.Panel({
                        buttonAlign : 'center',
                        footer : false,
                        border : false,
                        buttons: [{
	                        xtype : 'button',
	                        text : this.i18n._('Active Directory Test'),
	                        iconCls : 'adTestIcon',
	                        id : 'adConnector_ActiveDirectoryTest',
	                        name : 'Active Directory Test',
	                        handler : function() {
	                            this.panelActiveDirectoryConnector.onADTestClick();
	                        }.createDelegate(this)
                        }]
                    }), {
                        html : "<hr>",
                        border : false
                    }, new Ext.Panel({
                        buttonAlign : 'center',
                        footer : false,
                        border : false,
                        buttons: [{
	                        xtype : 'button',
	                        text : this.i18n._('AD Login Script'),
	                        name : 'AD Login Script',
	                        handler : function() {
	                            this.panelActiveDirectoryConnector.onADLoginScriptClick();
	                        }.createDelegate(this)
	                    }]
                    }), {
                        html : "<hr>",
                        border : false
                    }, {
                        border: false,
                        layout:'column',
                        items: [ { 
                            border: false,
                            columnWidth:.3,
                            layout: 'form',
                            items: [ new Ext.Panel({
		                        buttonAlign : 'right',
                                bodyStyle : "padding-top: 90px",
		                        footer : false,
		                        border : false,
		                        buttons: [{
		                            xtype : 'button',
		                            text : this.i18n._('Active Directory Users'),
			                        id : 'adConnector_ActiveDirectoryUsers',
			                        name : 'Active Directory Users',
			                        handler : function() {
			                            this.panelActiveDirectoryConnector.onADUsersClick();
			                        }.createDelegate(this)
		                        }]
		                    })]
                        }, { 
                            border: false,
                            columnWidth:.7,
                            layout: 'form',
                            items: [ {
		                        xtype : 'textarea',
		                        name : 'Active Directory Users Text Area',
		                        id : 'adConnector_ADUsersTextArea',
		                        hideLabel : true,
		                        readOnly : true,
		                        width : 300,
		                        height : 200
		                    }]
                        }]
                    }]
                }],
                
                onADTestClick : function() {
					Ext.MessageBox.show({
					   title : this.i18n._('Active Directory Test'),
					   msg : this.i18n._('Active Directory Test:'),
					   buttons : Ext.Msg.CANCEL,
                       modal : true,
                       wait : true,
                       waitConfig: {interval: 100},
                       progressText : this.i18n._('Testing...'),
                       width : 300
					});
                    
                    var message = main.getAppAddressBook().getStatus( function(result, exception) {
	                    if (exception) {
	                        Ext.MessageBox.alert(i18n._("Failed"), exception.message);
	                        return;
	                    }
                        var message = result.ADWorking == true ? this.i18n._('Success!  Your settings work.') : this.i18n._('Failure!  Your settings are not correct.');

                        Ext.MessageBox.show({
	                       title : this.i18n._('Active Directory Test'),
	                       msg : this.i18n._('Active Directory Test:'),
	                       buttons : Ext.Msg.CANCEL,
	                       modal : true,
	                       progress : true,
	                       waitConfig: {interval: 100},
	                       progressText : message,
	                       width : 300
	                    });
                    }.createDelegate(this));
                }.createDelegate(this),
                
                onADLoginScriptClick : function() {
	                rpc.adminManager.generateAuthNonce(function (result, exception) {
	                    if(exception) { 
                            Ext.MessageBox.alert(i18n._("Failed"),exception.message); 
                            return;
                        }
	                    var loginScriptUrl = "../adpb/?" + result;
                        window.open(loginScriptUrl);
	                }.createDelegate(this));
                },
                
                onADUsersClick : function() {
	                var userEntries = main.getAppAddressBook().getUserEntries('MS_ACTIVE_DIRECTORY').list;
                    
                    var usersList = "";
		            for(var i=0; i<userEntries.length; i++) {
			            if(userEntries[i] == null) {
                            usersList += this.i18n._('[any]');
                            continue;
                        }
                        
                        var repository = this.i18n._('UNKNOWN');
                        if(userEntries[i].storedIn == 'MS_ACTIVE_DIRECTORY') {
                            repository = this.i18n._('Active Directory');
                        }
                        else if(userEntries[i].storedIn == 'LOCAL_DIRECTORY') {
                            repository = this.i18n._('Local');
                        }

                        var uid = userEntries[i].UID != null ? userEntries[i].UID : this.i18n._('[any]');
		                usersList += ( uid + " (" + repository + ")" + "\r\n");
		            }
                    
                    Ext.getCmp('adConnector_ADUsersTextArea').setValue(usersList);
                }.createDelegate(this)                
            });
        },
        buildLocalDirectory : function() {
            var storeData=main.getAppAddressBook().getLocalUserEntries().list;
            for(var i=0; i<storeData.length; i++) {
                storeData[i].id = i;
                storeData[i].password = "***UNCHANGED***";
            }
            
            this.panelLocalDirectory = new Ext.Panel({
                name : 'Local Directory',
                parentId : this.getId(),
                title : this.i18n._('Local Directory'),
                layout : "form",
                bodyStyle : 'padding:5px 5px 0px 5px;',
                items : [this.gridUsers = new Ung.EditorGrid({
                        name : 'Users',
                        settingsCmp : this,
                        height : 500,
                        paginated : false,
                        emptyRow : {
                            "UID" : this.i18n._('[no ID/login]'),
                            "firstName" : this.i18n._('[no name]'),
                            "lastName" : this.i18n._('[no name]'),
                            "email" : this.i18n._('[no email]'),
                            "password" : "",
                            "javaClass" : "com.untangle.uvm.addrbook.UserEntry"
                        },
                        recordJavaClass : "com.untangle.uvm.addrbook.UserEntry",
                        data : storeData,
                        dataRoot: null,
                        fields : [{
                            name : 'id'
                        }, {
                            name : 'UID'
                        }, {
                            name : 'firstName'
                        }, {
                            name : 'lastName'
                        }, {
                            name : 'email'
                        }, {
                            name : 'password'
                        }, {
                            name : 'storedIn'
                        }, {
                            name : 'javaClass'
                        }],
                        columns : [{
                            id : 'UID',
                            header : this.i18n._("user/login ID"),
                            width : 100,
                            dataIndex : 'UID',
		                    editor : new Ext.form.TextField({
		                        allowBlank : false
		                    })
                        }, {
                            id : 'firstName',
                            header : this.i18n._("first name"),
                            width : 100,
                            dataIndex : 'firstName',
                            editor : new Ext.form.TextField({
                                allowBlank : false
                            })
                        }, {
                            id : 'lastName',
                            header : this.i18n._("last name"),
                            width : 100,
                            dataIndex : 'lastName',
                            editor : new Ext.form.TextField({
                            })
                        }, {
                            id : 'email',
                            header : this.i18n._("email address"),
                            width : 250,
                            dataIndex : 'email',
                            editor : new Ext.form.TextField({
                            })
                        }, {
                            id : 'password',
                            header : this.i18n._("password"),
                            width : 150,
                            dataIndex : 'password',
                            editor : new Ext.form.TextField({
                                inputType: 'password'
                            }),
		                    renderer : function(value, metadata, record) {
                                var result = "";
                                for(var i=0; value != null && i<value.length; i++) {
                                    result = result + "*";
                                }
                                return result;
		                    }
                        }],
                        sortField : 'UID',
                        columnsDefaultSortable : true,
                        autoExpandColumn : 'email',
                        rowEditorInputLines : [new Ext.form.TextField({
                            name : "User/Login ID",
                            dataIndex: "UID",
                            fieldLabel : this.i18n._("User/Login ID"),
                            labelStyle : 'width: 80px;',
                            allowBlank : false,
                            width : 100
                        }), new Ext.form.TextField({
                            name : "First Name",
                            dataIndex: "firstName",
                            fieldLabel : this.i18n._("First Name"),
                            labelStyle : 'width: 80px;',
                            allowBlank : false,
                            width : 100
                        }), new Ext.form.TextField({
                            name : "Last Name",
                            dataIndex: "lastName",
                            fieldLabel : this.i18n._("Last Name"),
                            labelStyle : 'width: 80px;',
                            width : 100
                        }), new Ext.form.TextField({
                            name : "Email Address",
                            dataIndex: "email",
                            fieldLabel : this.i18n._("Email Address"),
                            labelStyle : 'width: 80px;',
                            width : 250
                        }), new Ext.form.TextField({
                            inputType: 'password',
                            name : "Password",
                            dataIndex: "password",
                            fieldLabel : this.i18n._("Password"),
                            labelStyle : 'width: 80px;',
                            width : 150
                        })]
                    })
                ]
           });
        },
        
        validateClient : function() {
            return  this.validateLocalDirectoryUsers() && this.validateADConnectorSettings();
        },
        
        //validate AD connector settings
        validateADConnectorSettings : function() {
            if(this.getAddressBookSettings().addressBookConfiguration == 'AD_AND_LOCAL') {
                var hostCmp = Ext.getCmp('adConnector_LDAPHost');
                var portCmp = Ext.getCmp('adConnector_LDAPPort');
                var loginCmp = Ext.getCmp('adConnector_superuser');
                var passwordCmp = Ext.getCmp('adConnector_superuserPass');
                var domainCmp = Ext.getCmp('adConnector_domain');
                var orgCmp = Ext.getCmp('adConnector_OUFilter');

                //validate port
	            if (!portCmp.isValid()) {
                    Ext.MessageBox.alert(this.i18n._('Warning'), i18n.sprintf(this.i18n._("The port must be an integer number between %d and %d."), 1, 65535),
	                    function () {
	                        this.tabs.activate(this.panelActiveDirectoryConnector);
	                        portCmp.focus(true);
	                    }.createDelegate(this) 
	                );
	                return false;
	            }
                
                // CHECK THAT BOTH PASSWORD AND LOGIN ARE FILLED OR UNFILLED
                if (loginCmp.getValue().length > 0 && passwordCmp.getValue().length == 0) {
                    Ext.MessageBox.alert(this.i18n._('Warning'), this.i18n._('A "Password" must be specified if a "Login" is specified.'),
                        function () {
                            this.tabs.activate(this.panelActiveDirectoryConnector);
                            passwordCmp.focus(true);
                        }.createDelegate(this) 
                    );
                    return false;
                }
                else if(loginCmp.getValue().length == 0 && passwordCmp.getValue().length > 0) {
                    Ext.MessageBox.alert(this.i18n._('Warning'), this.i18n._('A "Login" must be specified if a "Password" is specified.'),
                        function () {
                            this.tabs.activate(this.panelActiveDirectoryConnector);
                            loginCmp.focus(true);
                        }.createDelegate(this) 
                    );
                    return false;
                }
                
                // CHECK THAT IF EITHER LOGIN OR PASSWORD ARE FILLED, A HOSTNAME IS GIVEN
                if (loginCmp.getValue().length > 0 && passwordCmp.getValue().length > 0 && hostCmp.getValue().length == 0) {
                    Ext.MessageBox.alert(this.i18n._('Warning'), this.i18n._('A "Hostname" must be specified if "Login" or "Password" are specified.'),
                        function () {
                            this.tabs.activate(this.panelActiveDirectoryConnector);
                            hostCmp.focus(true);
                        }.createDelegate(this) 
                    );
                    return false;
                }

                // CHECK THAT A DOMAIN IS SUPPLIED
                if (domainCmp.getValue().length == 0) {
                    Ext.MessageBox.alert(this.i18n._('Warning'), this.i18n._('A "Search Base" must be specified.'),
                        function () {
                            this.tabs.activate(this.panelActiveDirectoryConnector);
                            domainCmp.focus(true);
                        }.createDelegate(this) 
                    );
                    return false;
                }
                
                // SAVE SETTINGS
                this.getAddressBookSettings().ADRepositorySettings.LDAPHost = hostCmp.getValue();
                this.getAddressBookSettings().ADRepositorySettings.LDAPPort = portCmp.getValue();
                this.getAddressBookSettings().ADRepositorySettings.superuser = loginCmp.getValue();
                this.getAddressBookSettings().ADRepositorySettings.superuserPass = passwordCmp.getValue();
                this.getAddressBookSettings().ADRepositorySettings.domain = domainCmp.getValue();
                this.getAddressBookSettings().ADRepositorySettings.OUFilter = orgCmp.getValue();
            }
            return true;
        },

        //validate local directory users
        validateLocalDirectoryUsers : function() {
            var listUsers = this.gridUsers.getFullSaveList();
            
            for(var i=0; i<listUsers.length;i++) {
                // verify that the login name is not duplicated
                for(var j=i+1; j<listUsers.length;j++) {
                    if (listUsers[i].UID == listUsers[j].UID) {
                        Ext.MessageBox.alert(this.i18n._('Warning'), i18n.sprintf(this.i18n._('The login name "%s" at row %d has already been taken.'), listUsers[j].UID, j+1),
                            function () {
                                this.tabs.activate(this.panelLocalDirectory);
                            }.createDelegate(this) 
                        );
                        return false;
                    }
                }
                // first name contains no spaces
                if (listUsers[i].firstName.indexOf(" ") != -1) {
                    Ext.MessageBox.alert(this.i18n._('Warning'), i18n.sprintf(this.i18n._('The first name at row %d must not contain any space characters.'), i+1),
                        function () {
                            this.tabs.activate(this.panelLocalDirectory);
                        }.createDelegate(this) 
                    );
                    return false;
                }
                // last name contains no spaces
                if (listUsers[i].lastName.indexOf(" ") != -1) {
                    Ext.MessageBox.alert(this.i18n._('Warning'), i18n.sprintf(this.i18n._('The last name at row %d must not contain any space characters.'), i+1),
                        function () {
                            this.tabs.activate(this.panelLocalDirectory);
                        }.createDelegate(this) 
                    );
                    return false;
                }
                // the password is at least one character
                if (listUsers[i].password.length == 0) {
                    Ext.MessageBox.alert(this.i18n._('Warning'), i18n.sprintf(this.i18n._('The password at row %d must be at least 1 character long.'), i+1),
                        function () {
                            this.tabs.activate(this.panelLocalDirectory);
                        }.createDelegate(this) 
                    );
                    return false;
                }
                // the password contains no spaces
                if (listUsers[i].password.indexOf(" ") != -1) {
                    Ext.MessageBox.alert(this.i18n._('Warning'), i18n.sprintf(this.i18n._('The password at row %d must not contain any space characters.'), i+1),
                        function () {
                            this.tabs.activate(this.panelLocalDirectory);
                        }.createDelegate(this) 
                    );
                    return false;
                }
            }
            
            return true;
        },
        
        // save function
        saveAction : function() {
            if (this.validate()) {
                this.saveSemaphore = 2;
                Ext.MessageBox.show({
                   title : this.i18n._('Please wait'),
                   msg : this.i18n._('Saving...'),
                   modal : true,
                   wait : true,
                   waitConfig: {interval: 100},
                   progressText : " ",
                   width : 200
                });
                //save AD connector settings
                main.getAppAddressBook().setAddressBookSettings(function(result, exception) {
                    if (exception) {
                        Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                        return;
                    }
                    // exit settings screen
                    this.afterSave();
                }.createDelegate(this), this.getAddressBookSettings() );
                //save local users            
                main.getAppAddressBook().setLocalUserEntries(function(result, exception) {
                    if (exception) {
                        Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                        return;
                    }
                    // exit settings screen
                    this.afterSave();
                }.createDelegate(this), this.gridUsers ? {javaClass:"java.util.ArrayList",list:this.gridUsers.getFullSaveList()} : null);
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