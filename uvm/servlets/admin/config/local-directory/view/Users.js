Ext.define('Ung.config.local-directory.view.Users', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config-local-directory-users',
    itemId: 'users',
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
            localExpires: Util.serverToClientDate(new Date()),
            localForever: true,
            localEmpty: true,
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
                xtype:'textfield',
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
            flex:1,
            editor: {
                xtype: 'textfield',
                emptyText: '[email address]'.t(),
                vtype: 'email'
            }
        }, {
            header: 'Password'.t(),
            width: Renderer.messageWidth,
            dataIndex: 'password',
            renderer: Ext.bind(function(value, metadata, record) {
                if (record.get("passwordBase64Hash") == null) return('');
                if(Ext.isEmpty(value) && record.get("passwordBase64Hash").length > 0) return('*** ' + 'Unchanged'.t() + ' ***');
                var result = "";
                for(var i = 0 ; value != null && i < value.length ; i++) result = result + '*';
                return result;
            },this)
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
                change: function(cmp, nval, oval, opts) {
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
                change: function(cmp, nval, oval, opts) {
                    var finder = this.ownerCt.down("[fieldIndex='localForever']");
                    var target = this.ownerCt.down("[fieldIndex='expirationTime']");
                    if (finder.getValue() == false) target.setValue(nval.getTime());
                }
            }
        }]
    }]

});
