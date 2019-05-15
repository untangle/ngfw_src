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
        'Rule',

        'ReportsTree',
        'PoliciesTree'
    ],

    listen: {
        controller: {
            '#': {
                unmatchedroute: 'onUnmatchedRoute'
            }
        },
        global: {
            appinstall: 'onAppAction',
            appremove: 'onAppAction',
            resetfields: 'onResetFields', // used to reset the fields after saving the settings
            invalidquery: 'onUnmatchedRoute'
        }
    },

    hashBackup: '', // used to revert the hash when user changes are detected in Apps or Configs

    config: {
        refs: {
            mainView: '#main',
            dashboardView: '#dashboardMain',
            appsView: '#apps',
            reportsView: '#reports',
        },

        routes: {
            '': { before: 'detectChanges', action: 'onRoute' },
            'dashboard:params': {
                before: 'detectChanges',
                action: 'onRoute',
                conditions: {
                    ':params' : '(.*)'
                }
            },
            'reports:params': {
                before: 'detectChanges',
                action: 'onRoute',
                conditions: {
                    ':params' : '(.*)'
                }
            },

            'apps': { before: 'detectChanges', action: 'onApps' },
            'apps/:policyId': { before: 'detectChanges', action: 'onApps' },
            'apps/:policyId/:app': { before: 'detectChanges', action: 'onApps' },
            'apps/:policyId/:app/:view': 'onApps',
            'apps/:policyId/:app/:view/:subView': 'onApps',
            'service/:app': { before: 'detectChanges', action: 'onService' },
            'service/:app/:view': 'onService',
            'service/:app/:view/:subView': 'onService',

            'config': { before: 'detectChanges', action: 'onConfig' },
            'config/:configName': { before: 'detectChanges', action: 'onConfig' },
            'config/:configName/:configView': 'onConfig',
            'config/:configName/:configView/:subView': 'onConfig',
            'config/:configName/:configView/:subView/:subView': 'onConfig',
            'config/:configName/:configView/:subView/:subView/:subView': 'onConfig',

            'sessions': { before: 'detectChanges', action: 'onSessions' },
            'sessions/:params': {
                action: 'onSessions',
                conditions: {
                    ':params' : '([0-9a-zA-Z.?&=-]+)'
                }
            },
            'hosts': { before: 'detectChanges', action: 'onHosts' },
            'hosts/:params': {
                action: 'onHosts',
                conditions: {
                    ':params' : '([0-9a-zA-Z.?&=-]+)'
                }
            },
            'devices': { before: 'detectChanges', action: 'onDevices' },
            'devices/:params': {
                action: 'onDevices',
                conditions: {
                    ':params' : '([0-9a-zA-Z.?&=-]+)'
                }
            },
            'users': { before: 'detectChanges', action: 'onUsers' },
            'expert': 'setExpertMode',
            'noexpert': 'setNoExpertMode'
        }
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
            if (!field._neverDirty && field._isChanged && !dirtyFields) {
                dirtyFields = true;
            }
        });
        // check for grids changes
        Ext.Array.each(cmp.query('ungrid'), function (grid) {
            var store = grid.getStore();
            if (store.type === 'chained') { return; }
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

    /**
     * Common method used for routing Dashboard and Reports based on conditions query
     */
    onRoute: function (query) {
        var hash = window.location.hash, view, viewModel = null, validQuery = true,
            route = {}, conditions = [], condsQuery = '',
            decoded, parts, key, sep, val, fmt, table;

        if (hash === '' || Ext.String.startsWith(hash, '#') || Ext.String.startsWith(hash, '#dashboard')) {
            view = 'dashboardMain';
            viewModel = this.getDashboardView().getViewModel();
            Ung.app.conditionsContext = 'DASHBOARD';
        }
        if (Ext.String.startsWith(hash, '#reports')) {
            view = 'reports';
            viewModel = this.getReportsView().getViewModel();
            Ung.app.conditionsContext = 'REPORTS';
        }

        if (query) {
            Ext.Array.each(query.replace('?', '').split('&'), function (part) {
                decoded = decodeURIComponent(part);

                if (decoded.indexOf(':') > 0) {
                    parts = decoded.split(':');
                    key = parts[0];
                    sep = parts[1];
                    val = parts[2];
                    fmt = parseInt(parts[3], 10);
                    table = parts[4];
                } else {
                    parts = decoded.split('=');
                    key = parts[0];
                    val = parts[1];
                }
                if (key === 'cat' || key === 'rep') {
                    route[key] = Util.urlEncode(val);
                } else {
                    if (!key || !sep || !val) {
                        validQuery = false;
                    } else {
                        conditions.push({
                            column: key,
                            operator: sep,
                            value: val,
                            autoFormatValue: fmt === 1 ? true : false,
                            table: table,
                            javaClass: 'com.untangle.app.reports.SqlCondition'
                        });
                        condsQuery += '&' + key + ':' + encodeURIComponent(sep) + ':' + encodeURIComponent(val) + ':' + fmt + (table ? ':' + encodeURIComponent(table) : '');
                    }
                }
            });
        }

        if (!validQuery) {
            Ext.fireEvent('invalidquery');
            return;
        }

        if(viewModel != null){
            viewModel.set('query', {
                route: route,
                conditions: conditions,
                string: condsQuery
            });
        }

        this.getMainView().getViewModel().set('activeItem', view);
    },

    onUnmatchedRoute: function () {
        this.getMainView().getViewModel().set('activeItem', 'invalidRoute');
    },

    onApps: function (policyId, app, view, subView) {
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
            me.loadApp(policyId, app, view, subView);
        }
    },

    onAppAction: function () {
        if (Rpc.exists('rpc.reportsManager')) {
            Rpc.asyncData('rpc.reportsManager.getCurrentApplications').then(function (result) {
                Ext.getStore('categories').loadData(Ext.Array.merge(Util.baseCategories, result.list));
                Ext.getStore('reportstree').build();
            });
        }
    },

    onService: function (app, view, subView) {
        var me = this;
        this.getMainView().getViewModel().set('activeItem', 'apps');
        if (app) {
            me.loadApp(null, app, view, subView);
        }


    },

    loadApp: function (policyId, app, view, subView) {
        var subViews = [];
        for( var i = 3; i < arguments.length; i++){
            if(typeof(arguments[i]) != 'string'){
                break;
            }
            subViews.push(arguments[i]);
        }
        var me = this, mainView = me.getMainView();
        if (mainView.down('app-' + app)) {
            // if app card already exists activate it and select given view
            mainView.getViewModel().set('activeItem', 'appCard');
            var viewTarget = mainView.down('app-' + app).setActiveItem(view || 0);
            mainView.down('app-' + app).subViews = subViews;
            Ung.controller.Global.onSubtabActivate(viewTarget);
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
            // Util.handleException('Unable to find app: ' + app);
            Ext.fireEvent('invalidquery');
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
                            subViews: subViews || [],
                            viewModel: {
                                data: {
                                    instance: appInstance,
                                    policyName: me.getAppsView().getViewModel().get('policyName'),
                                    props: appProps,
                                    license: policy.get('licenseMap')[app],
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
                        var appViewModel = mainView.down('app-' + app).getViewModel();
                        appViewModel.set('state', Ext.create('Ung.model.AppState',{vm: appViewModel, app: result}));
                    }, function (ex) {
                        Util.handleException(ex);
                    }).always(function () {
                        mainView.setLoading(false);
                    });
            }
        });
    },

    onConfig: function (config, view, subView) {
        var subViews = [];

        // config must be one of those defined in array, otherwise route is invalid
        if (config && !Ext.Array.contains(['network', 'administration', 'events', 'email', 'local-directory', 'upgrade', 'system', 'about'], config)) {
            Ext.fireEvent('invalidquery');
            return;
        }

        for( var i = 2; i < arguments.length; i++){
            if(typeof(arguments[i]) != 'string'){
                break;
            }
            subViews.push(arguments[i]);
        }
        var me = this, mainView = me.getMainView();
        mainView.getViewModel().set('activeItem', 'config');
        if (config) {
            if (mainView.down('config-' + config)) {
                mainView.getViewModel().set('activeItem', 'configCard');
                var viewTarget = mainView.down('config-' + config).setActiveItem(view || 0);
                mainView.down('config-' + config).subViews = subViews;
                Ung.controller.Global.onSubtabActivate(viewTarget);
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
                        subViews: subViews || [],
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

    setExpertMode: function () {
        rpc.isExpertMode = true;
        this.getMainView().getViewModel().set('isExpertMode', true);
        Ung.app.redirectTo('#apps');
    },

    setNoExpertMode: function () {
        rpc.isExpertMode = false;
        this.getMainView().getViewModel().set('isExpertMode', false);
        Ung.app.redirectTo('#apps');
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
    },

    statics: {
        //
        // These two methods are used on tab panels with their own sub-tab panels and added
        // to the controller.  See openvpn/server and virus-blocker/advanced.
        //
        activateTaskDelay: 250,
        activateTaskDelayMax: 5000,
        onSubtabActivate: function(panel){
            var me = this;
            var parentPanel = panel.up('apppanel') || panel.up('configpanel');

            // While we're setting tabs, don't trigger the subtab activation.
            // Doing so will loop back into onConfig for a partial path, short-circuiting
            // the loop wer'e doing here.
            Ung.controller.Global.ignoreActivate = true;

            parentPanel.subViews.forEach(function(subView){
                var targetPanel = panel.down('[itemId='+subView+']');
                if(!targetPanel){
                    return;
                }
                var parentPanel = targetPanel.up('tabpanel');
                parentPanel.setActiveItem(subView);
                panel = targetPanel;
                if(parentPanel.disabled){
                    return false;
                }

                if(targetPanel.tab){
                    // For deeply nested tabs, settng the active item sets the tab panel
                    // properly but not the tab itself.  To verify tis properly set, we
                    // spawn a delayed task to keep trying to change the tabbar manually.
                    var activateTaskExpire = (new Date().getTime() / 1000) + 5000;
                    var runActivateTask = new Ext.util.DelayedTask( Ext.bind(function(){
                        if(parentPanel.destroyed){
                            return;
                        }
                        if( ( parentPanel.tabBar.activeTab != targetPanel.tab) &&
                            ( activateTaskExpire >  ( new Date().getTime() / 1000) ) ){
                            runActivateTask.delay( Ung.controller.Global.activateTaskDelay );
                        }
                        parentPanel.tabBar.setActiveTab(targetPanel.tab);
                    }, me) );
                    runActivateTask.delay( Ung.controller.Global.activateTaskDelay );
                }
            });
            Ung.controller.Global.ignoreActivate = false;
        },

        ignoreActivate: false,
        onBeforeSubtabChange: function (tabPanel, card, oldCard) {
            if(Ung.controller.Global.ignoreActivate){
                return;
            }
            var hash = window.location.hash;
            var id = tabPanel.itemId;
            if( id && hash.indexOf(id) > -1 ){
                Ung.app.redirectTo(hash.substr(0,hash.indexOf(id) + id.length) + '/' + card.getItemId());
            }
        }
    }
});
