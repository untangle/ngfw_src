Ext.define('Ung.controller.Global', {
    extend: 'Ext.app.Controller',
    namespace: 'Ung',

    /* requires-start */
    requires: [
        'Ung.util.Rpc',
        'Ung.util.Util',
        'Ung.util.Metrics',
        'Ung.view.main.Main',
        'Ung.overrides.data.SortTypes',
        'Ung.overrides.form.field.VTypes',
        'Ung.overrides.LoadMask',
        'Ung.view.extra.Sessions',
        'Ung.view.extra.Hosts',
        'Ung.view.extra.Devices',
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

        'ReportsTree',
        'PoliciesTree'
    ],

    listen: {
        global: {
            appinstall: 'onAppInstall'
        }
    },

    config: {
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
            'apps': 'onApps',
            'apps/:policyId': 'onApps',
            'apps/:policyId/:app': 'onApps',
            'apps/:policyId/:app/:view': 'onApps',
            'service/:app': 'onService',
            'service/:app/:view': 'onService',

            'config': 'onConfig',
            'config/:configName': 'onConfig',
            'config/:configName/:configView': 'onConfig',
            'reports': 'onReports',
            'reports/:category': 'onReports',
            'reports/:category/:entry': 'onReports',
            'sessions': 'onSessions',
            'hosts': 'onHosts',
            'devices': 'onDevices',
            'users': 'onUsers'

        },

        reportsEnabled: true
    },

    onAppInstall: function () {
        // refetch current applications and rebuild reports tree
        if (rpc.reportsManager) {
            Rpc.asyncData('rpc.reportsManager.getCurrentApplications').then(function (result) {
                Ext.getStore('categories').loadData(Ext.Array.merge(Util.baseCategories, result.list));
                Ext.getStore('reportstree').build();
            });
        }
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

        policyId = policyId || 1;
        if (!app) {
            Ung.app.redirectTo('#apps/' + policyId);
        }

        this.getMainView().getViewModel().set('activeItem', 'apps');
        this.getMainView().getViewModel().set('policyId', policyId);

        this.getAppsView().setActiveItem('installedApps');
        this.getAppsView().getViewModel().set('onInstalledApps', true);

        if (app) {
            me.loadApp(policyId, app, view);
        }
    },

    onService: function (app, view) {
        var me = this;
        this.getMainView().getViewModel().set('activeItem', 'apps');
        if (app) {
            me.loadApp(null, app, view);
        }


    },


    loadApp: function (policyId, app, view) {
        var me = this, mainView = me.getMainView();
        if (mainView.down('app-' + app)) {
            // if app card already exists activate it and select given view
            mainView.getViewModel().set('activeItem', 'appCard');
            mainView.down('app-' + app).setActiveItem(view || 0);
            return;
        } else {
            // eventually do not remove the old card
            mainView.remove('appCard');
        }

        if (!policyId) { policyId = 1;}

        var policy = Ext.getStore('policies').findRecord('policyId', policyId);
        var appInstance = Ext.Array.findBy(policy.get('instances').list, function (inst) {
            return inst.appName === app;
        });
        var appProps = Ext.Array.findBy(policy.get('appProperties').list, function (prop) {
            return prop.name === app;
        });

        mainView.setLoading(true);
        Ext.Loader.loadScript({
            //url: 'script/apps/' + app + '.js',
            // This hack changes the name of ad-blocker to ab.js
            // NGFW-10728
            url: 'script/apps/' + (app=='ad-blocker'?'ab':app) + '.js',
            onLoad: function () {
                Rpc.asyncData('rpc.appManager.app', appInstance.id)
                    .then(function (result) {
                        mainView.add({
                            xtype: 'app-' + app,
                            itemId: 'appCard',
                            appManager: result,
                            activeTab: view || 0,
                            viewModel: {
                                data: {
                                    // policyId: policyId,
                                    instance: appInstance,
                                    props: appProps,
                                    license: policy.get('licenseMap').map[app],
                                    urlName: app,
                                    runState: result.getRunState()
                                }
                            },
                            listeners: {
                                deactivate: function () {
                                    // remove the app container
                                    mainView.remove('appCard');
                                }
                            }
                        });
                        mainView.getViewModel().set('activeItem', 'appCard');
                        mainView.getViewModel().notify();
                    }, function (ex) {
                        Util.handleException(ex);
                    }).always(function () {
                        mainView.setLoading(false);
                    });
            }
        });
    },


    onConfig: function (config, view) {
        var me = this, mainView = me.getMainView();
        mainView.getViewModel().set('activeItem', 'config');
        if (config) {
            if (mainView.down('config-' + config)) {
                // if config card already exists activate it and select given view
                mainView.getViewModel().set('activeItem', 'configCard');
                mainView.down('config-' + config).setActiveItem(view || 0);
                return;
            } else {
                mainView.remove('configCard');
            }
            mainView.setLoading(true);
            Ext.Loader.loadScript({
                url: 'script/config/' + config + '.js',
                onLoad: function () {
                    mainView.add({
                        xtype: 'config-' + config,
                        name: config,
                        itemId: 'configCard',
                        activeTab: view || 0
                    });
                    mainView.getViewModel().set('activeItem', 'configCard');
                    mainView.getViewModel().notify();
                    mainView.setLoading(false);
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
        this.getMainView().getViewModel().set('activeItem', 'reports');
    },

    onSessions: function () {
        this.getMainView().add({
            xtype: 'ung.sessions',
            itemId: 'sessions'
        });
        this.getMainView().getViewModel().set('activeItem', 'sessions');
    },

    onHosts: function () {
        this.getMainView().add({
            xtype: 'ung.hosts',
            itemId: 'hosts'
        });
        this.getMainView().getViewModel().set('activeItem', 'hosts');
    },

    onDevices: function () {
        this.getMainView().add({
            xtype: 'ung.devices',
            itemId: 'devices'
        });
        this.getMainView().getViewModel().set('activeItem', 'devices');
    },

    onUsers: function () {
        this.getMainView().add({
            xtype: 'ung.users',
            itemId: 'users'
        });
        this.getMainView().getViewModel().set('activeItem', 'users');
    }
});
