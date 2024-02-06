Ext.define('Ung.view.extra.SessionsController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.sessions',

    control: {
        '#': {
            deactivate: 'onDeactivate',
            refresh: 'getSessions'
        },
        '#sessionsgrid': {
            afterrender: 'getSessions'
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
        var grid = this.getView().down('#sessionsgrid'),
            store = grid.getStore();

        Ext.state.Manager.clear(grid.stateId);
        store.getSorters().removeAll();
        store.sort('bypassed', 'ASC');
        store.clearFilter();
        grid.reconfigure(null, grid.initialConfig.columns);
    },

    getSessions: function () {
        var me = this,
            v = me.getView(),
            grid = v.down('#sessionsgrid'),
            filters = grid.getStore().getFilters(),
            store = Ext.getStore('sessions');

        var existingRouteFilter = filters.findBy( function( filter ){
            if(filter.config.source == "route"){
                return true;
            }
        } );
        if( existingRouteFilter != null ){
            filters.remove(existingRouteFilter);
        }
        if( v.routeFilter ){
            filters.add(v.routeFilter);
        }

        if( !store.getFields() ){
            store.setFields(grid.fields);
        }

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

                store.loadData( sessions );

                if(store.getSorters().items.length == 0){
                    store.sort('bypassed', 'ASC');
                }

                grid.getSelectionModel().select(0);
            });
    },

});
