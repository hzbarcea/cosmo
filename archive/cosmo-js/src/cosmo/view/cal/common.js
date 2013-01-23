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

dojo.provide('cosmo.view.cal.common');

dojo.require("cosmo.app.pim");
dojo.require("cosmo.util.i18n");
dojo.require("cosmo.util.deferred");
dojo.require("cosmo.convenience");
dojo.require("cosmo.util.hash");
dojo.require("cosmo.model");
dojo.require("cosmo.view.cal.CalItem");
dojo.require("cosmo.view.names");
dojo.require("cosmo.datetime");
dojo.require("cosmo.datetime.util");
dojo.require('cosmo.view.service');
dojo.require("cosmo.service.exception");

dojo.mixin(cosmo.view.cal, cosmo.view.viewBase);


cosmo.view.cal.init = function(){
    cosmo.view.viewBase.init.apply(this);
    cosmo.view.cal.setQuerySpan(cosmo.app.pim.currDate);
};

cosmo.view.cal.hasBeenInitialized = false;

cosmo.view.cal.viewId = cosmo.view.names.CAL;
// Stupid order-of-loading -- this gets set in the
// canvas instance. We'll just go ahead and declare
// it here anyway, so there's an obvious declaration
cosmo.view.cal.canvasInstance =
    typeof cosmo.view.cal.canvasInstance == 'undefined' ?
    null : cosmo.view.cal.canvasInstance;
cosmo.view.cal.viewStart = null;
cosmo.view.cal.viewEnd = null;
// The list of items -- cosmo.util.hash.Hash obj
cosmo.view.cal.itemRegistry = null;
cosmo.view.cal.collectionItemRegistries = {};

/**
 * Handle events published on the '/calEvent' channel, including
 * self-published events
 * @param cmd A JS Object, the command containing orders for
 * how to handle the published event.
 */

dojo.subscribe("cosmo:calLoadCollection", function(cmd){
    if (!cosmo.view.cal.isCurrentView()) return false;
    var opts = cmd.opts || {};
    cosmo.view.cal.loadItems(opts);
});
dojo.subscribe("cosmo:appKeyboardInput",
               dojo.hitch(cosmo.view.cal, cosmo.view.cal.handleKeyboardInput));


cosmo.view.cal.loadItems = function (p) {
    console.log("trigger!");
    var _cal = cosmo.view.cal; // Scope-ness
    var params = p || {};
    var goToNav = null;
    var start = null;
    var end = null;
    var eventLoadList = null;
    var isErr = false;
    var detail = '';
    var evData = null;
    var id = '';
    var ev = null;
    var collectionReg = cosmo.view.cal.collectionItemRegistries = {};
    var queryDate = null;
    // Changing dates
    // FIXME: There is similar logic is dup'd in ...
    // view.cal.common.loadItems
    // ui.minical -- setSelectionSpan private function
    // ui.navbar._showMonthheader
    // These different UI widgets have to be independent
    // of the calendar view, but still display sync'd
    // information -- what's a good way to consolidate this?
    // --------
    if (params.goTo) {
        goToNav = params.goTo;
        // param is 'back' or 'next'
        if (typeof goToNav == 'string') {
            var key = goToNav.toLowerCase();
            var incr = key.indexOf('back') > -1 ? -1 : 1;
            queryDate = cosmo.datetime.Date.add(_cal.viewStart,
                cosmo.datetime.util.dateParts.WEEK, incr);
        }
        // param is actual Date
        else {
            queryDate = goToNav;
        }
        _cal.setQuerySpan(queryDate);
    }

    // Opts obj to pass to topic publishing
    var opts = {
        viewStart: _cal.viewStart,
        viewEnd: _cal.viewEnd,
        currDate: cosmo.app.pim.currDate
    };
    for (var n in params) { opts[n] = params[n]; }

    // Default to the app's selectedCollection if one isn't passed
    var collection = opts.collection || cosmo.app.pim.getSelectedCollection();

    start = opts.viewStart;
    end = opts.viewEnd;
    var _this = this;
    var loadEach = function (collId, coll) {
        var loadDeferred = null;
        if (goToNav) { coll.isDisplayed = false; }
        var handleErr = function (e) {
            var reloadDeferred = null;
            if (e instanceof cosmo.service.exception.ResourceNotFoundException){
                reloadDeferred = cosmo.app.pim.reloadCollections({
                    removedCollection: collection,
                    removedByThisUser: false
                });
            }
            cosmo.app.showErr(_('Main.Error.LoadItemsFailed'),"", e);
            return reloadDeferred;
        };
        if (_this.doDisplay(coll)) {
            loadDeferred = cosmo.app.pim.serv.getItems(coll,
                { start: start, end: end });
            loadDeferred.addErrback(handleErr);
            loadDeferred.addCallback(function (eventLoadList){
                var h = _this.createEventRegistry(eventLoadList, collId);
                collectionReg[collId] = h;
            });
        }
        else {
            collectionReg[collId] = new cosmo.util.hash.Hash();
        }
        coll.isDisplayed = _this.doDisplay(coll);
        return loadDeferred || cosmo.util.deferred.getFiredDeferred();
    };
    var l = cosmo.app.pim.collections.each(loadEach);
    var loadDeferred = new dojo.DeferredList(l);
    cosmo.util.deferred.addStdDLCallback(loadDeferred);
    loadDeferred.addCallback(function(){
        var itemRegistry = cosmo.view.cal.createItemRegistryFromCollections();
        dojo.publish('cosmo:calEventsLoadSuccess', [{data: itemRegistry, opts: opts }]);
    });
    return loadDeferred;
};
cosmo.view.cal.doDisplay = function(collection){
    return (collection.isOverlaid || (collection == cosmo.app.pim._selectedCollection));
};
/**
 * Create a Hash of CalItem objects with data property of stamped
 * Note objects.
 * @param arr Array
 * containing stamped Note objects
 * @return Hash, the keys are the UID of the Notes, and the values are
 * the CalItem objects.
 */
