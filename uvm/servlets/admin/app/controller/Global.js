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
        Rpc.asyncData('rpc.reportsManager.getCurrentApplications').then(function (result) {
            Ext.getStore('categories').loadData(Ext.Array.merge(Util.baseCategories, result.list));
            Ext.getStore('reportstree').build();
        });
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

        if (!policyId) {
            Ung.app.redirectTo('#apps/1');
            return;
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
        var me = this;
        if (me.getMainView().down('app-' + app)) {
            // if app card already exists activate it and select given view
            me.getMainView().getViewModel().set('activeItem', 'appCard');
            me.getMainView().down('app-' + app).setActiveItem(view || 0);
            return;
        } else {
            // eventually do not remove the old card
            me.getMainView().remove('appCard');
        }

        if (!policyId) { policyId = 1;}

        var policy = Ext.getStore('policies').findRecord('policyId', policyId);
        var appInstance = Ext.Array.findBy(policy.get('instances').list, function (inst) {
            return inst.appName === app;
        });
        var appProps = Ext.Array.findBy(policy.get('appProperties').list, function (prop) {
            return prop.name === app;
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
                    me.getMainView().add({
                        xtype: 'app-' + app,
                        // region: 'center',
                        itemId: 'appCard',
                        appManager: result[0],
                        activeTab: view || 0,
                        viewModel: {
                            data: {
                                // policyId: policyId,
                                instance: appInstance,
                                props: appProps,
                                urlName: app,
                                runState: result[0].getRunState()
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
    },


    onConfig: function (config, view) {
        var me = this;
        me.getMainView().getViewModel().set('activeItem', 'config');
        if (config) {
            if (me.getMainView().down('config-' + config)) {
                // if config card already exists activate it and select given view
                me.getMainView().getViewModel().set('activeItem', 'configCard');
                me.getMainView().down('config-' + config).setActiveItem(view || 0);
                return;
            } else {
                me.getMainView().remove('configCard');
            }
            me.getMainView().setLoading(true);
            Ext.Loader.loadScript({
                url: 'script/config/' + config + '.js',
                onLoad: function () {
                    me.getMainView().add({
                        xtype: 'config-' + config,
                        name: config,
                        itemId: 'configCard',
                        activeTab: view || 0
                    });
                    me.getMainView().getViewModel().set('activeItem', 'configCard');
                    me.getMainView().getViewModel().notify();
                    me.getMainView().setLoading(false);
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
