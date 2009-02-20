// ************************** GLOBAL VARS *********************************//

// A table to cache the outerHTML of the _rtf elements before the rollover
// state is applied.
var gv_preRolloverTextCache = new Object();

// A table to store all the vertical alignments of all the parents of the text
// objects.
var gv_vAlignTable = new Object();


// ************************************************************************//

//Check if IE
var bIE = false;
if ((index = navigator.userAgent.indexOf("MSIE")) >= 0) 
{
	bIE = true;
}

function InsertAfterBegin( dom, html ) {
    if (!bIE) {
        var phtml; var range = dom.ownerDocument.createRange();
        range.selectNodeContents(dom);
        range.collapse(true);
        phtml = range.createContextualFragment( html );
        dom.insertBefore(phtml, dom.firstChild );
    } else {
        dom.insertAdjacentHTML("afterBegin", html);
    }
}
  
function InsertBeforeEnd( dom, html ) {
    if (!bIE) {
        var phtml; var range = dom.ownerDocument.createRange();
        range.selectNodeContents(dom);
        range.collapse(dom);
        phtml = range.createContextualFragment( html );
        dom.appendChild( phtml );
    } else {
        dom.insertAdjacentHTML("beforeEnd", html);
    }
}

var MaxZIndex = 1000;

//Get the id of the Workflow Dialog belonging to element with id = id
function Workflow(id) {
	return id+'WF';
}

//Get the id of the Workflow Description Box belonging to element with id = id
function WorkflowDescBox(id) {
	return id+'WFDesc';
}

//Get the id of the Element Description belonging to element with id = id			
function WorkflowElementDesc(id) 
{
	return id+'d';
}

function BringToFront(id)
{
	var target = document.getElementById(id);
	if (target == null) return;
	MaxZIndex = MaxZIndex + 1;
	target.style.zIndex = MaxZIndex ;
}

function HideElement(id)
{
	var source = document.getElementById(id);
	source.style.visibility = "hidden";
	RefreshScreen();
}

function RefreshScreen()
{
	var oldColor = document.body.style.backgroundColor;
	var setColor = (oldColor=="rgb(0,0,0)")?"#FFFFFF":"#000000";
	document.body.style.backgroundColor = setColor;
	document.body.style.backgroundColor = oldColor;
}

function getAbsoluteLeft(node)
{
   var currentNode=node;
   var left=0;
   while(currentNode.tagName!="BODY"){
      left+=currentNode.offsetLeft;
      currentNode=currentNode.offsetParent;
   }
   return left;
}

function getAbsoluteTop(node)
{
   var currentNode=node;
   var top=0;
   while(currentNode.tagName!="BODY"){
      top+=currentNode.offsetTop;
      currentNode=currentNode.offsetParent;
   }
   return top;
}

function ToggleWorkflow(event, id, width, height, hasWorkflow)
{
	SuppressBubble(event);
	var target = document.getElementById(Workflow(id));
	if (target.style.visibility == "visible") {HideElement(Workflow(id));}
	else 
	{
		var source = document.getElementById(id + "Note");
		BringToFront(target.id);
		var bufferH = 10;
		var bufferV = 10;
		var blnLeft = false;
		var blnAbove = false;
		height = height + 30;
		var sourceLeft;
		var sourceTop;
		if (bIE) 
		{
			sourceTop = window.event.y + document.body.scrollTop;
			sourceLeft = window.event.x + document.body.scrollLeft;
		}
		else
		{
			sourceTop = event.pageY;
			sourceLeft = event.pageX;
		}
		if (sourceLeft > width + bufferH + document.body.scrollLeft) 
		{
			blnLeft = true;
		}
		if (sourceTop > height + bufferV + document.body.scrollTop)
		{
			blnAbove = true;
		}
		DrawAnnotation(target.id, width, height);
		var descBox = document.getElementById(WorkflowDescBox(id));
		if (descBox.innerHTML == '') ShowDescription(id, id + 'base', '');
		if (blnAbove) target.style.top = sourceTop - height;
		else target.style.top = sourceTop;
		if (blnLeft) target.style.left = sourceLeft - width;
		else target.style.left = sourceLeft;
	}
	RefreshScreen();
}

