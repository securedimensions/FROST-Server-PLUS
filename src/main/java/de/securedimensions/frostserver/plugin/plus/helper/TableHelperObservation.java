package de.securedimensions.frostserver.plugin.plus.helper;

import java.security.Principal;
import java.util.Map;

import org.jooq.DataType;
import org.jooq.Field;

import de.fraunhofer.iosb.ilt.frostserver.model.core.Entity;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.PostgresPersistenceManager;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.factories.EntityFactories;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.factories.HookPreDelete;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.factories.HookPreInsert;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.factories.HookPreUpdate;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.tables.TableCollection;
import de.fraunhofer.iosb.ilt.frostserver.plugin.coremodel.PluginCoreModel;
import de.fraunhofer.iosb.ilt.frostserver.plugin.coremodel.TableImpObservations;
import de.fraunhofer.iosb.ilt.frostserver.plugin.coremodel.TableImpThings;
import de.fraunhofer.iosb.ilt.frostserver.plugin.multidatastream.PluginMultiDatastream;
import de.fraunhofer.iosb.ilt.frostserver.service.ServiceRequest;
import de.fraunhofer.iosb.ilt.frostserver.settings.CoreSettings;
import de.fraunhofer.iosb.ilt.frostserver.util.ParserUtils;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.IncompleteEntityException;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.NoSuchEntityException;
import de.securedimensions.frostserver.plugin.plus.PluginPLUS;

public class TableHelperObservation extends TableHelper {

	private TableImpObservations tableObservations;
	
	public TableHelperObservation(CoreSettings settings, PostgresPersistenceManager ppm) 
	{
		super(settings, ppm);
		
		this.tableObservations = tables.getTableForClass(TableImpObservations.class);
		
		
	}

	@Override
	public void registerPreHooks() {

        tableObservations.registerHookPreInsert(-10.0, new HookPreInsert() {

			@Override
			public boolean insertIntoDatabase(PostgresPersistenceManager pm, Entity entity,
					Map<Field, Object> insertFields) throws NoSuchEntityException, IncompleteEntityException {

            	if (pluginPlus.isEnforceOwnershipEnabled() == false)
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
				
            	if (pluginPlus.isEnforceOwnershipEnabled() == false)
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

            	if (pluginPlus.isEnforceOwnershipEnabled() == false)
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

	   private void assertOwnershipObservation(PostgresPersistenceManager pm, Entity entity, Principal principal)
	    {

		   Entity datastream = null;
		   if (entity.isSetProperty(pluginCoreModel.npDatastreamObservation))
			   datastream = entity.getProperty(pluginCoreModel.npDatastreamObservation);
		   else
		   {
			   Entity observation = (Entity)pm.get(pluginCoreModel.etObservation, entity.getId());
			   if ((observation != null)  && observation.isSetProperty(pluginCoreModel.npDatastreamObservation))
				   datastream = (Entity)pm.get(pluginCoreModel.etDatastream, observation.getProperty(pluginCoreModel.npDatastreamObservation).getId());
			   else
				   datastream = null;
		   }
		   
		   if (datastream != null)
			   if (datastream.isSetProperty(pluginPlus.npPartyDatastream))
				   assertOwnershipDatastream(datastream, principal);
			   else
				   assertOwnershipDatastream((Entity)pm.get(pluginCoreModel.etDatastream,datastream.getId()), principal);
	       
		   if (pluginMultiDatastream != null)
		   {
		   
			   Entity multiDatastream = null;
			   if (entity.isSetProperty(pluginMultiDatastream.npMultiDatastreamObservation))
				   multiDatastream = entity.getProperty(pluginMultiDatastream.npMultiDatastreamObservation);
			   else
			   {
				   Entity observation = (Entity)pm.get(pluginCoreModel.etObservation, entity.getId());
				   if ((observation != null)  && observation.isSetProperty(pluginMultiDatastream.npMultiDatastreamObservation))
					   multiDatastream = (Entity)pm.get(pluginMultiDatastream.etMultiDatastream, observation.getProperty(pluginMultiDatastream.npMultiDatastreamObservation).getId());
				   else
					   multiDatastream = null;
			   }
	
			   if (multiDatastream != null)
				   if (multiDatastream.isSetProperty(pluginPlus.npPartyMultiDatastream))
					   assertOwnershipMultiDatastream(multiDatastream, principal);
				   else
					   assertOwnershipMultiDatastream((Entity)pm.get(pluginMultiDatastream.etMultiDatastream, multiDatastream.getId()), principal);
		   
		   }
		   
	    }

}
