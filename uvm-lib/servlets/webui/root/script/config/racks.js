if (!Ung.hasResource["Ung.Racks"]) {
    Ung.hasResource["Ung.Racks"] = true;

    Ung.Racks = Ext.extend(Ung.ConfigWin, {
        panelPolicyManagement : null,
        gridRacks : null,
        gridPolicies : null,
        initComponent : function() {
            this.breadcrumbs = [{
                title : i18n._("Configuration"),
                action : function() {
                    this.cancelAction();
                }.createDelegate(this)
            }, {
                title : i18n._('Policy Management')
            }];
            Ung.Racks.superclass.initComponent.call(this);
        },

        onRender : function(container, position) {
            // call superclass renderer first
            Ung.Racks.superclass.onRender.call(this, container, position);
            this.initSubCmps.defer(1, this);
            // builds the tabs
        },
        initSubCmps : function() {
            this.buildPolicyManagement();
            // builds the tab panel with the tabs
            this.buildTabPanel([this.panelPolicyManagement]);
            this.tabs.activate(this.panelPolicyManagement);
            // this.loadGridRacks();
        },
        getPolicyConfiguration : function(forceReload) {
            if (forceReload || this.rpc.policyConfiguration === undefined) {
                this.rpc.policyConfiguration = rpc.policyManager.getPolicyConfiguration();
            }
            return this.rpc.policyConfiguration;
        },
        buildPolicyManagement : function() {
            this.buildRacks();
            this.buildPolicies();
            this.panelPolicyManagement = new Ext.Panel({
                // private fields
                name : 'Policy Management',
                parentId : this.getId(),
                title : this.i18n._('Policy Management'),
                layout : "form",
                autoScroll : true,
                items : [this.gridRacks
                // this.gridPolicies
                ]
            });

        },
        buildRacks : function() {
            this.gridRacks = new Ung.EditorGrid({
                settingsCmp : this,
                name : 'Racks',
                height : 300,
                bodyStyle : 'padding-bottom:20px;',
                autoScroll : true,
                parentId : this.getId(),
                title : this.i18n._('Racks'),
                recordJavaClass : "com.untangle.uvm.policy.Policy",
                emptyRow : {
                    "default" : false,
                    "name" : this.i18n._("[no name]"),
                    "notes" : this.i18n._("[no description]"),
                    "javaClass" : "com.untangle.uvm.policy.Policy"
                },
                //autoExpandColumn : 'notes',
                data : this.getPolicyConfiguration().policies,
                dataRoot: 'list',
                paginated: false,
                fields : [{
                    name : 'id'
                },{
                    name : 'default'
                }, {
                    name : 'name'
                }, {
                    name : 'notes'
                }, {
                    name : 'javaClass'
                }],
                columns : [{
                    header : this.i18n._("name"),
                    width : 200,
                    sortable : true,
                    dataIndex : 'name'
                }, {
                    header : this.i18n._("description"),
                    width : 200,
                    sortable : true,
                    dataIndex : 'notes'

                }],
                rowEditorInputLines : [new Ext.form.TextField({
                    name : "Name",
                    dataIndex : "name",
                    fieldLabel : this.i18n._("Name"),
                    allowBlank : false,
                    blankText : this.i18n._("The policy name cannot be blank."),
                    width : 200
                }), new Ext.form.TextField({
                    name : "Description",
                    dataIndex : "notes",
                    fieldLabel : this.i18n._("Description"),
                    allowBlank : false,
                    width : 200
                })]
            });

        },
        buildPolicies : function() {

        },
        validateClient : function() {
        	return true;
        },
        // save function
        saveAction : function() {
            if (this.validate()) {
                Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
                this.saveSemaphore = 1;
                // save language settings

                rpc.policyManager.setPolicyConfiguration(function(result, exception) {
                    if (exception) {
                        Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                        return;
                    }
                    this.afterSave();
                }.createDelegate(this), this.getPolicyConfiguration());
            }
        },
        afterSave : function() {
            this.saveSemaphore--;
            if (this.saveSemaphore == 0) {
                Ext.MessageBox.hide();
                this.cancelAction();
            }
        }

    });
}
