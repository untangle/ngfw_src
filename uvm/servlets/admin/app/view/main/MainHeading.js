Ext.define('Ung.view.main.MainHeading', {
    extend: 'Ext.toolbar.Toolbar',
    alias: 'widget.mainheading',

    ui: 'navigation',
    dock: 'top',
    border: false,

    defaults: {
        xtype: 'button',
        border: false,
        hrefTarget: '_self',
        plugins: 'responsive',
        responsiveConfig: { wide: { hidden: false, }, tall: { hidden: true } }
    },
    items: [{
        html: '<img src="' + '/images/BrandingLogo.png" style="height: 40px;"/>',
        cls: 'logo',
        href: '#',
        responsiveConfig: null
    }, {
        text: 'Dashboard'.t(),
        iconCls: 'fa fa-home fa-lg',
        href: '#',
        bind: { userCls: '{activeItem === "dashboardMain" ? "pressed" : ""}' },
    }, {
        text: 'Apps'.t(),
        iconCls: 'fa fa-th fa-lg',
        bind: { href: '#apps/{policyId}', userCls: '{(activeItem === "apps" || activeItem === "appCard") ? "pressed" : ""}' },
    }, {
        text: 'Config'.t(),
        iconCls: 'fa fa-cog fa-lg',
        href: '#config',
        bind: { userCls: '{(activeItem === "config" || activeItem === "configCard") ? "pressed" : ""}' },
    }, {
        text: 'Reports'.t(),
        iconCls: 'fa fa-line-chart fa-lg',
        href: '#reports',
        hidden: true,
        bind: {
            userCls: '{activeItem === "reports" ? "pressed" : ""}',
            hidden: '{!reportsInstalled || !reportsRunning}'
        }
    }, '->', {
        text: 'Sessions'.t(),
        href: '#sessions',
        bind: { userCls: '{activeItem === "sessions" ? "pressed" : ""}' }
    }, {
        text: 'Hosts'.t(),
        href: '#hosts',
        bind: { userCls: '{activeItem === "hosts" ? "pressed" : ""}' }
    }, {
        text: 'Devices'.t(),
        href: '#devices',
        bind: { userCls: '{activeItem === "devices" ? "pressed" : ""}' }
    }, {
        text: 'Users'.t(),
        href: '#users',
        bind: { userCls: '{activeItem === "users" ? "pressed" : ""}' }
    }, {
        xtype: 'component',
        width: 1,
        height: 40,
        margin: '0 10',
        style: { background: '#555' }
    }, {
        iconCls: 'fa fa-exclamation-triangle fa-lg fa-orange',
        itemId: 'notificationBtn',
        cls: 'notification-btn',
        arrowVisible: false,
        menuAlign: 'tr-br',
        hidden: true,
        responsiveConfig: null
    }, {
        iconCls: 'fa fa-question-circle fa-lg',
        handler: 'helpHandler',
        tooltip: 'Help'.t(),
        width: 52
    }, {
        iconCls: 'fa fa-life-ring fa-lg',
        handler: 'supportHandler',
        tooltip: 'Support'.t(),
        width: 52,
        bind: { hidden: '{!liveSupport}' }
    }, {
        responsiveConfig: { wide: { iconCls: 'fa fa-user-circle fa-lg' }, tall: { iconCls: 'fa fa-bars fa-lg' } },
        width: 52,
        margin: '0 10 0 0',
        arrowVisible: false,
        menu: {
            cls: 'heading-menu',
            minWidth: 200,
            plain: true,
            border: false,
            bodyBorder: false,
            frame: false,
            shadow: false,
            mouseLeaveDelay: 0,
            defaults: {
                border: false,
                plugins: 'responsive',
                responsiveConfig: { wide: { hidden: true }, tall: { hidden: false } }
            },
            items: [{
                text: 'Dashboard'.t(),
                iconCls: 'fa fa-home',
                href: '#'
            }, {
                text: 'Apps'.t(),
                iconCls: 'fa fa-th',
                href: '#apps' // href is not bindable in menu items, so no policy id passed from men uitem
            }, {
                text: 'Config'.t(),
                iconCls: 'fa fa-cog',
                href: '#config'
            }, {
                /**
                 * todo: fix responsive config/binding conflict
                 * binding takes precedence over responsiveConfig, so Reports is visible in menu even if it shouldn't
                 */
                text: 'Reports'.t(),
                iconCls: 'fa fa-line-chart',
                href: '#reports',
                hidden: true,
                bind: {
                    hidden: '{!reportsInstalled || !reportsRunning}'
                }
            }, {
                xtype: 'menuseparator'
            }, {
                text: 'Sessions'.t(),
                href: '#sessions'
            }, {
                text: 'Hosts'.t(),
                href: '#hosts'
            }, {
                text: 'Devices'.t(),
                href: '#devices'
            }, {
                text: 'Users'.t(),
                href: '#users'
            }, {
                xtype: 'menuseparator'
            }, {
                text: 'Help'.t(),
                iconCls: 'fa fa-question-circle',
                handler: 'helpHandler'
            }, {
                text: 'Support'.t(),
                iconCls: 'fa fa-life-ring',
                handler: 'supportHandler',
                bind: {
                    hidden: '{!liveSupport}'
                }
            }, {
                xtype: 'menuseparator'
            }, {
                text: 'Account Settings'.t(),
                iconCls: 'fa fa-user-circle',
                hrefTarget: '_blank',
                href: Util.getStoreUrl() + '?action=my_account&' + Util.getAbout(),
                responsiveConfig: null
            }, {
                text: 'Logout'.t(),
                iconCls: 'fa fa-sign-out',
                href: '/auth/logout?url=/admin&realm=Administrator',
                responsiveConfig: null
            }],
            listeners: {
                click: function (menu, item) {
                    // for touch devices this hack is required
                    if (Ext.supports.Touch) {
                        if (item.hrefTarget === '_blank') {
                            window.open(item.href);
                        } else {
                            document.location.href = item.href;
                        }
                    }
                }
            }
        }
    }]
});
