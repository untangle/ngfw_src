Ext.define('Ung.apps.applicationcontrol.view.Status', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-application-control-status',
    itemId: 'status',
    title: 'Status'.t(),
    scrollable: true,

    viewModel: {
        formulas: {
            trafficStats: function (get) {
                return [
                    { name: 'Sessions Scanned'.t(), value: get('statistics.sessionCount') },
                    { name: 'Sessions Allowed'.t(), value: get('statistics.allowedCount') },
                    { name: 'Sessions Flagged'.t(), value: get('statistics.flaggedCount') },
                    { name: 'Sessions Blocked'.t(), value: get('statistics.blockedCount') }
                ];
            },
            applicationStats: function (get) {
                return [
                    { name: 'Known Applications'.t(), value: get('statistics.protoTotalCount') },
                    { name: 'Flagged Applications'.t(), value: get('statistics.protoFlagCount') },
                    { name: 'Blocked Applications'.t(), value: get('statistics.protoBlockCount') },
                    { name: 'Tarpitted Applications'.t(), value: get('statistics.protoTarpitCount') }
                ];
            },
            ruleStats: function (get) {
                return [
                    { name: 'Total Rules'.t(), value: get('statistics.logicTotalCount') },
                    { name: 'Active Rules'.t(), value: get('statistics.logicLiveCount') }
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
            html: '<img src="/icons/apps/application-control.svg" width="80" height="80"/>' +
                '<h3>Application Control</h3>' +
                '<p>' + 'Application Control scans sessions and identifies the associated applications allowing each to be flagged and/or blocked.'.t() + '</p>'
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
            region: 'center',
            title: 'Statistics'.t(),
            xtype: 'tabpanel',
            disabled: true,
            bind: {
                disabled: '{!state.on}'
            },
            defaults: {
                xtype: 'propertygrid',
                border: false,
                nameColumnWidth: 250,
                sortableColumns: false,
                hideHeaders: true,
                listeners: {
                    beforeedit: function () {
                        return false;
                    }
                }
            },
            items: [{
                title: 'Traffic'.t(),
                bind: { store: { data: '{trafficStats}' } }
            }, {
                title: 'Application'.t(),
                bind: { store: { data: '{applicationStats}' } }
            }, {
                title: 'Rule'.t(),
                bind: { store: { data: '{ruleStats}' } }
            }]
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