cosmo.view.cal.createEventRegistry = function(arr, collId) {
    var h = new cosmo.util.hash.Hash();
    // Testing for a length property or such is generally
    // more reliable than instanceof Array
    if (typeof arr.length != 'number') {
        throw new Error('Items loaded not in an Array.');
    }
    for (var i = 0; i < arr.length; i++) {
        var note = arr[i];
        var item = cosmo.view.cal.createItemForCollection(note, collId);
        h.setItem(item.id, item);
    }
    return h;
};
cosmo.view.cal.createItemRegistryFromCollections = function () {
    var itemReg = new cosmo.util.hash.Hash();
    if (cosmo.app.pim.getSelectedCollection()){
        var collectionReg = cosmo.view.cal.collectionItemRegistries;
        var currCollId = '';
        var selCollId = cosmo.app.pim.getSelectedCollectionId();
        // Do something sensible with duplicate items when
        // building the consolidated itemRegistry
        var fillInItem = function (id, item) {
            // Always use the items from the selected collection
            if (currCollId == selCollId) {
                item.primaryCollectionId = currCollId;
            }
            itemReg.setItem(id, item);
        };
        for (var collId in collectionReg) {
            currCollId = collId;
            collectionReg[currCollId].each(fillInItem);
        }
    }
    return itemReg;
};
cosmo.view.cal.createItemForCollection = function (note, collId) {
    var collectionReg = cosmo.view.cal.collectionItemRegistries;
    var id = note.getItemUid();
    var item = null;
    for (var c in collectionReg) {
        item = collectionReg[c].getItem(id);
        if (item) { break; }
    }
    // If you found the item already in another collection,
    // it's a dup, so just add this collection's ID to the
    // list of collectionIds and return it
    if (item) {
        item.collectionIds.push(collId);
    }
    // Otherwise create one from scratch
    else {
        item = new cosmo.view.cal.CalItem(note, [collId]);
    }
    return item;
};
cosmo.view.cal.placeItemInItsCollectionRegistries = function (item) {
    var collIds = item.collectionIds;
    var itemId = item.data.getItemUid();
    for (var i = 0; i < collIds.length; i++) {
        cosmo.view.cal.collectionItemRegistries[collIds[i]].setItem(itemId, item);
    }
};
cosmo.view.cal.removeItemFromCollectionRegistry = function (item, coll) {
    var itemId = item.data.getItemUid();
    var collId = coll.getUid();
    item.removeCollection(coll);
    cosmo.view.cal.collectionItemRegistries[collId].removeItem(itemId);
};
cosmo.view.cal.placeRecurrenceGroupInItsCollectionRegistries =
    function (collectionIds, occurrenceList) {
    for (var i = 0; i < collectionIds.length; i++) {
        var collId = collectionIds[i];
        var origRegistry =
            cosmo.view.cal.collectionItemRegistries[collId];
        var newRegistry = cosmo.view.cal.createEventRegistry(occurrenceList, collId);
        origRegistry.append(newRegistry);
    }
};
cosmo.view.cal.removeRecurrenceGroupFromItsCollectionRegistries =
    function (collIds, idsToRemove, o) {
    for (var i = 0; i < collIds.length; i++) {
        var coll = cosmo.app.pim.collections.getItem(collIds[i]);
        cosmo.view.cal.removeRecurrenceGroupFromCollectionRegistry(
            coll, idsToRemove, o);
    }
};
cosmo.view.cal.removeRecurrenceGroupFromCollectionRegistry =
    function (coll, idsToRemove, o) {
    var opts = o || {};
    var collId = coll.getUid();
    var origRegistry =
        cosmo.view.cal.collectionItemRegistries[collId];
    var newRegistry = cosmo.view.cal.filterOutRecurrenceGroup(
        origRegistry, idsToRemove, { dateToBeginRemoval:
            opts.dateToBeginRemoval, collectionForRemoval: coll });
    cosmo.view.cal.collectionItemRegistries[collId] = newRegistry;
};
/**
 * Get the start and end for the span of time to view in the cal
 */
cosmo.view.cal.setQuerySpan = function (dt) {
    this.viewStart = cosmo.datetime.util.getWeekStart(dt);
    console.log("viewStart: " + this.viewStart);
    this.viewEnd = cosmo.datetime.util.getWeekEnd(dt);
    console.log("viewEnd: " + this.viewEnd);
    return true;
};
/**
 * Get the datetime for midnight Sunday given the current Sunday
 * and the number of weeks to move forward or backward
 */
cosmo.view.cal.getNewViewStart = function (key) {
    var queryDate = null;
    var incr = 0;
    // Increment/decrement week
    if (key.indexOf('next') > -1) {
        incr = 1;
    }
    else if (key.indexOf('back') > -1) {
        incr = -1;
    }
    queryDate = cosmo.datetime.Date.add(this.viewStart,
        cosmo.datetime.util.dateParts.WEEK, incr);
    return queryDate;
};


