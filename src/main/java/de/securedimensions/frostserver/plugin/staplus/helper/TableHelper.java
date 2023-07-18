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
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.factories.EntityFactories;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.tables.TableCollection;
import de.fraunhofer.iosb.ilt.frostserver.plugin.coremodel.PluginCoreModel;
import de.fraunhofer.iosb.ilt.frostserver.plugin.multidatastream.PluginMultiDatastream;
import de.fraunhofer.iosb.ilt.frostserver.query.Query;
import de.fraunhofer.iosb.ilt.frostserver.settings.CoreSettings;
import de.fraunhofer.iosb.ilt.frostserver.util.ParserUtils;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.ForbiddenException;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.UnauthorizedException;
import de.fraunhofer.iosb.ilt.frostserver.util.user.PrincipalExtended;
import de.securedimensions.frostserver.plugin.staplus.PluginPLUS;
import java.security.Principal;

public abstract class TableHelper {

    protected final TableCollection tables;
    protected final PluginCoreModel pluginCoreModel;
    protected final PluginPLUS pluginPlus;
    protected final PluginMultiDatastream pluginMultiDatastream;
    protected EntityFactories entityFactories;

    private TableHelper() {
        this.tables = null;
        this.pluginCoreModel = null;
        this.pluginPlus = null;
        this.pluginMultiDatastream = null;
        this.entityFactories = null;
    }

    protected TableHelper(CoreSettings settings, JooqPersistenceManager ppm) {
        this.tables = ppm.getTableCollection();
        this.pluginPlus = settings.getPluginManager().getPlugin(PluginPLUS.class);
        this.pluginCoreModel = settings.getPluginManager().getPlugin(PluginCoreModel.class);
        this.pluginMultiDatastream = settings.getPluginManager().getPlugin(PluginMultiDatastream.class);
        this.entityFactories = ppm.getEntityFactories();
    }

    protected boolean isAdmin(Principal principal) {
        if (principal == null)
            return false;

        return ((principal instanceof PrincipalExtended) && ((PrincipalExtended) principal).isAdmin());
    }

    protected void assertPrincipal(Principal principal) {
        if (principal == null)
            throw new UnauthorizedException("No Principal");
    }

    public abstract void registerPreHooks();

    protected void assertOwnershipObservation(JooqPersistenceManager pm, Entity entity, Principal principal) {

        Entity datastream = null;
        Entity multiDatastream = null;

        // Test if the Observation is inline and linked to a Datastream or MultiDatastream
        if (entity.isSetProperty(pluginCoreModel.npDatastreamObservation))
            datastream = entity.getProperty(pluginCoreModel.npDatastreamObservation);
        else if ((pluginMultiDatastream != null) && (entity.isSetProperty(pluginMultiDatastream.npMultiDatastreamObservation)))
            multiDatastream = entity.getProperty(pluginMultiDatastream.npMultiDatastreamObservation);

        // Test if Observation needs to be loaded - first from Datastream
        if ((datastream == null) && (multiDatastream == null) && (entity.getId() != null)) {
            Entity observation = pm.get(pluginCoreModel.etObservation, entity.getId());
            if ((observation != null) && observation.isSetProperty(pluginCoreModel.npDatastreamObservation))
                datastream = pm.get(pluginCoreModel.etDatastream,
                        observation.getProperty(pluginCoreModel.npDatastreamObservation).getId());
            else
                datastream = null;
        }

        // Test if Observation needs to be loaded - now from MultiDatastream
        if ((datastream == null) && (multiDatastream == null) && (entity.getId() != null)) {
            Entity observation = pm.get(pluginCoreModel.etObservation, entity.getId());
            if ((observation != null)
                    && observation.isSetProperty(pluginMultiDatastream.npMultiDatastreamObservation))
                multiDatastream = pm.get(pluginMultiDatastream.etMultiDatastream,
                        observation.getProperty(pluginMultiDatastream.npMultiDatastreamObservation).getId());
            else
                multiDatastream = null;
        }

        // If the Observation is linked to a Datastream...
        if (datastream != null)
            assertOwnershipDatastream(pm, datastream, principal);

        // If the Observation is linked to a MultiDatastream...
        if (multiDatastream != null)
            assertOwnershipMultiDatastream(pm, multiDatastream, principal);
    }

