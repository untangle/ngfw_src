Ext.define('Ung.RuleEditorGrid', {
    extend: 'Ung.EditorGrid',
    requires: [
        'Ext.toolbar.TextItem',
        'Ext.form.field.Checkbox',
        'Ext.form.field.Text',
        'Ext.ux.statusbar.StatusBar'
    ],

    /**
     * @private
     * search value initialization
     */
    searchValue: null,

    /**
     * @private
     * The generated regular expression used for searching.
     */
    searchRegExp: null,

    defaultStatusText: i18n._('Loading...'),

    /*
     * @public
     * store fields to search
     */
    searchFields:[
        'category',
        'rule'
    ],

    /*
     * @public
     * Minimum number of characters to start search
     */
    searchMinimumCharacters: 2,

    // Component initialization override: adds the top and bottom toolbars and setup headers renderer.
    initComponent: function() {
        var me = this;
        me.bbar = [
            i18n._('Search'),
            {
                xtype: 'textfield',
                name: 'searchField',
                hideLabel: true,
                width: 200,
                listeners: {
                change: {
                    fn: me.onTextFieldChange,
                        scope: this,
                        buffer: 100
                    }
                }
            },{
                xtype: 'statusbar',
                defaultText: me.defaultStatusText,
                name: 'searchStatusBar',
                border: 0
            }
        ];

        me.callParent(arguments);
    },

    // afterRender override: it adds textfield and statusbar reference and start monitoring keydown events in textfield input
    afterRender: function() {
        var me = this;
        me.callParent(arguments);

        me.searchTextField = me.down('textfield[name=searchField]');
        me.searchStatusBar = me.down('statusbar[name=searchStatusBar]');
    },

    afterDataBuild: function(handler){
        var me = this;
        me.callParent(arguments);

        me.searchStatusBar.setStatus({
            text: me.store.count() + ' ' + i18n._('total rules'),
            iconCls: 'x-status-valid'
        });

        me.storeCategories = Ext.create('Ext.data.Store', {
            fields: ['id', 'value']
        });
        me.storeClasstypes = Ext.create('Ext.data.Store', {
            fields: ['id', 'value']
        });
        me.store.each(
            function( record ){
                var category = record.get("category");
                if( this.storeCategories.find( 'id', category ) == -1 ){
                    this.storeCategories.add( { id: category, value: category } );
                }
                var classtype = record.get("classtype");
                if( this.storeClasstypes.find( 'id', classtype ) == -1 ){
                    this.storeClasstypes.add( { id: classtype, value: classtype } );
                }
            },
            this
        );
        me.rowEditor.down('combo[name=Classtype]').bindStore(me.storeClasstypes);
        me.rowEditor.down('combo[name=Category]').bindStore(me.storeCategories);
    },

    /*
     * @private
     * DEL ASCII code
    */
    searchTagsProtect: '\x0f',

    /*
     * @private 
     * detects regexp reserved word
     */
    searchRegExpProtect: /\\|\/|\+|\\|\.|\[|\]|\{|\}|\?|\$|\*|\^|\|/gm,
    /**
     * In normal mode it returns the value with protected regexp characters.
     * In regular expression mode it returns the raw value except if the regexp is invalid.
     * @return {String} The value to process or null if the textfield value is blank or invalid.
     * @private
     */
    getSearchValue: function() {
        var me = this,
            value = me.searchTextField.getValue();

        if (value === '') {
            return null;
        }
        value = value.replace(me.searchRegExpProtect, function(m) {
            return '\\' + m;
        });

        var length = value.length,
            resultArray = [me.searchTagsProtect + '*'],
            i = 0,
            c;

        for(; i < length; i++) {
            c = value.charAt(i);
            resultArray.push(c);
            if (c !== '\\') {
                resultArray.push(me.searchTagsProtect + '*');
            }
        }
        return resultArray.join('');
    },

    /**
     * Finds all strings that matches the searched value in each grid cells.
     * @private
     */
    onTextFieldChange: function() {
        var me = this;

        me.store.clearFilter(false);
        me.searchValue = me.getSearchValue();
        if( ( me.searchValue !== null ) &&
            ( me.searchTextField.getValue().length > me.searchMinimumCharacters ) ){

            me.searchRegExp = new RegExp(me.searchValue, 'g' + 'i');

            me.store.filterBy( function( record, id ) {
                me = this;
                for( var i = 0 ; i < me.searchFields.length; i++){
                    if( me.searchRegExp.test( record.get( me.searchFields[i] )) ){
                        return true;
                    }
                }
                return false;
            }, me );

            var count = me.store.count();
            me.searchStatusBar.setStatus({
                text: count ? count + ' ' + i18n._(' matche(s) found') : i18n._('No matches found') ,
                iconCls: 'x-status-valid'
            });
         }else{
            if( ( me.searchValue !== null ) &&
                ( me.searchTextField.getValue().length < ( me.searchMinimumCharacters + 1 ) ) ){
                me.searchStatusBar.setStatus({
                    text: i18n._("(type more than 2 characters)"),
                    iconCls: 'x-status-valid'
                });
            }else{
                me.searchStatusBar.setStatus({
                    text: me.store.count() + ' ' + i18n._('total rules'),
                    iconCls: 'x-status-valid'
                });
            }
         }

         // force textfield focus
         me.searchTextField.focus();
     },

     getPageList: function(useId, useInternalId) {
        this.store.clearFilter(true);
        return this.callSuper( useId, useInternalId );
     }
});

