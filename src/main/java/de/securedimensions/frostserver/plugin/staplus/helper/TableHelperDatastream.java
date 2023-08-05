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
import de.fraunhofer.iosb.ilt.frostserver.model.core.Id;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.JooqPersistenceManager;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.factories.HookPreDelete;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.factories.HookPreInsert;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.factories.HookPreUpdate;
import de.fraunhofer.iosb.ilt.frostserver.plugin.coremodel.TableImpDatastreams;
import de.fraunhofer.iosb.ilt.frostserver.service.ServiceRequest;
import de.fraunhofer.iosb.ilt.frostserver.settings.CoreSettings;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.IncompleteEntityException;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.NoSuchEntityException;
import de.securedimensions.frostserver.plugin.staplus.TableImpParty;
import java.security.Principal;
import java.util.Map;
import org.jooq.Field;
import org.jooq.impl.DSL;

public class TableHelperDatastream extends TableHelper {

    private final TableImpDatastreams tableDatastreams;

    public TableHelperDatastream(CoreSettings settings, JooqPersistenceManager ppm) {
        super(settings, ppm);

        this.tableDatastreams = tables.getTableForClass(TableImpDatastreams.class);

        final int partyDatastreamsIdIdx = tableDatastreams.registerField(DSL.name("PARTY_ID"), tables.getTableForClass(TableImpParty.class).getIdType());
        tableDatastreams.getPropertyFieldRegistry()
                .addEntry(pluginPlus.npPartyDatastream, table -> table.field(partyDatastreamsIdIdx));

    }

    public void registerPreHooks() {

        tableDatastreams.registerHookPreInsert(-10.0, new HookPreInsert() {

            @Override
            public boolean insertIntoDatabase(Phase phase, JooqPersistenceManager pm, Entity datastream,
                    Map<Field, Object> insertFields) throws NoSuchEntityException, IncompleteEntityException {

                /*
                 * Select Phase
                 */
                if (phase == Phase.PRE_RELATIONS)
                    return true;

                if (!pluginPlus.isEnforceOwnershipEnabled())
                    return true;

                Principal principal = ServiceRequest.getLocalRequest().getUserPrincipal();

                if (isAdmin(principal))
                    return true;

                assertOwnershipDatastream(pm, datastream, principal);

                if (pluginPlus.isEnforceLicensingEnabled()) {
                    assertLicenseDatastream(pm, datastream);
                    assertEmptyDatastream(pm, datastream);
                }

                return true;
            }
        });

        tableDatastreams.registerHookPreUpdate(-10.0, new HookPreUpdate() {

            @Override
            public void updateInDatabase(JooqPersistenceManager pm, Entity datastream, Id entityId)
                    throws NoSuchEntityException, IncompleteEntityException {

                Principal principal = ServiceRequest.getLocalRequest().getUserPrincipal();

                if (isAdmin(principal))
                    return;

                // We need to assert on the existing Datastream that is to be updated
                datastream = pm.get(pluginCoreModel.etDatastream, datastream.getId());
                assertOwnershipDatastream(pm, datastream, principal);

                if (pluginPlus.isEnforceLicensingEnabled()) {
                    assertLicenseDatastream(pm, datastream);
                    assertEmptyDatastream(pm, datastream);
                }

            }
        });

        tableDatastreams.registerHookPreDelete(-10.0, new HookPreDelete() {

            @Override
            public void delete(JooqPersistenceManager pm, Id entityId) throws NoSuchEntityException {

                if (!pluginPlus.isEnforceOwnershipEnabled())
                    return;

                Principal principal = ServiceRequest.getLocalRequest().getUserPrincipal();

                if (isAdmin(principal))
                    return;

                Entity datastream = pm.get(pluginCoreModel.etDatastream, entityId);
                assertOwnershipDatastream(pm, datastream, principal);

            }
        });

    }

}
