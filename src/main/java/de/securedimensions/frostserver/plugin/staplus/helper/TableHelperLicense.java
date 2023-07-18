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
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.JooqPersistenceManager;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.factories.HookPreDelete;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.factories.HookPreInsert;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.factories.HookPreUpdate;
import de.fraunhofer.iosb.ilt.frostserver.service.ServiceRequest;
import de.fraunhofer.iosb.ilt.frostserver.settings.CoreSettings;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.ForbiddenException;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.IncompleteEntityException;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.NoSuchEntityException;
import de.securedimensions.frostserver.plugin.staplus.PluginPLUS;
import de.securedimensions.frostserver.plugin.staplus.TableImpLicense;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.jooq.Field;

public class TableHelperLicense extends TableHelper {

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

        tableLicenses.registerHookPreInsert(-10.0, new HookPreInsert() {

            @Override
            public boolean insertIntoDatabase(Phase phase, JooqPersistenceManager pm, Entity license,
                    Map<Field, Object> insertFields) throws NoSuchEntityException, IncompleteEntityException {

                /*
                 * Select Phase
                 */
                if (phase == Phase.PRE_RELATIONS)
                    return true;

                if (!pluginPlus.isEnforceLicensingEnabled())
                    return true;

                String definition = (String) license.getProperty(pluginPlus.etLicense.getProperty("definition"));
                if (!TableHelperLicense.LICENSE_DEFINITIONS.contains(definition)) {
                    throw new IllegalArgumentException("The value for 'definition' is not allowed");
                }

                Principal principal = ServiceRequest.getLocalRequest().getUserPrincipal();

                if (isAdmin(principal)) {
                    return (pm.get(pluginPlus.etLicense, license.getId()) == null);
                }

                if (license.isSetProperty(pluginPlus.npDatastreamsLicense)) {
                    EntitySet ds = license.getProperty(pluginPlus.npDatastreamsLicense);
                    if (ds == null) {
                        throw new IllegalArgumentException("Datastreams do not exist.");
                    }
                    for (Entity d : ds) {
                        assertLicenseDatastream(pm, d);
                        assertOwnershipDatastream(pm, d, principal);
                        assertEmptyDatastream(pm, d);
                    }
                } else if (license.isSetProperty(pluginPlus.npMultiDatastreamsLicense)) {
                    EntitySet mds = license.getProperty(pluginPlus.npMultiDatastreamsLicense);
                    if (mds == null) {
                        throw new IllegalArgumentException("MultiDatastreams do not exist.");
                    }
                    for (Entity md : mds) {
                        assertLicenseMultiDatastream(pm, md);
                        assertOwnershipMultiDatastream(pm, md, principal);
                        assertEmptyMultiDatastream(pm, md);
                    }
                } else if (license.isSetProperty(pluginPlus.npProjectsLicense)) {
                    EntitySet ps = license.getProperty(pluginPlus.npProjectsLicense);
                    if (ps == null) {
                        throw new IllegalArgumentException("Projects do not exist.");
                    }
                    for (Entity p : ps) {
                        assertLicenseProject(pm, p);
                        assertOwnershipProject(pm, p, principal);
                        assertEmptyProject(pm, p);
                    }
                } else if (license.isSetProperty(pluginPlus.npGroupsLicense)) {
                    EntitySet gs = license.getProperty(pluginPlus.npGroupsLicense);
                    if (gs == null) {
                        throw new IllegalArgumentException("Groups do not exist.");
                    }
                    for (Entity g : gs) {
                        assertLicenseGroup(pm, g);
                        assertOwnershipGroup(pm, g, principal);
                        assertEmptyGroup(pm, g);
                    }
                } else
                    throw new IllegalArgumentException("License must be associated with `Datastream`, `MultiDatastream`, `Project` or `Group`.");

                return true;
            }
        });

        tableLicenses.registerHookPreUpdate(-10.0, new HookPreUpdate() {

            @Override
            public void updateInDatabase(JooqPersistenceManager pm, Entity license, Id entityId)
                    throws NoSuchEntityException, IncompleteEntityException {

                if (!pluginPlus.isEnforceLicensingEnabled())
                    return;

                Principal principal = ServiceRequest.getLocalRequest().getUserPrincipal();

                if (isAdmin(principal))
                    return;

                //if (LICENSE_IDS.contains(license.getId().getValue()))
                //throw new ForbiddenException("System license cannot be updated.");

                if (license.isSetProperty(pluginPlus.npDatastreamsLicense)) {
                    EntitySet ds = license.getProperty(pluginPlus.npDatastreamsLicense);
                    if (ds == null) {
                        throw new IllegalArgumentException("Datastreams do not exist.");
                    }
                    for (Entity d : ds) {
                        assertLicenseDatastream(pm, d);
                        assertOwnershipDatastream(pm, d, principal);
                        assertEmptyDatastream(pm, d);
                    }
                } else if (license.isSetProperty(pluginPlus.npMultiDatastreamsLicense)) {
                    EntitySet mds = license.getProperty(pluginPlus.npMultiDatastreamsLicense);
                    if (mds == null) {
                        throw new IllegalArgumentException("MultiDatastreams do not exist.");
                    }
                    for (Entity md : mds) {
                        assertLicenseMultiDatastream(pm, md);
                        assertOwnershipMultiDatastream(pm, md, principal);
                        assertEmptyMultiDatastream(pm, md);
                    }
                } else if (license.isSetProperty(pluginPlus.npProjectsLicense)) {
                    EntitySet ps = license.getProperty(pluginPlus.npProjectsLicense);
                    if (ps == null) {
                        throw new IllegalArgumentException("Projects do not exist.");
                    }
                    for (Entity p : ps) {
                        assertLicenseProject(pm, p);
                        assertOwnershipProject(pm, p, principal);
                        assertEmptyProject(pm, p);
                    }
                } else if (license.isSetProperty(pluginPlus.npGroupsLicense)) {
                    EntitySet gs = license.getProperty(pluginPlus.npGroupsLicense);
                    if (gs == null) {
                        throw new IllegalArgumentException("Groups do not exist.");
                    }
                    for (Entity g : gs) {
                        assertLicenseGroup(pm, g);
                        assertOwnershipGroup(pm, g, principal);
                        assertEmptyGroup(pm, g);
                    }
                } else
                    throw new ForbiddenException("License must be associated with `Datastream`, `MultiDatastream`, `Project` or `Group`.");
            }
        });

        tableLicenses.registerHookPreDelete(-10.0, new HookPreDelete() {

            @Override
            public void delete(JooqPersistenceManager pm, Id entityId) throws NoSuchEntityException {

                if (!pluginPlus.isEnforceLicensingEnabled())
                    return;

                Principal principal = ServiceRequest.getLocalRequest().getUserPrincipal();

                if (isAdmin(principal))
                    return;

                throw new ForbiddenException("License cannot be deleted.");

            }
        });
    }

}
