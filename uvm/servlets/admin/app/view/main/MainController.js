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
            afterlaunch: 'afterLaunch',
            openregister: 'openRegister'
        }
    },

    afterLaunch: function () {
        this.checkRegister();
        this.checkNotifications();
    },

    checkRegister: function () {
        var me = this;
        if(!Rpc.directData('rpc.isRegistered')) {
            Rpc.asyncData('rpc.UvmContext.isStoreAvailable')
            .then( function(result){
                if(Util.isDestroyed(me)){
                    return;
                }
                if (!result) {
                    me.openOffline();
                } else {
                    me.openRegister();
                }
            },function(ex){
                Util.handleException(ex);
            });
        }
    },

    openRegister: function () {
        var regView = Ext.create('Ung.view.main.Registration', {});
        regView.show();
    },

    openOffline: function () {
        var offView = Ext.create('Ung.view.main.Offline', {});
        offView.show();
    },

    checkNotifications: function () {
        var me = this;
        Rpc.asyncData('rpc.notificationManager.getNotifications')
        .then(function (result) {
            if(Util.isDestroyed(me)){
                return;
            }
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
                    href: Rpc.directData('rpc.helpUrl') + '?source=admin_notifications' + '&' + Util.getAbout(),
                    hrefTarget: '_blank'
                }]
            });
        }, function (ex) {
            Util.handleException(ex);
        });
    },




    init: function (view) {
        var vm = view.getViewModel();
        // //view.getViewModel().set('widgets', Ext.getStore('widgets'));
        // vm.set('reports', Ext.getStore('reports'));
        vm.set('policyId', 1);
    },

    onBeforeRender: function(view) {
        this.setLiveSupport();
    },

    setLiveSupport: function() {
        this.getViewModel().set('liveSupport', Rpc.directData('rpc.appManager.app', 'live-support') !== null);
    },

    helpHandler: function (btn) {
        var helpUrl = Rpc.directData('rpc.helpUrl') + '?fragment=' + window.location.hash.substr(1) + '&' + Util.getAbout();
        window.open(helpUrl);
    },

    suggestHandler: function (btn) {
        var suggestUrl = Rpc.directData('rpc.helpUrl') + '?fragment=feedback&' + Util.getAbout();
        window.open(suggestUrl);
    },

    supportHandler: function (btn) {
        var me = this;
        // check here if support is enabled and show modal only if not, otherwise open support window
        var systemSettings = Rpc.directData('rpc.systemManager.getSettings');
        if (systemSettings.cloudEnabled && systemSettings.supportEnabled) {
            me.supportLaunch();
        } else {
            me.getView().add({ xtype: 'support' }).show();
        }
    },

    supportLaunch: function () {
        var supportUrl = Util.getStoreUrl() + '?action=support&' + Util.getAbout() + '&fragment=' + window.location.hash.substr(1) + '&line=ngfw';
        var user = Rpc.directData('rpc.adminManager.getSettings').users.list[0];
        if (user) {
            supportUrl += '&email=' + user.emailAddress;
        }
        window.open(supportUrl);
    },

    onDashboard: function () {
        var me = this, route = 'dashboard',
            vm = me.getView().down('#dashboardMain').getViewModel();
        if (vm.get('query.string')) {
            route += vm.get('query.string').replace('&', '?');
        }
        Ung.app.redirectTo(route);
    },

    onReports: function () {
        var me = this, route = 'reports',
            vm = me.getView().down('#reports').getViewModel();
        if (vm.get('query.string')) {
            route += vm.get('query.string').replace('&', '?');
        }
        Ung.app.redirectTo(route);
    }

});
