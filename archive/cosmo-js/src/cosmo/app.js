/*
 * Copyright 2006 Open Source Applications Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

dojo.provide('cosmo.app');

dojo.require('cosmo.ui.widget.ModalDialog');
dojo.require("cosmo.ui.widget.Button");
dojo.require("cosmo.ui.timeout");
dojo.require("cosmo.util.i18n");
dojo.require("cosmo.util.auth");
dojo.require("cosmo.topics");
dojo.require("cosmo.convenience");
dojo.require("cosmo.util.popup");

cosmo.app = new function () {
    var self = this;
    var selectBoxVisibility = {};

    // App section code to run on init
    this.initObj = {};
    this.initParams = {};
    // warnings / confirmations
    this.modalDialog = null;
    // Full-screen div for masking while UI renders
    this.maskNode = null;
    // The item in the UI being dragged
    this.dragItem = null;
    // List of any queued-up error messages
    this.errorList = [];
    // Quick access to the currently logged-in user
    this.currentUsername =  cosmo.util.auth.getUsername();
    //select boxes to show/hide when displaying modal dialog box
    this._selectBoxIds = {};

    this.init = function () {
        self.maskNode = $('maskDiv');
        self.showMask();
        // Set up the modal dialog box for the app

        self.modalDialog = new cosmo.ui.widget.ModalDialog({id: "modalDialog"});
        self.modalDialog.startup();
        dojo.subscribe(
            cosmo.topics.PreferencesUpdatedMessage.topicName, self, 'updateUIFromPrefs');
        // Initialize the default view
        if (typeof self.initObj.init == 'function') {
            self.initObj.init(self.initParams);
        };
    };

    this.updateUIFromPrefs = function(){};

    // ==========================
    // Modal dialog boxes
    // ==========================
    /**
     * Show error dialog with either simple text msg, error object, or both
     * If new errors get spawned while this is processing, it queues the
     * messages for display after users dismisses the faux modal disalog box
     */
    this.showErr = function (primaryMessage, secondaryMessage, error) {
        var msg = '';

        var verboseInfo = null;
        if (error){
            verboseInfo = this._extractVerboseErrorInfo(error);
        }

        // If the error dialog is already showing, add this message to the error queue
        if (this.modalDialog.isDisplayed) {
            this.errorList.push(primaryMessage);
        }  // Otherwise display the error dialog
        else {
            // If there are errors waiting in the queue, prepend them to the error msg
            if (this.errorList.length) {
                var currErr = '';
                while (currErr = this.errorList.shift()) {
                    msg += '<div class="errText">' + currErr + '</div>';
                }
                msg += primaryMessage;
            }
            // Otherwise just display the current message
            else {
                msg = _createElem('div');
                // Primary error message -- simple message
                var d = _createElem('div');
                d.className = 'errText';
                d.innerHTML = primaryMessage;
                primaryMessage = d;
                msg.appendChild(primaryMessage);

                // Secondary message -- some details or string from the server
                d = _createElem('div');
                d.innerHTML = secondaryMessage || "";
                secondaryMessage = d;
                msg.appendChild(secondaryMessage);

                // If we have a verbose info, give the user the option of seeing it
                // in a pop-up window
                if (verboseInfo) {
                    d = _createElem('div');
                    d.innerHTML = "<textarea rows=30 cols=70>" + verboseInfo + "</textarea>"
                    verboseInfo = d;
                    var full = _createElem('div');
                    // Use a clone of the secondary message node
                    full.appendChild(secondaryMessage.cloneNode(true));
                    full.appendChild(verboseInfo);
                    var f = function () {
                        cosmo.util.popup.openFullSize(full);
                    }
                    d = _createElem('div');
                    d.style.marginTop = '8px';
                    var a = document.createElement('a');
                    // Avoid use of the ugly hack of '#' href prop
                    // Give the anchor some help to act like a real link
                    a.className = 'jsLink';
                    dojo.connect(a, 'onclick', f);
                    a.appendChild(_createText('Click here for details ...'));
                    d.appendChild(a);
                    msg.appendChild(d);
                }
            }
            this.modalDialog.type = this.modalDialog.ERROR;
            var but = new cosmo.ui.widget.Button({id: 'okButton', width: 64, handleOnClick: self.hideDialog,
                text: _('App.Button.OK'), small: true});
            this.modalDialog.btnsCenter[0] = but;
            this.modalDialog.defaultAction = self.hideDialog;
            this.modalDialog.content = msg;
            this.showDialog();
        }
    };

    this._extractVerboseErrorInfo = function(error){
        if (error.toStringVerbose){
            return error.toStringVerbose();
        }

        if (error instanceof Error){
            return "message: \n " + error.message + "\n\n"
                  +"name: \n" + error.name + "\n\n"
                  +"filename: \n" + error.fileName + "\n\n"
                  +"lineNumber: \n" + error.lineNumber + "\n\n"
                  +"stack: \n" + error.stack + "\n\n";
        }

        return null;
    };

    /**
     * Displays the mask that covers the entire browser window
     * until the UI is completely rendered
     */
    this.showMask = function () {
        if (this.maskNode) {
            this.showHideSelectBoxes(false);
            this.maskNode.style.display = 'block';
        }
    };
    /**
     * Hides the mask that covers the entire browser window
     * once the UI is completely rendered
     */
    this.hideMask = function () {
        if (this.maskNode) {
            this.maskNode.style.display = 'none';
            this.showHideSelectBoxes(true);
        }
    };
    /**
     * Display the modal dialog box with whatever set of attributes
     * is passed to it
     */
    this.showDialog = function (props) {
        for (var p in props) {
            self.modalDialog[p] = props[p];
        }
        self.setInputDisabled(true);
        // Hide the stupid IE6 select elements that ignore z-index
        // This is done app-wide right here instead of delegating
        // to individual widgets since select elements can be grabbed
        // for the entire DOM tree at one time
        if (dojo.isIE && dojo.isIE < 7) {
            self.showHideSelectBoxes(false);
        }
        // Publish message to individual widgets as well -- allow
        // UI components to run any browser-specific hacks needed --
        // e.g., hide scrollbars, turn off overflow to avoid
        // disappearing cursor, etc.
        cosmo.topics.publish(cosmo.topics.ModalDialogToggle, { isDisplayed: true });

        self.modalDialog.show();
        if (props) return props.deferred;
    };

    /**
      * Convenience function for popping a modal dialog and getting a value.
      */
    this.getValue = function (valuePrompt, defaultValue, retryConditions, kwArgs){
        kwArgs = kwArgs || {};
        var content;
        if (kwArgs.content) content = kwArgs.content;
        else {
            content = _createElem("div");
            content.innerHTML = "<input type='text' id='getValueInput' class='inputText' value='" + (defaultValue || "") + "'/>";
        }
        retryConditions = retryConditions || [];
        var deferred = new dojo.Deferred();
        var submitFunc = dojo.hitch(this, function () {
            var valueInput = dojo.query("#getValueInput")[0];
            var value = valueInput.value;
            for (var i = 0; i < retryConditions.length; i++){
                var valueErrorMessage = retryConditions[i](value);
                if (valueErrorMessage){
                    this.modalDialog.setPrompt(valueErrorMessage);
                    return false;
                }
            }
            deferred.callback(value);
        });
        var buttons = [];
        if (kwArgs.showCancel){
            buttons.push(
                new cosmo.ui.widget.Button(
                    { text: _('App.Button.Cancel'),
                      id: "getValueCancel",
                      width: 74,
                      handleOnClick: dojo.hitch(this, "hideDialog")
                    }
                )
            )
        }

        buttons.push( new cosmo.ui.widget.Button(
            { text: kwArgs.defaultActionButtonText || _('App.Button.Submit'),
              id: "getValueSubmit",
              width:74,
              handleOnClick: submitFunc
            }));

        var dialogProps = {
            "btnsRight": buttons,
            "content": content,
            "prompt": valuePrompt,
            "width": 250,
            "height": 100,
            "defaultAction": submitFunc
        };
        this.showDialog(dialogProps);
        var getValueInput = dojo.query("#getValueInput")[0];
        if (getValueInput) getValueInput.focus();
        if (!kwArgs.noAutoClose) deferred.addCallback(function(arg){cosmo.app.hideDialog(); return arg;});
        return deferred;
    };
    this.showAndWait = function (message, returnValue){
        var deferred = new dojo.Deferred();
        var submitFunc = dojo.hitch(this, function () {
            this.hideDialog();
            deferred.callback(returnValue);
        });
        var button = new cosmo.ui.widget.Button(
                              { text:_('App.Button.OK'),
                                width:74,
                                handleOnClick: submitFunc
                              });
        var dialogProps = {
            "btnsRight": [button],
            "prompt": message,
            "width" : 300,
            "height" : 100,
            "defaultAction" : submitFunc
        };
        self.showDialog(dialogProps);
        return deferred;
    };

    /**
     *  kwArgs:
     *    cancelDefault: make default action "No"
     */
    this.confirm = function (message, kwArgs){
        kwArgs = kwArgs || {};
        var deferred = new dojo.Deferred();
        var yesFunc = dojo.hitch(this, function () {
            this.hideDialog();
            deferred.callback(true);
        });
        var noFunc = dojo.hitch(this, function () {
            this.hideDialog();
            deferred.callback(false);
        });
        var yesButton = new cosmo.ui.widget.Button(
                              { text:_('App.Button.Yes'),
                                width:74,
                                handleOnClick: yesFunc,
                                id: "cosmoModalConfirmYes"
                              });
        var noButton = new cosmo.ui.widget.Button(
                              { text:_('App.Button.No'),
                                width:74,
                                handleOnClick: noFunc,
                                id: "cosmoModalConfirmNo"
                              });
        var dialogProps = {
            "btnsRight": [yesButton],
            "btnsLeft": [noButton],
            "prompt": message,
            "width" : 300,
            "height" : 100,
            "defaultAction" : kwArgs.cancelDefault? noFunc : yesFunc
        };
        self.showDialog(dialogProps);
        return deferred;
    };
    /**
     * Dismiss the faux modal dialog box -- check for queued error
     * messages to display if needed
     */
    this.hideDialog = function () {
        self.modalDialog.hide();
        // Un-hide the stupid IE6 select elements that ignore z-index
        // This is done app-wide right here instead of delegating
        // to individual widgets since select elements can be grabbed
        // for the entire DOM tree at one time
        if (dojo.isIE && dojo.isIE < 7) {
            self.showHideSelectBoxes(true);
        }
        // Publish to individual widgets as well -- allow
        // UI components to shut off any browser-specific hacks needed
        // in the display of the dialog box -- e.g., turning overflow
        // back on, etc.
        cosmo.topics.publish(cosmo.topics.ModalDialogToggle, { isDisplayed: false });
        // If there are error messages that have been added to the queue,
        // trigger another dialog to handle them
        if (self.errorList.length) {
            self.showErr('');
        }
        else {
            self.setInputDisabled(false);
        }
        return true;
    };
    this.setInputDisabled = function (isDisabled) {
        if (isDisabled) {
            this.inputDisabled = true;
        }
        else {
            this.inputDisabled = false;
        }
        return this.inputDisabled;
    };

    /**
     * Whether or not input from the entire UI is disabled
     * Returns true when the faux modal dialog box is displayed
     */
    this.getInputDisabled = function () {
        return this.inputDisabled;
    };
    this.showHideSelectBoxes = function (show) {
        var selects = document.body.getElementsByTagName('select');
        var vis = show ? 'visible' : 'hidden';
        dojo.map(selects, function (sel, show){
            if (sel.style) {
                // Storing state, or state has been stored
                if (sel.id && sel.style.visibility) {
                    // Preserve styles expressly set to 'hidden'
                    if (show) {
                        if (typeof selectBoxVisibility[sel.id] != 'undefined') {
                            sel.style.visibility = selectBoxVisibility[sel.id];
                        }
                        else {
                            sel.style.visibility = vis;
                        }
                    }
                    // Save state if necessary, and hide
                    else {
                        selectBoxVisibility[sel.id] = sel.style.visibility;
                        sel.style.visibility = vis;
                    }
                }
                else {
                    sel.style.visibility = vis;
                }
            }
        });
        // Reset the state map when finished
        if (show) {
           selectBoxVisibility = {};
        }
    };
    this.handleTimeout = function (){

        var logoutFunction = function () {
            location = cosmo.env.getRedirectUrl();
        }

        var autoLogoutTimeout =
            setTimeout(logoutFunction,
                       cosmo.ui.conf.timeoutDialogAutoLogout * 1000)

        var dialogHash = {};
        var cancelLogoutButton = new cosmo.ui.widget.Button({ text:_('Timeout.Cancel'), width:74,
            handleOnClick: function () {
                cosmo.app.hideDialog();
                clearTimeout(autoLogoutTimeout)
                cosmo.ui.timeout.updateLastActionTime()
            } });
        dialogHash.btnsLeft = [cancelLogoutButton];
        var logoutButton = new cosmo.ui.widget.Button({ text:_('Timeout.OK'), width:74,
            handleOnClick: logoutFunction
            });
        dialogHash.btnsRight = [logoutButton];
        dialogHash.title = _('App.Timeout.Title');
        dialogHash.prompt = _('App.Timeout.Prompt',
                              cosmo.env.getTimeoutMinutes(),
                              cosmo.ui.conf.timeoutDialogAutoLogout);
        self.showDialog(dialogHash);

    };
};
