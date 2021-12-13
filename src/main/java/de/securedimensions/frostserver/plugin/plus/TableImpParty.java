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

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

import org.jooq.DataType;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.TableField;
import org.jooq.TableLike;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fraunhofer.iosb.ilt.frostserver.model.EntityType;
import de.fraunhofer.iosb.ilt.frostserver.model.core.Entity;
import de.fraunhofer.iosb.ilt.frostserver.model.core.EntitySet;
import de.fraunhofer.iosb.ilt.frostserver.model.core.IdUuid;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.PostgresPersistenceManager;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.factories.EntityFactories;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.factories.HookPreDelete;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.factories.HookPreInsert;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.factories.HookPreUpdate;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.relations.RelationOneToMany;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.tables.StaTableAbstract;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.tables.TableCollection;
import de.fraunhofer.iosb.ilt.frostserver.plugin.coremodel.PluginCoreModel;
import de.fraunhofer.iosb.ilt.frostserver.plugin.coremodel.TableImpDatastreams;
import de.fraunhofer.iosb.ilt.frostserver.plugin.coremodel.TableImpLocations;
import de.fraunhofer.iosb.ilt.frostserver.plugin.coremodel.TableImpThings;
import de.fraunhofer.iosb.ilt.frostserver.plugin.multidatastream.PluginMultiDatastream;
import de.fraunhofer.iosb.ilt.frostserver.plugin.multidatastream.TableImpMultiDatastreams;
import de.fraunhofer.iosb.ilt.frostserver.service.ServiceRequest;
import de.fraunhofer.iosb.ilt.frostserver.util.ParserUtils;
import de.fraunhofer.iosb.ilt.frostserver.util.PrincipalExtended;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.ForbiddenException;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.IncompleteEntityException;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.NoSuchEntityException;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.UnauthorizedException;
/**
 *
 * @author am
 * @author scf
 */
