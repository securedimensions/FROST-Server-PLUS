/*
 * Copyright (C) 2021 Secure Dimensions GmbH, D-81377
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
package de.securedimensions.frostserver.plugin.plus;

import de.fraunhofer.iosb.ilt.frostserver.model.EntityType;
import de.fraunhofer.iosb.ilt.frostserver.model.ModelRegistry;
import de.fraunhofer.iosb.ilt.frostserver.model.core.Entity;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.PostgresPersistenceManager;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.bindings.JsonBinding;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.bindings.JsonValue;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.factories.EntityFactories;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.factories.HookPreDelete;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.factories.HookPreInsert;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.factories.HookPreUpdate;

import static de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.fieldwrapper.StaTimeIntervalWrapper.KEY_TIME_INTERVAL_END;
import static de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.fieldwrapper.StaTimeIntervalWrapper.KEY_TIME_INTERVAL_START;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.relations.RelationManyToMany;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.tables.StaTableAbstract;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.tables.TableCollection;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.utils.PropertyFieldRegistry.ConverterTimeInstant;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.utils.PropertyFieldRegistry.ConverterTimeInterval;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.utils.PropertyFieldRegistry.NFP;
import de.fraunhofer.iosb.ilt.frostserver.plugin.coremodel.PluginCoreModel;
import de.fraunhofer.iosb.ilt.frostserver.plugin.coremodel.TableImpObservations;
import de.fraunhofer.iosb.ilt.frostserver.service.ServiceRequest;
import de.fraunhofer.iosb.ilt.frostserver.util.ParserUtils;
import de.fraunhofer.iosb.ilt.frostserver.util.PrincipalExtended;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.ForbiddenException;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.IncompleteEntityException;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.NoSuchEntityException;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.UnauthorizedException;

import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.Map;

import org.jooq.DataType;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.TableField;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultDataType;
import org.jooq.impl.SQLDataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author am
 * @author scf
 */
