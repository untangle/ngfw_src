Ext.define('Ung.Setup.Wireless', {
    extend: 'Ext.form.Panel',
    alias: 'widget.Wireless',

    title: 'Wireless Settings'.t(),
    description: 'Configure Wireless Settings'.t(),

    layout: {
        type: 'center'
    },
    items: [{
        xtype: 'container',
        width: 200,
        padding: '0 0 100 0',
        layout: {
            type: 'vbox',
            align: 'stretch'
        },
        defaults: {
            labelAlign: 'top',
            msgTarget: 'side',
            validationEvent: 'blur'
        },
        hidden: true,
        bind: {
            hidden: '{!wirelessSettings}'
        },
        items: [{
            xtype: 'component',
            cls: 'sectionheader',
            // margin: '30 0 0 0',
            html: 'Settings'.t()
        }, {
            xtype: 'textfield',
            fieldLabel: 'Network Name (SSID)'.t(),
            width: 350,
            maxLength: 30,
            maskRe: /[a-zA-Z0-9\-_=]/,
            bind: {
                value: '{wirelessSettings.ssid}'
            },
            allowBlank: false
        }, {
            xtype: 'combo',
            fieldLabel: 'Encryption'.t(),
            width: 300,
            editable: false,
            store: [['NONE', 'None'.t()], ['WPA1', 'WPA'.t()], ['WPA12', 'WPA / WPA2'.t()], ['WPA2', 'WPA2'.t()]],
            bind: {
                value: '{wirelessSettings.encryption}'
            }
        }, {
            xtype: 'textfield',
            fieldLabel: 'Password'.t(),
            width: 350,
            maxLength: 63,
            minLength: 8,
            maskRe: /[a-zA-Z0-9~@#%_=,\!\-\/\?\(\)\[\]\\\^\$\+\*\.\|]/,
            disabled: true,
            bind: {
                value: '{wirelessSettings.password}',
                disabled: '{wirelessSettings.encryption === "NONE"}'
            },
            validator: function (val) {
                if (!val || val.length < 8) {
                    return 'The wireless password must be at least 8 characters.'.t();
                }
                if (val === '12345678') {
                    return 'You must choose a new and different wireless password.'.t();
                }
                return true;
            },
            listeners: {
                disable: function (el) {
                    el.setValue('');
                }
            }
        }]
    }],

    listeners: {
        activate: 'getSettings',
        save: 'onSave'
    },

    controller: {

        getSettings: function () {
            var me = this, vm = me.getViewModel(),
                interfaces = vm.get('networkSettings.interfaces.list'),
                // first wireless interface
                wireless = Ext.Array.findBy(interfaces, function (intf) {
                    return intf.isWirelessInterface;
                });

            if (!wireless) {
                Ext.Msg.show({
                    title: 'Warning!',
                    message: 'No wireless interfaces found. Do you want to continue the setup?',
                    buttons: Ext.Msg.YESNO,
                    icon: Ext.Msg.QUESTION,
                    fn: function (btn) {
                        if (btn === 'yes') {
                            me.getView().up('setupwizard').down('#nextBtn').click();
                        } else {
                            // if no is pressed
                        }
                    }
                });
                return;
            }

            Ung.app.loading('Loading Wireless Settings ...');
            Ext.Deferred.sequence([
                me.getSsid,
                me.getEncryption,
                me.getPassword
            ]).then(function (result) {
                vm.set('wirelessSettings', {
                    ssid: result[0] || '',
                    encryption: result[1] || 'NONE',
                    password: (!result[2] || result[2] === '12345678') ? '' : result[2]
                });

                me.initialSettings = Ext.clone(vm.get('wirelessSettings'));

            }, function (ex) {
                Util.handleException(ex);
            }).always(function(){
                Ung.app.loading(false);
            });
        },

        getSsid: function () {
            var deferred = new Ext.Deferred();
            rpc.networkManager.getWirelessSsid(function (result, ex) {
                if (ex) { deferred.reject(ex); }
                deferred.resolve(result);
            });
            return deferred.promise;
        },

        getEncryption: function () {
            var deferred = new Ext.Deferred();
            rpc.networkManager.getWirelessEncryption(function (result, ex) {
                if (ex) { deferred.reject(ex); }
                deferred.resolve(result);
            });
            return deferred.promise;
        },

        getPassword: function () {
            var deferred = new Ext.Deferred();
            rpc.networkManager.getWirelessPassword(function (result, ex) {
                if (ex) { deferred.reject(ex); }
                deferred.resolve(result);
            });
            return deferred.promise;
        },

        onSave: function (cb) {
            var me = this, form = me.getView(), vm = me.getViewModel();

            // if invalid form show invalid fields
            if (!form.isValid()) { return; }

            // if valid but no changes made just move to next step
            if (Ext.Object.equals(me.initialSettings, vm.get('wirelessSettings'))) {
                cb(); return;
            }

            // otherwise save settings
            Ung.app.loading('Saving Settings'.t());
            rpc.networkManager.setWirelessSettings(function (result, ex) {
                Ung.app.loading(false);
                if (ex) { Util.handleException(ex); return; }
                cb();
            }, vm.get('wirelessSettings.ssid'),
            vm.get('wirelessSettings.encryption'),
            vm.get('wirelessSettings.password'));
        }
    }
});
