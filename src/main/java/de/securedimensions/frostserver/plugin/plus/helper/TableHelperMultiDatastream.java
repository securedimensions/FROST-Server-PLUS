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

import org.jooq.DataType;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.TableField;
import org.jooq.TableLike;
import org.jooq.impl.DSL;

import de.fraunhofer.iosb.ilt.frostserver.model.core.Entity;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.PostgresPersistenceManager;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.factories.EntityFactories;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.factories.HookPreDelete;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.factories.HookPreInsert;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.factories.HookPreUpdate;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.tables.TableCollection;
import de.fraunhofer.iosb.ilt.frostserver.plugin.coremodel.PluginCoreModel;
import de.fraunhofer.iosb.ilt.frostserver.plugin.coremodel.TableImpLocations;
import de.fraunhofer.iosb.ilt.frostserver.plugin.multidatastream.PluginMultiDatastream;
import de.fraunhofer.iosb.ilt.frostserver.plugin.multidatastream.TableImpMultiDatastreams;
import de.fraunhofer.iosb.ilt.frostserver.service.ServiceRequest;
import de.fraunhofer.iosb.ilt.frostserver.settings.CoreSettings;
import de.fraunhofer.iosb.ilt.frostserver.util.ParserUtils;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.IncompleteEntityException;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.NoSuchEntityException;
import de.securedimensions.frostserver.plugin.plus.PluginPLUS;
import de.securedimensions.frostserver.plugin.plus.TableImpParty;

public class TableHelperMultiDatastream extends TableHelper {

	private TableImpMultiDatastreams tableMultiDatastreams;
	
	public TableHelperMultiDatastream(CoreSettings settings, PostgresPersistenceManager ppm) 
	{
		super(settings, ppm);

		this.tableMultiDatastreams = tables.getTableForClass(TableImpMultiDatastreams.class);
	}

	@Override
	public void registerPreHooks() {

        if (tableMultiDatastreams != null) {
            final int partyMDIdIdx = tableMultiDatastreams.registerField(DSL.name("PARTY_ID"), tables.getTableForClass(TableImpParty.class).getIdType());
            tableMultiDatastreams.getPropertyFieldRegistry()
                    .addEntry(pluginPlus.npPartyMultiDatastream, table -> (TableField<Record, ?>) ((TableLike<Record>) table).field(partyMDIdIdx));
            
            tableMultiDatastreams.registerHookPreInsert(-10.0, new HookPreInsert() {

    			@Override
    			public boolean insertIntoDatabase(PostgresPersistenceManager pm, Entity entity,
    					Map<Field, Object> insertFields) throws NoSuchEntityException, IncompleteEntityException {

                	if (pluginPlus.isEnforceOwnershipEnabled() != true)
                		return true;
                	
            		Principal principal = ServiceRequest.LOCAL_REQUEST.get().getUserPrincipal();

                	if (isAdmin(principal))
                		return true;
                	
                	assertOwnershipMultiDatastream(entity, principal);

                	return true;
    			}
            });
            
            tableMultiDatastreams.registerHookPreUpdate(-10.0, new HookPreUpdate() {

    			@Override
    			public void updateInDatabase(PostgresPersistenceManager pm, Entity entity, Object entityId)
    					throws NoSuchEntityException, IncompleteEntityException {
    				
                	if (pluginPlus.isEnforceOwnershipEnabled() != true)
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

                	if (pluginPlus.isEnforceOwnershipEnabled() != true)
                		return;
                	
            		Principal principal = ServiceRequest.LOCAL_REQUEST.get().getUserPrincipal();

                	if (isAdmin(principal))
                		return;
                	               	
                	Entity multiDatastream = (Entity)pm.get(pluginMultiDatastream.etMultiDatastream, ParserUtils.idFromObject((entityId)));
                	assertOwnershipMultiDatastream(multiDatastream, principal);
    			} 
    		});
 
        }

	}

}
