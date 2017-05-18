Ext.define('Ung.view.extra.SessionsController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.sessions',

    control: {
        '#': {
            deactivate: 'onDeactivate'
        },
        '#sessionsgrid': {
            afterrender: 'getSessions'
        },
        'toolbar textfield': {
            change: 'globalFilter'
        }
    },

    refreshInterval: null,

    onDeactivate: function (view) {
        view.destroy();
    },

    setAutoRefresh: function (btn) {
        var me = this,
            vm = this.getViewModel();
        vm.set('autoRefresh', btn.pressed);

        if (btn.pressed) {
            me.getSessions();
            this.refreshInterval = setInterval(function () {
                me.getSessions();
            }, 5000);
        } else {
            clearInterval(this.refreshInterval);
        }

    },

    resetView: function( btn ){
        var grid = this.getView().down('#sessionsgrid');
        Ext.state.Manager.clear(grid.stateId);
        grid.reconfigure(null, grid.initialConfig.columns);
    },

    getSessions: function () {
        var me = this,
            v = me.getView(),
            grid = v.down('#sessionsgrid');

        grid.getView().setLoading(true);
        Rpc.asyncData('rpc.sessionMonitor.getMergedSessions')
            .then(function(result) {
                grid.getView().setLoading(false);
                var sessions = result.list;

                sessions.forEach( function( session ){
                    var key;
                    if( session.attachments ){
                        for(key in session.attachments.map ){
                            session[key] = session.attachments.map[key];
                        }
                        delete session.attachments;
                    }
                });

                Ext.getStore('sessions').loadData( sessions );

                v.down('ungridstatus').fireEvent('update');

                grid.getSelectionModel().select(0);
            });
    },

    globalFilter: function (field, value) {
        var list = this.getView().down('#sessionsgrid'),
            re = new RegExp(value, 'gi');
        console.log('globalFilter');
        if (value.length > 0) {
            list.getStore().clearFilter();
            list.getStore().filterBy(function (record) {
                return re.test(record.get('protocol')) ||
                       re.test(record.get('preNatClient')) ||
                       re.test(record.get('postNatServer')) ||
                       re.test(record.get('preNatClientPort')) ||
                       re.test(record.get('postNatServerPort'));
            });

            // list.getStore().filter([
            //     { property: 'protocol', value: value }
            // ]);
        } else {
            list.getStore().clearFilter();
        }
        list.getSelectionModel().select(0);
    }

});
