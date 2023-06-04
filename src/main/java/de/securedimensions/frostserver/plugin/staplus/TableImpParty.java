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
package de.securedimensions.frostserver.plugin.staplus;

import de.fraunhofer.iosb.ilt.frostserver.model.EntityType;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.factories.EntityFactories;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.relations.RelationOneToMany;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.tables.StaMainTable;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.tables.StaTableAbstract;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.tables.TableCollection;
import de.fraunhofer.iosb.ilt.frostserver.plugin.coremodel.PluginCoreModel;
import de.fraunhofer.iosb.ilt.frostserver.plugin.coremodel.TableImpDatastreams;
import de.fraunhofer.iosb.ilt.frostserver.plugin.coremodel.TableImpThings;
import de.fraunhofer.iosb.ilt.frostserver.plugin.multidatastream.PluginMultiDatastream;
import de.fraunhofer.iosb.ilt.frostserver.plugin.multidatastream.TableImpMultiDatastreams;
import org.jooq.DataType;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.TableField;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

/**
 *
 * @author am
 * @author scf
 */
public class TableImpParty extends StaTableAbstract<TableImpParty> {

    /**
     * The column <code>public.PARTIES.EP_DESCRIPTION</code>.
     */
    public final TableField<Record, String> colDescription = createField(DSL.name("DESCRIPTION"), SQLDataType.CLOB, this);

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
        super(idType, DSL.name("PARTIES"), null, null);
        this.pluginPLUS = pluginParty;
        this.pluginCoreModel = pluginCoreModel;
        this.pluginMultiDatastream = pluginMultiDatastream;
    }

    private TableImpParty(Name alias, TableImpParty aliased, PluginPLUS pluginParty, PluginCoreModel pluginCoreModel, PluginMultiDatastream pluginMultiDatastream) {
        super(aliased.getIdType(), alias, aliased, null);
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
                .setTargetFieldAccessor(table -> (TableField<Record, ?>) table.field(tableThings.indexOf("PARTY_ID"))));

        // We add the relation to us from the Things table.
        tableThings.registerRelation(new RelationOneToMany<>(pluginPLUS.npPartyThing, tableThings, this)
                .setSourceFieldAccessor(table -> (TableField<Record, ?>) table.field(tableThings.indexOf("PARTY_ID")))
                .setTargetFieldAccessor(TableImpParty::getId));

        // We add relation to the Groups table
        registerRelation(new RelationOneToMany<>(pluginPLUS.npGroupsParty, this, tableGroups)
                .setSourceFieldAccessor(TableImpParty::getId)
                .setTargetFieldAccessor(table -> (TableField<Record, ?>) table.field(tableGroups.indexOf("PARTY_ID"))));

        // We add the relation to us from the Things table.
        tableGroups.registerRelation(new RelationOneToMany<>(pluginPLUS.npPartyGroup, tableGroups, this)
                .setSourceFieldAccessor(table -> (TableField<Record, ?>) table.field(tableGroups.indexOf("PARTY_ID")))
                .setTargetFieldAccessor(TableImpParty::getId));

        // We add relation to the Datstreams table
        registerRelation(new RelationOneToMany<>(pluginPLUS.npDatastreamsParty, this, tableDatastreams)
                .setSourceFieldAccessor(TableImpParty::getId)
                .setTargetFieldAccessor(table -> (TableField<Record, ?>) table.field(tableDatastreams.indexOf("PARTY_ID"))));

        // We add the relation to us from the Datastreams table.
        tableDatastreams.registerRelation(new RelationOneToMany<>(pluginPLUS.npPartyDatastream, tableDatastreams, this)
                .setSourceFieldAccessor(table -> (TableField<Record, ?>) table.field(tableDatastreams.indexOf("PARTY_ID")))
                .setTargetFieldAccessor(TableImpParty::getId));

        TableImpMultiDatastreams tableMultiDatastreams = tables.getTableForClass(TableImpMultiDatastreams.class);
        if (tableMultiDatastreams != null) {
            final int partyMDIdIdx = tableMultiDatastreams.indexOf("PARTY_ID");
            registerRelation(new RelationOneToMany<>(pluginPLUS.npMultiDatastreamsParty, this, tableMultiDatastreams)
                    .setSourceFieldAccessor(TableImpParty::getId)
                    .setTargetFieldAccessor(table -> (TableField<Record, ?>) table.field(partyMDIdIdx)));

            // We add the relation to us to the MultiDatastreams table.
            tableMultiDatastreams.registerRelation(new RelationOneToMany<>(pluginPLUS.npPartyMultiDatastream, tableMultiDatastreams, this)
                    .setSourceFieldAccessor(table -> (TableField<Record, ?>) table.field(partyMDIdIdx))
                    .setTargetFieldAccessor(TableImpParty::getId));
        }
    }

    @Override
    public void initProperties(final EntityFactories entityFactories) {
        final TableCollection tables = getTables();
        pfReg.addEntryId(TableImpParty::getId);
        pfReg.addEntryString(pluginPLUS.epPartyDescription, table -> table.colDescription);
        pfReg.addEntryString(pluginPLUS.epAuthId, table -> table.colAuthId);
        pfReg.addEntryString(pluginPLUS.epDisplayName, table -> table.colDisplayName);
        pfReg.addEntrySimple(pluginPLUS.epPartyRole, table -> table.colRole);

        // We register a navigationProperty on the Things table.
        pfReg.addEntry(pluginPLUS.npThingsParty, TableImpParty::getId);

        // We register a navigationProperty on the Groups table.
        pfReg.addEntry(pluginPLUS.npGroupsParty, TableImpParty::getId);

        // We register a navigationProperty on the Datastreams table.
        pfReg.addEntry(pluginPLUS.npDatastreamsParty, TableImpParty::getId);

        // We register a navigationProperty on the MultiDatastreams table.
        pfReg.addEntry(pluginPLUS.npMultiDatastreamsParty, TableImpParty::getId);

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
    public StaMainTable<TableImpParty> asSecure(String s) {
        return as(s);
    }

    @Override
    public TableImpParty getThis() {
        return this;
    }

}
