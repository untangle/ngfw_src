// Copyright (c) Untangle, Inc.
// All rights reserved.
var _popup,popupopened = false;
function acceptAgreement(){
    var agree = document.getElementById('agree'),agreeValue; 
    if(agree){
        agreeValue = agree.type == 'hidden' ? true : agree.checked;
        if(agreeValue === true){
            try{
                showPleaseWait();
                authenticateUser("agree-error");

            }catch(exn){            
                hidePleaseWait();
                showError("An error occured. Please try again.");
            }
            return;
        }else{
            showError("In order to continue, you must check the box below.");
            document.getElementById("agree-error").style.display = 'block';
        }
    }
}
function submitOnEnter(e){
    try{
        e = e || event;
        if(e.keyCode==13){
            authenticateUserWrapper('login-error');   
        }    
    }catch(exn){    
    }
}
function hideLogoutSuccess(){
    var ls = document.getElementById('logout-success');
    if(ls){
        ls.style.display = 'none';
    }
}
function showError(errorMessage){
    hideLogoutSuccess();
    document.getElementById('agree-error').style.display = 'block';
    document.getElementById('agree-error').innerHTML = errorMessage;
}
function showPleaseWait(){
    hideLogoutSuccess();
    document.getElementById('authenticateUser').style.display = 'none';
    document.getElementById('please-wait').style.display = '';
}
function hidePleaseWait(){
    document.getElementById('authenticateUser').style.display = '';
    document.getElementById('please-wait').style.display = 'none';
}
function getHttpReqObj(){
    var req = false;
    if (window.XMLHttpRequest) {
        req = new XMLHttpRequest();
        if (req.overrideMimeType) {
            req.overrideMimeType("text/xml");
        }
    } else if (window.ActiveXObject) {
        try {
            req = new ActiveXObject("Msxml2.XMLHTTP");
        } catch (exn) {
            try {
                req = new ActiveXObject("Microsoft.XMLHTTP");
            } catch (exn) {

            }
        }
    } 
    return req;   
}
function authenticateUserWrapper(errorfield){
    try{
        hideLogoutSuccess();    
        showPleaseWait();
        window.setTimeout(function(){authenticateUser(errorfield);},2000);
    
    }catch(exn){            
        hidePleaseWait();
        showError("An error occured. Please try again.")
    }
}
function authenticateUser( errorField )
{

    var e = document.getElementById("authenticateUser");
    if (e) {
        e.disabled = true;
    }
    document.getElementById(errorField).style.display = 'none';

    var req = getHttpReqObj();

    req.onreadystatechange = function()
    {
        if (req.readyState == 4) {
            var v = JSON.parse( req.responseText );
            if ( v["authenticate"] == true ) {
                redirectUser();                       
                return;
            }

            var e = document.getElementById("authenticateUser");

            if (e) {
                e.disabled = false;
            }
            hidePleaseWait();
            document.getElementById(errorField).style.display = 'block';
            
        }
    };

    var password, username;
    username = document.getElementById( "username" );
    password = document.getElementById( "password" );

    if ( username != null ) {
        username = username.value;
    }
    if ( username == null ) {
        username = "captive portal user";
    }

    if ( password != null ) {
        password = password.value;
    }
    if ( password == null ) {
        password = "empty";
    }
    
    req.open("POST", "/users/authenticate", true);
    if(req.overrideMimeType){
        req.overrideMimeType("application/json");
    }
    req.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
    req.send("username=" + escape( username ) + "&password=" + escape( password ));
};
function redirectUser()
{
    var t = redirectUrl;
    if ( t == null ) {
        t = "http://guide.untangle.com/captive-portal";
    }
    
    window.location.href = t;

}
var BrowserDetect = {
	init: function () {
		this.browser = this.searchString(this.dataBrowser) || "An unknown browser";
		this.version = this.searchVersion(navigator.userAgent)
			|| this.searchVersion(navigator.appVersion)
			|| "an unknown version";
		this.OS = this.searchString(this.dataOS) || "an unknown OS";
	},
	searchString: function (data) {
		for (var i=0;i<data.length;i++)	{
			var dataString = data[i].string;
			var dataProp = data[i].prop;
			this.versionSearchString = data[i].versionSearch || data[i].identity;
			if (dataString) {
				if (dataString.indexOf(data[i].subString) != -1)
					return data[i].identity;
			}
			else if (dataProp)
				return data[i].identity;
		}
        return null;
	},
	searchVersion: function (dataString) {
		var index = dataString.indexOf(this.versionSearchString);
		if (index == -1) return null;
		return parseFloat(dataString.substring(index+this.versionSearchString.length+1));
	},
	dataBrowser: [
		{
			string: navigator.userAgent,
			subString: "Chrome",
			identity: "Chrome"
		},
		{ 	string: navigator.userAgent,
			subString: "OmniWeb",
			versionSearch: "OmniWeb/",
			identity: "OmniWeb"
		},
		{
			string: navigator.vendor,
			subString: "Apple",
			identity: "Safari",
			versionSearch: "Version"
		},
		{
			prop: window.opera,
			identity: "Opera"
		},
		{
			string: navigator.vendor,
			subString: "iCab",
			identity: "iCab"
		},
		{
			string: navigator.vendor,
			subString: "KDE",
			identity: "Konqueror"
		},
		{
			string: navigator.userAgent,
			subString: "Firefox",
			identity: "Firefox"
		},
		{
			string: navigator.vendor,
			subString: "Camino",
			identity: "Camino"
		},
		{		// for newer Netscapes (6+)
			string: navigator.userAgent,
			subString: "Netscape",
			identity: "Netscape"
		},
		{
			string: navigator.userAgent,
			subString: "MSIE",
			identity: "Explorer",
			versionSearch: "MSIE"
		},
		{
			string: navigator.userAgent,
			subString: "Gecko",
			identity: "Mozilla",
			versionSearch: "rv"
		},
		{ 		// for older Netscapes (4-)
			string: navigator.userAgent,
			subString: "Mozilla",
			identity: "Netscape",
			versionSearch: "Mozilla"
		}
	],
	dataOS : [
		{
			string: navigator.platform,
			subString: "Win",
			identity: "Windows"
		},
		{
			string: navigator.platform,
			subString: "Mac",
			identity: "Mac"
		},
		{
			   string: navigator.userAgent,
			   subString: "iPhone",
			   identity: "iPhone/iPod"
	    },
		{
			string: navigator.platform,
			subString: "Linux",
			identity: "Linux"
		}
	]

};
BrowserDetect.init();
var TINY={};

