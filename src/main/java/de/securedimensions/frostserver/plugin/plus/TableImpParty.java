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
import de.fraunhofer.iosb.ilt.frostserver.persistence.IdManager;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.bindings.JsonBinding;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.bindings.JsonValue;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.factories.EntityFactories;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.relations.RelationOneToMany;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.tables.StaMainTable;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.tables.StaTable;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.tables.StaTableAbstract;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.tables.TableCollection;
import de.fraunhofer.iosb.ilt.frostserver.plugin.coremodel.PluginCoreModel;
import de.fraunhofer.iosb.ilt.frostserver.plugin.coremodel.TableImpDatastreams;
import de.fraunhofer.iosb.ilt.frostserver.plugin.coremodel.TableImpLocations;
import de.fraunhofer.iosb.ilt.frostserver.plugin.multidatastream.PluginMultiDatastream;
import de.fraunhofer.iosb.ilt.frostserver.plugin.multidatastream.TableImpMultiDatastreams;

import org.jooq.DataType;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.TableField;
import org.jooq.TableLike;
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
public class TableImpParty<J extends Comparable> extends StaTableAbstract<J, TableImpParty<J>> {

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
     * The column <code>public.PARTIES.EP_PROPERTIES</code>.
     */
    public final TableField<Record, JsonValue> colProperties = createField(DSL.name("PROPERTIES"), DefaultDataType.getDefaultDataType(TYPE_JSONB), this, "", new JsonBinding());

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
    public final TableField<Record, J> colId = createField(DSL.name("ID"), getIdType(), this);

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
    public TableImpParty(DataType<J> idType, PluginPLUS pluginParty, PluginCoreModel pluginCoreModel) {
        super(idType, DSL.name("PARTIES"), null);
        this.pluginPLUS = pluginParty;
        this.pluginCoreModel = pluginCoreModel;
    }

    private TableImpParty(Name alias, TableImpParty<J> aliased, PluginPLUS pluginParty, PluginCoreModel pluginCoreModel) {
        super(aliased.getIdType(), alias, aliased);
        this.pluginPLUS = pluginParty;
        this.pluginCoreModel = pluginCoreModel;
    }

    @Override
    public void initRelations() {
        final TableCollection<J> tables = getTables();
        
        TableImpDatastreams<J> tableDatastreams = tables.getTableForClass(TableImpDatastreams.class);
        final int partyIdIdx = tableDatastreams.indexOf("PARTY_ID");

        // We add relation to the Datstreams table
        registerRelation(new RelationOneToMany<>(pluginPLUS.npDatastreamsParty, this, tableDatastreams)
                .setSourceFieldAccessor(TableImpParty::getId)
                .setTargetFieldAccessor(table -> (TableField<Record, J>) table.field(partyIdIdx))
        );

        // We add the relation to us from the Datastreams table.
        tableDatastreams.registerRelation(new RelationOneToMany<>(pluginPLUS.npPartyDatastream, tableDatastreams, this)
                .setSourceFieldAccessor(table -> (TableField<Record, J>) table.field(partyIdIdx))
                .setTargetFieldAccessor(TableImpParty::getId)
        );

        TableImpMultiDatastreams<J> tableMultiDatastreams = tables.getTableForClass(TableImpMultiDatastreams.class);
        if (tableMultiDatastreams != null)
        {
	        final int partyMDIdIdx = tableMultiDatastreams.indexOf("PARTY_ID");
	        registerRelation(new RelationOneToMany<>(pluginPLUS.npMultiDatastreamsParty, this, tableMultiDatastreams)
	                .setSourceFieldAccessor(TableImpParty::getId)
	                .setTargetFieldAccessor(table -> (TableField<Record, J>) table.field(partyMDIdIdx))
	        );

	        // We add the relation to us to the MultiDatastreams table.
	        tableMultiDatastreams.registerRelation(new RelationOneToMany<>(pluginPLUS.npPartyMultiDatastream, tableMultiDatastreams, this)
	                .setSourceFieldAccessor(table -> (TableField<Record, J>) table.field(partyMDIdIdx))
	                .setTargetFieldAccessor(TableImpParty::getId)
	        );
        }
    }
    
    @Override
    public void initProperties(final EntityFactories<J> entityFactories) {
        final TableCollection<J> tables = getTables();
        final IdManager idManager = entityFactories.getIdManager();
        pfReg.addEntryId(idManager, TableImpParty::getId);
        pfReg.addEntryString(pluginCoreModel.epName, table -> table.colName);
        pfReg.addEntryString(pluginCoreModel.epDescription, table -> table.colDescription);
        pfReg.addEntryMap(ModelRegistry.EP_PROPERTIES, table -> table.colProperties);
        pfReg.addEntryString(pluginPLUS.epAuthId, table -> table.colAuthId);
        pfReg.addEntryString(pluginPLUS.epNickName, table -> table.colNickName);
        pfReg.addEntrySimple(pluginPLUS.epPartyRole, table -> table.colRole);

        // We register a navigationProperty on the Datastreams table.
        pfReg.addEntry(pluginPLUS.npDatastreamsParty, TableImpParty::getId, idManager);
     
        // We register a navigationProperty on the MultiDatastreams table.
        pfReg.addEntry(pluginPLUS.npMultiDatastreamsParty, TableImpParty::getId, idManager);

        TableImpDatastreams<J> tableDatastreams = tables.getTableForClass(TableImpDatastreams.class);
        final int partyDatastreamsIdIdx = tableDatastreams.registerField(DSL.name("PARTY_ID"), getIdType());
        tableDatastreams.getPropertyFieldRegistry()
                .addEntry(pluginPLUS.npPartyDatastream, table -> (TableField<Record, J>) table.field(partyDatastreamsIdIdx), idManager);

        TableImpMultiDatastreams<J> tableMultiDatastreams = tables.getTableForClass(TableImpMultiDatastreams.class);
        if (tableMultiDatastreams != null)
        {
        	final int partyMDIdIdx = tableMultiDatastreams.registerField(DSL.name("PARTY_ID"), getIdType());
            tableMultiDatastreams.getPropertyFieldRegistry()
        		.addEntry(pluginPLUS.npPartyMultiDatastream, table -> (TableField<Record, J>) ((TableLike<Record>) table).field(partyMDIdIdx), idManager);
        }
        
    }
    
    @Override
    public EntityType getEntityType() {
        return pluginPLUS.etParty;
    }

    @Override
    public TableField<Record, J> getId() {
        return colId;
    }

    @Override
    public TableImpParty<J> as(Name alias) {
        return new TableImpParty<>(alias, this, pluginPLUS, pluginCoreModel).initCustomFields();
    }

    @Override
    public TableImpParty<J> getThis() {
        return this;
    }

}
