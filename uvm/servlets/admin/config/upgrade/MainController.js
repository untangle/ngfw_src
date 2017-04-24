Ext.define('Ung.config.upgrade.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.config-upgrade',

    control: {
        '#': {
            afterrender: 'loadSettings'
        }
    },

    loadSettings: function (view) {

        // view.getViewModel().bind('{settings.autoUpgradeHour}', function (value) {
        //     console.log(value);
        // });

        view.down('progressbar').wait({
            interval: 500,
            text: 'Checking for upgrades...'.t()
        });
        this.checkUpgrades();
        console.log(view.getViewModel().get('settings'));
    },

    saveSettings: function () {
        var me = this;
        console.log(me.getViewModel().get('settings'));
        me.getView().setLoading('Saving ...');
        rpc.systemManager.setSettings(function (result, ex) {
            me.getView().setLoading(false);
            if (ex) { Util.handleException(ex); return; }
            Util.successToast('Upgrade Settings'.t() + ' saved!');
        }, me.getViewModel().get('settings'));

    },

    checkUpgrades: function () {
        var v = this.getView();
        Rpc.asyncData('rpc.systemManager.upgradesAvailable').then(function (result) {
            console.log(result);
            if(result) {
                var upgradeButton = v.down('[name="upgradeButton"]');
                upgradeButton.show();
            } else {
                var upgradeText = v.down('[name="upgradeText"]');
                upgradeText.show();
            }
            v.down('progressbar').reset();
            v.down('progressbar').hide();
        });
    },

    downloadUpgrades: function() {
        var me = this;
        Ext.MessageBox.progress("Downloading Upgrade...".t(), ".");
        this.checkDownloadStatus=true;

        Rpc.asyncData('rpc.systemManager.downloadUpgrades').then(function(result) {
            me.checkDownloadStatus=false;

            console.log("rpc.systemManager.downloadUpgrades()", result);
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

        Rpc.asyncData('rpc.systemManager.getDownloadStatus').then(function(result) {
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

        console.log("Applying Upgrades...");

        Ext.MessageBox.wait("Please wait".t(), "Launching Upgrade...".t(), {
            interval: 1000,
            increment: 200,
            duration: 180000
        });

        rpc.systemManager.upgrade(Ext.bind(function (result, exception) {
            // the upgrade will shut down the untangle-vm so often this returns an exception
            // either way show a wait dialog...

            Ext.MessageBox.hide();
            var applyingUpgradesWindow = Ext.create('Ext.window.MessageBox', {
                minProgressWidth: 360
            });

            // the untangle-vm is shutdown, just show a message dialog box for 45 seconds so the user won't poke at things.
            // then refresh browser.
            applyingUpgradesWindow.wait("Please wait".t(), "Upgrade in Progress...".t(), {
                interval: 1000,
                increment: 200,
                duration: 180000,
                scope: this,
                fn: function () {
                    console.log("Upgrade in Progress. Press ok to go to the Start Page...");
                    applyingUpgradesWindow.hide();
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

    onUpgradeTimeChange: function (field, value) {
        this.getViewModel().set('settings.autoUpgradeHour', value.getHours());
        this.getViewModel().set('settings.autoUpgradeMinute', value.getMinutes());
    }

});
