if (!Ung.hasResource["Ung.LocalDirectory"]) {
    Ung.hasResource["Ung.LocalDirectory"] = true;

    Ung.LocalDirectory = Ext.extend(Ung.ConfigWin, {
        fnCallback:null,
        gridUsers : null,
        initComponent : function() {
            this.breadcrumbs = [{
                title : i18n._("Configuration"),
                action : function() {
                    this.cancelAction();
                }.createDelegate(this)
            }, {
                title : i18n._('Local Directory')
            }];
            this.buildLocalDirectory();
            // builds the tab panel with the tabs
            this.buildTabPanel([this.gridUsers]);
            this.tabs.activate(this.gridUsers);
            Ung.LocalDirectory.superclass.initComponent.call(this);
        },
        buildLocalDirectory : function() {
            var storeData=main.getLocalDirectory().getUsers().list;
            for(var i=0; i<storeData.length; i++) {
                storeData[i].password = "***UNCHANGED***";
            }
            
            this.gridUsers = new Ung.EditorGrid({
                name : 'Local Users',
                helpSource : 'local_directory',
                title : this.i18n._('Local Users'),
                hasImportExport: false, /* password not actually in grid - cant export */
                settingsCmp : this,
                height : 500,
                paginated : false,
                emptyRow : {
                    "username" : this.i18n._('[no ID/login]'),
                    "firstName" : this.i18n._('[no name]'),
                    "lastName" : this.i18n._('[no name]'),
                    "email" : this.i18n._('[no email]'),
                    "password" : "",
                    "javaClass" : "com.untangle.uvm.LocalDirectoryUser"
                },
                recordJavaClass : "com.untangle.uvm.LocalDirectoryUser",
                data : storeData,
                dataRoot: null,
                autoGenerateId: true,
                fields : [{
                    name : 'username'
                }, {
                    name : 'firstName'
                }, {
                    name : 'lastName'
                }, {
                    name : 'email'
                }, {
                    name : 'password'
                }, {
                    name : 'javaClass'
                }],
                columns : [{
                    id : 'username',
                    header : this.i18n._("user/login ID"),
                    width : 140,
                    dataIndex : 'username',
                    editor : new Ext.form.TextField({
                        allowBlank : false,
                        regex: /^[\w ]+$/,
                        regexText: this.i18n._("The field user/login ID can have only alphanumeric characters.")
                    })
                }, {
                    id : 'firstName',
                    header : this.i18n._("first name"),
                    width : 120,
                    dataIndex : 'firstName',
                    editor : new Ext.form.TextField({
                        allowBlank : false
                    })
                }, {
                    id : 'lastName',
                    header : this.i18n._("last name"),
                    width : 120,
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
                sortField : 'username',
                columnsDefaultSortable : true,
                autoExpandColumn : 'email',
                rowEditorInputLines : [new Ext.form.TextField({
                    name : "User/Login ID",
                    dataIndex: "username",
                    fieldLabel : this.i18n._("User/Login ID"),
                    allowBlank : false,
                    regex: /^[\w ]+$/,
                    regexText: this.i18n._("The field user/login ID can have only alphanumeric character."),
                    width : 100
                }), new Ext.form.TextField({
                    name : "First Name",
                    dataIndex: "firstName",
                    fieldLabel : this.i18n._("First Name"),
                    allowBlank : false,
                    width : 100
                }), new Ext.form.TextField({
                    name : "Last Name",
                    dataIndex: "lastName",
                    fieldLabel : this.i18n._("Last Name"),
                    width : 100
                }), new Ext.form.TextField({
                    name : "Email Address",
                    dataIndex: "email",
                    fieldLabel : this.i18n._("Email Address"),
                    width : 250
                }), new Ext.form.TextField({
                    inputType: 'password',
                    name : "Password",
                    dataIndex: "password",
                    fieldLabel : this.i18n._("Password"),
                    width : 150
                })]
            });

        },
        
        validateClient : function()
        {
            return  this.validateLocalDirectoryUsers();
        },
        
        //validate local directory users
        validateLocalDirectoryUsers : function() {
            var listUsers = this.gridUsers.getFullSaveList();
            
            for(var i=0; i<listUsers.length;i++) {
                // verify that the login name is not duplicated
                for(var j=i+1; j<listUsers.length;j++) {
                    if (listUsers[i].username == listUsers[j].username) {
                        Ext.MessageBox.alert(this.i18n._('Warning'), String.format(this.i18n._('The login name "{0}" at row {1} has already been taken.'), listUsers[j].username, j+1),
                            function () {
                                this.tabs.activate(this.gridUsers);
                            }.createDelegate(this) 
                        );
                        return false;
                    }
                }
                // login name contains no forward slash character
                if (listUsers[i].username.indexOf("/") != -1) {
                    Ext.MessageBox.alert(this.i18n._('Warning'), String.format(this.i18n._('The login name at row {0} must not contain forward slash character.'), i+1),
                        function () {
                            this.tabs.activate(this.gridUsers);
                        }.createDelegate(this) 
                    );
                    return false;
                }
                // first name contains no spaces
                if (listUsers[i].firstName.indexOf(" ") != -1) {
                    Ext.MessageBox.alert(this.i18n._('Warning'), String.format(this.i18n._('The first name at row {0} must not contain any space characters.'), i+1),
                        function () {
                            this.tabs.activate(this.gridUsers);
                        }.createDelegate(this) 
                    );
                    return false;
                }
                // last name contains no spaces
                if (listUsers[i].lastName.indexOf(" ") != -1) {
                    Ext.MessageBox.alert(this.i18n._('Warning'), String.format(this.i18n._('The last name at row {0} must not contain any space characters.'), i+1),
                        function () {
                            this.tabs.activate(this.gridUsers);
                        }.createDelegate(this) 
                    );
                    return false;
                }
                // the password is at least one character
                if (listUsers[i].password.length == 0) {
                    Ext.MessageBox.alert(this.i18n._('Warning'), String.format(this.i18n._('The password at row {0} must be at least 1 character long.'), i+1),
                        function () {
                            this.tabs.activate(this.gridUsers);
                        }.createDelegate(this) 
                    );
                    return false;
                }
                // the password contains no spaces
                if (listUsers[i].password.indexOf(" ") != -1) {
                    Ext.MessageBox.alert(this.i18n._('Warning'), String.format(this.i18n._('The password at row {0} must not contain any space characters.'), i+1),
                        function () {
                            this.tabs.activate(this.gridUsers);
                        }.createDelegate(this) 
                    );
                    return false;
                }
            }
            
            return true;
        },

        applyAction : function()
        {
            this.commitSettings(this.reloadSettings.createDelegate(this));
        },
        reloadSettings : function()
        {
            var storeData=main.getLocalDirectory().getUsers().list;
            for(var i=0; i<storeData.length; i++) {
                storeData[i].password = "***UNCHANGED***";
            }

            this.gridUsers.clearChangedData();
            this.gridUsers.store.loadData(storeData);

            Ext.MessageBox.hide();
        },
        saveAction : function()
        {
            this.commitSettings(this.completeSaveAction.createDelegate(this));
        },

        completeSaveAction : function()
        {
            // exit settings screen
            Ext.MessageBox.hide();
            this.closeWindow();
            if(this.fnCallback) {
                this.fnCallback.call();
            }
        },
        
        // save function
        commitSettings : function(callback)
        {
            if (!this.validate()) {
                return;
            }
            Ext.MessageBox.show({
                title : this.i18n._('Please wait'),
                msg : this.i18n._('Saving...'),
                modal : true,
                wait : true,
                waitConfig: {interval: 100},
                progressText : " ",
                width : 200
            });
            //save local users            
            main.getLocalDirectory().setUsers(function(result, exception) {
                if(Ung.Util.handleException(exception)) return;
                callback();
            }.createDelegate(this), this.gridUsers ? {javaClass:"java.util.LinkedList",list:this.gridUsers.getFullSaveList()} : null);
        },
        
        isDirty : function()
        {
            return this.gridUsers.isDirty();
        }
        
    });

}