function T$(i){return document.getElementById(i);}
TINY.setStyle = function(o){
    if(o.id=='tinybox'){
        o.style.position = 'absolute';
        o.style.display = 'none';
        o.style.padding = '10px';
        o.style.background = '#FFF';
        o.style.border = '10px solid #339900';
        o.style.zIndex = 2000;
    }else if(o.id =='tinymask'){
        o.style.position = 'absolute';
        o.style.display = 'none';
        o.style.top = 0;
        o.style.left = 0;
        o.style.height = '100%';
        o.style.width = '100%';
        o.style.border = '1px solid #555';
        o.style.background = '#000';
        o.style.zIndex = 1500;
    }else if (o.is == 'tinycontent'){
        o.style.background = '#FFF';
    }else{
        Unt.db('id doesn not match');
    }
}
/**
 *  TINY - Standalone Popup by Michael Leigeber
 *  Licensed under Creative Commons 3.0 License http://creativecommons.org/licenses/by/3.0/us/ 
 *  Free for personal and commercial use    
 **/ 
TINY.box=function(){
	var p,m,b,fn,ic,iu,iw,ih,ia,f=0;
	return{
		show:function(c,u,w,h,a,t){
			if(!f){
				p=document.createElement('div'); p.id='tinybox';
				TINY.setStyle(p);
				m=document.createElement('div'); m.id='tinymask';
				TINY.setStyle(m);
				b=document.createElement('div'); b.id='tinycontent';
				TINY.setStyle(b);
				document.body.appendChild(m); document.body.appendChild(p); p.appendChild(b);
				m.onclick=TINY.box.hide; window.onresize=TINY.box.resize; f=1;
			}
			if(!a&&!u){
				p.style.width=w?w+'px':'auto'; p.style.height=h?h+'px':'auto';
				p.style.backgroundImage='none'; b.innerHTML=c;
			}else{
				b.style.display='none'; p.style.width=p.style.height='100px';
			}
			this.mask();
			ic=c; iu=u; iw=w; ih=h; ia=a; this.alpha(m,1,80,3);
			if(t){setTimeout(function(){TINY.box.hide();},1000*t);}
		},
		fill:function(c,u,w,h,a){
			if(u){
				p.style.backgroundImage='';
				var x=window.XMLHttpRequest?new XMLHttpRequest():new ActiveXObject('Microsoft.XMLHTTP');
				x.onreadystatechange=function(){
					if(x.readyState==4&&x.status==200){TINY.box.psh(x.responseText,w,h,a);}
				};
				x.open('GET',c,1); x.send(null);
			}else{
				this.psh(c,w,h,a);
			}
		},
		psh:function(c,w,h,a){
			if(a){
				if(!w||!h){
					var x=p.style.width, y=p.style.height; b.innerHTML=c;
					p.style.width=w?w+'px':''; p.style.height=h?h+'px':'';
					b.style.display='';
					w=parseInt(b.offsetWidth); h=parseInt(b.offsetHeight);
					b.style.display='none'; p.style.width=x; p.style.height=y;
				}else{
					b.innerHTML=c;
				}
				this.size(p,w,h);
			}else{
				p.style.backgroundImage='none';
			}
		},
		destroy : function(){
		    TINY.box.hide();
		    Unt.cleanup();
        },
        cleanup : function(){
		    try{
                document.body.removeChild(p);
                document.body.removeChild(m);
            }catch(e){
            
            }			        
        },
		hide:function(){
			TINY.box.alpha(p,-1,0,3);
			Unt.cleanup();
		},
		resize:function(){
			TINY.box.pos(); TINY.box.mask();
		},
		mask:function(){
			m.style.height=TINY.page.total(1)+'px';
			m.style.width=''; m.style.width=TINY.page.total(0)+'px';
		},
		pos:function(){
			var t=(TINY.page.height()/2)-(p.offsetHeight/2); t=t<10?10:t;
			p.style.top=(t+TINY.page.top())+'px';
			p.style.left=(TINY.page.width()/2)-(p.offsetWidth/2)+'px';
		},
		alpha:function(e,d,a){
			clearInterval(e.ai);
			if(d==1){
				e.style.opacity=0; e.style.filter='alpha(opacity=0)';
				e.style.display='block'; this.pos();
			}
			e.ai=setInterval(function(){TINY.box.ta(e,a,d);},20);
		},
		ta:function(e,a,d){
			var o=Math.round(e.style.opacity*100);
			if(o==a){
				clearInterval(e.ai);
				if(d==-1){
					e.style.display='none';
					e==p?TINY.box.alpha(m,-1,0,2):b.innerHTML=p.style.backgroundImage='';
				}else{
					e==m?this.alpha(p,1,100):TINY.box.fill(ic,iu,iw,ih,ia);
				}
			}else{
				var n=Math.ceil((o+((a-o)*.5))); n=n==1?0:n;
				e.style.opacity=n/100; e.style.filter='alpha(opacity='+n+')';
			}
		},
		size:function(e,w,h){
			e=typeof e=='object'?e:T$(e); clearInterval(e.si);
			var ow=e.offsetWidth, oh=e.offsetHeight,
			wo=ow-parseInt(e.style.width), ho=oh-parseInt(e.style.height);
			var wd=ow-wo>w?0:1, hd=(oh-ho>h)?0:1;
			e.si=setInterval(function(){TINY.box.ts(e,w,wo,wd,h,ho,hd);},20);
		},
		ts:function(e,w,wo,wd,h,ho,hd){
			var ow=e.offsetWidth-wo, oh=e.offsetHeight-ho;
			if(ow==w&&oh==h){
				clearInterval(e.si); p.style.backgroundImage='none'; b.style.display='block';
			}else{
				if(ow!=w){var n=ow+((w-ow)*.5); e.style.width=wd?Math.ceil(n)+'px':Math.floor(n)+'px';}
				if(oh!=h){var n=oh+((h-oh)*.5); e.style.height=hd?Math.ceil(n)+'px':Math.floor(n)+'px';}
				this.pos();
			}
		}
	};
}();

TINY.page=function(){
	return{
		top:function(){return document.documentElement.scrollTop||document.body.scrollTop;},
		width:function(){return self.innerWidth||document.documentElement.clientWidth||document.body.clientWidth;},
		height:function(){return self.innerHeight||document.documentElement.clientHeight||document.body.clientHeight;},
		total:function(d){
			var b=document.body, e=document.documentElement;
			return d?Math.max(Math.max(b.scrollHeight,e.scrollHeight),Math.max(b.clientHeight,e.clientHeight)):
			Math.max(Math.max(b.scrollWidth,e.scrollWidth),Math.max(b.clientWidth,e.clientWidth));
		}
	};
}();

