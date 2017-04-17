Ext.define('Ung.config.about.MainController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.config-about',

    control: {
        '#': {
            beforerender: 'onBeforeRender'
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