Ext.define('Webui.untangle-node-idps.settings', {
    extend:'Ung.NodeWin',
    statics: {
        preloadSettings: function(node){
            Ext.Ajax.request({
                url: "/webui/download",
                method: 'POST',
                params: {
                    type: "IdpsSettings",
                    arg1: "load",
                    arg2: node.nodeId
                },
                scope: node,
                success: function(response){
                    this.openSettings.call(this, Ext.decode( response.responseText ) );
                },
                failure: function(response){
                    this.openSettings.call(this, null );
                }
            });
        }
    },
    panelStatus: null,
    panelRules: null,
    gridRules: null,
    gridVariables: null,
    gridEventLog: null,
    statistics: null,
    // called when the component is rendered
    initComponent: function() {

        this.classtypesStore = Ext.create(
            'Ext.data.ArrayStore', {
            fields: [ 'name', 'description', 'priority' ],
            data: [
                [ "attempted-admin", this.i18n._("Attempted Administrator Privilege Gain"), "high"],
                [ "attempted-user", this.i18n._("Attempted User Privilege Gain"), "high" ],
                [ "inappropriate-content", this.i18n._("Inappropriate Content was Detected"), "high" ],
                [ "policy-violation", this.i18n._("Potential Corporate Privacy Violation"), "high" ],
                [ "shellcode-detect", this.i18n._("Executable code was detected"), "high" ],
                [ "successful-admin", this.i18n._("Successful Administrator Privilege Gain"), "high" ],
                [ "successful-user", this.i18n._("Successful User Privilege Gain"), "high" ],
                [ "trojan-activity", this.i18n._("A Network Trojan was detected"), "high" ],
                [ "unsuccessful-user", this.i18n._("Unsuccessful User Privilege Gain"), "high" ],
                [ "web-application-attack", this.i18n._("Web Application Attack"), "high" ],

                [ "attempted-dos", this.i18n._("Attempted Denial of Service"), "medium" ],
                [ "attempted-recon", this.i18n._("Attempted Information Leak"), "medium" ],
                [ "bad-unknown", this.i18n._("Potentially Bad Trafﬁc"), "medium" ],
                [ "default-login-attempt", this.i18n._("Attempt to login by a default username and password"), "medium" ],
                [ "denial-of-service", this.i18n._("Detection of a Denial of Service Attack"), "medium" ],
                [ "misc-attack", this.i18n._("Misc Attack"), "medium" ],
                [ "non-standard-protocol", this.i18n._("Detection of a non-standard protocol or event"), "medium" ],
                [ "rpc-portmap-decode", this.i18n._("Decode of an RPC Query"), "medium" ],
                [ "successful-dos", this.i18n._("Denial of Service"), "medium" ],
                [ "successful-recon-largescale", this.i18n._("Large Scale Information Leak"), "medium" ],
                [ "successful-recon-limited", this.i18n._("Information Leak"), "medium" ],
                [ "suspicious-filename-detect", this.i18n._("A suspicious filename was detected"), "medium" ],
                [ "suspicious-login", this.i18n._("An attempted login using a suspicious username was detected"), "medium" ],
                [ "system-call-detect", this.i18n._("A system call was detected"), "medium" ],
                [ "unusual-client-port-connection", this.i18n._("A client was using an unusual port"), "medium" ],
                [ "web-application-activity", this.i18n._("Access to a potentially vulnerable web application"), "medium" ],

                [ "icmp-event", this.i18n._("Generic ICMP event"), "low" ],
                [ "misc-activity", this.i18n._("Misc activity"), "low" ],
                [ "network-scan", this.i18n._("Detection of a Network Scan"), "low" ],
                [ "not-suspicious", this.i18n._("Not Suspicious Traffic"), "low" ],
                [ "protocol-command-decode", this.i18n._("Generic Protocol Command Decode"), "low" ],
                [ "string-detect", this.i18n._("A suspicious string was detected"), "low" ],
                [ "unknown", this.i18n._("Unknown Traffic"), "low" ],

                [ "tcp-connection", this.i18n._("A TCP connection was detected"), "low" ]
            ]
        });

        this.categoriesStore = Ext.create(
            'Ext.data.ArrayStore', {
            fields: [ 'name', 'description' ],
            data: [
            [ "app-detect", this.i18n._("This category contains rules that look for, and control, the traffic of certain applications that generate network activity. This category will be used to control various aspects of how an application behaves.") ],
            [ "blacklist", this.i18n._("This category contains URI, USER-AGENT, DNS, and IP address rules that have been determined to be indicators of malicious activity. These rules are based on activity from the Talos virus sandboxes, public list of malicious URLs, and other data sources.") ],
            [ "browser-chrome", this.i18n._("This category contains detection for vulnerabilities present in the Chrome browser. (This is separate from the 'browser-webkit' category, as Chrome has enough vulnerabilities to be broken out into it’s own, and while it uses the Webkit rendering engine, there’s a lot of other features to Chrome.)") ],
            [ "browser-firefox", this.i18n._("This category contains detection for vulnerabilities present in the Firefox browser, or products that have the 'Gecko' engine. (Thunderbird email client, etc)") ],
            [ "browser-ie", this.i18n._("This category contains detection for vulnerabilities present in the Internet Explorer browser (Trident or Tasman engines)") ],
            [ "browser-webkit", this.i18n._("This category contains detection of vulnerabilities present in the Webkit browser engine (aside from Chrome) this includes Apple's Safari, RIM’s mobile browser, Nokia, KDE, Webkit itself, and Palm.") ],
            [ "browser-other", this.i18n._("This category contains detection for vulnerabilities in other browsers not listed above.") ],
            [ "browser-plugins", this.i18n._("This category contains detection for vulnerabilities in browsers that deal with plugins to the browser. (Example: Active-x)") ],
            [ "content-replace", this.i18n._("This category containt any rule that utilizes the 'replace' functionality inside of Snort.") ],
            [ "deleted", this.i18n._("When a rule has been deprecated or replaced it is moved to this categories. Rules are never totally removed from the ruleset, they are moved here.") ],
            [ "exploit", this.i18n._("This is an older category which will be deprecated soon. This category looks for exploits against software in a generic form.") ],
            [ "exploit-kit", this.i18n._("This category contains rules that are specifically tailored to detect exploit kit activity. This does not include 'post-compromise' rules (as those would be in indicator-compromise). Files that are dropped as result of visiting an exploit kit would be in their respective file category.") ],
            [ "file-executable", this.i18n._("This category contains rules for vulnerabilities that are found or are delivered through executable files, regardless of platform.") ],
            [ "file-flash", this.i18n._("This category contains rules for vulnerabilities that are found or are delivered through flash files. Either compressed or uncompressed, regardless of delivery method platform being attacked.") ],
            [ "file-image", this.i18n._("This category contains rules for vulnerabilities that are found inside of images files. Regardless of delivery method, software being attacked, or type of image. (Examples include: jpg, png, gif, bmp, etc)") ],
            [ "file-identify", this.i18n._("This category is to identify files through file extension, the content in the file (file magic), or header found in the traffic. This information is usually used to then set a flowbit to be used in a different rule.") ],
            [ "file-multimedia", this.i18n._("This category contains rules for vulnerabilities present inside of multimedia files (mp3, movies, wmv)") ],
            [ "file-office", this.i18n._("This category contains rules for vulnerabilities present inside of files belonging to the Microsoft Office suite of software. (Excel, PowerPoint, Word, Visio, Access, Outlook, etc)") ],
            [ "file-pdf", this.i18n._("This category contains rules for vulnerabilities found inside of PDF files. Regardless of method of creation, delivery method, or which piece of software the PDF affects (for example, both Adobe Reader and FoxIt Reader)") ],
            [ "file-other", this.i18n._("This category contains rules for vulnerabilities present inside a file, that doesn't fit into the other categories above.") ],
            [ "indicator-compromise", this.i18n._("This category contains rules that are clearly to be used only for the detection of a positively compromised system, false positives may occur.") ],
            [ "indicator-obfuscation", this.i18n._("This category contains rules that are clearly used only for the detection of obfuscated content. Like encoded JavaScript rules.") ],
            [ "indicator-shellcode", this.i18n._("This category contains rules that are simply looking for simple identification markers of shellcode in traffic. This replaces the old 'shellcode.rules'.") ],
            [ "malware-backdoor", this.i18n._("This category contains rules for the detection of traffic destined to known listening backdoor command channels. If a piece of malicious soft are opens a port and waits for incoming commands for its control functions, this type of detection will be here. A simple example would be the detection for BackOrifice as it listens on a specific port and then executes the commands sent.") ],
            [ "malware-cnc", this.i18n._("This category contains known malicious command and control activity for identified botnet traffic. This includes call home, downloading of dropped files, and ex-filtration of data. Actual commands issued from 'Master to Zombie' type stuff will also be here.") ],
            [ "malware-tools", this.i18n._("This category contains rules that deal with tools that can be considered malicious in nature. For example, LOIC.") ],
            [ "malware-other", this.i18n._("This category contains rules that are malware related, but don’t fit into one of the other 'malware' categories.") ],
            [ "os-linux", this.i18n._("This category contains rules that are looking for vulnerabilities in Linux based OSes. Not for browsers or any other software on it, but simply against the OS itself.") ],
            [ "os-solaris", this.i18n._("This category contains rules that are looking for vulnerabilities in Solaris based OSes. Not for any browsers or any other software on top of the OS.") ],
            [ "os-windows", this.i18n._("This category contains rules that are looking for vulnerabilities in Windows based OSes. Not for any browsers or any other software on top of the OS.") ],
            [ "os-other", this.i18n._("This category contains rules that are looking for vulnerabilities in an OS that is not listed above.") ],
            [ "policy-multimedia", this.i18n._("This category contains rules that detect potential violations of policy for multimedia. Examples like the detection of the use of iTunes on the network. This is not for vulnerabilities found within multimedia files, as that would be in file-multimedia.") ],
            [ "policy-social", this.i18n._("This category contains rules for the detection potential violations of policy on corporate networks for the use of social media. (p2p, chat, etc)") ],
            [ "policy-other", this.i18n._("This category is for rules that may violate the end-users corporate policy bud do not fall into any of the other policy categories first.") ],
            [ "policy-spam", this.i18n._("This category is for rules that may indicate the presence of spam on the network.") ],
            [ "protocol-finger", this.i18n._("This category is for rules that may indicate the presence of the finger protocol or vulnerabilities in the finger protocol on the network.") ],
            [ "protocol-ftp", this.i18n._("This category is for rules that may indicate the presence of the ftp protocol or vulnerabilities in the ftp protocol on the network.") ],
            [ "protocol-icmp", this.i18n._("This category is for rules that may indicate the presence of icmp traffic or vulnerabilities in icmp on the network.") ],
            [ "protocol-imap", this.i18n._("This category is for rules that may indicate the presence of the imap protocol or vulnerabilities in the imap protocol on the network.") ],
            [ "protocol-pop", this.i18n._("This category is for rules that may indicate the presence of the pop protocol or vulnerabilities in the pop protocol on the network.") ],
            [ "protocol-services", this.i18n._("This category is for rules that may indicate the presence of the rservices protocol or vulnerabilities in the rservices protocols on the network.") ],
            [ "protocol-voip", this.i18n._("This category is for rules that may indicate the presence of voip services or vulnerabilities in the voip protocol on the network.") ],
            [ "pua-adware", this.i18n._("This category deals with 'pua' or Potentially Unwanted Applications that deal with adware or spyware.") ],
            [ "pua-p2p", this.i18n._("This category deals with 'pua' or Potentially Unwanted Applications that deal with p2p.") ],
            [ "pua-toolbars", this.i18n._("This category deals with 'pua' or Potentially Unwanted Applications that deal with toolbars installed on the client system. (Google Toolbar, Yahoo Toolbar, Hotbar, etc)") ],
            [ "pua-other", this.i18n._("This category deals with 'pua' or Potentially Unwanted Applications that don’t fit into one of the categories shown above.") ],
            [ "server-apache", this.i18n._("This category deals with vulnerabilities in or attacks against the Apache Web Server.") ],
            [ "server-iis", this.i18n._("This category deals with vulnerabilities in or attacks against the Microsoft IIS Web server.") ],
            [ "server-mssql", this.i18n._("This category deals with vulnerabilities in or attacks against the Microsoft SQL Server.") ],
            [ "server-mysql", this.i18n._("This category deals with vulnerabilities in or attacks against Oracle's MySQL server.") ],
            [ "server-oracle", this.i18n._("This category deals with vulnerabilities in or attacks against Oracle's Oracle DB Server.") ],
            [ "server-webapp", this.i18n._("This category deals with vulnerabilities in or attacks against Web based applications on servers.") ],
            [ "server-mail", this.i18n._("This category contains rules that detect vulnerabilities in mail servers. (Exchange, Courier). These are separate from the protocol categories, as those deal with the traffic going to the mail servers itself.") ],
            [ "server-other", this.i18n._("This category contains rules that detect vulnerabilities in or attacks against servers that are not detailed in the above list.") ]
            ]
        });

//        console.log(this);
//            this.statistics = this.getRpcNode().getStatistics();
        this.buildStatus();
        this.buildRules();
        this.buildEventLog();
        // builds the tab panel with the tabs
        this.buildTabPanel([this.panelStatus, this.panelRules, this.gridEventLog]);
//        this.buildTabPanel([this.panelConfiguration, this.panelRules, this.gridEventLog]);
        this.callParent(arguments);


        if( this.settings.configured == false &&
            !this.wizardWindow ){
            var launchWizard = Ext.create( 
                'Ext.util.DelayedTask', 
                function(){
                    this.setupWizard();
                },
                this
            );
            launchWizard.delay(100);
        }
    },
    // Status Panel
    buildStatus: function() {
        this.panelStatus = Ext.create('Ext.panel.Panel',{
            name: 'Status',
            // helpSource: 'intrusion_detection_prevention_status', //FIXME disabled for now so it doesnt break test - uncomment me when docs exist
            parentId: this.getId(),
            title: this.i18n._('Status'),
            cls: 'ung-panel',
            autoScroll: true,
            defaults: {
                xtype: 'fieldset',
                buttonAlign: 'left'
            },
            items: [{
                title: this.i18n._('Statistics'),
                labelWidth: 230,
                defaults: {
                    xtype: "textfield",
                    disabled: true
                },
                items: [{
                    fieldLabel: this.i18n._('Total Signatures Available'),
                    name: 'Total Signatures Available',
                    labelWidth:200,
                    labelAlign:'left',
//                        value: this.statistics.totalAvailable
                }, {
                    fieldLabel: this.i18n._('Total Signatures Logging'),
                    name: 'Total Signatures Logging',
                    labelWidth:200,
                    labelAlign:'left',
//                        value: this.statistics.totalLogging
                }, {
                    fieldLabel: this.i18n._('Total Signatures Blocking'),
                    name: 'Total Signatures Blocking',
                    labelWidth:200,
                    labelAlign:'left',
//                        value: this.statistics.totalBlocking
                }]
            }, {
                title: this.i18n._("Setup Wizard"),
                items: [
                    {
                        xtype: "button",
                        name: 'setup_wizard_button',
                        text: this.i18n._("Run Intrusion Detection/Prevention Setup Wizard"),
                        iconCls: "action-icon",
                        handler: Ext.bind(function() {
                            this.setupWizard();
                        }, this)
                    }]
            }, {
                title: this.i18n._('Note'),
                cls: 'description',
                html: Ext.String.format(this.i18n._("{0} continues to maintain the default signature settings through automatic updates. You are free to modify and add signatures, however it is not required."),
                        rpc.companyName)
            }]
        });
    },

    // Rules Panel
    buildRules: function() {
        this.panelRules = Ext.create('Ext.panel.Panel',{
            name: 'panelRules',
            // helpSource: 'intrusion_dection_prevention_rules', //FIXME disabled for now so it doesnt break test - uncomment me when docs exist
            parentId: this.getId(),
            title: this.i18n._('Rules'),
            border: false,
            layout: { type: 'vbox', align: 'stretch' },
            cls: 'ung-panel',
            items: [
                this.gridRules = Ext.create('Ung.RuleEditorGrid', {
                    name: 'Rules',
                    flex: 1,
                    groupField: 'classtype',
                    sortField: 'category',
                    style: "margin-bottom:10px;",
                    settingsCmp: this,
                    title: this.i18n._("Rules"),
                    recordJavaClass: "com.untangle.node.idps.IpsRule",
                    dataProperty: 'rules',
                    paginated: false,
                    columnsDefaultSortable: true,
                    plugins: {
                        ptype: 'bufferedrenderer',
                        trailingBufferZone: 20,  // Keep 20 rows rendered in the table behind scroll
                        leadingBufferZone: 50   // Keep 50 rows rendered in the table ahead of scroll
                    },
                    features: [ 
                        Ext.create('Ext.grid.feature.Grouping',{
                            groupHeaderTpl: '{columnName}: {name} ({rows.length} rule{[values.rows.length > 1 ? "s" : ""]})',
                            startCollapsed: true
                        })            
                    ] ,
                    fields: [{
                        name: 'sid',
                        sortType: 'asInt'
                    },{
                        name: 'category'
                    },{
                        name: 'classtype'
                    },{
                        name: 'msg'
                    },{
                        name: 'rule'
                    },{
                        name: 'log'
                    },{
                        name: 'block'
                    }],
                    emptyRow: {
                        "sid": "0",
                        "log": true,
                        "block": false,
                        "category": "",
                        "classtype": "",
                        "name" : "",
                        "rule": ""
                    },
                    columns: [{
                        header: this.i18n._("Sid"),
                        dataIndex: 'sid',
                        sortable: true,
                        width: 70,
                        editor: null,
                        menuDisabled: false
                    },{
                        header: this.i18n._("Classtype"),
                        dataIndex: 'classtype',
                        sortable: true,
                        width: 100,
                        flex:1,
                        editor: null,
                        menuDisabled: false
                    },{
                        header: this.i18n._("Category"),
                        dataIndex: 'category',
                        sortable: true,
                        width: 100,
                        flex:1,
                        editor: {
                            xtype:'texfield',
                            emptyText: this.i18n._("[enter category]"),
                            allowBlank: false
                        },
                        menuDisabled: false
                    },{
                        header: this.i18n._("Msg"),
                        dataIndex: 'msg',
                        sortable: true,
                        width: 200,
                        flex:3,
                        editor: null,
                        menuDisabled: false
                    },{
                        xtype:'checkcolumn',
                        header: this.i18n._("Log"),
                        dataIndex: 'log',
                        sortable: true,
                        resizable: false,
                        width:55,
                        menuDisabled: false
                    },{
                        xtype:'checkcolumn',
                        header: this.i18n._("Block"),
                        dataIndex: 'block',
                        sortable: true,
                        resizable: false,
                        width:55,
                        menuDisabled: false
                    }],
                    rowEditorInputLines: [{
                        name: "Classtype",
                        dataIndex: "classtype",
                        fieldLabel: this.i18n._("Classtype"),
                        emptyText: this.i18n._("[enter class]"),
                        allowBlank: false,
                        width: 400,
                        xtype: 'combo',
                        queryMode: 'local',
                        valueField: 'id',
                        displayField: 'value'
                  },{
                        name: "Category",
                        fieldLabel: this.i18n._("Category"),
                        dataIndex: "category",
                        emptyText: this.i18n._("[enter category]"),
                        allowBlank: false,
                        width: 400,
                        xtype: 'combo',
                        queryMode: 'local',
                        valueField: 'id',
                        displayField: 'value'
                    },{
                        xtype:'textfield',
                        name: "Name",
                        dataIndex: "msg",
                        fieldLabel: this.i18n._("Msg"),
                        emptyText: this.i18n._("[enter name]"),
                        allowBlank: false,
                        width: 400
                    },{
                        xtype:'textfield',
                        name: "Sid",
                        dataIndex: "sid",
                        fieldLabel: this.i18n._("Sid"),
                        emptyText: this.i18n._("[enter sid]"),
                        allowBlank: false,
                        width: 400
                    },{
                        xtype:'checkbox',
                        name: "Block",
                        dataIndex: "block",
                        fieldLabel: this.i18n._("Block")
                    },{
                        xtype:'checkbox',
                        name: "Log",
                        dataIndex: "log",
                        fieldLabel: this.i18n._("Log")
                    },{
                        xtype:'textfield',
                        name: "Rule",
                        dataIndex: "rule",
                        fieldLabel: this.i18n._("Rule"),
                        emptyText: this.i18n._("[enter rule]"),
                        allowBlank: false,
                        width: 1000
                    }]
                }),
                this.gridVariables = Ext.create('Ung.EditorGrid', {
                    flex: 1,
                    name: 'Variables',
                    settingsCmp: this,
                    emptyRow: {
                        "variable": "",
                        "definition": "",
                        "description": ""
                    },
                    title: this.i18n._("Variables"),
                    recordJavaClass: "com.untangle.node.idps.IpsVariable",
                    dataProperty: 'variables',
                    fields: [{
                        name: 'id'
                    },{
                        name: 'variable',
                        type: 'string'
                    },{
                        name: 'definition',
                        type: 'string'
                    },{
                        name: 'description',
                        type: 'string'
                    }],
                    columns: [{
                        header: this.i18n._("name"),
                        width: 170,
                        dataIndex: 'variable',
                        editor: {
                            xtype:'textfield',
                            emptyText: this.i18n._("[enter name]"),
                            allowBlank: false
                        }
                    },{
                        id: 'definition',
                        header: this.i18n._("pass"),
                        width: 300,
                        dataIndex: 'definition',
                        editor: {
                            xtype:'textfield',
                            emptyText: this.i18n._("[enter definition]"),
                            allowBlank: false
                        }
                    },{
                        header: this.i18n._("description"),
                        width: 300,
                        dataIndex: 'description',
                        flex:1,
                        editor: {
                            xtype:'textfield',
                            emptyText: this.i18n._("[enter description]"),
                            allowBlank: false
                        }
                    }],
                    sortField: 'variable',
                    columnsDefaultSortable: true,
                    rowEditorInputLines: [{
                        xtype:'textfield',
                        name: "Name",
                        dataIndex: "variable",
                        fieldLabel: this.i18n._("Name"),
                        emptyText: this.i18n._("[enter name]"),
                        allowBlank: false,
                        width: 300
                    },{
                        xtype:'textfield',
                        name: "Pass",
                        dataIndex: "definition",
                        fieldLabel: this.i18n._("Pass"),
                        emptyText: this.i18n._("[enter definition]"),
                        allowBlank: false,
                        width: 400
                    },{
                        xtype:'textfield',
                        name: "Description",
                        dataIndex: "description",
                        fieldLabel: this.i18n._("Description"),
                        emptyText: this.i18n._("[enter description]"),
                        allowBlank: false,
                        width: 400
                    }]
                })
        ]});
    },
    // Event Log
    buildEventLog: function() {
        var settingsCmpParam = this;
        var nameParam = 'EventLog';
        var titleParam = i18n._('Event Log');
        var helpSourceParam = 'intrusion_detection_prevention_event_log';
        var visibleColumnsParam = ['time_stamp','sig_id', 'source_addr', 'source_port', 'dest_addr', 'dest_port', 'protocol', 'blocked', 'category', 'classtype', 'msg' ];
        var eventQueriesFnParam = this.getRpcNode().getEventQueries;
        this.gridEventLog = Ext.create('Ung.GridEventLog',{
            name: nameParam,
            settingsCmp: settingsCmpParam,
            helpSource: helpSourceParam,
            eventQueriesFn: eventQueriesFnParam,
            title: titleParam,
            fields: [{
                name: 'time_stamp',
                sortType: Ung.SortTypes.asTimestamp
            }, {
                name: 'sig_id',
                sortType: 'asInt'
            }, {
                name: 'gen_id',
                sortType: 'asInt'
            }, {
                name: 'class_id',
                sortType: 'asInt'
            }, {
                name: 'source_addr',
                sortType: Ung.SortTypes.asIp
            }, {
                name: 'source_port',
                sortType: 'asInt'
            }, {
                name: 'dest_addr',
                sortType: Ung.SortTypes.asIp
            }, {
                name: 'dest_port',
                sortType: 'asInt'
            }, {
                name: 'protocol',
                sortType: 'asInt'
            }, {
                name: 'blocked',
                type: 'boolean'
            }, {
                name: 'category',
                type: 'string'
            }, {
                name: 'classtype',
                type: 'string'
            }, {
                name: 'msg',
                type: 'string'
            }],
            columns: [{
                hidden: visibleColumnsParam.indexOf('time_stamp') < 0,
                header: i18n._("Timestamp"),
                width: Ung.Util.timestampFieldWidth,
                sortable: true,
                dataIndex: 'time_stamp',
                renderer: function(value) {
                    return i18n.timestampFormat(value);
                }
            }, {
                hidden: visibleColumnsParam.indexOf('sig_id') < 0,
                header: i18n._("Signature ID"),
                width: 70,
                sortable: true,
                dataIndex: 'sig_id',
                filter: {
                    type: 'numeric'
                }
            }, {
                hidden: visibleColumnsParam.indexOf('gen_id') < 0,
                header: i18n._("Generator ID"),
                width: 70,
                sortable: true,
                dataIndex: 'gen_id',
                filter: {
                    type: 'numeric'
                }
            }, {
                hidden: visibleColumnsParam.indexOf('class_id') < 0,
                header: i18n._("Class ID"),
                width: 70,
                sortable: true,
                dataIndex: 'class_id',
                filter: {
                    type: 'numeric'
                }
            }, {
                hidden: visibleColumnsParam.indexOf('source_addr') < 0,
                header: i18n._("Source Address"),
                width: Ung.Util.ipFieldWidth,
                sortable: true,
                dataIndex: 'source_addr'
            }, {
                hidden: visibleColumnsParam.indexOf('source_port') < 0,
                header: i18n._("Source port"),
                width: Ung.Util.portFieldWidth,
                sortable: true,
                dataIndex: 'source_port',
                filter: {
                    type: 'numeric'
                }
            }, {
                hidden: visibleColumnsParam.indexOf('dest_addr') < 0,
                header: i18n._("Destination Address"),
                width: Ung.Util.ipFieldWidth,
                sortable: true,
                dataIndex: 'dest_addr'
            }, {
                hidden: visibleColumnsParam.indexOf('dest_port') < 0,
                header: i18n._("Destination port"),
                width: Ung.Util.portFieldWidth,
                sortable: true,
                dataIndex: 'dest_port',
                filter: {
                    type: 'numeric'
                }
            }, {
                hidden: visibleColumnsParam.indexOf('protocol') < 0,
                header: i18n._("Protocol"),
                width: 70,
                sortable: true,
                dataIndex: 'protocol',
                filter: {
                    type: 'numeric'
                }
            }, {
                hidden: visibleColumnsParam.indexOf('blocked') < 0,
                header: i18n._("Blocked"),
                width: Ung.Util.booleanFieldWidth,
                sortable: true,
                dataIndex: 'blocked',
                filter: {
                    type: 'boolean',
                    yesText: 'true',
                    noText: 'false'
                }
            }, {
                hidden: visibleColumnsParam.indexOf('category') < 0,
                header: i18n._("Category"),
                width: 200,
                sortable: true,
                dataIndex: 'category'
            }, {
                hidden: visibleColumnsParam.indexOf('classtype') < 0,
                header: i18n._("Classtype"),
                width: 200,
                sortable: true,
                dataIndex: 'classtype'
            }, {
                hidden: visibleColumnsParam.indexOf('msg') < 0,
                header: i18n._("Msg"),
                width: 200,
                sortable: true,
                dataIndex: 'msg'
            }]
        });
    },
    setupWizard: function() {
        var i;
        if (this.wizardWindow) {
            Ext.destroy(this.wizardWindow);
        }
        var welcomeCard = Ext.create('Webui.untangle-node-idps.Wizard.Welcome', {
            i18n: this.i18n,
            node: this.getRpcNode(),
            gui: this
        });
        var classtypesCard = Ext.create('Webui.untangle-node-idps.Wizard.Classtypes', {
            i18n: this.i18n,
            node: this.getRpcNode(),
            gui: this
        });
        var categoriesCard = Ext.create('Webui.untangle-node-idps.Wizard.Categories', {
            i18n: this.i18n,
            node: this.getRpcNode(),
            gui: this
        });
        var congratulationsCard = Ext.create('Webui.untangle-node-idps.Wizard.Congratulations', {
            i18n: this.i18n,
            node: this.getRpcNode(),
            nodeWidget: this.node,
            gui: this
        });
        var setupWizard = Ext.create('Ung.Wizard',{
            modalFinish: true,
            hasCancel: true,
            cardDefaults: {
                labelWidth: 200,
                cls: 'untangle-form-panel'
            },
            cards: [welcomeCard, classtypesCard, categoriesCard, congratulationsCard]
        });
        this.wizardWindow = Ext.create('Ung.Window',{
            title: this.i18n._("Intrusion Prevention Setup Wizard"),
            closeAction: "cancelAction",
            wizard: setupWizard,
            layout: "fit",
            items: setupWizard,
            endAction: Ext.bind(function() {
                this.wizardWindow.hide();
                Ext.destroy(this.wizardWindow);
//                this.reload();
            }, this),
            cancelAction: Ext.bind(function() {
                this.wizardWindow.wizard.cancelAction();
            }, this)
        });

        setupWizard.cancelAction=Ext.bind(function() {
            if(!this.wizardWindow.wizard.finished) {
                Ext.MessageBox.alert(this.i18n._("Setup Wizard Warning"), this.i18n._("You have not finished configuring Intrusion Prevention. Please run the Setup Wizard again."), Ext.bind(function () {
                    this.wizardWindow.endAction();
                }, this));
            } else {
                this.wizardWindow.endAction();
            }
        }, this);

        this.wizardWindow.show();
        setupWizard.goToPage(0);
    },
    applyAction: function(){
        this.callParent();
    },
    beforeSave: function(isApply,handler) {
        if( this.wizard ){
            this.wizard = false;
        }else{
            this.settings.rules.list = null;
            this.settings.variables.list = null;
            this.settings.rules.list = this.gridRules.getPageList();
            this.settings.variables.list = this.gridVariables.getPageList();
        }
        handler.call(this, isApply);
    },
    save: function(isApply) {
        // pop up a window        
        Ext.Ajax.request({
            url: "/webui/download",
            jsonData: this.settings,
            method: 'POST',
            params: {
                type: "IdpsSettings",
                arg1: "save",
                arg2: this.tid
            },
            scope: this,
            success: function(response){
                Ext.MessageBox.hide();

                if (!isApply) {
                    this.closeWindow();
                    return;
                }else{
                    this.clearDirty();
                }
            },
            failure: function(response){
                Ext.MessageBox.hide();
                Ext.MessageBox.alert(i18n._("Error"), i18n._("Unable to save settings"));
            }
        });
    },        
    afterSave: function() {
//            this.statistics = this.getRpcNode().getStatistics();
    }
});

