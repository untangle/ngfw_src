// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function Browser(url)
{
   this.tree = new YAHOO.widget.TreeView("tree");

   this.tree.setDynamicLoad(function(node, onCompleteCallback) {
      var url = node.data.url;

      var callback = {
         success: function(o) {
            var dom = o.responseXML.documentElement;
            var dirs = dom.getElementsByTagName("dir");

            for (var i = 0; i < dirs.length; i++) {
               var c = dirs[i];
               var name = c.getAttribute("name");
               new YAHOO.widget.TextNode(new SmbNode(url, name), node, false);
            }

            onCompleteCallback();
         }
      }

      YAHOO.util.Connect.asyncRequest("GET", "ls?url=" + url + "&type=dir",
                                      callback, null);
   });

   var r = this.tree.getRoot();
   new YAHOO.widget.TextNode(new SmbNode(url), r, false);

   this.tree.draw();
}

function SmbNode(path, name)
{
   this.href = "javascript:alert('HAHA')";

   if (name) {
      this.url = path + name;
      this.label = name;
   } else {
      this.url = path;
      this.label = path;
   }

   if (this.label.length - 1 == this.label.lastIndexOf("/")) {
      this.label = this.label.substring(0, this.label.length - 1);
   }
}

// old crap -------------------------------------------------------------------

var openImg = new Image();
openImg.src = "open.gif";
var closedImg = new Image();
closedImg.src = "closed.gif";

var refreshDetails = function() { };

function expandDir(dir)
{
   var dirElem = $(dir);

   if (isFetchable(dirElem)) {
      new Ajax.Request("ls",
                       { method: "get",
                          parameters: "url=" + dir + "&type=dir",
                          onComplete: function(req) {
                             var resp = parseDomFromString(req.responseText);
                             var root = resp.getElementsByTagName('root')[0];
                             if (isFetchable(dirElem)) {
                                addChildDirectories(dirElem, root);
                             }
                          }
                       });
   } else {
      toggleTree(dir);
   }
}

function showFileListing(dir)
{
   new Ajax.Request("ls",
                    { method: "get",
                       parameters: "url=" + dir + "&type=full",
                       onComplete: function(req) {
                          var resp = parseDomFromString(req.responseText);
                          var root = resp.getElementsByTagName('root')[0];
                                   // XXX check if we are the last outstanding request
                          displayDetail(root);
                       }
                    });

   refreshDetails = function() { showFileListing(dir); };
}

function deleteSelection()
{
   var selected = getSelectedFiles();

   var params = "command=delete";
   for (var i = 0; i < selected.length; i++) {
      params += "&file=" + selected[i];
   }

   new Ajax.Request("exec",
                    { method: "post",
                       parameters: params,
                       onComplete: function(req) {
                          refreshDetails();
                       }
                    });
}

function getSelectedFiles()
{
   var selected = document.getElementsByClassName("detail-row-selected", $("detail"));
   var elements = new Array();

   for (var i = 0; i < selected.length; i++) {
      elements.push(selected[i].path);
   }

   return elements;
}

function showFile(filename, name)
{
   // XXX lets make this either inline or save, depending on mime-type
   window.open("get/" + filename);
}

function displayDetail(root)
{
   var detail = $("detail");

   var table = document.createElement("table");
   Element.addClassName(table, "detail-table");

   var thead = table.appendChild(document.createElement("thead"));
   Element.addClassName(thead, "detail-head");
   appendTextElem(thead, "th", "", null);
   appendTextElem(thead, "th", "name", null);
   appendTextElem(thead, "th", "size", null);
   appendTextElem(thead, "th", "last modified", null);
   appendTextElem(thead, "th", "create date", null);

   var tbody = table.appendChild(document.createElement("tbody"));

   var path = root.getAttribute("path");
   var children = root.childNodes;

   var odd = true;
   for (var i = 0; i < children.length; i++) {
      var child = root.childNodes[i];
      var tagName = child.tagName;
      if ("dir" == tagName || "file" == tagName) {
         addDetail(child, "dir" == tagName, path, tbody, odd);
         odd = !odd;
      }
   }

   removeChildren(detail);
   detail.appendChild(table);
}

