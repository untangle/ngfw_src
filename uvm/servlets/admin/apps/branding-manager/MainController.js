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
        v.appManager.getSettings(function (result, ex) {
            v.setLoading(false);
            if (ex) { Util.exceptionToast(ex); return; }
            vm.set('settings', result);
            me.originalDefaultLogo = vm.get('settings').defaultLogo;
        });
    },

    setSettings: function () {
        var me = this, v = this.getView(), vm = this.getViewModel();

        if (!Util.validateForms(v)) {
            return;
        }

        v.setLoading(true);

        if(me.originalDefaultLogo != vm.get('settings').defaultLogo){
            me.needRackReload = true;
        }

        v.appManager.setSettings(function (result, ex) {
            v.setLoading(false);
            if (ex) { Util.exceptionToast(ex); return; }
            Util.successToast('Settings saved');
            me.getSettings();
        }, vm.get('settings'));

        if(me.needRackReload){
            window.location.reload()
        }

    },

    onUpload: function( field, value, eOpts){
        var formPanel = this.getView().down("form[name=upload_logo_form]");
        var fileField = formPanel.down('filefield');

        if (fileField.getValue().length === 0) {
            Ext.MessageBox.alert( "Failed".t(), 'Please select an image to upload.'.t() );
            return;
        }

        formPanel.getForm().submit({
            waitMsg: 'Please wait while your logo image is uploaded...'.t(),
            success: Ext.bind(function(form, action) {
                this.needRackReload = true;
                Ext.MessageBox.alert( "Succeeded".t(), "Upload Logo Succeeded.".t(),
                    function() {
                            fileField.reset();
                    }
                );
            }, this),
            failure: Ext.bind(function(form, action) {
                Ext.MessageBox.alert( "Failed".t(), "Upload Logo Failed. The logo must be the correct dimensions and in GIF, PNG, or JPG format.".t() );
            }, this)
        });

        return true;
    }

});