function DrawAnnotation(id, width, height) 
{
	var target = document.getElementById(id);
	target.style.width = width;
	target.style.height = height;
	var btnClose = document.getElementById(id+'Close');
	var crop = document.getElementById(id+'Crop');
	var desc = document.getElementById(id+'Desc');
	var label = document.getElementById(id+'Label');

	var resize = document.getElementById(id+'Resize');
	var heightCell = document.getElementById(id+'Height');
	label.style.left = 10;
	label.style.top = 4;
	label.style.width = width - 30;
	if(bIE)
	{
		btnClose.style.left = width - 18;
		btnClose.style.top = 7;
		//desc.style.left = 4;
		//desc.style.top = 24;
		//desc.style.width = width - 8;
		desc.style.height = height - 31;
		resize.style.left = width - 15;
		resize.style.top = height - 15;
	}
	else
	{
		heightCell.style.height = height - 40;
		btnClose.style.left = width - 18;
		btnClose.style.top = 7;
		//desc.style.left = 4;
		//desc.style.top = 24;
		//desc.style.width = width - 20;
		desc.style.height = height - 31;
		resize.style.left = width - 15;
		resize.style.top = height - 15;
	}
	target.style.visibility = "visible";
}

function ShowDescription(id, WFE, CurrentWFE)
{
	var source = document.getElementById(WorkflowElementDesc(WFE));
	var target = frames[WorkflowDescBox(id)];
	target.document.body.innerHTML = source.innerHTML;
	
	//var element = document.getElementById(WFE);
	//if (element != null) {element.style.border = "thin solid yellow"};
}

function ToggleLinks(event, linksid)
{
	var links = document.getElementById(linksid);
	if (links.style.visibility == "visible") {HideElement(linksid);}
	else {
		if (bIE) 
		{
			links.style.top = window.event.y + document.body.scrollTop;
			links.style.left = window.event.x + document.body.scrollLeft;
		}
		else
		{
			links.style.top = event.pageY;
			links.style.left = event.pageX;
		}
		links.style.visibility = "visible";
		BringToFront(linksid);
	}
	RefreshScreen();
}

var objDrag = new Object();
objDrag.zIndex = 0;

function StartDrag(event, id) 
{
	var x, y;
	objDrag.elNode = document.getElementById(id);
	if (bIE) 
	{
		x = window.event.clientX + document.documentElement.scrollLeft + document.body.scrollLeft;
		y = window.event.clientY + document.documentElement.scrollTop + document.body.scrollTop;
	}
	else
	{
		x = event.pageX;
		y = event.pageY;
	}
	objDrag.cursorStartX = x;
	objDrag.cursorStartY = y;
	objDrag.elStartLeft  = parseInt(objDrag.elNode.style.left, 10);
	objDrag.elStartTop   = parseInt(objDrag.elNode.style.top,  10);
	BringToFront(objDrag.elNode.id);
	if (bIE) 
	{
		document.attachEvent("onmousemove", Drag);
		document.attachEvent("onmouseup",   StopDrag);
	}
	else
	{
		document.addEventListener("mousemove", Drag, true);
		document.addEventListener("mouseup", StopDrag, true);
	}
	SuppressBubble(event);
}

function Drag(event)
{
	var x, y;
	if (bIE) 
	{
		x = window.event.clientX + document.documentElement.scrollLeft + document.body.scrollLeft;
		y = window.event.clientY + document.documentElement.scrollTop + document.body.scrollTop;
	}
	else
	{
		x = event.pageX;
		y = event.pageY;
	}
	objDrag.elNode.style.left = (objDrag.elStartLeft + x - objDrag.cursorStartX) + "px";
	objDrag.elNode.style.top  = (objDrag.elStartTop  + y - objDrag.cursorStartY) + "px";
	SuppressBubble(event);
}

function StopDrag(event) 
{
	objDrag.elNode = null;
	if (bIE)
	{
		document.detachEvent("onmousemove", Drag);
		document.detachEvent("onmouseup",   StopDrag);
	}
	else
	{
		document.removeEventListener("mousemove", Drag,   true);
		document.removeEventListener("mouseup",   StopDrag, true);
    }
}

var objResize = Object();

function StartResize(event, id)
{
	var el;
	var x, y;
	var element = document.getElementById(id);
	if(bIE)
	{
		x = window.event.clientX + document.documentElement.scrollLeft + document.body.scrollLeft;
		y = window.event.clientY + document.documentElement.scrollTop + document.body.scrollTop;
	}
	else
	{
		x = event.pageX;
		y = event.pageY;
	}
	objResize.id = id;
	objResize.cursorStartX = x;
	objResize.cursorStartY = y;
	objResize.startWidth = parseInt(element.style.width);
	objResize.startHeight = parseInt(element.style.height);
	if (bIE)
	{
		document.attachEvent("onmousemove", Resize);
		document.attachEvent("onmouseup",   StopResize);
	}
	else
	{
		document.addEventListener("mousemove", Resize, true);
		document.addEventListener("mouseup",   StopResize, true);
	}
}

