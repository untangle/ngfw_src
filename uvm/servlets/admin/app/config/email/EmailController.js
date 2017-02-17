Ext.define('Ung.config.email.EmailController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.config.email',

    control: {
        '#': {
            beforerender: 'loadSettings'
        },
        '#quarantine': {
            beforerender: 'loadQuarantine'
        }
    },

    mailSender: rpc.UvmContext.mailSender(),
    originalMailSender: null,

    smtpNode: rpc.nodeManager.node('untangle-casing-smtp'),
    safelistAdminView: null,

    loadSettings: function (view) {
        this.safelistAdminView =  this.smtpNode.getSafelistAdminView();

        // load mail settings
        this.mailSettings();
        this.getSafeList();
    },

    saveSettings: function () {
        var deferred = new Ext.Deferred(),
            invalidFields = [];
        this.getView().query('form').forEach(function (form) {
            form.query('field{isValid()==false}').forEach(function (field) {
                invalidFields.push({ label: field.getFieldLabel(), error: field.getActiveError() });
            });
        });

        if (invalidFields.length > 0) {
            Ung.Util.invalidFormToast(invalidFields);
            deferred.reject('invalid fields');
        }

        var me = this, view = this.getView();
        view.setLoading('Saving ...');
        this.mailSender.setSettings(function(result, ex) {
            view.setLoading(false);
            if (ex) {
                console.error(ex);
                Ung.Util.exceptionToast(ex);
                deferred.reject(ex);
            }
            me.mailSettings();
            Ung.Util.successToast('Email'.t() + ' settings saved!');
            deferred.resolve();
        }, me.getViewModel().get('mailSender'));
        return deferred.promise;
    },

    mailSettings: function () {
        var me = this;
        this.mailSender.getSettings(function (result, ex) {
            if (ex) { console.error(ex); Ung.Util.exceptionToast(ex); return; }
            me.getViewModel().set('mailSender', result);
            me.originalMailSender = Ext.clone(result);
            console.log(me.getViewModel());
        });
    },

    testEmail: function () {
        var me = this, vm = this.getViewModel(),
            modifiedVal = Ext.encode(vm.get('mailSender')),
            originalVal = Ext.encode(me.originalMailSender);

        if (originalVal !== modifiedVal) {
            Ext.Msg.show({
                title: 'Save Changes?'.t(),
                msg: Ext.String.format('Your current settings have not been saved yet.{0}Would you like to save your settings before executing the test?'.t(), '<br />'),
                buttons: Ext.Msg.YESNOCANCEL,
                fn: function(btnId) {
                    if (btnId === 'yes') {
                        me.saveSettings().then(function () {
                            Ext.create('Ung.config.email.EmailTest');
                        });
                    }
                    if (btnId === 'no') {
                        Ext.create('Ung.config.email.EmailTest');
                    }
                },
                animEl: 'elId',
                icon: Ext.MessageBox.QUESTION
            });
        } else {
            Ext.create('Ung.config.email.EmailTest');
        }
    },


    // Safe List
    getSafeList: function () {
        var me = this;
        me.safelistAdminView.getSafelistContents(function (result, ex) {
            if (ex) { console.error(ex); Ung.Util.exceptionToast(ex); return; }
            console.log(result);
            me.getViewModel().set('globalSafeList', result);
        }, 'GLOBAL');
    },

    // Quarantine
    loadQuarantine: function () {
        var me = this;
        this.smtpNode.getSmtpNodeSettings(function (result, ex) {
            if (ex) { console.error(ex); Ung.Util.exceptionToast(ex); return; }
             me.getViewModel().set('smtpNodeSettings', result);
        });
    }


});