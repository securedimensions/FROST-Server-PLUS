package de.securedimensions.frostserver.plugin.plus.helper;

import java.io.IOException;
import java.security.Principal;

import org.jooq.DataType;
import org.jooq.Record;
import org.jooq.TableField;
import org.jooq.impl.DSL;

import de.fraunhofer.iosb.ilt.frostserver.model.core.Entity;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.PostgresPersistenceManager;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.factories.EntityFactories;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.factories.HookPreDelete;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.factories.HookPreUpdate;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.tables.TableCollection;
import de.fraunhofer.iosb.ilt.frostserver.plugin.coremodel.PluginCoreModel;
import de.fraunhofer.iosb.ilt.frostserver.plugin.coremodel.TableImpDatastreams;
import de.fraunhofer.iosb.ilt.frostserver.plugin.multidatastream.PluginMultiDatastream;
import de.fraunhofer.iosb.ilt.frostserver.service.ServiceRequest;
import de.fraunhofer.iosb.ilt.frostserver.settings.CoreSettings;
import de.fraunhofer.iosb.ilt.frostserver.util.ParserUtils;
import de.fraunhofer.iosb.ilt.frostserver.util.PrincipalExtended;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.ForbiddenException;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.IncompleteEntityException;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.NoSuchEntityException;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.UnauthorizedException;
import de.securedimensions.frostserver.plugin.plus.PluginPLUS;

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
	};
	
	protected TableHelper(CoreSettings settings, PostgresPersistenceManager ppm)
	{
		this.tables = ppm.getTableCollection();
		this.pluginPlus = settings.getPluginManager().getPlugin(PluginPLUS.class);
		this.pluginCoreModel = settings.getPluginManager().getPlugin(PluginCoreModel.class);
		this.pluginMultiDatastream = settings.getPluginManager().getPlugin(PluginMultiDatastream.class);
		this.entityFactories = ppm.getEntityFactories();
	}
	
	
    protected boolean isAdmin(Principal principal)
    {    	
    	if (principal == null)
    		return false;
		
    	return ((principal instanceof PrincipalExtended) && ((PrincipalExtended)principal).isAdmin());
    }

    protected void assertPrincipal(Principal principal)
    {    	
    	if (principal == null)
    		throw new UnauthorizedException("No Principal");
    }

    public abstract void registerPreHooks();
    
    protected void assertOwnershipDatastream(Entity datastream, Principal principal)
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
    		party = datastream.getProperty(pluginPlus.npPartyDatastream);
    	
    	if (party == null)
    		throw new IllegalArgumentException("Datastream not linked to a Party");
    	
    	String partyId = (party.isSetProperty(pluginPlus.epAuthId)) ? party.getProperty(pluginPlus.epAuthId).toString() : party.getId().toString();
    	
    	if (!partyId.equalsIgnoreCase(userId))
    		throw new ForbiddenException("Datastream not linked to acting Party"); 

    	
    }

    protected void assertOwnershipMultiDatastream(Entity multiDatastream, Principal principal)
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
    		party = multiDatastream.getProperty(pluginPlus.npPartyMultiDatastream);
    	
    	if (party == null)
    		throw new IllegalArgumentException("MultiDatastream not linked to a Party");
    	
    	String partyId = (party.isSetProperty(pluginPlus.epAuthId)) ? party.getProperty(pluginPlus.epAuthId).toString() : party.getId().toString();
    	if (!partyId.equalsIgnoreCase(userId))
    		throw new ForbiddenException("MultiDatastream not linked to acting Party"); 

    	
    }

    protected void assertOwnershipThing(Entity thing, Principal principal)
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
    		party = thing.getProperty(pluginPlus.npPartyThing);
    	
    	if (party == null)
    		throw new IllegalArgumentException("Thing not linked to a Party");
    	
    	String partyId = (party.isSetProperty(pluginPlus.epAuthId)) ? party.getProperty(pluginPlus.epAuthId).toString() : party.getId().toString();

    	if (!partyId.equalsIgnoreCase(userId))
    		throw new ForbiddenException("Thing not linked to acting Party"); 

    	
    }
    
    protected void assertOwnershipGroup(Entity group, Principal principal)
    {
    	assertPrincipal(principal);
    	
    	if (group == null)
    		throw new IllegalArgumentException("Group does not exist");

    	if (!group.getEntityType().equals(pluginPlus.etGroup))
    		throw new IllegalArgumentException("Entity not of type Group");
    		
    	// We can get the username from the Principal
		String userId = principal.getName();

    	// Ensure Ownership for Group
    	Entity party = null;
    	
    	if (group != null)
    		party = group.getProperty(pluginPlus.npPartyGroup);
    	
    	if (party == null)
    		throw new IllegalArgumentException("Group not linked to a Party");
    	
    	String partyId = (party.isSetProperty(pluginPlus.epAuthId)) ? party.getProperty(pluginPlus.epAuthId).toString() : party.getId().toString();

    	if (!partyId.equalsIgnoreCase(userId))
    		throw new ForbiddenException("Group not linked to acting Party"); 
    	
    }

    protected void assertOwnershipParty(Entity party, Principal principal)
    {
    	assertPrincipal(principal);
    	
    	if (party == null)
    		throw new IllegalArgumentException("Party does not exist");

    	if (!party.getEntityType().equals(pluginPlus.etParty))
    		throw new IllegalArgumentException("Entity not of type Party");
    		
    	// We can get the username from the Principal
		String userId = principal.getName();
		String partyId = party.getProperty(pluginPlus.epAuthId);
		if ((partyId != null) && (!userId.equalsIgnoreCase(partyId)))
    	{
    		// The authId is set by the plugin - it cannot be changed via a PATCH
    		throw new ForbiddenException("Party not representing acting user");
    	}
		else
		{
			partyId = party.getId().toString();
			if ((partyId != null) && (!userId.equalsIgnoreCase(partyId)))
	    	{
	    		// The authId is set by the plugin - it cannot be changed via a PATCH
	    		throw new ForbiddenException("Party not representing acting user");
	    	}
		}
    }
    
    
}
