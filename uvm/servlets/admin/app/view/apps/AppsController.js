Ext.define('Ung.view.apps.AppsController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.apps',

    control: {
        '#': { afterrender: 'onAfterRender', activate: 'onActivate' },
        '#installableApps > dataview': { select: 'onInstallApp' }
    },
    listen: {
        store: {
            '#policiestree': {
                rootchange: 'onRootChange'
            }
        }
    },

    onActivate: function () {
        this.getApps();
    },

    onAfterRender: function () {
        var me = this, vm = this.getViewModel();
        // when policy changes get the apps, this is needed because
        vm.bind('{policyId}', function (val) {
            me.getApps();
        });
    },

    onRootChange: function () {
        var me = this, menuItems = [], vm = me.getViewModel();

        if (Ext.getStore('policiestree').getCount() > 0) {
            Ext.getStore('policiestree').each(function (node) {
                menuItems.push({
                    margin: '0 0 0 ' + (node.get('depth') - 1) * 20,
                    text: '<strong>' + node.get('name') + '</strong>',
                    iconCls: node.get('iconCls') + ' fa-lg',
                    href: '#apps/' + node.get('policyId'),
                    // href: '#apps/' + node.get('slug'),
                    hrefTarget: '_self'
                });
            });

            menuItems.push('-');
            menuItems.push({
                text: 'Manage Policies',
                iconCls: 'fa fa-cog fa-lg',
                href: '#service/policy-manager/policies',
                hrefTarget: '_self'
            });

            me.lookup('policyBtn').setMenu({
                plain: true,
                mouseLeaveDelay: 0,
                items: menuItems
            });
            vm.set('policyMenu', true);

            var policyNode = Ext.getStore('policiestree').findNode('policyId', vm.get('policyId'));
            vm.set('policyName', policyNode.get('name'));

        } else {
            vm.set('policyMenu', false);
        }

        me.getApps(); // set when route changed
    },

    refs: {
        installedApps: '#installedApps',
        installableApps: '#installableApps'
    },

    getApps: function () {
        var me = this, vm = this.getViewModel(), instance;

        // we need policies store before fetching apps
        if (Ext.getStore('policiestree').getCount() === 0) { return; }

        me.getView().setLoading(true);

        Rpc.asyncData('rpc.appManager.getAppsView', vm.get('policyId'))
            .then(function (policy) {
                me.getView().setLoading(false);
                var apps = [];
                vm.getStore('apps').removeAll();

                Ext.Array.each(policy.appProperties.list, function (app) {
                    var _app = {
                        name: app.name,
                        displayName: app.displayName,
                        route: app.type === 'FILTER' ? '#apps/' + policy.policyId + '/' + app.name : '#service/' + app.name,
                        type: app.type,
                        hasPowerButton: app.hasPowerButton,
                        viewPosition: app.viewPosition,
                        targetState: null,
                        desc: Util.appDescription[app.name],
                        extraCls: 'installed'
                    };
                    instance = Ext.Array.findBy(policy.instances.list, function(instance) { return instance.appName === app.name; });
                    if (instance) {
                        _app.targetState = instance.targetState;
                        if (instance.policyId && policy.policyId !== instance.policyId) {
                            _app.parentPolicy = Ext.getStore('policiestree').findNode('policyId', instance.policyId).get('name');
                        }
                    }
                    apps.push(_app);
                });

                Ext.Array.each(policy.installable.list, function (app) {
                    apps.push({
                        name: app.name,
                        displayName: app.displayName,
                        route: app.type === 'FILTER' ? '#apps/' + policy.policyId + '/' + app.name : '#service/' + app.name,
                        type: app.type,
                        viewPosition: app.viewPosition,
                        targetState: null,
                        desc: Util.appDescription[app.name],
                        hasPowerButton: app.hasPowerButton,
                        extraCls: 'installable'
                    });
                });
                vm.getStore('apps').loadData(apps);
            });
    },

    showInstall: function () {
        var me = this, vm = this.getViewModel();
        me.getView().setActiveItem('installableApps');
        vm.set('onInstalledApps', false);
    },

    backToApps: function () {
        var me = this, vm = this.getViewModel();
        me.getView().setActiveItem('installedApps');
        vm.set('onInstalledApps', true);
    },

    setPolicy: function (combo, newValue, oldValue) {
        if (oldValue !== null) {
            this.redirectTo('#apps/' + newValue, false);
            // this.updateApps();
        }
    },

    /**
     * method which initialize the app installation
     */
    onInstallApp: function (view, record) {
        var me = this, vm = me.getViewModel();
        record.set('extraCls', 'progress');

        // find and remove App item if it's from a parent policy
        var parentAppIdx = vm.getStore('apps').findBy(function (rec) {
            return rec.get('name') === record.get('name') && rec.get('parentPolicy');
        });
        if (parentAppIdx >= 0) { vm.getStore('apps').removeAt(parentAppIdx); }

        Rpc.asyncData('rpc.appManager.instantiate', record.get('name'), vm.get('policyId'))
        .then(function (result) {
            record.set('extraCls', 'finish');
            record.set('targetState', result.getRunState());

            if (record.get('name') === 'reports') { // just reload the page for now
                window.location.href = '/admin/index.do';
                return;
            }

            if (record.get('name') === 'policy-manager') { // build the policies tree
                Ext.getStore('policiestree').build();
            }

            Rpc.asyncData('rpc.appManager.getAppsViews')
                .then(function (policies) {
                    Ext.getStore('policies').loadData(policies);
                });

            Ext.fireEvent('appinstall');
            // me.getApps();
        });
    }

});
