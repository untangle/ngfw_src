Ext.define('Webui.phish-blocker.settings', {
    extend:'Ung.AppWin',
    lastUpdate: null,
    lastCheck: null,
    signatureVersion: null,
    smtpData: null,
    spamData: null,
    emailPanel: null,
    webPanel: null,
    gridEmailEventLog: null,
    getAppSummary: function() {
        return i18n._("Phish Blocker detects and blocks phishing emails using signatures.");
    },
    initComponent: function() {
        this.lastUpdate = this.getRpcApp().getLastUpdate();
        this.lastCheck = this.getRpcApp().getLastUpdateCheck();
        this.signatureVer = this.getRpcApp().getSignatureVersion();

        this.buildEmail();
        this.buildTabPanel([this.emailPanel]);
        this.callParent(arguments);
    },
    lookup: function(needle, haystack1, haystack2) {
        for (var i = 0; i < haystack1.length; i++) {
            if (haystack1[i] != undefined && haystack2[i] != undefined) {
                if (needle == haystack1[i]) {
                    return haystack2[i];
                }
                if (needle == haystack2[i]) {
                    return haystack1[i];
                }
            }
        }
        return null;
    },
    // Email Config Panel
    buildEmail: function() {
        this.smtpData = [['MARK', i18n._('Mark')], ['PASS', i18n._('Pass')],
                ['DROP', i18n._('Drop')], ['QUARANTINE', i18n._('Quarantine')]];
        this.spamData = [['MARK', i18n._('Mark')], ['PASS', i18n._('Pass')]];
        this.emailPanel = Ext.create('Ext.panel.Panel',{
            title: i18n._('Email'),
            name: 'Email',
            helpSource: 'phish_blocker_email',
            autoScroll: true,
            cls: 'ung-panel',
            items: [{
                xtype: 'fieldset',
                title: i18n._('SMTP'),
                defaults: {
                    width: 210
                },
                items: [{
                    xtype: 'checkbox',
                    boxLabel: i18n._('Scan SMTP'),
                    name: 'Scan SMTP',
                    hideLabel: true,
                    checked: this.settings.smtpConfig.scan,
                    handler: Ext.bind(function(elem, newValue) {
                        this.settings.smtpConfig.scan = newValue;
                    }, this)
                }, {
                    xtype: 'combo',
                    name: 'SMTP Action',
                    editable: false,
                    store:this.smtpData,
                    valueField: 'key',
                    displayField: 'name',
                    fieldLabel: i18n._('Action'),
                    queryMode: 'local',
                    value: this.settings.smtpConfig.msgAction,
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, newValue) {
                                this.settings.smtpConfig.msgAction = newValue;
                            }, this)
                        }
                    }
                }]
            }, {
                xtype: 'fieldset',
                title: i18n._('Note'),
                html: i18n._('Phish Blocker email signatures were last updated') + ":&nbsp;&nbsp;&nbsp;&nbsp;" +
                    (this.lastUpdate != null && this.lastUpdate.time != 0 ? i18n.timestampFormat(this.lastUpdate): i18n._("never"))
            }]
        });
    },
    afterSave: function()  {
        this.lastUpdate = this.getRpcApp().getLastUpdate();
        this.lastCheck = this.getRpcApp().getLastUpdateCheck();
        this.signatureVer = this.getRpcApp().getSignatureVersion();
    }
});
//# sourceURL=phish-blocker-settings.js