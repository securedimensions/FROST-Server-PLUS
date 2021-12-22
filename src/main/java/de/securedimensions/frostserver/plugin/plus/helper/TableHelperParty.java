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
package de.securedimensions.frostserver.plugin.plus.helper;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

import org.jooq.Field;

import de.fraunhofer.iosb.ilt.frostserver.model.core.Entity;
import de.fraunhofer.iosb.ilt.frostserver.model.core.IdUuid;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.PostgresPersistenceManager;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.factories.HookPreDelete;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.factories.HookPreInsert;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.factories.HookPreUpdate;
import de.fraunhofer.iosb.ilt.frostserver.plugin.coremodel.TableImpDatastreams;
import de.fraunhofer.iosb.ilt.frostserver.service.ServiceRequest;
import de.fraunhofer.iosb.ilt.frostserver.settings.CoreSettings;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.ForbiddenException;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.IncompleteEntityException;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.NoSuchEntityException;
import de.securedimensions.frostserver.plugin.plus.TableImpParty;

public class TableHelperParty extends TableHelper {

	private TableImpParty tableParties;
	
	public TableHelperParty(CoreSettings settings, PostgresPersistenceManager ppm) {
		super(settings, ppm);

		this.tableParties = tables.getTableForClass(TableImpParty.class);
	}

	@Override
	public void registerPreHooks() {

	       tableParties.registerHookPreInsert(-10.0, new HookPreInsert() {

				@Override
				public boolean insertIntoDatabase(PostgresPersistenceManager pm, Entity entity,
						Map<Field, Object> insertFields) throws NoSuchEntityException, IncompleteEntityException {

	            	if (pluginPlus.isEnforceOwnershipEnabled() == false)
	            	{
	            		// test if the authId is set
	            		if (!entity.isSetProperty(pluginPlus.epAuthId))
	            			return true;
	            		
	            		// make sure that the Party's iot.id is equal to the authId
	            		entity.setId(new IdUuid(entity.getProperty(pluginPlus.epAuthId)));
	            		// If the Party already exist, we can skip processing
	            		if (pm.get(pluginPlus.etParty, entity.getId()) == null)
	            			return true;
	            		else
	            			return false;
	            	}
	            	
	            	Principal principal = ServiceRequest.LOCAL_REQUEST.get().getUserPrincipal();
	        		
	            	if (isAdmin(principal))
	            	{
	            		// The admin has extra rights
	                	entity.setId(new IdUuid(entity.getProperty(pluginPlus.epAuthId)));
	                	Entity party = (Entity)pm.get(pluginPlus.etParty, entity.getId());
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
	            	
	            	if ((entity.isSetProperty(pluginPlus.epAuthId)) && (!userId.equalsIgnoreCase(entity.getProperty((pluginPlus.epAuthId)))))
	            	{
	            		// The authId is set by this plugin - it cannot be set via POSTed Party property authId
	            		throw new IllegalArgumentException("Party property authId cannot be set");                   	
	        		}
	            	
	            	entity.setProperty(pluginPlus.epAuthId, userId);
	        		entity.setId(new IdUuid(userId));
	        		Entity party = (Entity)pm.get(pluginPlus.etParty, entity.getId());
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
					
	            	if (pluginPlus.isEnforceOwnershipEnabled() != true)
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

	            	entity.setProperty(pluginPlus.epAuthId, userId);
	        		entity.setId(new IdUuid(userId));
				} 
			});
	        
	        tableParties.registerHookPreDelete(-10.0, new HookPreDelete() {

				@Override
				public void delete(PostgresPersistenceManager pm, Object entityId) throws NoSuchEntityException {

	            	if (pluginPlus.isEnforceOwnershipEnabled() != true)
	            		return;
	            	
	        		Principal principal = ServiceRequest.LOCAL_REQUEST.get().getUserPrincipal();

	            	if (isAdmin(principal))
	            		return;
	            	
					throw new ForbiddenException("Deleting Party is not allowed");
				} 
			});

	}

}
