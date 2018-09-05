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
        responsiveConfig: { 'width >= 1000': { hidden: false, }, 'width < 1000': { hidden: true } }
    },
    items: [{
        html: '<img src="' + '/images/BrandingLogo.png" style="height: 40px;"/>',
        cls: 'logo',
        href: '#',
        responsiveConfig: null
    }, {
        text: 'Dashboard'.t(),
        iconCls: 'fa fa-home fa-lg',
        handler: 'onDashboard',
        bind: { userCls: '{activeItem === "dashboardMain" ? "pressed" : ""}' }
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
        iconCls: 'fa fa-area-chart fa-lg',
        handler: 'onReports',
        bind: {
            userCls: '{activeItem === "reports" ? "pressed" : ""}',
        }
    }, '->', {
        text: 'Sessions'.t(),
        href: '#sessions',
        iconCls: 'monitor sessions',
        bind: { userCls: '{activeItem === "sessions" ? "pressed" : ""}' }
    }, {
        text: 'Hosts'.t(),
        href: '#hosts',
        iconCls: 'monitor hosts',
        bind: { userCls: '{activeItem === "hosts" ? "pressed" : ""}' }
    }, {
        text: 'Devices'.t(),
        href: '#devices',
        iconCls: 'monitor devices',
        bind: { userCls: '{activeItem === "devices" ? "pressed" : ""}' }
    }, {
        text: 'Users'.t(),
        href: '#users',
        iconCls: 'monitor users',
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
        iconCls: 'fa fa-lightbulb-o fa-lg',
        handler: 'suggestHandler',
        tooltip: 'Suggest Idea'.t(),
        width: 52,
        responsiveConfig: null
    }, {
        iconCls: 'fa fa-question-circle fa-lg',
        handler: 'helpHandler',
        tooltip: 'Help'.t(),
        width: 52,
        responsiveConfig: null
    }, {
        iconCls: 'fa fa-life-ring fa-lg',
        handler: 'supportHandler',
        tooltip: 'Support'.t(),
        width: 52,
        hidden: true,
        bind: {
            hidden: '{!liveSupport}'
        },
        responsiveConfig: null
    }, {
        responsiveConfig: { 'width >= 1000': { iconCls: 'fa fa-user-circle fa-lg' }, 'width < 1000': { iconCls: 'fa fa-bars fa-lg' } },
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
                responsiveConfig: { 'width >= 1000': { hidden: true }, 'width < 1000': { hidden: false } }
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
                text: 'Reports'.t(),
                iconCls: 'fa fa-line-chart',
                href: '#reports'
            }, {
                xtype: 'menuseparator'
            }, {
                text: 'Sessions'.t(),
                iconCls: 'monitor sessions',
                href: '#sessions'
            }, {
                text: 'Hosts'.t(),
                iconCls: 'monitor hosts',
                href: '#hosts'
            }, {
                text: 'Devices'.t(),
                iconCls: 'monitor devices',
                href: '#devices'
            }, {
                text: 'Users'.t(),
                iconCls: 'monitor users',
                href: '#users'
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
