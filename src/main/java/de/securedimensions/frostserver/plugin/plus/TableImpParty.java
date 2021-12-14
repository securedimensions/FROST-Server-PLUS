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
import de.fraunhofer.iosb.ilt.frostserver.plugin.coremodel.TableImpObservations;
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
     * The column <code>public.PARTIES.EP_DISPLAYNAME</code>.
     */
    public final TableField<Record, String> colDisplayName = createField(DSL.name("DISPLAYNAME"), SQLDataType.CLOB, this);

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
        pfReg.addEntryString(pluginPLUS.epDisplayName, table -> table.colDisplayName);
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

        TableImpThings tableThings = tables.getTableForClass(TableImpThings.class);
        final int partyThingsIdIdx = tableThings.registerField(DSL.name("PARTY_ID"), getIdType());
        tableThings.getPropertyFieldRegistry()
        	.addEntry(pluginPLUS.npPartyThing, table -> (TableField<Record, ?>) table.field(partyThingsIdIdx), entityFactories);
         
        tableThings.registerHookPreUpdate(-10.0, new HookPreUpdate() {

			@Override
			public void updateInDatabase(PostgresPersistenceManager pm, Entity entity, Object entityId)
					throws NoSuchEntityException, IncompleteEntityException {
				
        		Principal principal = ServiceRequest.LOCAL_REQUEST.get().getUserPrincipal();

        		
            	if (isAdmin(principal))
            		return;

            	Entity thing = (Entity)pm.get(pluginCoreModel.etThing, ParserUtils.idFromObject((entityId)));
            	assertOwnershipThing(thing, principal);

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
            	
            	Entity thing = (Entity)pm.get(pluginCoreModel.etThing, ParserUtils.idFromObject((entityId)));
            	assertOwnershipThing(thing, principal);
			} 
		});
        
        TableImpDatastreams tableDatastreams = tables.getTableForClass(TableImpDatastreams.class);
        final int partyDatastreamsIdIdx = tableDatastreams.registerField(DSL.name("PARTY_ID"), getIdType());
        tableDatastreams.getPropertyFieldRegistry()
        .addEntry(pluginPLUS.npPartyDatastream, table -> (TableField<Record, ?>) table.field(partyDatastreamsIdIdx), entityFactories);
        
        tableDatastreams.registerHookPreUpdate(-10.0, new HookPreUpdate() {

 			@Override
 			public void updateInDatabase(PostgresPersistenceManager pm, Entity entity, Object entityId)
 					throws NoSuchEntityException, IncompleteEntityException {
 				
         		Principal principal = ServiceRequest.LOCAL_REQUEST.get().getUserPrincipal();

             	if (isAdmin(principal))
             		return;
              		
             	Entity datastream = (Entity)pm.get(pluginCoreModel.etDatastream, entity.getId());
             	assertOwnershipDatastream(datastream, principal);             		            	

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
             	
            	Entity datastream = (Entity)pm.get(pluginCoreModel.etDatastream, ParserUtils.idFromObject((entityId)));
            	assertOwnershipDatastream(datastream, principal);
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
                	
                	Entity multiDatastream = (Entity)pm.get(pluginMultiDatastream.etMultiDatastream, entity.getId());
                	assertOwnershipMultiDatastream(multiDatastream, principal);

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
                	
                	Entity multiDatastream = (Entity)pm.get(pluginMultiDatastream.etMultiDatastream, entity.getId());
                	assertOwnershipMultiDatastream(multiDatastream, principal);
                
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
                	               	
                	Entity multiDatastream = (Entity)pm.get(pluginMultiDatastream.etMultiDatastream, ParserUtils.idFromObject((entityId)));
                	assertOwnershipMultiDatastream(multiDatastream, principal);
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
            	
            	assertOwnershipLocation(pm, entity, principal);
            	
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
            	
            	throw new ForbiddenException("Location cannot be deleted");
			}
        });

        TableImpObservations tableObservations = tables.getTableForClass(TableImpObservations.class);
        tableObservations.registerHookPreInsert(-10.0, new HookPreInsert() {

			@Override
			public boolean insertIntoDatabase(PostgresPersistenceManager pm, Entity entity,
					Map<Field, Object> insertFields) throws NoSuchEntityException, IncompleteEntityException {

            	if (pluginPLUS.isEnforceOwnershipEnabled() == false)
            		return true;
            	
        		Principal principal = ServiceRequest.LOCAL_REQUEST.get().getUserPrincipal();

            	if (isAdmin(principal))
            		return true;
            	
            	assertOwnershipObservation(pm, entity, principal);
            	
            	return true;
            	
			}
        });

        tableObservations.registerHookPreUpdate(-10.0, new HookPreUpdate() {

			@Override
			public void updateInDatabase(PostgresPersistenceManager pm, Entity entity, Object entityId)
					throws NoSuchEntityException, IncompleteEntityException {
				
            	if (pluginPLUS.isEnforceOwnershipEnabled() == false)
            		return;
            	
        		Principal principal = ServiceRequest.LOCAL_REQUEST.get().getUserPrincipal();

            	if (isAdmin(principal))
            		return;
            	
            	assertOwnershipObservation(pm, entity, principal);

			}
        });

        tableObservations.registerHookPreDelete(-10.0, new HookPreDelete() {

			@Override
			public void delete(PostgresPersistenceManager pm, Object entityId) throws NoSuchEntityException {

            	if (pluginPLUS.isEnforceOwnershipEnabled() == false)
            		return;
            	
        		Principal principal = ServiceRequest.LOCAL_REQUEST.get().getUserPrincipal();

            	if (isAdmin(principal))
            		return;
            	
            	// The Observation from the DB contains the Datastream
            	Entity observation = (Entity)pm.get(pluginCoreModel.etObservation, ParserUtils.idFromObject((entityId)));
            	assertOwnershipObservation(pm, observation, principal);

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
    
    private void assertOwnershipObservation(PostgresPersistenceManager pm, Entity entity, Principal principal)
    {

        if (entity.isSetProperty(pluginCoreModel.npDatastreamObservation))
        {
        	Entity datastream = (Entity)pm.get(pluginCoreModel.etDatastream, entity.getProperty(pluginCoreModel.npDatastreamObservation).getId());
        	assertOwnershipDatastream(datastream, principal);
        }
        
        if ((pluginMultiDatastream != null) && pluginMultiDatastream.isEnabled() && entity.isSetProperty(pluginMultiDatastream.npMultiDatastreamObservation)) {
        	Entity multiDatastream = (Entity)pm.get(pluginMultiDatastream.etMultiDatastream, entity.getProperty(pluginMultiDatastream.npMultiDatastreamObservation).getId());
       		assertOwnershipMultiDatastream(multiDatastream, principal);
        }
    	
    }
 
    private void assertOwnershipLocation(PostgresPersistenceManager pm, Entity location, Principal principal) throws IncompleteEntityException
    {
    	EntitySet things = location.getProperty(pluginCoreModel.npThingsLocation);
    	if ((things == null) || (things.getCount() > 1))
    		throw new IncompleteEntityException("Cannot check ownership of Location for more than one Thing");
    		
    	Entity thing = (Entity)pm.get(pluginCoreModel.etThing, things.iterator().next().getId());
    	assertOwnershipThing(thing, principal);
    }
    
    private void assertOwnershipThing(Entity thing, Principal principal)
    {
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
    		party = thing.getProperty(pluginPLUS.npPartyThing);
    	
    	if (party == null)
    		throw new ForbiddenException("Thing not linked to a Party");
    	
    	if (!party.getId().toString().equalsIgnoreCase(userId))
    		throw new ForbiddenException("Thing not linked to acting Party"); 

    	
    }
    


    private void assertOwnershipDatastream(Entity datastream, Principal principal)
    {
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
    		party = datastream.getProperty(pluginPLUS.npPartyDatastream);
    	
    	if (party == null)
    		throw new ForbiddenException("Datastream not linked to a Party");
    	
    	if (!party.getId().toString().equalsIgnoreCase(userId))
    		throw new ForbiddenException("Datastream not linked to acting Party"); 

    	
    }
    
    private void assertOwnershipMultiDatastream(Entity multiDatastream, Principal principal)
    {
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
    		party = multiDatastream.getProperty(pluginPLUS.npPartyMultiDatastream);
    	
    	if (party == null)
    		throw new ForbiddenException("MultiDatastream not linked to a Party");
    	
    	if (!party.getId().toString().equalsIgnoreCase(userId))
    		throw new ForbiddenException("MultiDatastream not linked to acting Party"); 

    	
    }

}
