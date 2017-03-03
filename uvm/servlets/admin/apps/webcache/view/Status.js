Ext.define('Ung.apps.webcache.view.Status', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-webcache-status',
    itemId: 'status',
    title: 'Status'.t(),

    layout: 'border',
    items: [{
        region: 'center',
        border: false,
        bodyPadding: 10,
        scrollable: 'y',
        items: [{
            xtype: 'component',
            cls: 'app-desc',
            html: '<img src="/skins/modern-rack/images/admin/apps/untangle-node-web-cache_80x80.png" width="80" height="80"/>' +
                '<h3>Web Cache</h3>' +
                '<p>' + 'Web Cache stores and serves web content from local cache for increased speed and reduced bandwidth usage.'.t() + '</p>'
        }, {
            xtype: 'appstate',
        }, {
            xtype: 'fieldset',
            title: '<i class="fa fa-eraser"></i> ' + 'Clear Cache'.t(),
            cls: 'app-section',
            padding: 10,
            margin: '20 0',
            disabled: true,
            bind: {
                disabled: '{instance.targetState !== "RUNNING"}'
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
                    }
                }]
            }]
        }, {
            xtype: 'appreports'
        }, {
            xtype: 'appremove'
        }]
    }, {
        region: 'west',
        border: false,
        width: 350,
        minWidth: 300,
        split: true,
        layout: 'border',
        // layout: {
        //     type: 'hbox'
        // },
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
            bind: {
                disabled: '{instance.targetState !== "RUNNING"}',
                source: '{statistics}'
            },
            sourceConfig: {
                hitCount:    { displayName: 'Cache Hit Count'.t() },
                missCount:   { displayName: 'Cache Miss Count'.t() },
                hitBytes:    { displayName: 'Cache Hit Bytes'.t() },
                missBytes:   { displayName: 'Cache Miss Bytes'.t() },
                bypassCount: { displayName: 'User Bypass Count'.t() },
                systemCount: { displayName: 'System Bypass Count'.t() }
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
            height: '40%',
            sourceConfig: {
                // attachments:       { displayName: 'Attachments'.t() }
            },
        }]
    }]

});
