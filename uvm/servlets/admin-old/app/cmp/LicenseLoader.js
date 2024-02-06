Ext.define('Ung.cmp.LicenseLoader', {
    alias: 'widget.licenseloader',

    // update interval in millisecond
    updateFrequency: 60000,
    //how many times to check (50 times x 1 minute = 50 minutes)
    count: 50,
    started: false,
    intervalId: null,
    check: function() {
        this.count = 50;
        if(!this.started) {
            this.intervalId = window.setInterval(function() {Ung.LicenseLoader.run();}, this.updateFrequency);
            this.started = true;
        }
        return true;
    },
    stop: function() {
        if (this.intervalId !== null) {
            window.clearInterval(this.intervalId);
        }
        this.started = false;
    },
    run: function () {
        this.count--;
        if(this.count>0) {
            Ung.Main.reloadLicenses();
        } else {
            this.stop();
        }
    }
});

Ung.LicenseLoader = new Ung.cmp.LicenseLoader();