    protected void assertOwnershipDatastream(JooqPersistenceManager pm, Entity datastream, Principal principal) {
        assertPrincipal(principal);

        if (datastream == null)
            throw new IllegalArgumentException("Datastream does not exist");

        if (!datastream.getEntityType().equals(pluginCoreModel.etDatastream))
            throw new IllegalArgumentException("Entity not of type Datastream");

        // We can get the username from the Principal
        String userId = principal.getName();

        // Ensure Ownership for Datastream
        Entity party = null;

        if (datastream != null)
            party = datastream.getProperty(pluginPlus.npPartyDatastream);

        if (party == null && datastream.getId() != null) {
            datastream = pm.get(pluginCoreModel.etDatastream, datastream.getId());
            if (datastream != null) {
                party = datastream.getProperty(pluginPlus.npPartyDatastream);
            }
        }

        if (party == null)
            throw new IllegalArgumentException("Datastream not linked to a Party");

        String partyId = (party.isSetProperty(pluginPlus.epAuthId)) ? party.getProperty(pluginPlus.epAuthId) : party.getId().toString();

        if (!partyId.equalsIgnoreCase(userId))
            throw new ForbiddenException("Datastream not linked to acting Party");

    }

    protected void assertOwnershipMultiDatastream(JooqPersistenceManager pm, Entity multiDatastream, Principal principal) {
        assertPrincipal(principal);

        if (multiDatastream == null)
            throw new IllegalArgumentException("MultiDatastream does not exist");

        if ((pluginMultiDatastream != null) && pluginMultiDatastream.isEnabled() && !multiDatastream.getEntityType().equals(pluginMultiDatastream.etMultiDatastream))
            throw new IllegalArgumentException("Entity not of type MultiDatastream");

        // We can get the username from the Principal
        String userId = principal.getName();

        // Ensure Ownership for MultiDatastream
        Entity party = null;

        if (multiDatastream != null)
            party = multiDatastream.getProperty(pluginPlus.npPartyMultiDatastream);

        if (party == null && multiDatastream.getId() != null) {
            multiDatastream = pm.get(pluginMultiDatastream.etMultiDatastream, multiDatastream.getId());
            if (multiDatastream != null) {
                party = multiDatastream.getProperty(pluginPlus.npPartyMultiDatastream);
            }
        }

        if (party == null)
            throw new IllegalArgumentException("MultiDatastream not linked to a Party");

        String partyId = (party.isSetProperty(pluginPlus.epAuthId)) ? party.getProperty(pluginPlus.epAuthId) : party.getId().toString();
        if (!partyId.equalsIgnoreCase(userId))
            throw new ForbiddenException("MultiDatastream not linked to acting Party");

    }

    protected void assertOwnershipThing(JooqPersistenceManager pm, Entity thing, Principal principal) {
        assertPrincipal(principal);

        if (thing == null)
            throw new IllegalArgumentException("Thing does not exist");

        if (!thing.getEntityType().equals(pluginCoreModel.etThing))
            throw new IllegalArgumentException("Entity not of type Thing");

        // We can get the username from the Principal
        String userId = principal.getName();

        // Ensure Ownership for Thing
        Entity party = null;

        if (thing != null)
            party = thing.getProperty(pluginPlus.npPartyThing);

        if (party == null && thing.getId() != null) {
            thing = pm.get(pluginPlus.etGroup, thing.getId());
            if (thing != null) {
                party = thing.getProperty(pluginPlus.npPartyThing);
            }
        }

        if (party == null)
            throw new IllegalArgumentException("Thing not linked to a Party");

        String partyId = (party.isSetProperty(pluginPlus.epAuthId)) ? party.getProperty(pluginPlus.epAuthId) : party.getId().toString();

        if (!partyId.equalsIgnoreCase(userId))
            throw new ForbiddenException("Thing not linked to acting Party");

    }

