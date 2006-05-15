<!--
/*
* ***** BEGIN LICENSE BLOCK *****
* Version: MPL 1.1
*
* The contents of this file are subject to the Mozilla Public
* License Version 1.1 ("License"); you may not use this file except in
* compliance with the License. You may obtain a copy of the License at
* http://www.zimbra.com/license
*
* Software distributed under the License is distributed on an "AS IS"
* basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
* the License for the specific language governing rights and limitations
* under the License.
*
* The Original Code is: Zimbra AJAX Toolkit.
*
* The Initial Developer of the Original Code is Zimbra, Inc.
* Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
* All Rights Reserved.
*
* Contributor(s):
*
* ***** END LICENSE BLOCK *****
*/
-->

<!-- BEGIN SCRIPT BLOCK -->
<!-- WARNING: Order matters.  Don't re-order these unless you know what you're doing! -->

<!-- AJAX utility classes -->
<script type="text/javascript" src="/AjaxTk/js/core/AjxCore.js"></script>
<script type="text/javascript" src="/AjaxTk/js/core/AjxEnv.js"></script>
<script type="text/javascript" src="/AjaxTk/js/util/AjxUtil.js"></script>
<script type="text/javascript" src="/AjaxTk/js/util/AjxText.js"></script>
<script type="text/javascript" src="/AjaxTk/js/core/AjxException.js"></script>
<script type="text/javascript" src="/AjaxTk/js/util/AjxCookie.js"></script>
<script type="text/javascript" src="/AjaxTk/js/soap/AjxSoapException.js"></script>
<script type="text/javascript" src="/AjaxTk/js/soap/AjxSoapFault.js"></script>
<script type="text/javascript" src="/AjaxTk/js/soap/AjxSoapDoc.js"></script>
<script type="text/javascript" src="/AjaxTk/js/net/AjxRpcRequest.js"></script>
<script type="text/javascript" src="/AjaxTk/js/net/AjxRpc.js"></script>
<script type="text/javascript" src="/AjaxTk/js/util/AjxWindowOpener.js"></script>
<script type="text/javascript" src="/AjaxTk/js/util/AjxVector.js"></script>
<script type="text/javascript" src="/AjaxTk/js/util/AjxStringUtil.js"></script>
<script type="text/javascript" src="/AjaxTk/js/debug/AjxDebug.js"></script>
<script type="text/javascript" src="/AjaxTk/js/debug/AjxDebugXmlDocument.js"></script>
<script type="text/javascript" src="/AjaxTk/js/xml/AjxXmlDoc.js"></script>
<script type="text/javascript" src="/AjaxTk/js/config/data/AjxConfig.js"></script>
<script type="text/javascript" src="/AjaxTk/js/core/AjxEnv.js"></script>
<script type="text/javascript" src="/AjaxTk/js/core/AjxImg.js"></script>
<script type="text/javascript" src="/AjaxTk/js/core/AjxException.js"></script>
<script type="text/javascript" src="/AjaxTk/js/util/AjxCallback.js"></script>
<script type="text/javascript" src="/AjaxTk/js/util/AjxTimedAction.js"></script>
<script type="text/javascript" src="/AjaxTk/js/events/AjxEvent.js"></script>
<script type="text/javascript" src="/AjaxTk/js/events/AjxEventMgr.js"></script>
<script type="text/javascript" src="/AjaxTk/js/events/AjxListener.js"></script>
<script type="text/javascript" src="/AjaxTk/js/util/AjxDateUtil.js"></script>
<script type="text/javascript" src="/AjaxTk/js/util/AjxStringUtil.js"></script>
<script type="text/javascript" src="/AjaxTk/js/util/AjxVector.js"></script>
<script type="text/javascript" src="/AjaxTk/js/util/AjxSelectionManager.js"></script>
<script type="text/javascript" src="/AjaxTk/js/net/AjxPost.js"></script>
<script type="text/javascript" src="/AjaxTk/js/util/AjxBuffer.js"></script>
<script type="text/javascript" src="/AjaxTk/js/util/AjxCache.js"></script>

