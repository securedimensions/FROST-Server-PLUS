/*
 * Copyright (C) 2021-2023 Secure Dimensions GmbH, D-81377
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

import de.fraunhofer.iosb.ilt.frostserver.model.core.Entity;
import de.fraunhofer.iosb.ilt.frostserver.model.core.EntitySet;
import de.fraunhofer.iosb.ilt.frostserver.model.core.Id;
import de.fraunhofer.iosb.ilt.frostserver.parser.path.PathParser;
import de.fraunhofer.iosb.ilt.frostserver.parser.query.QueryParser;
import de.fraunhofer.iosb.ilt.frostserver.path.ResourcePath;
import de.fraunhofer.iosb.ilt.frostserver.path.Version;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.JooqPersistenceManager;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.factories.HookPreDelete;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.factories.HookPreInsert;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.factories.HookPreUpdate;
import de.fraunhofer.iosb.ilt.frostserver.plugin.coremodel.TableImpFeatures;
import de.fraunhofer.iosb.ilt.frostserver.query.Query;
import de.fraunhofer.iosb.ilt.frostserver.service.ServiceRequest;
import de.fraunhofer.iosb.ilt.frostserver.settings.CoreSettings;
import de.fraunhofer.iosb.ilt.frostserver.util.ParserUtils;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.ForbiddenException;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.IncompleteEntityException;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.NoSuchEntityException;
import org.jooq.Field;

import java.security.Principal;
import java.util.Map;

public class TableHelperFeatureOfInterest extends TableHelper {

    private final TableImpFeatures tableFoI;

    public TableHelperFeatureOfInterest(CoreSettings settings, JooqPersistenceManager ppm) {
        super(settings, ppm);

        this.tableFoI = tables.getTableForClass(TableImpFeatures.class);
    }

    @Override
    public void registerPreHooks() {

        tableFoI.registerHookPreInsert(-10.0, new HookPreInsert() {

            @Override
            public boolean insertIntoDatabase(Phase phase, JooqPersistenceManager pm, Entity entity,
                    Map<Field, Object> insertFields) throws NoSuchEntityException, IncompleteEntityException {

                /*
                 * Select Phase
                 */
                if (phase == Phase.PRE_RELATIONS) {
                    final String encodingType = (String)entity.getProperty(pluginCoreModel.etFeatureOfInterest.getProperty("encodingType"));
                    if (!encodingType.equalsIgnoreCase("application/geo+json"))
                        throw new IncompleteEntityException("Property encodingType must have value application/geo+json");
                }

                Principal principal = ServiceRequest.getLocalRequest().getUserPrincipal();

                if (isAdmin(principal))
                    return true;

                if (!pluginPlus.isEnforceOwnershipEnabled())
                    assertOwnershipFeatureOfInterest(pm, entity, principal);

                return true;
            }
        });

        tableFoI.registerHookPreUpdate(-10.0, new HookPreUpdate() {

            @Override
            public void updateInDatabase(JooqPersistenceManager pm, Entity entity, Id entityId)
                    throws NoSuchEntityException, IncompleteEntityException {

                final String encodingType = (String)entity.getProperty(pluginCoreModel.etFeatureOfInterest.getProperty("encodingType"));
                if ((encodingType != null) && !encodingType.equalsIgnoreCase("application/geo+json"))
                    throw new IncompleteEntityException("Property encodingType must have value application/geo+json");

                Principal principal = ServiceRequest.getLocalRequest().getUserPrincipal();

                if (isAdmin(principal))
                    return;

                if (pluginPlus.isEnforceOwnershipEnabled()) {
                    // Unpredictable implications as we don't know all the observations were this FeatureOfInterest is associated to
                    throw new IllegalArgumentException("Updating a FeatureOfInterest is not supported");
                }
            }
        });

        tableFoI.registerHookPreDelete(-10.0, new HookPreDelete() {

            @Override
            public void delete(JooqPersistenceManager pm, Id entityId) throws NoSuchEntityException {

                Principal principal = ServiceRequest.getLocalRequest().getUserPrincipal();

                if (isAdmin(principal))
                    return;

                // Unpredictable implications as we don't know all the observations were this FeatureOfInterest is associated to
                throw new IllegalArgumentException("Deleting a FeatureOfInterest is not supported");
            }
        });
    }

    private void assertOwnershipFeatureOfInterest(JooqPersistenceManager pm, Entity location, Principal principal) throws IllegalArgumentException {
        EntitySet observations = location.getProperty(pluginCoreModel.npObservationsFeature);
        if ((observations != null) && (observations.getCount() > 1))
            throw new IllegalArgumentException("Cannot check ownership of FeatureOfInterest for more than one Observation");

        if (observations != null) {
            Entity observation = pm.get(pluginCoreModel.etObservation, observations.iterator().next().getId());
            assertOwnershipObservation(pm, observation, principal);
        }
    }

}
