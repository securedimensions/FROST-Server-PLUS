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
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.PostgresPersistenceManager;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.factories.HookPreDelete;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.factories.HookPreInsert;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.factories.HookPreUpdate;
import de.fraunhofer.iosb.ilt.frostserver.plugin.coremodel.TableImpObservations;
import de.fraunhofer.iosb.ilt.frostserver.service.ServiceRequest;
import de.fraunhofer.iosb.ilt.frostserver.settings.CoreSettings;
import de.fraunhofer.iosb.ilt.frostserver.util.ParserUtils;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.IncompleteEntityException;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.NoSuchEntityException;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import org.jooq.Field;

public class TableHelperObservation extends TableHelper {

    public static final String CC_PD_ID = "CC_PD";
    public static final String CC_BY_ID = "CC_BY";
    public static final String CC_BY_SA_ID = "CC_BY_SA";
    public static final String CC_BY_NC_ID = "CC_BY_NC";
    public static final String CC_BY_ND_ID = "CC_BY_ND";
    public static final String CC_BY_NC_SA_ID = "CC_BY_NC_SA";
    public static final String CC_BY_NC_ND_ID = "CC_BY_NC_ND";
    public static final String CC_PD_LICENSE = "{\n"
            + "            \"@iot.id\": \"" + CC_PD_ID + "\",\n"
            + "            \"name\": \"CC_PD\",\n"
            + "            \"definition\": \"https://creativecommons.org/publicdomain/zero/1.0/\",\n"
            + "            \"description\": \"CC0 1.0 Universal (CC0 1.0) Public Domain Dedication\",\n"
            + "            \"logo\": \"https://mirrors.creativecommons.org/presskit/buttons/88x31/png/cc-zero.png\"\n"
            + "        }";
    public static final String CC_BY_LICENSE = "{\n"
            + "            \"@iot.id\": \"" + CC_BY_ID + "\",\n"
            + "            \"name\": \"CC BY 3.0\",\n"
            + "            \"definition\": \"https://creativecommons.org/licenses/by/3.0\",\n"
            + "            \"description\": \"The Creative Commons Attribution license\",\n"
            + "            \"logo\": \"https://mirrors.creativecommons.org/presskit/buttons/88x31/png/by.png\"\n"
            + "        }";
    public static final String CC_BY_SA_LICENSE = "{\n"
            + "            \"@iot.id\": \"" + CC_BY_SA_ID + "\",\n"
            + "            \"name\": \"CC BY-SA 3.0\",\n"
            + "            \"definition\": \"https://creativecommons.org/licenses/by-sa/3.0\",\n"
            + "            \"description\": \"The Creative Commons Attribution & Share-alike license\",\n"
            + "            \"logo\": \"https://mirrors.creativecommons.org/presskit/buttons/88x31/png/by-sa.png\"\n"
            + "        }";
    public static final String CC_BY_NC_LICENSE = "{\n"
            + "            \"@iot.id\": \"" + CC_BY_NC_ID + "\",\n"
            + "            \"name\": \"CC BY-NC 3.0\",\n"
            + "            \"definition\": \"https://creativecommons.org/licenses/by-nc/3.0\",\n"
            + "            \"description\": \"The Creative Commons Attribution & non-commercial license\",\n"
            + "            \"logo\": \"https://mirrors.creativecommons.org/presskit/buttons/88x31/png/by-nc.png\"\n"
            + "        }";
    public static final String CC_BY_ND_LICENSE = "{\n"
            + "            \"@iot.id\": \"" + CC_BY_ND_ID + "\",\n"
            + "            \"name\": \"CC BY-ND 3.0\",\n"
            + "            \"definition\": \"https://creativecommons.org/licenses/by-nd/3.0\",\n"
            + "            \"description\": \"The Creative Commons Attribution & no-derivs license\",\n"
            + "            \"logo\": \"https://mirrors.creativecommons.org/presskit/buttons/88x31/png/by-nd.png\"\n"
            + "        }";
    public static final String CC_BY_NC_SA_LICENSE = "{\n"
            + "            \"@iot.id\": \"" + CC_BY_NC_SA_ID + "\",\n"
            + "            \"name\": \"CC BY-NC-SA 3.0\",\n"
            + "            \"definition\": \"https://creativecommons.org/licenses/by-nc-sa/3.0/\",\n"
            + "            \"description\": \"The Creative Commons Attribution & Share-alike non-commercial license\",\n"
            + "            \"logo\": \"https://mirrors.creativecommons.org/presskit/buttons/88x31/png/by-nc-sa.png\"\n"
            + "        }";
    public static final String CC_BY_NC_ND_LICENSE = "{\n"
            + "            \"@iot.id\": \"" + CC_BY_NC_ND_ID + "\",\n"
            + "            \"name\": \"CC BY-NC-ND 3.0\",\n"
            + "            \"definition\": \"https://creativecommons.org/licenses/by-nc-nd/3.0/\",\n"
            + "            \"description\": \"The Creative Commons Attribution & Share-alike non-commercial no-derivs license\",\n"
            + "            \"logo\": \"https://mirrors.creativecommons.org/presskit/buttons/88x31/png/by-nc-nd.png\"\n"
            + "        }";
    public static final Map<String, String> LICENSES = new HashMap<String, String>(7);
    private static final String[] CC_PD = {CC_PD_ID, CC_BY_ID, CC_BY_SA_ID, CC_BY_NC_ID, CC_BY_ND_ID, CC_BY_NC_ND_ID, CC_BY_NC_SA_ID};
    private static final String[] CC_BY = {CC_BY_ID, CC_BY_SA_ID, CC_BY_NC_ID, CC_BY_NC_SA_ID};
    private static final String[] CC_BY_SA = {CC_BY_ID, CC_BY_SA_ID};
    private static final String[] CC_BY_NC = {CC_BY_ID, CC_BY_NC_ID, CC_BY_NC_SA_ID};
    private static final String[] CC_BY_ND = {};
    private static final String[] CC_BY_NC_SA = {CC_BY_ID, CC_BY_NC_ID, CC_BY_NC_SA_ID};
    private static final String[] CC_BY_NC_ND_SA = {};

