Ext.define('Ung.apps.reports.view.Users', {
    extend: 'Ung.cmp.Grid',
    alias: 'widget.app-reports-users',
    itemId: 'users',
    title: 'Reports Users'.t(),

    controller: 'unreportsusersgrid',

    dockedItems: [{
        xtype: 'toolbar',
        dock: 'top',
        items: [{
            xtype: 'tbtext',
            padding: '8 5',
            style: { fontSize: '12px', fontWeight: 600 },
            html: 'Reports Users are users that can view reports and receive email reports but do not have administration privileges.'.t()
        }]
    }, {
        xtype: 'toolbar',
        dock: 'top',
        items: ['@add', '->', '@import', '@export']
    }],

    recordActions: ['changePassword', 'edit', 'delete'],

    listProperty: 'settings.reportsUsers.list',
    emptyRow: {
        emailAddress: '',
        emailSummaries: true,
        emailAlerts: false,
        emailTemplateIds: {
            javaClass: 'java.util.LinkedList',
            list: [1]
        },
        onlineAccess: false,
        password: null,
        passwordHashBase64: null,
        javaClass: 'com.untangle.app.reports.ReportsUser',
    },

    bind: '{users}',

    columns: [{
        header: 'Email Address (username)'.t(),
        width: 200,
        dataIndex: 'emailAddress',
        editor: {
            xtype: 'textfield',
            emptyText: '[enter email address]'.t(),
            allowBlank: false,
            blankText: 'The email address cannot be blank.'.t()
        }
    }, {
        xtype: 'checkcolumn',
        width: 100,
        header: 'Email Alerts'.t(),
        dataIndex: 'emailAlerts',
        resizable: false
    }, {
        xtype: 'checkcolumn',
        width: 100,
        header: 'Email Reports'.t(),
        dataIndex: 'emailSummaries',
        resizable: false
    }, {
        header: 'Email Templates'.t(),
        flex: 1,
        dataIndex: 'emailTemplateIds',
        renderer: 'emailTemplatesRenderer'
    }, {
        xtype: 'checkcolumn',
        header: 'Online Access'.t(),
        width: 100,
        dataIndex: 'onlineAccess',
        resizable: false
    }],

    editorFields: [{
        xtype: 'textfield',
        bind: '{record.emailAddress}',
        fieldLabel: 'Email Address (username)'.t(),
        emptyText: '[enter email address]'.t(),
        allowBlank: false,
        width: 300,
        blankText: 'The email address name cannot be blank.'.t(),
    }, {
        xtype: 'checkbox',
        bind: '{record.emailAlerts}',
        fieldLabel: 'Email Alerts'.t()
    }, {
        xtype: 'checkbox',
        bind: '{record.emailSummaries}',
        fieldLabel: 'Email Reports'.t()
    }, {
        xtype: 'unemailtemplateselect',
        bind: {
            store: '{record.emailTemplateIds.list}',
            disabled: '{record.emailSummaries == false}',
            hidden: '{record.emailSummaries == false}',
        },
        fieldLabel: 'Email Templates'.t(),
        disabled: true,
        hidden: true,
    }]
});
