Ext.define('Ung.controller.Global', {
    extend: 'Ext.app.Controller',
    namespace: 'Ung',

    requires: [
        'Ung.rpc.Rpc',
        'Ung.util.Util',
        'Ung.util.Metrics',
        'Ung.view.main.Main',
        'Ung.overrides.form.field.VTypes',
        'Ung.view.shd.Sessions',
        'Ung.view.shd.Hosts'
    ],


    stores: [
        'Policies',
        'Metrics',
        'Stats',
        'Reports',
        'Widgets',
        'Sessions',
        'Hosts',
        'Conditions',
        'Countries',
        'Categories',
        'UnavailableApps'
    ],

    config: {
        control: {
            '#main': {
                beforerender: 'onBeforeRender'
            },
            '#apps': {
                activate: 'onActivate'
            }
        },

        refs: {
            mainView: '#main',
            appsView: '#apps',
            dashboardView: '#dashboard'
        },

        routes: {
            '': 'onDashboard',
            // 'apps': 'onApps',
            'apps/:policyId': 'onApps',
            'apps/:policyId/:node': 'onApps',
            'config': 'onConfig',
            'config/:configName': 'onConfig',
            'reports': 'onReports',
            'sessions': 'onSessions',
            'hosts': 'onHosts',
            'devices': 'onDevices'
        },

        reportsEnabled: true
    },

    onBeforeRender: function () {
        // console.log('init');
    },

    onActivate: function () {
        console.log('activate');
    },

    onDashboard: function () {
        this.getMainView().setActiveItem('dashboard');
        this.getMainView().getViewModel().set('selectedNavItem', 'dashboard');
        // this.getMainView().setActiveItem('#dashboard');
        // this.getViewModel().set('activeItem', 'dashboard');
    },

    onApps: function (policyId, node) {
        console.log(node);
        this.getMainView().getViewModel().set('selectedNavItem', 'apps');
        this.getMainView().setActiveItem('apps');

        if (node) {
            if (node === 'install') {
                console.log(this.getAppsView());
                this.getAppsView().setActiveItem('installableApps');
            } else {
                // vm.set('nodeName', node);
                // vm.set('activeItem', 'settings');
            }
        } else {
            this.getAppsView().setActiveItem('installedApps');
            // vm.set('activeItem', 'apps');
        }

        // console.log(this);
        // console.log(this.getAppsView());
    },

    onConfig: function () {
        this.getMainView().getViewModel().set('selectedNavItem', 'config');
        this.getMainView().setActiveItem('config');
        // this.getMainView().setActiveItem('#dashboard');
        // this.getViewModel().set('activeItem', 'dashboard');
    },

    // onBeforeSessions: function (id, action) {
    //     rpc.sessionMonitor.getMergedSessions(function (result, exception) {
    //         console.log(result);
    //         // grid.getView().setLoading(false);
    //         if (exception) {
    //             Ung.Util.exceptionToast(exception);
    //             return;
    //         }
    //         // Ext.getStore('sessions').loadData(result.list);
    //         // grid.getSelectionModel().select(0);
    //         // grid.getStore().setData(result.list);
    //     });
    // },

    onSessions: function () {
        var shd = this.getMainView().down('#shd');
        if (shd) {
            // this.getMainView().remove('#shd', true);
            shd.destroy();
        }
        this.getMainView().add({
            xtype: 'ung.sessions',
            itemId: 'shd'
        });
        this.getMainView().getViewModel().set('selectedNavItem', 'sessions');
        this.getMainView().setActiveItem('shd');
    },

    onHosts: function () {
        var shd = this.getMainView().down('#shd');
        if (shd) {
            // this.getMainView().remove('#shd', true);
            shd.destroy();
        }
        this.getMainView().add({
            xtype: 'ung.hosts',
            itemId: 'shd'
        });
        this.getMainView().getViewModel().set('selectedNavItem', 'hosts');
        this.getMainView().setActiveItem('shd');
    }


});