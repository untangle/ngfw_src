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
        rpc.systemManager.upgradesAvailable(function (result, ex) {
            if (ex) { Util.handleException(ex); return; }
            console.log(result);
            if(result) {
                var upgradeButton = v.down('[name="upgradeButton"]');
                upgradeButton.show();
            }
            v.down('progressbar').reset();
            v.down('progressbar').hide();
        });
    },

    downloadUpgrades: function() {
        Ext.MessageBox.progress("Downloading packages... Please wait".t(), ".");
        this.checkDownloadStatus=true;

        rpc.systemManager.downloadUpgrades(Ext.bind(function(result, ex) {
            if (ex) { Util.handleException(ex); return; }
            this.checkDownloadStatus=false;

            console.log("rpc.systemManager.downloadUpgrades()", result);
            Ext.MessageBox.hide();
            if(result) {
                this.upgrade();
            } else {
                Ext.MessageBox.alert("Warning".t(), "Downloading upgrades failed.".t());
            }
        }, this));
        this.getDownloadStatus();
    },

    getDownloadStatus: function() {
        if(!this.checkDownloadStatus) {
            return;
        }
        rpc.systemManager.getDownloadStatus(Ext.bind(function(result, ex) {
            if (ex) { Util.handleException(ex); return; }
            if(!this.checkDownloadStatus) {
                return;
            }
            var text=Ext.String.format("Package: {0} of {1}<br/>Speed: {2}".t(),
                       result.downloadCurrentFileCount,
                       result.downloadTotalFileCount,
                       result.downloadCurrentFileRate);
            if(!Ext.MessageBox.isVisible() || Ext.MessageBox.title!=this.msgTitle) {
                Ext.MessageBox.progress(this.msgTitle, text);
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
            window.setTimeout( Ext.bind(this.getDownloadStatus, this), 500 );
        }, this));
    },

    upgrade: function () {
        // start ignoring all exceptions
        Util.ignoreExceptions = true;

        console.log("Applying Upgrades...");

        Ext.MessageBox.wait({
            title: "Please wait".t(),
            msg: "Applying Upgrades...".t()
        });

        var doneFn = Ext.bind(function () {
        }, this);

        rpc.systemManager.upgrade(Ext.bind(function (result, exception) {
            // the upgrade will shut down the untangle-vm so often this returns an exception
            // either way show a wait dialog...

            Ext.MessageBox.hide();
            var applyingUpgradesWindow = Ext.create('Ext.window.MessageBox', {
                minProgressWidth: 360
            });

            // the untangle-vm is shutdown, just show a message dialog box for 45 seconds so the user won't poke at things.
            // then refresh browser.
            applyingUpgradesWindow.wait("Applying Upgrades...".t(), "Please wait".t(), {
                interval: 500,
                increment: 120,
                duration: 120000,
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