<!-- DWT classes -->
<script type="text/javascript" src="/AjaxTk/js/dwt/core/DwtImg.js"></script>

<script type="text/javascript" src="/AjaxTk/js/dwt/core/Dwt.js"></script>
<script type="text/javascript" src="/AjaxTk/js/dwt/core/DwtException.js"></script>
<script type="text/javascript" src="/AjaxTk/js/dwt/core/DwtDraggable.js"></script>

<script type="text/javascript" src="/AjaxTk/js/dwt/graphics/DwtCssStyle.js"></script>
<script type="text/javascript" src="/AjaxTk/js/dwt/graphics/DwtPoint.js"></script>
<script type="text/javascript" src="/AjaxTk/js/dwt/graphics/DwtRectangle.js"></script>
<script type="text/javascript" src="/AjaxTk/js/dwt/graphics/DwtUnits.js"></script>

<script type="text/javascript" src="/AjaxTk/js/dwt/events/DwtEvent.js"></script>
<script type="text/javascript" src="/AjaxTk/js/dwt/events/DwtEventManager.js"></script>
<script type="text/javascript" src="/AjaxTk/js/dwt/events/DwtDateRangeEvent.js"></script>
<script type="text/javascript" src="/AjaxTk/js/dwt/events/DwtDisposeEvent.js"></script>
<script type="text/javascript" src="/AjaxTk/js/dwt/events/DwtUiEvent.js"></script>
<script type="text/javascript" src="/AjaxTk/js/dwt/events/DwtControlEvent.js"></script>
<script type="text/javascript" src="/AjaxTk/js/dwt/events/DwtKeyEvent.js"></script>
<script type="text/javascript" src="/AjaxTk/js/dwt/events/DwtMouseEvent.js"></script>
<script type="text/javascript" src="/AjaxTk/js/dwt/events/DwtMouseEventCapture.js"></script>
<script type="text/javascript" src="/AjaxTk/js/dwt/events/DwtListViewActionEvent.js"></script>
<script type="text/javascript" src="/AjaxTk/js/dwt/events/DwtSelectionEvent.js"></script>
<script type="text/javascript" src="/AjaxTk/js/dwt/events/DwtHtmlEditorStateEvent.js"></script>
<script type="text/javascript" src="/AjaxTk/js/dwt/events/DwtTreeEvent.js"></script>
<script type="text/javascript" src="/AjaxTk/js/dwt/events/DwtHoverEvent.js"></script>

<script type="text/javascript" src="/AjaxTk/js/dwt/dnd/DwtDragEvent.js"></script>
<script type="text/javascript" src="/AjaxTk/js/dwt/dnd/DwtDragSource.js"></script>
<script type="text/javascript" src="/AjaxTk/js/dwt/dnd/DwtDropEvent.js"></script>
<script type="text/javascript" src="/AjaxTk/js/dwt/dnd/DwtDropTarget.js"></script>

<script type="text/javascript" src="/AjaxTk/js/dwt/widgets/DwtHoverMgr.js"></script>

