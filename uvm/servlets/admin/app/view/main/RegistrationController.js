Ext.define('Ung.view.main.RegistrationController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.registration',

    control: {
        '#auth': {
            beforeactivate: 'onBeforeActivateAuth'
        },
        '#register': {
            beforeactivate: 'onBeforeActivateRegister'
        },
        'textfield': {
            keyup: 'submitForm'
        }
    },

    onBeforeActivateAuth: function (auth) {
        this.getViewModel().set('error', null);
        auth.down('#loginForm').reset();
    },

    onBeforeActivateRegister: function (form) {
        this.getViewModel().set('error', null);
        form.reset();
    },

    submitForm: function (field, e) {
        var me = this;
        if (e.getCharCode() === 13) {
            var formCmp = field.up('form');
            if (formCmp.getForm().isValid()) {
                if (formCmp.name === 'loginForm') {
                    me.login(formCmp.down('#loginBtn'));
                }
                if (formCmp.name === 'registerForm') {
                    me.createAccount(formCmp.down('#registerBtn'));
                }
            }
        }
    },

    skipReg: function () {
        this.getView().close();
    },

    finishReg: function() {
        me = this;
        rpc.UvmContext.setRegistered(function(result, ex) {
            if (ex) { Util.handleException(ex); return; }
            rpc.isRegistered = true;
            Ext.fireEvent('postregistration');
        });
        this.getView().close();
        Ext.destroy(this.getView());
    },

    showLogin: function () {
        this.getView().down('#cards').setActiveItem(1);
    },

    showRegister: function () {
        this.getView().down('#cards').setActiveItem(2);
    },

    login: function (btn) {
        var me = this,
            vm = me.getViewModel(),
            form = this.getView().down('#loginForm').getForm();

        vm.set('error', null);
        btn.setDisabled(true);

        if (form.isValid()) {
            me.getView().cloudManager.accountLogin(function(response, exception) {
                btn.setDisabled(false);
                if (exception) {
                    vm.set('error', exception);
                    return;
                }
                if (!response.success) {
                    vm.set('error', response.customerMessage.t());
                    return;
                }
                if (response.token) {
                    me.getView().email = form.findField('email').getValue();
                    me.getView().token = response.token;
                    me.getView().down('#cards').setActiveItem(3);
                }
            },
                form.findField('email').getValue(),
                form.findField('password').getValue(),
                rpc.serverUID,
                rpc.applianceModel,
                rpc.version,
                rpc.installType
            );
        }
    },

    createAccount: function (btn) {
        var me = this,
            vm = me.getViewModel(),
            form = me.getView().down('#register').getForm();

        vm.set('error', null);

        if (form.isValid()) {
            if (form.findField('password').getValue() !== form.findField('passwordConfirm').getValue()) {
                vm.set('error', '"Confirm password" must match the "Password" field.'.t());
                return;
            }
            btn.setDisabled(true);
            me.getView().cloudManager.accountCreate(function(response, exception) {
                    btn.setDisabled(false);
                    if (exception) {
                        console.log(exception);
                        vm.set('error', exception);
                        return;
                    }
                    if (!response.success) {
                        vm.set('error', response.customerMessage.t());
                        return;
                    }
                    if (response.token) {
                        me.getView().email = form.findField('email').getValue();
                        me.getView().token = response.token;
                        me.getView().down('#cards').setActiveItem(3);
                    }
                },
                form.findField('email').getValue(),
                form.findField('password').getValue(),
                form.findField('firstName').getValue(),
                form.findField('lastName').getValue(),
                form.findField('companyName').getValue(),
                rpc.serverUID,
                rpc.applianceModel,
                rpc.version,
                rpc.installType
            );
        }
    }
});