function Resize(event)
{
	var x, y;
	if(bIE)
	{
		x = window.event.clientX + document.documentElement.scrollLeft + document.body.scrollLeft;
		y = window.event.clientY + document.documentElement.scrollTop + document.body.scrollTop;
	}
	else
	{
		x = event.pageX;
		y = event.pageY;
	}
	width = objResize.startWidth + x - objResize.cursorStartX;
	if (width < 100) {width = 100};
	height = objResize.startHeight + y - objResize.cursorStartY;
	if (height < 100) {height = 100};
	DrawAnnotation(objResize.id, width, height);
	SuppressBubble(event);
}

function StopResize(event)
{
	objResize.id = null;
	if(bIE)
	{
		document.detachEvent("onmousemove", Resize);
		document.detachEvent("onmouseup",   StopResize);
	}
	else
	{
		document.removeEventListener("mousemove", Resize, true);
		document.removeEventListener("mouseup",   StopResize, true);
	}
}

var Forms = document.getElementsByTagName("FORM");
for (var i = 0; i < Forms.length; i++) 
{
	var Form = Forms(i);
	Form.onclick = SuppressBubble;
}

function SuppressBubble(event)
{
	if (bIE)
	{
		window.event.cancelBubble = true;
		window.event.returnValue = false;
	}
	else
	{
		if (event) {
			event.stopPropagation();
		}
	}
}

function IsTrueMouseOut(idNoSpace, e)
{
    if (!e) var e = window.event;
	var tg = (window.event) ? e.srcElement : e.target;
	if (tg.id != idNoSpace && tg.id != 'o' + idNoSpace) return false;

    while (tg.nodeName != 'HTML') {
        if (tg.style.visibility == 'hidden') return false;
        tg = tg.parentNode;
    }

	var reltg = (e.relatedTarget) ? e.relatedTarget : e.toElement;
	while (reltg != null && reltg.nodeName != 'HTML') {
		var id = reltg.id
		var i = id.indexOf('Links')
		if (i > 0) {
			if (id.substring(0,i) == tg.id) {
				return false;
			}
		}
		reltg = reltg.parentNode;
		if (reltg.id == idNoSpace) return false;
	}
	return true;
}

function IsTrueMouseOver(idNoSpace, e)
{
    if (!e) var e = window.event;
	var tg = (window.event) ? e.srcElement : e.target;
	if (tg.id != idNoSpace && tg.id != 'o' + idNoSpace) return false;
	var reltg = (e.relatedTarget) ? e.relatedTarget : e.fromElement;
	while (reltg != null && reltg.nodeName != 'HTML') {
		var id = reltg.id
		var i = id.indexOf('Links')
		if (i > 0) {
			if (id.substring(0,i) == tg.id) {
				return false;
			}
		}
		reltg= reltg.parentNode
		if (reltg.id == idNoSpace) return false;
	}
	return true;
}

function NewWindow(hyperlink, name, features, center, width, height)
{
	if(center)
	{
		var winl = (screen.width - width) / 2;
		var wint = (screen.height - height) / 2;
		features = features + ', left=' + winl + ', top=' + wint;
	}
	window.open(hyperlink, name, features);
}

var annwindow = "InsertAfterBegin(document.body, \"<!-- For each bubble on the page generate a div as follows --><div id='[[id]]WF' class='annwindow' onmousedown=\\\"StartDrag(event, this.id)\\\"><TABLE WIDTH=100% HEIGHT=100% BORDER=0 CELLPADDING=0 CELLSPACING=0><TR><TD class='annwindowtl'></TD><TD class='annwindowt'></TD><TD class='annwindowtr'></TD></TR><TR><TD ID='[[id]]WFHeight' class='annwindowml'></TD><TD><iframe src='Resources/ann.html' frameborder=0 id='[[id]]WFDesc' name='[[id]]WFDesc' class='annwindowcontent' onmousedown='SuppressBubble(event)' onmousemove='SuppressBubble(event)'></iframe></TD><TD class='annwindowmr'></TD></TR><TR><TD class='annwindowbl'></TD><TD class='annwindowb'></TD><TD class='annwindowbr'></TD></TR></TABLE><!-- Title --><div id='[[id]]WFLabel' class='annwindowtitle'>[[label]]</div><!-- Close button --><div id='[[id]]WFClose' class='annwindowclose' onclick=\\\"HideElement('[[id]]WF')\\\"></div><!-- Resize handle --><div id='[[id]]WFResize' class='annwindowresize' onmousedown=\\\"SuppressBubble(event);StartResize(event, '[[id]]WF')\\\"></div><!-- Div that contains the Workflow Description Box --></div>\");"

