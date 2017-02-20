Ext.define('Ung.view.apps.AppsController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.apps',

    control: {
        '#': { activate: 'getPolicies' },
        '#installedApps': { activate: 'filterInstalled' },
        '#installableApps': {
            activate: 'filterInstallable',
            select: 'onInstallNode'
        }
    },

    nodeDesc: {
        'untangle-node-web-filter': 'Web Filter scans and categorizes web traffic to monitor and enforce network usage policies.'.t(),
        'untangle-node-web-monitor': 'Web monitor scans and categorizes web traffic to monitor and enforce network usage policies.'.t(),
        'untangle-node-virus-blocker': 'Virus Blocker detects and blocks malware before it reaches users desktops or mailboxes.'.t(),
        'untangle-node-virus-blocker-lite': 'Virus Blocker Lite detects and blocks malware before it reaches users desktops or mailboxes.'.t(),
        'untangle-node-spam-blocker': 'Spam Blocker detects, blocks, and quarantines spam before it reaches users mailboxes.'.t(),
        'untangle-node-spam-blocker-lite': 'Spam Blocker Lite detects, blocks, and quarantines spam before it reaches users mailboxes.'.t(),
        'untangle-node-phish-blocker': 'Phish Blocker detects and blocks phishing emails using signatures.'.t(),
        'untangle-node-web-cache': 'Web Cache stores and serves web content from local cache for increased speed and reduced bandwidth usage.'.t(),
        'untangle-node-bandwidth-control': 'Bandwidth Control monitors, manages, and shapes bandwidth usage on the network'.t(),
        'untangle-casing-ssl-inspector': 'SSL Inspector allows for full decryption of HTTPS and SMTPS so that other applications can process the encrytped streams.'.t(),
        'untangle-node-application-control': 'Application Control scans sessions and identifies the associated applications allowing each to be flagged and/or blocked.'.t(),
        'untangle-node-application-control-lite': 'Application Control Lite identifies, logs, and blocks sessions based on the session content using custom signatures.'.t(),
        'untangle-node-captive-portal': 'Captive Portal allows administrators to require network users to complete a defined process, such as logging in or accepting a network usage policy, before accessing the internet.'.t(),
        'untangle-node-firewall': 'Firewall is a simple application that flags and blocks sessions based on rules.'.t(),
        'untangle-node-ad-blocker': 'Ad Blocker blocks advertising content and tracking cookies for scanned web traffic.'.t(),
        'untangle-node-reports': 'Reports records network events to provide administrators the visibility and data necessary to investigate network activity.'.t(),
        'untangle-node-policy-manager': 'Policy Manager enables administrators to create different policies and handle different sessions with different policies based on rules.'.t(),
        'untangle-node-directory-connector': 'Directory Connector allows integration with external directories and services, such as Active Directory, RADIUS, or Google.'.t(),
        'untangle-node-wan-failover': 'WAN Failover detects WAN outages and re-routes traffic to any other available WANs to maximize network uptime.'.t(),
        'untangle-node-wan-balancer': 'WAN Balancer spreads network traffic across multiple internet connections for better performance.'.t(),
        'untangle-node-ipsec-vpn': 'IPsec VPN provides secure network access and tunneling to remote users and sites using IPsec, GRE, L2TP, Xauth, and IKEv2 protocols.'.t(),
        'untangle-node-openvpn': 'OpenVPN provides secure network access and tunneling to remote users and sites using the OpenVPN protocol.'.t(),
        'untangle-node-intrusion-prevention': 'Intrusion Prevention blocks scans, detects, and blocks attacks and suspicious traffic using signatures.'.t(),
        'untangle-node-configuration-backup': 'Configuration Backup automatically creates backups of settings uploads them to My Account and Google Drive.'.t(),
        'untangle-node-branding-manager': 'The Branding Settings are used to set the logo and contact information that will be seen by users (e.g. reports).'.t(),
        'untangle-node-live-support': 'Live Support provides on-demand help for any technical issues.'.t()
    },

    refs: {
        installedApps: '#installedApps',
        installableApps: '#installableApps'
    },

    // listen: {
    //     global: {
    //         nodestatechange: 'updateNodes'
    //     },
    //     store: {
    //         '#policies': {
    //             datachanged: 'updateNodes'
    //         }
    //     }
    // },

    getPolicies: function () {
        console.log('on activate');
        var me = this;
        var vm = this.getViewModel();

        Rpc.asyncData('rpc.nodeManager.getAppsViews').then(function(result) {
            console.log(result);
            var nodes = [];
            vm.getStore('apps').removeAll();

            Ext.Array.each(result[0].nodeProperties.list, function (node) {
                nodes.push({
                    name: node.name,
                    displayName: node.displayName,
                    type: node.type,
                    viewPosition: node.viewPosition,
                    status: null,
                    targetState: result[0].instances.list.filter(function (instance) {
                        return node.name === instance.nodeName;
                    })[0].targetState
                });

                // var tState = result[0].instances.list.filter(function (instance) {
                //     return node.name === instance.nodeName;
                // });
                // console.log(tState[0]);
            });

            Ext.Array.each(result[0].installable.list, function (node) {
                nodes.push({
                    name: node.name,
                    displayName: node.displayName,
                    type: node.type,
                    viewPosition: node.viewPosition,
                    desc: me.nodeDesc[node.name],
                    status: 'available'
                });
            });
            // Ext.toast('Data loaded');
            vm.getStore('apps').loadData(nodes);
        });
    },

    /**
     * Based on which view is activated (Apps or Install Apps)
     * the nodes store is filtered to reflect current applications
     */
    filterInstalled: function () {
        this.getViewModel().set('onInstalledApps', true);
        console.log('filter installed');
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

    // onNodeStateChange: function (state, instance) {
    //     console.log(instance);
    // },

    // updateNodes: function () {
    //     var vm = this.getViewModel(),
    //         nodeInstance, i;

    //     rpc.nodeManager.getAppsViews(function(result, exception) {
    //         var policy = result.filter(function (p) {
    //             return parseInt(p.policyId) === parseInt(vm.get('policyId'));
    //         })[0];

    //         var nodes = policy.nodeProperties.list,
    //             instances = policy.instances.list;

    //         for (i = 0; i < nodes.length; i += 1) {
    //             nodeInstance = instances.filter(function (instance) {
    //                 return instance.nodeName === nodes[i].name;
    //             })[0];
    //             // console.log(nodeInstance.targetState);
    //             nodes[i].policyId = vm.get('policyId');
    //             nodes[i].state = nodeInstance.targetState.toLowerCase();
    //         }
    //         vm.set('nodes', nodes);
    //     });
    // },

    // onPolicy: function () {
    //     // this.getView().lookupReference('filters').removeAll();
    //     // this.getView().lookupReference('services').removeAll();
    //     // this.updateNodes();
    // },

    setPolicy: function (combo, newValue, oldValue) {
        if (oldValue !== null) {
            this.redirectTo('#apps/' + newValue, false);
            this.updateNodes();
        }
    },

    // onItemAfterRender: function (item) {
    //     Ext.defer(function () {
    //         item.removeCls('insert');
    //     }, 50);
    // },

    /**
     * method which initialize the node installation
     */
    onInstallNode: function (view, record) {
        record.set('status', 'installing');
        Rpc.asyncData('rpc.nodeManager.instantiate', record.get('name'), 1)
        .then(function (result) {
            record.set('status', 'installed');
        });
    }

});
