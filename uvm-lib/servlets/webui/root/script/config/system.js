if(!Ung.hasResource["Ung.System"]) {
Ung.hasResource["Ung.System"]=true;

Ung.System = Ext.extend(Ung.ConfigWin, {
    panelUntangleSupport: null,
    panelBackup: null,
    panelRestore: null,
    panelProtocolSettings: null,
    panelRegionalSettings: null,
	initComponent: function() {
        this.breadcrumbs=[{title:i18n._("Configuration"), action: function(){
                this.cancelAction();
            }.createDelegate(this) },
            {title:i18n._('System')}];
		Ung.System.superclass.initComponent.call(this);
	},
    
    onRender: function(container, position) {
    	//call superclass renderer first
    	Ung.System.superclass.onRender.call(this,container, position);
    	this.initSubCmps.defer(1, this);
		//builds the 2 tabs
    },
    initSubCmps: function() {
		this.buildUntangleSupport();
		this.buildBackup();
		this.buildRestore();
		this.buildProtocolSettings();
		this.buildRegionalSettings();
		//builds the tab panel with the tabs
		this.buildTabPanel([this.panelUntangleSupport,this.panelBackup,this.panelRestore,this.panelProtocolSettings,this.panelRegionalSettings]);
        this.tabs.activate(this.panelRegionalSettings);
        this.panelUntangleSupport.disable();
        this.panelBackup.disable();
        this.panelRestore.disable();
        this.panelProtocolSettings.disable();
    	
    },
    // get languange settings object
	getLanguageSettings: function(forceReload) {
		if(forceReload || this.rpc.languageSettings===undefined) {
			this.rpc.languageSettings=rpc.languageManager.getLanguageSettings();
		}
		return this.rpc.languageSettings;
	},
    getTODOPanel: function(title) {
    	return new Ext.Panel({
		    title: this.i18n._(title),
		    layout: "form",
		    bodyStyle:'padding:5px 5px 0px 5px',
			items: [{
	            xtype:'fieldset',
	            title: this.i18n._(title),
	            autoHeight:true,
	            items :[
	            	{
						xtype:'textfield',
						fieldLabel: 'TODO',
	                    name: 'todo',
	                    allowBlank:false,
	                    value: 'todo',
	                    disabled: true
	                }]
			}]
    	});
    },
    buildUntangleSupport: function() {
    	this.panelUntangleSupport=this.getTODOPanel("Untangle Support");
    },
    buildBackup: function() {
    	this.panelBackup=this.getTODOPanel("Backup");
    },
    buildRestore: function() {
    	this.panelRestore=this.getTODOPanel("Restore");
    },
    buildProtocolSettings: function() {
    	this.panelProtocolSettings=this.getTODOPanel("Protocol Settings");
    },
    buildRegionalSettings: function() {
    	var languagesStore = new Ext.data.Store({
				        proxy: new Ung.RpcProxy(rpc.languageManager.getLanguagesList, false),
				        reader: new Ext.data.JsonReader({
				        	root: 'list',
					        fields: ['code', 'name']
						})
					});	
        
    	this.panelRegionalSettings = new Ext.Panel({
    		//private fields
			parentId: this.getId(),
		    title: this.i18n._('Regional Settings'),
		    layout: "form",
		    bodyStyle:'padding:5px 5px 0px 5px',
		    autoScroll: true,
		    defaults: {
	            xtype:'fieldset',
	            autoHeight:true,
	            buttonAlign: 'left'
		    },
			items: [
			{
	            title: this.i18n._('Language'),
				items: [{
                    id: 'system_language_combo',
                    xtype:'combo',
					name: "language",
			 		store: languagesStore,
			 		lazyInit: false,
			 		forceSelection: true,
					displayField: 'name',
					valueField: 'code',
                    //value: this.getLanguageSettings().language,
				    typeAhead: true,
				    mode: 'local',
				    triggerAction: 'all',
				    listClass: 'x-combo-list-small',
				    selectOnFocus:true,
				    hideLabel: true,
					listeners: {
						"change": {
							fn: function(elem, newValue) {
								this.getLanguageSettings().language=newValue;
								Ext.MessageBox.alert(this.i18n._("Info"), this.i18n._("Please note that you have to relogin after saving for the new language to take effect."));
							}.createDelegate(this)
						}, 
						"render": {
                            fn: function(elem) {
                                languagesStore.load({callback: function (r, options, success) {
                                    if(success) {
                                        var languageComboCmp=Ext.getCmp("system_language_combo");
                                        if(languageComboCmp) {
                                            languageComboCmp.setValue(this.getLanguageSettings().language);
                                        }
                                    }
                                }.createDelegate(this)})
                            }.createDelegate(this)
						}
					}
				}]
            }, {
	            title: this.i18n._('Upload New Language Pack'),
	            items :
					{
			            fileUpload:true,
			            xtype:'form',
			            id:'upload_language_form',
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
			             value: 'language'
			            }]
					},				
	            buttons :[
	            	{
						text: this.i18n._("Upload"),
						handler: function() {this.panelRegionalSettings.onUpload();}.createDelegate(this)
	            	}
	            ]
			}],
            onUpload : function() {
				var prova = Ext.getCmp('upload_language_form');
				var cmp = Ext.getCmp(this.parentId); 

				var form = prova.getForm();
				form.submit( {
					parentId: cmp.getId(),
					waitMsg: cmp.i18n._('Please wait while your language pack is uploaded...'),
					success: function(form,action) { 
                        languagesStore.load();
						var cmp = Ext.getCmp(action.options.parentId);						
						Ext.MessageBox.alert(cmp.i18n._("Successed"), cmp.i18n._("Upload Language Pack Successed"));
					},
					failure: function(form,action) {
						var cmp = Ext.getCmp(action.options.parentId); 
						if (action.result && action.result.msg) {
							Ext.MessageBox.alert(cmp.i18n._("Failed"), cmp.i18n._(action.result.msg));
						} else {
							Ext.MessageBox.alert(cmp.i18n._("Failed"), cmp.i18n._("Upload Language Pack Failed")); 
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
            rpc.languageManager.setLanguageSettings(function (result, exception) {
                //re-enable tabs
                this.tabs.enable();
                if(exception) {Ext.MessageBox.alert(i18n._("Failed"),exception.message); return;}
                //exit settings screen
                this.cancelAction();
            }.createDelegate(this),
                    this.getLanguageSettings());
        }
    }
    
});

}