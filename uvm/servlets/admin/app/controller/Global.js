Ext.define('Ung.controller.Global', {
    extend: 'Ext.app.Controller',
    namespace: 'Ung',

    requires: [
        'Ung.rpc.Rpc',
        'Ung.util.Util',
        'Ung.util.Metrics',
        'Ung.view.main.Main',
        'Ung.overrides.form.field.VTypes'
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
            appsView: '#apps'
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
        console.log('init');
    },

    onActivate: function () {
        console.log('activate');
    },

    onDashboard: function () {
        this.getMainView().setActiveItem('dashboard');
        // this.getMainView().setActiveItem('#dashboard');
        // this.getViewModel().set('activeItem', 'dashboard');
    },

    onApps: function (policyId, node) {
        console.log(node);
        // console.log(this.getMainView());
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
    }
});