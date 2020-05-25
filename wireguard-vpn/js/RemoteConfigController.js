Ext.define('Ung.apps.wireguard-vpn.RemoteconfigController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app-wireguard-vpn-remote-config',

    control: {
        '#': {
            afterrender: 'afterrender'
        }
    },

    afterrender: function(view){
        var me = this,
            vm = me.getViewModel(),
            appManager = view.up('app-wireguard-vpn').appManager,
            publicKey = view.record.get('publicKey'),
            qrcodeImage = view.down('[itemId=qrcode]'),
            configFile = view.down('[itemId=file]');

        view.setLoading(true);
        Ext.Deferred.sequence([
            Rpc.asyncPromise(appManager, 'createRemoteQrCode', publicKey),
            Rpc.asyncPromise(appManager, 'getRemoteConfig', publicKey)
        ], this).then( function(result){
            if(Util.isDestroyed(view)){
                return;
            }

            if(result[0] == ""){
                vm.set('error', true);
            }else{
                vm.set('error', false);
                view.down('[itemId=qrcode]').setSrc('data:image/png;base64,' + result[0]);
                view.down('[itemId=file]').setHtml('<pre>' + result[1] + '</pre>');
            }

            view.setLoading(false);
        },function(ex){
            if(!Util.isDestroyed(view)){
                view.setLoading(false);
            }
            Util.handleException(ex);
        });

        vm.set('type', 'qrcode');
    },

    closeWindow: function (button) {
        button.up('window').close();
    },

});
