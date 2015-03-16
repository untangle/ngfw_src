Ext.define('Webui.config.localDirectoryNew', {
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
        this.gridUsers = Ext.create('Ung.grid.Panel',{
            name: 'Local Users',
            helpSource: 'local_directory_local_users',
            title: this.i18n._('Local Users'),
            settingsCmp: this,
            height: 500,
            bbar: [
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
                "username": "",
                "firstName": "",
                "lastName": "",
                "email": "",
                "password": "",
                "passwordBase64Hash": "",
                "expirationTime": 0,
                "javaClass": "com.untangle.uvm.LocalDirectoryUser"
            },
            recordJavaClass: "com.untangle.uvm.LocalDirectoryUser",
            dataFn: Ung.Main.getLocalDirectory().getUsers,
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
                name: 'passwordBase64Hash'
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
                    emptyText: this.i18n._('[enter login]'),
                    regex: /^[\w ]+$/,
                    regexText: this.i18n._("The field user/login ID can have only alphanumeric characters.")
                }
            }, {
                header: this.i18n._("first name"),
                width: 120,
                dataIndex: 'firstName',
                editor: {
                    xtype:'textfield',
                    emptyText: this.i18n._('[enter first name]'),
                    allowBlank: false
                }
            }, {
                header: this.i18n._("last name"),
                width: 120,
                dataIndex: 'lastName',
                editor: {
                    xtype: 'textfield',
                    emptyText: this.i18n._('[last name]')
                }
            }, {
                header: this.i18n._("email address"),
                width: 250,
                dataIndex: 'email',
                flex:1,
                editor: {
                    xtype:'textfield',
                    emptyText: this.i18n._('[email address]'),
                    vtype: 'email'
                }
            }, {
                header: this.i18n._("password"),
                width: 180,
                dataIndex: 'password',
                editor: {
                    xtype:'textfield',
                    inputType:'password',
                    allowBlank: false
                },
                renderer: Ext.bind(function(value, metadata, record) {
                    if (record.get("passwordBase64Hash") == null)
                        return "";
                    if(Ext.isEmpty(value) && record.get("passwordBase64Hash").length > 0) {
                        return "*** "+this.i18n._("Unchanged")+" ***";
                    }
                    var result = "";
                    for(var i=0; value != null && i<value.length; i++) {
                        result = result + "*";
                    }
                    return result;
                },this)
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
            columnsDefaultSortable: true
        });
        this.gridUsers.setRowEditor( Ext.create('Ung.RowEditorWindow',{
            inputLines: [{
                 xtype:'textfield',
                 name: "User/Login ID",
                 dataIndex: "username",
                 fieldLabel: this.i18n._("User/Login ID"),
                 emptyText: this.i18n._('[enter login]'),
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
                 emptyText: this.i18n._('[enter first name]'),
                 allowBlank: false,
                 width: 300
             },
             {
                 xtype:'textfield',
                 name: "Last Name",
                 dataIndex: "lastName",
                 fieldLabel: this.i18n._("Last Name"),
                 emptyText: this.i18n._('[last name]'),
                 width: 300
             },
             {
                 xtype:'textfield',
                 name: "Email Address",
                 dataIndex: "email",
                 fieldLabel: this.i18n._("Email Address"),
                 emptyText: this.i18n._('[email address]'),
                 vtype: 'email',
                 width: 300
             },
             {
                 xtype: 'container',
                 layout: 'column',
                 margin: '0 0 5 0',
                 items: [{
                     xtype:'textfield',
                     inputType: 'password',
                     name: "Password",
                     dataIndex: "password",
                     fieldLabel: this.i18n._("Password"),
                     allowBlank: false,
                     width: 300
                 },{
                     xtype: 'label',
                     name: 'passwordInfo',
                     html: this.i18n._("(leave empty to keep the current password unchanged)"),
                     cls: 'boxlabel'
                 }]
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
                         this.down("[name='expirationTime']").setVisible(false);
                         this.down("[name='neverExpire']").setValue(true);
                         this.down("[name='expirationTime']").setValue(new Date(0));
                     } else {
                         this.down("[name='neverExpire']").setValue(false);
                         this.down("[name='expirationTime']").setVisible(true);
                         this.down("[name='expirationTime']").setValue(value);
                     }
                 },
                 getValue: function() {
                     var neverExpired =this.down("[name='neverExpire']").getValue();
                     if ( neverExpired) {
                         return 0;
                     }
                     return this.down("[name='expirationTime']").getValue();
                 },
                 items:[{
                     xtype:'checkbox',
                     name:'neverExpire',
                     fieldLabel: this.i18n._("Expiration Time"),
                     boxLabel:this.i18n._('Never'),
                     listeners: {
                         "change": Ext.bind(function (elem,checked) {
                             var expirationCtl = this.gridUsers.rowEditor.down("[name='expirationTime']");
                             expirationCtl.setVisible(!checked);
                             if ( checked) {
                                 expirationCtl.setValue(new Date(0));
                             } else {
                                 var v = expirationCtl.getValue();
                                 if ( v == 0) {
                                     expirationCtl.setValue(new Date());
                                 }
                             }
                         },this)
                     },
                     width:180
                 },
                 {
                     xtype:'xdatetime',
                     name: "expirationTime"
                 }]
             }],
             populate: function(record, addMode) {
                 this.down('textfield[dataIndex="password"]').allowBlank = (record.get("passwordBase64Hash").length > 0);
                 this.down('label[name="passwordInfo"]').setVisible(record.get("passwordBase64Hash").length > 0);
                 Ung.RowEditorWindow.prototype.populate.apply(this, arguments);
             }
        }));

    },
    validate: function() {
        //validate local directory users
        var listUsers = this.gridUsers.getList();
        var mapUsers = {}, user;
        for(var i=0; i<listUsers.length;i++) {
            user = listUsers[i];
            // verify that the login name is not duplicated
            if(mapUsers[user.username]) {
                Ext.MessageBox.alert(this.i18n._('Warning'), Ext.String.format(this.i18n._('The login name "{0}" at row {1} has already been taken.'), user.username, i+1),
                    Ext.bind(function () {
                        this.tabs.setActiveTab(this.gridUsers);
                    }, this)
                );
                return false;
            }
            mapUsers[user.username]=true;
            // login name contains no forward slash character
            if (user.username.indexOf("/") != -1) {
                Ext.MessageBox.alert(this.i18n._('Warning'), Ext.String.format(this.i18n._('The login name at row {0} must not contain forward slash character.'), i+1),
                    Ext.bind(function () {
                        this.tabs.setActiveTab(this.gridUsers);
                    }, this)
                );
                return false;
            }
            // first name contains no spaces
            if (user.firstName.indexOf(" ") != -1) {
                Ext.MessageBox.alert(this.i18n._('Warning'), Ext.String.format(this.i18n._('The first name at row {0} must not contain any space characters.'), i+1),
                    Ext.bind(function () {
                        this.tabs.setActiveTab(this.gridUsers);
                    }, this)
                );
                return false;
            }
            // last name contains no spaces
            if (user.lastName.indexOf(" ") != -1) {
                Ext.MessageBox.alert(this.i18n._('Warning'), Ext.String.format(this.i18n._('The last name at row {0} must not contain any space characters.'), i+1),
                    Ext.bind(function () {
                        this.tabs.setActiveTab(this.gridUsers);
                    }, this)
                );
                return false;
            }
            // the password is at least one character
            if (Ext.isEmpty(user.passwordBase64Hash) && Ext.isEmpty(user.password)) {
                Ext.MessageBox.alert(this.i18n._('Warning'), Ext.String.format(this.i18n._('The password at row {0} must be at least 1 character long.'), i+1),
                    Ext.bind(function () {
                        this.tabs.setActiveTab(this.gridUsers);
                    }, this)
                );
                return false;
            }
            // the password contains no spaces
            if (user.password !=null && user.password.indexOf(" ") != -1) {
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
        // Calculate passwordBase64Hash for changed passwords and remove cleartext password before saving
        var user, listUsers = this.gridUsers.getList();
        for(var i=0; i<listUsers.length;i++) {
            user = listUsers[i];
            if(user.password == null) {
                user.password = "";
            } else if(user.password.length > 0) {
                user.passwordBase64Hash = Ung.Util.btoa(user.password);
                user.password = "";
            }
        }
        Ung.Main.getLocalDirectory().setUsers(Ext.bind(function(result, exception) {
            Ext.MessageBox.hide();
            if(Ung.Util.handleException(exception)) return;
            if (!isApply) {
                this.closeWindow();
            } else {
                this.clearDirty();
            }
        }, this), {javaClass:"java.util.LinkedList",list: listUsers});
    },
    cleanupExpiredUsers:function() {
        if ( this.isDirty()) {
            Ext.MessageBox.alert(this.i18n._("Warning"), this.i18n._("Please save the changes before cleaning up expired users !"));
            return;
        }
        Ext.MessageBox.wait(this.i18n._("Cleaning up expired users..."), i18n._("Please wait"));
        Ung.Main.getLocalDirectory().cleanupExpiredUsers(Ext.bind(function(result, exception) {
            Ext.MessageBox.hide();
            if(Ung.Util.handleException(exception)) return;
            this.gridUsers.reload();
        }, this));
    }
});
//# sourceURL=localDirectory.js