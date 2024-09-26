/*
 * Copyright (C) 2021-2024 Secure Dimensions GmbH, D-81377
 * Munich, Germany.
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

import static de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.factories.HookPreInsert.Phase.PRE_RELATIONS;

import de.fraunhofer.iosb.ilt.frostserver.model.core.Entity;
import de.fraunhofer.iosb.ilt.frostserver.model.core.EntitySet;
import de.fraunhofer.iosb.ilt.frostserver.parser.path.PathParser;
import de.fraunhofer.iosb.ilt.frostserver.parser.query.QueryParser;
import de.fraunhofer.iosb.ilt.frostserver.path.ResourcePath;
import de.fraunhofer.iosb.ilt.frostserver.path.Version;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.JooqPersistenceManager;
import de.fraunhofer.iosb.ilt.frostserver.plugin.coremodel.TableImpLocations;
import de.fraunhofer.iosb.ilt.frostserver.query.Query;
import de.fraunhofer.iosb.ilt.frostserver.service.ServiceRequest;
import de.fraunhofer.iosb.ilt.frostserver.settings.CoreSettings;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.IncompleteEntityException;
import java.security.Principal;

public class TableHelperLocation extends TableHelper {

    private final TableImpLocations tableLocations;

    public TableHelperLocation(CoreSettings settings, JooqPersistenceManager ppm) {
        super(settings, ppm);

        this.tableLocations = tables.getTableForClass(TableImpLocations.class);
    }

    @Override
    public void registerPreHooks() {

        tableLocations.registerHookPreInsert(-1,
                (phase, pm, entity, insertFields) -> {

                    /*
                     * Select Phase
                     */
                    if (phase == PRE_RELATIONS) {
                        final String encodingType = (String) entity.getProperty(pluginCoreModel.etLocation.getProperty("encodingType"));
                        if (!encodingType.equalsIgnoreCase("application/geo+json"))
                            throw new IncompleteEntityException("Property encodingType must have value application/geo+json");
                    }

                    Principal principal = ServiceRequest.getLocalRequest().getUserPrincipal();

                    if (isAdmin(principal))
                        return true;

                    if (pluginPlus.isEnforceOwnershipEnabled())
                        assertOwnershipLocation(pm, entity, principal);

                    return true;
                });

        tableLocations.registerHookPreUpdate(-1,
                (pm, entity, entityId, updateMode) -> {

                    final String encodingType = (String) entity.getProperty(pluginCoreModel.etLocation.getProperty("encodingType"));
                    if (encodingType != null && !encodingType.equalsIgnoreCase("application/geo+json"))
                        throw new IncompleteEntityException("Property encodingType must have value application/geo+json");

                    Principal principal = ServiceRequest.getLocalRequest().getUserPrincipal();

                    if (isAdmin(principal))
                        return;

                    if (pluginPlus.isEnforceOwnershipEnabled())
                        assertOwnershipLocation(pm, entity, principal);

                });

        tableLocations.registerHookPreDelete(-1, (pm, entityId) -> {

            if (!pluginPlus.isEnforceOwnershipEnabled())
                return;

            Principal principal = ServiceRequest.getLocalRequest().getUserPrincipal();

            if (isAdmin(principal))
                return;

            ResourcePath rp = PathParser.parsePath(pm.getCoreSettings().getModelRegistry(), pm.getCoreSettings().getQueryDefaults().getServiceRootUrl(), Version.V_1_1, "/Locations(" + entityId.get(0) + ")");
            Query query = QueryParser.parseQuery("$expand=Things", pm.getCoreSettings().getQueryDefaults(), rp.getMainElementType().getModelRegistry(), rp);
            query.validate();
            Entity location = (Entity) pm.get(rp, query);

            assertOwnershipLocation(pm, location, principal);
        });

    }

    private void assertOwnershipLocation(JooqPersistenceManager pm, Entity location, Principal principal) throws IllegalArgumentException {
        EntitySet things = location.getProperty(pluginCoreModel.npThingsLocation);
        if ((things != null) && (things.getCount() > 1))
            throw new IllegalArgumentException("Cannot check ownership of Location for more than one Thing");

        if (things != null) {
            Entity thing = pm.get(pluginCoreModel.etThing, things.iterator().next().getPrimaryKeyValues());
            assertOwnershipThing(pm, thing, principal);
        }
    }

}
