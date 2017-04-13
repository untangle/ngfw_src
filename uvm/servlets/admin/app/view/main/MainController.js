Ext.define('Ung.view.main.MainController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.main',

    control: {
        '#': {
            beforerender: 'onBeforeRender'
        }
    },

    listen: {
        global: {
            init: 'checkNotifications'
        }
    },

    checkNotifications: function () {
        var me = this;
        Rpc.asyncData('rpc.notificationManager.getNotifications')
            .then(function (result) {
                var btn = me.getView().down('#notificationBtn'), notificationArr = '', i;

                if (result != null && result.list.length > 0) {
                    btn.show();
                    notificationArr += '<h3>' + 'Notifications:'.t() + '</h3><ul>';
                    for (i = 0; i < result.list.length; i += 1) {
                        notificationArr += '<li>' + result.list[i] + '</li>';
                    }
                    notificationArr += '</ul>';
                    btn.setText(result.list.length);
                } else {
                    btn.hide();
                    return;
                }

                btn.setMenu({
                    cls: 'notification-menu',
                    plain: true,
                    shadow: false,
                    width: 300,
                    items: [{
                        xtype: 'component',
                        padding: '20',
                        style: {
                            color: '#CCC'
                        },
                        autoEl: {
                            html: notificationArr
                        }
                    }, {
                        xtype: 'button',
                        iconCls: 'fa fa-question-circle',
                        text: 'Help with Administration Notifications'.t(),
                        margin: '0 20 20 20',
                        href: rpc.helpUrl + '?source=admin_notifications' + '&' + Util.getAbout(),
                        hrefTarget: '_blank'
                    }]
                });
            }, function (ex) {
                Util.exceptionToast(ex);
            });
    },

    init: function (view) {
        var vm = view.getViewModel();
        // //view.getViewModel().set('widgets', Ext.getStore('widgets'));
        vm.set('reports', Ext.getStore('reports'));
        vm.set('policyId', 1);
    },

    onBeforeRender: function(view) {
        var vm = view.getViewModel();

        // vm.bind('{reportsEnabled}', function(enabled) {
        //     if (enabled) {
        //         view.down('#main').insert(3, {
        //             xtype: 'ung.reports',
        //             itemId: 'reports'
        //         });
        //     } else {
        //         view.down('#main').remove('reports');
        //     }
        // });

        vm.set('reportsInstalled', rpc.appManager.app('reports') !== null);
        if (rpc.appManager.app('reports')) {
            vm.set('reportsRunning', rpc.appManager.app('reports').getRunState() === 'RUNNING');
        }
        vm.notify();
    }
});
