/*
 * Copyright (C) 2023 Fraunhofer Institut IOSB, Fraunhoferstr. 1, D 76131
 * Karlsruhe, Germany.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.securedimensions.frostserver.plugin.staplus.helper;

import de.fraunhofer.iosb.ilt.frostserver.model.core.Entity;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.PostgresPersistenceManager;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.factories.EntityFactories;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.tables.TableCollection;
import de.fraunhofer.iosb.ilt.frostserver.plugin.coremodel.PluginCoreModel;
import de.fraunhofer.iosb.ilt.frostserver.plugin.multidatastream.PluginMultiDatastream;
import de.fraunhofer.iosb.ilt.frostserver.settings.CoreSettings;
import de.fraunhofer.iosb.ilt.frostserver.util.PrincipalExtended;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.ForbiddenException;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.UnauthorizedException;
import de.securedimensions.frostserver.plugin.staplus.PluginPLUS;
import java.security.Principal;

public abstract class TableHelper {

    protected final TableCollection tables;
    protected final PluginCoreModel pluginCoreModel;
    protected final PluginPLUS pluginPlus;
    protected final PluginMultiDatastream pluginMultiDatastream;
    protected EntityFactories entityFactories;

    private TableHelper() {
        this.tables = null;
        this.pluginCoreModel = null;
        this.pluginPlus = null;
        this.pluginMultiDatastream = null;
        this.entityFactories = null;
    }

    protected TableHelper(CoreSettings settings, PostgresPersistenceManager ppm) {
        this.tables = ppm.getTableCollection();
        this.pluginPlus = settings.getPluginManager().getPlugin(PluginPLUS.class);
        this.pluginCoreModel = settings.getPluginManager().getPlugin(PluginCoreModel.class);
        this.pluginMultiDatastream = settings.getPluginManager().getPlugin(PluginMultiDatastream.class);
        this.entityFactories = ppm.getEntityFactories();
    }

    protected boolean isAdmin(Principal principal) {
        if (principal == null)
            return false;

        return ((principal instanceof PrincipalExtended) && ((PrincipalExtended) principal).isAdmin());
    }

    protected void assertPrincipal(Principal principal) {
        if (principal == null)
            throw new UnauthorizedException("No Principal");
    }

    public abstract void registerPreHooks();

    protected void assertOwnershipDatastream(Entity datastream, Principal principal) {
        assertPrincipal(principal);

        if (datastream == null)
            throw new IllegalArgumentException("Datastream does not exist");

        if (!datastream.getEntityType().equals(pluginCoreModel.etDatastream))
            throw new IllegalArgumentException("Entity not of type Datastream");

        // We can get the username from the Principal
        String userId = principal.getName();

        // Ensure Ownership for Datastream
        Entity party = null;

        if (datastream != null)
            party = datastream.getProperty(pluginPlus.npPartyDatastream);

        if (party == null)
            throw new IllegalArgumentException("Datastream not linked to a Party");

        String partyId = (party.isSetProperty(pluginPlus.epAuthId)) ? party.getProperty(pluginPlus.epAuthId) : party.getId().toString();

        if (!partyId.equalsIgnoreCase(userId))
            throw new ForbiddenException("Datastream not linked to acting Party");

    }

    protected void assertOwnershipMultiDatastream(Entity multiDatastream, Principal principal) {
        assertPrincipal(principal);

        if (multiDatastream == null)
            throw new IllegalArgumentException("MultiDatastream does not exist");

        if ((pluginMultiDatastream != null) && pluginMultiDatastream.isEnabled() && !multiDatastream.getEntityType().equals(pluginMultiDatastream.etMultiDatastream))
            throw new IllegalArgumentException("Entity not of type MultiDatastream");

        // We can get the username from the Principal
        String userId = principal.getName();

        // Ensure Ownership for MultiDatastream
        Entity party = null;

        if (multiDatastream != null)
            party = multiDatastream.getProperty(pluginPlus.npPartyMultiDatastream);

        if (party == null)
            throw new IllegalArgumentException("MultiDatastream not linked to a Party");

        String partyId = (party.isSetProperty(pluginPlus.epAuthId)) ? party.getProperty(pluginPlus.epAuthId) : party.getId().toString();
        if (!partyId.equalsIgnoreCase(userId))
            throw new ForbiddenException("MultiDatastream not linked to acting Party");

    }

    protected void assertOwnershipThing(Entity thing, Principal principal) {
        assertPrincipal(principal);

        if (thing == null)
            throw new IllegalArgumentException("Thing does not exist");

        if (!thing.getEntityType().equals(pluginCoreModel.etThing))
            throw new IllegalArgumentException("Entity not of type Thing");

        // We can get the username from the Principal
        String userId = principal.getName();

        // Ensure Ownership for Thing
        Entity party = null;

        if (thing != null)
            party = thing.getProperty(pluginPlus.npPartyThing);

        if (party == null)
            throw new IllegalArgumentException("Thing not linked to a Party");

        String partyId = (party.isSetProperty(pluginPlus.epAuthId)) ? party.getProperty(pluginPlus.epAuthId) : party.getId().toString();

        if (!partyId.equalsIgnoreCase(userId))
            throw new ForbiddenException("Thing not linked to acting Party");

    }

    protected void assertOwnershipGroup(Entity group, Principal principal) {
        assertPrincipal(principal);

        if (group == null)
            throw new IllegalArgumentException("Group does not exist");

        if (!group.getEntityType().equals(pluginPlus.etGroup))
            throw new IllegalArgumentException("Entity not of type Group");

        // We can get the username from the Principal
        String userId = principal.getName();

        // Ensure Ownership for Group
        Entity party = null;

        if (group != null)
            party = group.getProperty(pluginPlus.npPartyGroup);

        if (party == null)
            throw new IllegalArgumentException("Group not linked to a Party");

        String partyId = (party.isSetProperty(pluginPlus.epAuthId)) ? party.getProperty(pluginPlus.epAuthId) : party.getId().toString();

        if (!partyId.equalsIgnoreCase(userId))
            throw new ForbiddenException("Group not linked to acting Party");

    }

    protected void assertOwnershipParty(Entity party, Principal principal) {
        assertPrincipal(principal);

        if (party == null)
            throw new IllegalArgumentException("Party does not exist");

        if (!party.getEntityType().equals(pluginPlus.etParty))
            throw new IllegalArgumentException("Entity not of type Party");

        // We can get the username from the Principal
        String userId = principal.getName();
        String partyId = party.getProperty(pluginPlus.epAuthId);
        if ((partyId != null) && (!userId.equalsIgnoreCase(partyId))) {
            // The authId is set by the plugin - it cannot be changed via a PATCH
            throw new ForbiddenException("Party not representing acting user");
        } else {
            partyId = party.getId().toString();
            if ((partyId != null) && (!userId.equalsIgnoreCase(partyId))) {
                // The authId is set by the plugin - it cannot be changed via a PATCH
                throw new ForbiddenException("Party not representing acting user");
            }
        }
    }

    protected void assertGroupLicense(Entity group) {
        if (group == null)
            throw new IllegalArgumentException("Group does not exist");

        if (!group.getEntityType().equals(pluginPlus.etGroup))
            throw new IllegalArgumentException("Entity not of type Group");

        // Ensure License for Group
        Entity license = group.getProperty(pluginPlus.npLicenseGroup);

        if (license == null)
            throw new IllegalArgumentException("Group not linked to a License");

    }

    protected void assertDatastreamLicense(Entity datastream) {
        if (datastream == null)
            throw new IllegalArgumentException("Datastream does not exist");

        if (!datastream.getEntityType().equals(pluginCoreModel.etDatastream))
            throw new IllegalArgumentException("Entity not of type Datastream");

        // Ensure License for Datastream
        Entity license = datastream.getProperty(pluginPlus.npLicenseDatastream);

        if (license == null)
            throw new IllegalArgumentException("Datastream not linked to a License");

    }

    protected void assertMultiDatastreamLicense(Entity multiDatastream) {
        if (multiDatastream == null)
            throw new IllegalArgumentException("MultiDatastream does not exist");

        if ((pluginMultiDatastream != null) && pluginMultiDatastream.isEnabled() && !multiDatastream.getEntityType().equals(pluginMultiDatastream.etMultiDatastream))
            throw new IllegalArgumentException("Entity not of type MultiDatastream");

        // Ensure License for MultiDatastream
        Entity license = multiDatastream.getProperty(pluginPlus.npLicenseMultiDatastream);

        if (license == null)
            throw new IllegalArgumentException("MultiDatastream not linked to a License");

    }

}
