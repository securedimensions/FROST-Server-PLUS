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
import de.fraunhofer.iosb.ilt.frostserver.model.core.IdUuid;
import de.fraunhofer.iosb.ilt.frostserver.parser.path.PathParser;
import de.fraunhofer.iosb.ilt.frostserver.parser.query.ExpressionParser;
import de.fraunhofer.iosb.ilt.frostserver.parser.query.Node;
import de.fraunhofer.iosb.ilt.frostserver.parser.query.QueryParser;
import de.fraunhofer.iosb.ilt.frostserver.path.ResourcePath;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.PostgresPersistenceManager;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.bindings.JsonBinding;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.bindings.JsonValue;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.factories.EntityFactories;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.factories.HookPreDelete;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.factories.HookPreInsert;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.factories.HookPreUpdate;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.relations.RelationOneToMany;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.tables.StaMainTable;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.tables.StaTableAbstract;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.tables.TableCollection;
import de.fraunhofer.iosb.ilt.frostserver.plugin.coremodel.PluginCoreModel;
import de.fraunhofer.iosb.ilt.frostserver.plugin.coremodel.TableImpDatastreams;
import de.fraunhofer.iosb.ilt.frostserver.plugin.coremodel.TableImpThings;
import de.fraunhofer.iosb.ilt.frostserver.plugin.multidatastream.TableImpMultiDatastreams;
import de.fraunhofer.iosb.ilt.frostserver.query.Query;
import de.fraunhofer.iosb.ilt.frostserver.util.PrincipalExtended;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.ForbiddenException;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.IncompleteEntityException;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.NoSuchEntityException;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.UnauthorizedException;

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
import org.jooq.impl.DefaultDataType;
import org.jooq.impl.SQLDataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.Expression;
import de.fraunhofer.iosb.ilt.frostserver.service.ServiceRequest;
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

    /**
     * Create a <code>public.PARTIES</code> table reference.
     *
     * @param idType The (SQL)DataType of the Id columns used in the actual
     * database.
     * @param pluginParty the party plugin this table belongs to.
     * @param pluginCoreModel the coreModel plugin that this data model links
     * to.
     */
    public TableImpParty(DataType<?> idType, PluginPLUS pluginParty, PluginCoreModel pluginCoreModel) {
        super(idType, DSL.name("PARTIES"), null);
        this.pluginPLUS = pluginParty;
        this.pluginCoreModel = pluginCoreModel;
    }

    private TableImpParty(Name alias, TableImpParty aliased, PluginPLUS pluginParty, PluginCoreModel pluginCoreModel) {
        super(aliased.getIdType(), alias, aliased);
        this.pluginPLUS = pluginParty;
        this.pluginCoreModel = pluginCoreModel;
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

        TableImpParty tableParty = tables.getTableForClass(TableImpParty.class);
        
        tableParty.registerHookPreInsert(-10.0, new HookPreInsert() {

			@Override
			public boolean insertIntoDatabase(PostgresPersistenceManager pm, Entity entity,
					Map<Field, Object> insertFields) throws NoSuchEntityException, IncompleteEntityException {

				ServiceRequest request = ServiceRequest.LOCAL_REQUEST.get();
            	Principal principal = request.getUserPrincipal();
            	
            	if (principal == null)
            		throw new UnauthorizedException("Cannot create Party - no user identified");
            	
				
            	if ((principal instanceof PrincipalExtended) && ((PrincipalExtended)principal).isAdmin())
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
        
        
        tableParty.registerHookPreUpdate(-10.0, new HookPreUpdate() {

			@Override
			public void updateInDatabase(PostgresPersistenceManager pm, Entity entity, Object entityId)
					throws NoSuchEntityException, IncompleteEntityException {
				
				ServiceRequest request = ServiceRequest.LOCAL_REQUEST.get();
            	Principal principal = request.getUserPrincipal();
            	
            	if (principal == null)
            		throw new UnauthorizedException("Cannot update existing Party - no user identified");
            	
            	if ((principal instanceof PrincipalExtended) && ((PrincipalExtended)principal).isAdmin())
            		return;
            	
            	// We have a username available from the Principal
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
        
        tableParty.registerHookPreDelete(-10.0, new HookPreDelete() {

			@Override
			public void delete(PostgresPersistenceManager pm, Object entityId) throws NoSuchEntityException {
				ServiceRequest request = ServiceRequest.LOCAL_REQUEST.get();
            	Principal principal = request.getUserPrincipal();
            	
            	if (principal == null)
            		throw new UnauthorizedException("Cannot delete Party - no user identified");
            	
				
            	if ((principal instanceof PrincipalExtended) && ((PrincipalExtended)principal).isAdmin())
            		return;
            	
				throw new ForbiddenException("Deleting Party is not allowed");
			} 
		});
        
        TableImpGroups tableGroups = tables.getTableForClass(TableImpGroups.class);
        TableImpThings tableThings = tables.getTableForClass(TableImpThings.class);
        TableImpDatastreams tableDatastreams = tables.getTableForClass(TableImpDatastreams.class);

        final int partyThingsIdIdx = tableThings.registerField(DSL.name("PARTY_ID"), getIdType());
        final int partyGroupsIdIdx = tableGroups.registerField(DSL.name("PARTY_ID"), getIdType());
        final int partyDatastreamsIdIdx = tableDatastreams.registerField(DSL.name("PARTY_ID"), getIdType());
        
        tableThings.getPropertyFieldRegistry()
        .addEntry(pluginPLUS.npPartyThing, table -> (TableField<Record, ?>) table.field(partyThingsIdIdx), entityFactories);

        tableGroups.getPropertyFieldRegistry()
        .addEntry(pluginPLUS.npPartyGroup, table -> (TableField<Record, ?>) table.field(partyGroupsIdIdx), entityFactories);

        tableDatastreams.getPropertyFieldRegistry()
        .addEntry(pluginPLUS.npPartyDatastream, table -> (TableField<Record, ?>) table.field(partyDatastreamsIdIdx), entityFactories);

        TableImpMultiDatastreams tableMultiDatastreams = tables.getTableForClass(TableImpMultiDatastreams.class);
        if (tableMultiDatastreams != null) {
            final int partyMDIdIdx = tableMultiDatastreams.registerField(DSL.name("PARTY_ID"), getIdType());
            tableMultiDatastreams.getPropertyFieldRegistry()
                    .addEntry(pluginPLUS.npPartyMultiDatastream, table -> (TableField<Record, ?>) ((TableLike<Record>) table).field(partyMDIdIdx), entityFactories);
        }

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
        return new TableImpParty(alias, this, pluginPLUS, pluginCoreModel).initCustomFields();
    }

    @Override
    public TableImpParty getThis() {
        return this;
    }

}
