/*global
 Ext, Ung, Webui, rpc:true, i18n:true, setTimeout, clearTimeout, console, window, document, JSONRpcClient, Highcharts
 */
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
    countryList: [],
    appPreviews: {},
    config: null,
    totalMemoryMb: 2000,
    apps: null,
    // the Ext.Viewport object for the application
    viewport: null,
    menuWidth: null,
    stats: null,
    init: function (config) {
        Ext.MessageBox.wait(i18n._("Starting..."), i18n._("Please wait"));
        Ext.apply(this, config);
        if (Ext.isGecko) {
            document.onkeypress = function (e) {
                return e.keyCode !== 27;
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
            rpc.policyManager = rpc.appManager.app("policy-manager");
        } catch (e) {
            Ung.Util.rpcExHandler(e);
        }
        if (rpc.isRegistered) {
            Ung.Main.getLicenseManager().reloadLicenses(Ext.bind(function (result, exception) {
              //just make sure the licenses are reloaded on page start
            }, this), true);
        }

        // non API store URL used for links like: My Account, Forgot Password
        this.storeUrl = rpc.storeUrl.replace('/api/v1', '/store/open.php');
        this.startApplication();
    },
    startApplication: function () {
        if (Ext.supports.LocalStorage) {
            Ext.state.Manager.setProvider(Ext.create('Ext.state.LocalStorageProvider'));
        }
        this.target = Ung.Util.getQueryStringParam("target");
        i18n = Ext.create('Ung.I18N', {
            map: rpc.translations,
            timeoffset: (new Date().getTimezoneOffset() * 60000) + rpc.timeZoneOffset
        });
        Highcharts.setOptions({
            global: {
                timezoneOffset: -(rpc.timeZoneOffset / 60000)
            }
        });

        Ung.Util.loadCss("/skins/" + rpc.skinSettings.skinName + "/css/common.css");
        Ung.Util.loadCss("/skins/" + rpc.skinSettings.skinName + "/css/admin.css");
        Ung.Util.loadCss("/skins/" + rpc.skinSettings.skinName + "/css/apps.css");

        document.title = rpc.companyName + (rpc.hostname ? " - " + rpc.hostname : "");
        if (rpc.languageSettings.language) {
            Ung.Util.loadScript('/ext6/classic/locale/locale-' + rpc.languageSettings.language + '.js');
        }
        Ung.VTypes.init(i18n);
        Ext.tip.QuickTipManager.init();
        Ext.on("resize", Ung.Util.resizeWindows);

        this.viewport = Ext.create('Ext.container.Viewport', {
            layout: 'border',
            items: [{
                region: 'north',
                xtype: 'container',
                name: 'mainMenu',
                cls: "main-menu",
                layout: { type: 'hbox', align: 'middle' },
                items: [{
                    xtype: 'component',
                    margin: '3 10 3 10',
                    cls: 'logo',
                    height: 60,
                    width: 100,
                    style: "background-image: url(/images/BrandingLogo.png?" + (new Date()).getTime() + ");"
                }, {
                    xtype: 'container',
                    cls: 'views-menu',
                    itemId: 'viewsMenu',
                    defaults: {
                        scale: 'large'
                    },
                    hidden: false,
                    plugins: 'responsive',
                    responsiveConfig: {
                        'width <= 520': {
                            hidden: true
                        },
                        'width > 520': {
                            hidden: false
                        }
                    },
                    items: [{
                        xtype: 'button',
                        html: '<i class="material-icons">home</i> <span>' + i18n._('Dashboard') + '</span>',
                        cls: 'main-menu-btn',
                        pressed: rpc.isRegistered,
                        handler: function (btn) {
                            if (!this.dashboardManager.isHidden()) {
                                this.dashboardManager.hide();
                            }
                            this.dashboardManager.removeAll(true);
                            this.panelCenter.setActiveItem("dashboard");
                            this.viewsMenu.items.each(function (button) { button.setPressed(false); });
                            btn.setPressed(true);
                        },
                        scope: this
                    }, {
                        xtype: 'button',
                        html: '<i class="material-icons">apps</i> <span>' + i18n._('Apps') + '</span>',
                        cls: 'main-menu-btn',
                        pressed: !rpc.isRegistered,
                        handler: function (btn) {
                            this.panelCenter.setActiveItem((rpc.rackView && rpc.rackView.instances.list.length == 0) ? 'installApps' : 'apps');
                            this.viewsMenu.items.each(function (button) { button.setPressed(false); });
                            btn.setPressed(true);
                        },
                        scope: this
                    }, {
                        xtype: 'button',
                        html: '<i class="material-icons">tune</i> <span>' + i18n._('Config') + '</span>',
                        cls: 'main-menu-btn',
                        handler: function (btn) {
                            this.panelCenter.setActiveItem("config");
                            this.viewsMenu.items.each(function (button) { button.setPressed(false); });
                            btn.setPressed(true);
                        },
                        scope: this
                    }, {
                        xtype: 'button',
                        html: '<i class="material-icons">show_chart</i> <span>' + i18n._('Reports') + '</span>',
                        id: 'reportsMenuItem',
                        cls: 'main-menu-btn',
                        hidden: !rpc.reportsEnabled,
                        handler: this.reportsMenuHandler,
                        scope: this
                    }]
                }, {
                    xtype: 'container',
                    flex: 1
                }, {
                    xtype: "container",
                    cls: 'notification-container',
                    items: [{
                        xtype: "button",
                        cls: 'main-menu-btn notification-button',
                        scale: 'large',
                        arrowVisible: false,
                        html: '<i class="material-icons" style="color: #FFB300; vertical-align: middle; font-size: 20px;">warning</i>',
                        itemId: "notificationButton",
                        hidden: true,
                        menuAlign: 'tr-br'
                    }]
                }, {
                    xtype: 'container',
                    cls: 'user-menu',
                    defaults: {
                        scale: 'large'
                    },
                    hidden: false,
                    plugins: 'responsive',
                    responsiveConfig: {
                        'width <= 520': {
                            hidden: true
                        },
                        'width > 520': {
                            hidden: false
                        }
                    },
                    items: [{
                        xtype: 'button',
                        html: '<i class="material-icons">help</i> <span>' + i18n._('Help') + '</span>',
                        cls: 'main-menu-btn',
                        href: this.getHelpLink(null),
                        hrefTarget: '_blank'
                    }, {
                        xtype: 'button',
                        html: '<i class="material-icons">&#xE853;</i> <span><span>' + i18n._('Account') + '</span><i class="material-icons">arrow_drop_down</i></span>',
                        cls: 'main-menu-btn',
                        margin: '0, 10, 0, 0',
                        arrowVisible: false,
                        menuAlign: 'tr-br',
                        menu: Ext.create('Ext.menu.Menu', {
                            cls: 'user-menu-dd',
                            shadow: false,
                            width: 200,
                            plain: true,
                            padding: '15 0 10 0',
                            items: [{
                                text: '<i class="material-icons">settings</i> <span>' + i18n._('My Account') + '</span>',
                                href: this.getMyAccountLink(),
                                hrefTarget: '_blank',
                                handler: function () {
                                    return Ung.LicenseLoader.check();
                                }
                            }, {
                                text: '<i class="material-icons">exit_to_app</i> <span>' + i18n._('Logout') + '</span>',
                                href: '/auth/logout?url=/webui&realm=Administrator'
                            }]
                        })
                    }]
                }, {
                    xtype: 'container',
                    cls: 'hamburger-menu',
                    defaults: {
                        scale: 'large'
                    },
                    hidden: true,
                    plugins: 'responsive',
                    responsiveConfig: {
                        'width <= 520': {
                            hidden: false
                        },
                        'width > 520': {
                            hidden: true
                        }
                    },
                    items: [{
                        xtype: 'button',
                        html: '<i class="material-icons">menu</i>',
                        cls: 'main-menu-btn',
                        margin: '0, 10, 0, 0',
                        arrowVisible: false,
                        menuAlign: 'tr-br',
                        menu: Ext.create('Ext.menu.Menu', {
                            cls: 'user-menu-dd',
                            shadow: false,
                            width: 200,
                            plain: true,
                            padding: '15 0 10 0',
                            items: [{
                                text: '<i class="material-icons">home</i> <span>' + i18n._('Dashboard') + '</span>',
                                handler: function () {
                                    if (!this.dashboardManager.isHidden()) {
                                        this.dashboardManager.hide();
                                    }
                                    this.dashboardManager.removeAll(true);
                                    this.panelCenter.setActiveItem("dashboard");

                                    this.viewsMenu.items.each(function (button, idx) {
                                        button.setPressed(idx === 0);
                                    });
                                },
                                scope: this
                            }, {
                                text: '<i class="material-icons">apps</i> <span>' + i18n._('Apps') + '</span>',
                                handler: function () {
                                    this.panelCenter.setActiveItem((rpc.rackView && rpc.rackView.instances.list.length == 0) ? 'installApps' : 'apps');
                                    this.viewsMenu.items.each(function (button, idx) {
                                        button.setPressed(idx === 1);
                                    });
                                },
                                scope: this
                            }, {
                                text: '<i class="material-icons">tune</i> <span>' + i18n._('Config') + '</span>',
                                handler: function () {
                                    this.panelCenter.setActiveItem("config");
                                    this.viewsMenu.items.each(function (button, idx) {
                                        button.setPressed(idx === 2);
                                    });
                                },
                                scope: this
                            }, {
                                text: '<i class="material-icons">show_chart</i> <span>' + i18n._('Reports') + '</span>',
                                handler: this.reportsMenuHandler,
                                scope: this
                            }, '-', {
                                text: '<i class="material-icons">help</i> <span>' + i18n._('Help') + '</span>',
                                href: this.getHelpLink(null),
                                hrefTarget: '_blank'
                            }, '-', {
                                text: '<i class="material-icons">account_circle</i> <span>' + i18n._('My Account') + '</span>',
                                href: this.getMyAccountLink(),
                                hrefTarget: '_blank',
                                handler: function () {
                                    return Ung.LicenseLoader.check();
                                }
                            }, {
                                text: '<i class="material-icons">exit_to_app</i> <span>' + i18n._('Logout') + '</span>',
                                href: '/auth/logout?url=/webui&realm=Administrator'
                            }]
                        })
                    }]
                }]
            }, {
                region: 'center',
                itemId: 'panelCenter',
                xtype: 'container',
                cls: 'main-panel',
                layout: 'card',
                activeItem: rpc.isRegistered ? 'dashboard' : 'apps',
                items: [{
                    itemId: 'dashboard',
                    layout: 'border',
                    border: false,
                    //scrollable: true,
                    items: [{
                        xtype: 'container',
                        region: 'west',
                        name: 'dashboardManager',
                        cls: 'dashboard-manager',
                        width: 350,
                        hidden: true,
                        border: false,
                        layout: {
                            type: 'vbox',
                            align: 'stretch',
                            pack : 'start'
                        },
                        style: {
                            boxShadow: '0 3px 5px rgba(0, 0, 0, 0.3)',
                            zIndex: 100
                        },
                        items: [],
                        listeners: {
                            hide: function () {
                                Ung.Main.panelCenter.down('[name=closeManagerBtn]').show();
                            },
                            show: function () {
                                Ung.Main.panelCenter.down('[name=closeManagerBtn]').hide();
                            }
                        }
                    }, {
                        layout: 'border',
                        region: 'center',
                        border: false,
                        items: [{
                            xtype: 'container',
                            region: 'north',
                            cls: 'top-container',
                            layout: {type: 'hbox', align: 'middle'},
                            height: 44,
                            items: [{
                                xtype: 'button',
                                margin: '0 0 0 10',
                                scale: 'medium',
                                cls: 'action-button material-button',
                                name: 'closeManagerBtn',
                                plugins: 'responsive',
                                responsiveConfig: {
                                    'width <= 520': {
                                        text: '<i class="material-icons">settings_applications</i>'
                                    },
                                    'width > 520': {
                                        text: '<i class="material-icons">settings_applications</i> <span style="vertical-align: middle;">' + i18n._("Manage Widgets") + '</span>'
                                    }
                                },
                                handler: function (btn) {
                                    this.dashboardManager.show();
                                    if (this.dashboardManager.items.length === 0) {
                                        this.dashboardManager.add({
                                            xtype: 'container',
                                            cls: 'dashboard-manager-header',
                                            layout: {
                                                type: 'hbox',
                                                align: 'middle'
                                            },
                                            items: [{
                                                xtype: 'component',
                                                html: '<h3>' + i18n._('Manage Widgets') + '</h3>',
                                                style: {
                                                    color: '#CCC'
                                                },
                                                padding: '5 10 5 10',
                                                border: false
                                            }, {
                                                xtype: 'component',
                                                html: '',
                                                flex: 1
                                            }, {
                                                xtype: 'button',
                                                border: false,
                                                margin: '0 5 0 0',
                                                style: {
                                                    background: 'none'
                                                },
                                                html: '<i class="material-icons" style="color: #CCC;">close</i>',
                                                handler: function () {
                                                    this.dashboardManager.hide();
                                                },
                                                scope: this
                                            }]
                                        });
                                        this.dashboardManager.add(Ext.create('Webui.config.dashboardManager', {}));
                                        this.dashboardManager.add({
                                            xtype: 'container',
                                            html: '<hr/><table>' +
                                                    '<tr><td style="width: 55px; text-align: right; vertical-align: top;"><i class="material-icons" style="color: #FFB300; font-size: 20px; vertical-align: middle;">warning</i></td><td>requires <em>Reports</em> App and any associated App</td></tr>' +
                                                    '<tr><td style="text-align: right;"><i class="material-icons" style="color: rgba(103,189,74,.9); font-size: 20px; vertical-align: middle;">visibility</i> | <i class="material-icons" style="color: #777; font-size: 20px; vertical-align: middle;">visibility_off</i></td><td>enables or disables the widget</td></tr>' +
                                                    '<tr><td style="text-align: right;"><i class="material-icons" style="color: #999; font-size: 20px; vertical-align: middle;">format_line_spacing</i></td><td>drag items to sort them</td></tr>' +
                                                  '</table>',
                                            /*
                                            html: '<hr/><p>' + '<i class="material-icons" style="color: #FFB300; font-size: 20px; vertical-align: middle;">warning</i> <span style="vertical-align: middle;">- Requires the Reports application and associated App to be installed and enabled, in order to see the widget in Dashboard.</span>' + '</p>' +
                                                  '<p><i class="material-icons" style="color: rgba(103,189,74,.9); font-size: 20px; vertical-align: middle;">visibility</i> | <i class="material-icons" style="color: #777; font-size: 20px; vertical-align: middle;">visibility_off</i> - toggles widget visibility on or off</p>' +
                                                  '<p><i class="material-icons" style="color: #999; font-size: 20px; vertical-align: middle;">format_line_spacing</i> - you can drag the items to sort them!</p>',
                                                  */
                                            style: {
                                                color: '#CCC'
                                            },
                                            padding: '0 10 0 10',
                                            border: false
                                        });
                                    }
                                    //Ung.Main.showDashboardManager();
                                },
                                scope: this
                            }, {
                                xtype: 'component',
                                html: '',
                                flex: 1
                            }, {
                                xtype: 'button',
                                cls: 'action-button material-button',
                                text: '<span>' + i18n._("Sessions") + '</span>',
                                height: 30,
                                margin: '0 5 0 0',
                                handler: Ung.Main.showSessions
                            }, {
                                xtype: 'button',
                                cls: 'action-button material-button',
                                text: '<span>' + i18n._("Hosts") + '</span>',
                                height: 30,
                                margin: '0 5 0 0',
                                handler: Ung.Main.showHosts
                            }, {
                                xtype: 'button',
                                cls: 'action-button material-button',
                                text: '<span>' + i18n._("Devices") + '</span>',
                                height: 30,
                                margin: '0 10 0 0',
                                handler: Ung.Main.showDevices
                            }, {
                                xtype: 'button',
                                cls: 'action-button material-button',
                                text: '<span>' + i18n._("Users") + '</span>',
                                height: 30,
                                margin: '0 10 0 0',
                                handler: Ung.Main.showUsers
                            }]
                        }, {
                            xtype: 'container',
                            region: 'center',
                            scrollable: true,
                            items: [{
                                xtype: 'container',
                                itemId: 'dashboardItems',
                                cls: 'dashboard',
                                listeners: {
                                    resize: function () {
                                        if (timer !== 'undefined') {
                                            clearTimeout(timer);
                                        }
                                        var timer = setTimeout(function () {
                                            Highcharts.charts.forEach(function (chart) {
                                                if (chart) {
                                                    chart.reflow();
                                                }
                                            });
                                        }, 250);
                                    }
                                }
                            }]
                        }]
                    }],
                    listeners: {
                        'activate': function (container) {
                            if (Ung.dashboard.reportEntriesModified || Ung.dashboard.timeZoneChanged) {
                                if (Ung.dashboard.timeZoneChanged) {
                                    try {
                                        rpc.timeZoneOffset = rpc.systemManager.getTimeZoneOffset();
                                    } catch (e) {
                                        Ung.Util.rpcExHandler(e);
                                    }
                                    Highcharts.setOptions({
                                        global: {
                                            timezoneOffset: -(rpc.timeZoneOffset / 60000)
                                        }
                                    });
                                }

                                Ung.dashboard.resetReports();
                                Ung.dashboard.loadDashboard();
                                Ung.dashboard.reportEntriesModified = false;
                                Ung.dashboard.timeZoneChanged = false;
                            }
                            Ung.dashboard.Queue.resume();
                        },
                        "deactivate": function (container) {
                            Ung.dashboard.Queue.pause();
                        },
                        scope: this
                    }
                }, {
                    xtype: 'container',
                    itemId: 'apps',
                    cls: 'apps',
                    layout: 'border',
                    //scrollable: true,
                    items: [{
                        xtype: 'container',
                        cls: 'top-container',
                        region: 'north',
                        layout: {type: 'hbox', align: 'middle'},
                        height: 44,
                        items: [{
                            xtype: 'button',
                            margin: '0 0 0 10',
                            scale: 'medium',
                            text: '',
                            textAlign: 'left',
                            name: 'policySelector',
                            menu: Ext.create('Ext.menu.Menu', {
                                hideDelay: 0,
                                plain: true,
                                shadow: false,
                                cls: 'policy-menu-dd',
                                items: []
                            })
                        }, {
                            xtype: 'component',
                            itemId: 'parentPolicy',
                            margin: '0 0 0 10',
                            hidden: true,
                            html: '',
                            style: {
                                color: '#FFF',
                                lineHeight: '22px'
                            }
                        }, {
                            xtype: 'button',
                            margin: '0 0 0 10',
                            scale: 'medium',
                            cls: 'action-button material-button',
                            plugins: 'responsive',
                            responsiveConfig: {
                                'width <= 520': {
                                    text: '<i class="material-icons">get_app</i>'
                                },
                                'width > 520': {
                                    text: '<i class="material-icons">get_app</i> <span>' + i18n._("Install Apps") + '</span>'
                                }
                            },
                            handler: function () {
                                Ung.Main.buildApps(); // when moving to App, rebuild them
                                Ung.Main.openInstallApps();
                            }
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
                        }, {
                            xtype: 'button',
                            cls: 'action-button material-button',
                            text: '<span>' + i18n._("Sessions") + '</span>',
                            height: 30,
                            margin: '0 5 0 0',
                            handler: Ung.Main.showSessions,
                            plugins: 'responsive',
                            responsiveConfig: {
                                'width <= 550': {
                                    hidden: true
                                },
                                'width > 550': {
                                    hidden: false
                                }
                            }
                        }, {
                            xtype: 'button',
                            cls: 'action-button material-button',
                            text: '<span>' + i18n._("Hosts") + '</span>',
                            height: 30,
                            margin: '0 5 0 0',
                            handler: Ung.Main.showHosts,
                            plugins: 'responsive',
                            responsiveConfig: {
                                'width <= 550': {
                                    hidden: true
                                },
                                'width > 550': {
                                    hidden: false
                                }
                            }
                        }, {
                            xtype: 'button',
                            cls: 'action-button material-button',
                            text: '<span>' + i18n._("Devices") + '</span>',
                            height: 30,
                            margin: '0 10 0 0',
                            handler: Ung.Main.showDevices,
                            plugins: 'responsive',
                            responsiveConfig: {
                                'width <= 550': {
                                    hidden: true
                                },
                                'width > 550': {
                                    hidden: false
                                }
                            }
                        }, {
                            xtype: 'button',
                            cls: 'action-button material-button',
                            text: '<span>' + i18n._("Users") + '</span>',
                            height: 30,
                            margin: '0 10 0 0',
                            handler: Ung.Main.showUsers,
                            plugins: 'responsive',
                            responsiveConfig: {
                                'width <= 550': {
                                    hidden: true
                                },
                                'width > 550': {
                                    hidden: false
                                }
                            }
                        }]
                    }, {
                        xtype: 'container',
                        cls: 'apps-content',
                        scrollable: true,
                        region: 'center',
                        // IIFE hack for material skin
                        layout: (function () {
                            if (rpc.skinSettings.skinName !== 'simple-gray' && rpc.skinSettings.skinName !== 'simple-triton') {
                                return {
                                    type: 'vbox',
                                    align: 'middle',
                                    pack: 'start'
                                };
                            }
                            return {};
                        }()),
                        items: [{
                            xtype: 'container',
                            cls: 'apps-top',
                            width: 785,
                            height: 50,
                            items: [this.systemStats = Ext.create('Ung.SystemStats', {})]
                        }, {
                            xtype: 'container',
                            itemId: 'filterApps',
                            cls: 'apps-apps'
                        }, {
                            xtype: 'component',
                            cls: 'apps-separator top-title',
                            itemId: 'servicesSeparator',
                            html: i18n._("Service Apps")
                        }, {
                            xtype: 'container',
                            itemId: 'serviceApps',
                            cls: 'apps-apps'
                        }]
                    }]
                }, {
                    xtype: 'container',
                    itemId: 'installApps',
                    layout: 'border',
                    //layout: {type: 'vbox', align: 'stretch'},
                    //scrollable: true,
                    items: [{
                        xtype: 'container',
                        cls: 'top-container',
                        region: 'north',
                        layout: {type: 'hbox', align: 'middle'},
                        height: 44,
                        items: [{
                            xtype: 'button',
                            margin: '0 0 0 10',
                            scale: 'medium',
                            cls: 'action-button material-button',
                            text: '<i class="material-icons">check</i> <span>' + i18n._("Done") + '</span>',
                            handler: function () {
                                this.panelCenter.setActiveItem("apps");
                            },
                            scope: this
                        }, {
                            xtype: 'component',
                            html: '',
                            flex: 1
                        }]
                    }, {
                        xtype: 'container',
                        region: 'center',
                        layout: {type: 'vbox', align: 'stretch'},
                        scrollable: true,
                        items: [{
                            xtype: 'container',
                            items: [{
                                xtype: 'component',
                                cls: 'config-separator top-title',
                                html: '<i class="material-icons">apps</i> <span>' + i18n._("Apps") + '</span>'
                            }, {
                                xtype: 'container',
                                itemId: 'appsContainer',
                                plugins: 'responsive',
                                baseCls: 'install',
                                responsiveConfig: {
                                    'width <= 550': {
                                        style: {
                                            padding: '0'
                                        }
                                    },
                                    'width > 550': {
                                        style: {
                                            padding: '10px'
                                        }
                                    }
                                }
                            }, {
                                xtype: 'component',
                                cls: 'config-separator top-title',
                                html: '<i class="material-icons">build</i> <span>' + i18n._("Service Apps") + '</span>'
                            }, {
                                xtype: 'container',
                                itemId: 'servicesContainer',
                                plugins: 'responsive',
                                baseCls: 'install',
                                responsiveConfig: {
                                    'width <= 550': {
                                        style: {
                                            padding: '0'
                                        }
                                    },
                                    'width > 550': {
                                        style: {
                                            padding: '10px'
                                        }
                                    }
                                }
                            }]
                        }]
                    }]
                }, {
                    xtype: 'container',
                    itemId: 'config',
                    layout: {type: 'vbox', align: 'stretch'},
                    scrollable: true,
                    items: [{
                        xtype: 'container',
                        items: [{
                            xtype: 'component',
                            cls: 'config-separator top-title',
                            html: '<i class="material-icons">tune</i> <span>' + i18n._("Configuration") + '</span>'
                        }, {
                            xtype: 'container',
                            itemId: 'configContainer',
                            baseCls: 'install',
                            plugins: 'responsive',
                            responsiveConfig: {
                                'width <= 550': {
                                    style: {
                                        padding: '0'
                                    }
                                },
                                'width > 550': {
                                    style: {
                                        padding: '10px'
                                    }
                                }
                            }
                        }, {
                            xtype: 'component',
                            cls: 'config-separator top-title',
                            html: '<i class="material-icons">build</i> <span>' + i18n._("Tools") + '</span>'
                        }, {
                            xtype: 'container',
                            title: i18n._('Tools'),
                            itemId: "toolsContainer",
                            baseCls: 'install',
                            plugins: 'responsive',
                            responsiveConfig: {
                                'width <= 550': {
                                    style: {
                                        padding: '0'
                                    }
                                },
                                'width > 550': {
                                    style: {
                                        padding: '10px'
                                    }
                                }
                            }
                        }]
                    }]
                }, {
                    xtype: 'container',
                    layout: "fit",
                    itemId: 'reports',
                    items: [],
                    listeners: {
                        'activate': function (container) {
                            this.viewport.down("#reports").removeAll();
                            this.viewport.down("#reports").add(Ext.create("Ung.panel.Reports", {
                                initEntry: this.reportsInitEntry
                            }));
                        },
                        "deactivate": function (container) {
                            this.reportsInitEntry = null;
                            this.viewport.down("#reports").removeAll();
                        },
                        scope: this
                    }
                }]
            }]
        });
        Ext.QuickTips.init();
        this.mainMenu = this.viewport.down("[name=mainMenu]");
        this.viewsMenu = this.viewport.down("#viewsMenu");
        this.menuWidth = this.mainMenu.getWidth();
        this.panelCenter = this.viewport.down("#panelCenter");
        this.policySelector =  this.viewport.down("button[name=policySelector]");
        this.filterApps = this.viewport.down("#filterApps");
        this.serviceApps = this.viewport.down("#serviceApps");
        this.parentPolicy = this.viewport.down("#parentPolicy");
        this.servicesSeparator = this.viewport.down("#servicesSeparator");
        this.appsPanel = this.viewport.down("#apps");
        this.appsContainer = this.viewport.down("#appsContainer");
        this.servicesContainer = this.viewport.down("#servicesContainer");
        this.dashboardManager = this.viewport.down("[name=dashboardManager]");
        Ung.dashboard.dashboardPanel = this.viewport.down("#dashboardItems");
        Ung.dashboard.dashboardContainer = this.viewport.down("#dashboard");

        //this.reportsMenu = this.viewport.down("#reportsMenu");

        Ung.dashboard.loadDashboard();
        this.buildConfig();
        this.loadPolicies();
    },

    reportsMenuHandler: function (btn) {
        if (this.panelCenter.getLayout().getActiveItem().getItemId() !== 'reports') {
            this.panelCenter.setActiveItem('reports');
            this.panelCenter.setLoading('Loading ...');
            this.viewsMenu.items.each(function (button) {
                button.setPressed(false);
            });
            btn.setPressed(true);
        } else {
            this.panelCenter.down('[name=panelReports]').categoryList.getSelectionModel().select(0);
        }
    },
    about: function (forceReload) {
        if (rpc.about === undefined) {
            var query = "";
            query = query + "uid=" + rpc.serverUID;
            query = query + "&" + "version=" + rpc.fullVersion;
            query = query + "&" + "webui=true";
            query = query + "&" + "lang=" + rpc.languageSettings.language;
            query = query + "&" + "applianceModel=" + rpc.applianceModel;

            rpc.about = query;
        }
        return rpc.about;
    },
    openLegal: function () {
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
    getHelpLink: function (topic) {
        return rpc.helpUrl + "?" + "source=" + topic + "&" + this.about();
    },
    openHelp: function (topic) {
        var url = Ung.Main.getHelpLink(topic);
        window.open(Ung.Main.getHelpLink(topic)); // open a new window
        console.log("Help link:", url);
    },
    openSupportScreen: function () {
        var url = Ung.Main.storeUrl + '?action=support&' + this.about();
        window.open(url); // open a new window
        Ung.LicenseLoader.check();
    },
    openFailureScreen: function () {
        Ext.require(['Webui.config.offline'], function () {
            Webui.config.offlineWin = Ext.create('Webui.config.offline', {});
            Webui.config.offlineWin.show();
        }, this);
    },
    openRegistrationScreen: function () {
        Ext.require(['Webui.config.accountRegistration'], function () {
            Webui.config.accountRegistrationWin = Ext.create('Webui.config.accountRegistration', {});
            Webui.config.accountRegistrationWin.show();
        }, this);
    },
    getMyAccountLink: function () {
        return Ung.Main.storeUrl + '?action=my_account&' + this.about();
    },
    openLibItemStore: function (libItemName) {
        var url = Ung.Main.storeUrl + '?action=buy&libitem=' + libItemName + '&' + this.about();
        window.open(url);
        console.log("Open Url   :", url);
        Ung.LicenseLoader.check();

    },
    openSetupWizardScreen: function () {
        var url = "/setup";
        window.open(url);
    },
    openReports: function (entry) {
        this.reportsInitEntry = entry;
        if (rpc.reportsEnabled) {
            Ung.Main.panelCenter.setActiveItem("reports");
            Ung.Main.panelCenter.setLoading('Loading ...');
            this.viewsMenu.items.each(function (button) { button.setPressed(false); });
            Ext.getCmp('reportsMenuItem').setPressed(true);
        }
    },
    upgrade: function () {
        Ung.MetricManager.stop();

        console.log("Applying Upgrades...");

        Ext.MessageBox.wait({
            title: i18n._("Please wait"),
            msg: i18n._("Applying Upgrades...")
        });

        var doneFn = Ext.bind(function () {
        }, this);

        rpc.systemManager.upgrade(Ext.bind(function (result, exception) {
            // the upgrade will shut down the untangle-vm so often this returns an exception
            // either way show a wait dialog...

            Ext.MessageBox.hide();
            var applyingUpgradesWindow = Ext.create('Ext.window.MessageBox', {
                minProgressWidth: 360
            });

            // the untangle-vm is shutdown, just show a message dialog box for 45 seconds so the user won't poke at things.
            // then refresh browser.
            applyingUpgradesWindow.wait(i18n._("Applying Upgrades..."), i18n._("Please wait"), {
                interval: 500,
                increment: 120,
                duration: 120000,
                scope: this,
                fn: function () {
                    console.log("Upgrade in Progress. Press ok to go to the Start Page...");
                    if (Ung.Main.configWin != null && Ung.Main.configWin.isVisible()) {
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
                        Ung.Util.goToStartPage
                    );
                }
            });
        }, this));
    },

    getNetworkManager: function (forceReload) {
        if (forceReload || rpc.networkManager === undefined) {
            try {
                rpc.networkManager = rpc.jsonrpc.UvmContext.networkManager();
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }
        }
        return rpc.networkManager;
    },

    getLoggingManager: function (forceReload) {
        if (forceReload || rpc.loggingManager === undefined) {
            try {
                rpc.loggingManager = rpc.jsonrpc.UvmContext.loggingManager();
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }
        }
        return rpc.loggingManager;
    },

    getCertificateManager: function (forceReload) {
        if (forceReload || rpc.certificateManager === undefined) {
            try {
                rpc.certificateManager = rpc.jsonrpc.UvmContext.certificateManager();
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }
        }
        return rpc.certificateManager;
    },

    getBrandingManager: function (forceReload) {
        if (forceReload || rpc.brandingManager === undefined) {
            try {
                rpc.brandingManager = rpc.jsonrpc.UvmContext.brandingManager();
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }
        }
        return rpc.brandingManager;
    },

    getOemManager: function (forceReload) {
        if (forceReload || rpc.oemManager === undefined) {
            try {
                rpc.oemManager = rpc.jsonrpc.UvmContext.oemManager();
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }
        }
        return rpc.oemManager;
    },

    getLicenseManager: function (forceReload) {
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
    getExecManager: function (forceReload) {
        if (forceReload || rpc.execManager === undefined) {
            try {
                rpc.execManager = rpc.jsonrpc.UvmContext.execManager();
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }

        }
        return rpc.execManager;
    },
    getLocalDirectory: function (forceReload) {
        if (forceReload || rpc.localDirectory === undefined) {
            try {
                rpc.localDirectory = rpc.jsonrpc.UvmContext.localDirectory();
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }
        }
        return rpc.localDirectory;
    },

    getMailSender: function (forceReload) {
        if (forceReload || rpc.mailSender === undefined) {
            try {
                rpc.mailSender = rpc.jsonrpc.UvmContext.mailSender();
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }
        }
        return rpc.mailSender;
    },
    getNetworkSettings: function (forceReload) {
        if (forceReload || rpc.networkSettings === undefined) {
            try {
                rpc.networkSettings = Ung.Main.getNetworkManager().getNetworkSettings();
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }
        }
        return rpc.networkSettings;
    },
    getReportsManager: function (forceReload) {
        if (forceReload || rpc.reportsManager === undefined) {
            try {
                rpc.reportsManager = this.getAppReports().getReportsManager();
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }
        }
        return rpc.reportsManager;
    },
    getGeographyManager: function (forceReload) {
        if (forceReload || rpc.geographyManager === undefined) {
            try {
                rpc.geographyManager = rpc.jsonrpc.UvmContext.geographyManager();
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }
        }
        return rpc.geographyManager;
    },
    // get app reports
    getAppReports: function (forceReload) {
        if (forceReload || rpc.appReports === undefined) {
            try {
                rpc.appReports = rpc.appManager.app("reports");
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }
        }
        return rpc.appReports;
    },
    updateReportsDependencies: function () {
        Ext.getCmp('reportsMenuItem').setVisible(rpc.reportsEnabled);
        Ung.dashboard.loadDashboard();
    },
    // load policies list
    loadPolicies: function () {
        Ext.MessageBox.wait(i18n._("Loading..."), i18n._("Please wait"));
        if (rpc.policyManager != null) {
            rpc.policyManager.getSettings(Ext.bind(function (result, exception) {
                if (Ung.Util.handleException(exception)) {
                    return;
                }
                rpc.policies = result.policies.list;
                this.buildPolicies();
            }, this));
        } else {
            // no policy manager, just one policy (Default Policy)
            rpc.policies = [{
                javaClass: "com.untangle.app.policy.PolicySettings",
                policyId: "1",
                name: i18n._("Default Policy"),
                description: i18n._("The Default Policy")
            }];
            this.buildPolicies();
        }
    },
    getAppPackageDesc: function (appSettings) {
        var i;
        if (this.myApps !== null) {
            for (i = 0; i < this.myApps.length; i += 1) {
                if (this.myApps[i].name == appSettings.appName) {
                    return this.myApps[i];
                }
            }
        }
        return null;
    },
    createApp: function (appProperties, appSettings, appMetrics, license, runState) {
        var app = {
            appId: appSettings.id,
            appSettings: appSettings,
            type: appProperties.type,
            hasPowerButton: appProperties.hasPowerButton,
            name: appProperties.name,
            displayName: appProperties.displayName,
            license: license,
            image: "/skins/" + rpc.skinSettings.skinName + "/images/admin/apps/" + appProperties.name + "_80x80.png",
            metrics: appMetrics,
            runState: runState,
            viewPosition: appProperties.viewPosition
        };
        return app;
    },
    buildApps: function () {
        var i, instance;
        //destroy Apps
        Ung.Main.appsContainer.removeAll();
        Ung.Main.servicesContainer.removeAll();
        //build Apps
        for (i = 0; i < rpc.rackView.installable.list.length; i += 1) {
            instance = rpc.rackView.installable.list[i];
            if (instance.type === 'FILTER') {
                Ung.Main.appsContainer.add(Ext.create("Ung.AppItem", {appProperties: instance}));
            } else {
                Ung.Main.servicesContainer.add(Ext.create("Ung.AppItem", {appProperties: instance}));
            }
        }
    },
    buildApps2: function () {
        //build apps
        Ung.MetricManager.stop();
        this.policySelector.hide();
        Ext.getCmp('policyManagerToolItem').hide();

        var appPreviews = Ext.clone(this.appPreviews);
        this.filterApps.removeAll();
        this.serviceApps.removeAll();

        this.apps = [];
        var i, app, appName,
            hasService = false, appSettings, appProperties;
        for (i = 0; i < rpc.rackView.instances.list.length; i += 1) {
            appSettings = rpc.rackView.instances.list[i];
            appProperties = rpc.rackView.appProperties.list[i];

            app = this.createApp(appProperties,
                     appSettings,
                     rpc.rackView.appMetrics.map[appSettings.id],
                     rpc.rackView.licenseMap.map[appProperties.name],
                     rpc.rackView.runStates.map[appSettings.id]);
            this.apps.push(app);
            if (!hasService && app.type != "FILTER") {
                hasService = true;
            }
        }

        if (!this.initialized) {
            this.initialized = true;
            if (!rpc.isRegistered) {
                this.showWelcomeScreen();
            }
        }

        this.servicesSeparator.setVisible(hasService);
        if (hasService && !this.appsPanel.hasCls("apps-have-services")) {
            this.appsPanel.addCls("apps-have-services");
        }
        if (!hasService && this.appsPanel.hasCls("apps-have-services")) {
            this.appsPanel.removeCls("apps-have-services");
        }

        this.apps.sort(function (a, b) {
            return a.viewPosition - b.viewPosition;
        });
        for (i = 0; i < this.apps.length; i += 1) {
            app = this.apps[i];
            this.addApp(app, appPreviews[app.name]);
            if (appPreviews[app.name]) {
                delete appPreviews[app.name];
            }
        }
        for (appName in appPreviews) {
            this.addAppPreview(appPreviews[appName]);
        }
        if (!this.disableThreads) {
            Ung.MetricManager.start(true);
        }
        this.openTarget();
        if (Ext.MessageBox.isVisible() && Ext.MessageBox.title == i18n._("Please wait")) {
            Ext.Function.defer(Ext.MessageBox.hide, 30, Ext.MessageBox);
        }
    },
    //TODO: implement routing mechanism available in new extjs, and remove this mechanism
    openTarget: function () {
        if (this.target) {
            //Open target if specified
            //target usage in the query string:
            //config.<configItemName>(.<tabName>(.subtabNane or .buttonName))
            //app.<appName>(.<tabName>(.subtabNane or .buttonName))
            //monitor.[sessions|hosts](.<tabName>)
            //reports.<category>.[report|event].<entryId>
            var targetTokens = this.target.split("."), i, appCmp;
            if (targetTokens.length >= 2) {
                var firstToken = targetTokens[0].toLowerCase();
                if (firstToken == "config") {
                    var configItem = this.configMap[targetTokens[1]];
                    if (configItem) {
                        Ung.Main.openConfig(configItem);
                    }
                } else if (firstToken == "app") {
                    var appName = targetTokens[1].toLowerCase();
                    for (i = 0; i < Ung.Main.apps.length; i += 1) {
                        if (Ung.Main.apps[i].name == appName) {
                            appCmp = Ung.App.getCmp(Ung.Main.apps[i].appId);
                            if (appCmp != null) {
                                appCmp.loadSettings();
                            }
                            break;
                        }
                    }
                } else if (firstToken == "monitor") {
                    var secondToken = targetTokens[1].toLowerCase();
                    if (secondToken == 'sessions') {
                        Ung.Main.showSessions();
                    } else if (secondToken == 'hosts') {
                        Ung.Main.showHosts();
                    }

                } else if (firstToken == "reports") {
                    Ung.Main.openReports(null);
                } else {
                    this.target = null;
                }
            } else {
                this.target = null;
            }
            // remove target in max 10 seconds to prevent using it again
            Ext.Function.defer(function () {
                Ung.Main.target = null;
            }, 10000, this);
        }
    },
    // load the rack view for current policy
    loadAppsView: function () {
        var callback = Ext.bind(function (result, exception) {
            if (Ung.Util.handleException(exception)) {
                return;
            }
            rpc.rackView = result;
            var parentRackName = this.getParentName(rpc.currentPolicy.parentId);
            this.parentPolicy.update((parentRackName == null) ? "" : i18n._("Inherits") + ' <strong>' + parentRackName + '</strong>');
            this.parentPolicy.setVisible(parentRackName != null);
            this.appPreviews = {};
            Ung.Main.buildApps();
            Ung.Main.buildApps2();
        }, this);
        Ung.Util.RetryHandler.retry(rpc.appManager.getAppsView, rpc.appManager, [ rpc.currentPolicy.policyId ], callback, 1500, 10);
    },
    updateAppsView: function () {
        var callback = Ext.bind(function (result, exception) {
            if (Ung.Util.handleException(exception)) {
                return;
            }
            rpc.rackView = result;
            //Ung.Main.buildApps(); - disable app removal from 'Install' view, when install finishes
            Ung.Main.buildApps2();
        }, this);
        Ung.Util.RetryHandler.retry(rpc.appManager.getAppsView, rpc.appManager, [ rpc.currentPolicy.policyId ], callback, 1500, 10);
    },
    reloadLicenses: function () {
        Ung.Main.getLicenseManager().reloadLicenses(Ext.bind(function (result, exception) {
            // do not pop-up license managerexceptions because they happen when offline
            // if(Ung.Util.handleException(exception)) return;
            if (exception) {
                return;
            }

            var callback = Ext.bind(function (result, exception) {
                if (Ung.Util.handleException(exception)) {
                    return;
                }
                rpc.rackView = result;
                var i, appCmp;
                for (i = 0; i < Ung.Main.apps.length; i += 1) {
                    appCmp = Ung.App.getCmp(Ung.Main.apps[i].appId);
                    if (appCmp && appCmp.license) {
                        appCmp.updateLicense(rpc.rackView.licenseMap.map[appCmp.name]);
                    }
                }
            }, this);

            Ung.Util.RetryHandler.retry(rpc.appManager.getAppsView, rpc.appManager, [ rpc.currentPolicy.policyId ], callback, 1500, 10);
        }, this), true);
    },

    installApp: function (appProperties, appItem, completeFn) {
        if (!rpc.isRegistered) {
            Ung.Main.openRegistrationScreen();
            return;
        }
        if (appProperties === null) {
            return;
        }
        // Sanity check to see if the app is already installed.
        var app = Ung.Main.getApp(appProperties.name);
        if ((app !== null) && (app.appSettings.policyId == rpc.currentPolicy.policyId)) {
            appItem.hide();
            return;
        }
        Ung.AppItem.setLoading(appProperties.name, true);
        Ung.Main.addAppPreview(appProperties);

        rpc.appManager.instantiate(Ext.bind(function (result, exception) {
            if (exception) {
                Ung.AppItem.setLoading(appProperties.name, false);
                Ung.Main.updateAppsView();
                Ung.Util.handleException(exception);
                return;
            }
            var app = Ung.AppItem.getApp(appProperties.name);
            if (app) {
                // apply disabled state for the installed app
                app.setDisabled(true);
            }
            Ung.Main.updateAppsView();
            if (completeFn) {
                completeFn();
            }
            if (appProperties.name == "reports") {
                rpc.reportsEnabled = true;
                Ung.Main.updateReportsDependencies();
            } else {
                Ung.dashboard.loadDashboard();
            }
        }, this), appProperties.name, rpc.currentPolicy.policyId);
    },
    openInstallApps: function () {
        Ung.Main.panelCenter.setActiveItem("installApps");
    },
    // build Config
    buildConfig: function () {
        this.config = [{
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
        for (i = 0; i < this.config.length; i += 1) {
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
                displayName: i18n._('Sessions'),
                iconClass: 'icon-config-sessions'
            },
            handler: Ung.Main.showSessions
        }, {
            item: {
                displayName: i18n._('Hosts'),
                iconClass: 'icon-config-hosts'
            },
            handler: Ung.Main.showHosts
        }, {
            item: {
                displayName: i18n._('Devices'),
                iconClass: 'icon-config-devices'
            },
            handler: Ung.Main.showDevices
        }];
        var toolsContainer = this.viewport.down("#toolsContainer");
        for (i = 0; i < tools.length; i += 1) {
            toolsContainer.add(Ext.create('Ung.ConfigItem', tools[i]));
        }

    },
    checkForIE: function (handler) {
        if (Ext.isIE) {
            var noIeContainer = this.viewport.down("#noIeContainer");
            noIeContainer.show();

            this.noIEToolTip = Ext.create('Ext.tip.ToolTip', {
                target: noIeContainer.getEl(),
                dismissDelay: 0,
                hideDelay: 1500,
                width: 500,
                cls: 'extended-stats',
                html: i18n._("For an optimal experience use Google Chrome or Mozilla Firefox.")
            });
        }
        if (Ext.isIE6 || Ext.isIE7 || Ext.isIE8) {
            Ext.MessageBox.alert(i18n._("Warning"),
                                 i18n._("Internet Explorer 8 and prior are not supported for administration.") + "<br/>" +
                                 i18n._("Please upgrade to a newer browser."));
        }
    },
    checkForNotifications: function (handler) {
        //check for upgrades
        rpc.notificationManager.getNotifications(Ext.bind(function (result, exception, opt, handler) {
            var notificationButton = this.viewport.down("#notificationButton");
            var notificationArr = '', i;

            if (result != null && result.list.length > 0) {
                notificationButton.show();
                notificationArr += '<h3>' + i18n._('Notifications:') + '</h3><ul>';
                for (i = 0; i < result.list.length; i += 1) {
                    notificationArr += '<li>' + i18n._(result.list[i]) + '</li>';
                }
                notificationArr += '</ul>';
            } else {
                notificationButton.hide();
            }

            notificationButton.setMenu(Ext.create('Ext.menu.Menu', {
                cls: 'notification-dd',
                plain: true,
                shadow: false,
                width: 250,
                items: [{
                    xtype: 'component',
                    padding: '10',
                    style: {
                        color: '#CCC'
                    },
                    autoEl: {
                        html: notificationArr
                    }
                }, {
                    xtype: 'button',
                    text: '<i class="material-icons" style="font-size: 16px;">help</i> ' + i18n._('Help with Administration Notifications'),
                    margin: '0 10 10 10',
                    textAlign: 'left',
                    handler: function () {
                        Ung.Main.openHelp('admin_notifications');
                    }
                }]
            }));
        }, this, [handler], true));
    },
    openConfig: function (configItem) {
        Ext.MessageBox.wait(i18n._("Loading Config..."), i18n._("Please wait"));
        var createWinFn = function (config) {
            Ung.Main.configWin = Ext.create(config.className, config);
            Ung.Main.configWin.show();
            Ext.MessageBox.hide();
        };
        Ext.Function.defer(function () {
            Ext.require([this.className], function () {
                var configClass = Ext.ClassManager.get(this.className);
                if (configClass != null && Ext.isFunction(configClass.preload)) {
                    configClass.preload(this, createWinFn);
                } else {
                    createWinFn(this);
                }
            }, this);
        }, 10, configItem);
    },
    getAppPosition: function (place, viewPosition) {
        var position = 0;
        if (place.items) {
            place.items.each(function (item, index) {
                if (item.viewPosition < viewPosition) {
                    position = index + 1;
                } else {
                    return false;
                }
            });
        }
        return position;
    },
    addApp: function (app, fadeIn) {
        var appCmp = Ext.create('Ung.App', app);
        appCmp.fadeIn = fadeIn;
        var place = (app.type == "FILTER") ? this.filterApps : this.serviceApps;
        place.add(appCmp);
        Ung.AppItem.setLoading(app.name, false);
        if (app.name == 'policy-manager') {
            // refresh rpc.policyManager to properly handle the case when the policy manager is removed and then re-added to the application list
            rpc.appManager.app(Ext.bind(function (result, exception) {
                if (Ung.Util.handleException(exception)) {
                    return;
                }
                this.policySelector.show();
                Ext.getCmp('policyManagerToolItem').show();
                rpc.policyManager = result;
            }, this), "policy-manager");
        }
    },
    addAppPreview: function (appProperties) {
        var appCmp = Ext.create('Ung.AppPreview', appProperties);
        var place = (appProperties.type == "FILTER") ? this.filterApps : this.serviceApps;
        var position = this.getAppPosition(place, appProperties.viewPosition);
        place.insert(position, appCmp);
    },
    getApp: function (appName) {
        if (Ung.Main.apps) {
            var appPolicyId, i;
            for (i = 0; i < Ung.Main.apps.length; i += 1) {
                appPolicyId = Ung.Main.apps[i].appSettings.policyId;
                if (appName == Ung.Main.apps[i].name && (appPolicyId == null || appPolicyId == rpc.currentPolicy.policyId)) {
                    return Ung.Main.apps[i];
                }
            }
        }
        return null;
    },

    updatePolicySelector: function () {
        var items = [], i, policy;
        var selVirtualRackIndex = 0;
        rpc.policyNamesMap = {};
        rpc.policyNamesMap[0] = i18n._("None");
        for (i = 0; i < rpc.policies.length; i += 1) {
            policy = rpc.policies[i];
            rpc.policyNamesMap[policy.policyId] = policy.name;
            items.push({
                text: policy.name,
                value: policy.policyId,
                index: i,
                handler: Ung.Main.changeRack,
                hideDelay: 0
            });
            if (policy.policyId == 1) {
                rpc.currentPolicy = policy;
                selVirtualRackIndex = i;
            }
        }

        items.push('-');
        items.push({text: i18n._('Show Policy Manager'), value: 'SHOW_POLICY_MANAGER', handler: Ung.Main.showPolicyManager, id: 'policyManagerMenuItem', hideDelay: 0});

        this.policySelector.setText(items[selVirtualRackIndex].text);
        var menu = this.policySelector.down("menu");
        menu.removeAll();
        menu.add(items);

    },
    // build policies select box
    buildPolicies: function () {
        this.updatePolicySelector();
        this.checkForNotifications();
        this.checkForIE();

        Ung.Main.loadAppsView();
    },
    getPolicyName: function (policyId) {
        if (Ext.isEmpty(policyId)) {
            return i18n._("Service Apps");
        }
        if (rpc.policyNamesMap[policyId] !== undefined) {
            return rpc.policyNamesMap[policyId];
        }
        return i18n._("Unknown Rack");
    },
    getCountryList: function() {
        if (Ung.Main.countryList.length != 0) return(Ung.Main.countryList);
        var data = [];
        data.push({ code:"XU", name:i18n._("Unknown") });
        data.push({ code:"XL", name:i18n._("Local") });
        data.push({ code:"AF", name:i18n._("Afghanistan") });
        data.push({ code:"AX", name:i18n._("Aland Islands") });
        data.push({ code:"AL", name:i18n._("Albania") });
        data.push({ code:"DZ", name:i18n._("Algeria") });
        data.push({ code:"AS", name:i18n._("American Samoa") });
        data.push({ code:"AD", name:i18n._("Andorra") });
        data.push({ code:"AO", name:i18n._("Angola") });
        data.push({ code:"AI", name:i18n._("Anguilla") });
        data.push({ code:"AQ", name:i18n._("Antarctica") });
        data.push({ code:"AG", name:i18n._("Antigua and Barbuda") });
        data.push({ code:"AR", name:i18n._("Argentina") });
        data.push({ code:"AM", name:i18n._("Armenia") });
        data.push({ code:"AW", name:i18n._("Aruba") });
        data.push({ code:"AU", name:i18n._("Australia") });
        data.push({ code:"AT", name:i18n._("Austria") });
        data.push({ code:"AZ", name:i18n._("Azerbaijan") });
        data.push({ code:"BS", name:i18n._("Bahamas") });
        data.push({ code:"BH", name:i18n._("Bahrain") });
        data.push({ code:"BD", name:i18n._("Bangladesh") });
        data.push({ code:"BB", name:i18n._("Barbados") });
        data.push({ code:"BY", name:i18n._("Belarus") });
        data.push({ code:"BE", name:i18n._("Belgium") });
        data.push({ code:"BZ", name:i18n._("Belize") });
        data.push({ code:"BJ", name:i18n._("Benin") });
        data.push({ code:"BM", name:i18n._("Bermuda") });
        data.push({ code:"BT", name:i18n._("Bhutan") });
        data.push({ code:"BO", name:i18n._("Bolivia, Plurinational State of") });
        data.push({ code:"BQ", name:i18n._("Bonaire, Sint Eustatius and Saba") });
        data.push({ code:"BA", name:i18n._("Bosnia and Herzegovina") });
        data.push({ code:"BW", name:i18n._("Botswana") });
        data.push({ code:"BV", name:i18n._("Bouvet Island") });
        data.push({ code:"BR", name:i18n._("Brazil") });
        data.push({ code:"IO", name:i18n._("British Indian Ocean Territory") });
        data.push({ code:"BN", name:i18n._("Brunei Darussalam") });
        data.push({ code:"BG", name:i18n._("Bulgaria") });
        data.push({ code:"BF", name:i18n._("Burkina Faso") });
        data.push({ code:"BI", name:i18n._("Burundi") });
        data.push({ code:"KH", name:i18n._("Cambodia") });
        data.push({ code:"CM", name:i18n._("Cameroon") });
        data.push({ code:"CA", name:i18n._("Canada") });
        data.push({ code:"CV", name:i18n._("Cape Verde") });
        data.push({ code:"KY", name:i18n._("Cayman Islands") });
        data.push({ code:"CF", name:i18n._("Central African Republic") });
        data.push({ code:"TD", name:i18n._("Chad") });
        data.push({ code:"CL", name:i18n._("Chile") });
        data.push({ code:"CN", name:i18n._("China") });
        data.push({ code:"CX", name:i18n._("Christmas Island") });
        data.push({ code:"CC", name:i18n._("Cocos (Keeling) Islands") });
        data.push({ code:"CO", name:i18n._("Colombia") });
        data.push({ code:"KM", name:i18n._("Comoros") });
        data.push({ code:"CG", name:i18n._("Congo") });
        data.push({ code:"CD", name:i18n._("Congo, the Democratic Republic of the") });
        data.push({ code:"CK", name:i18n._("Cook Islands") });
        data.push({ code:"CR", name:i18n._("Costa Rica") });
        data.push({ code:"CI", name:i18n._("Cote d'Ivoire") });
        data.push({ code:"HR", name:i18n._("Croatia") });
        data.push({ code:"CU", name:i18n._("Cuba") });
        data.push({ code:"CW", name:i18n._("Curacao") });
        data.push({ code:"CY", name:i18n._("Cyprus") });
        data.push({ code:"CZ", name:i18n._("Czech Republic") });
        data.push({ code:"DK", name:i18n._("Denmark") });
        data.push({ code:"DJ", name:i18n._("Djibouti") });
        data.push({ code:"DM", name:i18n._("Dominica") });
        data.push({ code:"DO", name:i18n._("Dominican Republic") });
        data.push({ code:"EC", name:i18n._("Ecuador") });
        data.push({ code:"EG", name:i18n._("Egypt") });
        data.push({ code:"SV", name:i18n._("El Salvador") });
        data.push({ code:"GQ", name:i18n._("Equatorial Guinea") });
        data.push({ code:"ER", name:i18n._("Eritrea") });
        data.push({ code:"EE", name:i18n._("Estonia") });
        data.push({ code:"ET", name:i18n._("Ethiopia") });
        data.push({ code:"FK", name:i18n._("Falkland Islands (Malvinas)") });
        data.push({ code:"FO", name:i18n._("Faroe Islands") });
        data.push({ code:"FJ", name:i18n._("Fiji") });
        data.push({ code:"FI", name:i18n._("Finland") });
        data.push({ code:"FR", name:i18n._("France") });
        data.push({ code:"GF", name:i18n._("French Guiana") });
        data.push({ code:"PF", name:i18n._("French Polynesia") });
        data.push({ code:"TF", name:i18n._("French Southern Territories") });
        data.push({ code:"GA", name:i18n._("Gabon") });
        data.push({ code:"GM", name:i18n._("Gambia") });
        data.push({ code:"GE", name:i18n._("Georgia") });
        data.push({ code:"DE", name:i18n._("Germany") });
        data.push({ code:"GH", name:i18n._("Ghana") });
        data.push({ code:"GI", name:i18n._("Gibraltar") });
        data.push({ code:"GR", name:i18n._("Greece") });
        data.push({ code:"GL", name:i18n._("Greenland") });
        data.push({ code:"GD", name:i18n._("Grenada") });
        data.push({ code:"GP", name:i18n._("Guadeloupe") });
        data.push({ code:"GU", name:i18n._("Guam") });
        data.push({ code:"GT", name:i18n._("Guatemala") });
        data.push({ code:"GG", name:i18n._("Guernsey") });
        data.push({ code:"GN", name:i18n._("Guinea") });
        data.push({ code:"GW", name:i18n._("Guinea-Bissau") });
        data.push({ code:"GY", name:i18n._("Guyana") });
        data.push({ code:"HT", name:i18n._("Haiti") });
        data.push({ code:"HM", name:i18n._("Heard Island and McDonald Islands") });
        data.push({ code:"VA", name:i18n._("Holy See (Vatican City State)") });
        data.push({ code:"HN", name:i18n._("Honduras") });
        data.push({ code:"HK", name:i18n._("Hong Kong") });
        data.push({ code:"HU", name:i18n._("Hungary") });
        data.push({ code:"IS", name:i18n._("Iceland") });
        data.push({ code:"IN", name:i18n._("India") });
        data.push({ code:"ID", name:i18n._("Indonesia") });
        data.push({ code:"IR", name:i18n._("Iran, Islamic Republic of") });
        data.push({ code:"IQ", name:i18n._("Iraq") });
        data.push({ code:"IE", name:i18n._("Ireland") });
        data.push({ code:"IM", name:i18n._("Isle of Man") });
        data.push({ code:"IL", name:i18n._("Israel") });
        data.push({ code:"IT", name:i18n._("Italy") });
        data.push({ code:"JM", name:i18n._("Jamaica") });
        data.push({ code:"JP", name:i18n._("Japan") });
        data.push({ code:"JE", name:i18n._("Jersey") });
        data.push({ code:"JO", name:i18n._("Jordan") });
        data.push({ code:"KZ", name:i18n._("Kazakhstan") });
        data.push({ code:"KE", name:i18n._("Kenya") });
        data.push({ code:"KI", name:i18n._("Kiribati") });
        data.push({ code:"KP", name:i18n._("Korea, Democratic People's Republic of") });
        data.push({ code:"KR", name:i18n._("Korea, Republic of") });
        data.push({ code:"KW", name:i18n._("Kuwait") });
        data.push({ code:"KG", name:i18n._("Kyrgyzstan") });
        data.push({ code:"LA", name:i18n._("Lao People's Democratic Republic") });
        data.push({ code:"LV", name:i18n._("Latvia") });
        data.push({ code:"LB", name:i18n._("Lebanon") });
        data.push({ code:"LS", name:i18n._("Lesotho") });
        data.push({ code:"LR", name:i18n._("Liberia") });
        data.push({ code:"LY", name:i18n._("Libya") });
        data.push({ code:"LI", name:i18n._("Liechtenstein") });
        data.push({ code:"LT", name:i18n._("Lithuania") });
        data.push({ code:"LU", name:i18n._("Luxembourg") });
        data.push({ code:"MO", name:i18n._("Macao") });
        data.push({ code:"MK", name:i18n._("Macedonia, the Former Yugoslav Republic of") });
        data.push({ code:"MG", name:i18n._("Madagascar") });
        data.push({ code:"MW", name:i18n._("Malawi") });
        data.push({ code:"MY", name:i18n._("Malaysia") });
        data.push({ code:"MV", name:i18n._("Maldives") });
        data.push({ code:"ML", name:i18n._("Mali") });
        data.push({ code:"MT", name:i18n._("Malta") });
        data.push({ code:"MH", name:i18n._("Marshall Islands") });
        data.push({ code:"MQ", name:i18n._("Martinique") });
        data.push({ code:"MR", name:i18n._("Mauritania") });
        data.push({ code:"MU", name:i18n._("Mauritius") });
        data.push({ code:"YT", name:i18n._("Mayotte") });
        data.push({ code:"MX", name:i18n._("Mexico") });
        data.push({ code:"FM", name:i18n._("Micronesia, Federated States of") });
        data.push({ code:"MD", name:i18n._("Moldova, Republic of") });
        data.push({ code:"MC", name:i18n._("Monaco") });
        data.push({ code:"MN", name:i18n._("Mongolia") });
        data.push({ code:"ME", name:i18n._("Montenegro") });
        data.push({ code:"MS", name:i18n._("Montserrat") });
        data.push({ code:"MA", name:i18n._("Morocco") });
        data.push({ code:"MZ", name:i18n._("Mozambique") });
        data.push({ code:"MM", name:i18n._("Myanmar") });
        data.push({ code:"NA", name:i18n._("Namibia") });
        data.push({ code:"NR", name:i18n._("Nauru") });
        data.push({ code:"NP", name:i18n._("Nepal") });
        data.push({ code:"NL", name:i18n._("Netherlands") });
        data.push({ code:"NC", name:i18n._("New Caledonia") });
        data.push({ code:"NZ", name:i18n._("New Zealand") });
        data.push({ code:"NI", name:i18n._("Nicaragua") });
        data.push({ code:"NE", name:i18n._("Niger") });
        data.push({ code:"NG", name:i18n._("Nigeria") });
        data.push({ code:"NU", name:i18n._("Niue") });
        data.push({ code:"NF", name:i18n._("Norfolk Island") });
        data.push({ code:"MP", name:i18n._("Northern Mariana Islands") });
        data.push({ code:"NO", name:i18n._("Norway") });
        data.push({ code:"OM", name:i18n._("Oman") });
        data.push({ code:"PK", name:i18n._("Pakistan") });
        data.push({ code:"PW", name:i18n._("Palau") });
        data.push({ code:"PS", name:i18n._("Palestine, State of") });
        data.push({ code:"PA", name:i18n._("Panama") });
        data.push({ code:"PG", name:i18n._("Papua New Guinea") });
        data.push({ code:"PY", name:i18n._("Paraguay") });
        data.push({ code:"PE", name:i18n._("Peru") });
        data.push({ code:"PH", name:i18n._("Philippines") });
        data.push({ code:"PN", name:i18n._("Pitcairn") });
        data.push({ code:"PL", name:i18n._("Poland") });
        data.push({ code:"PT", name:i18n._("Portugal") });
        data.push({ code:"PR", name:i18n._("Puerto Rico") });
        data.push({ code:"QA", name:i18n._("Qatar") });
        data.push({ code:"RE", name:i18n._("Reunion") });
        data.push({ code:"RO", name:i18n._("Romania") });
        data.push({ code:"RU", name:i18n._("Russian Federation") });
        data.push({ code:"RW", name:i18n._("Rwanda") });
        data.push({ code:"BL", name:i18n._("Saint Barthelemy") });
        data.push({ code:"SH", name:i18n._("Saint Helena, Ascension and Tristan da Cunha") });
        data.push({ code:"KN", name:i18n._("Saint Kitts and Nevis") });
        data.push({ code:"LC", name:i18n._("Saint Lucia") });
        data.push({ code:"MF", name:i18n._("Saint Martin (French part)") });
        data.push({ code:"PM", name:i18n._("Saint Pierre and Miquelon") });
        data.push({ code:"VC", name:i18n._("Saint Vincent and the Grenadines") });
        data.push({ code:"WS", name:i18n._("Samoa") });
        data.push({ code:"SM", name:i18n._("San Marino") });
        data.push({ code:"ST", name:i18n._("Sao Tome and Principe") });
        data.push({ code:"SA", name:i18n._("Saudi Arabia") });
        data.push({ code:"SN", name:i18n._("Senegal") });
        data.push({ code:"RS", name:i18n._("Serbia") });
        data.push({ code:"SC", name:i18n._("Seychelles") });
        data.push({ code:"SL", name:i18n._("Sierra Leone") });
        data.push({ code:"SG", name:i18n._("Singapore") });
        data.push({ code:"SX", name:i18n._("Sint Maarten (Dutch part)") });
        data.push({ code:"SK", name:i18n._("Slovakia") });
        data.push({ code:"SI", name:i18n._("Slovenia") });
        data.push({ code:"SB", name:i18n._("Solomon Islands") });
        data.push({ code:"SO", name:i18n._("Somalia") });
        data.push({ code:"ZA", name:i18n._("South Africa") });
        data.push({ code:"GS", name:i18n._("South Georgia and the South Sandwich Islands") });
        data.push({ code:"SS", name:i18n._("South Sudan") });
        data.push({ code:"ES", name:i18n._("Spain") });
        data.push({ code:"LK", name:i18n._("Sri Lanka") });
        data.push({ code:"SD", name:i18n._("Sudan") });
        data.push({ code:"SR", name:i18n._("Suriname") });
        data.push({ code:"SJ", name:i18n._("Svalbard and Jan Mayen") });
        data.push({ code:"SZ", name:i18n._("Swaziland") });
        data.push({ code:"SE", name:i18n._("Sweden") });
        data.push({ code:"CH", name:i18n._("Switzerland") });
        data.push({ code:"SY", name:i18n._("Syrian Arab Republic") });
        data.push({ code:"TW", name:i18n._("Taiwan, Province of China") });
        data.push({ code:"TJ", name:i18n._("Tajikistan") });
        data.push({ code:"TZ", name:i18n._("Tanzania, United Republic of") });
        data.push({ code:"TH", name:i18n._("Thailand") });
        data.push({ code:"TL", name:i18n._("Timor-Leste") });
        data.push({ code:"TG", name:i18n._("Togo") });
        data.push({ code:"TK", name:i18n._("Tokelau") });
        data.push({ code:"TO", name:i18n._("Tonga") });
        data.push({ code:"TT", name:i18n._("Trinidad and Tobago") });
        data.push({ code:"TN", name:i18n._("Tunisia") });
        data.push({ code:"TR", name:i18n._("Turkey") });
        data.push({ code:"TM", name:i18n._("Turkmenistan") });
        data.push({ code:"TC", name:i18n._("Turks and Caicos Islands") });
        data.push({ code:"TV", name:i18n._("Tuvalu") });
        data.push({ code:"UG", name:i18n._("Uganda") });
        data.push({ code:"UA", name:i18n._("Ukraine") });
        data.push({ code:"AE", name:i18n._("United Arab Emirates") });
        data.push({ code:"GB", name:i18n._("United Kingdom") });
        data.push({ code:"US", name:i18n._("United States") });
        data.push({ code:"UM", name:i18n._("United States Minor Outlying Islands") });
        data.push({ code:"UY", name:i18n._("Uruguay") });
        data.push({ code:"UZ", name:i18n._("Uzbekistan") });
        data.push({ code:"VU", name:i18n._("Vanuatu") });
        data.push({ code:"VE", name:i18n._("Venezuela, Bolivarian Republic of") });
        data.push({ code:"VN", name:i18n._("Viet Nam") });
        data.push({ code:"VG", name:i18n._("Virgin Islands, British") });
        data.push({ code:"VI", name:i18n._("Virgin Islands, U.S.") });
        data.push({ code:"WF", name:i18n._("Wallis and Futuna") });
        data.push({ code:"EH", name:i18n._("Western Sahara") });
        data.push({ code:"YE", name:i18n._("Yemen") });
        data.push({ code:"ZM", name:i18n._("Zambia") });
        data.push({ code:"ZW", name:i18n._("Zimbabwe") });
        Ung.Main.countryList = data;
        return(data);
    },
    getCountryName: function(code) {
        list = Ung.Main.getCountryList();
        for(i = 0;i < list.length;i++) {
            if (list[i].code != code) continue;
            return(list[i].name + " [" + list[i].code + "]");
        }
        return(code);
    },
    showHosts: function () {
        Ext.require(['Webui.config.hostMonitor'], function () {
            Ung.Main.hostMonitorWin = Ext.create('Webui.config.hostMonitor', {});
            Ung.Main.hostMonitorWin.show();
            Ext.MessageBox.wait(i18n._("Loading..."), i18n._("Please wait"));
            Ext.Function.defer(function () {
                Ung.Main.hostMonitorWin.gridCurrentHosts.reload();
                Ext.MessageBox.hide();
            }, 10, this);
        }, this);
    },
    showDevices: function () {
        Ext.require(['Webui.config.deviceMonitor'], function () {
            Ung.Main.deviceMonitorWin = Ext.create('Webui.config.deviceMonitor', {});
            Ung.Main.deviceMonitorWin.show();
        }, this);
    },
    showUsers: function () {
        Ext.require(['Webui.config.userMonitor'], function () {
            Ung.Main.userMonitorWin = Ext.create('Webui.config.userMonitor', {});
            Ung.Main.userMonitorWin.show();
            Ext.MessageBox.wait(i18n._("Loading..."), i18n._("Please wait"));
            Ext.Function.defer(function () {
                Ung.Main.userMonitorWin.gridCurrentUsers.reload();
                Ext.MessageBox.hide();
            }, 10, this);
        }, this);
    },
    showSessions: function () {
        Ung.Main.showAppSessions(0);
    },
    showAppSessions: function (appIdArg) {
        Ext.require(['Webui.config.sessionMonitor'], function () {
            if (Ung.Main.sessionMonitorWin == null) {
                Ung.Main.sessionMonitorWin = Ext.create('Webui.config.sessionMonitor', {});
            }
            Ung.Main.sessionMonitorWin.show();
            Ext.MessageBox.wait(i18n._("Loading..."), i18n._("Please wait"));
            Ext.Function.defer(function () {
                Ung.Main.sessionMonitorWin.gridCurrentSessions.setSelectedApp(appIdArg);
                Ext.MessageBox.hide();
            }, 10, this);
        }, this);
    },
    showPolicyManager: function () {
        var app = Ung.Main.getApp("policy-manager");
        if (app != null) {
            var appCmp = Ung.App.getCmp(app.appId);
            if (appCmp != null) {
                appCmp.loadSettings();
            }
        }
    },
    showDashboardManager: function () {
        Ext.require(['Webui.config.dashboardManager'], function () {
            Ung.Main.dashboardManagerWin = Ext.create('Webui.config.dashboardManager', {});
            Ung.Main.dashboardManagerWin.show();
        }, this);
    },
    // change current policy
    changeRack: function () {
        Ung.Main.policySelector.setText(this.text);
        rpc.currentPolicy = rpc.policies[this.index];
        Ung.Main.loadAppsView();
    },
    getParentName: function (parentId) {
        if (parentId == null || rpc.policies === null) {
            return null;
        }
        var c;
        for (c = 0; c < rpc.policies.length; c += 1) {
            if (rpc.policies[c].policyId == parentId) {
                return rpc.policies[c].name;
            }
        }
        return null;
    },
    // Prepares the uvm to display the welcome screen
    showWelcomeScreen: function () {
        //Test if box is online (store is available)
        Ext.MessageBox.wait(i18n._("Determining Connectivity..."), i18n._("Please wait"));

        rpc.jsonrpc.UvmContext.isStoreAvailable(Ext.bind(function (result, exception) {
            if (Ung.Util.handleException(exception)) {
                return;
            }
            Ext.MessageBox.hide();
            // If box is not online - show error message.
            // Otherwise show registration screen
            if (!result) {
                Ung.Main.openFailureScreen();
            } else {
                Ung.Main.openRegistrationScreen();
            }
        }, this));
    },
    isGoogleDriveConfigured: function () {
        var googleDriveConfigured = false, directoryConnectorLicense, directoryConnectorApp, googleManager;
        try {
            directoryConnectorLicense = Ung.Main.getLicenseManager().isLicenseValid("directory-connector");
            directoryConnectorApp = rpc.appManager.app("directory-connector");
            if (directoryConnectorLicense && directoryConnectorApp) {
                googleManager = directoryConnectorApp.getGoogleManager();
                if (googleManager && googleManager.isGoogleDriveConnected()) {
                    googleDriveConfigured = true;
                }
            }
        } catch (e) {
            Ung.Util.rpcExHandler(e);
        }
        return googleDriveConfigured;
    },
    configureGoogleDrive: function () {
        var app = Ung.Main.getApp("directory-connector");
        if (app != null) {
            var appCmp = Ung.App.getCmp(app.appId);
            if (appCmp != null) {
                Ung.Main.target = "app.directory-connector.Google Connector";
                appCmp.loadSettings();
            }
        } else {
            Ext.MessageBox.alert(i18n._("Error"), i18n._("Google Drive requires Directory Connector application."));
        }
    },

/*
    testInstallRandom: function(probability) {
        if(!probability) {
            probability = Math.random()*100;
        }
        var currentApps = Ung.Main.appsContainer.items.getRange();
        for(var i=0;i<currentApps.length;i++) {
            var app = Ung.AppItem.getApp(currentApps[i].appProperties.name);
            if ( app ) {
                if(Math.random()*100 < probability)
                app.installApp();
            }
        }
    },
    testUninstallAll: function() {
        for(var i=0;i<Ung.Main.apps.length;i++) {
            var app = Ung.App.getCmp(Ung.Main.apps[i].appId);
            app.removeAction();
        }
    },
*/
    showPostRegistrationPopup: function () {
        if (this.apps.length != 0) {
            // do not show anything if apps already installed
            return;
        }

        var popup = Ext.create('Ext.window.MessageBox', {
            buttons: [{
                name: 'Yes',
                text: i18n._("Yes, install the recommended apps."),
                handler: Ext.bind(function () {
                    var apps = [
                        { displayName: "Web Filter", name: 'web-filter'},
                        //{ displayName: "Virus Blocker", name: 'virus-blocker'},
                        //{ displayName: "Virus Blocker Lite", name: 'virus-blocker-lite'},
                        //{ displayName: "Spam Blocker", name: 'spam-blocker'},
                        //{ displayName: "Spam Blocker Lite", name: 'spam-blocker-lite'},
                        //{ displayName: "Phish Blocker", name: 'phish-blocker'},
                        //{ displayName: "Web Cache", name: 'web-cache'},
                        { displayName: "Bandwidth Control", name: 'bandwidth-control'},
                        { displayName: "SSL Inspector", name: 'ssl'},
                        { displayName: "Application Control", name: 'application-control'},
                        //{ displayName: "Application Control Lite", name: 'application-control-lite'},
                        { displayName: "Captive Portal", name: 'captive-portal'},
                        { displayName: "Firewall", name: 'firewall'},
                        //{ displayName: "Intrusion Prevention", name: 'intrusion-prevention'},
                        //{ displayName: "Ad Blocker", name: 'ad-blocker'},
                        { displayName: "Reports", name: 'reports'},
                        { displayName: "Policy Manager", name: 'policy-manager'},
                        { displayName: "Directory Connector", name: 'directory-connector'},
                        //{ displayName: "WAN Failover", name: 'wan-failover'},
                        //{ displayName: "WAN Balancer", name: 'wan-balancer'},
                        { displayName: "IPsec VPN", name: 'ipsec-vpn'},
                        { displayName: "OpenVPN", name: 'openvpn'},
                        { displayName: "Configuration Backup", name: 'configuration-backup'},
                        { displayName: "Branding Manager", name: 'branding-manager'},
                        { displayName: "Live Support", name: 'live-support'}];

                    // only install WAN failover/balancer apps if more than 2 interfaces
                    try {
                        var networkSettings = Ung.Main.getNetworkSettings();
                        if (networkSettings.interfaces.list.length > 2) {
                            apps.push({ displayName: "WAN Failover", name: 'wan-failover'});
                            apps.push({ displayName: "WAN Balancer", name: 'wan-balancer'});
                        }
                    } catch (e) {}

                    // only install this on 1gig+ machines
                    if (Ung.Main.totalMemoryMb > 900) {
                        apps.splice(2, 0, { displayName: "Phish Blocker", name: 'phish-blocker'});
                        apps.splice(2, 0, { displayName: "Spam Blocker", name: 'spam-blocker'});
                        apps.splice(2, 0, { displayName: "Virus Blocker Lite", name: 'virus-blocker-lite'});
                        apps.splice(2, 0, { displayName: "Virus Blocker", name: 'virus-blocker'});
                    }

                    var fn = function (appsToInstall) {
                        // if there are no more apps left to install we are done
                        if (appsToInstall.length == 0) {
                            Ext.MessageBox.alert(i18n._("Installation Complete!"),
                                                 i18n._("The recommended applications have successfully been installed.")  + "<br/><br/>" +
                                                 i18n._("Thank you for using Untangle!"),
                                                 function () {
                                    Ung.Main.panelCenter.setActiveItem("apps"); // go to the apps tab
                                });
                            return;
                        }
                        var name = appsToInstall[0].name;
                        appsToInstall.shift();
                        var completeFn = Ext.bind(fn, this, [appsToInstall]); // function to install remaining apps
                        var app = Ung.AppItem.getApp(name);
                        if (app) {
                            app.installApp(completeFn);
                        } else {
                            completeFn();
                        }
                    };
                    fn(apps);
                    popup.close();
                }, this)
            }, {
                name: 'No',
                text: i18n._("No, I will install the apps manually."),
                handler: Ext.bind(function () {
                    popup.close();
                    Ung.Main.panelCenter.setActiveItem("installApps"); // go to the install apps tab
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
