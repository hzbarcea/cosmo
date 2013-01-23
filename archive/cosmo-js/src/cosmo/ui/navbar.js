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

dojo.provide('cosmo.ui.navbar');

dojo.require("dojox.date.posix");
dojo.require("cosmo.app.pim");
dojo.require("cosmo.app.pim.layout");
dojo.require("cosmo.util.i18n");
dojo.require("cosmo.util.hash");
dojo.require("cosmo.util.html");
dojo.require("cosmo.util.deferred");
dojo.require("cosmo.datetime.util");
dojo.require("cosmo.convenience");
dojo.require("cosmo.ui.ContentBox");
dojo.require("cosmo.ui.widget.NavButtonSet");
dojo.require("cosmo.ui.widget.GraphicRadioButtonSet");
dojo.require("cosmo.ui.widget.Button");
dojo.require("cosmo.ui.widget.AwesomeBox");
dojo.require("cosmo.view.cal.common");
dojo.require("cosmo.view.list.common");
dojo.require("cosmo.account.preferences");

cosmo.ui.navbar.Bar = function (p) {
    var self = this;
    var params = p || {};
    this.domNode = null;
    this.id = '';
    this.width = 0;
    // Ref to the list canvas UI widget, passed in in constructor
    this.listCanvas = null;
    // Ref to the cal canvas UI widget, passed in in constructor
    this.calCanvas = null;
    this.monthHeader = null;
    this.listViewPageNum = null;
    this.defaultViewHasBeenInitialized = false;
    this.calViewNav = new cosmo.ui.navbar.CalViewNav({ parent: this });
    this.quickItemEntry = new cosmo.ui.widget.AwesomeBox({ parent: this, id: "awesomeBox"});
    this.listViewPager = new cosmo.ui.navbar.ListPager({ parent: this,
                                                         listCanvas: p.listCanvas
                                                       });

    for (var n in params) { this[n] = params[n]; }

    // Interface methods
    // ===========
    // Handle published messages -- in this case just a poke
    // to let the NavBar know to re-render

    var r = function(){self.render();};
    dojo.subscribe('cosmo:calEventsLoadSuccess', r);
    dojo.subscribe('cosmo:calNavigateLoadedCollection', r);
    dojo.subscribe('cosmo:calSaveSuccess', r);

    this.renderSelf = function () {
        var _pim = cosmo.app.pim;
        this.clearAll();
        this.children = [];
        // Both views -- floated left
        this._addViewToggle();
        // Cal view -- floated right
        if (_pim.currentView == _pim.views.CAL) {
            this.addChild(this.calViewNav);
        }
        // List view
        else if (_pim.currentView == _pim.views.LIST) {
            // Floated left
            this.addChild(this.quickItemEntry);
            // Floated right
            this.addChild(this.listViewPager);
            //this._showListPager();
        }
        // Break the float of the different NavBar items
        var t = _createElem('div');
        t.className = 'clearBoth';
        this.domNode.appendChild(t);

        if (self.parent) { self.width = self.parent.width; }
        self.setSize(self.width - 2, CAL_TOP_NAV_HEIGHT-1);
        self.setPosition(0, 0);
    };

    this.displayView = function (params) {
        // Do params with keywords, because there are
        // two optional params that are both Bools
        var viewName = params.viewName;
        // Optional, Bool -- don't refresh data before toggling view
        var noLoad = typeof params.noLoad == 'undefined' ? false : params.noLoad;
        // Optional, Bool -- don't check for unsaved data in
        // detail form -- this means user has explicitly chosen
        // Discard Changes in the dialog box
        var discardUnsavedChanges =
          typeof params.discardUnsavedChanges == 'undefined' ?
          false : params.discardUnsavedChanges;
        var _pim = cosmo.app.pim;
        var _view = cosmo.view[viewName];
        var _loading = cosmo.app.pim.layout.baseLayout.mainApp.centerColumn.loading;

        // Check for unsaved changes in the selected item
        // before toggling view
        var previousView = cosmo.view[_pim.currentView];
        if (!discardUnsavedChanges &&
            (previousView.canvasInstance && previousView.itemRegistry)) {
            var sel = previousView.canvasInstance.getSelectedItem();
            if (sel) {
                var discardFunc = function () {
                    params.discardUnsavedChanges = true;
                    self.displayView.apply(self, [params]);
                }
                var resetFunc = function () {
                    self.renderSelf();
                };
                if (!cosmo.view.handleUnsavedChanges(sel,
                    discardFunc, resetFunc, resetFunc)) {
                    return false;
                }
            }
        }

        // Set currentView to the new selected view
        _pim.currentView = viewName;

        // Save new selected view name for reloading
        cosmo.account.preferences.setCookiePreference(
            cosmo.account.preferences.DEFAULT_VIEW, viewName)

        // Set up the view on initial display
        if (!_view.hasBeenInitialized) {
            _view.init();
        }
        // When *not* to show the canvas-level 'loading' message
        // 1. on initial load -- we have the app-level one
        // still showing
        // 2. When there are no collections loaded -- in that
        // // case, navigating around should show no 'loading'
        // message, because nothing is loading
        if (cosmo.app.pim.getSelectedCollection() && this.defaultViewHasBeenInitialized) {
            _loading.show();
        }
        this.defaultViewHasBeenInitialized = true;

        var doDisplay = function () {
            var displayDeferred = null;
            if (viewName == _pim.views.LIST) {
                // Only switch views if the data for the view loads successfully
                displayDeferred = _view.loadItems();
                displayDeferred.addCallback(function(){
                    // If the cal canvas is currently showing, save the scroll
                    // offset of the timed canvas
                    if (self.calCanvas.domNode.style.display == 'block') {
                        cosmo.view.cal.canvas.saveTimedCanvasScrollOffset();
                    }
                    self.calCanvas.domNode.style.display = 'none';
                    self.listCanvas.domNode.style.display = 'block';
                });
            }
            else if (viewName == _pim.views.CAL) {
                // Set up topic subscriptions in the canvas -- published
                // message "data has loaded" tells the canvas to render
                if (!_view.canvas.hasBeenInitialized) {
                    _view.canvas.init();
                }
                // Only switch views if the data for the view loads successfully
                // or if we're not gettting data from the server
                var switchViews = function(){
                    self.listCanvas.domNode.style.display = 'none';
                    self.calCanvas.domNode.style.display = 'block';
                    cosmo.view.cal.canvas.resetTimedCanvasScrollOffset();
                }
                if (noLoad) {
                    switchViews();
                }
                else {
                    displayDeferred = _view.loadItems();
                    displayDeferred.addCallback(switchViews);
                }
            }
            else {
                throw(viewName + ' is not a valid view.');
            }
            displayDeferred = displayDeferred || cosmo.util.deferred.getFiredDeferred();
            displayDeferred.addErrback(function(e){console.log(e)});
            return displayDeferred;

        }
        setTimeout(doDisplay, 0);
    };

    // Private methods
    this._addViewToggle = function () {
        var d = this.domNode;
        var _pim = cosmo.app.pim;
        var selButtonIndex = _pim.currentView == _pim.views.LIST ? 0 : 1;

        var w = 24;
        var btns = [{ width: w,
                defaultImgSel: 'cosmoListViewDefault',
                id: "cosmoViewToggleListViewSelector",
                mouseoverImgSel: 'cosmoListViewSelected',
                downStateImgSel: 'cosmoListViewSelected',
                handleClick: function () {
                    self.displayView({ viewName: cosmo.app.pim.views.LIST }); }
                },
                { width: w,
                id: "cosmoViewToggleCalViewSelector",
                defaultImgSel: 'cosmoCalViewDefault',
                mouseoverImgSel: 'cosmoCalViewSelected',
                downStateImgSel: 'cosmoCalViewSelected',
                handleClick: function () {
                    self.displayView({ viewName:cosmo.app.pim.views.CAL }); }
                }
        ];

        // Clean up if previously rendered
        if (this.viewToggle) { this.viewToggle.destroyRecursive(); }

        var t = _createElem('div');
        t.id = 'viewToggle';
        t.className = 'floatLeft';
        this.viewToggleNode = t;
        var vT =  new cosmo.ui.widget.GraphicRadioButtonSet({
            selectedButtonIndex: selButtonIndex, buttonProps: btns,
            id: "viewToggle"
        });
        t.appendChild(vT.domNode);
        this.viewToggle = vT;
        d.appendChild(t);
    };
};

