/* * Copyright 2008 Open Source Applications Foundation *
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

/**
 * summary:
 *   This module chooses and provides a local storage provider.
 * description:
 *   This module chooses and provides a local storage provider.
 *   If WHATWG dom storage is available, it will use that, otherwise
 *   it will default to cookie storage.
 */
dojo.provide("cosmo.storage");
dojo.require("cosmo.storage.Dom");
dojo.require("cosmo.storage.Cookie");
(function(){
    var domProvider = new cosmo.storage.Dom();
    if (domProvider.isAvailable())
        cosmo.storage.provider = domProvider;
    else cosmo.storage.provider = new cosmo.storage.Cookie();
})();
