Ext.define('Ung.apps.bandwidthcontrol.ConfWizardController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app-tunnel-vpn-wizard',

    control: {
        '#': {
            afterrender: 'onAfterRender'
        },
        'window > panel': {
            activate: 'onActivateCard'
        }
    },

    onAfterRender: function () {
    },

    onActivateCard: function (panel) {
        var vm = this.getViewModel(),
            layout = this.getView().getLayout();

        vm.set('prevBtn', layout.getPrev());
        if (layout.getPrev()) {
            vm.set('prevBtnText', layout.getPrev().getTitle());
        }
        vm.set('nextBtn', layout.getNext());
        if (layout.getNext()) {
            vm.set('nextBtnText', layout.getNext().getTitle());
        }
    },

    onNext: function () {
        var v = this.getView(),
            vm = this.getViewModel(),
            activeItem = v.getLayout().getActiveItem();

        if (activeItem.getItemId() === 'welcome') {
            v.getLayout().next();
        }

        if (activeItem.getItemId() === 'vpn-service') {
            v.getLayout().next();
        }

        if (activeItem.getItemId() === 'upload') {
            if (vm.get('provider') == 'ExpressVPN') {
                this.getView().appManager.setUsernamePassword(vm.get('tunnelId'),vm.get('username'),vm.get('password'));
            }
            else if (vm.get('provider') == 'NordVPN') {
                this.getView().appManager.setUsernamePassword(vm.get('tunnelId'),vm.get('username'),vm.get('password'));
            }

            v.getLayout().next();
        }
    },

    onPrev: function () {
        var v = this.getView();
        if (v.getLayout().getPrev()) {
            v.getLayout().prev();
        }
    },

    uploadFile: function(cmp) {
        var form = Ext.ComponentQuery.query('form[name=upload_form]')[0];
        var file = Ext.ComponentQuery.query('textfield[name=upload_file]')[0].value;
        if ( file == null || file.length === 0 ) {
            Ext.MessageBox.alert('Select File'.t(), 'Please choose a file to upload.'.t());
            return;
        }
        Ext.MessageBox.wait('Uploading...'.t(), 'Please wait'.t());
        form.submit({
            url: "upload",
            success: Ext.bind(function( form, action ) {
                var tunnelId = this.getView().appManager.getNewTunnelId();
                this.getViewModel().set("tunnelId",tunnelId);
                Ext.MessageBox.alert('Configuration Import Success'.t(), action.result.msg);
            }, this),
            failure: Ext.bind(function( form, action ) {
                Ext.MessageBox.alert('Configuration Import Failure'.t(), action.result.msg);
            }, this)
        });
    },
    
    onFinish: function () {
        // fire finish event to reload settings ant try to start app
        this.getView().fireEvent('finish');
        this.getView().close();
    }});
