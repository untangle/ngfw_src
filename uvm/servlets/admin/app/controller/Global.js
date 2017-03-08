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
            'apps/:policyId/:app/:view': 'onApps',
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
        // console.log('activate');
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

    onApps: function (policyId, app, view) {
        var me = this;

        this.getMainView().getViewModel().set('selectedNavItem', 'apps');

        if (app) {
            if (app === 'install') {
                this.getMainView().setActiveItem('apps');
                this.getAppsView().setActiveItem('installableApps');
            } else {
                if (me.getMainView().down('app-' + app)) {
                    // if app card already exists activate it abd select given view
                    me.getMainView().setActiveItem('configCard');
                    me.getMainView().down('app-' + app).setActiveItem(view || 0);
                    return;
                } else {
                    // eventually do not remove the old card
                    me.getMainView().remove('configCard');
                }

                var policy = Ext.getStore('policies').findRecord('policyId', policyId);
                var appInstance = Ext.Array.findBy(policy.get('instances').list, function (inst) {
                    return inst.nodeName.replace('untangle-node-', '').replace('untangle-casing-', '') === app;
                });
                var appProps = Ext.Array.findBy(policy.get('nodeProperties').list, function (prop) {
                    return prop.name.replace('untangle-node-', '').replace('untangle-casing-', '') === app;
                });

                // var appClass = Ext.ClassManager.getByAlias('widget.app-' + app);
                me.getMainView().setLoading(true);
                Ext.Loader.loadScript({
                    url: 'script/apps/' + app + '.js',
                    onLoad: function () {
                        Ext.Deferred.sequence([
                            Rpc.asyncPromise('rpc.nodeManager.node', appInstance.id),
                            // Rpc.asyncPromise('rpc.networkManager.getInterfaceStatus'),
                            // Rpc.asyncPromise('rpc.networkManager.getDeviceStatus'),
                        ], this).then(function (result) {
                            // console.log(result[0]);
                            me.getMainView().setLoading(false);
                            me.getMainView().add({
                                xtype: 'app-' + app,
                                region: 'center',
                                itemId: 'configCard',
                                appManager: result[0],
                                activeTab: view || 0,
                                viewModel: {
                                    data: {
                                        instance: appInstance,
                                        props: appProps,
                                        urlName: app
                                    }
                                }
                            });
                            me.getMainView().setActiveItem('configCard');
                        }, function (ex) {
                            Util.exceptionToast(ex);
                        });
                    }
                });
            }
        } else {
            this.getMainView().setActiveItem('apps');
            this.getAppsView().setActiveItem('installedApps');
        }
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
                }
            });
        }
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