    protected void assertOwnershipProject(JooqPersistenceManager pm, Entity project, Principal principal) {
        assertPrincipal(principal);

        if (project == null)
            throw new IllegalArgumentException("Project does not exist");

        if (!project.getEntityType().equals(pluginPlus.etProject))
            throw new IllegalArgumentException("Entity not of type Project");

        // We can get the username from the Principal
        String userId = principal.getName();

        // Ensure Ownership for Group
        Entity party = null;

        if (project != null)
            party = project.getProperty(pluginPlus.npPartyProject);

        if (party == null && project.getId() != null) {
            project = pm.get(pluginPlus.etGroup, project.getId());
            if (project != null) {
                party = project.getProperty(pluginPlus.npPartyGroup);
            }
        }

        if (party == null)
            throw new IllegalArgumentException("Project not linked to a Party");

        String partyId = (party.isSetProperty(pluginPlus.epAuthId)) ? party.getProperty(pluginPlus.epAuthId) : party.getId().toString();

        if (!partyId.equalsIgnoreCase(userId))
            throw new ForbiddenException("Project not linked to acting Party");

    }

    protected void assertOwnershipGroup(JooqPersistenceManager pm, Entity group, Principal principal) {
        assertPrincipal(principal);

        if (group == null)
            throw new IllegalArgumentException("Group does not exist");

        if (!group.getEntityType().equals(pluginPlus.etGroup))
            throw new IllegalArgumentException("Entity not of type Group");

        // We can get the username from the Principal
        String userId = principal.getName();

        // Ensure Ownership for Group
        Entity party = group.getProperty(pluginPlus.npPartyGroup);
        if (party == null && group.getId() != null) {
            group = pm.get(pluginPlus.etGroup, group.getId());
            if (group != null) {
                party = group.getProperty(pluginPlus.npPartyGroup);
            }
        }

        if (party == null)
            throw new IllegalArgumentException("Group not linked to a Party");

        String partyId = (party.isSetProperty(pluginPlus.epAuthId)) ? party.getProperty(pluginPlus.epAuthId) : party.getId().toString();

        if (!partyId.equalsIgnoreCase(userId))
            throw new ForbiddenException("Group not linked to acting Party");

    }

    protected void assertOwnershipParty(Entity party, Principal principal) {
        assertPrincipal(principal);

        if (party == null)
            throw new IllegalArgumentException("Party does not exist");

        if (!party.getEntityType().equals(pluginPlus.etParty))
            throw new IllegalArgumentException("Entity not of type Party");

        // We can get the username from the Principal
        String userId = principal.getName();
        String partyId = party.getProperty(pluginPlus.epAuthId);
        if ((partyId != null) && (!userId.equalsIgnoreCase(partyId))) {
            // The authId is set by the plugin - it cannot be changed via a PATCH
            throw new ForbiddenException("Party not representing acting user");
        } else {
            partyId = party.getId().toString();
            if ((partyId != null) && (!userId.equalsIgnoreCase(partyId))) {
                // The authId is set by the plugin - it cannot be changed via a PATCH
                throw new ForbiddenException("Party not representing acting user");
            }
        }
    }

    protected void assertLicenseGroup(JooqPersistenceManager pm, Entity group) {
        if (group == null)
            throw new IllegalArgumentException("Group does not exist");

        if (!group.getEntityType().equals(pluginPlus.etGroup))
            throw new IllegalArgumentException("Entity not of type Group");

        if (group.isSetProperty(pluginPlus.npLicenseGroup)) {
            // The Datastream has License inline
            return;
        }

        // Ensure License for Group
        Entity license = group.getProperty(pluginPlus.npLicenseGroup);
        if (license == null && group.getId() != null) {
            group = pm.get(pluginPlus.etGroup, group.getId());
            if (group != null) {
                license = group.getProperty(pluginPlus.npLicenseGroup);
            }
        }
        if (license == null)
            throw new IllegalArgumentException("Group not linked to a License");

    }

