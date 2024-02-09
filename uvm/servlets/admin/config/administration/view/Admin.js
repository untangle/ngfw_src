Ext.define('Ung.config.administration.view.Admin', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config-administration-admin',
    itemId: 'admin',
    scrollable: true,
    withValidation: true,
    viewModel: true,

    title: 'Admin'.t(),
    layout: 'border',

    items: [{
        xtype: 'ungrid',
        title: 'Admin Accounts'.t(),
        region: 'center',

        controller: 'unadmingrid',

        bind: '{accounts}',

        listProperty: 'adminSettings.users.list',
        tbar: ['@add', '->', '@import', '@export'],
        recordActions: ['changePassword', 'delete'],

        emptyText: 'No Admin Accounts defined'.t(),

        emptyRow: {
            javaClass: 'com.untangle.uvm.AdminUserSettings',
            username: '',
            description: '',
            emailAddress: '',
            emailAlerts: false,
            passwordHashBase64: null,
            passwordHashShadow: null,
            password: null
        },

        // disable column moving and prevent editing "admin" account name
        enableColumnMove: false,
        listeners: {
            cellclick: function(grid,td,cellIndex,record,tr,rowIndex,e,eOpts)  {
                if (cellIndex === 0 && record.data.username === 'admin') { return false; }
            }
        },

        columns: [{
            header: 'Username'.t(),
            width: Renderer.usernameWidth,
            dataIndex: 'username',
            editor: {
                allowBlank: false,
                emptyText: '[enter username]'.t(),
                blankText: 'The username cannot be blank.'.t()
            }
        }, {
            header: 'Description'.t(),
            width: Renderer.messageWidth,
            flex: 1,
            dataIndex: 'description',
            editor:{
                emptyText: '[no description]'.t()
            }
        }, {
            header: 'Email Address'.t(),
            width: Renderer.emailWidth,
            dataIndex: 'emailAddress',
            editor: {
                allowOnlyWhitespace: false,
                emptyText: '[no email]'.t(),
                vtype: 'email'               
            }      
        }, {
            xtype: 'checkcolumn',
            header: 'Email Alerts'.t(),
            dataIndex: 'emailAlerts',
            width: Renderer.booleanWidth + 20,
            listeners: {
                // prevent enabling email alerts if missing email address
                beforecheckchange: function(column, rowIndex, checked, record) {
                    if (checked && !record.data.emailAddress) {
                        return false;
                    }
                }
            }
        }],
        editorFields: [{
            xtype: 'textfield',
            bind: '{record.username}',
            fieldLabel: 'Username'.t(),
            allowBlank: false,
            emptyText: '[enter username]'.t(),
            blankText: 'The username cannot be blank.'.t()
        },
        Field.description,
        {
            xtype: 'textfield',
            bind: '{record.emailAddress}',
            fieldLabel: 'Email Address'.t(),
            allowOnlyWhitespace: false,
            emptyText: '[no email]'.t(),
            vtype: 'email'
        }, {
            xtype: 'checkbox',
            disabled: true,
            bind: {
                value: '{record.emailAlerts}',
                disabled: '{!record.emailAddress}'
            },
            fieldLabel: 'Email Alerts'.t()
        }, {
            xtype: 'textfield',
            inputType: 'password',
            bind: '{record.password}',
            fieldLabel: 'Password'.t(),
            allowBlank: false,
            minLength: 3,
            minLengthText: Ext.String.format('The password is shorter than the minimum {0} characters.'.t(), 3)
        }, {
            xtype: 'textfield',
            inputType: 'password',
            fieldLabel: 'Confirm Password'.t(),
            allowBlank: false
        }]
    }, {
        xtype: 'panel',
        region: 'south',
        height: 'auto',
        border: false,
        bodyPadding: 10,
        items: [{
            xtype: 'checkbox',
            fieldLabel: 'Allow HTTP Administration'.t(),
            labelAlign: 'right',
            labelWidth: 250,
            bind: '{systemSettings.httpAdministrationAllowed}'
        }, {
            xtype: 'textfield',
            name: 'administrationSubnets',
            fieldLabel: 'Restrict Administration Subnet(s)'.t(),
            vtype: 'ipMatcher',
            labelAlign: 'right',
            maxWidth: 400,
            labelWidth: 250,
            bind: '{systemSettings.administrationSubnets}'
        }, {
            xtype: 'textfield',
            fieldLabel: 'Default Administration Username Text'.t(),
            labelAlign: 'right',
            maxWidth: 400,
            labelWidth: 250,
            bind: '{adminSettings.defaultUsername}'
        }, {
            xtype: 'fieldset',
            title: 'Note:'.t(),
            padding: 10,
            items: [{
                xtype: 'label',
                html: 'HTTP is open on non-WANs (internal interfaces) for blockpages and other services.'.t() + '<br/>' +
                    'This settings only controls the availability of <b>administration</b> via HTTP.'.t()
            }]
        }]
    }]

});
