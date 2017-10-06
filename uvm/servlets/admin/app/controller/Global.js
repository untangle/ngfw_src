Ext.define('Ung.controller.Global', {
    extend: 'Ext.app.Controller',
    namespace: 'Ung',

    /* requires-start */
    requires: [
        'Ung.util.Rpc',
        'Ung.util.Util',
        'Ung.util.Metrics',
        'Ung.view.main.Main',
        'Ung.overrides.controller.Container',
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
            appinstall: 'onAppInstall',
            resetfields: 'onResetFields' // used to reset the fields after saving the settings
        }
    },

    hashBackup: '', // used to revert the hash when user changes are detected in Apps or Configs

    config: {
        refs: {
            mainView: '#main',
            dashboardView: '#dashboard',
            appsView: '#apps',
            reportsView: '#reports',
        },

        routes: {
            '': { before: 'detectChanges', action: 'onDashboard' },
            'apps': { before: 'detectChanges', action: 'onApps' },
            'apps/:policyId': { before: 'detectChanges', action: 'onApps' },
            'apps/:policyId/:app': { before: 'detectChanges', action: 'onApps' },
            'apps/:policyId/:app/:view': 'onApps',
            'service/:app': { before: 'detectChanges', action: 'onService' },
            'service/:app/:view': 'onService',

            'config': { before: 'detectChanges', action: 'onConfig' },
            'config/:configName': { before: 'detectChanges', action: 'onConfig' },
            'config/:configName/:configView': 'onConfig',
            'config/:configName/:configView/:subView': 'onConfig',
            'reports': { before: 'detectChanges', action: 'onReports' },
            'reports/:category': { before: 'detectChanges', action: 'onReports' },
            'reports/:category/:entry': { before: 'detectChanges', action: 'onReports', conditions: { ':entry': '(.*)' } },
            'sessions': { before: 'detectChanges', action: 'onSessions' },
            'sessions/:params': {
                action: 'onSessions',
                conditions: {
                    ':params' : '([0-9a-zA-Z.\?\&=\-]+)'
                }
            },
            'hosts': { before: 'detectChanges', action: 'onHosts' },
            'hosts/:params': {
                action: 'onHosts',
                conditions: {
                    ':params' : '([0-9a-zA-Z.\?\&=\-]+)'
                }
            },
            'devices': { before: 'detectChanges', action: 'onDevices' },
            'devices/:params': {
                action: 'onDevices',
                conditions: {
                    ':params' : '([0-9a-zA-Z.\?\&=\-]+)'
                }
            },
            'users': { before: 'detectChanges', action: 'onUsers' },
            'expert': 'setExpertMode',
            'noexpert': 'setNoExpertMode'
        },

        reportsEnabled: true
    },

    detectChanges: function () {
        var action = arguments[arguments.length - 1]; // arguments length vary, action being the last one
        var cmp = Ung.app.getMainView().down('#appCard') || Ung.app.getMainView().down('#configCard');
        if (!cmp) {
            action.resume(); // resume if there is no app or config view
            return;
        }

        var dirtyFields = false, dirtyGrids = false;
        Ext.Array.each(cmp.query('field'), function (field) {
            if (field._isChanged && !dirtyFields) {
                dirtyFields = true;
            }
        });
        // check for grids changes
        Ext.Array.each(cmp.query('ungrid'), function (grid) {
            var store = grid.getStore();
            if (store.getModifiedRecords().length > 0 || store.getRemovedRecords().length > 0 || store.getNewRecords().length > 0 && !dirtyGrids) {
                dirtyGrids = true;
            }
        });

        if (dirtyFields || dirtyGrids) {
            Ext.MessageBox.confirm('Warning'.t(), 'There are unsaved settings which will be lost. Do you want to continue?'.t(),
            function(btn) {
                if (btn === 'yes') {
                    action.resume(); // if user wants to loose changes move on
                } else {
                    Ung.app.redirectTo(Ung.app.hashBackup); // otherwise keep it in same view and reset the hash to reflect the same view
                }
            });
        } else {
            action.resume();
        }
    },

    onResetFields: function (view) {
        Ext.Array.each(view.query('field'), function (field) {
            if (field.hasOwnProperty('_initialValue')) {
                delete field._initialValue;
            }
            if (field.hasOwnProperty('_isChanged')) {
                delete field._isChanged;
            }
        });
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
        this.getMainView().getViewModel().set('isExpertMode', true);
        Ung.app.redirectTo('#apps');
        // Ung.app.redirectTo(window.location.hash.replace('|expert', ''));
    },

    setNoExpertMode: function () {
        rpc.isExpertMode = false;
        this.getMainView().getViewModel().set('isExpertMode', false);
        Ung.app.redirectTo('#apps');
        // this.redirectTo(window.location.hash.replace('|noexpert', ''));
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

        if (!appInstance || !appProps) {
            Util.handleException("Unable to find app: " + app);
            return;
        }

        mainView.setLoading(true);
        Ext.Loader.loadScript({
            //url: 'script/apps/' + app + '.js',
            // This hack changes the name of ad-blocker to ab.js
            // NGFW-10728
            url: 'script/apps/' + (app=='ad-blocker'?'ab':app) + '.js',
            onLoad: function () {
                Rpc.asyncData('rpc.appManager.app', appInstance.id)
                    .then(function (result) {
                        appInstance.runState = result.getRunState();
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
                                    license: Util.findLicense(policy.get('licenseMap'),app),
                                    urlName: app
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


    onConfig: function (config, view, subView) {
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
                        activeTab: view || 0,
                        subTab: subView || 0,
                        listeners: {
                            deactivate: function () {
                                // remove the config container
                                mainView.remove('configCard');
                            }
                        }
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

    onMonitor: function(id, xtype, params){
        var me = this,
            mainview = me.getMainView();

        var filter = null;
        if (params) {
            filter = {
                property: params.split('=')[0].replace('?', ''),
                value: params.split('=')[1],
                source: 'route'
            };
        }

        var existing = mainview.getComponent( id );
        if(existing){
            existing.routeFilter = filter;
            existing.fireEvent('refresh');
        }else{
            this.getMainView().add({
                xtype: xtype,
                itemId: id,
                routeFilter: filter
            });
        }
        this.getMainView().getViewModel().set('activeItem', id);

    },

    onSessions: function (params) {
        this.onMonitor( 'sessions', 'ung.sessions', params);
    },

    onHosts: function ( params ) {
        this.onMonitor( 'hosts', 'ung.hosts', params);
    },

    onDevices: function ( params ) {
        this.onMonitor( 'devices', 'ung.devices', params);
    },

    onUsers: function () {
        this.getMainView().add({
            xtype: 'ung.users',
            itemId: 'users'
        });
        this.getMainView().getViewModel().set('activeItem', 'users');
    }
});
