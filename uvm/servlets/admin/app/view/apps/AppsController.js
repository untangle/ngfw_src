Ext.define('Ung.view.apps.AppsController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.apps',

    control: {
        '#': { activate: 'onActivate' },
        '#installedApps': { activate: 'filterInstalled' },
        '#installableApps': {
            activate: 'filterInstallable',
            select: 'onInstallApp'
        }
    },
    listen: {
        store: {
            '#policiestree': {
                rootchange: 'onRootChange'
            }
        }
    },

    onActivate: function () {
        // var vm = this.getViewModel();
        // vm.bind('{policyId}', function (id) {
        //     console.log(id);
        // });
        // this.getPolicies();
    },

    onRootChange: function () {
        var me = this, menuItems = [];

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
            me.lookup('policyBtn').setHidden(false);
        } else {
            me.lookup('policyBtn').setHidden(true);
        }
        me.getPolicies(); // set when route changed
    },

    appDesc: {
        'web-filter': 'Web Filter scans and categorizes web traffic to monitor and enforce network usage policies.'.t(),
        'web-monitor': 'Web monitor scans and categorizes web traffic to monitor and enforce network usage policies.'.t(),
        'virus-blocker': 'Virus Blocker detects and blocks malware before it reaches users desktops or mailboxes.'.t(),
        'virus-blocker-lite': 'Virus Blocker Lite detects and blocks malware before it reaches users desktops or mailboxes.'.t(),
        'spam-blocker': 'Spam Blocker detects, blocks, and quarantines spam before it reaches users mailboxes.'.t(),
        'spam-blocker-lite': 'Spam Blocker Lite detects, blocks, and quarantines spam before it reaches users mailboxes.'.t(),
        'phish-blocker': 'Phish Blocker detects and blocks phishing emails using signatures.'.t(),
        'web-cache': 'Web Cache stores and serves web content from local cache for increased speed and reduced bandwidth usage.'.t(),
        'bandwidth-control': 'Bandwidth Control monitors, manages, and shapes bandwidth usage on the network'.t(),
        'ssl-inspector': 'SSL Inspector allows for full decryption of HTTPS and SMTPS so that other applications can process the encrytped streams.'.t(),
        'application-control': 'Application Control scans sessions and identifies the associated applications allowing each to be flagged and/or blocked.'.t(),
        'application-control-lite': 'Application Control Lite identifies, logs, and blocks sessions based on the session content using custom signatures.'.t(),
        'captive-portal': 'Captive Portal allows administrators to require network users to complete a defined process, such as logging in or accepting a network usage policy, before accessing the internet.'.t(),
        'firewall': 'Firewall is a simple application that flags and blocks sessions based on rules.'.t(),
        'ad-blocker': 'Ad Blocker blocks advertising content and tracking cookies for scanned web traffic.'.t(),
        'reports': 'Reports records network events to provide administrators the visibility and data necessary to investigate network activity.'.t(),
        'policy-manager': 'Policy Manager enables administrators to create different policies and handle different sessions with different policies based on rules.'.t(),
        'directory-connector': 'Directory Connector allows integration with external directories and services, such as Active Directory, RADIUS, or Google.'.t(),
        'wan-failover': 'WAN Failover detects WAN outages and re-routes traffic to any other available WANs to maximize network uptime.'.t(),
        'wan-balancer': 'WAN Balancer spreads network traffic across multiple internet connections for better performance.'.t(),
        'ipsec-vpn': 'IPsec VPN provides secure network access and tunneling to remote users and sites using IPsec, GRE, L2TP, Xauth, and IKEv2 protocols.'.t(),
        'openvpn': 'OpenVPN provides secure network access and tunneling to remote users and sites using the OpenVPN protocol.'.t(),
        'intrusion-prevention': 'Intrusion Prevention blocks scans, detects, and blocks attacks and suspicious traffic using signatures.'.t(),
        'configuration-backup': 'Configuration Backup automatically creates backups of settings uploads them to My Account and Google Drive.'.t(),
        'branding-manager': 'The Branding Settings are used to set the logo and contact information that will be seen by users (e.g. reports).'.t(),
        'live-support': 'Live Support provides on-demand help for any technical issues.'.t()
    },

    refs: {
        installedApps: '#installedApps',
        installableApps: '#installableApps'
    },

    // listen: {
    //     global: {
    //         appstatechange: 'updateApps'
    //     },
    //     store: {
    //         '#policies': {
    //             datachanged: 'updateApps'
    //         }
    //     }
    // },

    getPolicies: function () {
        var me = this, vm = this.getViewModel(), instance;

        if (Ext.getStore('policiestree').getCount() > 0) {
            var policyNode = Ext.getStore('policiestree').findNode('policyId', vm.get('policyId'));
            this.lookup('policyBtn').setText(policyNode.get('name') + ' &nbsp;<i class="fa fa-angle-down fa-lg"></i>');
        }

        Rpc.asyncData('rpc.appManager.getAppsView', vm.get('policyId'))
            .then(function (policy) {

            var apps = [];
            vm.getStore('apps').removeAll();

            Ext.Array.each(policy.appProperties.list, function (app) {
                var _app = {
                    name: app.name,
                    displayName: app.displayName,
                    route: app.type === 'FILTER' ? '#apps/' + policy.policyId + '/' + app.name : '#service/' + app.name,
                    type: app.type,
                    viewPosition: app.viewPosition,
                    status: null,
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
                    // route: '#apps/' + policy.policyId + '/' + app.name,
                    type: app.type,
                    viewPosition: app.viewPosition,
                    desc: me.appDesc[app.name],
                    status: 'available'
                });
            });

            vm.getStore('apps').loadData(apps);
        });
    },

    /**
     * Based on which view is activated (Apps or Install Apps)
     * the apps store is filtered to reflect current applications
     */
    filterInstalled: function () {
        this.getViewModel().set('onInstalledApps', true);
        var appsStore = this.getViewModel().getStore('apps');

        appsStore.clearFilter();
        appsStore.filterBy(function (rec) {
            return !rec.get('status') || rec.get('status') === 'installing' || rec.get('status') === 'installed';
        });
    },

    filterInstallable: function () {
        this.getViewModel().set('onInstalledApps', false);
        var appsStore = this.getViewModel().getStore('apps');

        // initially, after install the nide item is kept on the Install Apps, having status 'installed'
        // when activating 'Install Apps', the 'installed' status is set as null so that app will not be shown
        appsStore.each(function (rec) {
            if (rec.get('status') === 'installed') {
                rec.set('status', null);
            }
        });
        appsStore.clearFilter();
        appsStore.filterBy(function (rec) {
            return Ext.Array.contains(['available', 'installing', 'installed'], rec.get('status'));
        });
    },

    // init: function (view) {
    //     view.getViewModel().bind({
    //         bindTo: '{policyId}'
    //     }, this.onPolicy, this);
    // },

    // onAppStateChange: function (state, instance) {
    //     console.log(instance);
    // },

    // updateApps: function () {
    //     var vm = this.getViewModel(),
    //         appInstance, i;

    //     rpc.appManager.getAppsViews(function(result, exception) {
    //         var policy = result.filter(function (p) {
    //             return parseInt(p.policyId) === parseInt(vm.get('policyId'));
    //         })[0];

    //         var apps = policy.appProperties.list,
    //             instances = policy.instances.list;

    //         for (i = 0; i < apps.length; i += 1) {
    //             appInstance = instances.filter(function (instance) {
    //                 return instance.appName === apps[i].name;
    //             })[0];
    //             // console.log(appInstance.targetState);
    //             apps[i].policyId = vm.get('policyId');
    //             apps[i].state = appInstance.targetState.toLowerCase();
    //         }
    //         vm.set('apps', apps);
    //     });
    // },

    // onPolicy: function () {
    //     // this.getView().lookupReference('filters').removeAll();
    //     // this.getView().lookupReference('services').removeAll();
    //     // this.updateApps();
    // },

    setPolicy: function (combo, newValue, oldValue) {
        if (oldValue !== null) {
            this.redirectTo('#apps/' + newValue, false);
            this.updateApps();
        }
    },

    // onItemAfterRender: function (item) {
    //     Ext.defer(function () {
    //         item.removeCls('insert');
    //     }, 50);
    // },

    /**
     * method which initialize the app installation
     */
    onInstallApp: function (view, record) {
        var me = this, vm = me.getViewModel();
        record.set('status', 'installing');
        Rpc.asyncData('rpc.appManager.instantiate', record.get('name'), vm.get('policyId'))
        .then(function (result) {
            // record.set('status', 'installed');

            if (record.get('name') === 'policy-manager') { // build the policies tree
                Ext.getStore('policiestree').build();
            }

            Ext.fireEvent('appinstall');
            me.getPolicies();
        });
    }

});
