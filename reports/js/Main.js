Ext.define('Ung.apps.reports.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-reports',

    controller: 'app-reports',

    viewModel: {
        data: {
            googleDriveConfigured: false,
            reportQueueSize: 0
        },
        formulas: {
            driveConfiguredText: function (get) {
                return get('googleDriveConfigured') ? 'The Google Connector is configured'.t() : 'The Google Connector is unconfigured.'.t();
            },
            emailIntervals: function( get ){
                var availableIntervals = [];
                var dbRetentionTime = get('settings').dbRetention * 86400;
                for(var interval in Renderer.timeIntervalMap ){
                    if( interval <= dbRetentionTime ){
                        availableIntervals.push( [ Number(interval), Renderer.timeIntervalMap[interval] ] );
                    }
                }
                return availableIntervals;
            }
        },
        stores: {
            users: { data: '{settings.reportsUsers.list}' },
            hostnames: { data: '{settings.hostnameMap.list}' },
            emailTemplates: { data: '{settings.emailTemplates.list}' },
            reportEntries: { data: '{settings.reportEntries.list}' }
        }
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
