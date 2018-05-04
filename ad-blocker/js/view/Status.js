Ext.define('Ung.apps.ad-blocker.view.Status', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-ad-blocker-status',
    itemId: 'status',
    title: 'Status'.t(),
    scrollable: true,

    viewModel: {
        formulas: {
            statistics: function (get) {
                return [{
                    name: 'Total Filters Available'.t(),
                    value: get('settings.rules.list').length + get('settings.userRules.list').length
                }, {
                    name: 'Total Filters Enabled'.t(),
                    value: Ext.Array.filter(get('settings.rules.list'), function (r) { return r.enabled; }).length +
                        Ext.Array.filter(get('settings.userRules.list'), function (r) { return r.enabled; }).length
                }, {
                    name: 'Total Cookie Rules Available'.t(),
                    value: get('settings.cookies.list').length + get('settings.userCookies.list').length
                }, {
                    name: 'Total Cookie Rules Enabled'.t(),
                    value: Ext.Array.filter(get('settings.cookies.list'), function (c) { return c.enabled; }).length +
                        Ext.Array.filter(get('settings.userCookies.list'), function (c) { return c.enabled; }).length
                }];
            }
        }
    },

    layout: 'border',
    items: [{
        region: 'center',
        border: false,
        bodyPadding: 10,
        scrollable: 'y',
        items: [{
            xtype: 'component',
            cls: 'app-desc',
            html: '<img src="/icons/apps/ad-blocker.svg" width="80" height="80"/>' +
                '<h3>Ad Blocker</h3>' +
                '<p>' + 'Ad Blocker blocks advertising content and tracking cookies for scanned web traffic.'.t() + '</p>'
        }, {
            xtype: 'applicense',
            hidden: true,
            bind: {
                hidden: '{!license || !license.trial}'
            }
        }, {
            xtype: 'appstate',
        }, {
            xtype: 'appreports'
        }]
    }, {
        region: 'west',
        border: false,
        width: Math.ceil(Ext.getBody().getViewSize().width / 4),
        split: true,
        items: [{
            xtype: 'appsessions',
            region: 'north',
            height: 200,
            split: true,
        }, {
            xtype: 'propertygrid',
            region: 'center',
            title: 'Statistics'.t(),
            border: false,
            nameColumnWidth: 250,
            sortableColumns: false,
            disabled: true,
            hideHeaders: true,
            bind: {
                disabled: '{!state.on}',
                store: { data: '{statistics}' }
            },
            listeners: {
                beforeedit: function () {
                    return false;
                }
            }
        }, {
            xtype: 'appmetrics',
            region: 'south',
            split: true,
            height: '40%'
        }],
        bbar: [{
            xtype: 'appremove',
            width: '100%'
        }]
    }]
});
