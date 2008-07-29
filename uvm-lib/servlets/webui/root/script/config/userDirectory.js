if (!Ung.hasResource["Ung.UserDirectory"]) {
    Ung.hasResource["Ung.UserDirectory"] = true;

    Ung.UserDirectory = Ext.extend(Ung.ConfigWin, {
        panelActiveDirectoryConnector : null,
        panelLocalDirectory : null,
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
        buildActiveDirectoryConnector : function() {
            this.panelActiveDirectoryConnector = new Ext.Panel({
                name : 'Active Directory (AD) Connector',
                parentId : this.getId(),
                title : this.i18n._('Active Directory (AD) Connector'),
                layout : "form",
                bodyStyle : 'padding:5px 5px 0px 5px;',
                items : [{
                    title : this.i18n._('Active Directory (AD) Server'),
                    name : 'Active Directory (AD) Server',
                    xtype : 'fieldset',
                    autoHeight : true,
                    items: [{
                        html : i18n.sprintf(this.i18n._('This alows your server to connect to an %sActive Directory Server%s in order to recognize various users for use in reporting, firewall, router, policies, etc.'),'<b>','</b>'),
                        border : false
                    }, {
                        html : "<br",
                        border : false
                    }, {
                        xtype : 'radio',
                        boxLabel : this.i18n._('<b>Disabled</b>'), 
                        hideLabel : true,
                        name : 'enabled',
                        checked : false,
                        listeners : {
                            "check" : {
                                fn : function(elem, checked) {
                                }.createDelegate(this)
                            }
                        }
                    }, {
                        xtype : 'radio',
                        boxLabel : this.i18n._('<b>Enabled</b>'), 
                        hideLabel : true,
                        name : 'enabled',
                        checked : true,
                        listeners : {
                            "check" : {
                                fn : function(elem, checked) {
                                }.createDelegate(this)
                            }
                         }
                    }]
                }]
            });
        },
        buildLocalDirectory : function() {
            var storeData=main.getAppAddressBook().getLocalUserEntries().list;
            for(var i=0; i<storeData.length; i++) {
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
                        sortField : 'uid',
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
//            var companyNameCmp = Ext.getCmp('companyName');
//            if (!companyNameCmp.isValid()) {
//                Ext.MessageBox.alert(this.i18n._('Warning'), this.i18n._('You must fill out the company name.'),
//                    function () {
//                        this.tabs.activate(this.panelRegistration);
//                        companyNameCmp.focus(true);
//                    }.createDelegate(this) 
//                );
//                return false;
//            }
//            this.getRegistrationInfo().companyName = companyNameCmp.getValue();
//
//            var firstNameCmp = Ext.getCmp('firstName');
//            if (!firstNameCmp.isValid()) {
//                Ext.MessageBox.alert(this.i18n._('Warning'), this.i18n._('You must fill out your first name.'),
//                    function () {
//                        this.tabs.activate(this.panelRegistration);
//                        firstNameCmp.focus(true);
//                    }.createDelegate(this) 
//                );
//                return false;
//            }
//            this.getRegistrationInfo().companyName = companyNameCmp.getValue();
//
//            var lastNameCmp = Ext.getCmp('lastName');
//            if (!lastNameCmp.isValid()) {
//                Ext.MessageBox.alert(this.i18n._('Warning'), this.i18n._('You must fill out the last name.'),
//                    function () {
//                        this.tabs.activate(this.panelRegistration);
//                        lastNameCmp.focus(true);
//                    }.createDelegate(this) 
//                );
//                return false;
//            }
//            this.getRegistrationInfo().companyName = companyNameCmp.getValue();
//
//            var emailAddrCmp = Ext.getCmp('emailAddr');
//            if (!emailAddrCmp.isValid()) {
//                Ext.MessageBox.alert(this.i18n._('Warning'), this.i18n._('You must fill out the email address in the format "user@domain.com".'),
//                    function () {
//                        this.tabs.activate(this.panelRegistration);
//                        emailAddrCmp.focus(true);
//                    }.createDelegate(this) 
//                );
//                return false;
//            }
//            this.getRegistrationInfo().companyName = companyNameCmp.getValue();
//
//            var numSeatsCmp = Ext.getCmp('numSeats');
//            if (!numSeatsCmp.isValid()) {
//                Ext.MessageBox.alert(this.i18n._('Warning'), this.i18n._('You must fill out the number of computers protected by Untangle.'),
//                    function () {
//                        this.tabs.activate(this.panelRegistration);
//                        numSeatsCmp.focus(true);
//                    }.createDelegate(this) 
//                );
//                return false;
//            }
//            this.getRegistrationInfo().companyName = companyNameCmp.getValue();
//
//            var address1Cmp = Ext.getCmp('address1');
//            this.getRegistrationInfo().address1 = address1Cmp.getValue();
//            var address2Cmp = Ext.getCmp('address2');
//            this.getRegistrationInfo().address2 = address2Cmp.getValue();
//            var cityCmp = Ext.getCmp('city');
//            this.getRegistrationInfo().city = cityCmp.getValue();
//            var stateCmp = Ext.getCmp('state');
//            this.getRegistrationInfo().state = stateCmp.getValue();
//            var zipcodeCmp = Ext.getCmp('zipcode');
//            this.getRegistrationInfo().zipcode = zipcodeCmp.getValue();
//            var phoneCmp = Ext.getCmp('phone');
//            this.getRegistrationInfo().phone = phoneCmp.getValue();
            
            return true;
        },
        // save function
        saveAction : function() {
            if (this.validate()) {
                Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
                main.getAppAddressBook().setLocalUserEntries(function(result, exception) {
                    Ext.MessageBox.hide();
                    if (exception) {
                        Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                        return;
                    }
                    // exit settings screen
                    this.cancelAction();
                }.createDelegate(this), this.gridUsers ? {javaClass:"java.util.ArrayList",list:this.gridUsers.getFullSaveList()} : null);
            }
        }

    });

}