public class TableImpParty extends StaTableAbstract<TableImpParty> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TableImpParty.class.getName());

    private static final long serialVersionUID = 1620371673;

    /**
     * The column <code>public.PARTIES.EP_DESCRIPTION</code>.
     */
    public final TableField<Record, String> colDescription = createField(DSL.name("DESCRIPTION"), SQLDataType.CLOB, this);

    /**
     * The column <code>public.PARTIES.EP_NAME</code>.
     */
    public final TableField<Record, String> colName = createField(DSL.name("NAME"), SQLDataType.CLOB.defaultValue(DSL.field("'no name'::text", SQLDataType.CLOB)), this);

    /**
     * The column <code>public.PARTIES.EP_AUTHID</code>.
     */
    public final TableField<Record, String> colAuthId = createField(DSL.name("AUTHID"), SQLDataType.CLOB, this);

    /**
     * The column <code>public.PARTIES.EP_NICKNAME</code>.
     */
    public final TableField<Record, String> colNickName = createField(DSL.name("NICKNAME"), SQLDataType.CLOB, this);

    /**
     * The column <code>public.PARTIES.EP_ROLE</code>.
     */
    public final TableField<Record, String> colRole = createField(DSL.name("ROLE"), SQLDataType.CLOB, this);

    /**
     * The column <code>public.PARTIES.EP_ID</code>.
     */
    public final TableField<Record, ?> colId = createField(DSL.name("ID"), getIdType(), this);

    private final PluginPLUS pluginPLUS;
    private final PluginCoreModel pluginCoreModel;
    private final PluginMultiDatastream pluginMultiDatastream;

    /**
     * Create a <code>public.PARTIES</code> table reference.
     *
     * @param idType The (SQL)DataType of the Id columns used in the actual
     * database.
     * @param pluginParty the party plugin this table belongs to.
     * @param pluginCoreModel the coreModel plugin that this data model links
     * to.
     */
    public TableImpParty(DataType<?> idType, PluginPLUS pluginParty, PluginCoreModel pluginCoreModel, PluginMultiDatastream pluginMultiDatastream) {
        super(idType, DSL.name("PARTIES"), null);
        this.pluginPLUS = pluginParty;
        this.pluginCoreModel = pluginCoreModel;
        this.pluginMultiDatastream = pluginMultiDatastream;
    }

    private TableImpParty(Name alias, TableImpParty aliased, PluginPLUS pluginParty, PluginCoreModel pluginCoreModel, PluginMultiDatastream pluginMultiDatastream) {
        super(aliased.getIdType(), alias, aliased);
        this.pluginPLUS = pluginParty;
        this.pluginCoreModel = pluginCoreModel;
        this.pluginMultiDatastream = pluginMultiDatastream;
    }

    @Override
    public void initRelations() {
        final TableCollection tables = getTables();

        TableImpDatastreams tableDatastreams = tables.getTableForClass(TableImpDatastreams.class);
        TableImpGroups tableGroups = tables.getTableForClass(TableImpGroups.class);
        TableImpThings tableThings = tables.getTableForClass(TableImpThings.class);

        // We add relation to the Things table
        registerRelation(new RelationOneToMany<>(pluginPLUS.npThingsParty, this, tableThings)
                .setSourceFieldAccessor(TableImpParty::getId)
                .setTargetFieldAccessor(table -> (TableField<Record, ?>) table.field(tableThings.indexOf("PARTY_ID")))
        );

        // We add the relation to us from the Things table.
        tableThings.registerRelation(new RelationOneToMany<>(pluginPLUS.npPartyThing, tableThings, this)
                .setSourceFieldAccessor(table -> (TableField<Record, ?>) table.field(tableThings.indexOf("PARTY_ID")))
                .setTargetFieldAccessor(TableImpParty::getId)
        );

        // We add relation to the Groups table
        registerRelation(new RelationOneToMany<>(pluginPLUS.npGroupsParty, this, tableGroups)
                .setSourceFieldAccessor(TableImpParty::getId)
                .setTargetFieldAccessor(table -> (TableField<Record, ?>) table.field(tableGroups.indexOf("PARTY_ID")))
        );

        // We add the relation to us from the Things table.
        tableGroups.registerRelation(new RelationOneToMany<>(pluginPLUS.npPartyGroup, tableGroups, this)
                .setSourceFieldAccessor(table -> (TableField<Record, ?>) table.field(tableGroups.indexOf("PARTY_ID")))
                .setTargetFieldAccessor(TableImpParty::getId)
        );

        // We add relation to the Datstreams table
        registerRelation(new RelationOneToMany<>(pluginPLUS.npDatastreamsParty, this, tableDatastreams)
                .setSourceFieldAccessor(TableImpParty::getId)
                .setTargetFieldAccessor(table -> (TableField<Record, ?>) table.field(tableDatastreams.indexOf("PARTY_ID")))
        );

        // We add the relation to us from the Datastreams table.
        tableDatastreams.registerRelation(new RelationOneToMany<>(pluginPLUS.npPartyDatastream, tableDatastreams, this)
                .setSourceFieldAccessor(table -> (TableField<Record, ?>) table.field(tableDatastreams.indexOf("PARTY_ID")))
                .setTargetFieldAccessor(TableImpParty::getId)
        );

        TableImpMultiDatastreams tableMultiDatastreams = tables.getTableForClass(TableImpMultiDatastreams.class);
        if (tableMultiDatastreams != null) {
            final int partyMDIdIdx = tableMultiDatastreams.indexOf("PARTY_ID");
            registerRelation(new RelationOneToMany<>(pluginPLUS.npMultiDatastreamsParty, this, tableMultiDatastreams)
                    .setSourceFieldAccessor(TableImpParty::getId)
                    .setTargetFieldAccessor(table -> (TableField<Record, ?>) table.field(partyMDIdIdx))
            );

            // We add the relation to us to the MultiDatastreams table.
            tableMultiDatastreams.registerRelation(new RelationOneToMany<>(pluginPLUS.npPartyMultiDatastream, tableMultiDatastreams, this)
                    .setSourceFieldAccessor(table -> (TableField<Record, ?>) table.field(partyMDIdIdx))
                    .setTargetFieldAccessor(TableImpParty::getId)
            );
        }
    }

    @Override
    public void initProperties(final EntityFactories entityFactories) {
        final TableCollection tables = getTables();
        pfReg.addEntryId(entityFactories, TableImpParty::getId);
        pfReg.addEntryString(pluginCoreModel.epName, table -> table.colName);
        pfReg.addEntryString(pluginCoreModel.epDescription, table -> table.colDescription);
        pfReg.addEntryString(pluginPLUS.epAuthId, table -> table.colAuthId);
        pfReg.addEntryString(pluginPLUS.epNickName, table -> table.colNickName);
        pfReg.addEntrySimple(pluginPLUS.epPartyRole, table -> table.colRole);

        // We register a navigationProperty on the Things table.
        pfReg.addEntry(pluginPLUS.npThingsParty, TableImpParty::getId, entityFactories);

        // We register a navigationProperty on the Groups table.
        pfReg.addEntry(pluginPLUS.npGroupsParty, TableImpParty::getId, entityFactories);

        // We register a navigationProperty on the Datastreams table.
        pfReg.addEntry(pluginPLUS.npDatastreamsParty, TableImpParty::getId, entityFactories);

        // We register a navigationProperty on the MultiDatastreams table.
        pfReg.addEntry(pluginPLUS.npMultiDatastreamsParty, TableImpParty::getId, entityFactories);

        TableImpParty tableParties = tables.getTableForClass(TableImpParty.class);
        
        tableParties.registerHookPreInsert(-10.0, new HookPreInsert() {

			@Override
			public boolean insertIntoDatabase(PostgresPersistenceManager pm, Entity entity,
					Map<Field, Object> insertFields) throws NoSuchEntityException, IncompleteEntityException {

            	if (pluginPLUS.isEnforceOwnershipEnabled() == false)
            	{
            		// test if the authId is set
            		if (!entity.isSetProperty(pluginPLUS.epAuthId))
            			return true;
            		
            		// make sure that the Party's iot.id is equal to the authId
            		entity.setId(new IdUuid(entity.getProperty(pluginPLUS.epAuthId)));
            		// If the Party already exist, we can skip processing
            		if (pm.get(pluginPLUS.etParty, entity.getId()) == null)
            			return true;
            		else
            			return false;
            	}
            	
            	Principal principal = ServiceRequest.LOCAL_REQUEST.get().getUserPrincipal();
        		
            	if (isAdmin(principal))
            	{
            		// The admin has extra rights
                	entity.setId(new IdUuid(entity.getProperty(pluginPLUS.epAuthId)));
                	Entity party = (Entity)pm.get(getEntityType(), entity.getId());
    				if (party != null)
    				{
    					// No need to insert the entity as it already exists. Just return the Id of the existing Party
    					return false;
    				}
    				return true;
            	}
            	
            	// We have a username available from the Principal
        		assertPrincipal(principal);
        		String userId = principal.getName();
             		
            	try
        		{
        		    // This throws exception if userId is not in UUID format
        			UUID.fromString(userId);
        		} 
            	catch (IllegalArgumentException exception)
        		{
            		// generate the UUID from the userId
        			userId = UUID.nameUUIDFromBytes(userId.getBytes()).toString();
        		}
            	
            	if ((entity.isSetProperty(pluginPLUS.epAuthId)) && (!userId.equalsIgnoreCase(entity.getProperty((pluginPLUS.epAuthId)))))
            	{
            		// The authId is set by this plugin - it cannot be set via POSTed Party property authId
            		throw new IllegalArgumentException("Party property authId cannot be set");                   	
        		}
            	
            	entity.setProperty(pluginPLUS.epAuthId, userId);
        		entity.setId(new IdUuid(userId));
        		Entity party = (Entity)pm.get(getEntityType(), entity.getId());
				if (party != null)
				{
					// No need to insert the entity as it already exists. Just return the Id of the existing Party
					return false;
				}
            	
				return true;					
				
			}
		});
        
        
        tableParties.registerHookPreUpdate(-10.0, new HookPreUpdate() {

			@Override
			public void updateInDatabase(PostgresPersistenceManager pm, Entity entity, Object entityId)
					throws NoSuchEntityException, IncompleteEntityException {
				
            	if (pluginPLUS.isEnforceOwnershipEnabled() != true)
            		return;
            	
        		Principal principal = ServiceRequest.LOCAL_REQUEST.get().getUserPrincipal();


            	if (isAdmin(principal))
            		return;
            	
            	// We have a username available from the Principal
        		assertPrincipal(principal);
            	String userId = principal.getName();
             		
            	try
        		{
        		    // This throws exception if userId is not in UUID format
        			UUID.fromString(userId);
        		} 
            	catch (IllegalArgumentException exception)
        		{
            		// generate the UUID from the userId
        			userId = UUID.nameUUIDFromBytes(userId.getBytes()).toString();
        		}
            	
            	if (!userId.equalsIgnoreCase(entity.getId().toString()))
            	{
            		// The authId is set by this plugin - it cannot be set via POSTed Party property authId
            		throw new ForbiddenException("Cannot update existing Party of another user");                   	
        		}

            	entity.setProperty(pluginPLUS.epAuthId, userId);
        		entity.setId(new IdUuid(userId));
			} 
		});
        
        tableParties.registerHookPreDelete(-10.0, new HookPreDelete() {

			@Override
			public void delete(PostgresPersistenceManager pm, Object entityId) throws NoSuchEntityException {

            	if (pluginPLUS.isEnforceOwnershipEnabled() != true)
            		return;
            	
        		Principal principal = ServiceRequest.LOCAL_REQUEST.get().getUserPrincipal();

            	if (isAdmin(principal))
            		return;
            	
				throw new ForbiddenException("Deleting Party is not allowed");
			} 
		});
        
        TableImpGroups tableGroups = tables.getTableForClass(TableImpGroups.class);
        final int partyGroupsIdIdx = tableGroups.registerField(DSL.name("PARTY_ID"), getIdType());
        tableGroups.getPropertyFieldRegistry()
        .addEntry(pluginPLUS.npPartyGroup, table -> (TableField<Record, ?>) table.field(partyGroupsIdIdx), entityFactories);

        tableGroups.registerHookPreInsert(-10.0, new HookPreInsert() {

			@Override
			public boolean insertIntoDatabase(PostgresPersistenceManager pm, Entity entity,
					Map<Field, Object> insertFields) throws NoSuchEntityException, IncompleteEntityException {

            	if (pluginPLUS.isEnforceOwnershipEnabled() != true)
            		return true;
            	
        		Principal principal = ServiceRequest.LOCAL_REQUEST.get().getUserPrincipal();

            	if (isAdmin(principal))
            		return true;
            	
            	// We have a username available from the Principal
            	String userId = principal.getName();

            	Entity thisGroup = (Entity)pm.get(pluginPLUS.etGroup, entity.getId());
        		assertPrincipal(principal);
        		if (thisGroup.isSetProperty(pluginPLUS.npPartyGroup))
            	{
            		Entity partyOnGroup = thisGroup.getProperty(pluginPLUS.npPartyGroup);
                	if (!partyOnGroup.getId().toString().equalsIgnoreCase(userId))
                		throw new IllegalArgumentException("Group not linked to acting Party"); 
                	
            	}
            	else if (entity.isSetProperty(pluginPLUS.npPartyGroup))
            	{
            		// The request contains a party to be linked to this Group. It needs to represent the acting user!
            		if (!entity.getProperty(pluginPLUS.npPartyGroup).getId().toString().equalsIgnoreCase(userId))
            			throw new ForbiddenException("Group cannot be linked to a Party that doesn't represent the acting user");
            	}
            		
            	
            	return true;
			}
        });
        
        tableGroups.registerHookPreUpdate(-10.0, new HookPreUpdate() {

			@Override
			public void updateInDatabase(PostgresPersistenceManager pm, Entity entity, Object entityId)
					throws NoSuchEntityException, IncompleteEntityException {
				
            	if (pluginPLUS.isEnforceOwnershipEnabled() != true)
            		return;
            	
        		Principal principal = ServiceRequest.LOCAL_REQUEST.get().getUserPrincipal();

            	if (isAdmin(principal))
            		return;
            	
            	// We have a username available from the Principal
        		assertPrincipal(principal);
            	String userId = principal.getName();
             		
            	Entity thisGroup = (Entity)pm.get(pluginPLUS.etGroup, entity.getId());
            	
        		if (thisGroup.isSetProperty(pluginPLUS.npPartyGroup))
        		{
        			// This Group is linked to a Party
                	Entity partyOnGroup = thisGroup.getProperty(pluginPLUS.npPartyGroup);
        			// The Datastream to patch has a Party associated
                	if (!partyOnGroup.getId().toString().equalsIgnoreCase(userId))
                		throw new IllegalArgumentException("Group not linked to acting Party"); 

                	if (entity.isSetProperty(pluginPLUS.npPartyGroup)) 
                	{
                		// The PATCH request tries to update the Party
                		Entity partyOnRequest = entity.getProperty(pluginPLUS.npPartyGroup);
                		
            			// =>If the request tries to link to the acting Party simply return
            			if (partyOnRequest.getId().toString().equalsIgnoreCase(userId))
            				return;
            			
            			// The request tries to link to another party - transfer of ownership
                		if (pluginPLUS.isTransferOwnershipEnabled() == false)
                			throw new ForbiddenException("Transfer of ownership to Party " + userId + " is not allowed");

                	}

                	// Update of all other properties can happen without checking, as the Datasream is linked to the acting party

        		}
        		else
        		{
        			// This Group is not yet linked to a Party
        			if (entity.isSetProperty(pluginPLUS.npPartyGroup))
        			{
                		// The PATCH request tries to update the Party
                		Entity partyOnRequest = entity.getProperty(pluginPLUS.npPartyGroup);
                		
            			// The Group to update has no Party associated.
            			// => We can only link to that Party which is representing the acting user
            			if (!partyOnRequest.getId().toString().equalsIgnoreCase(userId))
            				throw new IllegalArgumentException("Group can only be linked to the Party representing the acting user"); 
        			}
        			else
        			{
	        			// The PATCH request tries to update other properties
	                	// The Group is NOT associated to a Party, so updating any other properties is forbidden
	        			throw new ForbiddenException("Group not linked to a Party"); 
        			}
        		}
            		            	

			} 
		});

        tableGroups.registerHookPreDelete(-10.0, new HookPreDelete() {

			@Override
			public void delete(PostgresPersistenceManager pm, Object entityId) throws NoSuchEntityException {

            	if (pluginPLUS.isEnforceOwnershipEnabled() != true)
            		return;
            	
        		Principal principal = ServiceRequest.LOCAL_REQUEST.get().getUserPrincipal();

            	if (isAdmin(principal))
            		return;
            	
            	// We have a username available from the Principal
        		assertPrincipal(principal);
            	String userId = principal.getName();
            	
            	Entity thisGroup = (Entity)pm.get(pluginPLUS.etGroup, ParserUtils.idFromObject((entityId)));
            	if (thisGroup.isSetProperty(pluginPLUS.npPartyGroup))
            	{
            		Entity partyOnGroup = thisGroup.getProperty(pluginPLUS.npPartyGroup);
                	if (!partyOnGroup.getId().toString().equalsIgnoreCase(userId))
                		throw new IllegalArgumentException("Group not linked to acting Party"); 
                	
            	}
            	else
            		throw new ForbiddenException("Group not linked to a Party");
			} 
		});

        
        TableImpThings tableThings = tables.getTableForClass(TableImpThings.class);
        final int partyThingsIdIdx = tableThings.registerField(DSL.name("PARTY_ID"), getIdType());
        tableThings.getPropertyFieldRegistry()
        	.addEntry(pluginPLUS.npPartyThing, table -> (TableField<Record, ?>) table.field(partyThingsIdIdx), entityFactories);
         
        tableThings.registerHookPreUpdate(-10.0, new HookPreUpdate() {

			@Override
			public void updateInDatabase(PostgresPersistenceManager pm, Entity entity, Object entityId)
					throws NoSuchEntityException, IncompleteEntityException {
				
        		Principal principal = ServiceRequest.LOCAL_REQUEST.get().getUserPrincipal();

            	// We have a username available from the Principal
        		assertPrincipal(principal);
            	String userId = principal.getName();
             		
            	Entity thisThing = (Entity)pm.get(pluginCoreModel.etThing, entity.getId());
            	
        		if (thisThing.isSetProperty(pluginPLUS.npPartyThing))
        		{
        			// This thing is linked to a Party
                	Entity partyOnThing = thisThing.getProperty(pluginPLUS.npPartyThing);
        			// The thing to patch has a Party associated
                	if (!partyOnThing.getId().toString().equalsIgnoreCase(userId))
                		throw new IllegalArgumentException("Thing not associated with acting Party"); 

                	if (entity.isSetProperty(pluginPLUS.npPartyThing)) 
                	{
                		// The PATCH request tries to update the Party
                		Entity partyOnRequest = entity.getProperty(pluginPLUS.npPartyThing);
                		
            			// =>If the request tries to link to the acting Party simply return
            			if (partyOnRequest.getId().toString().equalsIgnoreCase(userId))
            				return;
            			
            			// The request tries to link to another party - transfer of ownership
                		if (pluginPLUS.isTransferOwnershipEnabled() == false)
                			throw new ForbiddenException("Transfer of ownership to Party " + userId + " is not allowed");

                	}

                	// Update of all other properties can happen without checking, as the thing is linked to the acting party

        		}
        		else
        		{
        			// This thing is not yet associated with a Party
        			if (entity.isSetProperty(pluginPLUS.npPartyThing))
        			{
                		// The PATCH request tries to update the Party
                		Entity partyOnRequest = entity.getProperty(pluginPLUS.npPartyThing);
                		
            			// The Thing to update has no Party associated.
            			// => We can only link to that Party which is representing the acting user
            			if (!partyOnRequest.getId().toString().equalsIgnoreCase(userId))
            				throw new IllegalArgumentException("Thing can only be associated with the Party representing the acting user"); 
        			}
        			else
        			{
	        			// The PATCH request tries to update other properties
	                	// The Thing is NOT associated with a Party, so updating any other properties is forbidden
	        			throw new ForbiddenException("Thing not associated with a Party"); 
        			}
        		}
            		            	

			} 
		});

        tableThings.registerHookPreDelete(-10.0, new HookPreDelete() {

			@Override
			public void delete(PostgresPersistenceManager pm, Object entityId) throws NoSuchEntityException {

            	if (pluginPLUS.isEnforceOwnershipEnabled() != true)
            		return;
            	
        		Principal principal = ServiceRequest.LOCAL_REQUEST.get().getUserPrincipal();


            	if (isAdmin(principal))
            		return;
            	
            	// We have a username available from the Principal
            	String userId = principal.getName();
            	
            	Entity thisThing = (Entity)pm.get(pluginCoreModel.etThing, ParserUtils.idFromObject((entityId)));
            	if (thisThing.isSetProperty(pluginPLUS.npPartyThing))
            	{
            		Entity partyOnThing = thisThing.getProperty(pluginPLUS.npPartyThing);
                	if (!partyOnThing.getId().toString().equalsIgnoreCase(userId))
                		throw new IllegalArgumentException("Thing not linked to acting Party"); 
                	
            	}
            	else
            		throw new ForbiddenException("Thing not linked to a Party");
			} 
		});
        
        TableImpDatastreams tableDatastreams = tables.getTableForClass(TableImpDatastreams.class);
        final int partyDatastreamsIdIdx = tableDatastreams.registerField(DSL.name("PARTY_ID"), getIdType());
        tableDatastreams.getPropertyFieldRegistry()
        .addEntry(pluginPLUS.npPartyDatastream, table -> (TableField<Record, ?>) table.field(partyDatastreamsIdIdx), entityFactories);
        
        tableDatastreams.registerHookPreInsert(-10.0, new HookPreInsert() {

			@Override
			public boolean insertIntoDatabase(PostgresPersistenceManager pm, Entity entity,
					Map<Field, Object> insertFields) throws NoSuchEntityException, IncompleteEntityException {

            	if (pluginPLUS.isEnforceOwnershipEnabled() != true)
            		return true;
            	
        		Principal principal = ServiceRequest.LOCAL_REQUEST.get().getUserPrincipal();

            	if (isAdmin(principal))
            		return true;
            	
            	// We have a username available from the Principal
        		assertPrincipal(principal);
            	String userId = principal.getName();

            	Entity thisDatastream = (Entity)pm.get(pluginCoreModel.etDatastream, entity.getId());
            	if (thisDatastream.isSetProperty(pluginPLUS.npPartyDatastream))
            	{
            		Entity partyOnThing = thisDatastream.getProperty(pluginPLUS.npPartyDatastream);
                	if (!partyOnThing.getId().toString().equalsIgnoreCase(userId))
                		throw new IllegalArgumentException("Datastream not linked to acting Party"); 
                	
            	}
            	else if (entity.isSetProperty(pluginPLUS.npPartyDatastream))
            	{
            		// The request contains a party to be linked to this Datastream. It needs to represent the acting user!
            		if (!entity.getProperty(pluginPLUS.npPartyDatastream).getId().toString().equalsIgnoreCase(userId))
            			throw new ForbiddenException("Datastrea, cannot be linked to a Party that doesn't represent the acting user");
            	}
            		
            	
            	return true;
			}
        });
        
        tableDatastreams.registerHookPreUpdate(-10.0, new HookPreUpdate() {

			@Override
			public void updateInDatabase(PostgresPersistenceManager pm, Entity entity, Object entityId)
					throws NoSuchEntityException, IncompleteEntityException {
				
            	if (pluginPLUS.isEnforceOwnershipEnabled() != true)
            		return;
            	
        		Principal principal = ServiceRequest.LOCAL_REQUEST.get().getUserPrincipal();

            	if (isAdmin(principal))
            		return;
            	
            	// We have a username available from the Principal
        		assertPrincipal(principal);
            	String userId = principal.getName();
             		
            	Entity thisDatastream = (Entity)pm.get(pluginCoreModel.etDatastream, entity.getId());
            	
        		if (thisDatastream.isSetProperty(pluginPLUS.npPartyDatastream))
        		{
        			// This Datastream is linked to a Party
                	Entity partyOnDatastream = thisDatastream.getProperty(pluginPLUS.npPartyDatastream);
        			// The Datastream to patch has a Party associated
                	if (!partyOnDatastream.getId().toString().equalsIgnoreCase(userId))
                		throw new IllegalArgumentException("Datastream not linked to acting Party"); 

                	if (entity.isSetProperty(pluginPLUS.npPartyDatastream)) 
                	{
                		// The PATCH request tries to update the Party
                		Entity partyOnRequest = entity.getProperty(pluginPLUS.npPartyDatastream);
                		
            			// =>If the request tries to link to the acting Party simply return
            			if (partyOnRequest.getId().toString().equalsIgnoreCase(userId))
            				return;
            			
            			// The request tries to link to another party - transfer of ownership
                		if (pluginPLUS.isTransferOwnershipEnabled() == false)
                			throw new ForbiddenException("Transfer of ownership to Party " + userId + " is not allowed");

                	}

                	// Update of all other properties can happen without checking, as the Datasream is linked to the acting party

        		}
        		else
        		{
        			// This Datastream is not yet linked to a Party
        			if (entity.isSetProperty(pluginPLUS.npPartyDatastream))
        			{
                		// The PATCH request tries to update the Party
                		Entity partyOnRequest = entity.getProperty(pluginPLUS.npPartyDatastream);
                		
            			// The Datastream to update has no Party associated.
            			// => We can only link to that Party which is representing the acting user
            			if (!partyOnRequest.getId().toString().equalsIgnoreCase(userId))
            				throw new IllegalArgumentException("Datastream can only be linked to the Party representing the acting user"); 
        			}
        			else
        			{
	        			// The PATCH request tries to update other properties
	                	// The Datastream is NOT associated to a Party, so updating any other properties is forbidden
	        			throw new ForbiddenException("Datastream not linked to a Party"); 
        			}
        		}
            		            	

			} 
		});

        tableDatastreams.registerHookPreDelete(-10.0, new HookPreDelete() {

			@Override
			public void delete(PostgresPersistenceManager pm, Object entityId) throws NoSuchEntityException {

            	if (pluginPLUS.isEnforceOwnershipEnabled() != true)
            		return;
            	
        		Principal principal = ServiceRequest.LOCAL_REQUEST.get().getUserPrincipal();

            	if (isAdmin(principal))
            		return;
            	
            	// We have a username available from the Principal
        		assertPrincipal(principal);
            	String userId = principal.getName();
            	
            	Entity thisDatastream = (Entity)pm.get(pluginCoreModel.etDatastream, ParserUtils.idFromObject((entityId)));
            	if (thisDatastream.isSetProperty(pluginPLUS.npPartyDatastream))
            	{
            		Entity partyOnDatastream = thisDatastream.getProperty(pluginPLUS.npPartyDatastream);
                	if (!partyOnDatastream.getId().toString().equalsIgnoreCase(userId))
                		throw new IllegalArgumentException("Datastream not linked to acting Party"); 
                	
            	}
            	else
            		throw new ForbiddenException("Datastream not linked to a Party");
			} 
		});

        TableImpMultiDatastreams tableMultiDatastreams = tables.getTableForClass(TableImpMultiDatastreams.class);
        if (tableMultiDatastreams != null) {
            final int partyMDIdIdx = tableMultiDatastreams.registerField(DSL.name("PARTY_ID"), getIdType());
            tableMultiDatastreams.getPropertyFieldRegistry()
                    .addEntry(pluginPLUS.npPartyMultiDatastream, table -> (TableField<Record, ?>) ((TableLike<Record>) table).field(partyMDIdIdx), entityFactories);
            
            tableMultiDatastreams.registerHookPreInsert(-10.0, new HookPreInsert() {

    			@Override
    			public boolean insertIntoDatabase(PostgresPersistenceManager pm, Entity entity,
    					Map<Field, Object> insertFields) throws NoSuchEntityException, IncompleteEntityException {

                	if (pluginPLUS.isEnforceOwnershipEnabled() != true)
                		return true;
                	
            		Principal principal = ServiceRequest.LOCAL_REQUEST.get().getUserPrincipal();

                	if (isAdmin(principal))
                		return true;
                	
                	// We have a username available from the Principal
            		assertPrincipal(principal);
                	String userId = principal.getName();

                	Entity thisMultiDatastream = (Entity)pm.get(pluginMultiDatastream.etMultiDatastream, entity.getId());
                	if (thisMultiDatastream.isSetProperty(pluginPLUS.npPartyMultiDatastream))
                	{
                		Entity partyOnMultiDatastream = thisMultiDatastream.getProperty(pluginPLUS.npPartyMultiDatastream);
                    	if (!partyOnMultiDatastream.getId().toString().equalsIgnoreCase(userId))
                    		throw new IllegalArgumentException("MultiDatastream not linked to acting Party"); 
                    	
                	}
                	else if (entity.isSetProperty(pluginPLUS.npPartyMultiDatastream))
                	{
                		// The request contains a party to be linked to this MultiDatastream. It needs to represent the acting user!
                		if (!entity.getProperty(pluginPLUS.npPartyMultiDatastream).getId().toString().equalsIgnoreCase(userId))
                			throw new ForbiddenException("MultiDatastream, cannot be linked to a Party that doesn't represent the acting user");
                	}
                		
                	
                	return true;
    			}
            });
            
            tableMultiDatastreams.registerHookPreUpdate(-10.0, new HookPreUpdate() {

    			@Override
    			public void updateInDatabase(PostgresPersistenceManager pm, Entity entity, Object entityId)
    					throws NoSuchEntityException, IncompleteEntityException {
    				
                	if (pluginPLUS.isEnforceOwnershipEnabled() != true)
                		return;
                	
            		Principal principal = ServiceRequest.LOCAL_REQUEST.get().getUserPrincipal();

                	if (isAdmin(principal))
                		return;
                	
                	// We have a username available from the Principal
            		assertPrincipal(principal);
                	String userId = principal.getName();
                 		
                	Entity thisMultiDatastream = (Entity)pm.get(pluginMultiDatastream.etMultiDatastream, entity.getId());
                	
            		if (thisMultiDatastream.isSetProperty(pluginPLUS.npPartyMultiDatastream))
            		{
            			// This MultiDatastream is linked to a Party
                    	Entity partyOnMultiDatastream = thisMultiDatastream.getProperty(pluginPLUS.npPartyMultiDatastream);
            			// The MultiDatastream to patch has a Party associated
                    	if (!partyOnMultiDatastream.getId().toString().equalsIgnoreCase(userId))
                    		throw new IllegalArgumentException("MultiDatastream not linked to acting Party"); 

                    	if (entity.isSetProperty(pluginPLUS.npPartyMultiDatastream)) 
                    	{
                    		// The PATCH request tries to update the Party
                    		Entity partyOnRequest = entity.getProperty(pluginPLUS.npPartyMultiDatastream);
                    		
                			// =>If the request tries to link to the acting Party simply return
                			if (partyOnRequest.getId().toString().equalsIgnoreCase(userId))
                				return;
                			
                			// The request tries to link to another party - transfer of ownership
                    		if (pluginPLUS.isTransferOwnershipEnabled() == false)
                    			throw new ForbiddenException("Transfer of ownership to Party " + userId + " is not allowed");

                    	}

                    	// Update of all other properties can happen without checking, as the Datasream is linked to the acting party

            		}
            		else
            		{
            			// This MultiDatastream is not yet linked to a Party
            			if (entity.isSetProperty(pluginPLUS.npPartyMultiDatastream))
            			{
                    		// The PATCH request tries to update the Party
                    		Entity partyOnRequest = entity.getProperty(pluginPLUS.npPartyMultiDatastream);
                    		
                			// The MultiDatastream to update has no Party associated.
                			// => We can only link to that Party which is representing the acting user
                			if (!partyOnRequest.getId().toString().equalsIgnoreCase(userId))
                				throw new IllegalArgumentException("MultiDatastream can only be linked to the Party representing the acting user"); 
            			}
            			else
            			{
    	        			// The PATCH request tries to update other properties
    	                	// The MultiDatastream is NOT associated to a Party, so updating any other properties is forbidden
    	        			throw new ForbiddenException("MultiDatastream not linked to a Party"); 
            			}
            		}
                		            	

    			} 
    		});

            tableMultiDatastreams.registerHookPreDelete(-10.0, new HookPreDelete() {

    			@Override
    			public void delete(PostgresPersistenceManager pm, Object entityId) throws NoSuchEntityException {

                	if (pluginPLUS.isEnforceOwnershipEnabled() != true)
                		return;
                	
            		Principal principal = ServiceRequest.LOCAL_REQUEST.get().getUserPrincipal();

                	if (isAdmin(principal))
                		return;
                	
                	// We have a username available from the Principal
            		assertPrincipal(principal);
                	String userId = principal.getName();
                	
                	Entity thisMultiDatastream = (Entity)pm.get(pluginMultiDatastream.etMultiDatastream, ParserUtils.idFromObject((entityId)));
                	if (thisMultiDatastream.isSetProperty(pluginPLUS.npPartyMultiDatastream))
                	{
                		Entity partyOnMultiDatastream = thisMultiDatastream.getProperty(pluginPLUS.npPartyMultiDatastream);
                    	if (!partyOnMultiDatastream.getId().toString().equalsIgnoreCase(userId))
                    		throw new IllegalArgumentException("MultiDatastream not linked to acting Party"); 
                    	
                	}
                	else
                		throw new ForbiddenException("MultiDatastream not linked to a Party");
    			} 
    		});
 
        }

        TableImpLocations tableLocations = tables.getTableForClass(TableImpLocations.class);
        tableLocations.registerHookPreInsert(-10.0, new HookPreInsert() {

			@Override
			public boolean insertIntoDatabase(PostgresPersistenceManager pm, Entity entity,
					Map<Field, Object> insertFields) throws NoSuchEntityException, IncompleteEntityException {

            	if (pluginPLUS.isEnforceOwnershipEnabled() != true)
            		return true;
            	
        		Principal principal = ServiceRequest.LOCAL_REQUEST.get().getUserPrincipal();

            	if (isAdmin(principal))
            		return true;
            	
            	// We have a username available from the Principal
        		assertPrincipal(principal);
            	String userId = principal.getName();

            	EntitySet things = entity.getProperty(pluginCoreModel.npThingsLocation);
            	if ((things == null) || (things.getCount() > 1))
            		throw new IncompleteEntityException("Cannot check ownership of Location for more than one Thing");
            		
            	Entity thing = (Entity)pm.get(pluginCoreModel.etThing, things.iterator().next().getId());
            	Entity party = thing.getProperty(pluginPLUS.npPartyThing);
            	
            	if (!party.getId().toString().equalsIgnoreCase(userId))
            		throw new ForbiddenException("Location cannot be linked to Thing because not linked to acting Party"); 
            	
            	return true;
			}
        });

        tableLocations.registerHookPreUpdate(-10.0, new HookPreUpdate() {

			@Override
			public void updateInDatabase(PostgresPersistenceManager pm, Entity entity, Object entityId)
					throws NoSuchEntityException, IncompleteEntityException {
				
            	if (pluginPLUS.isEnforceOwnershipEnabled() == false)
            		return;
            	
            	throw new ForbiddenException("Location cannot be updated");
			}
        });

        tableLocations.registerHookPreDelete(-10.0, new HookPreDelete() {

			@Override
			public void delete(PostgresPersistenceManager pm, Object entityId) throws NoSuchEntityException {

            	if (pluginPLUS.isEnforceOwnershipEnabled() == false)
            		return;
            	
            	throw new ForbiddenException("Location cannot be Deleted");
			}
        });
}
    
    @Override
    public EntityType getEntityType() {
        return pluginPLUS.etParty;
    }

    @Override
    public TableField<Record, ?> getId() {
        return colId;
    }

    @Override
    public TableImpParty as(Name alias) {
        return new TableImpParty(alias, this, pluginPLUS, pluginCoreModel, pluginMultiDatastream).initCustomFields();
    }

    @Override
    public TableImpParty getThis() {
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
}
