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
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.factories.HookPreDelete;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.factories.HookPreInsert;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.factories.HookPreUpdate;
import de.fraunhofer.iosb.ilt.frostserver.plugin.coremodel.TableImpDatastreams;
import de.fraunhofer.iosb.ilt.frostserver.service.ServiceRequest;
import de.fraunhofer.iosb.ilt.frostserver.settings.CoreSettings;
import de.fraunhofer.iosb.ilt.frostserver.util.ParserUtils;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.IncompleteEntityException;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.NoSuchEntityException;
import de.securedimensions.frostserver.plugin.staplus.TableImpParty;
import java.security.Principal;
import java.util.Map;
import org.jooq.Field;
import org.jooq.impl.DSL;

public class TableHelperDatastream extends TableHelper {

    private final TableImpDatastreams tableDatastreams;

    public TableHelperDatastream(CoreSettings settings, PostgresPersistenceManager ppm) {
        super(settings, ppm);

        this.tableDatastreams = tables.getTableForClass(TableImpDatastreams.class);

        final int partyDatastreamsIdIdx = tableDatastreams.registerField(DSL.name("PARTY_ID"), tables.getTableForClass(TableImpParty.class).getIdType());
        tableDatastreams.getPropertyFieldRegistry()
                .addEntry(pluginPlus.npPartyDatastream, table -> table.field(partyDatastreamsIdIdx));

    }

    public void registerPreHooks() {

        tableDatastreams.registerHookPreInsert(-10.0, new HookPreInsert() {

            @Override
            public boolean insertIntoDatabase(PostgresPersistenceManager pm, Entity entity,
                    Map<Field, Object> insertFields) throws NoSuchEntityException, IncompleteEntityException {

                if (!pluginPlus.isEnforceOwnershipEnabled())
                    return true;

                Principal principal = ServiceRequest.LOCAL_REQUEST.get().getUserPrincipal();

                if (isAdmin(principal))
                    return true;

                assertOwnershipDatastream(entity, principal);

                if (pluginPlus.isEnforceLicensingEnabled())
                    assertDatastreamLicense(entity);

                return true;
            }
        });

        tableDatastreams.registerHookPreUpdate(-10.0, new HookPreUpdate() {

            @Override
            public void updateInDatabase(PostgresPersistenceManager pm, Entity entity, Object entityId)
                    throws NoSuchEntityException, IncompleteEntityException {

                Principal principal = ServiceRequest.LOCAL_REQUEST.get().getUserPrincipal();

                if (isAdmin(principal))
                    return;

                Entity datastream = pm.get(pluginCoreModel.etDatastream, ParserUtils.idFromObject((entityId)));
                assertOwnershipDatastream(datastream, principal);

                if (pluginPlus.isEnforceLicensingEnabled())
                    assertDatastreamLicense(datastream);

            }
        });

        tableDatastreams.registerHookPreDelete(-10.0, new HookPreDelete() {

            @Override
            public void delete(PostgresPersistenceManager pm, Object entityId) throws NoSuchEntityException {

                if (!pluginPlus.isEnforceOwnershipEnabled())
                    return;

                Principal principal = ServiceRequest.LOCAL_REQUEST.get().getUserPrincipal();

                if (isAdmin(principal))
                    return;

                Entity datastream = pm.get(pluginCoreModel.etDatastream, ParserUtils.idFromObject((entityId)));
                assertOwnershipDatastream(datastream, principal);

            }
        });

    }

}
