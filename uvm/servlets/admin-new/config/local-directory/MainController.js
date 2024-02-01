Ext.define('Ung.config.local-directory.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.config-local-directory',
    alternateClassName: 'Ung.config.localdirectory.MainController',

    control: {
        '#': {
            beforerender: 'loadSettings'
        },
        '#radius-log': { afterrender: 'refreshRadiusLogFile' },
        '#radius-proxy': { afterrender: 'refreshRadiusProxyStatus' }
    },

    loadSettings: function () {
        var me = this, v = me.getView(), vm = me.getViewModel();

        v.setLoading(true);
        Ext.Deferred.sequence([
            Rpc.asyncPromise('rpc.UvmContext.localDirectory.getUsers'),
            Rpc.asyncPromise('rpc.systemManager.getSettings')
        ], this)
        .then(function (result) {
            if(Util.isDestroyed(v, vm)){
                return;
            }
            // set the local record fields we use to deal with the expiration time and add vs edit logic
            var users = result[0];
                for(var i = 0 ; i < users.list.length ; i++) {
                    users.list[i].localEmpty = false;
                        if (users.list[i].expirationTime == 0) {
                            users.list[i].localExpires = new Date();
                            users.list[i].localForever = true;
                        } else {
                            users.list[i].localExpires = new Date(users.list[i].expirationTime);
                            users.list[i].localForever = false;
                        }
                }

            vm.set('usersData', users);
            vm.set('systemSettings', result[1]);
            v.setLoading(false);
            vm.set('panel.saveDisabled', false);
        }, function(ex) {
            if(!Util.isDestroyed(v, vm)){
                vm.set('panel.saveDisabled', true);
                v.setLoading(false);
            }
        });
    },

    saveSettings: function () {
        var me = this, v = me.getView(), vm = me.getViewModel();

        if (!Util.validateForms(v)) {
            return;
        }

        v.setLoading(true);
        v.query('ungrid').forEach(function (grid) {
            var store = grid.getStore();

            /**
             * Important!
             * update custom grids only if are modified records or it was reordered via drag/drop
             */
            if (store.getModifiedRecords().length > 0 || store.isReordered) {
                store.each(function (record) {
                    if (record.get('markedForDelete')) {
                        record.drop();
                    }
                });
                store.isReordered = undefined;
                vm.set(grid.listProperty, Ext.Array.pluck(store.getRange(), 'data'));
            }
        });

        var userlist = vm.get('usersData');
        var user;

        for(var i = 0 ; i < userlist.list.length ; i++) {
            user = userlist.list[i];

            if(user.password == null) user.password = "";

            // calculate the passwordBase64Hash for any changed passwords and remove cleartext
            if(user.password.length > 0) {
                user.passwordBase64Hash = Util.base64encode(user.password);
                user.password = "";
            }

            // use localForever and localExpires to set the correct expirationTime
            if (user.localForever == true) {
                user.expirationTime = 0;
            } else {
                user.expirationTime = user.localExpires.getTime();
            }
        }

        var dirtyRadiusFields = false;
        Ext.Array.each(v.query('field'), function (field) {
            if (!field._neverDirty && field._isChanged && !dirtyRadiusFields) {
                dirtyRadiusFields = true;
            }
        });

        // Make sure we set the userlist last because that function will generate
        // the user credentials and shared secret configs for the freeradius server
        Ext.Deferred.sequence([
            Rpc.asyncPromise('rpc.systemManager.setSettings', vm.get('systemSettings'), dirtyRadiusFields),
            Rpc.asyncPromise('rpc.UvmContext.localDirectory.setUsers', userlist)
        ], this)
        .then(function (result) {
            if(Util.isDestroyed(v, me)){
                return;
            }
            var radiusProxyCheckbox = v.query('field')[2];
            v.setLoading(false);
            Ext.fireEvent('resetfields', v);
            me.loadSettings();
            v.setLoading(false);
            Util.successToast('Settings saved');

            //Disable Radius Proxy fieldsets if needed
            if (radiusProxyCheckbox.checked) {
                // After save, have to set original value to current value so isDirty() computes properly
                var radiusProxyTextFields = radiusProxyCheckbox.up('panel').query('fieldset')[0].query('textfield');
                radiusProxyTextFields.forEach(function(textField) {
                    textField.originalValue = textField.value;
                });

                //Disable fieldsetes if any field of AD server is disabled
                var emptyFields = me.checkDirtyOrEmpty(radiusProxyCheckbox);
                if (emptyFields) me.setDisabledForRadiusProxyFieldsets(radiusProxyCheckbox, true);
                else me.setDisabledForRadiusProxyFieldsets(radiusProxyCheckbox, false);
            }
        }, function (ex) {
            if(!Util.isDestroyed(v, vm)){
                vm.set('panel.saveDisabled', true);
                v.setLoading(false);
            }
            Util.handleException(ex);
        });
    },
    configureCertificate: function (btn) {
        Ung.app.redirectTo("#config/administration/certificates");
    },

    refreshRadiusLogFile: function (cmp) {
        var v = cmp.isXType('button') ? cmp.up('panel') : cmp;
        var target = v.down('textarea');

        if (!target) {
            return;
        }

        v.setLoading(true);
        Rpc.asyncData('rpc.UvmContext.localDirectory.getRadiusLogFile')
        .then(function(result){
            if(Util.isDestroyed(v, target)){
                return;
            }
            target.setValue(result);
            v.setLoading(false);
        });
    },

    refreshRadiusProxyStatus: function (cmp) {
        var vm = this.getViewModel();
        if (vm.get('systemSettings.radiusProxyEnabled') !== true) {
            return;
        }

        var v = cmp.isXType('button') ? cmp.up('panel') : cmp;
        var target = v.down('textarea');

        if (!target) {
            return;
        }

        v.setLoading(true);
        Rpc.asyncData('rpc.UvmContext.localDirectory.getRadiusProxyStatus')
        .then(function(result){
            if(Util.isDestroyed(v, target)){
                return;
            }
            target.setValue(result);
            v.setLoading(false);
        });
    },

    createComputerAccount: function (cmp) {
        var v = cmp.isXType('button') ? cmp.up('panel') : cmp;
        var dirtyFields = false;

        Ext.Array.each(v.query('field'), function (field) {
            if (!field._neverDirty && field._isChanged && !dirtyFields) {
                dirtyFields = true;
            }
        });

        if (dirtyFields) {
            Ext.MessageBox.alert('Unsaved Changes'.t(), 'The settings must be saved before the computer account can be created.'.t());
            return;
        }

        v.setLoading(true);
        Rpc.asyncData('rpc.UvmContext.localDirectory.addRadiusComputerAccount')
        .then(function(result){
            v.setLoading(false);
            Ext.MessageBox.alert({ buttons: Ext.Msg.OK, maxWidth: 1024, title: 'Account Creation Status'.t(), msg: '<tt>' + result.output + '</tt>' });
        });
    },

    testRadiusProxyLogin: function (cmp) {
        var v = cmp.isXType('button') ? cmp.up('panel') : cmp;
        var testuser = v.down("[fieldIndex='testUsername']").getValue();
        var testpass = v.down("[fieldIndex='testPassword']").getValue();
        var testdom = v.down("[fieldIndex='adDomain']").getValue();
        var dirtyFields = false;

        Ext.Array.each(v.query('field'), function (field) {
            if (!field._neverDirty && field._isChanged && !dirtyFields) {
                dirtyFields = true;
            }
        });

        if (dirtyFields) {
            Ext.MessageBox.alert('Unsaved Changes'.t(), 'The settings must be saved before using the authentication test.'.t());
            return;
        }

        v.setLoading(true);
        Rpc.asyncData('rpc.UvmContext.localDirectory.testRadiusProxyLogin', testuser, testpass, testdom)
        .then(function(result){
            v.setLoading(false);
            Ext.MessageBox.alert({ buttons: Ext.Msg.OK, maxWidth: 1024, title: 'Test Authentication Result'.t(), msg: '<tt>' + result + '</tt>' });
        });
    },

    /**
     * Logic to handle showing/hiding password in the Radius Proxy UI
     * @param {button} button that user clicks to indicate if they want to hide/show the password
     */
    showOrHideRadiusPassword: function (button) {
        var isShowPassword = button.iconCls === 'fa fa-eye';
        button.setTooltip(isShowPassword ? 'Hide password' : 'Show password');
        button.setIconCls(isShowPassword ? 'fa fa-eye-slash' : 'fa fa-eye');
        button.prev().getEl().query('input', false)[0].set({'type':isShowPassword?'text':'password'});
    },

    /**
     * When a field changes, determine if should disable or enable the fieldsets
     * @param {field} field field being changed
     * @param {string} newVal new value of field
     * @param {string} oldVal old value of field
     */
    radiusProxyDirtyFieldsHandler: function(field, newVal, oldVal) {
        var me = this;

        //The first change occurs always when the value is binded, so set it to that value
        // so original value is correct
        if (field._onFirstChange) {
            field.originalValue = newVal;
        }
        // Determine if any field is empty
        var emptyOrDirtyFields = me.checkDirtyOrEmpty(field);

        //If the checkbox is false, fields are empty, or a non-checkbox is dirty, disable fields
        if (!newVal                                         ||
            emptyOrDirtyFields                              ||
            (field.xtype === 'checkbox' && !newVal)         ||
            (field.xtype !== 'checkbox' && field.isDirty()) ||
            (field.xtype !== 'checkbox' && !field.up('panel').query('checkbox')[0].checked)) {
            me.setDisabledForRadiusProxyFieldsets(field, true);
        } else {
            me.setDisabledForRadiusProxyFieldsets(field, false);
        }

        field._onFirstChange = false;
    },

    /**
     * Deteremine if any fields in Active Directory Server fieldset of Radius Proxy are empty or dirty
     * @param {field} field field used to find textfields of AD server to see if empty
     */
    checkDirtyOrEmpty: function(field) {
        var emptyOrDirtyFields = false;
        textFields = field.up('panel').query('fieldset')[0].query('textfield');
        textFields.forEach(function(textField) {
            //Use original value as the getValue() is unreliable with the password input
            if (textField.value === '' || textField.isDirty()) {
                emptyOrDirtyFields = true;
            }
        });
        return emptyOrDirtyFields;
    },

    /**
     * Enable/Disable the Active Directory Test and Active Directory Status of Radius Proxy
     * @param {field} field field to find the Active Directory Test and Active Directory Status fieldsets
     * @param {boolean} disabled boolean on if fieldsets should be disabled or not
     */
    setDisabledForRadiusProxyFieldsets: function(field, disabled) {
        field.up('panel').query('fieldset')[1].setDisabled(disabled);
        field.up('panel').query('fieldset')[2].setDisabled(disabled);
    },

    statics:{
        expirationRenderer: function( value ){
            var date;
            if (value > 0) {
                date = new Date(value);
                return Renderer.timestamp(Util.clientToServerDate(date).getTime());
            } else {
                return 'Never'.t();
            }
        }
    }

});
