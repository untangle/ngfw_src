Ext.define('Ung.widget.Notifications', {
    extend: 'Ext.container.Container',
    alias: 'widget.notificationswidget',

    controller: 'widget',
    viewModel: {
        data: {
            count: null
        }
    },
    border: false,
    baseCls: 'widget small info-widget',
    height: 300,

    layout: {
        type: 'vbox',
        align: 'stretch'
    },

    refreshIntervalSec: 0,
    lastFetchTime: null,

    items: [{
        xtype: 'component',
        cls: 'header',
        itemId: 'header',
        bind: {
            html: '<h1>' + 'Notifications'.t() + ' {count}</h1>' +
                '<div class="actions"><a class="action-btn"><i class="fa fa-rotate-left fa-lg" data-action="refresh"></i></a></div>'
        }
    }, {
        xtype: 'component',
        itemId: 'notif',
        scrollable: 'y',
        flex: 1
    }, {
        xtype: 'button',
        margin: '10 20',
        text: 'Help'.t(),
        iconCls: 'fa fa-question-circle',
        href: Rpc.directData('rpc.helpUrl') + '?fragment=' + window.location.hash.substr(1) + '&' + Util.getAbout(),
        hidden: true,
        bind: { hidden: '{!count || count === "(0)"}' }
    }],

    fetchData: function (cb) {
        var me = this, vm = me.getViewModel(), notifCmp = me.down('#notif');
        me.setLoading({ useTargetEl: true });
        Rpc.asyncData('rpc.notificationManager.getNotifications')
            .then(function (result) {
                var notificationArr = '<ul style="margin: 0; padding: 0 10px 30px 30px;">', i;
                if (result != null && result.list.length > 0) {
                    for (i = 0; i < result.list.length; i += 1) {
                        notificationArr += '<li>' + result.list[i] + '</li>';
                    }
                    notificationArr += '</ul>';
                    notifCmp.setHtml(notificationArr);
                    vm.set('count', '(' + result.list.length + ')');
                } else {
                    notifCmp.setHtml('<p style="text-align: center; font-size: 14px;">' + 'No notifications'.t() + '!</p>');
                    vm.set('count', '(0)');
                }
            }, function (ex) {
                Util.handleException(ex);
            })
            .always(function() {
                me.setLoading(false);
                if (cb) cb();
            });
    }
});
