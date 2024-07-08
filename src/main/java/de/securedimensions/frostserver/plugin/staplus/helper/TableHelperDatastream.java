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
import de.fraunhofer.iosb.ilt.frostserver.plugin.coremodel.TableImpDatastreams;
import de.fraunhofer.iosb.ilt.frostserver.service.ServiceRequest;
import de.fraunhofer.iosb.ilt.frostserver.settings.CoreSettings;
import de.securedimensions.frostserver.plugin.staplus.TableImpParty;
import java.security.Principal;
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

        tableDatastreams.registerHookPreInsert(-1,
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

                    assertOwnershipDatastream(pm, entity, principal);

                    if (pluginPlus.isEnforceLicensingEnabled()) {
                        assertLicenseDatastream(pm, entity);
                        assertEmptyDatastream(pm, entity);
                    }

                    return true;
                });

        tableDatastreams.registerHookPreUpdate(-1,
                (pm, entity, entityId, updateMode) -> {

                    if (!pluginPlus.isEnforceOwnershipEnabled())
                        return;

                    Principal principal = ServiceRequest.getLocalRequest().getUserPrincipal();

                    if (isAdmin(principal))
                        return;

                    // We need to assert on the existing Datastream that is to be updated
                    entity = pm.get(pluginCoreModel.etDatastream, entityId);
                    assertOwnershipDatastream(pm, entity, principal);

                    if (pluginPlus.isEnforceLicensingEnabled()) {
                        assertLicenseDatastream(pm, entity);
                        assertEmptyDatastream(pm, entity);
                    }

                });

        tableDatastreams.registerHookPreDelete(-1, (pm, entityId) -> {

            if (!pluginPlus.isEnforceOwnershipEnabled())
                return;

            Principal principal = ServiceRequest.getLocalRequest().getUserPrincipal();

            if (isAdmin(principal))
                return;

            Entity datastream = pm.get(pluginCoreModel.etDatastream, entityId);
            assertOwnershipDatastream(pm, datastream, principal);

        });

    }

}
