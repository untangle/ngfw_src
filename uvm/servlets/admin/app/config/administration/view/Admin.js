Ext.define('Ung.config.administration.view.Admin', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config.administration.admin',
    itemId: 'admin',

    viewModel: true,

    title: 'Admin'.t(),
    layout: 'border',

    actions: {
        addAccount: { text: 'Add Account'.t(), iconCls: 'fa fa-plus', handler: 'addAccount' },
    },

    items: [{
        xtype: 'ungrid',
        border: false,
        title: 'Admin Accounts'.t(),
        region: 'center',

        // tbar: ['@addAccount'],
        bind: '{accounts}',


        listProperty: 'adminSettings.users.list',
        tbar: ['@add'],
        recordActions: ['@delete'],

        emptyRow: {
            javaClass: 'com.untangle.uvm.AdminUserSettings',
            username: '',
            description: '',
            emailAddress: '',
            // "emailAlerts": true,
            // "emailSummaries": true,
            passwordHashBase64: null,
            passwordHashShadow: null,
            password: null
        },

        columns: [{
            header: 'Username'.t(),
            width: 150,
            dataIndex: 'username'
        }, {
            header: 'Description'.t(),
            flex: 1,
            dataIndex: 'description'
        }, {
            header: 'Email Address'.t(),
            width: 150,
            dataIndex: 'emailAddress'
        }, {
            header: 'Email Alerts'.t(),
            dataIndex: 'emailAlerts',
            align: 'center',
            renderer: function (value) {
                return value ? '<i class="fa fa-check"></i>' : '<i class="fa fa-minus"></i>';
            }
        }, {
            header: 'Email Summaries'.t(),
            dataIndex: 'emailSummaries',
            align: 'center',
            renderer: function (value) {
                return value ? '<i class="fa fa-check"></i>' : '<i class="fa fa-minus"></i>';
            }
        }, {
            xtype: 'actioncolumn',
            header: 'Change Password'.t(),
            align: 'center',
            width: 130,
            iconCls: 'fa fa-lock',
            handler: 'changePassword'
        }],
        editorFields: [{
            xtype: 'textfield',
            bind: '{record.username}',
            fieldLabel: 'Username'.t(),
            allowBlank: false,
            emptyText: '[enter username]'.t(),
            blankText: 'The username cannot be blank.'.t()
        },
        Fields.description, {
            xtype: 'textfield',
            bind: '{record.emailAddress}',
            fieldLabel: 'Email Address'.t(),
            emptyText: '[no email]'.t(),
            vtype: 'email'
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