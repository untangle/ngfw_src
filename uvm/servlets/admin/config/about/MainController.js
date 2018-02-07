Ext.define('Ung.config.about.MainController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.config-about',

    control: {
        '#': {
            afterrender: 'onAfterRender'
        },
        '#licenses': {
            afterrender: 'reloadLicenses'
        }
    },

    onAfterRender: function(view){
        var me = this, v = me.getView(), vm = me.getViewModel();

        // There's nothing to save on this form.
        vm.set('panel.saveDisabled', true);

        v.setLoading(true);
        Ext.Deferred.sequence([
            Rpc.directPromise('rpc.adminManager.getKernelVersion'),
            Rpc.directPromise('rpc.adminManager.getModificationState'),
            Rpc.directPromise('rpc.adminManager.getRebootCount'),
            Rpc.directPromise('rpc.hostTable.getCurrentActiveSize'),
            Rpc.directPromise('rpc.hostTable.getMaxActiveSize'),
        ], this).then(function (result) {
            if(Util.isDestroyed(vm)){
                return;
            }
            vm.set({
                kernelVersion: result[0],
                modificationState: result[1],
                rebootCount: result[2],
                activeSize: result[3],
                maxActiveSize: result[4]
            });
        }).always(function() {
            if(Util.isDestroyed(v)){
                return;
            }
            v.setLoading(false);
        });

        var accountComponent = view.down('[itemId=account]');
        if( accountComponent &&
            accountComponent.isHidden() ){
            var serverUID = rpc.serverUID;
            if( serverUID &&
                serverUID.length == 19 ) {
                Ext.data.JsonP.request({
                    url: Util.getStoreUrl() + '?action=find_account&uid=' + serverUID,
                    type: 'GET',
                    success: function(response, opts) {
                        if( !Util.isDestroyed(accountComponent) &&
                            response!=null &&
                            response.account) {
                            accountComponent.setHtml('Account'.t() + ": " + response.account);
                            accountComponent.setVisible(true);
                        }
                    },
                    failure: function(response, opts) {
                        console.log("Failed to get account info fro UID:", serverUID);
                    }
                });
            }
        }
    },

    reloadLicenses: function () {
        var vm = this.getViewModel();

        rpc.licenseManager = rpc.UvmContext.licenseManager();
        Rpc.asyncData('rpc.licenseManager.reloadLicenses', true).then(function(result) {
            Rpc.asyncData('rpc.licenseManager.getLicenses').then(function(result) {
                if(Util.isDestroyed(vm)){
                    return;
                }
                vm.set('licenses', result.list);
            });
        });
    }

});
