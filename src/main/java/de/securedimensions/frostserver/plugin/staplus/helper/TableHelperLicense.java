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
import de.fraunhofer.iosb.ilt.frostserver.service.ServiceRequest;
import de.fraunhofer.iosb.ilt.frostserver.settings.CoreSettings;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.ForbiddenException;
import de.securedimensions.frostserver.plugin.staplus.PluginPLUS;
import de.securedimensions.frostserver.plugin.staplus.TableImpLicense;
import java.security.Principal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TableHelperLicense extends TableHelper {

    public static final String CC_PD_ID = "CC_PD";
    public static final String CC_BY_ID = "CC_BY";
    public static final String CC_BY_SA_ID = "CC_BY_SA";
    public static final String CC_BY_NC_ID = "CC_BY_NC";
    public static final String CC_BY_ND_ID = "CC_BY_ND";
    public static final String CC_BY_NC_SA_ID = "CC_BY_NC_SA";
    public static final String CC_BY_NC_ND_ID = "CC_BY_NC_ND";
    public static final Map<String, String> LICENSES = new HashMap<String, String>(7);
    public static final String[] CC_PD = {CC_PD_ID, CC_BY_ID, CC_BY_SA_ID, CC_BY_NC_ID, CC_BY_ND_ID, CC_BY_NC_ND_ID, CC_BY_NC_SA_ID};
    public static final String[] CC_BY = {CC_BY_ID, CC_BY_SA_ID, CC_BY_NC_ID, CC_BY_NC_SA_ID};
    public static final String[] CC_BY_SA = {CC_BY_ID, CC_BY_SA_ID};
    public static final String[] CC_BY_NC = {CC_BY_ID, CC_BY_NC_ID, CC_BY_NC_SA_ID};
    public static final String[] CC_BY_ND = {};
    public static final String[] CC_BY_NC_SA = {CC_BY_ID, CC_BY_NC_ID, CC_BY_NC_SA_ID};
    public static final String[] CC_BY_NC_ND_SA = {};

    private final PluginPLUS pluginPlus;
    private final TableImpLicense tableLicenses;

    static final List<String> LICENSE_DEFINITIONS = Arrays.asList(
            "https://creativecommons.org/licenses/by/3.0/deed.en",
            "https://creativecommons.org/licenses/by-nc/3.0/deed.en",
            "https://creativecommons.org/licenses/by-sa/3.0/deed.en",
            "https://creativecommons.org/licenses/by-nc-sa/3.0/deed.en",
            "https://creativecommons.org/licenses/by-nd/3.0/deed.en",
            "https://creativecommons.org/licenses/by-nc-nd/3.0/deed.en");

    public TableHelperLicense(CoreSettings settings, JooqPersistenceManager ppm) {
        super(settings, ppm);

        this.tableLicenses = tables.getTableForClass(TableImpLicense.class);
        this.pluginPlus = settings.getPluginManager().getPlugin(PluginPLUS.class);
    }

    @Override
    public void registerPreHooks() {

        tableLicenses.registerHookPreInsert(-1,
                (phase, pm, entity, insertFields) -> {

                    /*
                     * Select Phase
                     */
                    if (phase == PRE_RELATIONS) {
                        return true;
                    }

                    if (!pluginPlus.isEnforceLicensingEnabled())
                        return true;

                    String definition = (String) entity.getProperty(pluginPlus.etLicense.getProperty("definition"));
                    if (!TableHelperLicense.LICENSE_DEFINITIONS.contains(definition)) {
                        throw new IllegalArgumentException("This value for 'definition' is not allowed");
                    }

                    Principal principal = ServiceRequest.getLocalRequest().getUserPrincipal();

                    if (isAdmin(principal)) {
                        return (pm.get(pluginPlus.etLicense, entity.getPrimaryKeyValues()) == null);
                    }

                    if (entity.isSetProperty(pluginPlus.npDatastreamsLicense)) {
                        EntitySet ds = entity.getProperty(pluginPlus.npDatastreamsLicense);
                        if (ds == null) {
                            throw new IllegalArgumentException("Datastreams do not exist.");
                        }
                        for (Entity d : ds) {
                            assertLicenseDatastream(pm, d);
                            assertOwnershipDatastream(pm, d, principal);
                            assertEmptyDatastream(pm, d);
                        }
                    } else if (entity.isSetProperty(pluginPlus.npMultiDatastreamsLicense)) {
                        EntitySet mds = entity.getProperty(pluginPlus.npMultiDatastreamsLicense);
                        if (mds == null) {
                            throw new IllegalArgumentException("MultiDatastreams do not exist.");
                        }
                        for (Entity md : mds) {
                            assertLicenseMultiDatastream(pm, md);
                            assertOwnershipMultiDatastream(pm, md, principal);
                            assertEmptyMultiDatastream(pm, md);
                        }
                    } else if (entity.isSetProperty(pluginPlus.npCampaignsLicense)) {
                        EntitySet ps = entity.getProperty(pluginPlus.npCampaignsLicense);
                        if (ps == null) {
                            throw new IllegalArgumentException("Campaigns do not exist.");
                        }
                        for (Entity p : ps) {
                            assertLicenseCampaign(pm, p);
                            assertOwnershipCampaign(pm, p, principal);
                            assertEmptyCampaign(pm, p);
                        }
                    } else if (entity.isSetProperty(pluginPlus.npGroupsLicense)) {
                        EntitySet gs = entity.getProperty(pluginPlus.npGroupsLicense);
                        if (gs == null) {
                            throw new IllegalArgumentException("Groups do not exist.");
                        }
                        for (Entity g : gs) {
                            assertLicenseGroup(pm, g);
                            assertOwnershipGroup(pm, g, principal);
                            assertEmptyGroup(pm, g);
                        }
                    } else
                        ;//throw new IllegalArgumentException("License must be associated with `Datastream`, `MultiDatastream`, `Campaign` or `Group`.");

                    return true;
                });

        tableLicenses.registerHookPreUpdate(-1,
                (pm, entity, entityId, updateMode) -> {

                    if (!pluginPlus.isEnforceLicensingEnabled())
                        return;

                    Principal principal = ServiceRequest.getLocalRequest().getUserPrincipal();

                    if (isAdmin(principal))
                        return;

                    //if (LICENSE_IDS.contains(license.getId().getValue()))
                    //throw new ForbiddenException("System license cannot be updated.");

                    if (entity.isSetProperty(pluginPlus.npDatastreamsLicense)) {
                        EntitySet ds = entity.getProperty(pluginPlus.npDatastreamsLicense);
                        if (ds == null) {
                            throw new IllegalArgumentException("Datastreams do not exist.");
                        }
                        for (Entity d : ds) {
                            assertLicenseDatastream(pm, d);
                            assertOwnershipDatastream(pm, d, principal);
                            assertEmptyDatastream(pm, d);
                        }
                    } else if (entity.isSetProperty(pluginPlus.npMultiDatastreamsLicense)) {
                        EntitySet mds = entity.getProperty(pluginPlus.npMultiDatastreamsLicense);
                        if (mds == null) {
                            throw new IllegalArgumentException("MultiDatastreams do not exist.");
                        }
                        for (Entity md : mds) {
                            assertLicenseMultiDatastream(pm, md);
                            assertOwnershipMultiDatastream(pm, md, principal);
                            assertEmptyMultiDatastream(pm, md);
                        }
                    } else if (entity.isSetProperty(pluginPlus.npCampaignsLicense)) {
                        EntitySet ps = entity.getProperty(pluginPlus.npCampaignsLicense);
                        if (ps == null) {
                            throw new IllegalArgumentException("Campaigns do not exist.");
                        }
                        for (Entity p : ps) {
                            assertLicenseCampaign(pm, p);
                            assertOwnershipCampaign(pm, p, principal);
                            assertEmptyCampaign(pm, p);
                        }
                    } else if (entity.isSetProperty(pluginPlus.npGroupsLicense)) {
                        EntitySet gs = entity.getProperty(pluginPlus.npGroupsLicense);
                        if (gs == null) {
                            throw new IllegalArgumentException("Groups do not exist.");
                        }
                        for (Entity g : gs) {
                            assertLicenseGroup(pm, g);
                            assertOwnershipGroup(pm, g, principal);
                            assertEmptyGroup(pm, g);
                        }
                    } else
                        throw new ForbiddenException("License must be associated with `Datastream`, `MultiDatastream`, `Campaign` or `Group`.");

                });

        tableLicenses.registerHookPreDelete(-1, (pm, entityId) -> {

            if (!pluginPlus.isEnforceLicensingEnabled())
                return;

            Principal principal = ServiceRequest.getLocalRequest().getUserPrincipal();

            if (isAdmin(principal))
                return;

            throw new ForbiddenException("License cannot be deleted.");

        });
    }

}
