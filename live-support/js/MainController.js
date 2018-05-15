Ext.define('Ung.apps.livesupport.MainController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.app-live-support',

    control: {
        '#': {
            afterrender: 'onAfterRender'
        }
    },

    onAfterRender: function(){
        var me = this, v = me.getView(), vm = me.getViewModel();

        // There's nothing to save on this form.
        vm.set('panel.saveDisabled', true);

        v.setLoading(true);
        Ext.Deferred.sequence([
            Rpc.directPromise('rpc.companyName'),
            Rpc.directPromise('rpc.serverUID'),
            Rpc.directPromise('rpc.fullVersionAndRevision')
        ], this)
        .then(function (result) {
            if(Util.isDestroyed(v, vm)){
                return;
            }
            vm.set({
                companyName: result[0],
                serverUID: result[1],
                fullVersionAndRevision: result[2],
            });

            v.setLoading(false);
        });
    },

    supportHandler: function(btn){
        this.getView().up('[itemId=main]').getController().supportHandler(btn);
    }

});
