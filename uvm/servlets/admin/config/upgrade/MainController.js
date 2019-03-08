Ext.define('Ung.config.upgrade.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.config-upgrade',

    control: {
        '#': {
            afterrender: 'onAfterRender'
        }
    },

    onAfterRender: function() {
        var me = this, view = me.getView(),
            daysCombo = view.down('#dayscombo'),
            daysGroup = view.down('#daysgroup');

        // bind once to set the initial upgrade days/hours
        me.getViewModel().bind('{settings}', function (settings) {
            var autoUpgradeDays = settings.autoUpgradeDays,
                upgradeTime = new Date(); // used for setting upgrade hours

            if (autoUpgradeDays === 'any' || autoUpgradeDays === 'all') {
                daysCombo.setValue(autoUpgradeDays);
            } else {
                daysCombo.setValue('specific');
                daysGroup.setValue(autoUpgradeDays);
            }

            upgradeTime.setHours(settings.autoUpgradeHour || 0);
            upgradeTime.setMinutes(settings.autoUpgradeMinute || 0);
            view.down('timefield').setValue(upgradeTime);
        }, me, {
            single: true
        });

        view.down('progressbar').wait({
            interval: 500,
            text: 'Checking for upgrades...'.t()
        });

        me.loadSettings();
        me.checkUpgrades();

    },

    loadSettings: function () {
        var me = this,
            v = me.getView(),
            vm = me.getViewModel();

        me.getView().setLoading(true);
        Rpc.asyncData('rpc.systemManager.getSettings')
            .then(function(result){
                if(Util.isDestroyed(me, v, vm)){
                    return;
                }

                vm.set('settings', result);

                vm.set('panel.saveDisabled', false);
                v.setLoading(false);
            }, function(ex) {
                if(!Util.isDestroyed(v, vm)){
                    vm.set('panel.saveDisabled', true);
                    v.setLoading(false);
                }
            });
    },

    saveSettings: function () {
        var me = this, v = me.getView(),
            vm = me.getViewModel(), settings = vm.get('settings');

        if (settings.autoUpgrade) {
            if (settings.autoUpgradeDays === '') {
                Ext.Msg.alert('Warning!', 'Please set automatic upgrade days schedule!');
                return;
            }
        }
        v.setLoading(true);

        Rpc.asyncData('rpc.systemManager.setSettings', settings)
            .then(function () {
                if(Util.isDestroyed(me, v, vm)){
                    return;
                }
                me.loadSettings();
                Util.successToast('Upgrade Settings'.t() + ' saved!');
                Ext.fireEvent('resetfields', v);
            }, function(ex) {
                if(!Util.isDestroyed(v, vm)){
                    vm.set('panel.saveDisabled', true);
                    v.setLoading(false);
                }
            });
    },

    checkUpgrades: function () {
        var view = this.getView();

        setTimeout( function(){
            Rpc.asyncData('rpc.systemManager.upgradesAvailable')
            .then(function (result) {
                if(Util.isDestroyed(view)){
                    return;
                }
                if(result) {
                    var upgradeButton = view.down('[name="upgradeButton"]');
                    if (upgradeButton)
                        upgradeButton.show();
                } else {
                    var upgradeText = view.down('[name="upgradeText"]');
                    if (upgradeText)
                        upgradeText.show();
                }
                var progressbar = view.down('progressbar');
                if (progressbar) {
                    progressbar.reset();
                    progressbar.hide();
                }
            });
        }, 100);
    },

    downloadUpgrades: function() {
        var me = this;
        Ext.MessageBox.progress("Downloading Upgrade...".t(), ".");
        this.checkDownloadStatus=true;

        Rpc.asyncData('rpc.systemManager.downloadUpgrades')
        .then(function(result) {
            if(Util.isDestroyed(me)){
                return;
            }
            me.checkDownloadStatus=false;

            Ext.MessageBox.hide();
            if(result) {
                me.upgrade();
            } else {
                Ext.MessageBox.alert("Warning".t(), "Downloading upgrades failed.".t());
            }
        });
        me.getDownloadStatus();
    },

    getDownloadStatus: function() {
        var me = this;
        if(!me.checkDownloadStatus) {
            return;
        }

        Rpc.asyncData('rpc.systemManager.getDownloadStatus')
        .then(function(result) {
            if(Util.isDestroyed(me)){
                return;
            }
            if(!me.checkDownloadStatus) {
                return;
            }
            var text=Ext.String.format("Package".t() + ": {0} / {1}" + "<br/>" + "Speed".t() + ": {2} ",
                                       result.downloadCurrentFileCount,
                                       result.downloadTotalFileCount,
                                       result.downloadCurrentFileRate);
            if(!Ext.MessageBox.isVisible()) {
                Ext.MessageBox.progress("Downloading Upgrade...".t(), text);
            }
            var downloadCurrentFileProgress = 0;
            if(result.downloadCurrentFileProgress!=null && result.downloadCurrentFileProgress.length>0) {
                try {
                    downloadCurrentFileProgress = parseFloat(result.downloadCurrentFileProgress.replace("%",""))/100;
                } catch (e) {
                    }
            }
            var downloadCurrentFileIndex = parseFloat(result.downloadCurrentFileCount);
            if(downloadCurrentFileIndex > 0) {
                downloadCurrentFileIndex = downloadCurrentFileIndex - 1;
            }
            var currentPercentComplete = (downloadCurrentFileIndex + downloadCurrentFileProgress) / parseFloat(result.downloadTotalFileCount != 0 ? result.downloadTotalFileCount: 1);
            //console.log("downloadCurrentFileProgress:", downloadCurrentFileProgress,"currentPercentComplete",currentPercentComplete,"downloadCurrentFileCount",result.downloadCurrentFileCount);
            var progressIndex = parseFloat(0.99 * currentPercentComplete);
            Ext.MessageBox.updateProgress(progressIndex, "", text);
            window.setTimeout( Ext.bind(me.getDownloadStatus, me), 500 );
        });
    },

    upgrade: function () {
        // start ignoring all exceptions
        Util.ignoreExceptions = true;

        Ext.MessageBox.wait("Please wait".t(), "Launching Upgrade...".t(), {
            interval: 1000,
            increment: 200,
            duration: 180000
        });

        rpc.systemManager.upgrade(Ext.bind(function (result, exception) {
            // the upgrade will shut down the untangle-vm so often this returns an exception
            // either way show a wait dialog...
            Ext.MessageBox.hide();

            // the untangle-vm is shutdown, just show a message dialog box for 45 seconds so the user won't poke at things.
            // then refresh browser.
            Ext.MessageBox.wait("Please wait".t(), "Processing Upgrade...".t(), {
                interval: 1000,
                increment: 200,
                duration: 180000,
                scope: this,
                fn: function () {
                    console.log("Upgrade in Progress. Press ok to go to the Start Page...");
                    Ext.MessageBox.hide();
                    Ext.MessageBox.alert(
                        "Upgrade in Progress".t(),
                        "The upgrades have been downloaded and are now being applied.".t() + "<br/>" +
                            "<strong>" + "DO NOT REBOOT AT THIS TIME.".t() + "</strong>" + "<br/>" +
                            "Please be patient this process will take a few minutes.".t() + "<br/>" +
                            "After the upgrade is complete you will be able to log in again.".t(),
                        Util.goToStartPage
                    );
                }
            });
        }, this));
    },

    onDaysComboChange: function (combo, value) {
        var me = this, vm = me.getViewModel(), daysGroup = me.getView().down('#daysgroup');
        if (value !== 'specific') {
            vm.set('settings.autoUpgradeDays', value);
        } else {
            daysGroup.setValue('1,2,3,4,5,6,7');
        }
    },

    onDaysGroupChange: function (cmp, value) {
        this.getViewModel().set('settings.autoUpgradeDays', value);
    },

    onUpgradeTimeChange: function (field, value) {
        var vm = this.getViewModel();
        vm.set('settings.autoUpgradeHour', value.getHours());
        vm.set('settings.autoUpgradeMinute', value.getMinutes());
    }

});
