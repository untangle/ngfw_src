Ext.define('Ung.apps.reports.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app.reports',

    viewModel: {
        data: {
            nodeName: 'untangle-node-reports',
            appName: 'Reports'
        }
    },

    items: [
        { xtype: 'app.reports.status' },
        { xtype: 'app.reports.allreports' },
        { xtype: 'app.reports.data' },
        { xtype: 'app.reports.alertrules' },
        { xtype: 'app.reports.emailtemplates' },
        { xtype: 'app.reports.users' },
        { xtype: 'app.reports.syslog' },
        { xtype: 'app.reports.namemap' }
    ]

});
