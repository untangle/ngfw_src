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

    onAfterRender: function(){
        var me = this, v = me.getView(), vm = me.getViewModel();

        // There's nothing to save on this form.
        vm.set('panel.saveDisabled', true);

        v.setLoading(true);
        Ext.Deferred.sequence([
            Rpc.asyncPromise('rpc.adminManager.getKernelVersion'),
            Rpc.asyncPromise('rpc.adminManager.getModificationState'),
            Rpc.asyncPromise('rpc.adminManager.getRebootCount'),
            Rpc.asyncPromise('rpc.hostTable.getCurrentActiveSize'),
            Rpc.asyncPromise('rpc.hostTable.getMaxActiveSize'),
            Rpc.directPromise('rpc.fullVersionAndRevision'),
            Rpc.directPromise('rpc.serverUID'),
            Rpc.directPromise('rpc.serverSerialnumber')
        ], this)
        .then(function (result) {
            if(Util.isDestroyed(v, vm)){
                return;
            }
            vm.set({
                kernelVersion: result[0],
                modificationState: result[1],
                rebootCount: result[2],
                activeSize: result[3],
                maxActiveSize: result[4],
                fullVersionAndRevision: result[5],
                serverUID: result[6],
                serialNumber: (result[7] != "") ? '<br/>' + 'Serial Number'.t() + ': ' + result[7] : ""
            });

            var accountComponent = v.down('[itemId=account]');
            if( accountComponent &&
                accountComponent.isHidden() ){
                var serverUID = vm.get('serverUID');
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
            v.setLoading(false);
        });
    },

    reloadLicenses: function () {
        var vm = this.getViewModel();

        Rpc.asyncData('rpc.UvmContext.licenseManager.reloadLicenses', true)
        .then(function(result) {
            Rpc.asyncData('rpc.UvmContext.licenseManager.getLicenses').
            then(function(result) {
                if(Util.isDestroyed(vm)){
                    return;
                }
                vm.set('licenses', result.list);
            });
        });
    }

});