function GetDynamicPanelScript(dpId, numberStates) {
    var s = "var currentState" + dpId + " = document.getElementById(\"pd0" + dpId + "\"); function SetPanelVisibility" + dpId + "(visibility) { document.getElementById(\"" + dpId + "\").style.visibility = visibility;	if (visibility == \"hidden\") {";
    for (var i = 0; i < numberStates; i++) {
        s = s + "document.getElementById(\"pd" + i + dpId + "\").style.visibility = visibility; ";
    }
    s = s + "} else { currentState" + dpId + ".style.visibility = visibility; } } ";
    
    s = s + "function SetPanelState" + dpId + "(stateid) {	SetPanelVisibility" + dpId + "(\"hidden\");	document.getElementById(\"" + dpId + "\").style.visibility = \"\";	currentState" + dpId + " = document.getElementById(stateid); currentState" + dpId + ".style.visibility = \"\"; }";
    
    return s;
}

function ParentWindowNeedsReload(newPageName) {
    var reload = false;
    try {
    var oldParentUrl = top.opener.window.location.href.split("#")[0];
    var lastslash = oldParentUrl.lastIndexOf("/");
    if (lastslash > 0) {
        oldParentUrl = oldParentUrl.substring(lastslash + 1, oldParentUrl.length);
        if (oldParentUrl == encodeURI(newPageName)) {
        	reload = true;
        }
    }
    } catch (e) {}
    return reload;
}

// ******************  Sim Functions ****************** //
function SetCheckState(id, value) {
    var boolValue = Boolean(value);
    document.getElementById(id).checked = boolValue;
}

function SetSelectedOption(id, value) {
    document.getElementById(id).value = value;
}

function SetGlobalVariableValue(id, value) {
    if (value.length > 200) {
        value = value.substring(0,200);
    }
    eval(id +' = value');
    try {
        eval('if (top.opener) { top.opener.' + id + ' = value }');
    } catch (e) {}
}

function SetWidgetFormText(id, value) {
    var value = PopulateVariables(value);
    document.getElementById(id).value = value;
}

function SetWidgetRichText(id, value) {
    var value = PopulateVariables(value);
    var rtfElement = document.getElementById(id + '_rtf');
    var oldHeight = rtfElement.offsetHeight;
    rtfElement.innerHTML = value;
    var newHeight = rtfElement.offsetHeight;
    
    var container = document.getElementById(id);
    var oldTop = Number(container.style.top.replace("px", ""));
    var vAlign = gv_vAlignTable[id];
    
    if (vAlign == "center") {
        var newTop = oldTop  -(newHeight - oldHeight)/2;
        container.style.top = newTop + 'px';
    } else if (vAlign == "bottom") {
        var newTop = oldTop - newHeight + oldHeight;
        container.style.top = newTop + 'px';
    } // do nothing if the alignment is top
}

function GetCheckState(id) {
    return document.getElementById(id).checked;
}

function GetSelectedOption(id) {
    return document.getElementById(id).value;
}

function GetGlobalVariableValue(id) {
    return eval (id);
}

function GetGlobalVariableLength(id) {
    return GetGlobalVariableValue(id).length;
}

function GetWidgetFormText(id) {
    return document.getElementById(id).value;
}

function GetWidgetValueLength(id) {
    return document.getElementById(id).value.length;
}

// *****************  Validation Functions ***************** //

function IsValueAlpha(val) {
    return /^[a-z\s]+$/gi.test(val);
}

function IsValueNumeric(val) {
    return /^[0-9,\.\s]+$/gi.test(val);
}

function IsValueAlphaNumeric(val) {
    return /^[0-9a-z\s]+$/gi.test(val);
}

function IsValueOneOf(val, values) {
    for (i = 0; i < values.length; i++) {
        var option = values[i];
        if (val == option) return true;
    }
    // by default, return false
    return false;
}

// ******************  Rollover Functions ****************** //

function SwapOut(id, textid, textRolloverJson, bringToFront) {
document.getElementById('o' + id).style.visibility = 'hidden';
document.getElementById('r' + id).style.visibility = '';
ApplyTextRollover(textid, textRolloverJson);
if (bringToFront) {BringToFront('r' + id); BringToFront(id + 'container'); BringToFront(id); BringToFront(id + 'ann');}
}

