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
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.JooqPersistenceManager;
import de.fraunhofer.iosb.ilt.frostserver.plugin.coremodel.TableImpFeatures;
import de.fraunhofer.iosb.ilt.frostserver.service.ServiceRequest;
import de.fraunhofer.iosb.ilt.frostserver.settings.CoreSettings;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.IncompleteEntityException;
import java.security.Principal;

public class TableHelperFeatureOfInterest extends TableHelper {

    private final TableImpFeatures tableFoI;

    public TableHelperFeatureOfInterest(CoreSettings settings, JooqPersistenceManager ppm) {
        super(settings, ppm);

        this.tableFoI = tables.getTableForClass(TableImpFeatures.class);
    }

    @Override
    public void registerPreHooks() {

        tableFoI.registerHookPreInsert(-1,
                (phase, pm, entity, insertFields) -> {

                    /*
                     * Select Phase
                     */
                    if (phase == PRE_RELATIONS) {
                        final String encodingType = (String) entity.getProperty(pluginCoreModel.etFeatureOfInterest.getProperty("encodingType"));
                        if (!encodingType.equalsIgnoreCase("application/geo+json"))
                            throw new IncompleteEntityException("Property encodingType must have value application/geo+json");
                    }

                    Principal principal = ServiceRequest.getLocalRequest().getUserPrincipal();

                    if (isAdmin(principal))
                        return true;

                    if (!pluginPlus.isEnforceOwnershipEnabled())
                        assertOwnershipFeatureOfInterest(pm, entity, principal);

                    return true;
                });

        tableFoI.registerHookPreUpdate(-1,
                (pm, entity, entityId, updateMode) -> {

                    final String encodingType = (String) entity.getProperty(pluginCoreModel.etFeatureOfInterest.getProperty("encodingType"));
                    if ((encodingType != null) && !encodingType.equalsIgnoreCase("application/geo+json"))
                        throw new IncompleteEntityException("Property encodingType must have value application/geo+json");

                    Principal principal = ServiceRequest.getLocalRequest().getUserPrincipal();

                    if (isAdmin(principal))
                        return;

                    if (pluginPlus.isEnforceOwnershipEnabled()) {
                        // Unpredictable implications as we don't know all the observations were this FeatureOfInterest is associated to
                        throw new IllegalArgumentException("Updating a FeatureOfInterest is not supported");
                    }

                });

        tableFoI.registerHookPreDelete(-1, (pm, entityId) -> {

            Principal principal = ServiceRequest.getLocalRequest().getUserPrincipal();

            if (isAdmin(principal))
                return;

            // Unpredictable implications as we don't know all the observations were this FeatureOfInterest is associated to
            throw new IllegalArgumentException("Deleting a FeatureOfInterest is not supported");
        });
    }

    private void assertOwnershipFeatureOfInterest(JooqPersistenceManager pm, Entity location, Principal principal) throws IllegalArgumentException {
        EntitySet observations = location.getProperty(pluginCoreModel.npObservationsFeature);
        if ((observations != null) && (observations.getCount() > 1))
            throw new IllegalArgumentException("Cannot check ownership of FeatureOfInterest for more than one Observation");

        if (observations != null) {
            Entity observation = pm.get(pluginCoreModel.etObservation, observations.iterator().next().getPrimaryKeyValues());
            assertOwnershipObservation(pm, observation, principal);
        }
    }

}
