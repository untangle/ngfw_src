Ext.define('Ung.view.main.MainController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.main',

    control: {
        '#': {
            beforerender: 'onBeforeRender'
        }
    },

    init: function (view) {
        var vm = view.getViewModel();
        //view.getViewModel().set('widgets', Ext.getStore('widgets'));
        vm.set('reports', Ext.getStore('reports'));
        vm.set('policyId', 1);
    },

    // routes: {
    //     '': 'onDashboard',
    //     'apps': 'onApps',
    //     'apps/:policyId': 'onApps',
    //     'apps/:policyId/:node': 'onApps',
    //     'config': 'onConfig',
    //     'config/:configName': 'onConfig',
    //     'reports': 'onReports',
    //     'sessions': 'onSessions',
    //     'hosts': 'onHosts',
    //     'devices': 'onDevices'
    // },

    onBeforeRender: function(view) {
        var vm = view.getViewModel();

        // vm.bind('{reportsEnabled}', function(enabled) {
        //     if (enabled) {
        //         view.down('#main').insert(3, {
        //             xtype: 'ung.reports',
        //             itemId: 'reports'
        //         });
        //     } else {
        //         view.down('#main').remove('reports');
        //     }
        // });

        vm.set('reportsInstalled', rpc.appManager.app('reports') !== null);
        if (rpc.appManager.app('reports')) {
            vm.set('reportsRunning', rpc.appManager.app('reports').getRunState() === 'RUNNING');
        }
        vm.notify();
        /*
        setTimeout(function () {
            vm.set('reportsInstalled', false);
        }, 5000);
        */

        view.getViewModel().set('policies', Ext.getStore('policies'));
        view.getViewModel().set('policy', Ext.getStore('policies').findRecord('policyId', 1));
        //this.getViewModel().set('activeItem', Ext.util.History.getHash());
    },

    // afterRender: function () {
    //     this.redirectTo(Ext.util.History.getHash(), true);
    // },

    onDashboard: function () {
        console.log('on dashboard');
        this.getViewModel().set('activeItem', 'dashboard');
    },

    onApps: function (policyId, node) {
        console.log('on apps');
        var vm = this.getViewModel();
        // var _policyId = policyId || 1,
        //     _currentPolicy = vm.get('policyId'),
        //     _newPolicy;

        //if (!_currentPolicy || _currentPolicy.get('policyId') !== policyId) {
            //_newPolicy = Ext.getStore('policies').findRecord('policyId', _policyId) || Ext.getStore('policies').findRecord('policyId', 1);

        vm.set('policyId', policyId);
        //}
        // var view = 'Ung.view.apps.Apps';
        // var ctrl = Ung.app.getController('Ung.view.apps.AppsController');

        if (node) {
            if (node === 'install') {
                vm.set('activeItem', 'appsinstall');
            } else {
                vm.set('appName', node);
                vm.set('activeItem', 'settings');
            }
        } else {
            vm.set('activeItem', 'apps');
        }
    },

    onConfig: function (configName) {
        this.getViewModel().set('activeItem', 'config');
        var view = this.getView();
        this.getViewModel().set('activeItem', 'config');
        if (configName) {
            Ext.require('Ung.view.config.network.Network', function () {
                view.down('#config').add({
                    xtype: 'ung.config.network'
                });
                view.down('#config').setActiveItem(1);
            });
        } else {
            view.down('#config').setActiveItem(0);
        }
    },

    onReports: function () {
        this.getViewModel().set('activeItem', 'reports');
    },


    // sessions, hosts, devices

    onSessions: function () {
        // this.setShd('sessions');
        this.getViewModel().set('activeItem', 'sessions');
    },

    onHosts: function () {
        this.setShd('hosts');
    },

    onDevices: function () {
        this.setShd('devices');
    },

    setShd: function (viewName) {
        this.getViewModel().set('activeItem', 'shd');
        this.getViewModel().set('shdActiveItem', viewName);
        this.getView().down('#shdcenter').setActiveItem(viewName);
    }

});