/* IDPS wizard configuration cards. */
Ext.define('Webui.untangle-node-idps.Wizard.Welcome',{
    constructor: function( config ) {
        this.i18n = config.i18n;
        this.node = config.node;
        this.gui = config.gui;

        var items = [{
            xtype: 'container',
            html: '<h2 class="wizard-title">'+this.i18n._("Welcome to the Intrusion Prevention Setup Wizard!")+'</h2>'
        },{
            xtype: 'container',
            html: 
                this.i18n._("Intrusion Prevention operates using rules to identify possible threats.  An enabled ruled performs an action, either logging or blocking traffic.  Not all rules are necessary for a given network environment and enabling all of them may negatively impact your network."),
            cls: 'description',
            bodyStyle: 'padding-bottom:10px',
            border: false
        },{
            xtype: 'container',
            html: 
                this.i18n._("This wizard is designed to help you correctly configure the appropriate amount of rules for your network by selecting rule identifiers: classtypes and categories.  The more that you select, the more rules will be enabled.  Again, too many enabled rules may negatively impact your network."),
            cls: 'description',
            bodyStyle: 'padding-bottom:10px',
            border: false
        },{
            xtype: 'container',
            html: 
                this.i18n._("It is highly suggested that you use Recommended values."),
            cls: 'description',
            bodyStyle: 'padding-bottom:10px',
            border: false
        }];

        if( this.gui.getSettings().configured == true ){
            items.push({
                html: this.i18n._('WARNING: Completing this setup wizard will overwrite the previous settings with new settings. All previous settings will be lost!'),
                cls: 'description warning',
                border: false
            });
        }

        this.title = this.i18n._("Welcome");
        this.panel = Ext.create('Ext.form.Panel',{
            border: false,
            items: items
        });

        this.onNext = Ext.bind( this.loadDefaultSettings, this );
        this.initialLoad = true;

    },

    loadDefaultSettings: function(handler){
        if( this.initialLoad == true ){
            this.initialLoad = false;
            handler();
        }else{
            Ext.MessageBox.wait(this.i18n._("Determining recommended settings..."), this.i18n._("Please wait"));
            Ext.Ajax.request({
                url: "/webui/download",
                method: 'POST',
                params: {
                    type: "IdpsSettings",
                    arg1: "wizard",
                    arg2: this.gui.node.nodeId
                },
                scope: this,
                success: function(response){
                    this.gui.wizardSettings = Ext.decode( response.responseText );
                    this.gui.wizardSettings.recommended = this.gui.wizardSettings.active_rules;
                    Ext.MessageBox.hide();
                    handler();
                },
                failure: function(response){
                    Ext.MessageBox.hide();
                    Ext.MessageBox.alert(
                        this.i18n._("Setup Wizard Error"), 
                        this.i18n._("Unable to obtain default settings.  Please run the Setup Wizard again."), 
                        Ext.bind(function () {
                        this.gui.wizardWindow.hide();
                    }, this));
                }
            });
        }
    }
});

