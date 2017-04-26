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
        },
        global: {
            postregistration: 'onPostRegistration',
        }
    },

    onActivate: function () {
        this.getViewModel().set('policyManagerInstalled', rpc.appManager.app('policy-manager') ? true : false);
        this.getApps();
    },

    onAfterRender: function () {
        var me = this, vm = this.getViewModel();
        // when policy changes get the apps, this is needed because
        vm.bind('{policyId}', function (val) {
            if (Ext.getStore('policiestree').getCount() > 0) {
                var policyNode = Ext.getStore('policiestree').findNode('policyId', vm.get('policyId'));
                vm.set('policyName', policyNode.get('name'));
                me.getApps();
            }
        });
    },

    updateCounters: function () {
        var vm = this.getViewModel();
        vm.set({
            appsCount: vm.getStore('installedApps').getCount(),
            servicesCount: vm.getStore('installedServices').getCount()
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

            var policyNode = Ext.getStore('policiestree').findNode('policyId', vm.get('policyId'));
            vm.set('policyName', policyNode.get('name'));

            vm.set('policyMenu', true);
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
        var me = this, vm = this.getViewModel(), instance, license;

        // we need policies store before fetching apps
        if (Ext.getStore('policiestree').getCount() === 0) { return; }

        me.getView().setLoading(true);

        // vm.getStore('apps').removeAll();
        Rpc.asyncData('rpc.appManager.getAppsView', vm.get('policyId'))
            .then(function (policy) {
                me.getView().setLoading(false);
                var apps = [];

                Ext.Array.each(policy.appProperties.list, function (app) {
                    var _app = {
                        name: app.name,
                        displayName: app.displayName,
                        route: app.type === 'FILTER' ? '#apps/' + policy.policyId + '/' + app.name : '#service/' + app.name,
                        type: app.type,
                        hasPowerButton: app.hasPowerButton,
                        viewPosition: app.viewPosition,
                        targetState: null,
                        runState: null,
                        desc: Util.appDescription[app.name],
                        extraCls: 'installed',
                        parentPolicy: null,
                        licenseExpired: false
                    };
                    instance = Ext.Array.findBy(policy.instances.list, function(instance) {
                        return instance.appName === app.name;
                    });
                    if (instance) {
                        _app.targetState = instance.targetState;
                        _app.runState = rpc.appManager.app(instance.id).getRunState();
                        if (instance.policyId && policy.policyId !== instance.policyId) {
                            _app.parentPolicy = Ext.getStore('policiestree').findNode('policyId', instance.policyId).get('name');
                        }
                    }

                    license = policy.licenseMap.map[app.name];
                    if (license) {
                        _app.licenseMessage = Util.getLicenseMessage(license);
                        _app.licenseExpired = license.trial ? license.expired : false;
                    }
                    apps.push(_app);
                });

                Ext.Array.each(policy.installable.list, function (app) {
                    apps.push({
                        name: app.name,
                        displayName: app.displayName,
                        // route: app.type === 'FILTER' ? '#apps/' + policy.policyId + '/' + app.name : '#service/' + app.name,
                        type: app.type,
                        viewPosition: app.viewPosition,
                        targetState: null,
                        runState: null,
                        desc: Util.appDescription[app.name],
                        hasPowerButton: app.hasPowerButton,
                        extraCls: 'installable',
                        parentPolicy: null,
                        licenseExpired: false
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

        if (!rpc.isRegistered) {
            Ext.fireEvent('openregister');
            return;
        }

        if (record.get('extraCls') === 'progress') {
            return;
        }
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
            record.set('route', record.get('type') === 'FILTER' ? '#apps/' + vm.get('policyId') + '/' + record.get('name') : '#service/' + record.get('name'));
            if (record.get('name') === 'reports') { // just reload the page for now
                window.location.href = '/admin/index.do';
                return;
            }

            if (record.get('name') === 'policy-manager') { // build the policies tree
                Ext.getStore('policiestree').build();
                vm.set('policyManagerInstalled', true);
            }

            Rpc.asyncData('rpc.appManager.getAppsViews')
                .then(function (policies) {
                    Ext.getStore('policies').loadData(policies);
                });


            me.updateCounters();
            Ext.fireEvent('appinstall');
            // me.getApps();
        });
    },

    onPostRegistration: function () {
        var me = this;
        Ung.app.redirectTo('#apps');

        var popup = Ext.create('Ext.window.MessageBox', {
            buttons: [{
                name: 'Yes',
                text: 'Yes, install the recommended apps.'.t(),
                handler: function () {
                    var apps = [
                        { displayName: 'Web Filter', name: 'web-filter'},
                        { displayName: 'Bandwidth Control', name: 'bandwidth-control'},
                        { displayName: 'SSL Inspector', name: 'ssl'},
                        { displayName: 'Application Control', name: 'application-control'},
                        { displayName: 'Captive Portal', name: 'captive-portal'},
                        { displayName: 'Firewall', name: 'firewall'},
                        { displayName: 'Reports', name: 'reports'},
                        { displayName: 'Policy Manager', name: 'policy-manager'},
                        { displayName: 'Directory Connector', name: 'directory-connector'},
                        { displayName: 'IPsec VPN', name: 'ipsec-vpn'},
                        { displayName: 'OpenVPN', name: 'openvpn'},
                        { displayName: 'Configuration Backup', name: 'configuration-backup'},
                        { displayName: 'Branding Manager', name: 'branding-manager'},
                        { displayName: 'Live Support', name: 'live-support'}];

                    // only install WAN failover/balancer apps if more than 2 interfaces
                    try {
                        if (rpc.networkSettings.interfaces.list.length > 2) {
                            apps.push({ displayName: 'WAN Failover', name: 'wan-failover'});
                            apps.push({ displayName: 'WAN Balancer', name: 'wan-balancer'});
                        }
                    } catch (e) {}

                    try {
                        var memTotal = Util.bytesToMBs(Ext.getStore('stats').first().get('MemTotal'));
                        if (memTotal && memTotal > 900) {
                            apps.splice(2, 0, { displayName: 'Phish Blocker', name: 'phish-blocker'});
                            apps.splice(2, 0, { displayName: 'Spam Blocker', name: 'spam-blocker'});
                            apps.splice(2, 0, { displayName: 'Virus Blocker Lite', name: 'virus-blocker-lite'});
                            apps.splice(2, 0, { displayName: 'Virus Blocker', name: 'virus-blocker'});
                        }
                    } catch (e) {}

                    popup.close();
                    me.installRecommendedApps(apps);
                }
            }, {
                name: 'No',
                text: 'No, I will install the apps manually.',
                handler: function () {
                    popup.close();
                    me.getView().setActiveItem('installableApps');
                }
            }]
        });

        popup.show({
            title: 'Registration complete.'.t(),
            width: 470,
            msg: 'Thank you for using Untangle!'.t() + '<br/><br/>' +
                'Applications can now be installed and configured.'.t() + '<br/>' +
                'Would you like to install the recommended applications now?'.t(),
            icon: Ext.MessageBox.QUESTION
        });
    },

    installRecommendedApp: function (app, cb) {
        var me = this, vm = me.getViewModel();

        var record = vm.getStore('apps').findRecord('name', app.name);

        record.set('extraCls', 'progress');

        Rpc.asyncData('rpc.appManager.instantiate', record.get('name'), vm.get('policyId'))
        .then(function (result) {
            record.set('extraCls', 'finish');
            record.set('targetState', result.getRunState());
            record.set('route', record.get('type') === 'FILTER' ? '#apps/' + vm.get('policyId') + '/' + record.get('name') : '#service/' + record.get('name'));
            me.updateCounters();
            cb();
        });
    },

    installRecommendedApps: function (apps) {
        var me = this, appsToInstall = apps.length;

        Ext.Array.each(apps, function (app) {
            me.installRecommendedApp(app, function () {
                appsToInstall--;
                if (appsToInstall === 0) { // all apps installed
                    Ext.MessageBox.alert('Installation Complete!'.t(),
                        'The recommended applications have successfully been installed.'.t()  + '<br/><br/>' +
                        'Thank you for using Untangle!'.t(),
                        function () {
                            window.location.href = '/admin/index.do';
                        });
                    return;
                }
            });
        });
    }

});
