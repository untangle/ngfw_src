Ext.define('Ung.apps.sslinspector.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app-ssl-inspector',

    control: {
        '#': {
            afterrender: 'onAfterRender'
        }
    },

    onAfterRender: function () {
        var vm = this.getViewModel();

        Rpc.asyncData('rpc.UvmContext.certificateManager.validateActiveInspectorCertificates')
        .then(function (result) {
            if(Util.isDestroyed(vm)){
                return;
            }
            vm.set('serverCertificateVerification', result);
        }, function (ex) {
            Util.handleException(ex);
        });
        this.getSettings();
        this.getTrustedCerts();
    },

    getSettings: function () {
        var v = this.getView(), vm = this.getViewModel();

        v.setLoading(true);
        Rpc.asyncData(v.appManager, 'getSettings')
        .then( function(result){
            if(Util.isDestroyed(v, vm)){
                return;
            }

           vm.set('settings', result);

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
        v.query('ungrid').forEach(function (grid) {
            if (grid.listProperty) {
                var store = grid.getStore();
                if (store.getModifiedRecords().length > 0 ||
                    store.getNewRecords().length > 0 ||
                    store.getRemovedRecords().length > 0 ||
                    store.isReordered) {
                    store.each(function (record) {
                        if (record.get('markedForDelete')) {
                            record.drop();
                        }
                    });
                    store.isReordered = undefined;
                    vm.set(grid.listProperty, Ext.Array.pluck(store.getRange(), 'data'));
                }
            }
        });

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
        }, function(ex) {
            if(!Util.isDestroyed(v, vm)){
                vm.set('panel.saveDisabled', true);
                v.setLoading(false);
            }
            Util.handleException(ex);
        });
    },

    getTrustedCerts: function() {
        var v = this.getView(), vm = this.getViewModel();

        Rpc.asyncData(v.appManager, 'getTrustCatalog')
        .then(function(result){
            if(Util.isDestroyed(vm)){
                return;
            }
            vm.set('trustedCertData', result.list);
        }, function(ex) {
            Util.handleException(ex);
        });
    },

    uploadTrustedCertificate: function(view, row, colIndex, item, e, record) {
        this.uploadCertificateWin = Ext.create('Ext.Window', {
            title: "Upload Trusted Certificate".t(),
            layout: 'fit',
            modal: true,
            width: 600,
            height: 200,
            autoScroll: true,
            items: [{
                xtype: "form",
                name: "upload_trusted_cert_form",
                url: "upload",
                items: [{
                    xtype: 'filefield',
                    fieldLabel: "File".t(),
                    name: "filename",
                    margin: "10 10 10 10",
                    width: 560,
                    labelWidth: 50,
                    allowBlank: false,
                    validateOnBlur: false
                }, {
                    xtype: 'textfield',
                    fieldLabel: "Alias".t(),
                    name: "argument",
                    margin: "10 10 10 10",
                    width: 200,
                    labelWidth: 50,
                    allowBlank: false
                }, {
                    xtype: "button",
                    text: "Upload Certificate".t(),
                    width: 200,
                    margin: "10 10 10 80",
                    handler: Ext.bind(function() {
                        this.handleFileUpload();
                    }, this)
                }, {
                    xtype: "button",
                    text: "Cancel".t(),
                    width: 200,
                    margin: "10 10 10 10",
                    handler: Ext.bind(function() {
                        this.uploadCertificateWin.close();
                    }, this)
                }, {
                    xtype: "hidden",
                    name: "type",
                    value: "trusted_cert"
                    }]
                }]
        });

        this.uploadCertificateWin.show();
    },

    handleFileUpload: function() {
        var me = this;
        var form = this.uploadCertificateWin.down('form[name="upload_trusted_cert_form"]');
        var fileText = form.down('filefield[name="filename"]');
        var nameText = form.down('textfield[name="argument"]');
        if (fileText.getValue().length === 0) {
            Ext.MessageBox.alert("Invalid or missing File".t(), "Please select a certificate to upload.".t());
            return false;
            }
        if (nameText.getValue().length === 0) {
            Ext.MessageBox.alert("Invalid or missing Alias".t(), "Please enter a unique alias or nickname for the certificate.".t());
            return false;
        }

        form.submit({
            waitMsg: "Inspecting File...".t(),
            success: Ext.bind(function(form, action) {
                this.uploadCertificateWin.close();
                me.getTrustedCerts();
            }, this),
            failure: Ext.bind(function(form, action) {
                this.uploadCertificateWin.close();
                Ext.MessageBox.alert("Upload Failure".t(), action.result.msg);
            }, this)
        });
        return true;
    },

    statics:{
        actionRenderer: function(action){
            switch (action.actionType) {
                case 'INSPECT': return 'Inspect'.t();
                case 'IGNORE': return 'Ignore'.t();
                default: return 'Unknown Action'.t() + ': ' + act;
            }
        }
    }

});

Ext.define('Ung.apps.sslinspector.SpecialGridController', {
    extend: 'Ung.cmp.GridController',
    alias: 'controller.app-sslinspector-special',

    deleteTrustedCertificate: function(view, row, colIndex, item, e, record) {
        var me = this, v = this.getView();

        v.setLoading('Deleting Certificate...'.t());
        Rpc.asyncData(view.up('apppanel').appManager, 'removeTrustedCertificate', record.get("certAlias"))
        .then(function(result){
            if(Util.isDestroyed(me, v)){
                return;
            }
            setTimeout(function() {
                if(Util.isDestroyed(me, v)){
                    return;
                }
                me.getView().up('app-ssl-inspector').getController().getTrustedCerts();
                v.setLoading(false);
            },500);

        }, function(ex) {
            Util.handleException(ex);
        });
    },
});