    static {
        LICENSES.put(TableHelperObservation.CC_PD_ID, TableHelperObservation.CC_PD_LICENSE);
        LICENSES.put(TableHelperObservation.CC_BY_ID, TableHelperObservation.CC_BY_LICENSE);
        LICENSES.put(TableHelperObservation.CC_BY_SA_ID, TableHelperObservation.CC_BY_SA_LICENSE);
        LICENSES.put(TableHelperObservation.CC_BY_NC_ID, TableHelperObservation.CC_BY_NC_LICENSE);
        LICENSES.put(TableHelperObservation.CC_BY_ND_ID, TableHelperObservation.CC_BY_ND_LICENSE);
        LICENSES.put(TableHelperObservation.CC_BY_NC_SA_ID, TableHelperObservation.CC_BY_NC_SA_LICENSE);
        LICENSES.put(TableHelperObservation.CC_BY_NC_ND_ID, TableHelperObservation.CC_BY_NC_ND_LICENSE);
    }

    private final TableImpObservations tableObservations;

    public TableHelperObservation(CoreSettings settings, PostgresPersistenceManager ppm) {
        super(settings, ppm);

        this.tableObservations = tables.getTableForClass(TableImpObservations.class);

    }

    @Override
    public void registerPreHooks() {

        tableObservations.registerHookPreInsert(-10.0, new HookPreInsert() {

            @Override
            public boolean insertIntoDatabase(Phase phase, PostgresPersistenceManager pm, Entity entity,
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

                assertOwnershipObservation(pm, entity, principal);

                if (pluginPlus.isEnforceLicensingEnabled())
                    assertLicenseCompatibilty(pm, entity);

                return true;

            }
        });

        tableObservations.registerHookPreUpdate(-10.0, new HookPreUpdate() {

            @Override
            public void updateInDatabase(PostgresPersistenceManager pm, Entity entity, Id entityId)
                    throws NoSuchEntityException, IncompleteEntityException {

                if (!pluginPlus.isEnforceOwnershipEnabled())
                    return;

                Principal principal = ServiceRequest.getLocalRequest().getUserPrincipal();

                if (isAdmin(principal))
                    return;

                assertOwnershipObservation(pm, entity, principal);

            }
        });

        tableObservations.registerHookPreDelete(-10.0, new HookPreDelete() {

            @Override
            public void delete(PostgresPersistenceManager pm, Id entityId) throws NoSuchEntityException {

                if (!pluginPlus.isEnforceOwnershipEnabled())
                    return;

                Principal principal = ServiceRequest.getLocalRequest().getUserPrincipal();

                if (isAdmin(principal))
                    return;

                // The Observation from the DB contains the Datastream
                Entity observation = pm.get(pluginCoreModel.etObservation,
                        ParserUtils.idFromObject((entityId)));
                assertOwnershipObservation(pm, observation, principal);

            }
        });

    }

