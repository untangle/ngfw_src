Ext.define('Ung.config.local-directory.view.Users', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config-local-directory-users',
    itemId: 'local-users',
    title: 'Local Users'.t(),
    scrollable: true,

    layout: 'fit',
    items: [{
        xtype: 'ungrid',
        border: false,
        title: 'Local Users'.t(),
        itemId: 'local-users-grid',

        emptyText: 'No Local Users defined'.t(),

        tbar: ['@add', '->', '@import', '@export'],
        recordActions: ['edit', 'delete'],

        listProperty: 'usersData.list',

        emptyRow: {
            username: '',
            firstName: '',
            lastName: '',
            email: '',
            password: '',
            passwordBase64Hash: '',
            twofactorSecretKey: '',
            localExpires: Util.serverToClientDate(new Date()),
            localForever: true,
            localEmpty: true,
            mfaEnabled: false,
            expirationTime: 0,
            javaClass: 'com.untangle.uvm.LocalDirectoryUser'
        },

        bind: '{users}',

        columns: [{
            header: 'User/Login ID'.t(),
            width: Renderer.usernameWidth,
            dataIndex: 'username',
            editor: {
                xtype: 'textfield',
                allowBlank: false,
                emptyText: '[enter login]'.t(),
                regex: /^[\w\. ]+$/,
                regexText: 'The field user/login ID can have only alphanumeric characters.'.t()
            }
        }, {
            header: 'First Name'.t(),
            width: Renderer.messageWidth,
            dataIndex: 'firstName',
            editor: {
                xtype: 'textfield',
                emptyText: '[enter first name]'.t(),
                allowBlank: false
            }
        }, {
            header: 'Last Name'.t(),
            width: Renderer.messageWidth,
            dataIndex: 'lastName',
            editor: {
                xtype: 'textfield',
                emptyText: '[last name]'.t()
            }
        }, {
            header: 'Email Address'.t(),
            width: Renderer.emailWidth,
            dataIndex: 'email',
            flex: 1,
            editor: {
                xtype: 'textfield',
                emptyText: '[email address]'.t(),
                vtype: 'email'
            }
        }, {
            header: 'Password'.t(),
            width: Renderer.messageWidth,
            dataIndex: 'password',
            renderer: Ext.bind(function (value, metadata, record) {
                if (record.get("passwordBase64Hash") == null) return ('');
                if (Ext.isEmpty(value) && record.get("passwordBase64Hash").length > 0) return ('*** ' + 'Unchanged'.t() + ' ***');
                var result = "";
                for (var i = 0; value != null && i < value.length; i++) result = result + '*';
                return result;
            }, this)
        },
        {
            header: 'OpenVPN MFA secret'.t(),
            xtype: 'actioncolumn',
            width: Renderer.messageWidth,
            dataIndex: 'twofactorSecretKey',
            iconCls: 'fa fa-cog',
            align: 'center',
            tooltip: 'Enable/disable MFA by editting user.'.t(),
            getClass: function (value, metadata, record) {
                if (record.get("twofactorSecretKey") !== "") {
                    return 'fa fa-cog';
                }
                return 'fa fa-minus';
            },
            handler: function (unk1, unk2, unk3, event, unk5, record) {
                if (record.get('twofactorSecretKey') !== "") {
                    Rpc.asyncData('rpc.UvmContext.localDirectory.showSecretQR', record.get('username'), 'Untangle', record.get('twofactorSecretKey'))
                        .then(function (result) {
                            Ext.MessageBox.alert({ buttons: Ext.Msg.OK, maxWidth: 1024, title: 'Provide user with key or QR image.'.t(), msg: result });
                        });
                }
            },
            isDisabled: function (view, rowIndex, colIndex, item, record) {
                if (!record.get("mfaEnabled") || record.get("twofactorSecretKey") === "")
                    return true;
                return false;
            },
        }, {
            header: 'Expiration'.t(),
            dataIndex: 'expirationTime',
            width: Renderer.timestampWidth,
            resizable: false,
            renderer: Ung.config.localdirectory.MainController.expirationRenderer
        }],
        editorFields: [{
            xtype: 'textfield',
            fieldLabel: 'User/Login ID'.t(),
            bind: '{record.username}',
            emptyText: '[enter login]'.t(),
            allowBlank: false,
            regex: /^[\w\. ]+$/,
            regexText: 'The field user/login ID can have only alphanumeric character.'.t(),
        }, {
            xtype: 'textfield',
            fieldLabel: 'First Name'.t(),
            bind: '{record.firstName}',
            emptyText: '[enter first name]'.t(),
            allowBlank: false,
            width: 300
        }, {
            xtype: 'textfield',
            fieldLabel: 'Last Name'.t(),
            bind: '{record.lastName}',
            emptyText: '[last name]'.t(),
            width: 300
        }, {
            xtype: 'textfield',
            fieldLabel: 'Email Address'.t(),
            bind: '{record.email}',
            emptyText: '[email address]'.t(),
            vtype: 'email',
            width: 300
        }, {
            xtype: 'container',
            layout: 'column',
            items: [{
                xtype: 'textfield',
                fieldLabel: 'Password'.t(),
                labelAlign: 'right',
                labelWidth: 180,
                width: 300,
                inputType: 'password',
                allowBlank: true,
                bind: {
                    value: '{record.password}',
                    hidden: '{record.localEmpty}',
                    disabled: '{record.localEmpty}',
                },
            }, {
                xtype: 'textfield',
                fieldLabel: 'Password'.t(),
                labelAlign: 'right',
                labelWidth: 180,
                width: 300,
                inputType: 'password',
                allowBlank: false,
                bind: {
                    value: '{record.password}',
                    hidden: '{!record.localEmpty}',
                    disabled: '{!record.localEmpty}',
                },
            }, {
                xtype: 'displayfield',
                margin: '0 0 0 10',
                value: '(leave empty to keep the current password unchanged)'.t(),
                bind: {
                    hidden: '{record.localEmpty}'
                }
            }, {
                xtype: 'displayfield',
                margin: '0 0 0 10',
                value: '',
                bind: {
                    hidden: '{!record.localEmpty}'
                }
            }]
        }, {
            xtype: 'checkbox',
            fieldLabel: 'Enable MFA for OpenVPN'.t(),
            bind: {
                value: '{record.mfaEnabled}',
                hidden: '{!record.username}'
            }
        }, {
            xtype: 'container',
            layout: 'column',
            items: [{
                xtype: 'textfield',
                fieldLabel: 'MFA Secret key'.t(),
                labelAlign: 'right',
                inputType: 'password',
                labelWidth: 180,
                width: 500,
                allowBlank: true,
                emptyText: '[Key not currently set]'.t(),
                bind: {
                    value: '{record.twofactorSecretKey}',
                    hidden: '{!record.username || !record.mfaEnabled}'
                },
            }, {
                xtype: 'button',
                text: 'Generate new key'.t(),
                bind: {
                    hidden: '{!record.username || !record.mfaEnabled}'
                },
                handler: function (btn) {
                    Rpc.asyncData('rpc.UvmContext.localDirectory.generateSecret')
                        .then(function (result) {
                            btn.lookupViewModel().get('record').set('twofactorSecretKey', result);
                        });
                }
            },
            {
                xtype: 'button',
                iconCls: 'fa fa-cog',
                align: 'center',
                bind: {
                    hidden: '{!record.twofactorSecretKey || !record.mfaEnabled}'
                },
                handler: function (btn) {
                    var record = btn.lookupViewModel().get('record');
                    Rpc.asyncData('rpc.UvmContext.localDirectory.showSecretQR', record.get('username'), 'Untangle', record.get('twofactorSecretKey'))
                        .then(function (result) {
                            Ext.MessageBox.alert({ buttons: Ext.Msg.OK, maxWidth: 1024, title: 'Provide user with key or QR image.'.t(), msg: result });
                        });
                }
            }]
        }, {
            xtype: 'numberfield',
            fieldIndex: 'expirationTime',
            hidden: true,
            bind: '{record.expirationTime}'
        }, {
            xtype: 'checkbox',
            fieldLabel: 'User Never Expires'.t(),
            fieldIndex: 'localForever',
            bind: '{record.localForever}',
            listeners: {
                change: function (cmp, nval, oval, opts) {
                    var target = this.ownerCt.down("[fieldIndex='expirationTime']");
                    if (nval == true) {
                        target.setValue(0);
                    } else {
                        var finder = this.ownerCt.down("[fieldIndex='DateTimePicker']");
                        target.setValue(finder.getValue().getTime());
                    }
                }
            }
        }, {
            xtype: 'datetimefield',
            fieldLabel: 'Expiration Date/Time'.t(),
            fieldIndex: 'DateTimePicker',
            format: 'timestamp_fmt'.t(),
            editable: false,
            bind: {
                value: '{record.localExpires}',
                hidden: '{record.localForever}',
                disabled: '{record.localForever}'
            },
            listeners: {
                change: function (cmp, nval, oval, opts) {
                    var finder = this.ownerCt.down("[fieldIndex='localForever']");
                    var target = this.ownerCt.down("[fieldIndex='expirationTime']");
                    if (finder.getValue() == false) target.setValue(nval.getTime());
                }
            }
        }]
    }]

});
