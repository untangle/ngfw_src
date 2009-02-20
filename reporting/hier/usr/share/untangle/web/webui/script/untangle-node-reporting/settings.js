if (!Ung.hasResource["Ung.Reporting"]) {
    Ung.hasResource["Ung.Reporting"] = true;
    Ung.NodeWin.registerClassName('untangle-node-reporting', 'Ung.Reporting');

    Ung.Reporting = Ext.extend(Ung.NodeWin, {
        layout: 'fit',
        initComponent : function(container, position) {
            // builds the tab panel with the tabs
            var url="../reports/new.jsp";
            this.items={
                autoWidth : true,
                border: false,
                html: '<iframe id="reports_iframe" src="'+url+'" name="reports_iframe" width="100%" height="100%" frameborder="0"/>'
            };
            Ung.Reporting.superclass.initComponent.call(this);
        }
    });
}