    protected void assertLicenseProject(JooqPersistenceManager pm, Entity project) {
        if (project == null)
            throw new IllegalArgumentException("Project does not exist");

        if (!project.getEntityType().equals(pluginPlus.etProject))
            throw new IllegalArgumentException("Entity not of type Project");

        if (project.isSetProperty(pluginPlus.npLicenseProject)) {
            // The Datastream has License inline
            return;
        }

        // Ensure License for Project
        Entity license = project.getProperty(pluginPlus.npLicenseProject);
        if (license == null && project.getId() != null) {
            project = pm.get(pluginPlus.etLicense, project.getId());
            if (project != null) {
                license = project.getProperty(pluginPlus.npLicenseProject);
            }
        }
        if (license == null)
            throw new IllegalArgumentException("Project not linked to a License");

    }

    protected void assertLicenseDatastream(JooqPersistenceManager pm, Entity datastream) {
        if (datastream == null)
            throw new IllegalArgumentException("Datastream does not exist");

        if (!datastream.getEntityType().equals(pluginCoreModel.etDatastream))
            throw new IllegalArgumentException("Entity not of type Datastream");

        if (datastream.isSetProperty(pluginPlus.npLicenseDatastream)) {
            // The Datastream has License inline
            return;
        }

        // Ensure License for Datastream
        Entity license = datastream.getProperty(pluginPlus.npLicenseDatastream);
        if (license == null && datastream.getId() != null) {
            datastream = pm.get(pluginCoreModel.etDatastream, datastream.getId());
            if (datastream != null) {
                license = datastream.getProperty(pluginPlus.npLicenseDatastream);
            }
        }

        if (license == null)
            throw new IllegalArgumentException("Datastream not linked to a License");

    }

    protected void assertLicenseMultiDatastream(JooqPersistenceManager pm, Entity multiDatastream) {
        if (multiDatastream == null)
            throw new IllegalArgumentException("MultiDatastream does not exist");

        if ((pluginMultiDatastream != null) && pluginMultiDatastream.isEnabled() && !multiDatastream.getEntityType().equals(pluginMultiDatastream.etMultiDatastream))
            throw new IllegalArgumentException("Entity not of type MultiDatastream");

        if (multiDatastream.isSetProperty(pluginPlus.npLicenseMultiDatastream)) {
            // The MultiDatastream has License inline
            return;
        }

        // Ensure License for MultiDatastream
        Entity license = multiDatastream.getProperty(pluginPlus.npLicenseMultiDatastream);
        if (license == null && multiDatastream.getId() != null) {
            multiDatastream = pm.get(pluginMultiDatastream.etMultiDatastream, multiDatastream.getId());
            if (multiDatastream != null) {
                license = multiDatastream.getProperty(pluginPlus.npLicenseMultiDatastream);
            }
        }

        if (license == null)
            throw new IllegalArgumentException("MultiDatastream not linked to a License");

    }

    protected void assertEmptyDatastream(JooqPersistenceManager pm, Entity datastream) {
        if (datastream == null)
            throw new IllegalArgumentException("Datastream does not exist");

        if (!datastream.getEntityType().equals(pluginCoreModel.etDatastream))
            throw new IllegalArgumentException("Entity not of type Datastream");

        if (datastream.isSetProperty(pluginCoreModel.npObservationsDatastream)) {
            // Observations are inline and part of creating a Datastream from scratch -> That's OK
            return;
        }

        // Ensure Datastream by reference has no Observations
        if (datastream.getId() != null) {
            Id id = ParserUtils.idFromObject(datastream.getId());
            ResourcePath rp = PathParser.parsePath(pm.getCoreSettings().getModelRegistry(), pm.getCoreSettings().getQueryDefaults().getServiceRootUrl(), Version.V_1_1, "/Datastreams(" + id.getUrl() + ")/Observations");
            Query query = QueryParser.parseQuery("", pm.getCoreSettings(), rp);
            query.validate();
            EntitySet obs = (EntitySet) pm.get(rp, query);
            if (!obs.isEmpty()) {
                throw new IllegalArgumentException("Referenced Datastream already contains observations.");
            }
        }
    }

