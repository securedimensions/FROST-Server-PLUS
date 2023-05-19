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
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.tables.StaTableAbstract;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.tables.TableCollection;
import de.fraunhofer.iosb.ilt.frostserver.plugin.coremodel.PluginCoreModel;
import de.fraunhofer.iosb.ilt.frostserver.plugin.coremodel.TableImpDatastreams;
import de.fraunhofer.iosb.ilt.frostserver.plugin.multidatastream.TableImpMultiDatastreams;
import org.jooq.DataType;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.TableField;
import org.jooq.TableLike;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

/**
 *
 * @author am
 * @author scf
 */
public class TableImpLicense extends StaTableAbstract<TableImpLicense> {

    /**
     * The column <code>public.LICENSES.EP_DESCRIPTION</code>.
     */
    public final TableField<Record, String> colDescription = createField(DSL.name("DESCRIPTION"), SQLDataType.CLOB, this);

    /**
     * The column <code>public.LICENSES.EP_NAME</code>.
     */
    public final TableField<Record, String> colName = createField(DSL.name("NAME"), SQLDataType.CLOB.defaultValue(DSL.field("'no name'::text", SQLDataType.CLOB)), this);

    /**
     * The column <code>public.LICENSES.EP_DEFINITION</code>.
     */
    public final TableField<Record, String> colDefinition = createField(DSL.name("DEFINITION"), SQLDataType.CLOB, this);

    /**
     * The column <code>public.LICENSES.EP_ATTRIBUTION_TEXT</code>.
     */
    public final TableField<Record, String> colAttributionText = createField(DSL.name("ATTRIBUTION_TEXT"), SQLDataType.CLOB, this);

    /**
     * The column <code>public.LICENSES.EP_LOGO</code>.
     */
    public final TableField<Record, String> colLogo = createField(DSL.name("LOGO"), SQLDataType.CLOB, this);

    /**
     * The column <code>public.LICENSES.EP_ID</code>.
     */
    public final TableField<Record, ?> colId = createField(DSL.name("ID"), getIdType(), this);

    private final PluginPLUS pluginPLUS;
    private final PluginCoreModel pluginCoreModel;

    /**
     * Create a <code>public.LICENSES</code> table reference.
     *
     * @param idType The (SQL)DataType of the Id columns used in the actual
     * database.
     * @param pluginParty the license plugin this table belongs to.
     * @param pluginCoreModel the coreModel plugin that this data model links
     * to.
     */
    public TableImpLicense(DataType<?> idType, PluginPLUS pluginParty, PluginCoreModel pluginCoreModel) {
        super(idType, DSL.name("LICENSES"), null);
        this.pluginPLUS = pluginParty;
        this.pluginCoreModel = pluginCoreModel;
    }

    private TableImpLicense(Name alias, TableImpLicense aliased, PluginPLUS pluginLicense, PluginCoreModel pluginCoreModel) {
        super(aliased.getIdType(), alias, aliased);
        this.pluginPLUS = pluginLicense;
        this.pluginCoreModel = pluginCoreModel;
    }

    @Override
    public void initRelations() {
        final TableCollection tables = getTables();

        initDatastreams(tables);
        initMultiDatastreams(tables);
        initGroups(tables);
    }

    private void initDatastreams(TableCollection tables) {
        TableImpDatastreams tableDatastreams = tables.getTableForClass(TableImpDatastreams.class);
        final int licenseIdIdx = tableDatastreams.indexOf("LICENSE_ID");

        registerRelation(new RelationOneToMany<>(pluginPLUS.npDatastreamsLicense, this, tableDatastreams)
                .setSourceFieldAccessor(TableImpLicense::getId)
                .setTargetFieldAccessor(table -> (TableField<Record, ?>) table.field(licenseIdIdx)));

        // We add the relation to us to the Datastreams table.
        tableDatastreams.registerRelation(new RelationOneToMany<>(pluginPLUS.npLicenseDatastream, tableDatastreams, this)
                .setSourceFieldAccessor(table -> (TableField<Record, ?>) table.field(licenseIdIdx))
                .setTargetFieldAccessor(TableImpLicense::getId));

    }

