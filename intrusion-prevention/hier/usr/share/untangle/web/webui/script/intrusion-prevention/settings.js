Ext.define('Webui.intrusion-prevention.settings', {
    extend:'Ung.AppWin',
    statics: {
        preloadSettings: function(node){
            Ext.Ajax.request({
                url: "/webui/download",
                method: 'POST',
                params: {
                    type: "IntrusionPreventionSettings",
                    arg1: "load",
                    arg2: node.nodeId
                },
                scope: node,
                timeout: 600000,
                success: function(response){
                    this.openSettings.call(this, Ext.decode( response.responseText ) );
                },
                failure: function(response){
                    this.openSettings.call(this, null );
                }
            });
        }
    },
    getAppSummary: function() {
        return i18n._("Intrusion Prevention blocks scans, detects, and blocks attacks and suspicious traffic using signatures.");
    },
    gridRules: null,
    gridVariables: null,
    gridEventLog: null,
    statistics: null,
    regexRuleSid: /\s+sid:\s*([^;]+);/,
    regexRuleGid: /\s+gid:\s*([^;]+);/,
    regexRuleVariable :  /^\$([A-Za-z0-9\_]+)/,
    regexRule: /^([#]+|)(alert|log|pass|activate|dynamic|drop|sdrop|reject)\s+(tcp|udp|icmp|ip)\s+([^\s]+)\s+([^\s]+)\s+([^\s]+)\s+([^\s]+)\s+([^\s]+)\s+\((.+)\)$/,
    regexRuleReference: /\s+reference:\s*([^\;]+)\;/g,
    getRuleId: function( rule ){
        var gid = "1";
        var sid = "1";
        if( this.regexRuleGid.test( rule ) ){
            gid = this.regexRuleGid.exec( rule )[1];
            this.regexRuleGid.lastIndex = 0;
        }
        if( this.regexRuleSid.test( rule ) ){
            sid = this.regexRuleSid.exec( rule )[1];
            this.regexRuleSid.lastIndex = 0;
        }
        return sid + "_" + gid;
    },
    isVariableUsed: function(variable) {
        if(Ext.isEmpty(variable)) {
            return false;
        }
        var rule, originalId, ruleMatches, variableMatches, j, internalId, d;
        var isUsed = false;
        this.gridRules.getStore().each(function( record ) {
            rule = record.get("rule");
            ruleMatches = this.regexRule.exec( rule );
            this.regexRule.lastIndex = 0;
            if( ruleMatches ) {
                originalId = record.get("originalId");
                for( j = 1; j < ruleMatches.length; j++ ) {
                    variableMatches = this.regexRuleVariable.exec( ruleMatches[j] );
                    this.regexRuleVariable.lastIndex = 0;
                    if(variable)
                    if( variableMatches && variableMatches.shift().indexOf(variable)!= -1) {
                        isUsed = true;
                        return false;
                    }
                }
            }
        }, this);
        return isUsed;
    },

    massageSettings: function(){
        var rules = this.getSettings().rules.list , rule, i;
        for(i=0; i<rules.length; i++) {
            rule = rules[i];
            rule.originalId = this.getRuleId( rule.rule );
        }

    },

    /*
     * Import/export functions for Rules and Variables.
     * Instead of creating new classes for grids and import settings window,
     * just attach these appropriately when the objects are created.
     */
     /*
      * Grid's onImportContinue override.
      */
    gridOnImportContinue: function (importMode, importedRows) {
        var invalidRecords=0;
        if(importedRows == null) {
            importedRows=[];
        }
        var records=[];

        /*
         * Validate fields.
         * Unlike import verification elsewhere, INTRUSION-PREVENTION variables are not associated with
         * classes so creating a record and letting it fail won't neccessarily work.
         * To play it safe, verify that the keys from the first record in the store
         * match the imported row set.
         */
        var keysMatch = true;
        var recordKeys = Object.keys(this.getStore().getAt(0).data);
        /*
         * From the record key list, remove unneccessary fields.
         */
        var fpos = recordKeys.indexOf("internalId");
        if( fpos != -1){
            recordKeys.splice(fpos,1);
        }
        fpos = recordKeys.indexOf("originalId");
        if( fpos != -1){
            recordKeys.splice(fpos,1);
        }
        fpos = recordKeys.indexOf("id");
        if( fpos != -1){
            recordKeys.splice(fpos,1);
        }
        var importKeys = Object.keys(importedRows[0]);  
        if( recordKeys.length != importKeys.length){
            keysMatch = false;
        }else{
            for( var j = 0; j < recordKeys.length; j++){
                if(recordKeys[j] == "internalId"){
                    continue;
                }
                if(importKeys.indexOf(recordKeys[j]) == -1){
                    keysMatch = false;
                }
            }
        }

        if( keysMatch == false){
            Ext.MessageBox.alert(i18n._('Warning'), i18n._("Import failed. Imported file has no records for this data."));
            return;
        }

        for (var i = 0; i < importedRows.length; i++) {
            try {
                var record= Ext.create(Ext.getClassName(this.getStore().getProxy().getModel()), importedRows[i]);
                record.set("internalId", this.genAddedId());
                if( this.name == "Rules"){
                    this.updateRule(record, null);
                }
                records.push(record);
            } catch(e) {
                invalidRecords++;
            }
        }

        var validRecords=records.length;
        if(validRecords > 0) {
            if(importMode=='replace' ) {
                this.deleteAllRecords();
                this.getStore().insert(0, records);
                this.updateChangedDataOnImport(records, "added");
            } else {
                if(importMode=='append') {
                    this.getStore().add(records);
                } else if(importMode=='prepend') { //replace or prepend mode
                    this.getStore().insert(0, records);
                }
                this.updateChangedDataOnImport(records, "added");
            }
        }
        this.reconfigure();
        this.importSettingsWindow.close();
        if(validRecords > 0) {
            if(invalidRecords==0) {
                Ext.MessageBox.alert(i18n._('Import successful'), Ext.String.format(i18n._("Imported file contains {0} valid records."), validRecords));
            } else {
                Ext.MessageBox.alert(i18n._('Import successful'), Ext.String.format(i18n._("Imported file contains {0} valid records and {1} invalid records."), validRecords, invalidRecords));
            }
        } else {
            if(invalidRecords==0) {
                Ext.MessageBox.alert(i18n._('Warning'), i18n._("Import failed. Imported file has no records."));
            } else {
                Ext.MessageBox.alert(i18n._('Warning'), Ext.String.format(i18n._("Import failed. Imported file contains {0} invalid records and no valid records."), invalidRecords));
            }
        }
    },

    /*
     * ImportSettingsWindow's updateAction override.
     */
    importSettingsWindowUpdateAction: function(){
        Ext.MessageBox.wait(i18n._("Importing Settings..."), i18n._("Please wait"));
        var me = this;

        /* Setup event for reading uploaded file directly into browser. */
        var fileField = this.down("[name=import_settings_textfield]");
        var file = fileField.eventFiles[0];
        var reader = new FileReader();
        reader.onload = function(event){
            var fileContents = atob(event.target.result.substr(event.target.result.indexOf("base64,") + 7));
            var json = null;
            try{
                json = Ext.decode(fileContents);
            }catch(e){
                console.log("json conversion failed");
            }
            if( json == null ){
                Ext.MessageBox.alert(i18n._('Warning'), i18n._("Import failed. Settings must be formatted as a JSON Array."));
                return;
            }
            Ext.Function.defer(me.grid.onImportContinue, 1, me.grid, [me.importMode, json]);
        };
        reader.onerror = function(){
            console.log('On Error Event');
        };
        reader.readAsDataURL(file);
    },

    initComponent: function() {
        this.massageSettings();
        var categories = [], categoriesMap = {}, classtypes=[], classtypesMap = {};
        var rules = this.getSettings().rules.list , rule, category, classtype, i;
        for(i=0; i<rules.length; i++) {
            rule = rules[i];
            category = rule.category;
            classtype = rule.classtype;
            if(!categoriesMap[category]) {
                categoriesMap[category] = true;
                categories.push([category, category]);
            }
            if(!classtypesMap[classtype]) {
                classtypesMap[classtype] = true;
                classtypes.push([classtype, classtype]);
            }
        }

        this.storeCategories = Ext.create('Ext.data.ArrayStore', {
            fields: ['id', 'value'],
            data: categories,
            sorters: [{
                property: "value", direction: "ASC"
            }]
        });
        this.storeClasstypes = Ext.create('Ext.data.ArrayStore', {
            fields: ['id', 'value'],
            data: classtypes,
            sorters: [{
                property: "value", direction: "ASC"
            }]
        });

        this.referencesMap = {
            "bugtraq": "http://www.securityfocus.com/bid/",
            "cve": "http://cve.mitre.org/cgi-bin/cvename.cgi?name=",
            "nessus": "http://cgi.nessus.org/plugins/dump.php3?id=",
            "arachnids": "http://www.whitehats.com/info/IDS",
            "mcafee": "http://vil.nai.com/vil/content/v",
            "osvdb": "http://osvdb.org/show/osvdb/",
            "msb": "http://technet.microsoft.com/en-us/security/bulletin/",
            "url": "http://"
        };

        var classtypesInfoList = [
            [ "attempted-admin", i18n._("Attempted Administrator Privilege Gain"), "high"],
            [ "attempted-user", i18n._("Attempted User Privilege Gain"), "high" ],
            [ "inappropriate-content", i18n._("Inappropriate Content was Detected"), "high" ],
            [ "policy-violation", i18n._("Potential Corporate Privacy Violation"), "high" ],
            [ "shellcode-detect", i18n._("Executable code was detected"), "high" ],
            [ "successful-admin", i18n._("Successful Administrator Privilege Gain"), "high" ],
            [ "successful-user", i18n._("Successful User Privilege Gain"), "high" ],
            [ "trojan-activity", i18n._("A Network Trojan was detected"), "high" ],
            [ "unsuccessful-user", i18n._("Unsuccessful User Privilege Gain"), "high" ],
            [ "web-application-attack", i18n._("Web Application Attack"), "high" ],

            [ "attempted-dos", i18n._("Attempted Denial of Service"), "medium" ],
            [ "attempted-recon", i18n._("Attempted Information Leak"), "medium" ],
            [ "bad-unknown", i18n._("Potentially Bad Traffic"), "medium" ],
            [ "default-login-attempt", i18n._("Attempt to login by a default username and password"), "medium" ],
            [ "denial-of-service", i18n._("Detection of a Denial of Service Attack"), "medium" ],
            [ "misc-attack", i18n._("Misc Attack"), "medium" ],
            [ "non-standard-protocol", i18n._("Detection of a non-standard protocol or event"), "medium" ],
            [ "rpc-portmap-decode", i18n._("Decode of an RPC Query"), "medium" ],
            [ "successful-dos", i18n._("Denial of Service"), "medium" ],
            [ "successful-recon-largescale", i18n._("Large Scale Information Leak"), "medium" ],
            [ "successful-recon-limited", i18n._("Information Leak"), "medium" ],
            [ "suspicious-filename-detect", i18n._("A suspicious filename was detected"), "medium" ],
            [ "suspicious-login", i18n._("An attempted login using a suspicious username was detected"), "medium" ],
            [ "system-call-detect", i18n._("A system call was detected"), "medium" ],
            [ "unusual-client-port-connection", i18n._("A client was using an unusual port"), "medium" ],
            [ "web-application-activity", i18n._("Access to a potentially vulnerable web application"), "medium" ],

            [ "icmp-event", i18n._("Generic ICMP event"), "low" ],
            [ "misc-activity", i18n._("Misc activity"), "low" ],
            [ "network-scan", i18n._("Detection of a Network Scan"), "low" ],
            [ "not-suspicious", i18n._("Not Suspicious Traffic"), "low" ],
            [ "protocol-command-decode", i18n._("Generic Protocol Command Decode"), "low" ],
            [ "string-detect", i18n._("A suspicious string was detected"), "low" ],
            [ "unknown", i18n._("Unknown Traffic"), "low" ],

            [ "tcp-connection", i18n._("A TCP connection was detected"), "low" ]
        ];
        this.classtypesInfoStore = Ext.create('Ext.data.ArrayStore', {
            fields: [ 'name', 'description', 'priority' ],
            data: classtypesInfoList
        });
        this.classtypesInfoMap = Ung.Util.createStoreMap(classtypesInfoList);
        
        var categoriesInfoList = [
            ["app-detect", i18n._("This category contains rules that look for, and control, the traffic of certain applications that generate network activity. This category will be used to control various aspects of how an application behaves.") ],
            ["blacklist", i18n._("This category contains URI, USER-AGENT, DNS, and IP address rules that have been determined to be indicators of malicious activity. These rules are based on activity from the Talos virus sandboxes, public list of malicious URLs, and other data sources.") ],
            ["browser-chrome", i18n._("This category contains detection for vulnerabilities present in the Chrome browser. (This is separate from the browser-webkit category, as Chrome has enough vulnerabilities to be broken out into it's own, and while it uses the Webkit rendering engine, there's a lot of other features to Chrome.)") ],
            ["browser-firefox", i18n._("This category contains detection for vulnerabilities present in the Firefox browser, or products that have the Gecko engine. (Thunderbird email client, etc)") ],
            ["browser-ie", i18n._("This category contains detection for vulnerabilities present in the Internet Explorer browser (Trident or Tasman engines)") ],
            ["browser-webkit", i18n._("This category contains detection of vulnerabilities present in the Webkit browser engine (aside from Chrome) this includes Apple's Safari, RIM's mobile browser, Nokia, KDE, Webkit itself, and Palm.") ],
            ["browser-other", i18n._("This category contains detection for vulnerabilities in other browsers not listed above.") ],
            ["browser-plugins", i18n._("This category contains detection for vulnerabilities in browsers that deal with plugins to the browser. (Example: Active-x)") ],
            ["content-replace", i18n._("This category containt any rule that utilizes the replace functionality inside of Snort.") ],
            ["deleted", i18n._("When a rule has been deprecated or replaced it is moved to this categories. Rules are never totally removed from the ruleset, they are moved here.") ],
            ["exploit", i18n._("This is an older category which will be deprecated soon. This category looks for exploits against software in a generic form.") ],
            ["exploit-kit", i18n._("This category contains rules that are specifically tailored to detect exploit kit activity. This does not include post-compromise rules (as those would be in indicator-compromise). Files that are dropped as result of visiting an exploit kit would be in their respective file category.") ],
            ["file-executable", i18n._("This category contains rules for vulnerabilities that are found or are delivered through executable files, regardless of platform.") ],
            ["file-flash", i18n._("This category contains rules for vulnerabilities that are found or are delivered through flash files. Either compressed or uncompressed, regardless of delivery method platform being attacked.") ],
            ["file-image", i18n._("This category contains rules for vulnerabilities that are found inside of images files. Regardless of delivery method, software being attacked, or type of image. (Examples include: jpg, png, gif, bmp, etc)") ],
            ["file-identify", i18n._("This category is to identify files through file extension, the content in the file (file magic), or header found in the traffic. This information is usually used to then set a flowbit to be used in a different rule.") ],
            ["file-multimedia", i18n._("This category contains rules for vulnerabilities present inside of multimedia files (mp3, movies, wmv)") ],
            ["file-office", i18n._("This category contains rules for vulnerabilities present inside of files belonging to the Microsoft Office suite of software. (Excel, PowerPoint, Word, Visio, Access, Outlook, etc)") ],
            ["file-pdf", i18n._("This category contains rules for vulnerabilities found inside of PDF files. Regardless of method of creation, delivery method, or which piece of software the PDF affects (for example, both Adobe Reader and FoxIt Reader)") ],
            ["file-other", i18n._("This category contains rules for vulnerabilities present inside a file, that doesn't fit into the other categories above.") ],
            ["indicator-compromise", i18n._("This category contains rules that are clearly to be used only for the detection of a positively compromised system, false positives may occur.") ],
            ["indicator-obfuscation", i18n._("This category contains rules that are clearly used only for the detection of obfuscated content. Like encoded JavaScript rules.") ],
            ["indicator-shellcode", i18n._("This category contains rules that are simply looking for simple identification markers of shellcode in traffic. This replaces the old shellcode.rules.") ],
            ["malware-backdoor", i18n._("This category contains rules for the detection of traffic destined to known listening backdoor command channels. If a piece of malicious soft are opens a port and waits for incoming commands for its control functions, this type of detection will be here. A simple example would be the detection for BackOrifice as it listens on a specific port and then executes the commands sent.") ],
            ["malware-cnc", i18n._("This category contains known malicious command and control activity for identified botnet traffic. This includes call home, downloading of dropped files, and ex-filtration of data. Actual commands issued from Master to Zombie type stuff will also be here.") ],
            ["malware-tools", i18n._("This category contains rules that deal with tools that can be considered malicious in nature. For example, LOIC.") ],
            ["malware-other", i18n._("This category contains rules that are malware related, but don't fit into one of the other malware categories.") ],
            ["os-linux", i18n._("This category contains rules that are looking for vulnerabilities in Linux based OSes. Not for browsers or any other software on it, but simply against the OS itself.") ],
            ["os-solaris", i18n._("This category contains rules that are looking for vulnerabilities in Solaris based OSes. Not for any browsers or any other software on top of the OS.") ],
            ["os-windows", i18n._("This category contains rules that are looking for vulnerabilities in Windows based OSes. Not for any browsers or any other software on top of the OS.") ],
            ["os-other", i18n._("This category contains rules that are looking for vulnerabilities in an OS that is not listed above.") ],
            ["preprocessor_portscan", i18n._("This category contains rules that are looking for vulnerabilities via portscans.") ],
            ["policy-multimedia", i18n._("This category contains rules that detect potential violations of policy for multimedia. Examples like the detection of the use of iTunes on the network. This is not for vulnerabilities found within multimedia files, as that would be in file-multimedia.") ],
            ["policy-social", i18n._("This category contains rules for the detection potential violations of policy on corporate networks for the use of social media. (p2p, chat, etc)") ],
            ["policy-other", i18n._("This category is for rules that may violate the end-users corporate policy bud do not fall into any of the other policy categories first.") ],
            ["policy-spam", i18n._("This category is for rules that may indicate the presence of spam on the network.") ],
            ["protocol-finger", i18n._("This category is for rules that may indicate the presence of the finger protocol or vulnerabilities in the finger protocol on the network.") ],
            ["protocol-ftp", i18n._("This category is for rules that may indicate the presence of the ftp protocol or vulnerabilities in the ftp protocol on the network.") ],
            ["protocol-icmp", i18n._("This category is for rules that may indicate the presence of icmp traffic or vulnerabilities in icmp on the network.") ],
            ["protocol-imap", i18n._("This category is for rules that may indicate the presence of the imap protocol or vulnerabilities in the imap protocol on the network.") ],
            ["protocol-pop", i18n._("This category is for rules that may indicate the presence of the pop protocol or vulnerabilities in the pop protocol on the network.") ],
            ["protocol-services", i18n._("This category is for rules that may indicate the presence of the rservices protocol or vulnerabilities in the rservices protocols on the network.") ],
            ["protocol-voip", i18n._("This category is for rules that may indicate the presence of voip services or vulnerabilities in the voip protocol on the network.") ],
            ["pua-adware", i18n._("This category deals with pua or Potentially Unwanted Applications that deal with adware or spyware.") ],
            ["pua-p2p", i18n._("This category deals with pua or Potentially Unwanted Applications that deal with p2p.") ],
            ["pua-toolbars", i18n._("This category deals with pua or Potentially Unwanted Applications that deal with toolbars installed on the client system. (Google Toolbar, Yahoo Toolbar, Hotbar, etc)") ],
            ["pua-other", i18n._("This category deals with pua or Potentially Unwanted Applications that don't fit into one of the categories shown above.") ],
            ["server-apache", i18n._("This category deals with vulnerabilities in or attacks against the Apache Web Server.") ],
            ["server-iis", i18n._("This category deals with vulnerabilities in or attacks against the Microsoft IIS Web server.") ],
            ["server-mssql", i18n._("This category deals with vulnerabilities in or attacks against the Microsoft SQL Server.") ],
            ["server-mysql", i18n._("This category deals with vulnerabilities in or attacks against Oracle's MySQL server.") ],
            ["server-oracle", i18n._("This category deals with vulnerabilities in or attacks against Oracle's Oracle DB Server.") ],
            ["server-webapp", i18n._("This category deals with vulnerabilities in or attacks against Web based applications on servers.") ],
            ["server-mail", i18n._("This category contains rules that detect vulnerabilities in mail servers. (Exchange, Courier). These are separate from the protocol categories, as those deal with the traffic going to the mail servers itself.") ],
            ["server-other", i18n._("This category contains rules that detect vulnerabilities in or attacks against servers that are not detailed in the above list.") ]
        ];
        this.categoriesInfoStore = Ext.create('Ext.data.ArrayStore', {
            fields: [ 'name', 'description' ],
            data: categoriesInfoList
        });
        this.categoriesInfoMap = Ung.Util.createStoreMap(categoriesInfoList);
        

        this.lastUpdate = this.getRpcNode().getLastUpdate();
        this.lastUpdateCheck = this.getRpcNode().getLastUpdateCheck();

        this.buildRules();
        this.buildVariables();
        this.buildTabPanel([this.gridRules, this.gridVariables]);
        this.callParent(arguments);

    },
    // Status Panel
    buildStatus: function() {
        this.panelStatus = Ext.create('Ung.panel.Status', {
            settingsCmp: this,
            helpSource: 'intrusion_prevention_status', 
            itemsAfterLicense: [{
                title: i18n._("Setup Wizard"),
                items: [{
                    xtype: 'component',
                    html: i18n._("Intrusion Prevention is unconfigured. Use the Wizard to configure Intrusion Prevention."),
                    cls: 'warning',
                    hidden: this.getSettings().configured
                }, {
                    xtype: "button",
                    name: 'setup_wizard_button',
                    margin: '10 0 0 0',
                    text: i18n._("Run Intrusion Detection/Prevention Setup Wizard"),
                    iconCls: "action-icon",
                    handler: Ext.bind(function() {
                        this.setupWizard();
                    }, this)
                }]
            }, {
                title: i18n._('Note'),
                html: Ext.String.format(i18n._("{0} continues to maintain the default signature settings through automatic updates. You are free to modify and add signatures, however it is not required."), rpc.companyName)
            },{
                title: i18n._("Updates"),
                items: [{
                    xtype: 'displayfield',
                    fieldLabel: i18n._("Last check for updates"),
                    name: 'lastUpdateCheck',
                    labelWidth:200,
                    value: ( this.lastUpdateCheck !== null && this.lastUpdateCheck.time !== 0 ) ? i18n.timestampFormat(this.lastUpdateCheck) : i18n._("Never")
                },{
                    xtype: 'displayfield',
                    fieldLabel: i18n._("Last update"),
                    name: 'lastUpdateCheck',
                    labelWidth:200,
                    value: ( this.lastUpdate !== null && this.lastUpdate.time !== 0 && this.lastUpdateCheck !== null && this.lastUpdateCheck.time !== 0 ) ? i18n.timestampFormat(this.lastUpdate) : i18n._("Never")
                }]
            }]
        });
    },
    // Rules Panel
    buildRules: function() {
        var me = this;
        this.gridRules = Ext.create('Ung.grid.Panel', {
            helpSource: 'intrusion_prevention_rules',
            name: 'Rules',
            groupField: 'classtype',
            settingsCmp: this,
            title: i18n._("Rules"),
            dataProperty: 'rules',
            plugins: ['gridfilters'],
            features: [{
                ftype: 'grouping',
                groupHeaderTpl: '{columnName}: {name} ({rows.length} rule{[values.rows.length > 1 ? "s" : ""]})',
                startCollapsed: true
            }],
            updateRulesStatus: function(){
                var hasLogOrBlockFilter = this.findLog.getValue() || this.findBlock.getValue();
                var hasFilter = hasLogOrBlockFilter || (this.searchField.getValue().length>=2);
                var statusText = "", logOrBlockText = "", totalEnabled = 0;
                if(!hasLogOrBlockFilter) {
                    this.store.each(function( record ){
                        if( ( record.get('log')) || ( record.get('block')) ) {
                            totalEnabled++;
                        }
                    });
                    logOrBlockText = Ext.String.format(i18n._("{0} logging or blocking"), totalEnabled);
                }
                if(hasFilter) {
                    statusText = Ext.String.format(i18n._('{0} matching rules(s) found'), this.getStore().count());
                    if(!hasLogOrBlockFilter) {
                        statusText += ', ' + logOrBlockText;
                    }
                } else {
                    statusText = Ext.String.format(i18n._("{0} available rules"), this.getStore().getCount()) + ', ' + logOrBlockText;
                }
                this.searchStatusBar.update(statusText);
            },
            initComponent: function() {
                this.logFilter = Ext.create('Ext.util.Filter', {
                    id: 'logFilter',
                    property: 'log',
                    value: true
                });
                this.blockFilter = Ext.create('Ext.util.Filter', {
                    id: 'blockFilter',
                    property: 'block',
                    value: true
                });
                this.filterFeature=Ext.create('Ung.grid.feature.GlobalFilter', {
                    useVisibleColumns: false,
                    searchFields: ['category', 'rule']
                });
                this.features.push(this.filterFeature);
                
                this.bbar = [i18n._('Search'), {
                    xtype: 'textfield',
                    name: 'searchField',
                    margin: '0 10 0 0',
                    width: 200,
                    listeners: {
                        change: {
                            fn: function(elem, newValue, oldValue, eOpts) {
                                var searchValue = (newValue.length < 2)?"":newValue;
                                this.filterFeature.updateGlobalFilter(searchValue, false);
                            },
                            scope: this,
                            buffer: 800
                        }
                    }
                },{
                    xtype: 'checkbox',
                    name: 'searchLog',
                    boxLabel: i18n._("Log"),
                    margin: '0 10 0 0',
                    listeners: {
                        change: {
                            fn: function(elem, newValue, oldValue, eOpts) {
                                if (newValue) {
                                    this.getStore().addFilter(this.logFilter);
                                } else {
                                    this.getStore().removeFilter(this.logFilter);
                                }
                            },
                            scope: this
                        }
                    }
                }, {
                    xtype: 'checkbox',
                    name: 'searchBlock',
                    boxLabel: i18n._("Block"),
                    margin: '0 10 0 0',
                    listeners: {
                        change: {
                            fn: function(elem, newValue, oldValue, eOpts) {
                                if (newValue) {
                                    this.getStore().addFilter(this.blockFilter);
                                } else {
                                    this.getStore().removeFilter(this.blockFilter);
                                }
                            },
                            scope: this
                        }
                    }
                },{
                    xtype: 'tbtext',
                    name: 'searchStatusBar',
                    html: i18n._('Loading...')
                }];
                Ung.grid.Panel.prototype.initComponent.apply(this, arguments);
                this.getStore().addFilter(this.filterFeature.globalFilter);

                this.searchField = this.down('textfield[name=searchField]');
                this.findLog = this.down('checkbox[name=searchLog]');
                this.findBlock = this.down('checkbox[name=searchBlock]');
                this.searchStatusBar = this.down('tbtext[name=searchStatusBar]');
                this.getStore().getFilters().on('endupdate', Ext.bind(function(eOpts) {
                    this.updateRulesStatus();
                }, this));
                this.updateRulesStatus();

                this.importSettingsWindow = Ext.create('Ung.ImportSettingsWindow',{
                    grid: this
                });
                this.importSettingsWindow.updateAction = this.settingsCmp.importSettingsWindowUpdateAction;
            },
            onImportContinue: this.gridOnImportContinue,
            exportHandler: function(){
                Ext.MessageBox.wait(i18n._("Exporting Settings..."), i18n._("Please wait"));
                var changedDataSet = {};
                changedDataSet.rules = this.changedData;

                var downloadForm = document.getElementById('downloadForm'); 
                downloadForm["type"].value = "IntrusionPreventionSettings";
                downloadForm["arg1"].value = "export";
                downloadForm["arg2"].value = this.up("window").nodeId;
                downloadForm["arg3"].value = this.name;
                downloadForm["arg4"].value = Ext.encode(changedDataSet);
                downloadForm.submit();
                Ext.MessageBox.hide();
            },
            emptyRow: {
                "originalId": "1_1",
                "classtype": "unknown",
                "category": "app-detect",
                "msg" : "new rule",
                "sid": "1999999",
                "log": true,
                "block": false,
                "rule": "alert tcp any any -> any any ( msg:\"new rule\"; classtype:unknown; sid:1999999; content:\"matchme\"; nocase;)"
            },
            sortField: 'category',
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
            columns: [{
                header: i18n._("Sid"),
                dataIndex: 'sid',
                sortable: true,
                width: 70,
                editor: null,
                menuDisabled: false,
                renderer: function( value, metaData, record, rowIdx, colIdx, store ){
                    var id = record.get("originalId");
                    if(id != null){
                        id = id.split("_");
                        metaData.tdAttr = 'data-qtip="' + Ext.String.htmlEncode( i18n._("Sid:") + id[0] + ", " + i18n._("Gid:") +id[1]) + '"';
                    }
                    return value;
                }
            },{
                header: i18n._("Classtype"),
                dataIndex: 'classtype',
                sortable: true,
                width: 100,
                flex:1,
                editor: null,
                menuDisabled: false,
                renderer: function( value, metaData, record, rowIdx, colIdx, store ){
                    var description = value;
                    if( this.up("window") ){
                        description = this.up("window").classtypesInfoMap[value];
                    }
                    metaData.tdAttr = 'data-qtip="' + Ext.String.htmlEncode(description!=null?description: value ) + '"';
                    return value;
                }
            },{
                header: i18n._("Category"),
                dataIndex: 'category',
                sortable: true,
                width: 100,
                flex:1,
                menuDisabled: false,
                renderer: function( value, metaData, record, rowIdx, colIdx, store ){
                    var description = value;
                    if( this.up("window") ){
                        description = this.up("window").categoriesInfoMap[value];
                    }
                    metaData.tdAttr = 'data-qtip="' + Ext.String.htmlEncode(description!=null?description: value) + '"';
                    return value;
                }
            },{
                header: i18n._("Msg"),
                dataIndex: 'msg',
                sortable: true,
                width: 200,
                flex:3,
                editor: null,
                menuDisabled: false
            },{
                header: i18n._("Reference"),
                dataIndex: 'rule',
                sortable: true,
                width: 100,
                flex:1,
                menuDisabled: false,
                renderer: function( value, metaData, record, rowIdx, colIdx, store ){
                    var matches = null;
                    if(this.up("window")){
                        matches = value.match(this.up("window").regexRuleReference);
                    }
                    if( matches == null ){
                        return "";
                    }
                    var references = [];
                    for( var i = 0; i < matches.length; i++ ){
                        var rmatches = this.up("window").regexRuleReference.exec( matches[i] );
                        this.up("window").regexRuleReference.lastIndex = 0;

                        var url = "";
                        var referenceFields = rmatches[1].split(",");
                        var prefix = this.up("window").referencesMap[referenceFields[0]];
                        if( prefix != null ){
                            referenceFields[1] = referenceFields[1].trim();
                            if((referenceFields[1].charAt(0) == '"') && 
                                (referenceFields[1].charAt(referenceFields[1].length - 1) == '"')){
                                referenceFields[1] = referenceFields[1].substr(1,referenceFields[1].length - 2);
                            }
                            url = prefix + referenceFields[1];
                            references.push('<a href="'+ url + '" class="icon-detail-row href-icon" target="_reference"></a>');
                        }
                    }
                    return references.join("");
                }
            },{
                xtype:'checkcolumn',
                header: i18n._("Log"),
                dataIndex: 'log',
                sortable: true,
                resizable: false,
                width:55,
                menuDisabled: false,
                listeners: {
                    beforecheckchange: Ext.bind(function ( elem, rowIndex, checked ){
                        var record = elem.getView().getRecord(rowIndex);
                        if( !checked){
                            record.set('log', false);
                            record.set('block', false );
                        }else{
                            record.set('log', true);
                        }
                        this.gridRules.updateRule(record, null );
                    }, this)
                },
                checkAll: {
                    handler: function(checkbox, checked) {
                        Ext.MessageBox.wait(checked?i18n._("Checking All ..."):i18n._("Unchecking All ..."), i18n._("Please wait"));
                        Ext.Function.defer(function() {
                            var grid=checkbox.up("grid");
                            var records=grid.getStore().getRange();
                            grid.getStore().suspendEvents(true);
                            for(var i=0; i<records.length; i++) {
                                records[i].set('log', checked);
                                if(!checked) {
                                    records[i].set('block', false);
                                }
                                grid.updateRule(records[i], null );
                            }
                            grid.getStore().resumeEvents();
                            grid.getStore().getFilters().notify('endupdate');
                            Ext.MessageBox.hide();
                        }, 100, this);
                    }
                }
            },{
                xtype:'checkcolumn',
                header: i18n._("Block"),
                dataIndex: 'block',
                sortable: true,
                resizable: false,
                width:55,
                menuDisabled: false,
                listeners: {
                    beforecheckchange: Ext.bind(function ( elem, rowIndex, checked ){
                        var record = elem.getView().getRecord(rowIndex);
                        if(checked) {
                            record.set('log', true );
                            record.set('block', true);
                        }else{
                            record.set('block', false);
                        }
                        this.gridRules.updateRule(record, null );
                    }, this)
                },
                checkAll: {
                    handler: function(checkbox, checked) {
                        Ext.MessageBox.wait(checked?i18n._("Checking All ..."):i18n._("Unchecking All ..."), i18n._("Please wait"));
                        Ext.Function.defer(function() {
                            var grid=checkbox.up("grid");
                            var records=grid.getStore().getRange();
                            grid.getStore().suspendEvents(true);
                            for(var i=0; i<records.length; i++) {
                                records[i].set('block', checked);
                                if(checked) {
                                    records[i].set('log', true);
                                }
                                grid.updateRule(records[i], null );
                            }
                            grid.getStore().resumeEvents();
                            grid.getStore().getFilters().notify('endupdate');
                            Ext.MessageBox.hide();
                        }, 100, this);
                    }
                }
            }],
            rowEditorInputLines: [{
                name: "Classtype",
                dataIndex: "classtype",
                fieldLabel: i18n._("Classtype"),
                emptyText: i18n._("[enter class]"),
                allowBlank: false,
                width: 400,
                xtype: 'combo',
                queryMode: 'local',
                store: this.storeClasstypes,
                valueField: 'id',
                displayField: 'value',
                regexMatch: /\s+classtype:([^;]+);/,
                validator: function( value ){
                    if( this.store.find( "value", value ) == -1 ){
                        return i18n._("Invalid Classtype");
                    }
                    return true;
                },
                listeners: {
                    select: function( combo, record, eOpts ){
                        var rule = this.up("window").down("[name=Rule]");
                        var ruleValue = rule.getValue();

                        var newField = " classtype:" + combo.getValue() + ";";
                        if( this.regexMatch.test( ruleValue )) {
                            ruleValue = ruleValue.replace( this.regexMatch, newField );
                        } else {
                            var idx = ruleValue.lastIndexOf(")");
                            if(idx != -1) {
                                ruleValue = ruleValue.slice(0, idx-1) + newField +ruleValue.slice(idx-1);
                            } else {
                                ruleValue += " ("+newValue+" )";
                            }
                        }
                        rule.setRawValue(ruleValue);
                    }
                }
            },{
                dataIndex: "original_id",
                xtype: 'hidden'
            },{
                name: "Category",
                fieldLabel: i18n._("Category"),
                dataIndex: "category",
                emptyText: i18n._("[enter category]"),
                allowBlank: false,
                width: 400,
                xtype: 'combo',
                queryMode: 'local',
                store: this.storeCategories,
                valueField: 'id',
                displayField: 'value'
            },{
                xtype:'textfield',
                name: "Msg",
                dataIndex: "msg",
                fieldLabel: i18n._("Msg"),
                emptyText: i18n._("[enter name]"),
                allowBlank: false,
                width: 400,
                regexMatch: /\s+msg:"([^;]+)";/,
                validator: function( value ){
                    if( /[";]/.test(value) ){
                        return i18n._("Msg contains invalid characters.");
                    }
                    return true;
                },
                listeners: {
                    change: function( me, newValue, oldValue, eOpts ){
                        var rule = this.up("window").down("[name=Rule]");
                        var ruleValue = rule.getValue();

                        var newField = " msg:\"" + newValue + "\";";
                        if( this.regexMatch.test( ruleValue )) {
                            ruleValue = ruleValue.replace( this.regexMatch, newField );
                        } else {
                            var idx = ruleValue.indexOf("(");
                            if(idx != -1) {
                                ruleValue = ruleValue.slice(0, idx+1) + newField +ruleValue.slice(idx+1);
                            } else {
                                ruleValue += " ("+newValue+" )";
                            }
                        }
                        rule.setRawValue(ruleValue);
                    }
                }
            },{
                xtype:'numberfield',
                name: "Sid",
                dataIndex: "sid",
                fieldLabel: i18n._("Sid"),
                emptyText: i18n._("[enter sid]"),
                allowBlank: false,
                width: 400,
                hideTrigger: true,
                regexMatch: /\s+sid:([^;]+);/,
                gidRegex: /\s+gid:\s*([^;]+);/,
                validator: function( ourValue ) {
                    var ruleEditor = this.up("window");
                    if( ! /^[0-9]+$/.test( ourValue )){
                        return i18n._("Sid must be numeric");
                    }
                    var record = ruleEditor.record;
                    var rule = ruleEditor.down("[name=Rule]");
                    var ourGid = "1", ruleGid, ruleValue;
                    if( this.gidRegex.test( rule.getValue() )){
                        ourGid = this.gidRegex.exec( rule.getValue() )[1];
                        this.gidRegex.lastIndex = 0;
                    }

                    var match = false;
                    ruleEditor.grid.getStore().each( function( storeRecord ) {
                        if( storeRecord != record && storeRecord.get("sid") == ourValue) {
                            ruleGid = "1";
                            ruleValue = storeRecord.get("rule");
                            if( this.gidRegex.test( ruleValue ) ) {
                                ruleGid = this.gidRegex.exec( ruleValue )[1];
                                this.gidRegex.lastIndex = 0;
                            }

                            if( ourGid == ruleGid ){
                                match = true;
                                return false;
                            }
                        }
                    }, this);
                    if( match === true ){
                        return i18n._("Sid already in use.");
                    }
                    return true;
                },
                listeners: {
                    change: function( me, newValue, oldValue, eOpts ) {
                        var rule = this.up("window").down("[name=Rule]");
                        var ruleValue = rule.getValue();

                        var newField = " sid:" + newValue + ";";
                        if( this.regexMatch.test( ruleValue )) {
                            ruleValue = ruleValue.replace( this.regexMatch, newField );
                        } else {
                            var idx = ruleValue.lastIndexOf(")");
                            if(idx != -1) {
                                ruleValue = ruleValue.slice(0, idx-1) + newField +ruleValue.slice(idx-1);
                            } else {
                                ruleValue += " ("+newValue+" )";
                            }
                        }
                        rule.setRawValue(ruleValue);
                    }
                }
            },{
                xtype:'checkbox',
                name: "Log",
                dataIndex: "log",
                fieldLabel: i18n._("Log"),
                listeners: {
                    change: function( me, newValue, oldValue, eOpts ) {
                        var ruleEditor = this.up("window");
                        if( !newValue ) {
                            ruleEditor.down("[dataIndex=block]").setValue(false);
                        }
                        ruleEditor.grid.updateRule(ruleEditor.record, ruleEditor);
                    }
                }
            },{
                xtype:'checkbox',
                name: "Block",
                dataIndex: "block",
                fieldLabel: i18n._("Block"),
                listeners: {
                    change: function( me, newValue, oldValue, eOpts ) {
                        var ruleEditor = this.up("window");
                        if( newValue === true ){
                            ruleEditor.down("[dataIndex=log]").setValue(true);
                        }  
                        ruleEditor.grid.updateRule(ruleEditor.record, ruleEditor);
                    }
                }
            },{
                xtype:'textareafield',
                name: "Rule",
                dataIndex: "rule",
                fieldLabel: i18n._("Rule"),
                emptyText: i18n._("[enter rule]"),
                allowBlank: false,
                width: 500,
                height: 150,
                actionRegexMatch: /^([#]+|)(alert|log|pass|activate|dynamic|drop|sdrop|reject)/,
                regexMatch: /^([#]+|)(alert|log|pass|activate|dynamic|drop|sdrop|reject)(\s+(tcp|udp|icmp|ip)\s+([^\s]+)\s+([^\s]+)\s+([^\s]+)\s+([^\s]+)\s+([^\s]+))?\s+\((.+)\)$/,
                validator: function( value ){
                    if( !this.regexMatch.test(value)){
                        return i18n._("Rule formatted wrong.");
                    }
                    return true;
                },
                listeners: {
                    change: function( me, newValue, oldValue, eOpts ){
                        var ruleEditor = this.up("window");
                        var value = this.getValue();

                        value = value.replace( /(\r\n|\n|\r)/gm, "" );

                        var updateFields = [ "Classtype", "Msg", "Sid" ]; 
                        var match;
                        for( var i = 0; i < updateFields.length; i++ ){
                            var field = ruleEditor.down("[name=" + updateFields[i] + "]");
                            if( field.regexMatch.test( value ) ){
                                match = field.regexMatch.exec( value );
                                field.setRawValue( match[1] );
                                field.validate();
                            }
                        }

                        // Action
                        var log = ruleEditor.down("[name=Log]");
                        var block = ruleEditor.down("[name=Block]");

                        var logValue = false;
                        var blockValue = false;
                        if( this.actionRegexMatch.test( value ) === true ){
                            match = this.actionRegexMatch.exec( value );
                            if( match[2] == "alert" ){
                                logValue = true;
                            }else if( ( match[2] == "drop" ) || ( match[2] == "sdrop" ) ){
                                logValue = true;
                                blockValue = true;
                            }
                            if( match[1] == "#" ){
                                logValue = false;
                                blockValue = false;
                            }
                        }
                        log.setRawValue(logValue);
                        block.setRawValue(blockValue);

                        this.setRawValue( value );
                    }
                }
            }],
            actionRegexMatch: /^([#]+|)(alert|log|pass|activate|dynamic|drop|sdrop|reject)/,
            updateRule: function( record, source ){
                // Rebuild rule according to record or form values
                var logValue = false;
                var blockValue = false;
                var ruleValue = "";
                if( source === null ) {
                    // Pull values from record
                    logValue = record.get("log");
                    blockValue = record.get("block");
                    ruleValue = record.get("rule");
                } else {
                    // Pull values from form
                    logValue = source.down("[dataIndex=log]").getValue();
                    blockValue = source.down("[dataIndex=block]").getValue();
                    ruleValue = source.down("[dataIndex=rule]").getValue();
                }

                var newField = "alert";
                if( logValue === true && blockValue === true ) {
                    newField = "drop";
                } else if( logValue === false && blockValue === true ) {
                    newField = "sdrop";
                } else if( logValue === false && blockValue === false ) {
                    newField = "#" + newField;
                }

                if( this.actionRegexMatch.test( ruleValue ) === true ) {
                    ruleValue = ruleValue.replace( this.actionRegexMatch, newField );
                } else {
                    ruleValue = ruleValue + newField;
                }

                if( source === null ) {
                    record.data.rule = ruleValue;
                } else {
                    source.down("[dataIndex=rule]").setRawValue(ruleValue);    
                }
            }
        });
    },

    // Variables Panel
    buildVariables: function() {
        var me = this;
        this.gridVariables = Ext.create('Ung.grid.Panel', {
            helpSource: 'intrusion_prevention_variables',
            title: i18n._("Variables"),
            name: 'Variables',
            settingsCmp: this,
            dataProperty: 'variables',
            recordJavaClass: "com.untangle.app.intrusion_prevention.IpsVariable",
            initComponent: function() {
                Ung.grid.Panel.prototype.initComponent.apply(this, arguments);
                this.importSettingsWindow = Ext.create('Ung.ImportSettingsWindow',{
                    grid: this
                });
                this.importSettingsWindow.updateAction = this.settingsCmp.importSettingsWindowUpdateAction;
            },
            onImportContinue: this.gridOnImportContinue,
            exportHandler: function(){
                Ext.MessageBox.wait(i18n._("Exporting Settings..."), i18n._("Please wait"));
                var changedDataSet = {};
                changedDataSet.variables = this.changedData;

                var downloadForm = document.getElementById('downloadForm'); 
                downloadForm["type"].value = "IntrusionPreventionSettings";
                downloadForm["arg1"].value = "export";
                downloadForm["arg2"].value = this.up("window").nodeId;
                downloadForm["arg3"].value = this.name;
                downloadForm["arg4"].value = Ext.encode(changedDataSet);
                downloadForm.submit();
                Ext.MessageBox.hide();
            },
            emptyRow: {
                "variable": "",
                "definition": "",
                "description": ""
            },
            sortField: 'variable',
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
            }, {
                name: 'originalId',
                mapping: 'variable'
            }],
            columns: [{
                header: i18n._("Name"),
                width: 170,
                dataIndex: 'variable'
            },{
                id: 'definition',
                header: i18n._("Definition"),
                width: 300,
                dataIndex: 'definition',
                editor: {
                    xtype:'textfield',
                    emptyText: i18n._("[enter definition]"),
                    allowBlank: false
                }
            },{
                header: i18n._("Description"),
                width: 300,
                dataIndex: 'description',
                flex:1,
                editor: {
                    xtype:'textfield',
                    emptyText: i18n._("[enter description]"),
                    allowBlank: false
                }
            }],
            deleteHandler: function( record ){
                Ext.MessageBox.wait(i18n._("Validating..."), i18n._("Please wait") );
                Ext.Function.defer(function() {
                    var variable = record.get('variable');
                    if(me.isVariableUsed(variable)) {
                        Ext.MessageBox.alert( i18n._("Cannot Delete Variable"), i18n._("Variable is used by one or more rules.") );
                    } else {
                        Ext.MessageBox.hide();
                        this.stopEditing();
                        this.updateChangedData(record, "deleted");
                    }
                }, 100, this);
            }
        });
        this.gridVariables.setRowEditor( Ext.create('Ung.RowEditorWindow',{
            inputLines: [{
                xtype: 'container',
                layout: 'column',
                margin: '0 0 5 0',
                items: [{
                    xtype:'textfield',
                    name: "Name",
                    dataIndex: "variable",
                    fieldLabel: i18n._("Name"),
                    emptyText: i18n._("[enter name]"),
                    allowBlank: false,
                    width: 300,
                    validator: function( ourValue ) {
                        var ruleEditor = this.up("window");
                        var match = false;
                        ruleEditor.grid.getStore().each(function(record) {
                            if( record != ruleEditor.record && record.get("variable") == ourValue) {
                                match = true;
                                return false;
                            }
                        }, this);
                        if( match === true ){
                            return i18n._("Variable name already in use.");
                        }
                        return true;
                    }
                }, {
                    xtype: 'label',
                    name: 'inUseNotice',
                    hidden: true,
                    html: i18n._("Variable is used by one or more rules."),
                    cls: 'boxlabel'
                }]
            },{
                xtype:'textfield',
                name: "Definition",
                dataIndex: "definition",
                fieldLabel: i18n._("Definition"),
                emptyText: i18n._("[enter definition]"),
                allowBlank: false,
                width: 400
            },{
                xtype:'textfield',
                name: "Description",
                dataIndex: "description",
                fieldLabel: i18n._("Description"),
                emptyText: i18n._("[enter description]"),
                allowBlank: false,
                width: 400
            }],
            populate: function(record, addMode) {
                var inUseNotice = this.down('component[name=inUseNotice]');
                inUseNotice.setVisible(false);
                Ext.Function.defer(function() {
                    var isUsed = me.isVariableUsed(record.get("variable"));
                    this.down('textfield[dataIndex=variable]').setReadOnly(isUsed);
                    inUseNotice.setVisible(isUsed);
                }, 100, this);
                Ung.RowEditorWindow.prototype.populate.apply(this, arguments);
            }
        }));
    },
    setupWizard: function() {
        var welcomeCard = Ext.create('Webui.intrusion-prevention.Wizard.Welcome', {
            i18n: i18n,
            gui: this
        });
        var classtypesCard = Ext.create('Webui.intrusion-prevention.Wizard.Classtypes', {
            i18n: i18n,
            gui: this
        });
        var categoriesCard = Ext.create('Webui.intrusion-prevention.Wizard.Categories', {
            i18n: i18n,
            gui: this
        });
        var congratulationsCard = Ext.create('Webui.intrusion-prevention.Wizard.Congratulations', {
            i18n: i18n,
            gui: this
        });
        var setupWizard = Ext.create('Ung.Wizard',{
            modalFinish: true,
            hasCancel: false, // cancel not working, still the window can be closed and acts like a cancel
            cardDefaults: {
                labelWidth: 200,
                cls: 'ung-panel'
            },
            cards: [welcomeCard, classtypesCard, categoriesCard, congratulationsCard],
            cancelAction: function() {
                this.up("window").close();
            }
        });
        this.wizardWindow = Ext.create('Ung.Window',{
            title: i18n._("Intrusion Prevention Setup Wizard"),
            items: setupWizard,
            maxWidth: 800,
            sizeToRack: false,
            border: false,
            closeWindow: Ext.bind(function() {
                this.wizardWindow.hide();
                Ext.destroy(this.wizardWindow);
                if(this.wizardCompleted) {
                    this.getSettings().configured = true;
                    Ext.apply(this.settings, this.wizardSettings);
                    this.markDirty();
                    // Save, enable, teardown wizard
                    this.afterSave = Ext.bind(function() {
                        this.afterSave = null;
                        var nodeCmp = Ung.Node.getCmp(this.nodeId);
                        if(nodeCmp) {
                            nodeCmp.start(Ext.bind(function() {
                                this.reload();
                            }, this));
                        }
                    }, this);
                    
                    this.applyAction();
                }
            }, this),
            listeners: {
                beforeclose: Ext.bind(function() {
                    var wizard = this.wizardWindow.down('panel[name="wizard"]');
                    if(!wizard.finished) {
                        wizard.finished=true;
                        Ext.MessageBox.alert(i18n._("Setup Wizard Warning"), i18n._("You have not finished configuring Intrusion Prevention. Please run the Setup Wizard again."), Ext.bind(function () {
                            this.wizardWindow.close();
                        }, this));
                        return false;
                    }
                    return true;
                }, this)
            }
        });
        this.wizardWindow.show();
    },
    beforeSave: function(isApply, handler) {
        this.getRpcNode().getUpdatedSettingsFlag(Ext.bind(function (result, exception) {
            if(Ung.Util.handleException(exception)) return;
            if(result) {
                Ext.MessageBox.alert(i18n._("Intrusion Prevention Warning"), i18n._("Settings have been changed by rule updater.  Current changes must be discarded."), Ext.bind(function () {
                    this.reload();
                }, this));
                return;
            }
            handler.call(this, isApply);
        }, this));
    },
    saveAction: function (isApply) {
        if(!this.isDirty()) {
            if(!isApply) {
                this.closeWindow();
            }
            return;
        }
        if(!this.validate(isApply)) {
            return;
        }
        // Override the default save message due to how long it takes for
        // python managment scripts and snort to restart.  Revisit if we
        // can figure out a way to make it within the ballpark of other
        // configuration settings.
        Ext.MessageBox.wait(i18n._("Saving and creating settings (this may take a few minutes)..."), i18n._("Please wait"));
        // Give the browser time to "breath" to bring up save progress bar.
        Ext.Function.defer(
            function(){
                if(Ext.isFunction(this.beforeSave)) {
                    this.beforeSave(isApply, this.save);
                } else {
                    this.save.call(this, isApply);
                }
            },
            100,
            this
        );
    },
    save: function(isApply) {
        // Due to the number of Snort rules, it takes too long to send everything back.
        // Instead, we send all settings except rules and variables.  Rules and variables
        // are sent as changedData sets from their grids.
        var changedDataSet = {};
        var keys = Object.keys(this.settings);
        for( var i = 0; i < keys.length; i++){
            if( ( keys[i] == "rules" ) || ( keys[i] == "variables" ) ||
                ( keys[i] == "activeGroups" && !this.wizardCompleted ) ){
                continue;
            }
            changedDataSet[keys[i]] = this.settings[keys[i]];
        }

        // This will always set rules/variables to minimally empty "diff"  objects if nothing  has changed
        changedDataSet.rules = this.gridRules.changedData;
        changedDataSet.variables = this.gridVariables.changedData;

        Ext.Ajax.request({
            url: "/webui/download",
            jsonData: changedDataSet,
            method: 'POST',
            params: {
                type: "IntrusionPreventionSettings",
                arg1: "save",
                arg2: this.nodeId
            },
            scope: this,
            timeout: 600000,
            success: function(response){
                var r = Ext.decode( response.responseText );
                if( !r.success) {
                    Ext.MessageBox.alert(i18n._("Error"), i18n._("Unable to save settings"));
                } else {
                    this.getRpcNode().reconfigure(Ext.bind(function(result, exception) {
                        if(Ung.Util.handleException(exception)) return;
                        if (!isApply) {
                            Ext.MessageBox.hide();
                            this.closeWindow();
                        } else {
                            var me = this;
                            Ext.Ajax.request({
                                url: "/webui/download",
                                method: 'POST',
                                params: {
                                    type: "IntrusionPreventionSettings",
                                    arg1: "load",
                                    arg2: this.nodeId
                                },
                                scope: this,
                                timeout: 600000,
                                success: function(response){
                                    me.settings = Ext.decode( response.responseText );
                                    me.massageSettings();
                                    me.clearDirty();
                                    Ext.MessageBox.hide();
                                    if(Ext.isFunction(me.afterSave)) {
                                        me.afterSave.call(me);
                                    }
                                },
                                failure: function(response){
                                    me.clearDirty();
                                    Ext.MessageBox.hide();
                                    if(Ext.isFunction(me.afterSave)) {
                                        me.afterSave.call(me);
                                    }
                                }
                            });
                        }
                    }, this));
                }
            },
            failure: function(response){
                Ext.MessageBox.alert(i18n._("Error"), i18n._("Unable to save settings"));
            }
        });
    }
});

// INTRUSION-PREVENTION wizard configuration cards.
Ext.define('Webui.intrusion-prevention.Wizard.Welcome',{
    constructor: function( config ) {
        Ext.apply(this, config);

        var items = [{
            xtype: 'component',
            html: '<h2 class="wizard-title">'+i18n._("Welcome to the Intrusion Prevention Setup Wizard!")+'</h2>',
            margin: '10'
        },{
            xtype: 'component',
            html: i18n._("Intrusion Prevention operates using rules to identify possible threats.  An enabled ruled performs an action, either logging or blocking traffic.  Not all rules are necessary for a given network environment and enabling all of them may negatively impact your network."),
            margin: '0 0 10 10'
        },{
            xtype: 'component',
            html: i18n._("This wizard is designed to help you correctly configure the appropriate amount of rules for your network by selecting rule identifiers: classtypes and categories.  The more that you select, the more rules will be enabled.  Again, too many enabled rules may negatively impact your network."),
            margin: '0 0 10 10'
        },{
            xtype: 'component',
            html: i18n._("It is highly suggested that you use Recommended values."),
            margin: '0 0 10 10'
        }];

        if( this.gui.getSettings().configured === true ){
            items.push({
                xtype: 'component',
                html: i18n._('WARNING: Completing this setup wizard will overwrite the previous settings with new settings. All previous settings will be lost!'),
                cls: 'warning',
                margin: '0 0 10 10'
            });
        }

        this.title = i18n._("Welcome");
        this.panel = Ext.create('Ext.container.Container',{
            items: items
        });

        this.onNext = Ext.bind( this.loadDefaultSettings, this );
    },

    loadDefaultSettings: function(handler){
        Ext.MessageBox.wait(i18n._("Determining recommended settings..."), i18n._("Please wait"));
        Ext.Ajax.request({
            url: "/webui/download",
            method: 'POST',
            params: {
                type: "IntrusionPreventionSettings",
                arg1: "wizard",
                arg2: this.gui.nodeId
            },
            scope: this,
            timeout: 600000,
            success: function(response){
                var wizardDefaults = Ext.decode( response.responseText );
                // Determine profile to use based on system stats.
                rpc.metricManager.getMetricsAndStats(Ext.bind(function(result, exception) {
                    if(Ung.Util.handleException(exception)) return;
                    var stats = result;
                    var memoryTotal = stats.systemStats["MemTotal"];
                    var architecture = stats.systemStats["architecture"];
                    if( architecture == "i386"){
                        architecture = "32";
                    } else if( architecture == "amd64") {
                        architecture = "64";
                    } else {
                        architecture = "unknown";
                    }
                    
                    var profile, match, systemStats;
                    for( var p = 0; p < wizardDefaults.profiles.length; p++ ) {
                        match = false;
                        systemStats = wizardDefaults.profiles[p].systemStats;
                        for( var statKey in systemStats ){
                            if( statKey == "MemTotal") {
                                match = Ext.isEmpty(systemStats[statKey]) || ( memoryTotal < parseFloat(systemStats[statKey] * 1.10 ) ) ;
                            } else if( statKey == "architecture") {
                                match = ( architecture == systemStats[statKey] );
                            } else {
                                match = ( stats.systemStats[statKey] == systemStats[statKey] );
                            }
                            if(!match){
                                break;
                            }
                        }
                        if( match){
                            profile = wizardDefaults.profiles[p];
                            profile.profileVersion = wizardDefaults.version;
                            break;
                        }
                    }
                    // Preserve recommended values
                    this.gui.wizardRecommendedSettings = Ext.clone(profile);
                    this.gui.wizardSettings = Ext.clone(profile);
                    delete this.gui.wizardSettings.systemStats;
                    if( this.gui.getSettings().configured){
                        // Setup wizard already configured.  Pull current settings.
                        if( this.gui.getSettings().activeGroups ) {
                            Ext.apply(this.gui.wizardSettings.activeGroups, this.gui.getSettings().activeGroups);
                        }
                    }

                    Ext.MessageBox.hide();
                    handler();
                }, this));
            },
            failure: function(response){
                Ext.MessageBox.alert( i18n._("Setup Wizard Error"), i18n._("Unable to obtain default settings.  Please run the Setup Wizard again."), Ext.bind(function () {
                    this.gui.wizardWindow.hide();
                }, this));
            }
        });
    }
});

Ext.define('Webui.intrusion-prevention.Wizard.Classtypes',{
    constructor: function( config ) {
        Ext.apply(this, config);

        this.classtypesCheckboxGroup = {
            xtype: 'checkboxgroup',
            fieldLabel: i18n._("Classtypes"),
            columns: 1,
            items: []
        };

        this.gui.classtypesInfoStore.each( function(record){
            this.classtypesCheckboxGroup.items.push({
                boxLabel: record.get( 'name' ) + ' (' + record.get( 'priority' ) + ')',
                name: 'classtypes_selected',
                inputValue: record.get( 'name' ),
                listeners: {
                    render: function(c){
                        Ext.QuickTips.register({
                            target:  c.boxLabelEl,
                            text: record.get( 'description' ),
                            dismissDelay: 5000
                        });
                    },
                    destroy: function(c){
                        Ext.QuickTips.unregister(c.boxLabelEl);
                    }
                }
            });
        }, this );

        this.title = i18n._( "Classtypes" );
        this.panel = Ext.create('Ext.container.Container',{
            scrollable: true,
            items: [{
                xtype: 'container',
                html: '<h2 class="wizard-title">'+i18n._("Classtypes")+'</h2>',
                margin: '10'
            },{
                xtype: 'container',
                html: i18n._("Classtypes are a generalized  grouping for rules, such as attempts to gain user access."),
                margin: '0 10 10 10'
            },{
                name: 'classtypes',
                xtype: 'radio',
                inputValue: 'recommended',
                boxLabel: i18n._('Recommended (default)'),
                hideLabel: true,
                checked: false,
                handler: Ext.bind(function(elem, checked) {
                    this.setVisible( elem.inputValue, checked );
                }, this),
                margin: '0 10'
            },{
                name: 'classtypes_recommended_settings',
                xtype: 'fieldset',
                hidden: true,
                html: "<i>" + i18n._("Recommended classtype Settings") + "</i>",
                margin: '0 10'
            },{
                name: 'classtypes',
                xtype: 'radio',
                inputValue: 'custom',
                boxLabel: i18n._('Custom'),
                hideLabel: true,
                checked: false,
                handler: Ext.bind(function(elem, checked) {
                    this.setVisible( elem.inputValue, checked );
                }, this),
                margin: '0 10'
            },{
                name: 'classtypes_custom_settings',
                xtype:'fieldset',
                hidden: true,
                items: [
                    this.classtypesCheckboxGroup
                ],
                margin: '0 10'
            }]
        });

        this.onLoad = Ext.bind( this.onLoad, this );
        this.onNext = Ext.bind( this.getValues, this );
    },
    setVisible: function( id, checked ) {
        if( !checked ) return;
        Ext.Array.each( this.panel.query("fieldset"), function( c ) {
            if( c.name && c.name.indexOf('classtypes_') == 0 ) {
                c.setVisible( c.name.indexOf( id ) != -1 );
            }
        });
    },

    onLoad: function( handler ){
        if( !this.loaded ) {
            if( this.gui.wizardSettings.activeGroups ) {
                Ext.Array.each(this.panel.query("radio[name=classtypes]"), function(c) {
                    if( c.inputValue == this.gui.wizardSettings.activeGroups.classtypes ){
                        c.setValue(true);
                    }
                }, this);

                var i, value;
                var checkboxes = this.panel.query("checkbox[name=classtypes_selected]");
                for( i = 0; i < this.gui.wizardSettings.activeGroups.classtypesSelected.length; i++ ){
                    value = this.gui.wizardSettings.activeGroups.classtypesSelected[i];
                    if( value.indexOf("+") == 0 || value.indexOf("-") == 0) {
                        value = value.substr(1);
                    }
                    for( var j = 0; j < checkboxes.length; j++ ){
                        if( checkboxes[j].inputValue == value ){
                            checkboxes[j].setValue(true);
                        }
                    }
                }

                if( this.gui.wizardRecommendedSettings.activeGroups.classtypesSelected.length === 0 ) {
                    this.panel.down( "[name=classtypes_recommended_settings]" ).update( i18n._("None.  Classtypes within selected categories will be used.") );
                } else {
                    var recommendedValues = [];
                    for( i = 0 ; i < this.gui.wizardRecommendedSettings.activeGroups.classtypesSelected.length; i++ ){
                        value = this.gui.wizardRecommendedSettings.activeGroups.classtypesSelected[i];
                        if( value.indexOf("+") == 0 || value.indexOf("-") == 0) {
                            value = value.substr(1);
                        }
                        recommendedValues.push(value);
                    }
                    this.panel.down( "[name=classtypes_recommended_settings]" ).update(recommendedValues.join( ", "));
                }
            }
            this.loaded = true;
        }
        handler();
    },
    getValues: function( handler ){
        this.gui.wizardSettings.activeGroups.classtypes = this.panel.down("radio[name=classtypes]").getGroupValue();
        if( this.gui.wizardSettings.activeGroups.classtypes == "recommended") {
            this.gui.wizardSettings.activeGroups.classtypesSelected = this.gui.wizardRecommendedSettings.activeGroups.classtypesSelected;
        } else {
            this.gui.wizardSettings.activeGroups.classtypesSelected = [];
            Ext.Array.each( this.panel.query("checkbox[name=classtypes_selected][checked=true]"), function( c ) { 
                this.gui.wizardSettings.activeGroups.classtypesSelected.push( "+" + c.inputValue );
            }, this);
        }
        handler();
    }
});

Ext.define('Webui.intrusion-prevention.Wizard.Categories',{
    constructor: function( config ) {
        Ext.apply(this, config);

        var categoriesCheckboxGroup = {
            xtype: 'checkboxgroup',
            fieldLabel: i18n._("Categories"),
            columns: 1,
            items: []
        };

        this.gui.categoriesInfoStore.each( function(record) {
            categoriesCheckboxGroup.items.push({
                boxLabel: record.get( 'name' ),
                name: 'categories_selected',
                inputValue: record.get( 'name' ),
                listeners: {
                    render: function(c){
                        Ext.QuickTips.register({
                            target:  c.boxLabelEl, 
                            text: record.get( 'description' ),
                            dismissDelay: 5000
                        });
                    },
                    destroy: function(c){
                        Ext.QuickTips.unregister(c.boxLabelEl);
                    }
                }
            });
        });

        this.title = i18n._( "Categories" );
        this.panel = Ext.create('Ext.container.Container',{
            scrollable: true,
            items: [{
                xtype: 'container',
                html: '<h2 class="wizard-title">'+i18n._("Categories")+'</h2>',
                margin: '10'
            },{
                xtype: 'container',
                html: i18n._("Categories are a different rule grouping that can span multiple classtypes, such as VOIP access."),
                margin: '0 10 10 10'
            },{
                name: 'categories',
                xtype: 'radio',
                inputValue: 'recommended',
                boxLabel: i18n._('Recommended (default)'),
                hideLabel: true,
                checked: false,
                handler: Ext.bind(function(elem, checked) {
                    this.setVisible( elem.inputValue, checked );
                }, this),
                margin: '0 10'
            },{
                name: 'categories_recommended_settings',
                xtype:'fieldset',
                hidden:true,
                margin: '0 10'
            },{
                name: 'categories',
                xtype: 'radio',
                inputValue: 'custom',
                boxLabel: i18n._('Select by name'),
                hideLabel: true,
                checked: false,
                handler: Ext.bind(function(elem, checked) {
                    this.setVisible( elem.inputValue, checked );
                }, this),
                margin: '0 10'
            },{
                name: 'categories_custom_settings',
                xtype:'fieldset',
                hidden:true,
                items: [
                    categoriesCheckboxGroup
                ],
                margin: '0 10'
            }]
        });

        this.onLoad = Ext.bind( this.onLoad, this );
        this.onNext = Ext.bind( this.getValues, this );
    },
    setVisible: function( id, checked ) {
        if( !checked ) return;
        Ext.Array.each( this.panel.query("fieldset"), function( c ) {
            if( c.name && c.name.indexOf('categories_') == 0 ) {
                c.setVisible( c.name.indexOf( id ) != -1 );
            }
        });
    },
    onLoad: function( handler ) {
        if( this.loaded !== true ){
            if( this.gui.wizardSettings.activeGroups ){
                Ext.Array.each( this.panel.query("radio[name=categories]"), function(c){
                    if( c.inputValue == this.gui.wizardSettings.activeGroups.categories ){
                        c.setValue(true);
                    }
                }, this );

                var i, value;
                var checkboxes = this.panel.query("checkbox[name=categories_selected]");
                for( i = 0; i < this.gui.wizardSettings.activeGroups.categoriesSelected.length; i++ ){
                    value = this.gui.wizardSettings.activeGroups.categoriesSelected[i];
                    if( value.indexOf("+") == 0 || value.indexOf("-") == 0) {
                        value = value.substr(1);
                    }
                    for( var j = 0; j < checkboxes.length; j++ ){
                        if( checkboxes[j].inputValue == value ){
                            checkboxes[j].setValue(true);
                        }
                    }
                }

                if( this.gui.wizardRecommendedSettings.activeGroups.categoriesSelected.length === 0 ) {
                    this.panel.down( "[name=categories_recommended_settings]" ).update( i18n._("None.  Categories within selected classtypes will be used.") );
                } else {
                    var recommendedValues = [];
                    for( i = 0 ; i < this.gui.wizardRecommendedSettings.activeGroups.categoriesSelected.length; i++ ){
                        value = this.gui.wizardRecommendedSettings.activeGroups.categoriesSelected[i];
                        if( value.indexOf("+") == 0 || value.indexOf("-") == 0) {
                            value = value.substr(1);
                        }
                        recommendedValues.push(value);
                    }
                    this.panel.down( "[name=categories_recommended_settings]" ).update(recommendedValues.join( ", "));
                }
            }
            this.loaded = true;
        }
        handler();
    },
    getValues: function( handler ){
        this.gui.wizardSettings.activeGroups.categories = this.panel.down("radio[name=categories]").getGroupValue();
        
        if( this.gui.wizardSettings.activeGroups.categories == "recommended") {
            this.gui.wizardSettings.activeGroups.categoriesSelected = this.gui.wizardRecommendedSettings.categoriesSelected;
        } else {
            this.gui.wizardSettings.activeGroups.categoriesSelected = [];
            Ext.Array.each( this.panel.query("checkbox[name=categories_selected][checked=true]"), function( c ) { 
                this.gui.wizardSettings.activeGroups.categoriesSelected.push( "+" + c.inputValue );
            }, this);
        }
        handler();
    }
});

Ext.define('Webui.intrusion-prevention.Wizard.Congratulations',{
    constructor: function( config ) {
        Ext.apply(this, config);
        this.title = i18n._( "Finish" );
        this.panel = Ext.create('Ext.container.Container',{
            items: [{
                xtype: 'container',
                html: '<h2 class="wizard-title">'+i18n._("Congratulations!")+'</h2>',
                margin: '10'
            }, {
                xtype: 'container',
                html: i18n._('Intrusion Prevention is now configured and enabled.'),
                margin: '10 0 0 10'
            }]
        });
        this.onLoad = Ext.bind(function(handler) {
            this.gui.wizardCompleted = true;
            handler();
        }, this);
        this.onNext = Ext.bind(function(handler) {
            this.gui.wizardWindow.close();
            handler();
        }, this);
    }
});
//# sourceURL=intrusion-prevention-settings.js