    protected void assertEmptyMultiDatastream(JooqPersistenceManager pm, Entity mds) {
        if (mds == null)
            throw new IllegalArgumentException("MultiDatastream does not exist");

        if (!mds.getEntityType().equals(pluginMultiDatastream.etMultiDatastream))
            throw new IllegalArgumentException("Entity not of type MultiDatastream");

        if (mds.isSetProperty(pluginMultiDatastream.npObservationsMDs)) {
            // Observations are inline and part of creating a MultiDatastream from scratch -> That's OK
            return;
        }

        // Ensure Datastream by reference has no Observations
        if (mds.getId() != null) {
            Id id = ParserUtils.idFromObject(mds.getId());
            ResourcePath rp = PathParser.parsePath(pm.getCoreSettings().getModelRegistry(), pm.getCoreSettings().getQueryDefaults().getServiceRootUrl(), Version.V_1_1, "/MultiDatastreams(" + id.getUrl() + ")/Observations");
            Query query = QueryParser.parseQuery("", pm.getCoreSettings(), rp);
            query.validate();
            EntitySet obs = (EntitySet) pm.get(rp, query);
            if (!obs.isEmpty()) {
                throw new IllegalArgumentException("Referenced MultiDatastream already contains observations.");
            }
        }
    }

    protected void assertEmptyGroup(JooqPersistenceManager pm, Entity group) {
        if (group == null)
            throw new IllegalArgumentException("Group does not exist");

        if (!group.getEntityType().equals(pluginPlus.etGroup))
            throw new IllegalArgumentException("Entity not of type Group");

        if (group.isSetProperty(pluginPlus.npObservationGroups)) {
            // Observations are inline and part of creating a Datastream from scratch -> That's OK
            return;
        }

        // Ensure Group by reference has no Observations
        if (group.getId() != null) {
            Id id = ParserUtils.idFromObject(group.getId());
            ResourcePath rp = PathParser.parsePath(pm.getCoreSettings().getModelRegistry(), pm.getCoreSettings().getQueryDefaults().getServiceRootUrl(), Version.V_1_1, "/Groups(" + id.getUrl() + ")/Observations");
            Query query = QueryParser.parseQuery("", pm.getCoreSettings(), rp);
            query.validate();
            EntitySet obs = (EntitySet) pm.get(rp, query);
            if (!obs.isEmpty()) {
                throw new IllegalArgumentException("Referenced Group already contains observations.");
            }
        }
    }

    protected void assertEmptyProject(JooqPersistenceManager pm, Entity project) {
        if (project == null)
            throw new IllegalArgumentException("Project does not exist");

        if (!project.getEntityType().equals(pluginPlus.etProject))
            throw new IllegalArgumentException("Entity not of type Project");

        // Ensure Project by reference has no Datastreams and no MultiDatastreams
        if (project.getId() != null) {
            Id id = ParserUtils.idFromObject(project.getId());
            ResourcePath rp = PathParser.parsePath(pm.getCoreSettings().getModelRegistry(), pm.getCoreSettings().getQueryDefaults().getServiceRootUrl(), Version.V_1_1, "/Projects(" + id.getUrl() + ")");
            Query query = QueryParser.parseQuery("$expand=Datastreams($top=0;$count=true),MultiDatastreams($top=0;$count=true)", pm.getCoreSettings(), rp);
            query.validate();
            project = (Entity) pm.get(rp, query);
            if (project.getProperty(pluginPlus.npDatastreamsProject).getCount() != 0) {
                throw new IllegalArgumentException("Referenced Project already contains Datastream(s).");
            }
            if (project.getProperty(pluginPlus.npMultiDatastreamsProject).getCount() != 0) {
                throw new IllegalArgumentException("Referenced Project already contains MultiDatastream(s).");
            }
        }
    }
}
