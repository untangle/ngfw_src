if(!Ung.hasResource["Ung.Administration"]) {
Ung.hasResource["Ung.Administration"]=true;

Ung.Administration = Ext.extend(Ung.ConfigWin, {
    panelAdministration: null,
    panelPublicAddress: null,
    panelCertificates: null,
    panelMonitoring: null,
    panelSkins: null,
	initComponent: function() {
		this.breadcrumbs=[{title:i18n._("Configuration"), action: function(){
                this.cancelAction();
            }.createDelegate(this) },
            {title:i18n._('Administration')}];
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
		this.tabs.activate(this.panelSkins);
		this.panelAdministration.disable();
		this.panelPublicAddress.disable();
		this.panelCertificates.disable();
		this.panelMonitoring.disable();
    	
    },
    // get base settings object
	getSkinSettings: function(forceReload) {
		if(forceReload || this.rpc.skinSettings===undefined) {
			this.rpc.skinSettings=rpc.skinManager.getSkinSettings();
		}
		return this.rpc.skinSettings;
	},
    getTODOPanel: function(title) {
    	return new Ext.Panel({
		    title: this.i18n._(title),
		    layout: "form",
		    bodyStyle:'padding:5px 5px 0px 5px;',
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
    	var skinsStore = new Ext.data.Store({
				        proxy: new Ung.RpcProxy(rpc.skinManager.getSkinsList, false),
				        reader: new Ung.JsonListReader({
				        	root: 'list',
					        fields: ['skinName']
						})
					});	
    
    	this.panelSkins = new Ext.Panel({
    		//private fields
			parentId: this.getId(),
		    title: this.i18n._('Skins'),
		    layout: "form",
		    bodyStyle:'padding:5px 5px 0px 5px;',
		    autoScroll: true,
		    defaults: {
	            xtype:'fieldset',
	            autoHeight:true,
	            buttonAlign: 'left'
		    },
			items: [
			{
	            title: this.i18n._('Administration Skin'),
				items: [{
                    bodyStyle:'padding:0px 0px 5px 5px;',
                    border: false,
					html: this.i18n._("This skin will used in the administration client")
				}, {
                    xtype:'combo',
					name: "administrationClientSkin",
			 		store: skinsStore,	
					displayField: 'skinName',
					valueField: 'skinName',
                    value: this.getSkinSettings().administrationClientSkin,
				    typeAhead: true,
				    mode: 'remote',
				    triggerAction: 'all',
				    listClass: 'x-combo-list-small',
				    selectOnFocus:true,
				    hideLabel: true,
					listeners: {
						"change": {
							fn: function(elem, newValue) {
								this.getSkinSettings().administrationClientSkin=newValue;
                                Ext.MessageBox.alert(this.i18n._("Info"), this.i18n._("Please note that you have to relogin after saving for the new skin to take effect."));
							}.createDelegate(this)
						}
					}
				}]
			}, {
                title: this.i18n._('Block Page Skin'),
                items: [{
                    bodyStyle:'padding:0px 0px 5px 5px;',
                    border: false,
                    html: this.i18n._("This skin will used in the user pages like quarantine and block pages")
                }, {
                    xtype:'combo',
                    name: "userPagesSkin",
                    store: skinsStore,  
                    displayField: 'skinName',
                    valueField: 'skinName',
                    value: this.getSkinSettings().userPagesSkin,
                    typeAhead: true,
                    mode: 'remote',
                    triggerAction: 'all',
                    listClass: 'x-combo-list-small',
                    selectOnFocus:true,
                    hideLabel: true,
                    listeners: {
                        "change": {
                            fn: function(elem, newValue) {
                                this.getSkinSettings().userPagesSkin=newValue;
                            }.createDelegate(this)
                        }
                    }
                }]
            }, {
	            title: this.i18n._('Upload New Skin'),
	            items :
					{
			            fileUpload:true,
			            xtype:'form',
			            id:'upload_skin_form',
			            url: 'upload',
			            border: false,
			            items:[{
			                fieldLabel: 'File', 
			                name: 'file', 
			                inputType: 'file', 
			                xtype:'textfield', 
			                allowBlank:false 
			            },{
			             xtype:'hidden',
			             name: 'type',
			             value: 'skin'
			            }]
					},				
	            buttons :[
	            	{
						text: this.i18n._("Upload"),
						handler: function() {this.panelSkins.onUpload();}.createDelegate(this)
	            	}
	            ]
			}],
            onUpload : function() {
				var prova = Ext.getCmp('upload_skin_form');
				var cmp = Ext.getCmp(this.parentId); 

				var form = prova.getForm();
				form.submit( {
					parentId: cmp.getId(),
					waitMsg: cmp.i18n._('Please wait while your skin is uploaded...'),
					success: function(form,action) { 
                        skinsStore.load();
						var cmp = Ext.getCmp(action.options.parentId);						
						Ext.MessageBox.alert(cmp.i18n._("Successed"), cmp.i18n._("Upload Skin Successed"));
					},
					failure: function(form,action) {
						var cmp = Ext.getCmp(action.options.parentId); 
						if (action.result.msg) {
							Ext.MessageBox.alert(cmp.i18n._("Failed"), cmp.i18n._(action.result.msg));
						} else {
							Ext.MessageBox.alert(cmp.i18n._("Failed"), cmp.i18n._("Upload Skin Failed")); 
						}
					}
				});
			}
    	});
    },
    // save function
    saveAction: function() {
        if (this.validate()) {
            //disable tabs during save
            this.tabs.disable();
            rpc.skinManager.setSkinSettings(function (result, exception) {
                //re-enable tabs
                this.tabs.enable();
                if(exception) {Ext.MessageBox.alert(i18n._("Failed"),exception.message); return;}
                //exit settings screen
                this.cancelAction();
            }.createDelegate(this),
                    this.getSkinSettings());
        }
    }
    
});

}