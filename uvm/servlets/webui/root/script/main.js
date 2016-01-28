//Webui servlet main
Ext.Loader.setConfig({
    enabled: true,
    disableCaching: false,
    paths: {
        'Webui': 'script'
    }
});

var rpc = {}; // the main json rpc object
var testMode = false;

// Main object class
Ext.define("Ung.Main", {
    singleton: true,
    webuiMode: true,
    debugMode: false,
    buildStamp: null,
    disableThreads: false, // in development environment is useful to disable threads.
    apps: [],
    nodePreviews: {},
    config: null,
    totalMemoryMb: 2000,
    nodes: null,
    // the Ext.Viewport object for the application
    viewport: null,
    menuWidth: null,
    init: function(config) {
        Ext.MessageBox.wait(i18n._("Starting..."), i18n._("Please wait"));
        Ext.apply(this, config);
        if (Ext.isGecko) {
            document.onkeypress = function(e) {
                if (e.keyCode==27) {
                    return false;
                }
                return true;
            };
        }
        JSONRpcClient.toplevel_ex_handler = Ung.Util.rpcExHandler;
        JSONRpcClient.max_req_active = 25;

        // get JSONRpcClient
        rpc.jsonrpc = new JSONRpcClient("/webui/JSON-RPC");
        //load all managers and startup info
        var startupInfo;
        try {
            startupInfo = rpc.jsonrpc.UvmContext.getWebuiStartupInfo();
        } catch (e) {
            Ung.Util.rpcExHandler(e);
        }
        Ext.applyIf(rpc, startupInfo);
        //Had to get policyManager this way because startupInfo.policyManager contains sometimes an object instead of a callableReference
        try {
            rpc.policyManager=rpc.nodeManager.node("untangle-node-policy-manager");
        } catch (e) {
            Ung.Util.rpcExHandler(e);
        }
        if(rpc.isRegistered) {
            Ung.Main.getLicenseManager().reloadLicenses(Ext.bind(function(result,exception) {
              //just make sure the licenses are reloaded on page start
            }, this), true);
        }

        this.startApplication();
    },
    startApplication: function() {
        if(Ext.supports.LocalStorage) {
            Ext.state.Manager.setProvider(Ext.create('Ext.state.LocalStorageProvider'));
        }
        this.target = Ung.Util.getQueryStringParam("target");
        i18n = Ext.create('Ung.I18N',{
            map: rpc.translations,
            timeoffset: (new Date().getTimezoneOffset()*60000)+rpc.timeZoneOffset
        });
        Ung.Util.loadCss("/skins/"+rpc.skinSettings.skinName+"/css/common.css");
        Ung.Util.loadCss("/skins/"+rpc.skinSettings.skinName+"/css/admin.css");
        Ung.Util.loadCss("/skins/"+rpc.skinSettings.skinName+"/css/apps.css");

        document.title = rpc.companyName + (rpc.hostname ? " - " + rpc.hostname : "");
        if(rpc.languageSettings.language) {
            Ung.Util.loadScript('/ext6/classic/locale/locale-' + rpc.languageSettings.language + '.js');
        }
        Ung.VTypes.init(i18n);
        Ext.tip.QuickTipManager.init();
        Ext.on("resize", Ung.Util.resizeWindows);
        // initialize viewport object

        this.viewport = Ext.create('Ext.container.Viewport',{
            layout: 'border',
            responsiveFormulas: {
                small: 'width < 600'
            },
            items:[{
                region: 'west',
                xtype: 'container',
                name: 'mainMenu',
                cls: "main-menu",
                plugins: 'responsive',
                responsiveConfig: {
                    'small': {
                        width: 97
                    },
                    '!small': {
                        width: 150
                    }
                },
                layout: { type: 'vbox'},
                scrollable: 'y',
                items: [{
                    xtype: 'image',
                    itemId: 'logoImage',
                    alt: rpc.companyName,
                    margin: '3 5 3 5',
                    src: '/images/BrandingLogo.png?'+(new Date()).getTime(),
                    plugins: 'responsive',
                    responsiveConfig: {
                        'phone || small': {
                            height: 55
                        },
                        '!(phone || small)': {
                            height: 82
                        }
                    }
                }, {
                    xtype: 'segmentedbutton',
                    itemId: 'viewsMenu',
                    vertical: true,
                    defaults: {
                        scale: 'large',
                        textAlign: 'left',
                        cls: 'menu-button'
                    },
                    width: '100%',
                    items: [{
                        text: i18n._('Dashboard'),
                        iconCls: 'icon-dashboard',
                        handler: function() {
                            this.panelCenter.setActiveItem("dashboard");
                        },
                        scope: this
                    }, {
                        text: i18n._('Apps'),
                        pressed: true,
                        iconCls: 'icon-apps',
                        handler: function() {
                            this.panelCenter.setActiveItem((rpc.rackView && rpc.rackView.instances.list.length==0) ? 'installApps': 'apps');
                        },
                        scope: this
                    }, {
                        text: i18n._('Config'),
                        iconCls: 'icon-config',
                        handler: function() {
                            this.panelCenter.setActiveItem("config");
                        },
                        scope: this
                    }, {
                        text: i18n._('Reports'),
                        id: 'reportsMenuItem',
                        hidden: true,
                        iconCls: 'icon-reports',
                        handler: function() {
                            this.panelCenter.setActiveItem("reports");
                        },
                        scope: this
                    }]
                }]
            }, {
                region: 'center',
                itemId: 'panelCenter',
                xtype: 'container',
                cls: 'main-panel',
                layout: 'card',
                activeItem: 'apps',
                items: [{
                    xtype: 'container',
                    itemId: 'dashboard',
                    layout: {type: 'vbox', align: 'stretch'},
                    scrollable: true,
                    items: [{
                        xtype: 'container',
                        cls: 'top-container',
                        layout: {type: 'hbox', align: 'middle'},
                        height: 88,
                        items: [{
                            xtype: 'button',
                            margin: '0 0 0 20',
                            minWidth: 120,
                            scale: 'large',
                            cls: 'action-button',
                            text : i18n._( "Manage Widgets" ),
                            handler: function() {
                                Ung.Main.showDashboardManager();
                            }
                        }, {
                            xtype: 'component',
                            html: '',
                            flex: 1
                        },this.buildLinksMenu()]
                    }, {
                        xtype: 'container',
                        itemId: 'dashboardItems',
                        cls: 'dashboard'
                    }],
                    listeners: {
                        'activate': function(container) {
                            Ung.dashboard.Queue.resume();
                        },
                        "deactivate": function(container) {
                            Ung.dashboard.Queue.pause();
                        },
                        scope: this
                    }
                }, {
                    xtype: 'container',
                    itemId: 'apps',
                    cls: 'apps',
                    scrollable: true,
                    items: [{
                        xtype: 'container',
                        cls: 'top-container',
                        layout: {type: 'hbox', align: 'middle'},
                        height: 88,
                        items: [{
                            xtype: 'button',
                            margin: '0 0 0 20',
                            scale: 'large',
                            cls: 'policy-selector',
                            minWidth: 180,
                            maxWidth: 250,
                            text: '',
                            name: 'policySelector',
                            menu: Ext.create('Ext.menu.Menu', {
                                hideDelay: 0,
                                plain: true,
                                items: []
                            })
                        }, {
                            xtype: 'button',
                            margin: '0 0 0 10',
                            minWidth: 120,
                            scale: 'large',
                            cls: 'action-button',
                            text: i18n._('Install Apps'),
                            handler: function() {
                                Ung.Main.openInstallApps();
                            }
                        }, {
                            xtype: 'component',
                            itemId: 'parentPolicy',
                            margin: '0 0 0 10',
                            hidden: true,
                            html: ""
                        }, {
                            xtype: "component",
                            cls: "alert-container",
                            margin: '0 0 0 10',
                            itemId: "alertContainer",
                            hidden: true
                        }, {
                            xtype: "component",
                            cls: "no-ie-container",
                            margin: '0 0 0 10',
                            itemId: "noIeContainer",
                            hidden: true
                        }, {
                            xtype: 'component',
                            html: '',
                            flex: 1
                        },this.buildLinksMenu()]
                    }, {
                        xtype: 'container',
                        cls: 'apps-content',
                        items: [{
                            xtype: 'container',
                            cls: 'apps-top',
                            items: [this.systemStats = Ext.create('Ung.SystemStats', {})]
                        }, {
                            xtype: 'container',
                            itemId: 'filterNodes'

                        }, {
                            xtype: 'component',
                            cls: 'apps-separator',
                            itemId: 'servicesSeparator',
                            html: i18n._("Services")
                        }, {
                            xtype: 'container',
                            itemId: 'serviceNodes'
                        }]
                    }]
                }, {
                    xtype: 'container',
                    itemId: 'installApps',
                    layout: {type: 'vbox', align: 'stretch'},
                    scrollable: true,
                    items: [{
                        xtype: 'container',
                        cls: 'top-container',
                        layout: {type: 'hbox', align: 'middle'},
                        height: 88,
                        items: [{
                            xtype: 'component',
                            cls: 'top-title',
                            margin: '0 0 0 20',
                            html: i18n._('Install Apps')
                        }, {
                            xtype: 'button',
                            margin: '0 0 0 20',
                            minWidth: 120,
                            scale: 'large',
                            cls: 'action-button',
                            text : i18n._( "Done" ),
                            handler: function() {
                                this.panelCenter.setActiveItem("apps");
                            },
                            scope: this
                        }, {
                            xtype: 'component',
                            html: '',
                            flex: 1
                        }, this.buildLinksMenu()]
                    }, {
                        xtype: 'container',
                        itemId: "appsContainer"
                    }]
                }, {
                    xtype: 'container',
                    itemId: 'config',
                    layout: {type: 'vbox', align: 'stretch'},
                    scrollable: true,
                    items: [{
                        xtype: 'container',
                        cls: 'top-container',
                        layout: {type: 'hbox', align: 'middle'},
                        height: 88,
                        items: [{
                            xtype: 'component',
                            cls: 'top-title',
                            margin: '0 0 0 20',
                            html: i18n._('Config'),
                            flex: 1
                        }, this.buildLinksMenu()]
                    }, {
                        xtype: 'container',
                        items: [{
                            xtype: 'container',
                            itemId: 'configContainer'
                        }, {
                            xtype: 'component',
                            cls: 'config-separator top-title',
                            html: i18n._("Tools")
                        }, {
                            xtype: 'container',
                            title: i18n._('Tools'),
                            itemId: "toolsContainer"
                        }]
                    }]
                }, {
                    xtype: 'container',
                    itemId: 'reports',
                    layout: "border",
                    items: [{
                        xtype: 'container',
                        region: "north",
                        cls: 'top-container',
                        layout: {type: 'hbox', align: 'middle'},
                        height: 40,
                        items: [{
                            xtype: 'component',
                            cls: 'top-title',
                            margin: '0 0 0 20',
                            html: i18n._('Reports Viewer'),
                            flex: 1
                        }, this.buildLinksMenu()]
                    }, {
                        xtype: 'container',
                        region: "center",
                        layout: "fit",
                        itemId: 'reportsContainer',
                        items: []
                    }],
                    listeners: {
                        'activate': function(container) {
                            this.viewport.down("#reportsContainer").removeAll();
                            var reportsViewer = Ext.create("Ung.reportsViewer", {});
                            this.viewport.down("#reportsContainer").add(reportsViewer);
                        },
                        "deactivate": function(container) {
                            this.viewport.down("#reportsContainer").removeAll();
                        },
                        scope: this
                    }
                }]
            }
        ]});
        Ext.QuickTips.init();
        this.mainMenu = this.viewport.down("[name=mainMenu]");
        this.menuWidth = this.mainMenu.getWidth();
        this.panelCenter = this.viewport.down("#panelCenter");
        this.policySelector =  this.viewport.down("button[name=policySelector]");
        this.filterNodes = this.viewport.down("#filterNodes");
        this.serviceNodes = this.viewport.down("#serviceNodes");
        this.parentPolicy = this.viewport.down("#parentPolicy");
        this.servicesSeparator = this.viewport.down("#servicesSeparator");
        this.appsPanel = this.viewport.down("#apps");
        this.appsContainer = this.viewport.down("#appsContainer");
        Ung.dashboard.dashboardPanel = this.viewport.down("#dashboardItems");

        Ung.dashboard.loadDashboard();
        this.buildConfig();
        this.loadPolicies();
    },
    buildLinksMenu: function() {
        return {
            xtype: 'component',
            margin: '0 10 0 20',
            html: '<a class="menu-link" href="'+this.getHelpLink(null)+'" target="_blank">'+i18n._('Help')+'</a> '+
                '<a class="menu-link" onclick="return Ung.LicenseLoader.check();" href="'+this.getMyAccountLink()+'" target="_blank">'+i18n._('My Account')+'</a> ' +
                '<a class="menu-link logout" href="/auth/logout?url=/webui&realm=Administrator">'+i18n._('Logout')+'</a>'
        };
    },
    about: function (forceReload) {
        if(rpc.about === undefined) {
            var query = "";
            query = query + "uid=" + rpc.serverUID;
            query = query + "&" + "version=" + rpc.fullVersion;
            query = query + "&" + "webui=true";
            query = query + "&" + "lang=" + rpc.languageSettings.language;

            rpc.about = query;
        }
        return rpc.about;
    },
    openLegal: function() {
        var baseUrl;
        try {
            baseUrl = rpc.jsonrpc.UvmContext.getLegalUrl();
        } catch (e) {
            Ung.Util.rpcExHandler(e);
        }
        var url = baseUrl + "?" + this.about();

        console.log("Open Url   :", url);
        window.open(url); // open a new window
    },
    getHelpLink: function( topic ) {
        return rpc.helpUrl + "?" + "source=" + topic + "&" + this.about();
    },
    openHelp: function( topic ) {
        var url = Ung.Main.getHelpLink(topic);
        window.open(Ung.Main.getHelpLink(topic)); // open a new window
        console.log("Help link:", url);
    },
    openSupportScreen: function() {
        var url = rpc.storeUrl + "?" + "action=support" + "&" + this.about();
        window.open(url); // open a new window
        Ung.LicenseLoader.check();
    },
    openFailureScreen: function () {
        Ext.require(['Webui.config.offline'], function() {
            Webui.config.offlineWin = Ext.create('Webui.config.offline', {});
            Webui.config.offlineWin.show();
        }, this);
    },
    openRegistrationScreen: function () {
        Ext.require(['Webui.config.accountRegistration'], function() {
            Webui.config.accountRegistrationWin = Ext.create('Webui.config.accountRegistration', {});
            Webui.config.accountRegistrationWin.show();
        }, this);
    },
    getMyAccountLink: function() {
        return rpc.storeUrl + "?" + "action=my_account" + "&" + this.about();
    },
    openLibItemStore: function (libItemName) {
        var url = rpc.storeUrl + "?" + "action=buy" + "&" + "libitem=" + libItemName + "&" + this.about() ;
        window.open(url);
        console.log("Open Url   :", url);
        Ung.LicenseLoader.check();

    },
    openSetupWizardScreen: function() {
        var url = "/setup";
        window.open(url);
    },
    upgrade: function () {
        Ung.MetricManager.stop();

        console.log("Applying Upgrades...");

        Ext.MessageBox.wait({
            title: i18n._("Please wait"),
            msg: i18n._("Applying Upgrades...")
        });

        var doneFn = Ext.bind( function() {
        }, this);

        rpc.systemManager.upgrade(Ext.bind(function(result, exception) {
            // the upgrade will shut down the untangle-vm so often this returns an exception
            // either way show a wait dialog...

            Ext.MessageBox.hide();
            var applyingUpgradesWindow=Ext.create('Ext.window.MessageBox', {
                minProgressWidth: 360
            });

            // the untangle-vm is shutdown, just show a message dialog box for 45 seconds so the user won't poke at things.
            // then refresh browser.
            applyingUpgradesWindow.wait(i18n._("Applying Upgrades..."), i18n._("Please wait"), {
                interval: 500,
                increment: 120,
                duration: 45000,
                scope: this,
                fn: function() {
                    console.log("Upgrade in Progress. Press ok to go to the Start Page...");
                    if(Ung.Main.configWin!=null && Ung.Main.configWin.isVisible()) {
                        Ung.Main.configWin.closeWindow();
                    }
                    applyingUpgradesWindow.hide();
                    Ext.MessageBox.hide();
                    Ext.MessageBox.alert(
                        i18n._("Upgrade in Progress"),
                        i18n._("The upgrades have been downloaded and are now being applied.") + "<br/>" +
                            "<strong>" + i18n._("DO NOT REBOOT AT THIS TIME.") + "</strong>" + "<br/>" +
                            i18n._("Please be patient this process will take a few minutes.") + "<br/>" +
                            i18n._("After the upgrade is complete you will be able to log in again."),
                        Ung.Util.goToStartPage);
                }
            });
        }, this));
    },

    getNetworkManager: function(forceReload) {
        if (forceReload || rpc.networkManager === undefined) {
            try {
                rpc.networkManager = rpc.jsonrpc.UvmContext.networkManager();
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }
        }
        return rpc.networkManager;
    },

    getLoggingManager: function(forceReload) {
        if (forceReload || rpc.loggingManager === undefined) {
            try {
                rpc.loggingManager = rpc.jsonrpc.UvmContext.loggingManager();
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }
        }
        return rpc.loggingManager;
    },

    getCertificateManager: function(forceReload) {
        if (forceReload || rpc.certificateManager === undefined) {
            try {
                rpc.certificateManager = rpc.jsonrpc.UvmContext.certificateManager();
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }
        }
        return rpc.certificateManager;
    },

    getBrandingManager: function(forceReload) {
        if (forceReload || rpc.brandingManager === undefined) {
            try {
                rpc.brandingManager = rpc.jsonrpc.UvmContext.brandingManager();
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }
        }
        return rpc.brandingManager;
    },

    getOemManager: function(forceReload) {
        if (forceReload || rpc.oemManager === undefined) {
            try {
                rpc.oemManager = rpc.jsonrpc.UvmContext.oemManager();
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }
        }
        return rpc.oemManager;
    },

    getLicenseManager: function(forceReload) {
        // default functionality is to reload license manager as it might change in uvm
        if (typeof forceReload === 'undefined') {
            forceReload = true;
        }
        if (forceReload || rpc.licenseManager === undefined) {
            try {
              rpc.licenseManager = rpc.jsonrpc.UvmContext.licenseManager();
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }
        }
        return rpc.licenseManager;
    },
    getExecManager: function(forceReload) {
        if (forceReload || rpc.execManager === undefined) {
            try {
                rpc.execManager = rpc.jsonrpc.UvmContext.execManager();
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }

        }
        return rpc.execManager;
    },
    getLocalDirectory: function(forceReload) {
        if (forceReload || rpc.localDirectory === undefined) {
            try {
                rpc.localDirectory = rpc.jsonrpc.UvmContext.localDirectory();
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }
        }
        return rpc.localDirectory;
    },

    getMailSender: function(forceReload) {
        if (forceReload || rpc.mailSender === undefined) {
            try {
                rpc.mailSender = rpc.jsonrpc.UvmContext.mailSender();
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }
        }
        return rpc.mailSender;
    },
    getNetworkSettings: function(forceReload) {
        if (forceReload || rpc.networkSettings === undefined) {
            try {
                rpc.networkSettings = Ung.Main.getNetworkManager().getNetworkSettings();
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }
        }
        return rpc.networkSettings;
    },
    getReportsManager: function(forceReload) {
        if (forceReload || rpc.reportsManager === undefined) {
            try {
                rpc.reportsManager = this.getNodeReports().getReportsManager();
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }
        }
        return rpc.reportsManager;
    },
    // get node reports
    getNodeReports: function(forceReload) {
        if (forceReload || rpc.nodeReports === undefined) {
            try {
                rpc.nodeReports = rpc.nodeManager.node("untangle-node-reports");
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }
        }
        return rpc.nodeReports;
    },
    // is reports node installed
    isReportsAppInstalled: function(forceReload) {
        if (forceReload || rpc.reportsAppInstalledAndEnabled === undefined) {
            try {
                if (!Ung.Main.getNodeReports(true)) {
                    rpc.reportsAppInstalledAndEnabled = false;
                } else {
                    if (rpc.nodeReports.getRunState() == "RUNNING"){
                        rpc.reportsAppInstalledAndEnabled = true;
                    } else {
                        rpc.reportsAppInstalledAndEnabled = false;
                    }
                }
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }
        }
        return rpc.reportsAppInstalledAndEnabled;
    },
    // load policies list
    loadPolicies: function() {
        Ext.MessageBox.wait(i18n._("Loading Apps..."), i18n._("Please wait"));
        if (rpc.policyManager != null) {
            rpc.policyManager.getSettings(Ext.bind(function (result, exception) {
                if(Ung.Util.handleException(exception)) return;
                rpc.policies=result.policies.list;
                this.buildPolicies();
            }, this));
        } else {
            // no policy manager, just one policy (Default Policy)
            rpc.policies = [{
                javaClass: "com.untangle.node.policy.PolicySettings",
                policyId: "1",
                name: i18n._("Default Policy"),
                description: i18n._("The Default Policy")
            }];
            this.buildPolicies();
        }
    },
    getNodePackageDesc: function(nodeSettings) {
        var i;
        if(this.myApps!==null) {
            for(i=0;i<this.myApps.length;i++) {
                if(this.myApps[i].name==nodeSettings.nodeName) {
                    return this.myApps[i];
                }
            }
        }
        return null;
    },
    createNode: function (nodeProperties, nodeSettings, nodeMetrics, license, runState) {
        var node = {
            nodeId: nodeSettings.id,
            nodeSettings: nodeSettings,
            type: nodeProperties.type,
            hasPowerButton: nodeProperties.hasPowerButton,
            name: nodeProperties.name,
            displayName: nodeProperties.displayName,
            license: license,
            image: "/skins/"+rpc.skinSettings.skinName+"/images/admin/apps/"+nodeProperties.name+"_42x42.png",
            metrics: nodeMetrics,
            runState: runState,
            viewPosition: nodeProperties.viewPosition
        };
        return node;
    },
    buildApps: function () {
        //destroy Apps
        Ung.Main.appsContainer.removeAll();
        //build Apps
        for(var i=0;i<rpc.rackView.installable.list.length;i++) {
            Ung.Main.appsContainer.add(Ext.create("Ung.AppItem", {nodeProperties: rpc.rackView.installable.list[i]}));
        }
    },
    buildNodes: function() {
        //build nodes
        Ung.MetricManager.stop();
        Ext.getCmp('policyManagerMenuItem').disable();
        Ext.getCmp('policyManagerToolItem').hide();
        Ext.getCmp('reportsMenuItem').hide();

        var nodePreviews = Ext.clone(this.nodePreviews);
        this.filterNodes.removeAll();
        this.serviceNodes.removeAll();

        delete rpc.reportsAppInstalledAndEnabled;
        this.nodes=[];
        var i, node;
        var hasService = false;
        for(i=0;i<rpc.rackView.instances.list.length;i++) {
            var nodeSettings=rpc.rackView.instances.list[i];
            var nodeProperties=rpc.rackView.nodeProperties.list[i];

            node = this.createNode(nodeProperties,
                     nodeSettings,
                     rpc.rackView.nodeMetrics.map[nodeSettings.id],
                     rpc.rackView.licenseMap.map[nodeProperties.name],
                     rpc.rackView.runStates.map[nodeSettings.id]);
            this.nodes.push(node);
            if(!hasService && node.type != "FILTER") {
                hasService = true;
            }
        }

        if(!this.initialized) {
            this.initialized = true;
            if(!rpc.isRegistered) {
                this.showWelcomeScreen();
            }
            //start with installApps if no app is installed
            this.panelCenter.setActiveItem((rpc.rackView && rpc.rackView.instances.list.length==0) ? 'installApps': 'apps');
        }

        this.servicesSeparator.setVisible(hasService);
        if(hasService && !this.appsPanel.hasCls("apps-have-services")) {
            this.appsPanel.addCls("apps-have-services");
        }
        if(!hasService && this.appsPanel.hasCls("apps-have-services")) {
            this.appsPanel.removeCls("apps-have-services");
        }

        this.nodes.sort(function(a,b) {
            return a.viewPosition - b.viewPosition;
        });
        for(i=0; i<this.nodes.length; i++) {
            node=this.nodes[i];
            this.addNode(node, nodePreviews[node.name]);
            if(nodePreviews[node.name]) {
                delete nodePreviews[node.name];
            }
        }
        for(var nodeName in nodePreviews) {
            this.addNodePreview(nodePreviews[nodeName]);
        }
        if(!this.disableThreads) {
            Ung.MetricManager.start(true);
        }
        this.openTarget();
        if(Ext.MessageBox.isVisible() && Ext.MessageBox.title==i18n._("Please wait")) {
            Ext.Function.defer(Ext.MessageBox.hide,30,Ext.MessageBox);
        }
    },
    //TODO: implement routing mechanism available in new extjs, and remove this mechanism
    openTarget: function() {
        if(this.target) {
            //Open target if specified
            //target usage in the query string:
            //config.<configItemName>(.<tabName>(.subtabNane or .buttonName))
            //node.<nodeName>(.<tabName>(.subtabNane or .buttonName))
            //monitor.[sessions|hosts](.<tabName>)
            var targetTokens = this.target.split(".");
            if(targetTokens.length >= 2) {
                var firstToken = targetTokens[0].toLowerCase();
                if(firstToken == "config" ) {
                    var configItem =this.configMap[targetTokens[1]];
                    if(configItem) {
                        Ung.Main.openConfig(configItem);
                    }
                } else if(firstToken == "node") {
                    var nodeName = targetTokens[1].toLowerCase();
                    for( i=0 ; i<Ung.Main.nodes.length ; i++) {
                        if(Ung.Main.nodes[i].name == nodeName) {
                            var nodeCmp = Ung.Node.getCmp(Ung.Main.nodes[i].nodeId);
                            if (nodeCmp != null) {
                                nodeCmp.loadSettings();
                            }
                            break;
                        }
                    }
                } else if(firstToken == "monitor") {
                    var secondToken = targetTokens[1].toLowerCase();
                    if(secondToken == 'sessions') {
                        Ung.Main.showSessions();
                    } else if(secondToken == 'hosts') {
                        Ung.Main.showHosts();
                    }
                }
            } else {
                this.target = null;
            }
            // remove target in max 10 seconds to prevent using it again
            Ext.Function.defer(function() {
                Ung.Main.target = null;
            }, 10000, this);
        }
    },
    // load the rack view for current policy
    loadRackView: function() {
        var callback = Ext.bind(function (result, exception) {
            if(Ung.Util.handleException(exception)) return;
            rpc.rackView=result;
            var parentRackName = this.getParentName( rpc.currentPolicy.parentId );
            this.parentPolicy.update((parentRackName == null)? "": i18n._("Parent Rack")+"<br/>"+parentRackName);
            this.parentPolicy.setVisible(parentRackName != null);
            this.nodePreviews = {};
            Ung.Main.buildApps();
            Ung.Main.buildNodes();
        }, this);
        Ung.Util.RetryHandler.retry( rpc.rackManager.getRackView, rpc.rackManager, [ rpc.currentPolicy.policyId ], callback, 1500, 10 );
    },
    updateRackView: function() {
        var callback = Ext.bind(function(result,exception) {
            if(Ung.Util.handleException(exception)) return;
            rpc.rackView=result;
            Ung.Main.buildApps();
            Ung.Main.buildNodes();
        }, this);
        Ung.Util.RetryHandler.retry( rpc.rackManager.getRackView, rpc.rackManager, [ rpc.currentPolicy.policyId ], callback, 1500, 10 );
    },
    reloadLicenses: function() {
        Ung.Main.getLicenseManager().reloadLicenses(Ext.bind(function(result,exception) {
            // do not pop-up license managerexceptions because they happen when offline
            // if(Ung.Util.handleException(exception)) return;
            if (exception) return;

            var callback = Ext.bind(function(result,exception) {
                if(Ung.Util.handleException(exception)) return;
                rpc.rackView=result;
                for (var i = 0; i < Ung.Main.nodes.length; i++) {
                    var nodeCmp = Ung.Node.getCmp(Ung.Main.nodes[i].nodeId);
                    if (nodeCmp && nodeCmp.license) {
                        nodeCmp.updateLicense(rpc.rackView.licenseMap.map[nodeCmp.name]);
                    }
                }
            }, this);

            Ung.Util.RetryHandler.retry( rpc.rackManager.getRackView, rpc.rackManager, [ rpc.currentPolicy.policyId ], callback, 1500, 10 );
        }, this), true);
    },

    installNode: function(nodeProperties, appItem, completeFn) {
        if(!rpc.isRegistered) {
            Ung.Main.openRegistrationScreen();
            return;
        }
        if( nodeProperties === null ) {
            return;
        }
        // Sanity check to see if the node is already installed.
        var node = Ung.Main.getNode( nodeProperties.name );
        if (( node !== null ) && ( node.nodeSettings.policyId == rpc.currentPolicy.policyId )) {
            appItem.hide();
            return;
        }
        Ung.AppItem.setLoading(nodeProperties.name, true);
        Ung.Main.addNodePreview( nodeProperties );

        rpc.nodeManager.instantiate(Ext.bind(function (result, exception) {
            if (exception) {
                Ung.AppItem.setLoading(nodeProperties.name, false);
                Ung.Main.updateRackView();
                Ung.Util.handleException(exception);
                return;
            }
            Ung.Main.updateRackView();
            if (completeFn)
                completeFn();
        }, this), nodeProperties.name, rpc.currentPolicy.policyId);
    },
    openInstallApps: function() {
        Ung.Main.panelCenter.setActiveItem("installApps");
    },
    // build Config
    buildConfig: function() {
        this.config =[{
            name: 'network',
            displayName: i18n._('Network'),
            iconClass: 'icon-config-network',
            helpSource: 'network',
            className: 'Webui.config.network'
        }, {
            name: 'administration',
            displayName: i18n._('Administration'),
            iconClass: 'icon-config-admin',
            helpSource: 'administration',
            className: 'Webui.config.administration'
        }, {
            name: 'email',
            displayName: i18n._('Email'),
            iconClass: 'icon-config-email',
            helpSource: 'email',
            className: 'Webui.config.email'
        }, {
            name: 'localDirectory',
            displayName: i18n._('Local Directory'),
            iconClass: 'icon-config-directory',
            helpSource: 'local_directory',
            className: 'Webui.config.localDirectory'
        }, {
            name: 'upgrade',
            displayName: i18n._('Upgrade'),
            iconClass: 'icon-config-upgrade',
            helpSource: 'upgrade',
            className: 'Webui.config.upgrade'
        }, {
            name: 'system',
            displayName: i18n._('System'),
            iconClass: 'icon-config-system',
            helpSource: 'system',
            className: 'Webui.config.system'
        }, {
            name: 'about',
            displayName: i18n._('About'),
            iconClass: 'icon-config-about',
            helpSource: 'about',
            className: 'Webui.config.about'
        }];
        this.configMap = Ung.Util.createRecordsMap(this.config, "name");
        var i;
        var configContainer = this.viewport.down("#configContainer");
        for(i=0; i<this.config.length; i++) {
            configContainer.add(Ext.create('Ung.ConfigItem', {
                item: this.config[i]
            }));
        }
        var tools = [{
            id: 'policyManagerToolItem',
            item: {
                displayName: i18n._('Policy Manager'),
                iconClass: 'icon-policy-manager'

            },
            handler: Ung.Main.showPolicyManager
        }, {
            item: {
                displayName: i18n._('Session Viewer'),
                iconClass: 'icon-tools'
            },
            handler: Ung.Main.showSessions
        }, {
            item: {
                displayName: i18n._('Host Viewer'),
                iconClass: 'icon-tools'
            },
            handler: Ung.Main.showHosts
        }, {
            item: {
                displayName: i18n._('Device List'),
                iconClass: 'icon-tools'
            },
            handler: Ung.Main.showDevices
        }];
        var toolsContainer = this.viewport.down("#toolsContainer");
        for(i=0; i<tools.length; i++) {
            toolsContainer.add(Ext.create('Ung.ConfigItem', tools[i]));
        }

    },
    checkForIE: function (handler) {
        if (Ext.isIE) {
            var noIeContainer = this.viewport.down("#noIeContainer");
            noIeContainer.show();

            this.noIEToolTip= Ext.create('Ext.tip.ToolTip', {
                target: noIeContainer.getEl(),
                dismissDelay: 0,
                hideDelay: 1500,
                width: 500,
                cls: 'extended-stats',
                html: i18n._("For an optimal experience use Google Chrome or Mozilla Firefox.")
            });
        }
        if (Ext.isIE6 || Ext.isIE7 || Ext.isIE8 ) {
            Ext.MessageBox.alert( i18n._("Warning"),
                                  i18n._("Internet Explorer 8 and prior are not supported for administration.") + "<br/>" +
                                  i18n._("Please upgrade to a newer browser.") );
        }
    },
    checkForAlerts: function (handler) {
        //check for upgrades
        rpc.alertManager.getAlerts(Ext.bind(function( result, exception, opt, handler ) {
            var alertContainer = this.viewport.down("#alertContainer");
            var alertArr=[];
            if (result != null && result.list.length > 0) {
                alertContainer.show();
                alertArr.push('<div class="title">'+i18n._("Alerts:")+'</div>');
                for (var i = 0; i < result.list.length; i++) {
                    alertArr.push('<div class="values">&middot;&nbsp;'+i18n._(result.list[i])+'</div>');
                }
            } else {
                alertContainer.hide();
            }

            this.alertToolTip= Ext.create('Ext.tip.ToolTip', {
                target: alertContainer.getEl(),
                dismissDelay: 0,
                hideDelay: 1500,
                width: 500,
                cls: 'extended-stats',
                items: [{
                    xtype: 'container',
                    html: alertArr.join('')
                },{
                    xtype: 'container',
                    html: '<br/>' + '<b>' + i18n._('Press Help for more information') + "</b>"
                },{
                    xtype: 'button',
                    name: 'Help',
                    iconCls: 'icon-help',
                    text: i18n._('Help with Administration Alerts'),
                    handler: function() {
                        //helpSource: 'admin_alerts'
                        Ung.Main.openHelp('admin_alerts');
                    }
                }]
            });
        }, this,[handler],true));
    },
    openConfig: function(configItem) {
        Ext.MessageBox.wait(i18n._("Loading Config..."), i18n._("Please wait"));
        var createWinFn= function(config) {
            Ung.Main.configWin = Ext.create(config.className, config);
            Ung.Main.configWin.show();
            Ext.MessageBox.hide();
        };
        Ext.Function.defer(function() {
            Ext.require([this.className], function() {
                var configClass = Ext.ClassManager.get(this.className);
                if( configClass != null && Ext.isFunction( configClass.preload ) ) {
                    configClass.preload(this, createWinFn);
                } else {
                    createWinFn(this);
                }
            }, this);
        }, 10, configItem);
    },
    getNodePosition: function(place, viewPosition) {
        var position=0;
        if(place.items) {
            place.items.each(function(item, index) {
                if(item.viewPosition<viewPosition) {
                    position = index+1;
                } else {
                    return false;
                }
            });
        }
        return position;
    },
    addNode: function (node, fadeIn) {
        var nodeCmp = Ext.create('Ung.Node', node);
        nodeCmp.fadeIn=fadeIn;
        var place=(node.type=="FILTER")? this.filterNodes : this.serviceNodes;
        place.add(nodeCmp);
        Ung.AppItem.setLoading(node.name, false);
        if ( node.name == 'untangle-node-policy-manager') {
            // refresh rpc.policyManager to properly handle the case when the policy manager is removed and then re-added to the application list
            rpc.nodeManager.node(Ext.bind(function(result, exception) {
                if(Ung.Util.handleException(exception)) return;
                Ext.getCmp('policyManagerMenuItem').enable();
                Ext.getCmp('policyManagerToolItem').show();
                rpc.policyManager = result;
            }, this),"untangle-node-policy-manager");
        }
        if ( node.name == 'untangle-node-reports') {
            rpc.nodeManager.node(Ext.bind(function(result, exception) {
                if(Ung.Util.handleException(exception)) return;
                Ext.getCmp('reportsMenuItem').show();
                rpc.nodeReports = result;
                delete rpc.reportsManager;
            }, this),"untangle-node-reports");

        }
    },
    addNodePreview: function ( nodeProperties ) {
        var nodeCmp = Ext.create('Ung.NodePreview', nodeProperties );
        var place = ( nodeProperties.type=="FILTER") ? this.filterNodes : this.serviceNodes;
        var position = this.getNodePosition( place, nodeProperties.viewPosition );
        place.insert(position, nodeCmp);
    },
    getNode: function(nodeName) {
        if(Ung.Main.nodes) {
            var nodePolicyId;
            for (var i = 0; i < Ung.Main.nodes.length; i++) {
                nodePolicyId = Ung.Main.nodes[i].nodeSettings.policyId;
                if (nodeName == Ung.Main.nodes[i].name && (nodePolicyId == null || nodePolicyId == rpc.currentPolicy.policyId)) {
                    return Ung.Main.nodes[i];
                }
            }
        }
        return null;
    },
    updatePolicySelector: function() {
        var items=[];
        var selVirtualRackIndex = 0;
        rpc.policyNamesMap = {};
        rpc.policyNamesMap[0] = i18n._("No Rack");
        for( var i=0 ; i<rpc.policies.length ; i++ ) {
            var policy = rpc.policies[i];
            rpc.policyNamesMap[policy.policyId] = policy.name;
            items.push({
                text: policy.name,
                value: policy.policyId,
                index: i,
                handler: Ung.Main.changeRack,
                hideDelay: 0
            });
            if( policy.policyId == 1 ) {
                rpc.currentPolicy = policy;
                selVirtualRackIndex = i;
            }
        }
        items.push('-');
        items.push({text: i18n._('Show Policy Manager'), value: 'SHOW_POLICY_MANAGER', handler: Ung.Main.showPolicyManager, id:'policyManagerMenuItem', disabled: true, hideDelay: 0});
        items.push('-');
        items.push({text: i18n._('Show Sessions'), value: 'SHOW_SESSIONS', handler: Ung.Main.showSessions, hideDelay: 0});
        items.push({text: i18n._('Show Hosts'), value: 'SHOW_HOSTS', handler: Ung.Main.showHosts, hideDelay: 0});
        items.push({text: i18n._('Show Devices'), value: 'SHOW_DEVICES', handler: Ung.Main.showDevices, hideDelay: 0});

        this.policySelector.setText(items[selVirtualRackIndex].text);
        var menu = this.policySelector.down("menu");
        menu.removeAll();
        menu.add(items);

    },
    // build policies select box
    buildPolicies: function () {
        this.updatePolicySelector();
        this.checkForAlerts();
        this.checkForIE();

        Ung.Main.loadRackView();
    },
    getPolicyName: function(policyId) {
        if (Ext.isEmpty(policyId)){
            return i18n._( "Services" );
        }
        if (rpc.policyNamesMap[policyId] !== undefined) {
            return rpc.policyNamesMap[policyId];
        } else {
            return i18n._( "Unknown Rack" );
        }
    },
    showHosts: function() {
        Ext.require(['Webui.config.hostMonitor'], function() {
            if ( Ung.Main.hostMonitorWin == null) {
                Ung.Main.hostMonitorWin=Ext.create('Webui.config.hostMonitor', {});
            }
            Ung.Main.hostMonitorWin.show();
            Ext.MessageBox.wait(i18n._("Loading..."), i18n._("Please wait"));
            Ext.Function.defer(function() {
                Ung.Main.hostMonitorWin.gridCurrentHosts.reload();
                Ext.MessageBox.hide();
            }, 10, this);
        }, this);
    },
    showDevices: function() {
        Ext.require(['Webui.config.deviceMonitor'], function() {
            Ung.Main.deviceMonitorWin=Ext.create('Webui.config.deviceMonitor', {});
            Ung.Main.deviceMonitorWin.show();
        }, this);
    },
    showSessions: function() {
        Ung.Main.showNodeSessions(0);
    },
    showNodeSessions: function(nodeIdArg) {
        Ext.require(['Webui.config.sessionMonitor'], function() {
            if ( Ung.Main.sessionMonitorWin == null) {
                Ung.Main.sessionMonitorWin = Ext.create('Webui.config.sessionMonitor', {});
            }
            Ung.Main.sessionMonitorWin.show();
            Ext.MessageBox.wait(i18n._("Loading..."), i18n._("Please wait"));
            Ext.Function.defer(function() {
                Ung.Main.sessionMonitorWin.gridCurrentSessions.setSelectedApp(nodeIdArg);
                Ext.MessageBox.hide();
            }, 10, this);
        }, this);
    },
    showPolicyManager: function() {
        var node = Ung.Main.getNode("untangle-node-policy-manager");
        if (node != null) {
            var nodeCmp = Ung.Node.getCmp(node.nodeId);
            if (nodeCmp != null) {
                nodeCmp.loadSettings();
            }
        }
    },
    showDashboardManager: function() {
        Ext.require(['Webui.config.dashboardManager'], function() {
            Ung.Main.dashboardManagerWin=Ext.create('Webui.config.dashboardManager', {});
            Ung.Main.dashboardManagerWin.show();
        }, this);
    },
    // change current policy
    changeRack: function () {
        Ung.Main.policySelector.setText(this.text);
        rpc.currentPolicy = rpc.policies[this.index];
        Ung.Main.loadRackView();
    },
    getParentName: function( parentId ) {
        if( parentId == null || rpc.policies === null) {
            return null;
        }
        for ( var c = 0 ; c < rpc.policies.length ; c++ ) {
            if ( rpc.policies[c].policyId == parentId ) {
                return rpc.policies[c].name;
            }
        }
        return null;
    },
    // Prepares the uvm to display the welcome screen
    showWelcomeScreen: function () {
        //Test if box is online (store is available)
        Ext.MessageBox.wait(i18n._("Determining Connectivity..."), i18n._("Please wait"));

        rpc.jsonrpc.UvmContext.isStoreAvailable(Ext.bind(function(result, exception) {
            if(Ung.Util.handleException(exception)) return;
            Ext.MessageBox.hide();
            // If box is not online - show error message.
            // Otherwise show registration screen
            if(!result) {
                Ung.Main.openFailureScreen();
            } else {
                Ung.Main.openRegistrationScreen();
            }
        }, this));
    },
/*
    testInstallRandom: function(probability) {
        if(!probability) {
            probability = Math.random()*100;
        }
        var currentApps = Ung.Main.appsContainer.items.getRange();
        for(var i=0;i<currentApps.length;i++) {
            var app = Ung.AppItem.getApp(currentApps[i].nodeProperties.name);
            if ( app ) {
                if(Math.random()*100 < probability)
                app.installNode();
            }
        }
    },
    testUninstallAll: function() {
        for(var i=0;i<Ung.Main.nodes.length;i++) {
            var node = Ung.Node.getCmp(Ung.Main.nodes[i].nodeId);
            node.removeAction();
        }
    },
*/
    showPostRegistrationPopup: function() {
        if (this.nodes.length != 0) {
            // do not show anything if apps already installed
            return;
        }

        var popup = Ext.create('Ext.window.MessageBox', {
            buttons: [{
                name: 'Yes',
                text: i18n._("Yes, install the recommended apps."),
                handler: Ext.bind(function() {
                    var apps = [
                        { displayName: "Web Filter", name: 'untangle-node-web-filter'},
                        //{ displayName: "Web Filter Lite", name: 'untangle-node-web-filter-lite'},
                        { displayName: "Virus Blocker", name: 'untangle-node-virus-blocker'},
                        //{ displayName: "Virus Blocker Lite", name: 'untangle-node-virus-blocker-lite'},
                        { displayName: "Spam Blocker", name: 'untangle-node-spam-blocker'},
                        //{ displayName: "Spam Blocker Lite", name: 'untangle-node-spam-blocker-lite'},
                        //{ displayName: "Phish Blocker", name: 'untangle-node-phish-blocker'},
                        //{ displayName: "Web Cache", name: 'untangle-node-web-cache'},
                        { displayName: "Bandwidth Control", name: 'untangle-node-bandwidth-control'},
                        { displayName: "SSL Inspector", name: 'untangle-casing-ssl'},
                        { displayName: "Application Control", name: 'untangle-node-application-control'},
                        //{ displayName: "Application Control Lite", name: 'untangle-node-application-control-lite'},
                        { displayName: "Captive Portal", name: 'untangle-node-captive-portal'},
                        { displayName: "Firewall", name: 'untangle-node-firewall'},
                        //{ displayName: "Intrusion Prevention", name: 'untangle-node-intrusion-prevention'},
                        //{ displayName: "Ad Blocker", name: 'untangle-node-ad-blocker'},
                        { displayName: "Reports", name: 'untangle-node-reports'},
                        { displayName: "Policy Manager", name: 'untangle-node-policy-manager'},
                        { displayName: "Directory Connector", name: 'untangle-node-directory-connector'},
                        { displayName: "WAN Failover", name: 'untangle-node-wan-failover'},
                        { displayName: "WAN Balancer", name: 'untangle-node-wan-balancer'},
                        { displayName: "IPsec VPN", name: 'untangle-node-ipsec-vpn'},
                        { displayName: "OpenVPN", name: 'untangle-node-openvpn'},
                        { displayName: "Configuration Backup", name: 'untangle-node-configuration-backup'},
                        { displayName: "Branding Manager", name: 'untangle-node-branding-manager'},
                        { displayName: "Live Support", name: 'untangle-node-live-support'}];

                    // only install this on 1gig+ machines
                    if ( Ung.Main.totalMemoryMb > 900 ) {
                        apps.splice(4,0,{ displayName: "Phish Blocker", name: 'untangle-node-phish-blocker'});
                        apps.splice(2,0,{ displayName: "Virus Blocker Lite", name: 'untangle-node-virus-blocker-lite'});
                    }

                    var fn = function( appsToInstall ) {
                        // if there are no more apps left to install we are done
                        if ( appsToInstall.length == 0 ) {
                            Ext.MessageBox.alert(i18n._("Installation Complete!"), i18n._("Thank you for using Untangle!"), function(){
                                Ung.Main.panelCenter.setActiveItem("installApps");
                            });
                            return;
                        }
                        var name = appsToInstall[0].name;
                        appsToInstall.shift();
                        var completeFn = Ext.bind( fn, this, [appsToInstall] ); // function to install remaining apps
                        var app = Ung.AppItem.getApp(name);
                        if ( app ) {
                            app.installNode( completeFn );
                        } else {
                            completeFn();
                        }
                    };
                    fn( apps );
                    popup.close();
                }, this)
            },{
                name: 'No',
                text: i18n._("No, I will install the apps manually."),
                handler: Ext.bind(function() {
                    popup.close();
                }, this)
            }]
        });
        popup.show({
            title: i18n._("Registration complete."),
            width: 470,
            msg: i18n._("Thank you for using Untangle!") + "<br/>" + "<br/>" +
                i18n._("Applications can now be installed and configured.") + "<br/>" +
                i18n._("Would you like to install the recommended applications now?"),
            icon: Ext.MessageBox.QUESTION
        });
    }
});
