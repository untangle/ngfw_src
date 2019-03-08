Ext.define('Ung.apps.webcache.view.Status', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-web-cache-status',
    itemId: 'status',
    title: 'Status'.t(),
    scrollable: true,

    viewModel: {
        formulas: {
            stats: function (get) {
                return [
                    { name: 'Cache Hit Count'.t(), value: get('statistics.hitCount') },
                    { name: 'Cache Miss Count'.t(), value: get('statistics.missCount') },
                    { name: 'Cache Hit Bytes'.t(), value: get('statistics.hitBytes') },
                    { name: 'Cache Miss Bytes'.t(), value: get('statistics.missBytes') },
                    { name: 'User Bypass Count'.t(), value: get('statistics.bypassCount') },
                    { name: 'System Bypass Count'.t(), value: get('statistics.systemCount') }
                ];
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
            html: '<img src="/icons/apps/web-cache.svg" width="80" height="80"/>' +
                '<h3>Web Cache</h3>' +
                '<p>' + 'Web Cache stores and serves web content from local cache for increased speed and reduced bandwidth usage.'.t() + '</p>'
        }, {
            xtype: 'applicense',
            hidden: true,
            bind: {
                hidden: '{!license || !license.trial}'
            }
        }, {
            xtype: 'appstate',
        }, {
            xtype: 'fieldset',
            title: '<i class="fa fa-eraser"></i> ' + 'Clear Cache'.t(),
            cls: 'app-section',
            padding: 10,
            margin: '20 0',
            disabled: true,
            collapsed: true,
            bind: {
                collapsed: '{!state.on}',
                disabled: '{!state.on}'
            },
            items: [{
                xtype: 'component',
                style: {
                    lineHeight: 1.4
                },
                html: 'If content stored in the cache somehow becomes stale or corrupt, the cache can be cleared with the'.t() +
                    ' <strong>' + 'Clear Cache'.t() + '</strong> ' + 'button.'.t() + '<br/><br/>' +
                    '<i class="fa fa-exclamation-triangle fa-red"></i> ' +
                    '<strong>' + 'Caution'.t() + ':</strong> ' + 'Clearing the cache requires restarting the caching engine.'.t() + '<br/>' +
                    'This will cause active web sessions to be dropped and may disrupt web traffic for several seconds.'.t()
            }, {
                xtype: 'container',
                margin: '20 0 0 0',
                layout: {
                    type: 'hbox'
                },
                items: [{
                    xtype: 'checkbox',
                    reference: 'clearCacheConsent',
                    boxLabel: 'I understand the risks.'.t()
                }, {
                    xtype: 'button',
                    margin: '0 0 0 10',
                    text: 'Clear Cache'.t(),
                    iconCls: 'fa fa-eraser',
                    disabled: true,
                    bind: {
                        disabled: '{!clearCacheConsent.checked}'
                    },
                    handler: 'clearCache'
                }]
            }]
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
            hideHeaders: true,
            disabled: true,
            bind: {
                disabled: '{!state.on}',
                store: { data: '{stats}' }
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
