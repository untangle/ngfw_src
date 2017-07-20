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

        if (!rpc.certificateManager) {
            rpc.certificateManager = rpc.UvmContext.certificateManager();
        }
        Rpc.asyncData('rpc.certificateManager.validateActiveInspectorCertificates')
            .then(function (result) {
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
        v.appManager.getSettings(function (result, ex) {
            v.setLoading(false);
            if (ex) { Util.handleException(ex); return; }
            console.log(result);
            vm.set('settings', result);
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
        v.appManager.setSettings(function (result, ex) {
            v.setLoading(false);
            if (ex) { Util.handleException(ex); return; }
            Util.successToast('Settings saved');
            me.getSettings();
            Ext.fireEvent('resetfields', v);
        }, vm.get('settings'));
    },

    getTrustedCerts: function() {
        var me = this, v = this.getView(), vm = this.getViewModel();

        v.appManager.getTrustCatalog(function(result, ex) {
            if (ex) { Util.handleException(ex); return; }
            vm.set('trustedCertData', result.list);
        });
    },

    uploadTrustedCertificate: function(view, row, colIndex, item, e, record) {
        var me = this, v = this.getView(), vm = this.getViewModel();

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
        var me = this, v = this.getView(), vm = this.getViewModel();
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

});

Ext.define('Ung.apps.sslinspector.SpecialGridController', {
    extend: 'Ung.cmp.GridController',
    alias: 'controller.app-sslinspector-special',

    deleteTrustedCertificate: function(view, row, colIndex, item, e, record) {
        var me = this, v = this.getView(), vm = this.getViewModel();
        var app = rpc.appManager.app('ssl-inspector');

        v.setLoading('Deleting Certificate...'.t());
        app.removeTrustedCertificate(Ext.bind(function(result, ex) {
        if (ex) { Util.handleException(ex); return; }
            // this gives the app a little time to process the delete before we refresh
            var timer = setTimeout(function() {
                me.getView().up('app-ssl-inspector').getController().getTrustedCerts();
                v.setLoading(false);
            },500);
        }, this), record.get("certAlias"));
    },
});
