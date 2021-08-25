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
            Rpc.directPromise('rpc.serverSerialnumber'),
            Rpc.directPromise('rpc.regionName')
        ], this)
        .then(function (result) {
            if(Util.isDestroyed(v, vm)){
                return;
            }
            var protectedAbout = new Ext.data.ArrayStore({
                fields: ['name', 'value'],
                data: []
            });
            var serverUID = result[6];
            protectedAbout.add({name: 'UID'.t(), value: serverUID});
            if (result[7] !='') {
                protectedAbout.add({name: 'Serial Number'.t(), value: result[7]});
            }
            protectedAbout.commitChanges();

            var publicAbout = new Ext.data.ArrayStore({
                fields: ['name', 'value'],
                data: []
            });
            publicAbout.add({name: 'Build'.t(), value: result[5]});
            publicAbout.add({name: 'Kernel'.t(), value: result[0]});
            publicAbout.add({name: 'Region'.t(), value: result[8]});
            publicAbout.add({name: 'History'.t(), value: result[1]});
            publicAbout.add({name: 'Reboots'.t(), value: result[2]});
            publicAbout.add({name: 'Current active device count'.t(), value: result[3]});
            publicAbout.add({name: 'Highest active device count since reboot'.t(), value: result[4]});

            publicAbout.commitChanges();

            if( serverUID &&
                serverUID.length == 19 ) {
                Ext.data.JsonP.request({
                    url: Util.getStoreUrl() + '?action=find_account&uid=' + serverUID,
                    type: 'GET',
                    success: function(response, opts) {
                        if( response!=null &&
                            response.account) {
                            protectedAbout.add({name: 'Account'.t(), value: response.account});
                            protectedAbout.commitChanges();
                        }
                    },
                    failure: function(response, opts) {
                        console.log("Failed to get account info from UID:", serverUID);
                    }
                });
            }

            vm.set({
                protectedAbout: protectedAbout,
                publicAbout: publicAbout
            });
            v.setLoading(false);
        });
    },

    reloadLicenses: function () {
        var vm = this.getViewModel(), v = this.getView();

        v.setLoading(true);
        Ung.util.Util.reloadLicenses();
        v.setLoading(false);

        Rpc.asyncData('rpc.UvmContext.licenseManager.getLicenses').
        then(function(result) {
            if(Util.isDestroyed(vm)){
                return;
            }
            vm.set('licenses', result.list);
        });
    }

});