function addDetail(fileInfo, isDir, path, tbody, odd)
{
   var name = fileInfo.getAttribute("name");

   var row = tbody.appendChild(document.createElement("tr"));
   row.path = path + name;

   Element.addClassName(row, "detail-row");
   Element.addClassName(row, "detail-row-" + (odd ? "odd" : "even"));

   var td = document.createElement("td");
   Element.addClassName(td, "detail-checkbox");
   var checkbox = document.createElement("input")
   checkbox.setAttribute("type", "checkbox");
   td.appendChild(checkbox);
   row.appendChild(td);
   row.onclick = function() {
      checkbox.checked = !checkbox.checked;
      if (checkbox.checked) {
         Element.addClassName(row, "detail-row-selected");
      } else {
         Element.removeClassName(row, "detail-row-selected");
      }
   };

   var nameRow = row.appendChild(document.createElement("td"));
   Element.addClassName(nameRow, "detail-name");
   var nameAnchor = appendTextElem(nameRow, "a", name, null);

   if (isDir) {
      nameAnchor.onclick = function() { showFileListing(row.path); };
   } else {
      nameAnchor.onclick = function() { showFile(row.path, name); };
   }

   appendTextElem(row, "td", fileInfo.getAttribute("size"), "detail-size");
   var date = formatDate(parseInt(fileInfo.getAttribute("mtime")));
   appendTextElem(row, "td", date, "detail-mtime");
   date = formatDate(parseInt(fileInfo.getAttribute("ctime")));
   appendTextElem(row, "td", date, "detail-ctime");
}

function appendTextElem(parent, type, text, clazz)
{
   var td = document.createElement(type);
   if (null != clazz) {
      Element.addClassName(td, clazz);
   }
   td.appendChild(document.createTextNode(text));
   parent.appendChild(td);

   return td;
}

function addChildDirectories(target, dom)
{
   var path = target.getAttribute("id");

   for (var i = 0; i < dom.childNodes.length; i++) {
      if ("dir" == dom.childNodes[i].tagName) {
         var name = dom.childNodes[i].getAttribute("name");
         var childPath = path + name;
         addChildDirectory(target, name, childPath);
      }
   }

   toggleTree(path);
}

function addChildDirectory(target, name, path)
{
   var trig = document.createElement("span");
   Element.addClassName(trig, "trigger");

   var img = document.createElement("img");
   img.setAttribute("src", "closed.gif");
   img.setAttribute("id", "I" + path);
   img.onclick = function() { expandDir(path); };
   trig.appendChild(img);

   var listTrigger = document.createElement("span");
   Element.addClassName(listTrigger, "list-trigger");
   listTrigger.onclick = function() { showFileListing(path); };

   var text = document.createTextNode(name.substring(0, name.length - 1));
   listTrigger.appendChild(text);

   trig.appendChild(listTrigger);

   var br = document.createElement("br");
   trig.appendChild(br);

   var dir = document.createElement("span");
   Element.addClassName(dir, "dir");
   dir.setAttribute("id", path);

   target.appendChild(trig);
   target.appendChild(dir);
}

function toggleTree(dir)
{
   var dirElem = $(dir);
   var dirStyle = dirElem.style;
   var dirImg = $("I" + dir);

   if (dirStyle.display == "block") {
      dirStyle.display = "none";
      dirImg.src = closedImg.src;
   } else {
      dirStyle.display = "block";
      dirImg.src = openImg.src;
   }
}

function isFetchable(target)
{
  // XXX add leaf node checking
   return 0 == target.childNodes.length;
}

function formatDate(unixTime)
{
   date = new Date();
   date.setTime(unixTime);
   var year = date.getYear();
   if (1000 > year) {
      year = year + 1900;
   }

   return zeroPad(date.getMonth(), 2) + "/" + zeroPad(date.getDay(), 2)
      + "/" + year + " " + zeroPad(date.getHours(), 2) + ":"
      + zeroPad(date.getMinutes(), 2);
}

function zeroPad(num, width)
{
   var num = "" + num;
   while (num.length < width) {
      num = "0" + num;
   }

   return num;
}

// function removeChildren(node)
function removeChildren(node)
{
   while (null != node.firstChild) {
      node.removeChild(node.firstChild);
   }
}

function parseDomFromString(text)
{
   return Try.these(function()
                    {
                       return new DOMParser().parseFromString(text, 'text/xml');
                    },
                    function()
                    {
                       var xmlDom = new ActiveXObject("Microsoft.XMLDOM");
                       xmlDom.async = "false";
                       xmlDom.loadXML(text);
                       return xmlDom;
                    });
}

