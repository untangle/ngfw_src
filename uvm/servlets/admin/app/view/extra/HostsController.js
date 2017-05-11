Ext.define('Ung.view.extra.HostsController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.hosts',

    control: {
        '#': {
            deactivate: 'onDeactivate'
        },
        '#hostsgrid': {
            afterrender: 'getHosts'
        }
    },

    onDeactivate: function (view) {
        view.destroy();
    },

    setAutoRefresh: function (btn) {
        var me = this,
            vm = this.getViewModel();
        vm.set('autoRefresh', btn.pressed);

        if (btn.pressed) {
            me.getHosts();
            this.refreshInterval = setInterval(function () {
                me.getHosts();
            }, 5000);
        } else {
            clearInterval(this.refreshInterval);
        }

    },

    resetView: function( btn ){
        var grid = this.getView().down('#hostsgrid');
        Ext.state.Manager.clear(grid.stateId);
        grid.reconfigure(null, grid.initialConfig.columns);
    },

    getHosts: function () {
        var me = this,
            grid = me.getView().down('#hostsgrid');
        grid.getView().setLoading(true);
        Rpc.asyncData('rpc.hostTable.getHosts')
            .then(function(result) {
                grid.getView().setLoading(false);
                Ext.getStore('hosts').loadData(result.list);
                grid.getSelectionModel().select(0);
            });
    },

    megaByteRenderer: function(bytes) {
        var units = ['bytes','Kbytes','Mbytes','Gbytes'];
        var units_itr = 0;

        while ((bytes >= 1000 || bytes <= -1000) && units_itr < 3) {
            bytes = bytes/1000;
            units_itr++;
        }

        bytes = Math.round(bytes*100)/100;

        return '' + bytes + ' ' + units[units_itr];
    },

    refillQuota: function (view, rowIndex, colIndex, item, e, record) {
        var me = this;
        Ext.MessageBox.wait('Refilling...'.t(), 'Please wait'.t());
        Rpc.asyncData('rpc.hostTable.refillQuota', record.get('address'))
            .then(function () {
                me.getHosts();
            }, function (ex) {
                Util.handleException(ex);
            }).always(function () {
                Ext.MessageBox.hide();
            });
    },

    dropQuota: function (view, rowIndex, colIndex, item, e, record) {
        var me = this;
        Ext.MessageBox.wait('Removing Quota...'.t(), 'Please wait'.t());
        Rpc.asyncData('rpc.hostTable.removeQuota', record.get('address'))
            .then(function () {
                me.getHosts();
            }, function (ex) {
                Util.handleException(ex);
            }).always(function () {
                Ext.MessageBox.hide();
            });
    }

});