    private void initMultiDatastreams(TableCollection tables) {
        TableImpMultiDatastreams tableMultiDatastreams = tables.getTableForClass(TableImpMultiDatastreams.class);
        if (tableMultiDatastreams != null) {
            final int licenseIdIdx = tableMultiDatastreams.indexOf("LICENSE_ID");
            registerRelation(new RelationOneToMany<>(pluginPLUS.npMultiDatastreamsLicense, this, tableMultiDatastreams)
                    .setSourceFieldAccessor(TableImpLicense::getId)
                    .setTargetFieldAccessor(table -> (TableField<Record, ?>) table.field(licenseIdIdx)));

            // We add the relation to us to the MultiDatastreams table.
            tableMultiDatastreams.registerRelation(new RelationOneToMany<>(pluginPLUS.npLicenseMultiDatastream, tableMultiDatastreams, this)
                    .setSourceFieldAccessor(table -> (TableField<Record, ?>) table.field(licenseIdIdx))
                    .setTargetFieldAccessor(TableImpLicense::getId));
        }

    }

    private void initGroups(TableCollection tables) {
        TableImpGroups tableGroups = tables.getTableForClass(TableImpGroups.class);
        final int licenseIdIdx = tableGroups.indexOf("LICENSE_ID");

        registerRelation(new RelationOneToMany<>(pluginPLUS.npGroupsLicense, this, tableGroups)
                .setSourceFieldAccessor(TableImpLicense::getId)
                .setTargetFieldAccessor(table -> (TableField<Record, ?>) table.field(licenseIdIdx)));

        // We add the relation to us to the Groups table.
        tableGroups.registerRelation(new RelationOneToMany<>(pluginPLUS.npLicenseGroup, tableGroups, this)
                .setSourceFieldAccessor(table -> (TableField<Record, ?>) table.field(licenseIdIdx))
                .setTargetFieldAccessor(TableImpLicense::getId));

    }

    @Override
    public void initProperties(final EntityFactories entityFactories) {
        final TableCollection tables = getTables();
        pfReg.addEntryId(TableImpLicense::getId);
        pfReg.addEntryString(pluginCoreModel.epName, table -> table.colName);
        pfReg.addEntryString(pluginPLUS.epLicenseDescription, table -> table.colDescription);
        pfReg.addEntryString(pluginPLUS.epLicenseDefinition, table -> table.colDefinition);
        pfReg.addEntryString(pluginPLUS.epLicenseAttributionText, table -> table.colAttributionText);
        pfReg.addEntryString(pluginPLUS.epLicenseLogo, table -> table.colLogo);

        // We register a navigationProperty on the Datastreams table.
        pfReg.addEntry(pluginPLUS.npDatastreamsLicense, TableImpLicense::getId);

        // We register a navigationProperty on the Groups table.
        pfReg.addEntry(pluginPLUS.npGroupsLicense, TableImpLicense::getId);

        TableImpDatastreams datastreamsTable = tables.getTableForClass(TableImpDatastreams.class);
        final int licenseDatastreamsIdIdx = datastreamsTable.registerField(DSL.name("LICENSE_ID"), getIdType());
        datastreamsTable.getPropertyFieldRegistry()
                .addEntry(pluginPLUS.npLicenseDatastream, table -> (TableField<Record, ?>) table.field(licenseDatastreamsIdIdx));

        TableImpMultiDatastreams tableMultiDatastreams = tables.getTableForClass(TableImpMultiDatastreams.class);
        if (tableMultiDatastreams != null) {
            final int licenseMDIdIdx = tableMultiDatastreams.registerField(DSL.name("LICENSE_ID"), getIdType());
            tableMultiDatastreams.getPropertyFieldRegistry()
                    .addEntry(pluginPLUS.npLicenseMultiDatastream, table -> (TableField<Record, ?>) ((TableLike<Record>) table).field(licenseMDIdIdx));
        }

        TableImpGroups groupsTable = tables.getTableForClass(TableImpGroups.class);
        if (groupsTable != null) {
            final int licenseGroupsIdIdx = groupsTable.registerField(DSL.name("LICENSE_ID"), getIdType());
            groupsTable.getPropertyFieldRegistry()
                    .addEntry(pluginPLUS.npLicenseGroup, table -> (TableField<Record, ?>) table.field(licenseGroupsIdIdx));
        }

    }

    @Override
    public EntityType getEntityType() {
        return pluginPLUS.etLicense;
    }

    @Override
    public TableField<Record, ?> getId() {
        return colId;
    }

    @Override
    public TableImpLicense as(Name alias) {
        return new TableImpLicense(alias, this, pluginPLUS, pluginCoreModel).initCustomFields();
    }

    @Override
    public TableImpLicense getThis() {
        return this;
    }

}
