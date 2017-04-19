Ext.define('Ung.config.about.MainController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.config-about',

    control: {
        '#': {
            beforerender: 'onBeforeRender',
            afterrender: 'onAfterRender'
        },
        '#licenses': {
            afterrender: 'reloadLicenses'
        }
    },

    onBeforeRender: function (view) {
        try {
            view.getViewModel().set({
                kernelVersion: rpc.adminManager.getKernelVersion(),
                modificationState: rpc.adminManager.getModificationState(),
                rebootCount: rpc.adminManager.getRebootCount(),
                activeSize: rpc.hostTable.getCurrentActiveSize(),
                maxActiveSize: rpc.hostTable.getMaxActiveSize()
            });
        } catch (ex) {

        }
    },

    onAfterRender: function(view){
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
                        if( response!=null &&
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
                vm.set('licenses', result.list);
            });
        });
    }

});

Ext.define('Ung.config.about.cmp.LicenseGridController', {
    extend: 'Ung.cmp.GridController',

    alias: 'controller.unaboutlicensegrid',

    dateRenderer: function(value){
        return Util.timestampFormat(value * 1000);
    }

});