if (!Ung.hasResource["Ung.LocalDirectory"]) {
    Ung.hasResource["Ung.LocalDirectory"] = true;

    Ext.define('Ung.LocalDirectory', {
        extend: 'Ung.ConfigWin',
        gridUsers: null,
        initComponent: function() {
            this.breadcrumbs = [{
                title: i18n._("Configuration"),
                action: Ext.bind(function() {
                    this.cancelAction();
                }, this)
            }, {
                title: i18n._('Local Directory')
            }];
            this.buildLocalDirectory();
            // builds the tab panel with the tabs
            this.buildTabPanel([this.gridUsers]);
            this.tabs.setActiveTab(this.gridUsers);
            this.callParent(arguments);
        },
        buildLocalDirectory: function() {
            this.gridUsers = Ext.create('Ung.EditorGrid',{
                name: 'Local Users',
                helpSource: 'local_directory',
                title: this.i18n._('Local Users'),
                hasImportExport: false, /* password not actually in grid - cant export */
                settingsCmp: this,
                height: 500,
                paginated: false,
                tbar: [
                        '-',
                        {
                            xtype: 'button',
                            text: i18n._('Cleanup expired users'),
                            iconCls: 'icon-delete-row',
                            handler: Ext.bind(function() {
                                this.cleanupExpiredUsers();
                            }, this)
                        },
                        '-'
                ],
                emptyRow: {
                    "username": this.i18n._('[login]'),
                    "firstName": this.i18n._('[firstName]'),
                    "lastName": this.i18n._('[lastName]'),
                    "email": this.i18n._('[email@example.com]'),
                    "password": "",
                    "expirationTime": 0,
                    "javaClass": "com.untangle.uvm.LocalDirectoryUser"
                },
                recordJavaClass: "com.untangle.uvm.LocalDirectoryUser",
                dataFn:Ext.bind( function() {
                    var storeData=main.getLocalDirectory().getUsers().list;
                    for(var i=0; i<storeData.length; i++) {
                        storeData[i].password = "***UNCHANGED***";
                    }
                    return storeData;
                }, this),            
                dataRoot: null,
                fields: [{
                    name: 'username'
                }, {
                    name: 'firstName'
                }, {
                    name: 'lastName'
                }, {
                    name: 'email'
                }, {
                    name: 'password'
                },{
                    name: 'expirationTime'
                },{
                    name: 'javaClass'
                }],
                columns: [{
                    header: this.i18n._("user/login ID"),
                    width: 140,
                    dataIndex: 'username',
                    editor: {
                        xtype: 'textfield',
                        allowBlank: false,
                        regex: /^[\w ]+$/,
                        regexText: this.i18n._("The field user/login ID can have only alphanumeric characters.")
                    }
                }, {
                    header: this.i18n._("first name"),
                    width: 120,
                    dataIndex: 'firstName',
                    editor: {
                        xtype:'textfield',
                        allowBlank: false
                    }
                }, {
                    header: this.i18n._("last name"),
                    width: 120,
                    dataIndex: 'lastName',
                    editor: {
                        xtype: 'textfield'
                    }                    
                }, {
                    header: this.i18n._("email address"),
                    width: 250,
                    dataIndex: 'email',
                    flex:1,
                    editor: {
                        xtype:'textfield',
                        vtype: 'email'
                    }
                }, {
                    header: this.i18n._("password"),
                    width: 150,
                    dataIndex: 'password',
                    editor: {
                        xtype:'textfield',
                        inputType:'password'
                    },
                    renderer: function(value, metadata, record) {
                        var result = "";
                        for(var i=0; value != null && i<value.length; i++) {
                            result = result + "*";
                        }
                        return result;
                    }
                },
                {
                    header: this.i18n._("expiration time"),
                    width: 150,
                    dataIndex: 'expirationTime',
                    renderer: Ext.bind(function(value, metadata, record) {
                        if (value == 0) {
                            return this.i18n._("Never");
                        } else {
                            return i18n.timestampFormat(value);
                        }
                    },this)
                }],
                sortField: 'username',
                columnsDefaultSortable: true,
                rowEditorInputLines: [
                {
                    xtype:'textfield',
                    name: "User/Login ID",
                    dataIndex: "username",
                    fieldLabel: this.i18n._("User/Login ID"),
                    allowBlank: false,
                    regex: /^[\w ]+$/,
                    regexText: this.i18n._("The field user/login ID can have only alphanumeric character."),
                    width: 300
                }, 
                {
                    xtype:'textfield',
                    name: "First Name",
                    dataIndex: "firstName",
                    fieldLabel: this.i18n._("First Name"),
                    allowBlank: false,
                    width: 300
                },
                {
                    xtype:'textfield',
                    name: "Last Name",
                    dataIndex: "lastName",
                    fieldLabel: this.i18n._("Last Name"),
                    width: 300
                },
                {
                    xtype:'textfield',
                    name: "Email Address",
                    dataIndex: "email",
                    fieldLabel: this.i18n._("Email Address"),
                    vtype: 'email',
                    width: 300
                },
                {
                    xtype:'textfield',
                    inputType: 'password',
                    name: "Password",
                    dataIndex: "password",
                    fieldLabel: this.i18n._("Password"),
                    width: 300
                },
                {
                    xtype:'container',
                    layout: {
                        type: 'hbox',
                        align: 'stretch'
                    },
                    dataIndex:'expirationTime',
                    setValue:function(value) {
                        if ( value == 0) {
                            this.query("[name='expirationTime']")[0].setVisible(false);
                            this.query("[name='neverExpire']")[0].setValue(true);
                            this.query("[name='expirationTime']")[0].setValue(new Date(0));
                        } else {
                            this.query("[name='neverExpire']")[0].setValue(false);
                            this.query("[name='expirationTime']")[0].setVisible(true);
                            this.query("[name='expirationTime']")[0].setValue(value);
                        }
                    },
                    getValue: function() {
                        var neverExpired =this.query("[name='neverExpire']")[0].getValue();
                        if ( neverExpired) {
                            return 0;
                        }
                        return this.query("[name='expirationTime']")[0].getValue();
                    },
                    items:[{
                        xtype:'checkbox',
                        name:'neverExpire',
                        fieldLabel: this.i18n._("Expiration Time"),
                        boxLabel:this.i18n._('Never'),
                        listeners: {
                            "change": Ext.bind(function (elem,checked) { 
                                var expirationCtl = this.gridUsers.rowEditor.query("[name='expirationTime']")[0];
                                expirationCtl.setVisible(!checked);
                            },this)
                        },
                        width:180
                    },
                    {
                        xtype:'xdatetime',
                        name: "expirationTime"
                    }]
                }]
            });

        },
        validate: function() {
            //validate local directory users
            var listUsers = this.gridUsers.getPageList();
            var mapUsers = {};
            for(var i=0; i<listUsers.length;i++) {
                // verify that the login name is not duplicated
                if(mapUsers[listUsers[i].username]) {
                    Ext.MessageBox.alert(this.i18n._('Warning'), Ext.String.format(this.i18n._('The login name "{0}" at row {1} has already been taken.'), listUsers[i].username, i+1),
                        Ext.bind(function () {
                            this.tabs.setActiveTab(this.gridUsers);
                        }, this) 
                    );
                    return false;
                }
                mapUsers[listUsers[i].username]=true;
                
                // login name contains no forward slash character
                if (listUsers[i].username.indexOf("/") != -1) {
                    Ext.MessageBox.alert(this.i18n._('Warning'), Ext.String.format(this.i18n._('The login name at row {0} must not contain forward slash character.'), i+1),
                        Ext.bind(function () {
                            this.tabs.setActiveTab(this.gridUsers);
                        }, this) 
                    );
                    return false;
                }
                // first name contains no spaces
                if (listUsers[i].firstName.indexOf(" ") != -1) {
                    Ext.MessageBox.alert(this.i18n._('Warning'), Ext.String.format(this.i18n._('The first name at row {0} must not contain any space characters.'), i+1),
                        Ext.bind(function () {
                            this.tabs.setActiveTab(this.gridUsers);
                        }, this) 
                    );
                    return false;
                }
                // last name contains no spaces
                if (listUsers[i].lastName.indexOf(" ") != -1) {
                    Ext.MessageBox.alert(this.i18n._('Warning'), Ext.String.format(this.i18n._('The last name at row {0} must not contain any space characters.'), i+1),
                        Ext.bind(function () {
                            this.tabs.setActiveTab(this.gridUsers);
                        }, this) 
                    );
                    return false;
                }
                // the password is at least one character
                if (listUsers[i].password.length == 0) {
                    Ext.MessageBox.alert(this.i18n._('Warning'), Ext.String.format(this.i18n._('The password at row {0} must be at least 1 character long.'), i+1),
                        Ext.bind(function () {
                            this.tabs.setActiveTab(this.gridUsers);
                        }, this) 
                    );
                    return false;
                }
                // the password contains no spaces
                if (listUsers[i].password.indexOf(" ") != -1) {
                    Ext.MessageBox.alert(this.i18n._('Warning'), Ext.String.format(this.i18n._('The password at row {0} must not contain any space characters.'), i+1),
                        Ext.bind(function () {
                            this.tabs.setActiveTab(this.gridUsers);
                        }, this) 
                    );
                    return false;
                }
            }
            return true;
        },
        save: function (isApply) {
            main.getLocalDirectory().setUsers(Ext.bind(function(result, exception) {
                Ext.MessageBox.hide();
                if(Ung.Util.handleException(exception)) return;
                if (!isApply) {
                    this.closeWindow();
                } else {
                    this.clearDirty();
                }
            }, this), {javaClass:"java.util.LinkedList",list:this.gridUsers.getPageList()});
        },
        cleanupExpiredUsers:function() {
            if ( this.isDirty()) {
                Ext.MessageBox.alert(this.i18n._("Warning"), this.i18n._("Please save the changes before cleaning up expired users !"));
                return;
            }
            Ext.MessageBox.wait(this.i18n._("Cleaning up expired users..."), i18n._("Please wait"));
            main.getLocalDirectory().cleanupExpiredUsers(Ext.bind(function(result, exception) {
                Ext.MessageBox.hide();
                if(Ung.Util.handleException(exception)) return;
                this.gridUsers.reload();
            }, this));
        }
    });
}
//@ sourceURL=localDirectory.js
