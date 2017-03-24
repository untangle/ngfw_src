Ext.define('Ung.view.main.MainModel', {
    extend: 'Ext.app.ViewModel',

    alias: 'viewmodel.main',

    data: {
        reportsInstalled: false,
        reportsRunning: false,
        dashboardManagerOpen: false
    },
    formulas: {
        // selected: function () {
        //     return 'dashboard';
        // },
        // reports are enabled only if are installed and has running state
        reportsEnabled: function (get) {
            return (get('reportsInstalled') && get('reportsRunning'));
        },
        isDashboard: function(get) {
            return get('activeItem') === 'dashboard';
        },
        isApps: function(get) {
            return get('activeItem') === 'apps';
        },
        isConfig: function(get) {
            return get('activeItem') === 'config';
        },
        isReports: function(get) {
            return get('activeItem') === 'reports';
        },
        isSessions: function(get) {
            return get('shdActiveItem') === 'sessions';
        },
        isHosts: function(get) {
            return get('shdActiveItem') === 'hosts';
        },
        isDevices: function(get) {
            return get('shdActiveItem') === 'devices';
        }
    }
});
