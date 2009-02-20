/*
 * Ext JS Library 2.2
 * Copyright(c) 2006-2008, Ext JS, LLC.
 * licensing@extjs.com
 * 
 * http://extjs.com/license
 */


//
// Extend the XmlTreeLoader to set some custom TreeNode attributes specific to our application:
//
Ext.app.BookLoader = Ext.extend(Ext.ux.XmlTreeLoader, {
    processAttributes : function(attr){
        if(attr.first){ // is it an author node?
            
            // Set the node text that will show in the tree since our raw data does not include a text attribute:
            attr.text = attr.first + ' ' + attr.last;
            
            // Author icon, using the gender flag to choose a specific icon:
            attr.iconCls = attr.type;//'author-' + attr.gender;
            
            // Override these values for our folder nodes because we are loading all data at once.  If we were
            // loading each node asynchronously (the default) we would not want to do this:
            attr.loaded = true;
            //attr.expanded = true;
        }
        else if(attr.title){ // is it a book node?
            
            // Set the node text that will show in the tree since our raw data does not include a text attribute:
            attr.text = attr.title ;//+ ' (' + attr.published + ')';
            
            // Book icon:
           // attr.iconCls = 'book';
			attr.iconCls = attr.icon;
        
            // Tell the tree this is a leaf node.  This could also be passed as an attribute in the original XML,
            // but this example demonstrates that you can control this even when you cannot dictate the format of 
            // the incoming source XML:
            attr.leaf = true;
        }
    }
});

