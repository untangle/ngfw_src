if (!Ung.hasResource["Ung.Network"]) {
    Ung.hasResource["Ung.Network"] = true;

    Ext.define("Ung.Network", {
        extend: "Ung.ConfigWin",
        panelYay: null,
        initComponent: function() {
            this.breadcrumbs = [{
                title: i18n._("Configuration"),
                action: Ext.bind(function() {
                    this.cancelAction();
                }, this)
            }, {
                title: i18n._('System Info')
            }];
            this.buildYay();
            
            // builds the tab panel with the tabs
            var pageTabs = [this.panelYay];
            this.buildTabPanel(pageTabs);
            this.callParent(arguments);
        },
        buildYay: function() {
            this.panelYay = Ext.create('Ext.panel.Panel',{
                name: 'Yay',
                helpSource: 'yay',
                parentId: this.getId(),
                title: this.i18n._('Yay'),
                cls: 'ung-panel',
                autoScroll: true,
                html: this.i18n._('Yay')
            });
        }
    });
}
//@ sourceURL=network.js