Ext.define('Webui.untangle-node-idps.Wizard.Classtypes',{
    constructor: function( config ) {
        this.i18n = config.i18n;
        this.node = config.node;
        this.nodeWidget = config.nodeWidget;
        this.gui = config.gui;

        this.classtypesCheckboxGroup = {
            xtype: 'checkboxgroup',
            fieldLabel: this.i18n._("Classtypes"),
            columns: 1,
            items: [],
//            value: this.gui.wizardSettings.active_rules.classtypes,
            // getValue: function(){
            //     return 'abc';
            // }
        };
        this.gui.classtypesStore.each( function(record){
            this.classtypesCheckboxGroup.items.push({
                boxLabel: record.get( 'name' ) + ' (' + record.get( 'priority' ) + ')',
                name: 'classtypes_selected',
//                tooltip: record.get( 'description' ),
                inputValue: record.get( 'name' )
            });
        }, this );

        this.title = this.i18n._( "Classtypes" );
        this.panel = Ext.create('Ext.form.Panel',{
            border: false,
            items: [{
                xtype: 'container',
                html: '<h2 class="wizard-title">'+this.i18n._("Classtypes")+'</h2>'
            },{
                xtype: 'container',
                html: this.i18n._("Classtypes are a generalized  grouping for rules, such as attempts to gain user access."),
                cls: 'description',
                border: false,
            },{
                name: 'classtypes',
                xtype: 'radio',
                inputValue: 'recommended',
                boxLabel: this.i18n._('Recommended (default)'),
                hideLabel: true,
                checked: true,
                handler: Ext.bind(function(elem, checked) {
                    this.setVisible( elem.inputValue, checked );
                }, this)
            },{
                name: 'classtypes_recommended_settings',
                xtype:'fieldset',
                hidden:true,
                html: "<i>" + this.i18n._("Recommended classtype Settings") + "</i>"
            },{
                name: 'classtypes',
                xtype: 'radio',
                inputValue: 'name',
                boxLabel: this.i18n._('Custom'),
                hideLabel: true,
                checked: false,
                handler: Ext.bind(function(elem, checked) {
                    this.setVisible( elem.inputValue, checked );
                }, this)                
            },{
                name: 'classtypes_name_settings',
                xtype:'fieldset',
                hidden:true,
                items: [
                    this.classtypesCheckboxGroup
                ]
            }]
        });

        this.setVisible('recommended', true);

        this.onLoad = Ext.bind( this.setEnabled, this );
        this.onNext = Ext.bind( this.getValues, this );
    },

    setVisible: function( id, checked ){
        if( checked == false ){
            return;
        }
        Ext.Array.each( 
            this.panel.query(""), 
            function( c ){ 
                if( !c.name || c.name.indexOf('classtypes_') != 0 ){
                    return true;
                }
                if( c.xtype == "fieldset"){
                    if( c.name.indexOf( id ) != -1 ){
                        c.setVisible(true);
                    }else{
                        c.setVisible(false);
                    }
                }
            }
        );
    },

    setEnabled: function( handler ){
        if( this.gui.wizardSettings.active_rules &&
            typeof( this.gui.wizardSettings.active_rules.classtypes ) == 'object' ){
            for( var i = 0; i < this.gui.wizardSettings.active_rules.classtypes.length; i++ ){
                var value = this.gui.wizardSettings.active_rules.classtypes[i];
                Ext.Array.each(
                    this.panel.query("checkbox[name=classtypes_selected]"),
                    function(c){
                        if( c.inputValue == value ){
                            c.setValue(true);
                        }
                    }
                );
            }

            if( this.gui.wizardSettings.active_rules.classtypes.length == 0 ){
                this.panel.down( "[name=classtypes_recommended_settings]" ).update( this.i18n._("None.  Classtypes within selected categories will be used.") );
            }else{
                this.panel.down( "[name=classtypes_recommended_settings]" ).update(this.gui.wizardSettings.active_rules.classtypes.join( ", "));
            }
        }
        handler();
    },

    getValues: function( handler ){
        if( this.panel.down("radio[name=classtypes]").getGroupValue() == "recommended") {
            this.gui.wizardSettings.active_rules.classtypes = this.gui.wizardSettings.recommended.classtypes;
        }else{
            this.gui.wizardSettings.active_rules.classtypes = [];
            Ext.Array.each( 
                this.panel.query("checkbox[name=classtypes_selected][checked=true]"), 
                function( c ){ 
                    this.gui.wizardSettings.active_rules.classtypes.push( c.inputValue );
                },
                this
            );
        }
        handler();
    }

});

