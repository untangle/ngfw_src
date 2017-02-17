Ext.define('Ung.config.system.SystemController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.config.system',

    control: {
        '#regional': {
            afterrender: 'loadRegional'
        }
    },

    loadRegional: function (view) {
        this.getTime();
        var vm = view.getViewModel(),
            timeZones = [];
        eval(rpc.systemManager.getTimeZones()).forEach(function (tz) {
            timeZones.push({name: '(' + tz[1] + ') ' + tz[0], value: tz[0]})
        });
        vm.set('timeZones', timeZones);
    },


    // Regional
    getTime: function () {
        var vm = this.getView().down('#regional').getViewModel();
        rpc.systemManager.getDate(function (result, ex) {
            if (ex) { console.error(ex); Ung.Util.exceptionToast(ex); return; }
            vm.set('time', result);
        });
    },

    // getTimeZone: function () {
    //     var tz = rpc.systemManager.getTimeZone();
    //     if (tz && typeof tz !== 'string' ) {
    //         tz = tz.ID;
    //     }
    //     // this.rpc.timeZone = tz;
    // },

    syncTime: function () {
        var me = this;
        Ext.MessageBox.confirm(
            'Force Time Synchronization'.t(),
            'Forced time synchronization can cause problems if the current date is far in the future.'.t() + '<br/>' +
            'A reboot is suggested after time sychronization.'.t() + '<br/><br/>' +
            'Continue?'.t(),
            function(btn) {
                if (btn === 'yes') {
                    Ext.MessageBox.wait('Syncing time with the internet...'.t(), 'Please wait'.t());
                    rpc.UvmContext.forceTimeSync(function (result, ex) {
                        Ext.MessageBox.hide();
                        if (ex) { console.error(ex); Ung.Util.exceptionToast(ex); return; }
                        if (result !== 0) {
                            Ung.Util.exceptionToast('Time synchronization failed. Return code:'.t() + ' ' + result);
                        } else {
                            me.getTime();
                            Ung.Util.successToast('Time was synchronized!');
                        }
                    });
                }
            });
    },


    saveSettings: function () {
        var v = this.getView();
        v.setLoading('Saving...');
        Ext.Deferred.sequence([
            this.setLanguage,
            this.setSystem,
            this.setTimezone,
            this.setDate
        ], this).then(function () {
            v.setLoading(false);
            Ung.Util.successToast('System settings saved!');
        }, function (ex) {
            v.setLoading(false);
            console.error(ex);
            Ung.Util.exceptionToast(ex);
        });
    },

    setLanguage: function () {
        console.log('Saving Language...');
        var deferred = new Ext.Deferred(),
            vm = this.getView().down('#regional').getViewModel();

        if (vm.get('languageSettings.regionalFormats') === 'default') {
            // reset overrides
            vm.set('languageSettings.overrideDateFmt', '');
            vm.set('languageSettings.overrideDecimalSep', '');
            vm.set('languageSettings.overrideThousandSep', '');
            vm.set('languageSettings.overrideTimestampFmt', '');
        }
        rpc.languageManager.setLanguageSettings(function (result, ex) { if (ex) { deferred.reject(ex); } deferred.resolve(); }, vm.get('languageSettings'));
        return deferred.promise;
    },

    setSystem: function () {
        console.log('Saving System...');
        var deferred = new Ext.Deferred(),
            vm = this.getView().down('#regional').getViewModel();
        rpc.systemManager.setSettings(function (result, ex) { if (ex) { deferred.reject(ex); } deferred.resolve(); }, vm.get('systemSettings'));
        return deferred.promise;
    },

    setTimezone: function () {
        console.log('Saving Timezone...');
        var deferred = new Ext.Deferred(),
            vm = this.getView().down('#regional').getViewModel();
        rpc.systemManager.setTimeZone(function (result, ex) { if (ex) { deferred.reject(ex); } deferred.resolve(); }, vm.get('tz'));
        return deferred.promise;
    },

    setDate: function () {
        console.log('Saving Date...');
        var deferred = new Ext.Deferred(),
            v = this.getView().down('#regional');
        rpc.systemManager.setDate(function (result, ex) { if (ex) { deferred.reject(ex); } deferred.resolve(); }, new Date(v.down('datefield').getValue()).getTime());
        return deferred.promise;
    }

    // setTimezone: function () {
    //     console.log('Saving Timezone...');
    //     var deferred = new Ext.Deferred(),
    //         vm = this.getView().down('#regional').getViewModel();
    //     rpc.systemManager.setTimeZone(function (result, ex) { if (ex) { deferred.reject(ex); } deferred.resolve(); }, vm.get('tz'));
    //     return deferred.promise;
    // }


});