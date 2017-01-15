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
        'Ung.view.shd.Hosts',
        'Ung.view.shd.Devices',
        'Ung.view.config.network.Network'
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
            dashboardView: '#dashboard',
            appsView: '#apps',
            reportsView: '#reports',
        },

        routes: {
            '': 'onDashboard',
            // 'apps': 'onApps',
            'apps/:policyId': 'onApps',
            'apps/:policyId/:node': 'onApps',
            'config': 'onConfig',
            'config/:configName': 'onConfig',
            'reports': 'onReports',
            'reports/:category': 'onReports',
            'reports/:category/:entry': 'onReports',
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

    onConfig: function (configName) {
        if (!configName) {
            this.getMainView().getViewModel().set('selectedNavItem', 'config');
            this.getMainView().setActiveItem('config');
        } else {
            this.getMainView().add({
                xtype: 'ung.config.network',
                region: 'center',
                itemId: 'configCard'
            });
            this.getMainView().setActiveItem('configCard');
        }
        // this.getMainView().setActiveItem('#dashboard');
        // this.getViewModel().set('activeItem', 'dashboard');
    },

    onReports: function (category) {
        if (category) {
            this.getReportsView().getViewModel().set('category', category.replace(/-/g, ' '));
        } else {
            this.getReportsView().getViewModel().set('category', null);
        }
        this.getMainView().getViewModel().set('selectedNavItem', 'reports');
        this.getMainView().setActiveItem('reports');
        // console.log(this.getReportsView().getViewModel());
    },

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
    },

    onDevices: function () {
        var shd = this.getMainView().down('#shd');
        if (shd) {
            // this.getMainView().remove('#shd', true);
            shd.destroy();
        }
        this.getMainView().add({
            xtype: 'ung.devices',
            itemId: 'shd'
        });
        this.getMainView().getViewModel().set('selectedNavItem', 'devices');
        this.getMainView().setActiveItem('shd');
    }

});