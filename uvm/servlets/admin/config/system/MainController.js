Ext.define('Ung.config.system.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.config-system',

    control: {
        '#': {
            render: 'onRender',
            afterrender: 'loadSettings',
            hide: 'onHide'
        },
    },

    onHide: function () {
        if (this._boundHandler) {
            window.removeEventListener('message', this._boundHandler);
            this._boundHandler = null;
        }
    },

    onRender: function () {
         this._boundHandler = this.handleMessage.bind(this);
         window.addEventListener('message', this._boundHandler);
    },

    handleMessage: function(event) {
        // Check the origin of the message for security
        if (event.origin !== window.location.origin) {
          return;
        }
        if (event && event.data && event.data.action === Util.ACTION_EVENTS.REFRESH_SYSTEM_SETTINGS) {
            this.loadSettings();
        }
    },

    loadSettings: function () {
        var me = this, v= me.getView(), vm = me.getViewModel();

        var rpcSequence = [
            Rpc.asyncPromise('rpc.languageManager.getLanguageSettings'),
            Rpc.asyncPromise('rpc.systemManager.getSettings'),
            Rpc.directPromise('rpc.isExpertMode'),
        ];

        var dataNames = [
            'languageSettings',
            'systemSettings',
            'isExpertMode'
        ];
        if(Rpc.directData('rpc.appManager.app', 'http')){
            rpcSequence.push(Rpc.asyncPromise('rpc.appManager.app("http").getHttpSettings'));
            dataNames.push('httpSettings');
        }
        if(Rpc.directData('rpc.appManager.app', 'ftp')){
            rpcSequence.push(Rpc.asyncPromise('rpc.appManager.app("ftp").getFtpSettings'));
            dataNames.push('ftpSettings');
        }
        if(Rpc.directData('rpc.appManager.app', 'smtp')){
            rpcSequence.push(Rpc.asyncPromise('rpc.appManager.app("smtp").getSmtpSettings'));
            dataNames.push('smtpSettings');
        }

        v.setLoading(true);
        Ext.Deferred.sequence(rpcSequence, this)
        .then(function(result){
            if(Util.isDestroyed(vm, dataNames)){
                return;
            }


            // Massage language to include the appropriate source.
            var languageSettings = result[0];
            languageSettings['language'] = languageSettings['source'] + '-' + languageSettings['language'];

            dataNames.forEach(function(name, index) {
                if (name !== 'timeZonesList') {
                    vm.set(name, result[index]);
                }
            });

            vm.set('panel.saveDisabled', false);

            // Load language list.
            if(!vm.get('languagesList')){
                Rpc.asyncData('rpc.languageManager.getLanguagesList')
                    .then( function(result){
                        if(!Util.isDestroyed(v,vm)){
                            v.setLoading(false);
                            var languageList = [];
                            result["list"].forEach( function(language){
                                // Only add enabled languages
                                // OR if a disabled language matches the currently selected.
                                if(language["enabled"] === true || language["languageCode"] == languageSettings['language']){
                                    languageList.push(language);
                                }
                            });
                            vm.set('languagesList', {"list": languageList});
                        }
                    });
            }else{
                v.setLoading(false);
            }
        }, function(ex) {
            if(!Util.isDestroyed(v, vm)){
                vm.set('panel.saveDisabled', true);
                v.setLoading(false);
            }
        });
    },


    syncLanguage: function () {
        Ext.MessageBox.wait('Synchronizing languages with the internet...'.t(), 'Please wait'.t());
        Rpc.asyncData('rpc.languageManager.synchronizeLanguage')
        .then(function(result, ex){
           document.location.reload();
        });
    },

    saveSettings: function () {
        var me = this, v = me.getView(),
            vm = me.getViewModel();

        if (!Util.validateForms(v)) {
            return;
        }

        v.setLoading(true);
        if (vm.get('languageSettings.regionalFormats') === 'default') {
            // reset overrides
            vm.set('languageSettings.overrideDateFmt', '');
            vm.set('languageSettings.overrideDecimalSep', '');
            vm.set('languageSettings.overrideThousandSep', '');
            vm.set('languageSettings.overrideTimestampFmt', '');
        }

        v.query('ungrid').forEach(function (grid) {
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
        });

        var languageSettings = vm.get('languageSettings');
        var languageSplit = languageSettings['language'].split('-');
        if(languageSplit[0] != 'official'){
            // Something bad has happened; referve to known good language.
            languageSplit[0] = "official";
            languageSplit[1] = "en";
        }
        languageSettings['source'] = languageSplit[0];
        languageSettings['language'] = languageSplit[1];

        var rpcSequence = [
            Rpc.asyncPromise('rpc.languageManager.setLanguageSettings', languageSettings),
            Rpc.asyncPromise('rpc.systemManager.setSettings', vm.get('systemSettings')),
        ];

        if(Rpc.directData('rpc.appManager.app', 'http')){
            rpcSequence.push(Rpc.asyncPromise('rpc.appManager.app("http").setSettings', vm.get('httpSettings')));
        }
        if(Rpc.directData('rpc.appManager.app', 'ftp')){
            rpcSequence.push(Rpc.asyncPromise('rpc.appManager.app("ftp").setSettings', vm.get('ftpSettings')));
        }
        if(Rpc.directData('rpc.appManager.app', 'smtp')){
            rpcSequence.push(Rpc.asyncPromise('rpc.appManager.app("smtp").setSettings', vm.get('smtpSettings')));
        }

        Ext.Deferred.sequence(rpcSequence, this)
        .then(function () {
            if(Util.isDestroyed(me, v, vm)){
                return;
            }
            Util.successToast('System settings saved!');
            if(vm.get('localizationChanged') == true){
                window.location.reload();
            }
            Ext.fireEvent('resetfields', v);
            v.setLoading(false);
            me.loadSettings();
        }, function (ex) {
            if(!Util.isDestroyed(v, vm)){
                v.setLoading(false);
                vm.set('panel.saveDisabled', true);
            }
        });
    },

    // Support methods
    downloadSystemLogs: function () {
        var downloadForm = document.getElementById('downloadForm');
        downloadForm.type.value = 'SystemSupportLogs';
        downloadForm.submit();
    },

    // Backup method(s)
    backupToFile: function () {
        var downloadForm = document.getElementById('downloadForm');
        downloadForm.type.value = 'backup';
        downloadForm.submit();
    },

    // Restore method(s)
    restoreFromFile: function (btn) {
        var restoreFile = this.getView().down('#restoreFile').getValue();
        if (!restoreFile || restoreFile.length === 0) {
            Util.handleException('Please select a file to upload.'.t());
            return;
        }
        btn.up('form').submit({
            waitMsg: 'Restoring from File...'.t(),
            success: function (form, action) {
                Ext.MessageBox.alert('Restore'.t(), action.result.msg);
            },
            failure: function (form, action) {
                var errorMsg = 'The File restore procedure failed.'.t();
                if (action.result && action.result.msg) {
                    errorMsg = action.result.msg;
                }
                Ext.MessageBox.alert('Failed', errorMsg);
            }
        });
    },

    languageChange: function(combo, newValue, oldValue){
        var me = this,
            vm = me.getViewModel();

        if( ( oldValue != null ) &&
            ( newValue != oldValue ) ){
            vm.set('localizationChanged', true);
        }
    }

});