Ext.define('Webui.untangle-node-idps.Wizard.Categories',{
    constructor: function( config ) {
        this.i18n = config.i18n;
        this.node = config.node;
        this.nodeWidget = config.nodeWidget;
        this.gui = config.gui;

        var categoriesCheckboxGroup = {
            xtype: 'checkboxgroup',
            fieldLabel: this.i18n._("Categories"),
            columns: 1,
            items: []
        };
        this.gui.categoriesStore.each( function(record){
            categoriesCheckboxGroup.items.push({
                boxLabel: record.get( 'name' ),
//                tooltip: record.get( 'description' ),
                name: 'categories_selected',
                inputValue: record.get( 'name' )
            });
        } );

        this.title = this.i18n._( "Categories" );
        this.panel = Ext.create('Ext.form.Panel',{
            border: false,
            items: [{
                xtype: 'container',
                html: '<h2 class="wizard-title">'+this.i18n._("Categories")+'</h2>'
            },{
                xtype: 'container',
                html: this.i18n._("Categories are a different rule grouping that can span multiple classtypes, such as VOIP access."),
                cls: 'description',
                border: false,
            },{
                name: 'categories',
                xtype: 'radio',
                inputValue: 'recommended',
                boxLabel: this.i18n._('Recommended (default)'),
                hideLabel: true,
                checked: true,
                handler: Ext.bind(function(elem, checked) {
                    this.setVisible( elem.inputValue, checked );
                }, this)
            },{
                name: 'categories_recommended_settings',
                xtype:'fieldset',
                hidden:true
            },{
                name: 'categories',
                xtype: 'radio',
                inputValue: 'name',
                boxLabel: this.i18n._('Select by name'),
                hideLabel: true,
                checked: false,
                handler: Ext.bind(function(elem, checked) {
                    this.setVisible( elem.inputValue, checked );
                }, this)                
            },{
                name: 'categories_name_settings',
                xtype:'fieldset',
                hidden:true,
                items: [{
                    html: "<i>" + this.i18n._("Named Category Settings") + "</i>",
                    cls: 'description',
                    bodyStyle: 'padding-top:10px',
                    border: false
                    },
                    categoriesCheckboxGroup
                ]
            }]
        });

        this.setVisible('recommended', true);

        this.onLoad = Ext.bind( this.setEnabled, this );
        this.onNext = Ext.bind( this.getValues, this );
    },

    setVisible: function( id, checked ){
        if( checked == false ){
            return;
        }
        Ext.Array.each( 
            this.panel.query(""), 
            function( c ){ 
                if( !c.name || c.name.indexOf('categories_') != 0 ){
                    return true;
                }
                if( c.xtype == "fieldset"){
                    if( c.name.indexOf( id ) != -1 ){
                        c.setVisible(true);
                    }else{
                        c.setVisible(false);
                    }
                }
            }
        );
    },

    setEnabled: function( handler ){
        if( this.gui.wizardSettings.active_rules &&
            typeof( this.gui.wizardSettings.active_rules.categories ) == 'object' ){
            for( var i = 0; i < this.gui.wizardSettings.active_rules.categories.length; i++ ){
                var value = this.gui.wizardSettings.active_rules.categories[i];
                Ext.Array.each(
                    this.panel.query("checkbox[name=categories_selected]"),
                    function(c){
                        if( c.inputValue == value ){
                            c.setValue(true);
                        }
                    }
                );
            }

            if( this.gui.wizardSettings.active_rules.categories.length == 0 ){
                this.panel.down( "[name=categories_recommended_settings]" ).update( this.i18n._("None.  Categories within selected classtypes will be used.") );
            }else{
                this.panel.down( "[name=categories_recommended_settings]" ).update(this.gui.wizardSettings.active_rules.categories.join( ", "));
            }
        }
        handler();
    },
    
    getValues: function( handler ){
        if( this.panel.down("radio[name=categories]").getGroupValue() == "recommended") {
            this.gui.wizardSettings.active_rules.categories = this.gui.wizardSettings.recommended.categories;
        }else{
            this.gui.wizardSettings.active_rules.categories = [];
            Ext.Array.each( 
                this.panel.query("checkbox[name=categories_selected][checked=true]"), 
                function( c ){ 
                    this.gui.wizardSettings.active_rules.categories.push( c.inputValue );
                },
                this
            );
        }
        handler();
    }
});