cosmo.ui.navbar.Bar.prototype = new cosmo.ui.ContentBox();

cosmo.ui.navbar.CalViewNav = function (p) {
    var params = p || {};
    this.parent = params.parent;
    this.domNode = _createElem('div');
    this.navButtons = null;

    this.renderSelf = function () {
        var t = this.domNode;
        var _html = cosmo.util.html;
        t.id = 'calViewNav';
        t.className = 'floatRight';
        t.style.paddingRight = '12px';

        this.clearAll();

        var table = _createElem('table');
        var body = _createElem('tbody');
        var tr = _createElem('tr');
        table.cellPadding = '0';
        table.cellSpacing = '0';
        table.appendChild(body);
        body.appendChild(tr);
        // Month/year header
        var td = _createElem('td');
        td.id = 'monthHeaderDiv';
        this.monthHeader = td;
        td.className = 'labelTextXL';
        tr.appendChild(td);
        // Spacer
        var td = _createElem('td');
        td.style.width = '12px';
        td.appendChild(_html.nbsp());
        tr.appendChild(td);
        // Buttons
        var td = _createElem('td');
        td.id = 'viewNavButtons';
        self.viewNavButtons = td;
        // It would be nicer to do this in a decentralized way
        // with topics, but we need the feedback box to show
        // instantly -- so, call the 'loading' box's show method
        // directly, then publish the message to the topic on
        // a setTimeout
        var loading = cosmo.app.pim.layout.baseLayout.mainApp.centerColumn.loading;
        var showLoading = function () {
            // Don't display the 'loading data' message if
            // there's no selected collection to load data from
            if (cosmo.app.pim.getSelectedCollection()) {
                loading.show();
            }
        }
        var publish = function (dir) {
            dojo.publish('cosmo:calLoadCollection',[{
                opts: { loadType: 'changeTimespan', goTo: dir },
                    data: {} }]);
        };
        var back = function back() {
            var backFunc = function () {
                publish('back');
            };
            showLoading();
            setTimeout(backFunc, 0);
        }
        var next = function next() {
            var nextFunc = function () {
                publish('next');
            };
            showLoading();
            setTimeout(nextFunc, 0);
        }
        if (this.navButtons) this.navButtons.destroy();
        this.navButtons = new cosmo.ui.widget.NavButtonSet(
            {id: 'viewNav', leftClickHandler: back, rightClickHandler: next});
        self.viewNavButtons.appendChild(this.navButtons.domNode);
        tr.appendChild(td);

        t.appendChild(table);

        // Show the specified month
        this._showMonthHeader();
    };
    // FIXME: There is similar logic is dup'd in ...
    // view.cal.common.loadItems
    // ui.minical -- setSelectionSpan private function
    // ui.navbar._showMonthheader
    // These different UI widgets have to be independent
    // of the calendar view, but still display sync'd
    // information -- what's a good way to consolidate this?
    this._showMonthHeader = function () {
        // FIXME; Logic for getting week start / week end is
        // dup'd in view.cal.common, and ui.minical
        if (!cosmo.view.cal.viewStart || !cosmo.view.cal.viewEnd) {
            var defaultDate = cosmo.app.pim.currDate;
            var vS = cosmo.datetime.util.getWeekStart(defaultDate);
            var vE = cosmo.datetime.util.getWeekEnd(defaultDate);
        }
        else {
            var vS = cosmo.view.cal.viewStart;
            var vE =  cosmo.view.cal.viewEnd;
        }
        var mS = vS.getMonth();
        var mE = vE.getMonth();
        var headerDiv = this.monthHeader;
        var str = '';

        // Format like 'March-April, 2006'
        if (mS < mE) {
            str += dojox.date.posix.strftime(vS, '%B-');
            str += dojox.date.posix.strftime(vE, '%B %Y');
        }
        // Format like 'December 2006-January 2007'
        else if (mS > mE) {
            str += dojox.date.posix.strftime(vS, '%B %Y-');
            str += dojox.date.posix.strftime(vE, '%B %Y');
        }
        // Format like 'April 2-8, 2006'
        else {
            str += dojox.date.posix.strftime(vS, '%B %Y');
        }
        if (headerDiv.firstChild) {
            headerDiv.removeChild(headerDiv.firstChild);
        }
        headerDiv.appendChild(document.createTextNode(str));
    };
};

