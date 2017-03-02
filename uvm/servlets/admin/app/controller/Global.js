Ext.define('Ung.controller.Global', {
    extend: 'Ext.app.Controller',
    namespace: 'Ung',

    /* requires-start */
    requires: [
        'Ung.util.Rpc',
        'Ung.util.Util',
        'Ung.util.Metrics',
        'Ung.view.main.Main',
        'Ung.overrides.form.field.VTypes',
        'Ung.overrides.LoadMask',
        'Ung.view.shd.Sessions',
        'Ung.view.shd.Hosts',
        'Ung.view.shd.Devices',
        'Ung.config.network.Network'
    ],
    /* requires-end */

    stores: [
        'Policies',
        'Metrics',
        'Stats',
        'Reports',
        'Widgets',
        'Sessions',
        'Hosts',
        'Devices',
        'Conditions',
        'Countries',
        'Categories',
        'UnavailableApps',
        'Rule'
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
            'expert': 'setExpertMode',
            'noexpert': 'setNoExpertMode',
            'apps/:policyId': 'onApps',
            'apps/:policyId/:app': 'onApps',
            'config': 'onConfig',
            'config/:configName': 'onConfig',
            'config/:configName/:configView': 'onConfig',
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

    setExpertMode: function () {
        rpc.isExpertMode = true;
        this.redirectTo(window.location.hash.replace('|expert', ''));
    },

    setNoExpertMode: function () {
        rpc.isExpertMode = false;
        this.redirectTo(window.location.hash.replace('|noexpert', ''));
    },


    onDashboard: function () {
        this.getMainView().setActiveItem('dashboard');
        this.getMainView().getViewModel().set('selectedNavItem', 'dashboard');
        // this.getMainView().setActiveItem('#dashboard');
        // this.getViewModel().set('activeItem', 'dashboard');
    },

    onApps: function (policyId, app) {
        var me = this;
        console.log(app);
        this.getMainView().getViewModel().set('selectedNavItem', 'apps');
        this.getMainView().setActiveItem('apps');

        if (app) {
            if (app === 'install') {
                console.log(this.getAppsView());
                this.getAppsView().setActiveItem('installableApps');
            } else {
                me.getMainView().setLoading(true);
                var policy = Ext.getStore('policies').findRecord('policyId', policyId);
                var nodeInstance = policy.get('instances').list.filter(function (node) {
                    return node.nodeName === Util.appsMapping[app];
                })[0];

                Ext.Loader.loadScript({
                    url: 'script/apps/' + app + '.js',
                    onLoad: function () {
                        Ext.Deferred.sequence([
                            Rpc.asyncPromise('rpc.nodeManager.node', nodeInstance.id),
                            // Rpc.asyncPromise('rpc.networkManager.getInterfaceStatus'),
                            // Rpc.asyncPromise('rpc.networkManager.getDeviceStatus'),
                        ], this).then(function (result) {
                            me.getMainView().setLoading(false);
                            // me.getView().nodeManager = result[0];
                            me.getMainView().add({
                                xtype: 'app.' + app,
                                region: 'center',
                                itemId: 'configCard',
                                appManager: result[0],
                                viewModel: {
                                    data: {
                                        instance: nodeInstance,
                                    }
                                }
                            });
                            me.getMainView().setActiveItem('configCard');
                        }, function (ex) {
                            Util.exceptionToast(ex);
                        });

                        // if (configView) {
                        //     console.log('here');
                        //     me.getMainView().down('#configCard').setActiveItem(configView);
                        // }

                        // console.log('loaded');
                        // Ext.require('Ung.config.network.Network', function () {
                        //     console.log('require');
                        // });
                        // setTimeout(function() {
                        //     me.getMainView().add({
                        //         xtype: 'ung.config.network',
                        //         region: 'center',
                        //         itemId: 'configCard'
                        //     });
                        // }, 1000);
                    }
                });

            }
        } else {
            this.getAppsView().setActiveItem('installedApps');
            // vm.set('activeItem', 'apps');
        }

        // console.log(this);
        // console.log(this.getAppsView());
    },

    onConfig: function (configName, configView) {
        var me = this;
        if (!configName) {
            this.getMainView().getViewModel().set('selectedNavItem', 'config');
            this.getMainView().setActiveItem('config');
        } else {
            me.getMainView().setLoading(true);
            Ext.Loader.loadScript({
                url: 'script/config/' + configName + '.js',
                onLoad: function () {
                    me.getMainView().setLoading(false);
                    me.getMainView().add({
                        xtype: 'config.' + configName,
                        region: 'center',
                        itemId: 'configCard'
                    });
                    me.getMainView().setActiveItem('configCard');

                    // if (configView) {
                    //     console.log('here');
                    //     me.getMainView().down('#configCard').setActiveItem(configView);
                    // }

                    // console.log('loaded');
                    // Ext.require('Ung.config.network.Network', function () {
                    //     console.log('require');
                    // });
                    // setTimeout(function() {
                    //     me.getMainView().add({
                    //         xtype: 'ung.config.network',
                    //         region: 'center',
                    //         itemId: 'configCard'
                    //     });
                    // }, 1000);
                }
            });
        }


        // if (!configName) {
        //     this.getMainView().getViewModel().set('selectedNavItem', 'config');
        //     this.getMainView().setActiveItem('config');
        // } else {
        //     this.getMainView().add({
        //         xtype: 'ung.config.network',
        //         region: 'center',
        //         itemId: 'configCard'
        //     });
        //     this.getMainView().setActiveItem('configCard');
        // }
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
        // var shd = this.getMainView().down('#shd');
        // if (shd) {
        //     // this.getMainView().remove('#shd', true);
        //     shd.destroy();
        // }
        this.getMainView().add({
            xtype: 'ung.sessions',
            itemId: 'sessions'
        });
        this.getMainView().getViewModel().set('selectedNavItem', 'sessions');
        this.getMainView().setActiveItem('sessions');
    },

    onHosts: function () {
        // var shd = this.getMainView().down('#shd');
        // if (shd) {
        //     // this.getMainView().remove('#shd', true);
        //     shd.destroy();
        // }
        this.getMainView().add({
            xtype: 'ung.hosts',
            itemId: 'hosts'
        });
        this.getMainView().getViewModel().set('selectedNavItem', 'hosts');
        this.getMainView().setActiveItem('hosts');
    },

    onDevices: function () {
        // var shd = this.getMainView().down('#shd');
        // if (shd) {
        //     // this.getMainView().remove('#shd', true);
        //     shd.destroy();
        // }
        this.getMainView().add({
            xtype: 'ung.devices',
            itemId: 'devices'
        });
        this.getMainView().getViewModel().set('selectedNavItem', 'devices');
        this.getMainView().setActiveItem('devices');
    }

});
