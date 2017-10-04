Ext.define('Ung.apps.directoryconnector.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-directory-connector',
    controller: 'app-directory-connector',

    viewModel: {
        type: 'app-directory-connector',
    },

    tabBar: {
        items: [{
            xtype: 'component',
            margin: '0 0 0 10',
            cls: 'view-reports',
            autoEl: {
                tag: 'a',
                href: '#reports/directory-connector',
                html: '<i class="fa fa-line-chart"></i> ' + 'View Reports'.t()
            },
            hidden: true,
            bind: {
                hidden: '{instance.runState !== "RUNNING"}'
            }
        }]
    },

    items: [
        { xtype: 'app-directory-connector-status' },
        { xtype: 'app-directory-connector-usernotificationapi' },
        { xtype: 'app-directory-connector-activedirectory' },
        { xtype: 'app-directory-connector-radius' },
        { xtype: 'app-directory-connector-google' }
    ]

});