<script type="text/javascript" src="/AjaxTk/js/dwt/widgets/DwtControl.js"></script>
<script type="text/javascript" src="/AjaxTk/js/dwt/widgets/DwtComposite.js"></script>
<script type="text/javascript" src="/AjaxTk/js/dwt/widgets/DwtShell.js"></script>
<script type="text/javascript" src="/AjaxTk/js/dwt/widgets/DwtColorPicker.js"></script>
<script type="text/javascript" src="/AjaxTk/js/dwt/widgets/DwtBaseDialog.js"></script>
<script type="text/javascript" src="/AjaxTk/js/dwt/widgets/DwtDialog.js"></script>
<script type="text/javascript" src="/AjaxTk/js/dwt/widgets/DwtLabel.js"></script>
<script type="text/javascript" src="/AjaxTk/js/dwt/widgets/DwtListView.js"></script>
<script type="text/javascript" src="/AjaxTk/js/dwt/widgets/DwtButton.js"></script>
<script type="text/javascript" src="/AjaxTk/js/dwt/widgets/DwtMenuItem.js"></script>
<script type="text/javascript" src="/AjaxTk/js/dwt/widgets/DwtMenu.js"></script>
<script type="text/javascript" src="/AjaxTk/js/dwt/widgets/DwtMessageDialog.js"></script>
<script type="text/javascript" src="/AjaxTk/js/dwt/widgets/DwtHtmlEditor.js"></script>
<script type="text/javascript" src="/AjaxTk/js/dwt/widgets/DwtInputField.js"></script>
<script type="text/javascript" src="/AjaxTk/js/dwt/widgets/DwtSash.js"></script>
<script type="text/javascript" src="/AjaxTk/js/dwt/widgets/DwtToolBar.js"></script>
<script type="text/javascript" src="/AjaxTk/js/dwt/graphics/DwtBorder.js"></script>
<script type="text/javascript" src="/AjaxTk/js/dwt/widgets/DwtToolTip.js"></script>
<script type="text/javascript" src="/AjaxTk/js/dwt/widgets/DwtStickyToolTip.js"></script>
<script type="text/javascript" src="/AjaxTk/js/dwt/widgets/DwtTreeItem.js"></script>
<script type="text/javascript" src="/AjaxTk/js/dwt/widgets/DwtTree.js"></script>
<script type="text/javascript" src="/AjaxTk/js/dwt/widgets/DwtCalendar.js"></script>
<script type="text/javascript" src="/AjaxTk/js/dwt/widgets/DwtPropertyPage.js"></script>
<script type="text/javascript" src="/AjaxTk/js/dwt/widgets/DwtTabView.js"></script>
<script type="text/javascript" src="/AjaxTk/js/dwt/widgets/DwtWizardDialog.js"></script>
<script type="text/javascript" src="/AjaxTk/js/dwt/widgets/DwtSelect.js"></script>
<script type="text/javascript" src="/AjaxTk/js/dwt/widgets/DwtAddRemove.js"></script>
<script type="text/javascript" src="/AjaxTk/js/dwt/widgets/DwtAlert.js"></script>
<script type="text/javascript" src="/AjaxTk/js/dwt/widgets/DwtText.js"></script>
<script type="text/javascript" src="/AjaxTk/js/dwt/widgets/DwtIframe.js"></script>
<script type="text/javascript" src="/AjaxTk/js/dwt/widgets/DwtXFormDialog.js"></script>
<script type="text/javascript" src="/AjaxTk/js/dwt/widgets/DwtPropertySheet.js"></script>
<script type="text/javascript" src="/AjaxTk/js/dwt/widgets/DwtGrouper.js"></script>
<script type="text/javascript" src="/AjaxTk/js/dwt/widgets/DwtProgressBar.js"></script>

<script type="text/javascript" src="/AjaxTk/js/dwt/events/DwtXFormsEvent.js"></script>
<script type="text/javascript" src="/AjaxTk/js/dwt/xforms/XFormGlobal.js"></script>
<script type="text/javascript" src="/AjaxTk/js/dwt/xforms/XModel.js"></script>
<script type="text/javascript" src="/AjaxTk/js/dwt/xforms/XModelItem.js"></script>
<script type="text/javascript" src="/AjaxTk/js/dwt/xforms/XForm.js"></script>
<script type="text/javascript" src="/AjaxTk/js/dwt/xforms/XFormItem.js"></script>
<script type="text/javascript" src="/AjaxTk/js/dwt/xforms/XFormChoices.js"></script>
<script type="text/javascript" src="/AjaxTk/js/dwt/xforms/OSelect_XFormItem.js"></script>
<script type="text/javascript" src="/AjaxTk/js/dwt/xforms/ButtonGrid.js"></script>
<!-- END SCRIPT BLOCK -->