Ext.define('Webui.untangle-node-idps.Wizard.Congratulations',{
    constructor: function( config ) {
        this.i18n = config.i18n;
        this.node = config.node;
        this.nodeWidget = config.nodeWidget;
        this.gui = config.gui;

        this.title = this.i18n._( "Finish" );
        this.panel = Ext.create('Ext.form.Panel',{
            border: false,
            items: [{
                    xtype: 'container',
                    html: '<h2 class="wizard-title">'+this.i18n._("Congratulations!")+'</h2>'
                }, {
                    xtype: 'container',
                    html: this.i18n._('Intrusion Prevention is now configured and enabled.'),
                    cls: 'description',
                    border: false
                }]
        });

        this.onNext = Ext.bind(this.completeWizard, this );
    },

    completeWizard: function( handler ) {
        /*
         * From the default list, disable rules that aren't in specified classtypes or categories.
         */
        var match;
        for( var i = 0; i < this.gui.wizardSettings.rules.list.length; i++ ){
            match = false;
            if( ( this.gui.wizardSettings.active_rules.classtypes.indexOf(this.gui.wizardSettings.rules.list[i].classtype) != -1 ) ||
                ( this.gui.wizardSettings.active_rules.categories.indexOf(this.gui.wizardSettings.rules.list[i].category) != -1 ) ){
                match = true;
            }
            if( match == false ){
                this.gui.wizardSettings.rules.list[i].log = false;
                this.gui.wizardSettings.rules.list[i].block = false;
            }
        }

        this.gui.settings.active_rules = this.gui.wizardSettings.active_rules;
        this.gui.settings.rules = this.gui.wizardSettings.rules;
        this.gui.settings.configured = true;

        /*
         * Reload rules editor.  
         */
        Ext.Function.defer(
            function(){
                this.gui.down("[name=Rules]").buildData();
                this.gui.down("[name=Rules]").reload();
            },
            1000,
            this
        );

        /*
         * Save, enable, teardown wizard
         */
        this.gui.dirtyFlag = true;
        this.gui.wizard = true;

        this.gui.applyAction();

        this.nodeWidget.setPowerOn(true);
        this.nodeWidget.setState("attention");
        this.gui.getRpcNode().start(Ext.bind(function(result, exception) {
            this.gui.wizardWindow.endAction();
            this.gui.getRpcNode().getRunState(Ext.bind(function(result, exception) {
                if(Ung.Util.handleException(exception)) return;
                this.nodeWidget.updateRunState(result);
            }, this));
        }, this));
        this.gui.wizardWindow.hide();

    }
});