cosmo.ui.navbar.CalViewNav.prototype = new cosmo.ui.ContentBox();


cosmo.ui.navbar.ListPager = function (p) {
    var self = this;
    var params = p || {};
    this.parent = params.parent;
    this.listCanvas = p.listCanvas;
    this.domNode = _createElem('div');
    this.prevButton = null;
    this.nextButton = null;
    this.renderSelf = function () {
        var form = null;
        // Go-to page num
        var goToPage = function (e) {
            if (e.target && e.target.id && e.keyCode && e.keyCode == 13) {
                var list = self.listCanvas;
                var n = form[e.target.id].value;
                n = n > list.pageCount ? list.pageCount : n;
                n = n < 1 ? 1 : n;
                list.currPageNum = n;
                list.displayListViewTable();
            }
        };
        var t = this.domNode;
        t.className = 'floatRight';
        t.style.paddingRight = '12px';

        // Cleanup
        this.clearAll();
        if (this.prevButton || this.nextButton) {
            this.prevButton.destroy();
            this.nextButton.destroy();
        }

        // Bail out if the list view is a zero-lenth list
        // no length = nothing to page through
        if (!cosmo.view.list.itemRegistry.length) { return false; }

        var table = _createElem('table');
        var body = _createElem('tbody');
        var tr = _createElem('tr');
        table.cellPadding = '0';
        table.cellSpacing = '0';
        table.appendChild(body);
        body.appendChild(tr);

        var td = _createElem('td');
        if (this.parent.listCanvas.currPageNum == 1) {
            var fPrev = null;
        }
        else {
            var fPrev = this.parent.listCanvas.goPrevPage;
        }
        button = new cosmo.ui.widget.Button(
            {
                text: 'Prev',
                handleOnClick: fPrev,
                small: true,
                width: 44,
                enabled: (typeof fPrev == 'function') })

        td.appendChild(button.domNode);
        this.prevButton = button;
        tr.appendChild(td);

        var td = _createElem('td');
        td.id = 'listViewPageNum';
        td.style.padding = '0px 8px';
        form = _createElem('form');
        // Suppress submission
        form.onsubmit = function () { return false; };
        td.appendChild(form);
        var o = { type: 'text',
            id: 'listViewGoToPage',
            name: 'listViewGoToPage',
            size: 1,
            className: 'inputText',
            value: this.parent.listCanvas.currPageNum
        };
        var text = cosmo.util.html.createInput(o);
        dojo.connect(text, 'onkeyup', goToPage);
        form.appendChild(text);
        td.appendChild(_createText(' of ' + this.parent.listCanvas.pageCount));
        this.listViewPageNum = td;
        tr.appendChild(td);

        var td = _createElem('td');
        if (this.parent.listCanvas.currPageNum == this.parent.listCanvas.pageCount) {
            var fNext = null;
        }
        else {
            var fNext = this.parent.listCanvas.goNextPage;
        }
        var buttonDiv = _createElem('div');
        button = new cosmo.ui.widget.Button(
            {
                text: 'Next',
                handleOnClick: fNext,
                small: true,
                width: 44,
                enabled: (typeof fNext == 'function') });

        td.appendChild(button.domNode);
        this.nextButton = button;
        tr.appendChild(td);

        t.appendChild(table);
    };
};

cosmo.ui.navbar.ListPager.prototype = new cosmo.ui.ContentBox();



