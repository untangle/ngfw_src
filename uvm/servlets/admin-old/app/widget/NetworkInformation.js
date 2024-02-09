Ext.define('Ung.widget.NetworkInformation', {
    extend: 'Ext.container.Container',
    alias: 'widget.networkinformationwidget',

    controller: 'widget',

    border: false,
    baseCls: 'widget small',
    layout: {
        type: 'vbox',
        align: 'stretch'
    },

    viewModel: true,

    refreshIntervalSec: 10,

    items: [{
        xtype: 'component',
        cls: 'header',
        itemId: 'header',
        html: '<h1>' + 'Network Information'.t() + '</h1>' +
            '<div class="actions"><a class="action-btn"><i class="fa fa-rotate-left fa-lg" data-action="refresh"></i></a></div>'
    }, {
        xtype: 'container',
        //cls: 'wg-wrapper flex',
        items: [{
            xtype: 'component',
            bind: {
                html: '<div class="info-box" style="border-bottom: 1px #EEE solid;">' +
                '<div class="info-item">' + 'Currently Active'.t() + '<br/><span>{stats.activeHosts}</span></div>' +
                '<div class="info-item">' + 'Maximum Active'.t() + '<br/><span>{stats.maxActiveHosts}</span></div>' +
                '<div class="info-item">' + 'Known Devices'.t() + '<br/><span>{stats.knownDevices}</span></div>' +
                '<div class="info-actions">' +
                '<a class="wg-button" href="#hosts">' + 'Hosts'.t() + '</a>' +
                '<a class="wg-button" href="#devices">' + 'Devices'.t() + '</a>' +
                '</div>' +
                '</div>'
            }
        }, {
            xtype: 'component',
            bind: {
                html: '<div class="info-box">' +
                '<div class="info-item">' + 'Total Sessions'.t() + '<br/><span>{sessions.totalSessions}</span></div>' +
                '<div class="info-item">' + 'Scanned Sessions'.t() + '<br/><span>{sessions.scannedSessions}</span></div>' +
                '<div class="info-item">' + 'Bypassed Sessions'.t() + '<br/><span>{sessions.bypassedSessions}</span></div>' +
                '<div class="info-actions">' +
                '<a class="wg-button" href="#sessions">' + 'Sessions'.t() + '</a> ' +
                '</div>' +
                '</div>'
            }
        }]
    }],

    fetchData: function (cb) {
        var me = this, vm = me.getViewModel();

        if (vm) {
            me.setLoading({ useTargetEl: true });
            Rpc.asyncData('rpc.sessionMonitor.getSessionStats')
                .then(function(result) {
                    vm.set('sessions', result);
                    me.setLoading(false);
                    cb();
                });
        }
    }
});
