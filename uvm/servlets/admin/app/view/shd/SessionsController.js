Ext.define('Ung.view.shd.SessionsController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.sessions',

    control: {
        '#': {
            afterrender: 'getSessions'
        },
        '#list': {
            select: 'onSelect'
        },
        'toolbar textfield': {
            change: 'globalFilter'
        }
    },

    refreshInterval: null,

    setAutoRefresh: function (btn) {
        var me = this,
            vm = this.getViewModel();
        vm.set('autoRefresh', btn.pressed);

        console.log(btn.pressed);

        if (btn.pressed) {
            me.getSessions();
            this.refreshInterval = setInterval(function () {
                me.getSessions();
            }, 5000);
        } else {
            clearInterval(this.refreshInterval);
        }

    },

    getSessions: function () {
        console.log('get sessions');
        var me = this,
            grid = me.getView().down('#list');
        grid.getView().setLoading(true);
        rpc.sessionMonitor.getMergedSessions(function (result, exception) {
            grid.getView().setLoading(false);
            if (exception) {
                Ung.Util.exceptionToast(exception);
                return;
            }
            Ext.getStore('sessions').loadData(result.list);
            grid.getSelectionModel().select(0);
            // grid.getStore().setData(result.list);
        });
    },

    onSelect: function (grid, record) {
        var vm = this.getViewModel(),
            props = record.getData();

        delete props._id;
        delete props.javaClass;
        delete props.mark;
        delete props.localAddr;
        delete props.remoteAddr;
        vm.set('selectedSession', props);
    },

    globalFilter: function (field, value) {
        var list = this.getView().down('#list'),
            re = new RegExp(value, 'gi');
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
