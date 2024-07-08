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
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.JooqPersistenceManager;
import de.fraunhofer.iosb.ilt.frostserver.plugin.coremodel.TableImpObservations;
import de.fraunhofer.iosb.ilt.frostserver.service.ServiceRequest;
import de.fraunhofer.iosb.ilt.frostserver.settings.CoreSettings;
import java.security.Principal;

public class TableHelperObservation extends TableHelper {

    private final TableImpObservations tableObservations;

    public TableHelperObservation(CoreSettings settings, JooqPersistenceManager ppm) {
        super(settings, ppm);

        this.tableObservations = tables.getTableForClass(TableImpObservations.class);

    }

    @Override
    public void registerPreHooks() {

        tableObservations.registerHookPreInsert(-1,
                (phase, pm, entity, insertFields) -> {

                    /*
                     * Select Phase
                     */
                    if (phase == PRE_RELATIONS)
                        return true;

                    if (!pluginPlus.isEnforceOwnershipEnabled())
                        return true;

                    Principal principal = ServiceRequest.getLocalRequest().getUserPrincipal();

                    if (isAdmin(principal))
                        return true;

                    assertOwnershipObservation(pm, entity, principal);

                    if (pluginPlus.isEnforceLicensingEnabled())
                        assertLicenseCompatibilty(pm, entity);

                    return true;

                });

        tableObservations.registerHookPreUpdate(-1,
                (pm, entity, entityId, updateMode) -> {

                    if (!pluginPlus.isEnforceOwnershipEnabled())
                        return;

                    Principal principal = ServiceRequest.getLocalRequest().getUserPrincipal();

                    if (isAdmin(principal))
                        return;

                    // We need to assert on the existing Observation that is to be updated
                    entity = pm.get(pluginCoreModel.etObservation, entityId);
                    assertOwnershipObservation(pm, entity, principal);

                });

        tableObservations.registerHookPreDelete(-1, (pm, entityId) -> {

            if (!pluginPlus.isEnforceOwnershipEnabled())
                return;

            Principal principal = ServiceRequest.getLocalRequest().getUserPrincipal();

            if (isAdmin(principal))
                return;

            // The Observation from the DB contains the Datastream
            Entity observation = pm.get(pluginCoreModel.etObservation, entityId);
            assertOwnershipObservation(pm, observation, principal);

        });

    }

}
