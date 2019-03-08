Ext.define('Ung.apps.brandingmanager.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app-branding-manager',

    needRackReload: false,
    originalDefaultLogo: true,

    control: {
        '#': {
            afterrender: 'getSettings',
        }
    },

    getSettings: function () {
        var me = this, v = this.getView(), vm = this.getViewModel();

        v.setLoading(true);
        Rpc.asyncData(v.appManager, 'getSettings')
        .then( function(result){
            if(Util.isDestroyed(me, v, vm)){
                return;
            }

            vm.set('settings', result);
            me.originalDefaultLogo = vm.get('settings').defaultLogo;

            vm.set('panel.saveDisabled', false);
            v.setLoading(false);
        },function(ex){
            if(!Util.isDestroyed(v, vm)){
                vm.set('panel.saveDisabled', true);
                v.setLoading(false);
            }
            Util.handleException(ex);
        });
    },

    setSettings: function () {
        var me = this, v = this.getView(), vm = this.getViewModel();

        if (!Util.validateForms(v)) {
            return;
        }

        if(me.originalDefaultLogo !== vm.get('settings').defaultLogo){
            me.needRackReload = true;
        }

        v.setLoading(true);
        Rpc.asyncData(v.appManager, 'setSettings', vm.get('settings'))
        .then(function(result){
            if(Util.isDestroyed(v, vm)){
                return;
            }
            Util.successToast('Settings saved');
            vm.set('panel.saveDisabled', false);
            v.setLoading(false);

            me.getSettings();
            Ext.fireEvent('resetfields', v);

            if(me.needRackReload){
                window.location.reload();
            }
        }, function(ex) {
            if(!Util.isDestroyed(v, vm)){
                vm.set('panel.saveDisabled', true);
                v.setLoading(false);
            }
            Util.handleException(ex);
        });

    },

    onUpload: function( field, value, eOpts){
        var formPanel = this.getView().down('form[name=upload_logo_form]');
        var fileField = formPanel.down('filefield');

        if (fileField.getValue().length === 0) {
            Ext.MessageBox.alert('Failed'.t(), 'Please select an image to upload.'.t() );
            return false;
        }

        formPanel.getForm().submit({
            waitMsg: 'Please wait while your logo image is uploaded...'.t(),
            success: Ext.bind(function(form, action) {
                this.needRackReload = true;
                Ext.MessageBox.alert( 'Succeeded'.t(), 'Upload Logo Succeeded.'.t(),
                    function() {
                        fileField.reset();
                    }
                );
            }, this),
            failure: Ext.bind(function(form, action) {
                Ext.MessageBox.alert( 'Failed'.t(), 'Upload Logo Failed. The logo must be the correct dimensions and in GIF, PNG, or JPG format.'.t() );
            }, this)
        });

        return true;
    }

});
