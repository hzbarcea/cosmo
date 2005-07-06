/*
 * Copyright 2005 Open Source Applications Foundation
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
package org.osaf.cosmo.dao;

import org.osaf.cosmo.model.Ticket;

/**
 * DAO interface for Tickets.
 *
 * A ticket is associated with an item in a repository located by a
 * path. An item may have more than one ticket, but each of the item's
 * tickets must have a unique id.
 */
public interface TicketDao {

    /**
     * Returns the identified ticket for the item at the given path,
     * or <code>null</code> if the ticket does not exist. Tickets are
     * inherited, so if the specified item does not have the ticket
     * but an ancestor does, it will still be returned.
     *
     * @param path the path of the ticketed item unique to the repository
     * @param id the id of the ticket unique to the parent item
     *
     * @throws DataRetrievalFailureException if either the item or the
     * ticket are not found
     */
    public Ticket getTicket(String path, String id);
}
