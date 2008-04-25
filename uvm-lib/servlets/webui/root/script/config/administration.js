if(!Ung.hasResource["Ung.Administration"]) {
Ung.hasResource["Ung.Administration"]=true;

Ung.Administration = Ext.extend(Ung.ConfigWin, {
    panelAdministration: null,
    panelPublicAddress: null,
    panelCertificates: null,
    panelMonitoring: null,
    panelSkins: null,
	initComponent: function() {
		this.title=i18n._('Administration');
		Ung.Administration.superclass.initComponent.call(this);
	},
    
    onRender: function(container, position) {
    	//call superclass renderer first
    	Ung.Administration.superclass.onRender.call(this,container, position);
    	this.initSubCmps.defer(1, this);
		//builds the 2 tabs
    },
    initSubCmps: function() {
		this.buildAdministration();
		this.buildPublicAddress();
		this.buildCertificates();
		this.buildMonitoring();
		this.buildSkins();
		//builds the tab panel with the tabs
		this.buildTabPanel([this.panelAdministration,this.panelPublicAddress,this.panelCertificates,this.panelMonitoring,this.panelSkins]);
    	
    },
    getTODOPanel: function(title) {
    	return new Ext.Panel({
		    title: this.i18n._(title),
		    layout: "form",
		    bodyStyle:'padding:5px 5px 0',
			items: [{
	            xtype:'fieldset',
	            title: this.i18n._(title),
	            autoHeight:true,
	            items :[
	            	{
						xtype:'textfield',
						fieldLabel: this.i18n._('TODO'),
	                    name: 'todo',
	                    allowBlank:false,
	                    value: 'todo',
	                    disabled: true
	                }]
			}]
    	});
    },
    buildAdministration: function() {
    	this.panelAdministration=this.getTODOPanel("Administration");
    },
    buildPublicAddress: function() {
    	this.panelPublicAddress=this.getTODOPanel("Public Address");
    },
    buildCertificates: function() {
    	this.panelCertificates=this.getTODOPanel("Certificates");
    },
    buildMonitoring: function() {
    	this.panelMonitoring=this.getTODOPanel("Monitoring");
    },
    buildSkins: function() {
    	this.panelSkins=this.getTODOPanel("Skins");
    }
});

}