Ext.define('Ung.apps.reports.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-reports',

    controller: 'app-reports',

    viewModel: {
        data: {
            googleDriveConfigured: false,
        },
        formulas: {
            driveConfiguredText: function (get) {
                return get('googleDriveConfigured') ? 'The Google Connector is configured'.t() : 'The Google Connector is unconfigured.'.t();
            }
        },
        stores: {
            users: { data: '{settings.reportsUsers.list}' },
            hostnames: { data: '{settings.hostnameMap.list}' }
        }
        // stores: {
        //     reports: {
        //         model: 'Ung.model.Report',
        //         data: '{settings.reportEntries.list}',
        //         groupField: 'category'
        //     }
        // }
    },

    items: [
        { xtype: 'app-reports-status' },
        { xtype: 'app-reports-allreports' },
        { xtype: 'app-reports-data' },
        { xtype: 'app-reports-emailtemplates' },
        { xtype: 'app-reports-users' },
        { xtype: 'app-reports-namemap' }
    ]

});
