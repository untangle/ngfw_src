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
        'Rule',

        'ReportsTree'
    ],

    listen: {
        global: {
            appinstall: 'onAppInstall'
        }
    },

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

    onAppInstall: function () {
        // refetch current applications and rebuild reports tree
        Rpc.asyncData('rpc.reportsManager.getCurrentApplications').then(function (result) {
            Ext.getStore('categories').loadData(Ext.Array.merge(Util.baseCategories, result.list));
            Ext.getStore('reportstree').build();
        });
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
        this.getMainView().getViewModel().set('activeItem', 'dashboard');
    },

    onApps: function (policyId, app, view) {
        var me = this;

        this.getMainView().getViewModel().set('activeItem', 'apps');
        console.log(this.getMainView().getViewModel().get('activeItem'));
        if (app) {
            if (app === 'install') {
                this.getMainView().getViewModel().set('activeItem', 'apps');
                this.getAppsView().setActiveItem('installableApps');
            } else {
                if (me.getMainView().down('app-' + app)) {
                    // if app card already exists activate it and select given view
                    me.getMainView().getViewModel().set('activeItem', 'appCard');
                    me.getMainView().down('app-' + app).setActiveItem(view || 0);
                    return;
                } else {
                    // eventually do not remove the old card
                    me.getMainView().remove('appCard');
                }

                var policy = Ext.getStore('policies').findRecord('policyId', policyId);
                var appInstance = Ext.Array.findBy(policy.get('instances').list, function (inst) {
                    return inst.appName.replace('', '').replace('', '') === app;
                });
                var appProps = Ext.Array.findBy(policy.get('appProperties').list, function (prop) {
                    return prop.name.replace('', '').replace('', '') === app;
                });

                // var appClass = Ext.ClassManager.getByAlias('widget.app-' + app);
                me.getMainView().setLoading(true);
                Ext.Loader.loadScript({
                    url: 'script/apps/' + app + '.js',
                    onLoad: function () {
                        Ext.Deferred.sequence([
                            Rpc.asyncPromise('rpc.appManager.app', appInstance.id),
                            // Rpc.asyncPromise('rpc.networkManager.getInterfaceStatus'),
                            // Rpc.asyncPromise('rpc.networkManager.getDeviceStatus'),
                        ], this).then(function (result) {
                            // console.log(result[0]);
                            me.getMainView().add({
                                xtype: 'app-' + app,
                                // region: 'center',
                                itemId: 'appCard',
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
                            // me.getMainView().setActiveItem('configCard');
                            me.getMainView().getViewModel().set('activeItem', 'appCard');
                            me.getMainView().getViewModel().notify();
                            me.getMainView().setLoading(false);
                        }, function (ex) {
                            Util.exceptionToast(ex);
                        });
                    }
                });
            }
        } else {
            console.log('here');
            this.getAppsView().setActiveItem('installedApps');
        }
    },

    onConfig: function (configName, configView) {
        var me = this;

        if (!configName) {
            this.getMainView().getViewModel().set('activeItem', 'config');
        } else {
            me.getMainView().setLoading(true);
            Ext.Loader.loadScript({
                url: 'script/config/' + configName + '.js',
                onLoad: function () {
                    me.getMainView().setLoading(false);
                    me.getMainView().add({
                        xtype: 'config.' + configName,
                        itemId: 'configCard'
                    });
                    me.getMainView().getViewModel().set('activeItem', 'configCard');
                }
            });
        }
    },

    onReports: function (categoryName, reportName) {
        var reportsVm = this.getReportsView().getViewModel();
        var hash = '';
        if (categoryName) {
            hash += categoryName;
        }
        if (reportName) {
            hash += '/' + reportName;
            // reportsVm.set('activeCard', 'report');
        }
        reportsVm.set('hash', hash);
        // if (categoryName) {
        //     reportsVm.set('categoryName', categoryName.replace(/-/g, ' '));
        //     reportsVm.notify(); // !important to notify this before report is tried to be set
        //     if (!reportName) {
        //         reportsVm.set('activeCard', 'category');
        //         reportsVm.set('reportName', null);
        //     } else {
        //         reportsVm.set('reportName', reportName);
        //         reportsVm.set('activeCard', 'report');
        //     }
        // } else {
        //     reportsVm.set('categoryName', null);
        //     reportsVm.set('category', null);
        //     reportsVm.set('reportName', null);
        //     reportsVm.set('report', null);
        //     reportsVm.set('activeCard', 'allCategories');
        // }
        this.getMainView().getViewModel().set('activeItem', 'reports');
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
        this.getMainView().getViewModel().set('activeItem', 'sessions');
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
        this.getMainView().getViewModel().set('activeItem', 'hosts');
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
        this.getMainView().getViewModel().set('activeItem', 'devices');
    }

});