function SwapBack(id, textid) {
document.getElementById('r' + id).style.visibility = 'hidden'
document.getElementById('o' + id).style.visibility = ''
RemoveTextRollover(textid);
}

//-------------------------------------------------------------------------
// ApplyTextRollover
//
// Applies a rollover style to a text element.
//       id : the id of the text object to set.
//       styleProperties : an object mapping style properties to values. eg:
//                         { 'fontWeight' : 'bold',
//                           'fontStyle' : 'italic' }
//-------------------------------------------------------------------------
function ApplyTextRollover(id, styleProperties) {
    
    if (gv_preRolloverTextCache[id]) return;
    
    CachePreRolloverText(id);
    
    var rtfElement = document.getElementById(id + '_rtf');
    var oldHeight = rtfElement.offsetHeight;
    
    for (var prop in styleProperties) {
        ApplyTextProperty(rtfElement, prop, styleProperties[prop]);
    }
    
    // now handle vertical alignment
    var newHeight = rtfElement.offsetHeight;
    var container = document.getElementById(id);
    var oldTop = Number(container.style.top.replace("px", ""));
    var vAlign = gv_vAlignTable[id];
    
    if (vAlign == "center") {
        var newTop = oldTop  -(newHeight - oldHeight)/2;
        container.style.top = newTop + 'px';
    } else if (vAlign == "bottom") {
        var newTop = oldTop - newHeight + oldHeight;
        container.style.top = newTop + 'px';
    } // do nothing if the alignment is top      
    

    //--------------------------------------------------------------------------
    // ApplyStyleRecursive
    //
    // Applies a style recursively to all span and div tags including elementNode
    // and all of its children.
    //
    //     element : the element to apply the style to
    //     styleName : the name of the style property to set (eg. 'font-weight')     
    //     styleValue : the value of the style to set (eg. 'bold')
    //--------------------------------------------------------------------------
    function ApplyStyleRecursive(element, styleName, styleValue) {
        var nodeName = element.nodeName.toLowerCase();
        
        if (nodeName == 'div' || nodeName == 'span') {
            element.style[styleName] = styleValue;
        }
        
        for (var i = 0; i < element.childNodes.length; i++) {
            ApplyStyleRecursive(element.childNodes[i], styleName, styleValue);
        }
    }

    //---------------------------------------------------------------------------
    // ApplyTextProperty
    //
    // Applies a text property to rtfElement.
    //
    //     rtfElement : the the root text element of the rtf object (this is the
    //                  element named <id>_rtf
    //     prop : the style property to set.
    //     value : the style value to set.
    //---------------------------------------------------------------------------
    function ApplyTextProperty(rtfElement, prop, value) {
        var oldHtml = rtfElement.innerHTML;
        if (prop == 'fontWeight') {
            rtfElement.innerHTML = oldHtml.replace(/< *b *\/?>/gi, "");
        } else if (prop == 'fontStyle') {
            rtfElement.innerHTML = oldHtml.replace(/< *i *\/?>/gi, "");
        } else if (prop == 'textDecoration') {
            rtfElement.innerHTML = oldHtml.replace(/< *u *\/?>/gi, "");
        } 
        
        for (var i = 0; i < rtfElement.childNodes.length; i++) {        
        	ApplyStyleRecursive(rtfElement.childNodes[i], prop, value);
	    }
    }    


    //---------------------------------------------------------------------------
    // GetAndCachePreRolloverText
    //
    // Gets the html for the pre-rollover state and returns the Html representing
    // the Rich text.
    //---------------------------------------------------------------------------
    function CachePreRolloverText(id) {
        var rtfElement = document.getElementById(id + '_rtf');
        var container = document.getElementById(id);
        
        var cacheObject = new Object();
        cacheObject.innerHTML = rtfElement.innerHTML;
        cacheObject.top = container.style.top;
        
        gv_preRolloverTextCache[id] = cacheObject;
    }
}


//-------------------------------------------------------------------------
// RemoveTextRollover
//
// Removes the text rollover on an id.
//
//       id : the id of the text element.
//-------------------------------------------------------------------------
function RemoveTextRollover(id) {
    var cacheObject = gv_preRolloverTextCache[id];
    if (cacheObject) {
        var rtfElement = document.getElementById(id + '_rtf');
        var container = document.getElementById(id);
 
        rtfElement.innerHTML = cacheObject.innerHTML;       
        container.style.top = cacheObject.top;
        
        gv_preRolloverTextCache[id] = null;
    }   
}