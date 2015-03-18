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
    debugMode: false,
    buildStamp: null,
    disableThreads: false, // in development environment is useful to disable threads.
    apps: null,
    appsLastState: null,
    nodePreviews: null,
    config: null,
    totalMemoryMb: 2000,
    nodes: null,
    // the Ext.Viewport object for the application
    viewport: null,
    contentLeftWidth: null,
    iframeWin: null,
    policyNodeWidget:null,
    initialScreenAlreadyShown: false,

    init: function(config) {
        Ext.apply(this, config);
        if (Ext.isGecko) {
            document.onkeypress = function(e) {
                if (e.keyCode==27) {
                    return false;
                }
                return true;
            };
        }
        if(Ext.supports.LocalStorage) {
            Ext.state.Manager.setProvider(Ext.create('Ext.state.LocalStorageProvider'));
        }
        this.target = Ung.Util.getQueryStringParam("target");
        this.appsLastState = {};
        this.nodePreviews = {};
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
            rpc.policyManager=rpc.nodeManager.node("untangle-node-policy");
        } catch (e) {
            Ung.Util.rpcExHandler(e);
        }

        i18n=new Ung.I18N({"map":rpc.translations});
        i18n.timeoffset = (new Date().getTimezoneOffset()*60000)+rpc.timeZoneOffset;
        Ext.MessageBox.wait(i18n._("Starting..."), i18n._("Please wait"));
        Ung.Util.loadCss("/skins/"+rpc.skinSettings.skinName+"/css/adminNew.css");
        if (rpc.skinSettings.outOfDate) {
            var win = Ext.create('Ext.Window', {
                layout: 'fit',
                width: 300,
                height: 200,
                closeAction: 'hide',
                plain: true,
                html: i18n._('The current custom skin is no longer compatible and has been disabled. The Default skin is temporarily being used. To disable this message change the skin settings under Config Administration. To get more information on how to fix the custom skin: <a href="http://wiki.untangle.com/index.php/Skins" target="_blank">Where can I find updated skins and new skins?</a>'),
                title: i18n._('Skin Out of Date'),
                buttons: [ {
                    text: i18n._('Ok'),
                    handler: function() {
                        win.hide();
                    }
                }]
            });
            win.show();
        }
        this.setDocumentTitle();
        this.startApplication();
    },
    setDocumentTitle: function() {
        document.title = rpc.companyName + ((rpc.hostname!=null)?(" - " + rpc.hostname):"");
    },
    setAppLastState: function(displayName,state,options,download) {
        if(state==null) {
            Ung.Main.appsLastState[displayName]=null;
        } else {
            Ung.Main.appsLastState[displayName]={state:state, options:options, download:download};
        }
    },
    startApplication: function() {
        this.initExtI18n();
        this.initExtGlobal();
        this.initExtVTypes();
        Ext.on("resize", Ung.Util.resizeWindows);
        // initialize viewport object
        var contentRightArr=[
            '<div id="content-right">',
                '<div id="racks" style="">',
                    '<div id="rack-list"><div id="rack-select-container"></div><div id="parent-rack-container"></div><div id="alert-container" style="display:none;"></div><div id="no-ie-container" style="display:none;"></div>',
                    '</div>',
                    '<div id="rack-nodes">',
                        '<div id="filter_nodes"></div>',
                        '<div id="nodes-separator" style="display:none;"><div id="nodes-separator-text"></div></div>',
                        '<div id="service_nodes"></div>',
                    '</div>',
                '</div>',
            '</div>'];

        var cssRule = Ext.util.CSS.getRule(".content-left",true);
        this.contentLeftWidth = ( cssRule ) ? parseInt( cssRule.style.width, 10 ): 215;
        this.viewport = Ext.create('Ext.container.Viewport',{
            layout:'border',
            items:[{
                region: 'west',
                id: 'west',
                cls: "content-left",
                xtype: 'container',
                width: this.contentLeftWidth,
                layout: { type: 'vbox', align: 'stretch' },
                items: [{
                    xtype: 'container',
                    cls: "logo",
                    html: '<img src="/images/BrandingLogo.png?'+(new Date()).getTime()+'" border="0"/>',
                    border: false,
                    height: 141,
                    flex: 0
                }, {
                    xtype: 'tabpanel',
                    activeTab: 0,
                    deferredRender: false,
                    border: false,
                    flex: 1,
                    bodyStyle: 'background-color: transparent;',
                    defaults: {
                        autoScroll: true,
                        border: false,
                        bodyStyle: 'background-color: transparent;'
                    },
                    items:[{
                        xtype: 'panel',
                        title: i18n._('Apps'),
                        id: 'leftTabApps',
                        html:'<div id="appsItems"></div>',name:'Apps'
                    },{
                        xtype: 'panel',
                        title: i18n._('Config'),
                        id: 'leftTabConfig',
                        html: '<div id="configItems"></div>',
                        name: 'Config'
                    }],
                    bbar: [{
                        xtype: 'button',
                        name: 'Help',
                        iconCls: 'icon-help',
                        text: i18n._('Help'),
                        handler: function() {
                            Ung.Main.openHelp(null);
                        }
                    }, {
                        name: 'MyAccount',
                        iconCls: 'icon-myaccount',
                        text: i18n._('My Account'),
                        tooltip: i18n._('You can access your online account and reinstall apps you already purchased, redeem vouchers, or buy new ones.'),
                        handler: function() {
                            Ung.Main.openMyAccountScreen();
                        }
                    }, {
                        xtype: 'button',
                        name: 'Logout',
                        iconCls: 'icon-logout',
                        text: i18n._('Logout'),
                        handler: function() {
                            window.location.href = '/auth/logout?url=/webui&realm=Administrator';
                        }
                    }]
                }
            ]}, {
                region:'center',
                id: 'center',
                xtype: 'container',
                html: contentRightArr.join(""),
                cls: 'center-region',
                autoScroll: true
            }
        ]});
        Ext.QuickTips.init();

        Ung.Main.systemStats = new Ung.SystemStats({});
        this.loadConfig();
        this.loadPolicies();
    },
    about: function (forceReload) {
        if(forceReload || rpc.about === undefined) {
            var serverUID, fullVersion, language;
            try {
                serverUID = rpc.jsonrpc.UvmContext.getServerUID();
                fullVersion = rpc.jsonrpc.UvmContext.getFullVersion();
                language = rpc.languageManager.getLanguageSettings()['language'];
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }
            var query = "";
            query = query + "uid=" + serverUID;
            query = query + "&" + "version=" + fullVersion;
            query = query + "&" + "webui=true";
            query = query + "&" + "lang=" + language;

            rpc.about = query;
        }
        return rpc.about;
    },
    openLegal: function( topic ) {
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
    openHelp: function( topic ) {
        var baseUrl;
        try {
            baseUrl = rpc.jsonrpc.UvmContext.getHelpUrl();
        } catch (e) {
            Ung.Util.rpcExHandler(e);
        }
        var url = baseUrl + "?" + "source=" + topic + "&" + this.about();

        console.log("Open Url   :", url);
        window.open(url); // open a new window
    },
    openSupportScreen: function() {
        var url = rpc.storeUrl + "?" + "action=support" + "&" + this.about();
        window.open(url); // open a new window
    },
    openRegisterScreen: function() {
        var url = rpc.storeUrl + "?" + "action=register" + "&" + this.about();
        this.openIFrame( url, i18n._("Register"));
    },
    openMyAccountScreen: function() {
        var url = rpc.storeUrl + "?" + "action=my_account" + "&" + this.about();
        window.open(url); // open a new window
    },
    openLibItemStore: function (libItemName, title) {
        var url = rpc.storeUrl + "?" + "action=buy" + "&" + "libitem=" + libItemName + "&" + this.about() ;

        console.log("Open Url   :", url);
        window.open(url); // open a new window
    },
    openSetupWizardScreen: function() {
        var url = "/setup";
        window.open(url);
    },
    closeIframe: function() {
        if(this.iframeWin!=null && this.iframeWin.isVisible() ) {
            this.iframeWin.closeWindow();
        }
        this.reloadLicenses();
    },

    initExtI18n: function() {
        var locale = rpc.languageSettings.language;
        if(locale) {
          Ung.Util.loadScript('/ext4/locale/ext-lang-' + locale + '.js', Ung.Main.overrideLanguageSettings);
        } else {
            Ung.Main.overrideLanguageSettings();
        }
    },
    overrideLanguageSettings: function () {
        // Uncomment this to override the language timefield format for the current language
        //TODO: consider adding support to set this in a Time Format section in Config -> Settings -> Regional Settings (would be stored in AdminManager.AdminSettings)
        /*
        Ext.apply(Ext.form.field.Time.prototype, {
            format : "H:i"    //may also use: "g:i A"
        });
        */
    },
    initExtGlobal: function() {
        // init quick tips
        Ext.QuickTips.init();
    },
    // Add the additional 'advanced' VTypes
    initExtVTypes: function() {
        var macAddrMaskRe = /^[a-fA-F0-9]{2}:[a-fA-F0-9]{2}:[a-fA-F0-9]{2}:[a-fA-F0-9]{2}$/;
        var ip4AddrMaskRe = /^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/;
        var ip6AddrMaskRe = /^\s*((([0-9A-Fa-f]{1,4}:){7}([0-9A-Fa-f]{1,4}|:))|(([0-9A-Fa-f]{1,4}:){6}(:[0-9A-Fa-f]{1,4}|((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3})|:))|(([0-9A-Fa-f]{1,4}:){5}(((:[0-9A-Fa-f]{1,4}){1,2})|:((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3})|:))|(([0-9A-Fa-f]{1,4}:){4}(((:[0-9A-Fa-f]{1,4}){1,3})|((:[0-9A-Fa-f]{1,4})?:((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){3}(((:[0-9A-Fa-f]{1,4}){1,4})|((:[0-9A-Fa-f]{1,4}){0,2}:((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){2}(((:[0-9A-Fa-f]{1,4}){1,5})|((:[0-9A-Fa-f]{1,4}){0,3}:((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){1}(((:[0-9A-Fa-f]{1,4}){1,6})|((:[0-9A-Fa-f]{1,4}){0,4}:((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3}))|:))|(:(((:[0-9A-Fa-f]{1,4}){1,7})|((:[0-9A-Fa-f]{1,4}){0,5}:((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3}))|:)))(%.+)?\s*$/;
        var email = /^(")?(?:[^\."])(?:(?:[\.])?(?:[\w\-!#$%&'*+/=?^_`{|}~]))*\1@(\w[\-\w]*\.){1,5}([A-Za-z]){2,63}$/;
        Ext.apply(Ext.form.VTypes, {
            email: function (v) {
                return email.test(v);
            },
            ipMatcher: function(val) {
                if ( val.indexOf("/") == -1 && val.indexOf(",") == -1 && val.indexOf("-") == -1) {
                    switch(val) {
                      case 'any':
                        return true;
                    default:
                        return Ung.RuleValidator.isSingleIpValid(val);
                    }
                }
                if ( val.indexOf(",") != -1) {
                    return Ung.RuleValidator.isIpListValid(val);
                } else {
                    if ( val.indexOf("-") != -1) {
                        return Ung.RuleValidator.isIpRangeValid(val);
                    }
                    if ( val.indexOf("/") != -1) {
                        var cidrValid = Ung.RuleValidator.isCIDRValid(val);
                        var ipNetmaskValid = Ung.RuleValidator.isIpNetmaskValid(val);
                        return cidrValid || ipNetmaskValid;
                    }
                    console.log("Unhandled case while handling vtype for ipAddr:", val, " returning true !");
                    return true;
                }
            },
            ipMatcherText: i18n._('Invalid IP Address.'),

            ip4Address: function(val) {
                return ip4AddrMaskRe.test(val);
            },
            ip4AddressText: i18n._('Invalid IPv4 Address.'),

            ip4AddressList:  function(v) {
                var addr = v.split(",");
                for ( var i = 0 ; i < addr.length ; i++ ) {
                    if ( ! ip4AddrMaskRe.test(addr[i]) )
                        return false;
                }
                return true;
            },
            ip4AddressListText: i18n._('Invalid IPv4 Address(es).'),

            ip6Address: function(val) {
                return ip6AddrMaskRe.test(val);
            },
            ip6AddressText: i18n._('Invalid IPv6 Address.'),

            ipAddress: function(val) {
                return ip4AddrMaskRe.test(val) || ip6AddrMaskRe.test(val);
            },
            ipAddressText: i18n._('Invalid IP Address.'),

            macAddress: function(val) {
                return macAddrMaskRe.test(val);
            },
            macAddressText: i18n._('Invalid Mac Address.'),
            
            cidrBlock:  function(v) {
                return (/^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}\/\d{1,2}$/.test(v));
            },
            cidrBlockText: i18n._('Must be a network in CIDR format.') + ' ' + '(192.168.123.0/24)',

            cidrBlockList:  function(v) {
                var blocks = v.split(",");
                for ( var i = 0 ; i < blocks.length ; i++ ) {
                    if ( ! (/^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}\/\d{1,2}$/.test(blocks[i])) )
                        return false;
                }
                return true;
            },
            cidrBlockListText: i18n._('Must be a comma seperated list of networks in CIDR format.') + ' ' + '(192.168.123.0/24,1.2.3.4/24)',

            portMatcher: function(val) {
                switch(val) {
                  case 'any':
                    return true;
                default:
                    if ( val.indexOf('>') != -1 && val.indexOf(',') == -1) {
                        return Ung.RuleValidator.isSinglePortValid( val.substring( val.indexOf('>') + 1 ));
                    }
                    if ( val.indexOf('<') != -1 && val.indexOf(',') == -1) {
                        return Ung.RuleValidator.isSinglePortValid( val.substring( val.indexOf('<') + 1 ));
                    }
                    if ( val.indexOf('-') == -1 && val.indexOf(',') == -1) {
                        return Ung.RuleValidator.isSinglePortValid(val);
                    }
                    if ( val.indexOf('-') != -1 && val.indexOf(',') == -1) {
                        return Ung.RuleValidator.isPortRangeValid(val);
                    }
                    return Ung.RuleValidator.isPortListValid(val);
                }
            },
            portMatcherText: Ext.String.format(i18n._("The port must be an integer number between {0} and {1}."), 1, 65535),

            port: function(val) {
                var minValue = 1;
                var maxValue = 65535;
                return (minValue <= val && val <= maxValue);
            },
            portText: Ext.String.format(i18n._("The port must be an integer number between {0} and {1} or one of the following values: any, all, n/a, none."), 1, 65535),

            password: function(val) {
                if (field.initialPassField) {
                    var pwd = Ext.getCmp(field.initialPassField);
                    return (val == pwd.getValue());
                }
                return true;
            },
            passwordText: i18n._('Passwords do not match')
        });
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
            // no policy manager, just one policy (Default Rack)
            rpc.policies = [{
                javaClass: "com.untangle.node.policy.PolicySettings",
                policyId: "1",
                name: i18n._("Default Rack"),
                description: i18n._("The Default Rack/Policy")
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
        var node={};
        node.nodeId=nodeSettings.id;
        node.nodeSettings=nodeSettings;
        node.type=nodeProperties.type;
        node.hasPowerButton=nodeProperties.hasPowerButton;
        node.name=nodeProperties.name;
        node.displayName=nodeProperties.displayName;
        node.license=license;
        node.image='chiclet?name='+node.name;
        node.metrics=nodeMetrics;
        node.runState=runState;
        node.viewPosition=nodeProperties.viewPosition;
        return node;
    },
    buildApps: function () {
        //destroy Apps
        var i;
        if(Ung.Main.apps!=null) {
            for(i=0; i<Ung.Main.apps.length; i++) {
                Ext.destroy(Ung.Main.apps[i]);
            }
            this.apps=null;
        }
        //build Apps
        Ung.Main.apps=[];
        for(i=0;i<rpc.rackView.installable.list.length;i++) {
            var application=rpc.rackView.installable.list[i];
            var appCmp = Ext.create("Ung.AppItem", {nodeProperties: application});
            Ung.Main.apps.push(appCmp);
        }
    },
    buildNodes: function() {
        //build nodes
        Ung.MetricManager.stop();
        Ext.getCmp('policyManagerMenuItem').disable();
        var nodePreviews = Ext.clone(Ung.Main.nodePreviews);
        this.destoyNodes();
        this.nodes=[];
        var i;
        var node;

        for(i=0;i<rpc.rackView.instances.list.length;i++) {
            var nodeSettings=rpc.rackView.instances.list[i];
            var nodeProperties=rpc.rackView.nodeProperties.list[i];

            node=this.createNode(nodeProperties,
                     nodeSettings,
                     rpc.rackView.nodeMetrics.map[nodeSettings.id],
                     rpc.rackView.licenseMap.map[nodeProperties.name],
                     rpc.rackView.runStates.map[nodeSettings.id]);
            this.nodes.push(node);
        }
        if(!rpc.isRegistered) {
            this.showWelcomeScreen();
        }
        this.updateSeparator();
        for(i=0; i<this.nodes.length; i++) {
            node=this.nodes[i];
            this.addNode(node, nodePreviews[node.name]);
        }
        if(!Ung.Main.disableThreads) {
            Ung.MetricManager.start(true);
        }
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
                                nodeCmp.onSettingsAction();
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
        if(Ext.MessageBox.isVisible() && Ext.MessageBox.title==i18n._("Please wait")) {
            Ext.Function.defer(Ext.MessageBox.hide,30,Ext.MessageBox);
        }
    },
    // load the rack view for current policy
    loadRackView: function() {
        var callback = Ext.bind(function (result, exception) {
            if(Ung.Util.handleException(exception)) return;
            rpc.rackView=result;
            var parentRackName = this.getParentName( rpc.currentPolicy.parentId );
            var parentRackDisplay = Ext.get('parent-rack-container');

            if (parentRackName === "None") {
                parentRackDisplay.dom.innerHTML = "";
                parentRackDisplay.hide();
            } else {
                parentRackDisplay.show();
                parentRackDisplay.dom.innerHTML = i18n._("Parent Rack")+":<br/>" + parentRackName;
            }

            Ung.Main.buildApps();
            Ung.Main.buildNodes();
        }, this);
        Ung.Util.RetryHandler.retry( rpc.rackManager.getRackView, rpc.rackManager, [ rpc.currentPolicy.policyId ], callback, 1500, 10 );
    },
    loadApps: function() {
        var callback = Ext.bind(function(result,exception) {
            if(Ung.Util.handleException(exception)) return;
            rpc.rackView=result;
            Ung.Main.buildApps();
        }, this);
        Ung.Util.RetryHandler.retry( rpc.rackManager.getRackView, rpc.rackManager, [ rpc.currentPolicy.policyId ], callback, 1500, 10 );
    },
    updateRackView: function() {
        var callback = Ext.bind(function(result,exception) {
            if(Ung.Util.handleException(exception)) return;
            rpc.rackView=result;
            var i=0, j=0; installableNodes=rpc.rackView.installable.list;
            var updatedApps = [];
            while(i<installableNodes.length || j<Ung.Main.apps.length) {
                var appCmp;
                if(i==installableNodes.length) {
                    Ext.destroy(Ung.Main.apps[j]);
                    Ung.Main.apps[j]=null;
                    j++;
                } else if(j == Ung.Main.apps.length) {
                    appCmp = Ext.create("Ung.AppItem", {nodeProperties: installableNodes[i], renderPosition: updatedApps.length});
                    updatedApps.push(appCmp);
                    i++;
                } else if(installableNodes[i].name == Ung.Main.apps[j].nodeProperties.name) {
                    updatedApps.push(Ung.Main.apps[j]);
                    i++;
                    j++;
                } else if(installableNodes[i].viewPosition < Ung.Main.apps[j].nodeProperties.viewPosition) {
                    appCmp = Ext.create("Ung.AppItem", {nodeProperties: installableNodes[i], renderPosition: updatedApps.length});
                    updatedApps.push(appCmp);
                    i++;
                } else if(installableNodes[i].viewPosition >= Ung.Main.apps[j].nodeProperties.viewPosition){
                    Ext.destroy(Ung.Main.apps[j]);
                    Ung.Main.apps[j]=null;
                    j++;
                }
            }
            Ung.Main.apps=updatedApps;
            Ung.Main.buildNodes();
        }, this);
        Ung.Util.RetryHandler.retry( rpc.rackManager.getRackView, rpc.rackManager, [ rpc.currentPolicy.policyId ], callback, 1500, 10 );
    },
    loadLicenses: function() {
        try {
            //force re-sync with server
            Ung.Main.getLicenseManager().reloadLicenses();
        } catch (e) {
            Ung.Util.rpcExHandler(e, true);
        }
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
        }, this));
    },

    installNode: function(nodeProperties, appItem, completeFn) {
        if(!rpc.isRegistered) {
            Ung.Main.openRegisterScreen();
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

        Ung.AppItem.updateState( nodeProperties.displayName, "loadapp");
        Ung.Main.addNodePreview( nodeProperties );
        rpc.nodeManager.instantiate(Ext.bind(function (result, exception) {
            if (exception) {
                Ung.AppItem.updateState( nodeProperties.displayName, null );
                Ung.Main.removeNodePreview( nodeProperties.name );
                Ung.Main.updateRackView();
                Ung.Util.handleException(exception);
                return;
            }
            Ung.Main.updateRackView();
            if (completeFn)
                completeFn();
        }, this), nodeProperties.name, rpc.currentPolicy.policyId);
    },
    getIframeWin: function() {
        if(this.iframeWin==null) {
            this.iframeWin=Ext.create("Ung.Window",{
                id: 'iframeWin',
                layout: 'fit',
                defaults: {},
                items: {
                    html: '<iframe id="iframeWin_iframe" name="iframeWin_iframe" width="100%" height="100%" frameborder="0"/>'
                },
                closeWindow: function() {
                    this.setTitle('');
                    this.hide();
                    window.frames["iframeWin_iframe"].location.href="/webui/blank.html";
                    Ung.Main.reloadLicenses();
                }
            });
        }
        return this.iframeWin;
    },
    // load Config
    loadConfig: function() {
        this.config =
            [{
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
                iconClass: 'icon-config-setup',
                helpSource: 'system',
                className: 'Webui.config.system'
            }, {
                name: 'about',
                displayName: i18n._('About'),
                iconClass: 'icon-config-support',
                helpSource: 'system_info',
                className: 'Webui.config.about'
            }];
        this.configMap = Ung.Util.createRecordsMap(this.config, "name");
        for(var i=0;i<this.config.length;i++) {
            Ext.create('Ung.ConfigItem', {
                item: this.config[i]
            });
        }
    },
    checkForIE: function (handler) {
        if (Ext.isIE) {
            var noIEDisplay = Ext.get('no-ie-container');
            noIEDisplay.show();

            this.noIEToolTip= new Ext.ToolTip({
                target: document.getElementById("no-ie-container"),
                dismissDelay: 0,
                hideDelay: 1500,
                width: 500,
                cls: 'extended-stats',
                html: i18n._("For an optimal experience use Google Chrome or Mozilla Firefox.")
            });
            this.noIEToolTip.render(Ext.getBody());
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
            var alertDisplay = Ext.get('alert-container');
            var alertArr=[];

            if (result != null && result.list.length > 0) {
                alertDisplay.show();
                alertArr.push('<div class="title">'+i18n._("Alerts:")+'</div>');
                for (var i = 0; i < result.list.length; i++) {
                    alertArr.push('<div class="values">&middot;&nbsp;'+i18n._(result.list[i])+'</div>');
                }
            } else {
                alertDisplay.hide();
            }

            this.alertToolTip= new Ext.ToolTip({
                target: document.getElementById("alert-container"),
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
            this.alertToolTip.render(Ext.getBody());
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
    destoyNodes: function () {
        if(this.nodes!==null) {
            for(var i=0;i<this.nodes.length;i++) {
                var node=this.nodes[i];
                var cmp=Ung.Node.getCmp(this.nodes[i].nodeId);
                if(cmp) {
                    cmp.destroy();
                    cmp=null;
                }
            }
        }
        for(var nodeName in this.nodePreviews) {
            Ung.Main.removeNodePreview(nodeName);
        }
    },
    getNodePosition: function(place, viewPosition) {
        var placeEl=document.getElementById(place);
        var position=0;
        if(placeEl.hasChildNodes()) {
            for(var i=0;i<placeEl.childNodes.length;i++) {
                if(placeEl.childNodes[i].getAttribute('viewPosition')-viewPosition<0) {
                    position=i+1;
                } else {
                    break;
                }
            }
        }
        return position;
    },
    addNode: function (node, fadeIn) {
        var nodeWidget=new Ung.Node(node);
        nodeWidget.fadeIn=fadeIn;
        var place=(node.type=="FILTER")?'filter_nodes':'service_nodes';
        var position=this.getNodePosition(place,node.viewPosition);
        nodeWidget.render(place,position);
        Ung.AppItem.updateState(node.displayName, null);
        if ( node.name == 'untangle-node-policy') {
            // refresh rpc.policyManager to properly handle the case when the policy manager is removed and then re-added to the application list
            try {
                rpc.policyManager=rpc.jsonrpc.UvmContext.nodeManager().node("untangle-node-policy");
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }
            Ext.getCmp('policyManagerMenuItem').enable();
            this.policyNodeWidget = nodeWidget;
        }
    },
    addNodePreview: function ( nodeProperties ) {
        var nodeWidget=new Ung.NodePreview( nodeProperties );
        var place = ( nodeProperties.viewPosition < 1000) ? 'filter_nodes' : 'service_nodes';
        var position = this.getNodePosition( place, nodeProperties.viewPosition );
        nodeWidget.render(place,position);
        Ung.Main.nodePreviews[ nodeProperties.name ]=true;
    },
    removeNodePreview: function(nodeName) {
        if(Ung.Main.nodePreviews[nodeName] !== undefined) {
            delete Ung.Main.nodePreviews[nodeName];
        }
        var nodePreview=Ext.getCmp("node_preview_"+nodeName);
        if(nodePreview) {
            Ext.destroy(nodePreview);
        }
    },
    removeNode: function(index) {
        var tid = Ung.Main.nodes[index].nodeId;
        var nodeUI = (tid != null) ? Ext.getCmp('node_'+tid): null;
        Ung.Main.nodes.splice(index, 1);
        if(nodeUI) {
            Ext.destroy(nodeUI);
            return true;
        }
        return false;
    },
    getNode: function(nodeName, nodePolicyId) {
        var cp = rpc.currentPolicy.policyId ,np = null;
        if(Ung.Main.nodes) {
            for (var i = 0; i < Ung.Main.nodes.length; i++) {
                if(nodePolicyId==null) {
                    cp = null;
                } else {
                    cp = Ung.Main.nodes[i].nodeSettings.policyId;
                }
                if ((nodeName == Ung.Main.nodes[i].name)&& (nodePolicyId==cp)) {
                    return Ung.Main.nodes[i];
                }
            }
        }
        return null;
    },
    removeParentNode: function (node, nodePolicyId) {
        var cp = rpc.currentPolicy.policyId;
        if(Ung.Main.nodes) {
            for (var i = 0; i < Ung.Main.nodes.length; i++) {
                cp = (nodePolicyId==null) ? null : Ung.Main.nodes[i].nodeSettings.policyId;
                if (node.name === Ung.Main.nodes[i].name) {
                    if(nodePolicyId!=cp) {
                        //parent found
                        return Ung.Main.removeNode(i);
                    }
                }
            }
        }
        return false;
    },
    isNodeRunning: function(nodeName) {
        var node = Ung.Main.getNode(nodeName);
        if (node != null) {
             var nodeCmp = Ung.Node.getCmp(node.nodeId);
             if (nodeCmp != null && nodeCmp.isRunning()) {
                return true;
             }
        }
        return false;
    },
    // Show - hide Services header in the rack
    updateSeparator: function() {
        var hasUtil=false;
        var hasService=false;
        for(var i=0;i<this.nodes.length;i++) {
            if(this.nodes[i].type != "FILTER") {
                hasService=true;
                if(this.nodes[i].type != "SERVICE") {
                    hasUtil=true;
                }
            }
        }
        document.getElementById("nodes-separator-text").innerHTML=(hasService && hasUtil)?i18n._("Services & Utilities"):hasService?i18n._("Services"):"";
        document.getElementById("nodes-separator").style.display= hasService?"":"none";
        if(hasService) {
            document.getElementById("racks").style.backgroundPosition="0px 100px";
        } else {
            document.getElementById("racks").style.backgroundPosition="0px 50px";
        }
    },
    // build policies select box
    buildPolicies: function () {
        if(Ung.Main.rackSelect!=null) {
            Ext.destroy(Ung.Main.rackSelect);
            Ext.get('rack-select-container').dom.innerHTML = '';
        }
        var items=[];
        var selVirtualRackIndex = 0;
        rpc.policyNamesMap = {};
        rpc.policyNamesMap[0] = i18n._("No Rack");
        for( var i=0 ; i<rpc.policies.length ; i++ ) {
            var policy = rpc.policies[i];

            selVirtualRackIndex = (policy.policyId ==1 ? i: selVirtualRackIndex);

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
            }
        }
        items.push('-');
        items.push({text: i18n._('Show Policy Manager'), value: 'SHOW_POLICY_MANAGER', handler: Ung.Main.showPolicyManager, id:'policyManagerMenuItem', disabled: true, hideDelay: 0});
        items.push('-');
        items.push({text: i18n._('Show Sessions'), value: 'SHOW_SESSIONS', handler: Ung.Main.showSessions, hideDelay: 0});
        items.push({text: i18n._('Show Hosts'), value: 'SHOW_HOSTS', handler: Ung.Main.showHosts, hideDelay: 0});
        Ung.Main.rackSelect = new Ext.SplitButton({
            renderTo: 'rack-select-container', // the container id
            text: items[selVirtualRackIndex].text,
            id:'rack-select',
            menu: new Ext.menu.Menu({
                hideDelay: 0,
                items: items
            })
        });
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
                Ung.Main.hostMonitorWin=Ext.create('Webui.config.hostMonitor', {"name":"hostMonitor", "helpSource":"host_viewer"});
            }
            Ung.Main.hostMonitorWin.show();
            Ext.MessageBox.wait(i18n._("Loading..."), i18n._("Please wait"));
            Ext.Function.defer(function() {
                Ung.Main.hostMonitorWin.gridCurrentHosts.reload();
                Ext.MessageBox.hide();
            }, 10, this);
        }, this);
    },
    showSessions: function() {
        Ung.Main.showNodeSessions(0);
    },
    showNodeSessions: function(nodeIdArg) {
        Ext.require(['Webui.config.sessionMonitor'], function() {
            if ( Ung.Main.sessionMonitorWin == null) {
                Ung.Main.sessionMonitorWin=Ext.create('Webui.config.sessionMonitor', {"name":"sessionMonitor", "helpSource":"session_viewer"});
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
        if (Ung.Main.policyNodeWidget) {
            Ung.Main.policyNodeWidget.loadSettings();
        }
    },
    // change current policy
    changeRack: function () {
        Ext.getCmp('rack-select').setText(this.text);
        rpc.currentPolicy=rpc.policies[this.index];
        Ung.Main.loadRackView();
    },
    getParentName: function( parentId ) {
        if( parentId == null ) {
            return i18n._("None");
        }
        if ( rpc.policies === null ) {
            return i18n._("None");
        }
        for ( var c = 0 ; c < rpc.policies.length ; c++ ) {
            if ( rpc.policies[c].policyId == parentId ) {
                return rpc.policies[c].name;
            }
        }
        return i18n._("None");
    },
    /**
     * Opens a link in a iframe pop-up window in the middle of the rack
     */
    openIFrame: function( url, title ) {
        console.log("Open IFrame:", url);
        if ( url == null ) {
            alert("can not open window to null URL");
        }
        var iframeWin = Ung.Main.getIframeWin();

        var position = [];
        var size = Ung.Main.viewport.getSize();
        var centerSize = Ext.getCmp('center').getSize();
        var centerPosition = Ext.getCmp('center').getPosition();
        var scale = 0.90;
        if ( centerSize.width < 850 ) {
            scale = 1.00; // if we are in a low resolution, use the whole rack screen
            position[0] = centerPosition[0];
            position[1] = centerPosition[1];
        } else {
            scale = 0.90; // use 90% of the screen for the popup
            position[0] = centerPosition[0]+Math.round(centerSize.width/20);
            position[1] = centerPosition[1]+Math.round(centerSize.height/20);
        }
        iframeWin.show();

        iframeWin.setSize({width:centerSize.width*scale,height:centerSize.height*scale});
        iframeWin.setPosition(position[0],position[1]);
        iframeWin.setTitle(title);

        window.frames["iframeWin_iframe"].location.href = url;
    },
    openFailureScreen: function () {
        var url = "/webui/offline.jsp";
        this.openIFrame( url, i18n._("Warning") );
    },
    /**
     *  Prepares the uvm to display the welcome screen
     */
    showWelcomeScreen: function () {
        if(this.welcomeScreenAlreadShown) {
            return;
        }
        this.welcomeScreenAlreadShown = true;

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
                Ung.Main.openRegisterScreen();
                Ung.CheckStoreRegistration.start();
            }
        }, this));
    },
    /**
     *  Hides the welcome screen
     */
    hideWelcomeScreen: function() {
        Ung.Main.closeIframe();
    },
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
                    var apps = [ "Web Filter",
                                 //"Web Filter Lite",
                                 "Virus Blocker",
                                 //"Virus Blocker Lite",
                                 "Spam Blocker",
                                 //"Spam Blocker Lite",
                                 //"Phish Blocker",
                                 //"Web Cache",
                                 "Bandwidth Control",
                                 "HTTPS Inspector",
                                 "Application Control",
                                 //"Application Control Lite",
                                 "Captive Portal",
                                 "Firewall",
                                 //"Intrusion Prevention",
                                 //"Ad Blocker",
                                 "Reports",
                                 "Policy Manager",
                                 "Directory Connector",
                                 "WAN Failover",
                                 "WAN Balancer",
                                 "IPsec VPN",
                                 "OpenVPN",
                                 "Configuration Backup",
                                 "Branding Manager",
                                 "Live Support"];

                    // only install this on 1gig+ machines
                    if ( Ung.Main.totalMemoryMb > 900 ) {
                        apps.splice(2,0,"Virus Blocker Lite");
                        apps.splice(4,0,"Phish Blocker");
                    }

                    var fn = function( appsToInstall ) {
                        // if there are no more apps left to install we are done
                        if ( appsToInstall.length == 0 ) {
                            Ext.MessageBox.alert(i18n._("Installation Complete!"), i18n._("Thank you for using Untangle!"));
                            return;
                        }
                        var name = appsToInstall[0];
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