public class TableImpGroups extends StaTableAbstract<TableImpGroups> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TableImpGroups.class.getName());

    private static final long serialVersionUID = 1626971256;

    /**
     * The column <code>public.GROUPS.EP_DESCRIPTION</code>.
     */
    public final TableField<Record, String> colDescription = createField(DSL.name("DESCRIPTION"), SQLDataType.CLOB, this);

    /**
     * The column <code>public.GROUPS.EP_NAME</code>.
     */
    public final TableField<Record, String> colName = createField(DSL.name("NAME"), SQLDataType.CLOB.defaultValue(DSL.field("'no name'::text", SQLDataType.CLOB)), this);

    /**
     * The column <code>public.GROUPS.EP_PROPERTIES</code>.
     */
    public final TableField<Record, JsonValue> colProperties = createField(DSL.name("PROPERTIES"), DefaultDataType.getDefaultDataType(TYPE_JSONB), this, "", new JsonBinding());

    /**
     * The column <code>public.GROUPS.EP_CLASSIFICATION</code>.
     */
    public final TableField<Record, String> colPurpose = createField(DSL.name("PURPOSE"), SQLDataType.CLOB, this);

    /**
     * The column <code>public.GROUPS.RUNTIME_START</code>.
     */
    public final TableField<Record, OffsetDateTime> colRuntimeTimeStart = createField(DSL.name("RUNTIME_START"), SQLDataType.TIMESTAMPWITHTIMEZONE, this);

    /**
     * The column <code>public.GROUPS.RUNTIME_END</code>.
     */
    public final TableField<Record, OffsetDateTime> colRuntimeTimeEnd = createField(DSL.name("RUNTIME_END"), SQLDataType.TIMESTAMPWITHTIMEZONE, this);

    /**
     * The column <code>public.GROUPS.CREATED_TIME</code>.
     */
    public final TableField<Record, OffsetDateTime> colCreatedTime = createField(DSL.name("CREATED"), SQLDataType.TIMESTAMPWITHTIMEZONE, this);

    /**
     * The column <code>public.GROUPS.EP_ID</code>.
     */
    public final TableField<Record, ?> colId = createField(DSL.name("ID"), getIdType(), this);

    private final PluginPLUS pluginPLUS;
    private final PluginCoreModel pluginCoreModel;

    /**
     * Create a <code>public.GROUPS</code> table reference.
     *
     * @param idType The (SQL)DataType of the Id columns used in the actual
     * database.
     * @param pluginProject the party plugin this table belongs to.
     * @param pluginCoreModel the coreModel plugin that this data model links
     * to.
     */
    public TableImpGroups(DataType<?> idType, PluginPLUS pluginPLUS, PluginCoreModel pluginCoreModel) {
        super(idType, DSL.name("GROUPS"), null);
        this.pluginPLUS = pluginPLUS;
        this.pluginCoreModel = pluginCoreModel;
    }

    private TableImpGroups(Name alias, TableImpGroups aliased, PluginPLUS pluginPLUS, PluginCoreModel pluginCoreModel) {
        super(aliased.getIdType(), alias, aliased);
        this.pluginPLUS = pluginPLUS;
        this.pluginCoreModel = pluginCoreModel;
    }

    @Override
    public void initRelations() {
        final TableCollection tables = getTables();

        initObservationGroups(tables);
        initObservationRelations(tables);
    }

    private void initObservationGroups(TableCollection tables) {
        final TableImpGroupsObservations tableGroupObservations = tables.getTableForClass(TableImpGroupsObservations.class);
        TableImpObservations tableObservations = tables.getTableForClass(TableImpObservations.class);

        registerRelation(new RelationManyToMany<>(pluginPLUS.npObservations, this, tableGroupObservations, tableObservations)
                .setSourceFieldAcc(TableImpGroups::getId)
                .setSourceLinkFieldAcc(TableImpGroupsObservations::getGroupId)
                .setTargetLinkFieldAcc(TableImpGroupsObservations::getObservationId)
                .setTargetFieldAcc(TableImpObservations::getId)
        );
        tableObservations.registerRelation(new RelationManyToMany<>(pluginPLUS.npObservationGroups, tableObservations, tableGroupObservations, this)
                .setSourceFieldAcc(TableImpObservations::getId)
                .setSourceLinkFieldAcc(TableImpGroupsObservations::getObservationId)
                .setTargetLinkFieldAcc(TableImpGroupsObservations::getGroupId)
                .setTargetFieldAcc(TableImpGroups::getId)
        );

    }

    private void initObservationRelations(TableCollection tables) {
        final TableImpGroupsRelations tableGroupRelations = tables.getTableForClass(TableImpGroupsRelations.class);
        TableImpRelations tableRelations = tables.getTableForClass(TableImpRelations.class);

        registerRelation(new RelationManyToMany<>(pluginPLUS.npRelations, this, tableGroupRelations, tableRelations)
                .setSourceFieldAcc(TableImpGroups::getId)
                .setSourceLinkFieldAcc(TableImpGroupsRelations::getGroupId)
                .setTargetLinkFieldAcc(TableImpGroupsRelations::getRelationId)
                .setTargetFieldAcc(TableImpRelations::getId)
        );
        tableRelations.registerRelation(new RelationManyToMany<>(pluginPLUS.npRelationGroups, tableRelations, tableGroupRelations, this)
                .setSourceFieldAcc(TableImpRelations::getId)
                .setSourceLinkFieldAcc(TableImpGroupsRelations::getRelationId)
                .setTargetLinkFieldAcc(TableImpGroupsRelations::getGroupId)
                .setTargetFieldAcc(TableImpGroups::getId)
        );

    }

    @Override
    public void initProperties(final EntityFactories entityFactories) {
        final TableCollection tables = getTables();
        pfReg.addEntryId(entityFactories, TableImpGroups::getId);
        pfReg.addEntryString(pluginCoreModel.epName, table -> table.colName);
        pfReg.addEntryString(pluginCoreModel.epDescription, table -> table.colDescription);
        pfReg.addEntryMap(ModelRegistry.EP_PROPERTIES, table -> table.colProperties);
        pfReg.addEntryString(pluginPLUS.epPurpose, table -> table.colPurpose);

        pfReg.addEntry(pluginPLUS.epGroupCreated, table -> table.colCreatedTime,
                new ConverterTimeInstant<>(pluginPLUS.epGroupCreated, table -> table.colCreatedTime));
        pfReg.addEntry(pluginPLUS.epGroupRuntime,
                new ConverterTimeInterval<>(pluginPLUS.epGroupRuntime, table -> table.colRuntimeTimeStart, table -> table.colRuntimeTimeEnd),
                new NFP<>(KEY_TIME_INTERVAL_START, table -> table.colRuntimeTimeStart),
                new NFP<>(KEY_TIME_INTERVAL_END, table -> table.colRuntimeTimeEnd));

        // Register with Observations
        pfReg.addEntry(pluginPLUS.npObservations, TableImpGroups::getId, entityFactories);

        TableImpObservations tableObservations = tables.getTableForClass(TableImpObservations.class);
        tableObservations.getPropertyFieldRegistry()
                .addEntry(pluginPLUS.npObservationGroups, TableImpObservations::getId, entityFactories);

        // Register with Relations
        pfReg.addEntry(pluginPLUS.npRelations, TableImpGroups::getId, entityFactories);

        TableImpRelations tableRelations = tables.getTableForClass(TableImpRelations.class);
        tableRelations.getPropertyFieldRegistry()
                .addEntry(pluginPLUS.npRelationGroups, TableImpRelations::getId, entityFactories);

        /*
        // Register with Party
        pfReg.addEntry(pluginPLUS.npParties, TableImpGroups::getId, entityFactories);

        TableImpParty tableParties = tables.getTableForClass(TableImpParty.class);
        tableParties.getPropertyFieldRegistry()
                .addEntry(pluginPLUS.npPartyGroups, TableImpParty::getId, entityFactories);
        */

        
        this.registerHookPreInsert(-10.0, new HookPreInsert() {

			@Override
			public boolean insertIntoDatabase(PostgresPersistenceManager pm, Entity entity,
					Map<Field, Object> insertFields) throws NoSuchEntityException, IncompleteEntityException {

            	if (pluginPLUS.isEnforceOwnershipEnabled() != true)
            		return true;
            	
        		Principal principal = ServiceRequest.LOCAL_REQUEST.get().getUserPrincipal();

            	if (isAdmin(principal))
            		return true;
            	
            	Entity party = entity.getProperty(pluginPLUS.npPartyGroup);
            	if (party != null)
            		assertOwnershipParty(party, principal);
            	            	
            	return true;
			}
        });
        
        this.registerHookPreUpdate(-10.0, new HookPreUpdate() {

			@Override
			public void updateInDatabase(PostgresPersistenceManager pm, Entity entity, Object entityId)
					throws NoSuchEntityException, IncompleteEntityException {
				
            	if (pluginPLUS.isEnforceOwnershipEnabled() != true)
            		return;
            	
        		Principal principal = ServiceRequest.LOCAL_REQUEST.get().getUserPrincipal();

            	if (isAdmin(principal))
            		return;
            	
            	Entity group = (Entity)pm.get(pluginPLUS.etGroup, ParserUtils.idFromObject((entityId)));
            	assertOwnershipGroup(group, principal);            		            	

			} 
		});

        this.registerHookPreDelete(-10.0, new HookPreDelete() {

			@Override
			public void delete(PostgresPersistenceManager pm, Object entityId) throws NoSuchEntityException {

            	if (pluginPLUS.isEnforceOwnershipEnabled() != true)
            		return;
            	
        		Principal principal = ServiceRequest.LOCAL_REQUEST.get().getUserPrincipal();

            	if (isAdmin(principal))
            		return;
            	
            	Entity group = (Entity)pm.get(pluginPLUS.etGroup, ParserUtils.idFromObject((entityId)));
            	assertOwnershipGroup(group, principal);
			} 
		});

    }

    @Override
    public EntityType getEntityType() {
        return pluginPLUS.etGroup;
    }

    @Override
    public TableField<Record, ?> getId() {
        return colId;
    }

    @Override
    public TableImpGroups as(Name alias) {
        return new TableImpGroups(alias, this, pluginPLUS, pluginCoreModel).initCustomFields();
    }

    @Override
    public TableImpGroups getThis() {
        return this;
    }

    private void assertPrincipal(Principal principal)
    {    	
    	if (principal == null)
    		throw new UnauthorizedException("No Principal");
    }
    
    private boolean isAdmin(Principal principal)
    {    	
    	if (principal == null)
    		throw new UnauthorizedException("Cannot create Party - no user identified");
    	
		
    	return ((principal instanceof PrincipalExtended) && ((PrincipalExtended)principal).isAdmin());
    }

    private void assertOwnershipGroup(Entity group, Principal principal)
    {
    	assertPrincipal(principal);
    	
    	if (group == null)
    		throw new IllegalArgumentException("Group does not exist");

    	if (!group.getEntityType().equals(pluginPLUS.etGroup))
    		throw new IllegalArgumentException("Entity not of type Group");
    		
    	// We can get the username from the Principal
		String userId = principal.getName();

    	// Ensure Ownership for Group
    	Entity party = null;
    	
    	if (group != null)
    		party = group.getProperty(pluginPLUS.npPartyGroup);
    	
    	if (party == null)
    		throw new ForbiddenException("Group not linked to a Party");
    	
    	if (!party.getId().toString().equalsIgnoreCase(userId))
    		throw new ForbiddenException("Group not linked to acting Party"); 
    	
    }

    private void assertOwnershipParty(Entity party, Principal principal)
    {
    	assertPrincipal(principal);
    	
    	if (party == null)
    		throw new IllegalArgumentException("Party does not exist");

    	if (!party.getEntityType().equals(pluginPLUS.etParty))
    		throw new IllegalArgumentException("Entity not of type Party");
    		
    	// We can get the username from the Principal
		String userId = principal.getName();
		if ((party.isSetProperty(pluginPLUS.epAuthId)) && (!userId.equalsIgnoreCase(party.getProperty(pluginPLUS.epAuthId))))
    	{
    		// The authId is set by the plugin - it cannot be changed via a PATCH
    		throw new ForbiddenException("Party not representing acting user");
    	}
    }
    
}
