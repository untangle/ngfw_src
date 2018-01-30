Ext.define('Ung.config.system.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.config-system',

    control: {
        '#': { afterrender: 'loadSettings' }
    },

    loadSettings: function () {
        var me = this, vm = me.getViewModel();

        me.getView().setLoading(true);
        Ext.Deferred.sequence([
            Rpc.directPromise('rpc.languageManager.getLanguageSettings'),
            Rpc.directPromise('rpc.languageManager.getLanguagesList'),
            Rpc.directPromise('rpc.systemManager.getSettings'),
            Rpc.asyncPromise('rpc.systemManager.getDate'),
            Rpc.directPromise('rpc.systemManager.getTimeZone'),
            Rpc.directPromise('rpc.systemManager.getTimeZones'),
            Rpc.directPromise('rpc.appManager.app("http").getHttpSettings'),
            Rpc.directPromise('rpc.appManager.app("ftp").getFtpSettings'),
            Rpc.directPromise('rpc.appManager.app("smtp").getSmtpSettings'),
            Rpc.directPromise('rpc.appManager.app("shield").getSettings'),
            Rpc.directPromise('rpc.isExpertMode')
            ], this).then(function(result){
                if(Util.isDestroyed(me, vm)){
                    return;
                }
                me.getView().setLoading(false);

                var timeZones = [];
                if (result[5]) {
                    eval(result[5]).forEach(function (tz) {
                        timeZones.push({name: '(' + tz[1] + ') ' + tz[0], value: tz[0]});
                    });
                }

                // Massage language to include the appropriate source.
                var languageSettings = result[0];
                languageSettings['language'] = languageSettings['source'] + '-' + languageSettings['language'];

                vm.set({
                    languageSettings: result[0],
                    languagesList: result[1],
                    systemSettings: result[2],
                    time: result[3],
                    timeZone: result[4],
                    timeZonesList: timeZones,
                    httpSettings: result[6],
                    ftpSettings: result[7],
                    smtpSettings: result[8],
                    shieldSettings: result[9],
                    isExpertMode: result[10]
                });
        });
    },

    syncTime: function () {
        Ext.MessageBox.confirm(
            'Force Time Synchronization'.t(),
            'Forced time synchronization can cause problems if the current date is far in the future.'.t() + '<br/>' +
            'A reboot is suggested after time sychronization.'.t() + '<br/><br/>' +
            'Continue?'.t(),
            function(btn) {
                if (btn === 'yes') {
                    Ext.MessageBox.wait('Synchronizing time with the internet...'.t(), 'Please wait'.t());
                    Rpc.asyncData('rpc.UvmContext.forceTimeSync').then(function(result, ex){
                        Ext.MessageBox.hide();
                        if (ex) { Util.handleException(ex); return; }
                        if (result !== 0) {
                            Util.handleException('Time synchronization failed. Return code:'.t() + ' ' + result);
                        } else {
                            Util.successToast('Time was synchronized!');
                        }
                    });
                }
            });
    },

    syncLanguage: function () {
        Ext.MessageBox.wait('Synchronizing languages with the internet...'.t(), 'Please wait'.t());
        Rpc.asyncData('rpc.UvmContext.forceTimeSync').then(function(result, ex){
           document.location.reload();
        });
    },

    saveSettings: function () {
        var me = this, v = me.getView(),
            vm = me.getViewModel();

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
        languageSettings['source'] = languageSplit[0];
        languageSettings['language'] = languageSplit[1];
        vm.set('languageSettings', languageSettings);

        Ext.Deferred.sequence([
            Rpc.asyncPromise('rpc.languageManager.setLanguageSettings', vm.get('languageSettings')),
            Rpc.asyncPromise('rpc.systemManager.setSettings', vm.get('systemSettings')),
            Rpc.asyncPromise('rpc.systemManager.setTimeZone', vm.get('timeZone')),
            Rpc.asyncPromise('rpc.appManager.app("shield").setSettings', vm.get('shieldSettings')),
            Rpc.asyncPromise('rpc.appManager.app("smtp").setSettings', vm.get('smtpSettings')),
            Rpc.asyncPromise('rpc.appManager.app("http").setSettings', vm.get('httpSettings')),
            Rpc.asyncPromise('rpc.appManager.app("ftp").setSettings', vm.get('ftpSettings'))
            ], this).then(function () {
                if(Util.isDestroyed(me, v, vm)){
                    return;
                }
                v.setLoading(false);
                me.loadSettings();
                Util.successToast('System settings saved!');
                Ext.fireEvent('resetfields', v);
                if(vm.get('localizationChanged') == true){
                    window.location.reload();
                }
        }, function (ex) {
            Util.handleException(ex);
            if(Util.isDestroyed(me, v, vm)){
                return;
            }
            v.setLoading(false);
        });
    },

    // Support methods
    downloadSystemLogs: function () {
        var downloadForm = document.getElementById('downloadForm');
        downloadForm.type.value = 'SystemSupportLogs';
        downloadForm.submit();
    },

    manualReboot: function () {
        Ext.MessageBox.confirm('Manual Reboot Warning'.t(),
            Ext.String.format('The server is about to manually reboot.  This will interrupt normal network operations until the {0} Server is finished automatically restarting. This may take up to several minutes to complete.'.t(), rpc.companyName),
            function (btn) {
                if (btn === 'yes') {
                    rpc.UvmContext.rebootBox(function (result, ex) {
                        if (ex) { console.error(ex); Util.handleException(Ext.String.format('Error: Unable to reboot {0} Server', rpc.companyName)); return; }
                        Ext.MessageBox.wait(
                            Ext.String.format('The {0} Server is rebooting.'.t(), rpc.companyName),
                            'Please wait'.t(), {
                                interval: 20, //bar will move fast!
                                increment: 500,
                                animate: true,
                                text: ''
                            });
                    });
                }
            });
    },

    manualShutdown: function () {
        Ext.MessageBox.confirm('Manual Shutdown Warning'.t(),
            Ext.String.format('The {0} Server is about to shutdown.  This will stop all network operations.'.t(), rpc.companyName),
            function (btn) {
                if (btn === 'yes') {
                    rpc.UvmContext.shutdownBox(function (result, ex) {
                        if (ex) { console.error(ex); Util.handleException(Ext.String.format('Error: Unable to shutdown {0} Server', rpc.companyName)); return; }
                        Ext.MessageBox.wait(
                            Ext.String.format('The {0} Server is shutting down.'.t(), rpc.companyName),
                            'Please wait'.t(), {
                                interval: 20,
                                increment: 500,
                                animate: true,
                                text: ''
                            });
                    });
                }
            });
    },

    factoryDefaults: function () {
        Ext.MessageBox.confirm('Reset to Factory Defaults Warning'.t(),
            'This will RESET ALL SETTINGS to factory defaults. ALL current settings WILL BE LOST.'.t(),
            function (btn) {
                if (btn === 'yes') {
                    // Ung.MetricManager.stop(); stop metrics
                    Ext.MessageBox.wait('Resetting to factory defaults...'.t(), 'Please wait'.t(), {
                        interval: 20,
                        increment: 500,
                        animate: true,
                        text: ''
                    });
                    rpc.execManager.exec(function (result, ex) {
                        Ext.MessageBox.hide();
                        if (ex) { console.error(ex); Util.handleException(ex); return; }
                        Ext.MessageBox.alert(
                            'Factory Defaults'.t(),
                            'All settings have been reset to factory defaults.', console.log('reload homepage'));
                    }, 'nohup /usr/share/untangle/bin/ut-factory-defaults');
                }
            });
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
    },

    statics: {
        shieldActionRenderer: function(value){
            var action;
            switch (value) {
                case 'SCAN': action = 'Scan'.t(); break;
                case 'PASS': action = 'Pass'.t(); break;
                default: action = 'Unknown Action'.t() + ': ' + value;
            }
            return action;
        }

    }

});