Ext.onReady(function(){
	Ext.util.CSS.swapStyleSheet('theme', "../../resources/css/xtheme-gray.css");	
    var detailsText = '<i>Select a report to see more information...</i>';
    
	var tpl = new Ext.Template(
        '<h2 class="title">{title}</h2>',
        '<p><b>Details of the report display as Tabs here - the tabs would be </b></p>',
		'<ul><li>Summary</li><li>Email Reports</li><li>Incidents</li><li>etc.,</li></ul>'
		
	);
    tpl.compile();
  
	var tpldashboard = new Ext.Template(
      '<p>{graph}</p>'		
	);
	
	var tplSummary = new Ext.Template(
		'<table class="stats">',
		'<tr><td class="graph">{graph}</td><td><div style="float:left;width:100%;"><div class="export-excel"></div><div class="export-printer"></div></div>',
		'<div class="statsheader">Key Statistics</div>{stats}</td></tr>',
		'</table>'
	);
    tpldashboard.compile();
	new Ext.SplitButton({
		renderTo: '_date', // the container id
		text: 'Sep 16, 2008 - Sep 22, 2008',
		//handler: optionsHandler, // handle a click on the button itself
		menu: new Ext.menu.Menu({
			items: [
				// these items will render as dropdown menu items when the arrow is clicked:
				{text: 'Sep 8, 2008 - Sep 15, 2008'},
				{text: 'Aug 30, 2008 - Sep 7, 2008'}
			]
		})
	});
	
    new Ext.Panel({
	    renderTo: 'tree',
        layout: 'border',
	    width: 960,
        height: 700,
        items: [{
            xtype: 'treepanel',
            id: 'tree-panel',
            region: 'center',
            margins: '2 2 0 2',
            autoScroll: true,
	        rootVisible: false,
			title:'Reports',
	        root: new Ext.tree.AsyncTreeNode(),
            
            // Our custom TreeLoader:
	        loader: new Ext.app.BookLoader({
	            dataUrl:'xml-tree-data.xml'
	        }),
            
	        listeners: {
				'load':function(node){
						Ext.getCmp('tree-panel').getSelectionModel().select(Ext.getCmp('tree-panel').getRootNode().firstChild);

				},
	            'render': function(tp){
                    tp.getSelectionModel().on('selectionchange', function(tree, node){
						var tab = Ext.getCmp('report-details-tab');
						var ars = [];
                        var el = Ext.get('summary');//getCmp('details-panel').body;
						var section = node.attributes.title;
                        var show = true;
							switch(section){
								case 'Dashboard':
									hideTabStrips(tab,[1,2,3,4,5,6,7]);		
									showTabStrips(tab,[0]);
									ars = ['summary'];								
								break;
								case 'Spam Blocker':
									hideTabStrips(tab,[3,4,5,6,7]);		
									showTabStrips(tab,[0,1,2]);
									ars = ['summary','emailreports'];
								break;
								case 'Platform':
									hideTabStrips(tab,[1,2,6,7]);		
									showTabStrips(tab,[3,4,5]);
									ars = ['summary','traffic'];
								break;
                                case 'Users':
                                    show = false;
                                break;
								default:
									if(node.leaf){
										tpl.overwrite(el, node.attributes);									
									}else{
										el.update(detailsText);
									}
								break;
							}
                            Ext.getCmp('report-details-tab').setVisible(show);                                    
                            Ext.getCmp('bc-component').setVisible(!show);
                            
							for(var j=0;j<ars.length;j++){
								el = Ext.get(ars[j]);									
								el.update('');
								if(MetaData[section]){
									for(var i=MetaData[section][ars[j]].length-1;i>=0;i--){
										if(MetaData[section][ars[j]]){
											if(section=='Dashboard'){
												tpldashboard.insertFirst(el,MetaData[section][ars[j]][i]);																					
											}else{
												tplSummary.insertFirst(el,MetaData[section][ars[j]][i]);										
											}
										}
									}	
								}
								tab.setActiveTab('_summary');										
							}							
                    })
	            }
	        }
        },{
            region: 'east',
            title: 'Report Details',
            id: 'details-panel',
            autoScroll: true,
            collapsible: false,
            split: true,
            margins: '0 2 2 2',
            cmargins: '2 2 2 2',
			width:750,
            items:[
                {
                    xtype:'fieldset',
                    id:'bc-component',
                    html:'<pre>xxxx</pre>',
                    hidden:true
                },
				new Ext.TabPanel({
					id:'report-details-tab',
					ctCls:'reports-tab-panel',
					activeTab:0,
					height:640,
					width:725,
					items:[
						{title:'Summary',contentEl:'summary',id:'_summary'},
						{title:'Email Reports',contentEl:'emailreports',id:'_emailreports'},
						{title:'Incident Report',contentEl:'incidents',id:'_incidents'},
						{title:'Traffic',contentEl:'traffic',id:'_traffic'},
						{title:'Servers',contentEl:'servers',id:'_servers'},						
						{title:'Administration',contentEl:'administration',id:'_administration'},			
						{title:'Users',contentEl:'users',id:'_users'},				
						{title:'Hosts',contentEl:'hosts',id:'_hosts'}				
                        
						
					],
					listeners:{
						'tabchange':function(tp,tab){
							Ext.get(tab.initialConfig.contentEl).setStyle('display','');
							var selNode = Ext.getCmp('tree-panel').getSelectionModel().getSelectedNode();
							if(selNode){
							if(MetaData[selNode.attributes.title]){
							if(MetaData[selNode.attributes.title][tab.title]){
								var x = MetaData[selNode.attributes.title][tab.title];
								switch(x.nature){
									case 'grid':
										if(tab.items) tab.items.clear();
										x.grid.store = new Ext.data.JsonStore(x.store);
										x.grid.bbar = new Ext.PagingToolbar({store:x.grid.store,pageSize: 100})
										//x.grid.bbar = new Ext.PagingToolbar(x.grid.store),
										tab.add(new Ext.grid.GridPanel(x.grid));
										tab.doLayout();
									break;
								}
								
							}
							}
							}
						}
					
					}
				})
			],
			cls:'xxx'
        }]
    });

});
function hideTabStrips(obj,ar){
	for(var i=0;i<ar.length;i++){
		obj.hideTabStripItem(ar[i]);
	}
}
function showTabStrips(obj,ar){
	for(var i=0;i<ar.length;i++){
		obj.unhideTabStripItem(ar[i]);
	}
}
var MetaData = {
	Dashboard:{
		summary:[
		{
			graph:'<img src="../reports-v2/Dashboard - Summary_files/u0.png">',
			stats:null
		}
		]
	},
	Platform:{
		summary:[
		{
			graph:'<img  src="../reports-v2/Platform - Summary_files/u0.png">',
			stats:'<table class="keystats">'+
			'<tr><td class="label">Average Data Rate</td><td class="value">64 Kb/sec</td></tr>'+
			'<tr class="alternate"><td>Peak Data Rate</td><td>205 Kb/sec</td></tr>'+
			'<tr><td>7-Day Average Data Rate</td><td>2.5 Gb/day</td></tr>'+
			'<tr class="alternate"><td>7-Day Data Transfered</td><td>7.8 Gb</td></tr>'+		
			'</table>'
		},
		{
			graph:'<img src="../reports-v2/Platform - Summary_files/u1.png">',
			stats:'<table class="keystats">'+
			'<tr><td class="label">1-Day Average CPU</td><td class="value">0.78</td></tr>'+
			'<tr class="alternate"><td>1-Day Max CPU Load</td><td>5.90</td></tr>'+
			'<tr><td>1-Day Min CPU Load</td><td>0.001</td></tr>'+
			'</table>'
		},
		{
			graph:'<img src="../reports-v2/Platform - Summary_files/u4.png">',
			stats:'<table class="keystats">'+
			'<tr><td class="label">7-Day Average Swap Usage</td><td class="value">5.3%</td></tr>'+
			'<tr class="alternate"><td>7-Day Max Swap Used</td><td>5.4%</td></tr>'+
			'</table>'
		}		
		],
		traffic:[
		{
			graph:'<img src="../reports-v2/Platform-Traffic_files/u0.png">',
			stats:'<table class="keystats">'+
			'<tr><td class="label">Average Data Rate</td><td class="value">64 Kb/sec</td></tr>'+
			'<tr class="alternate"><td>Peak Data Rate</td><td>205 Kb/sec</td></tr>'+
			'<tr><td>7-Day Average Data Rate</td><td>2.5 Gb/day</td></tr>'+
			'<tr class="alternate"><td>7-Day Data Transfered</td><td>7.8 Gb</td></tr>'+		
			'</table>'
		},
		{
			graph:'<img src="../reports-v2/Platform-Traffic_files/u1.png">',
			stats:'<table class="keystats">'+
			'<tr><td class="label">1-Day Average CPU</td><td class="value">0.78</td></tr>'+
			'<tr class="alternate"><td>1-Day Max CPU Load</td><td>5.90</td></tr>'+
			'<tr><td>1-Day Min CPU Load</td><td>0.001</td></tr>'+
			'</table>'
		},
		{
			graph:'<img src="../reports-v2/Platform-Traffic_files/u9.png">',
			stats:'<table class="keystats">'+
			'<tr><td class="label">7-Day Average Swap Usage</td><td class="value">5.3%</td></tr>'+
			'<tr class="alternate"><td>7-Day Max Swap Used</td><td>5.4%</td></tr>'+
			'</table>'
		}		
		]		
	},
	'Spam Blocker':{
		summary:[
			{
				graph:'<img src="../reports-v2/Platform - Summary_files/u0.png">',
				stats:'<table class="keystats">'+
				'<tr><td class="label">Average Data Rate</td><td class="value">64 Kb/sec</td></tr>'+
				'<tr class="alternate"><td>Peak Data Rate</td><td>205 Kb/sec</td></tr>'+
				'<tr><td>7-Day Average Data Rate</td><td>2.5 Gb/day</td></tr>'+
				'<tr class="alternate"><td>7-Day Data Transfered</td><td>7.8 Gb</td></tr>'+		
				'</table>'
			}
		],
		'Email Reports':[
		
		],
		'Incident Report':{
			nature:'grid',
			store:{
		        // load using HTTP
		        url: 'http://kram-store/misc/ext/examples/tree/attachment.js',
				root:'rows',
				fields:['s2p_chunks','content_length','c_client_addr','c_client_port','s_server_addr','s_server_port','reason','time_stamp'],
				autoLoad :true
			},
			grid:{
				bbar:null,
		       // bbar: null, //new Ext.PagingToolbar(store),//will be replaced
		        store: null, //will be replaced
		        columns: [
		            {header: "Client Address",  width:120, dataIndex: 'c_client_addr', sortable: true},
		            {header: "Client Port",  width:70,dataIndex: 'c_client_port', sortable: true},
		            {header: "Server Address",  width:120,dataIndex: 's_server_addr', sortable: true},
		            {header: "Server Port",  width:75,dataIndex: 's_server_port', sortable: true},
		            {header: "Content Length", width:70,dataIndex: 'content_length', sortable: true},
		            {header: "Reason",  width:70,dataIndex: 'reason', sortable: true},
		            {header: "Time Stamp",  dataIndex: 'time_stamp', sortable: true}

		        ],
				tbar:[{
			            text:'Filter',
			            tooltip:'Filter Rows By Some Criteria',
						listener:searchGrid
			        },
					{
			            text:'Clear',
			            tooltip:'Clear Filter',
						disabled:true
			        }],			
				viewConfig: {
				        forceFit: true
				    },
				sm: new Ext.grid.RowSelectionModel({singleSelect:true}),
				width:625,
				height:580,
				border:true,
				iconCls:'icon-grid'				
		    }
		},
        'Hosts':{
			nature:'grid',
			store:{
		        // load using HTTP
		        url: 'http://kram-store/misc/ext/examples/tree/attachment.js',
				root:'rows',
				fields:['s2p_chunks','content_length','c_client_addr','c_client_port','s_server_addr','s_server_port','reason','time_stamp'],
				autoLoad :true
			},
			grid:{
				bbar:null,
		       // bbar: null, //new Ext.PagingToolbar(store),//will be replaced
		        store: null, //will be replaced
		        columns: [
		            {header: "Client Address",  width:120, dataIndex: 'c_client_addr', sortable: true},
		            {header: "Client Port",  width:70,dataIndex: 'c_client_port', sortable: true},
		            {header: "Server Address",  width:120,dataIndex: 's_server_addr', sortable: true},
		            {header: "Server Port",  width:75,dataIndex: 's_server_port', sortable: true},
		            {header: "Content Length", width:70,dataIndex: 'content_length', sortable: true},
		            {header: "Reason",  width:70,dataIndex: 'reason', sortable: true},
		            {header: "Time Stamp",  dataIndex: 'time_stamp', sortable: true}

		        ],
				tbar:[{
			            text:'Filter',
			            tooltip:'Filter Rows By Some Criteria',
						listener:searchGrid
			        },
					{
			            text:'Clear',
			            tooltip:'Clear Filter',
						disabled:true
			        }],			
				viewConfig: {
				        forceFit: true
				    },
				sm: new Ext.grid.RowSelectionModel({singleSelect:true}),
				width:625,
				height:580,
				border:true,
				iconCls:'icon-grid'				
		    }
		}            
	}

};
function searchGrid(){

}