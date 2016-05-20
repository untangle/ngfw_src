Ext.define('Webui.config.offline', {
    extend: 'Ung.StatusWin',
    name: 'offline',
    helpSource: 'offline',
    doSize: function() {
        var objSize = Ung.Main.viewport.getSize();
        var width = Math.min(770, objSize.width);
        var height = Math.min(650, objSize.height);
        var x = Math.round((objSize.width - width)/2);
        var y = Math.round((objSize.height-height)/2);
        this.setPosition(x, y);
        this.setSize({width:width, height: height});
    },
    initComponent: function() {
        this.breadcrumbs = [{
            title: i18n._('Offline')
        }];
        
        this.items = {
            xtype: 'panel',
            layout: { type: 'vbox', align: 'stretch'},
            items: [{
                xtype: 'container',
                layout: 'center',
                items: {
                    xtype: 'component',
                    html: '<img src="/images/BrandingLogo.png?'+(new Date()).getTime()+'" border="0"/>',
                    width: 166,
                    height: 100
                },
                height: 118
            }, {
                xtype: 'container',
                flex: 1,
                autoScroll: true,
                items: [{
                    xtype: 'component',
                    html: i18n._("Welcome!"),
                    margin: '10 20 0 150'
                }, {
                    xtype: 'component',
                    html: i18n._("The installation is complete and ready for deployment. The next step are registration and installing apps from the App Store."),
                    margin: '10 20 0 150'
                }, {
                    xtype: 'component',
                    cls: 'warning',
                    html: i18n._("Unfortunately, Your server was unable to contact the App Store."),
                    margin: '10 20 0 150'
                }, {
                    xtype: 'component',
                    html: i18n._("Before Installing apps, this must be resolved."),
                    margin: '10 20 0 150'
                }, {
                    xtype: 'component',
                    html: "<b>"+i18n._("Possible Resolutions")+"</b>",
                    margin: '10 20 0 150'
                },{
                    xtype: 'component',
                    margin: '0 20 0 180',
                    html: "<ol><li>" + i18n._("Verify the network settings are correct and the Connectivity Test succeeds.") + "</li>"+
                        "<li>" + i18n._("Verify that there are no upstream firewalls blocking HTTP access to the internet.") + "</li>"+
                        "<li>" + i18n._("Verify the external interface has the correct IP and DNS settings.") + "</li></ol>"
                }, {
                    xtype: 'container',
                    layout: 'center',
                    height: 70,
                    items: {
                        xtype: 'button',
                        text: i18n._('Open Network Settings'),
                        padding: '7 30 7 30',
                        handler: function() {
                            this.closeWindow();
                            Ung.Main.openConfig(Ung.Main.configMap["network"]);
                        },
                        scope: this
                    }
                }]
            }]
        };
        this.callParent(arguments);
    },
    closeWindow: function() {
        this.hide();
        Ext.destroy(this);
    }
});
//# sourceURL=offline.js