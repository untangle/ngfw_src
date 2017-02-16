Ext.define('Ung.config.localdirectory.LocalDirectory', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config.localdirectory',

    requires: [
        'Ung.config.localdirectory.LocalDirectoryController',
        'Ung.config.localdirectory.LocalDirectoryModel'
    ],

    controller: 'config.localdirectory',

    viewModel: {
        type: 'config.localdirectory'
    },

    dockedItems: [{
        xtype: 'toolbar',
        weight: -10,
        border: false,
        items: [{
            text: 'Back',
            iconCls: 'fa fa-arrow-circle-left fa-lg',
            hrefTarget: '_self',
            href: '#config'
        }, '-', {
            xtype: 'tbtext',
            html: '<strong>' + 'Local Directory'.t() + '</strong>'
        }],
    }, {
        xtype: 'toolbar',
        dock: 'bottom',
        border: false,
        items: ['->', {
            text: 'Apply Changes'.t(),
            scale: 'large',
            iconCls: 'fa fa-floppy-o fa-lg',
            handler: 'saveSettings'
        }]
    }],

    layout: 'fit',

    items: [{
        xtype: 'grid',
        border: false,
        title: 'Local Users'.t(),

        // tbar: ['@add'],
        // recordActions: ['@edit', '@delete'],

        emptyRow: {
            username: '',
            firstName: '',
            lastName: '',
            email: '',
            password: '',
            passwordBase64Hash: '',
            expirationTime: 0
        },

        bind: '{users}',

        columns: [{
            header: 'user/login ID'.t(),
            width: 140,
            dataIndex: 'username',
            editor: {
                xtype: 'textfield',
                allowBlank: false,
                emptyText: '[enter login]'.t(),
                regex: /^[\w\. ]+$/,
                regexText: 'The field user/login ID can have only alphanumeric characters.'.t()
            }
        }, {
            header: 'first name'.t(),
            width: 120,
            dataIndex: 'firstName',
            editor: {
                xtype:'textfield',
                emptyText: '[enter first name]'.t(),
                allowBlank: false
            }
        }, {
            header: 'last name'.t(),
            width: 120,
            dataIndex: 'lastName',
            editor: {
                xtype: 'textfield',
                emptyText: '[last name]'.t()
            }
        }, {
            header: 'email address'.t(),
            width: 250,
            dataIndex: 'email',
            flex:1,
            editor: {
                xtype: 'textfield',
                emptyText: '[email address]'.t(),
                vtype: 'email'
            }
        }, {
            header: 'datetime',
            dataIndex: 'expirationTime',
        }, {
            header: 'datetime',
            dataIndex: 'datetime',
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
            xtype: 'textfield',
            fieldLabel: 'Date Time',
            bind: '{record.datetime}'
        }, {
            xtype: 'container',
            layout: {
                type: 'hbox'
            },
            items: [{
                xtype: 'displayfield',
                fieldLabel: 'Expiration Time'.t(),
                labelAlign: 'right',
                labelWidth: 180,
                width: 185
            }, {
                xtype: 'checkbox',
                boxLabel: 'Never'.t(),
                bind: '{checked}'
            },
            // {
            //     xtype: 'xdatetime',
            //     bind: '{record.expirationTime}'
            // }, {
            //     xtype: 'textfield',
            //     bind: '{record.expirationTime}'
            // }, {
            //     xtype: 'textfield',
            //     bind: '{record.expirationTime}'
            // }
            {
                xtype: 'datefield',
                // format: 'timestamp',
                // altFormats: 'm/d/Y',
                minValue: '',
                margin: '0 10',
                editable: false,
                bind: {
                    value: '{datetime}'
                }
            }, {
                xtype: 'numberfield',
                bind: '{record.expirationTime}'
            }
            ]
        }]
        // extraVM: {
        //     formulas: {
        //         checked: function (get) {
        //             return 'test';
        //         }
        //     }
        // }

    }]

});
Ext.define('Ung.config.localdirectory.LocalDirectoryController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.config.localdirectory',

    control: {
        '#': {
            beforerender: 'loadSettings'
        }
    },

    localDirectory: rpc.UvmContext.localDirectory(),

    loadSettings: function () {
        var me = this;
        this.localDirectory.getUsers(function (result, ex) {
            if (ex) { console.error(ex); Ung.Util.exceptionToast(ex); return; }
            console.log(result);
            me.getViewModel().set('usersData', result);
        });
    }

});
Ext.define('Ung.config.localdirectory.LocalDirectoryModel', {
    extend: 'Ext.app.ViewModel',

    alias: 'viewmodel.config.localdirectory',
    // requires: ['Ung.model.LocalDirectoryUser'],


    data: {
        usersData: null
    },

    stores: {
        users: {
            // model: 'Ung.model.LocalDirectoryUser',
            data: '{usersData.list}'
        }
    }
});