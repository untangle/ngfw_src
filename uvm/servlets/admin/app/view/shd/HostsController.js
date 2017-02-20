Ext.define('Ung.view.shd.HostsController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.hosts',

    control: {
        '#': {
            deactivate: 'onDeactivate'
        },
        '#hostsgrid': {
            beforerender: 'onBeforeRenderHostsGrid'
        }
    },

    onDeactivate: function (view) {
        view.destroy();
    },

    onBeforeRenderHostsGrid: function (grid) {
        // this.getHosts();
    },

    getHosts: function () {
        console.log('get hosts');
        var me = this,
            grid = me.getView().down('#hostsgrid');
        grid.getView().setLoading(true);
        Rpc.asyncData('rpc.hostTable.getHosts')
            .then(function(result) {
                grid.getView().setLoading(false);
                Ext.getStore('hosts').loadData(result.list);
                // grid.getSelectionModel().select(0);
                // grid.getStore().setData(result.list);
            });
    }

});
