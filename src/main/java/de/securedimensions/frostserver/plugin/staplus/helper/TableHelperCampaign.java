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

import de.fraunhofer.iosb.ilt.frostserver.model.core.Entity;
import de.fraunhofer.iosb.ilt.frostserver.model.core.Id;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.JooqPersistenceManager;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.factories.HookPreDelete;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.factories.HookPreInsert;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.factories.HookPreUpdate;
import de.fraunhofer.iosb.ilt.frostserver.service.ServiceRequest;
import de.fraunhofer.iosb.ilt.frostserver.settings.CoreSettings;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.IncompleteEntityException;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.NoSuchEntityException;
import de.securedimensions.frostserver.plugin.staplus.TableImpCampaign;
import java.security.Principal;
import java.util.Map;
import org.jooq.Field;

public class TableHelperCampaign extends TableHelper {

    private final TableImpCampaign tableCampaign;

    public TableHelperCampaign(CoreSettings settings, JooqPersistenceManager ppm) {
        super(settings, ppm);

        this.tableCampaign = tables.getTableForClass(TableImpCampaign.class);
    }

    @Override
    public void registerPreHooks() {

        tableCampaign.registerHookPreInsert(-10.0, new HookPreInsert() {

            @Override
            public boolean insertIntoDatabase(Phase phase, JooqPersistenceManager pm, Entity campaign,
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

                assertOwnershipCampaign(pm, campaign, principal);

                if (pluginPlus.isEnforceLicensingEnabled()) {
                    assertLicenseCampaign(pm, campaign);
                    if (campaign.isSetProperty(pluginPlus.npLicenseCampaign))
                        assertEmptyCampaign(pm, campaign);
                }

                return true;
            }
        });

        tableCampaign.registerHookPreUpdate(-10.0, new HookPreUpdate() {

            @Override
            public void updateInDatabase(JooqPersistenceManager pm, Entity campaign, Id entityId)
                    throws NoSuchEntityException, IncompleteEntityException {

                if (!pluginPlus.isEnforceOwnershipEnabled())
                    return;

                Principal principal = ServiceRequest.getLocalRequest().getUserPrincipal();

                if (isAdmin(principal))
                    return;

                // We need to assert on the existing Campaign that is to be updated
                Entity storedCampaign = pm.get(pluginPlus.etCampaign, campaign.getId());
                assertOwnershipCampaign(pm, storedCampaign, principal);

                if (pluginPlus.isEnforceLicensingEnabled()) {
                    assertLicenseCampaign(pm, storedCampaign);
                    if (campaign.isSetProperty(pluginPlus.npLicenseCampaign))
                        assertEmptyCampaign(pm, storedCampaign);
                }

            }
        });

        tableCampaign.registerHookPreDelete(-10.0, new HookPreDelete() {

            @Override
            public void delete(JooqPersistenceManager pm, Id entityId) throws NoSuchEntityException {

                if (!pluginPlus.isEnforceOwnershipEnabled())
                    return;

                Principal principal = ServiceRequest.getLocalRequest().getUserPrincipal();

                if (isAdmin(principal))
                    return;

                Entity campaign = pm.get(pluginPlus.etCampaign, entityId);
                assertOwnershipCampaign(pm, campaign, principal);
            }
        });

    }

}