    protected void assertLicenseCompatibilty(PostgresPersistenceManager pm, Entity entity) {

        Entity group = null;
        if (entity.isSetProperty(pluginPlus.npObservationGroups)) {
            EntitySet groups = entity.getProperty(pluginPlus.npObservationGroups);
            if ((groups == null) || (groups.getCount() > 1))
                throw new IllegalArgumentException("Cannot check license of Observation for more than one Group");

            group = groups.iterator().next();
        } else {
            group = null;
        }

        if (group != null) {
            // The Observation is added to a group
            Entity groupLicense = null;
            if (group.isSetProperty(pluginPlus.npLicenseGroup))
                groupLicense = group.getProperty(pluginPlus.npLicenseGroup);
            else {
                group = pm.get(pluginPlus.etGroup, entity.getId());
                if ((group != null) && group.isSetProperty(pluginPlus.npLicenseGroup))
                    groupLicense = pm.get(pluginPlus.etLicense,
                            group.getProperty(pluginPlus.npLicenseGroup).getId());
                else
                    groupLicense = null;
            }

            if (groupLicense != null) {
                // The Group has a License so we need to check the compatibility with the license for the Datastream of the Observation
                String groupLicenseId = groupLicense.getProperty(pluginPlus.epIdLicense).toString();

                Entity datastream = null;
                Entity multiDatastream = null;

                if (entity.isSetProperty(pluginCoreModel.npDatastreamObservation))
                    datastream = entity.getProperty(pluginCoreModel.npDatastreamObservation);
                else if ((pluginMultiDatastream != null) && (entity.isSetProperty(pluginMultiDatastream.npMultiDatastreamObservation)))
                    multiDatastream = entity.getProperty(pluginMultiDatastream.npMultiDatastreamObservation);

                if ((datastream == null) && (multiDatastream == null)) {
                    Entity observation = pm.get(pluginCoreModel.etObservation, entity.getId());
                    if ((observation != null) && observation.isSetProperty(pluginCoreModel.npDatastreamObservation))
                        datastream = pm.get(pluginCoreModel.etDatastream,
                                observation.getProperty(pluginCoreModel.npDatastreamObservation).getId());
                    else
                        datastream = null;
                }

                if ((datastream == null) && (multiDatastream == null)) {
                    Entity observation = pm.get(pluginCoreModel.etObservation, entity.getId());
                    if ((observation != null)
                            && observation.isSetProperty(pluginMultiDatastream.npMultiDatastreamObservation))
                        multiDatastream = pm.get(pluginMultiDatastream.etMultiDatastream,
                                observation.getProperty(pluginMultiDatastream.npMultiDatastreamObservation).getId());
                    else
                        multiDatastream = null;
                }

                if (datastream != null)
                    if (datastream.isSetProperty(pluginPlus.npLicenseDatastream)) {
                        assertLicenseCompatibilty(datastream.getProperty(pluginPlus.npLicenseDatastream).getId().toString(), groupLicenseId);
                    } else {
                        datastream = pm.get(pluginCoreModel.etDatastream, datastream.getId());
                        assertLicenseCompatibilty(datastream.getProperty(pluginPlus.npLicenseDatastream).getId().toString(), groupLicenseId);
                    }

                if (multiDatastream != null)
                    if (multiDatastream.isSetProperty(pluginPlus.npLicenseMultiDatastream)) {
                        assertLicenseCompatibilty(multiDatastream.getProperty(pluginPlus.npLicenseMultiDatastream).getId().toString(), groupLicenseId);
                    } else {
                        multiDatastream = pm.get(pluginMultiDatastream.etMultiDatastream, multiDatastream.getId());
                        assertLicenseCompatibilty(multiDatastream.getProperty(pluginPlus.npLicenseMultiDatastream).getId().toString(), groupLicenseId);

                    }
            }
        }
    }

    protected void assertLicenseCompatibilty(String sourceId, String targetId) {
        boolean compatible = false;
        switch (sourceId) {
            case CC_PD_ID:
                compatible = contains(CC_PD, targetId);
                break;
            case CC_BY_ID:
                compatible = contains(CC_BY, targetId);
                break;
            case CC_BY_NC_ID:
                compatible = contains(CC_BY_NC, targetId);
                break;
            case CC_BY_SA_ID:
                compatible = contains(CC_BY_SA, targetId);
                break;
            case CC_BY_ND_ID:
                compatible = contains(CC_BY_ND, targetId);
                break;
            case CC_BY_NC_SA_ID:
                compatible = contains(CC_BY_NC_SA, targetId);
                break;
            case CC_BY_NC_ND_ID:
                compatible = contains(CC_BY_NC_ND_SA, targetId);
                break;
        }

        if (!compatible)
            throw new IllegalArgumentException("Observation License not compatible with Group License.");
    }

    private boolean contains(String[] haystack, String needle) {
        for (String s : haystack) {
            if (s.equalsIgnoreCase(needle))
                return true;
        }

        return false;
    }
}
