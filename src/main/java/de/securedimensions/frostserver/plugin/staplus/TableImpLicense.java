/*
 * Copyright (C) 2021-2024 Secure Dimensions GmbH, D-81377
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
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.JooqPersistenceManager;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.factories.EntityFactories;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.relations.RelationOneToMany;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.tables.StaMainTable;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.tables.StaTableAbstract;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.tables.TableCollection;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.utils.validator.SecurityTableWrapper;
import de.fraunhofer.iosb.ilt.frostserver.plugin.coremodel.PluginCoreModel;
import de.fraunhofer.iosb.ilt.frostserver.plugin.coremodel.TableImpDatastreams;
import de.fraunhofer.iosb.ilt.frostserver.plugin.multidatastream.TableImpMultiDatastreams;
import java.util.Arrays;
import java.util.List;
import org.jooq.*;
import org.jooq.Record;
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
        super(idType, DSL.name("LICENSES"), null, null);
        this.pluginPLUS = pluginParty;
        this.pluginCoreModel = pluginCoreModel;
    }

    private TableImpLicense(Name alias, TableImpLicense aliased, PluginPLUS pluginLicense, PluginCoreModel pluginCoreModel) {
        this(alias, aliased, aliased, pluginLicense, pluginCoreModel);
    }

    private TableImpLicense(Name alias, TableImpLicense aliased, Table updatedSql, PluginPLUS pluginLicense, PluginCoreModel pluginCoreModel) {
        super(aliased.getIdType(), alias, aliased, updatedSql);
        this.pluginPLUS = pluginLicense;
        this.pluginCoreModel = pluginCoreModel;
    }

    @Override
    public void initRelations() {
        final TableCollection tables = getTables();

        initDatastreams(tables);
        initMultiDatastreams(tables);
        initGroups(tables);
        initCampaign(tables);
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
        TableImpGroup tableGroups = tables.getTableForClass(TableImpGroup.class);
        registerRelation(new RelationOneToMany<>(pluginPLUS.npGroupsLicense, this, tableGroups)
                .setSourceFieldAccessor(TableImpLicense::getId)
                .setTargetFieldAccessor(TableImpGroup::getLicenseId));
    }

    private void initCampaign(TableCollection tables) {
        TableImpCampaign tableCampaign = tables.getTableForClass(TableImpCampaign.class);
        registerRelation(new RelationOneToMany<>(pluginPLUS.npCampaignsLicense, this, tableCampaign)
                .setSourceFieldAccessor(TableImpLicense::getId)
                .setTargetFieldAccessor(TableImpCampaign::getLicenseId));
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

        // We register a navigationProperty on the Campaign table.
        pfReg.addEntry(pluginPLUS.npCampaignsLicense, TableImpLicense::getId);

        TableImpDatastreams datastreamsTable = tables.getTableForClass(TableImpDatastreams.class);
        final int licenseDatastreamsIdIdx = datastreamsTable.registerField(DSL.name("LICENSE_ID"), getIdType());
        datastreamsTable.getPropertyFieldRegistry()
                .addEntry(pluginPLUS.npLicenseDatastream, table -> (TableField<Record, ?>) table.field(licenseDatastreamsIdIdx));

        TableImpMultiDatastreams tableMultiDatastreams = tables.getTableForClass(TableImpMultiDatastreams.class);
        if (tableMultiDatastreams != null) {
            // We register a navigationProperty on the MultiDatastreams table.
            pfReg.addEntry(pluginPLUS.npMultiDatastreamsLicense, TableImpLicense::getId);

            final int licenseMDIdIdx = tableMultiDatastreams.registerField(DSL.name("LICENSE_ID"), getIdType());
            tableMultiDatastreams.getPropertyFieldRegistry()
                    .addEntry(pluginPLUS.npLicenseMultiDatastream, table -> (TableField<Record, ?>) ((TableLike<Record>) table).field(licenseMDIdIdx));
        }

    }

    @Override
    public EntityType getEntityType() {
        return pluginPLUS.etLicense;
    }

    @Override
    public List<Field> getPkFields() {
        return Arrays.asList(colId);
    }

    public TableField<Record, ?> getId() {
        return colId;
    }

    @Override
    public TableImpLicense as(Name alias) {
        return new TableImpLicense(alias, this, pluginPLUS, pluginCoreModel).initCustomFields();
    }

    @Override
    public StaMainTable<TableImpLicense> asSecure(String name, JooqPersistenceManager pm) {
        final SecurityTableWrapper securityWrapper = getSecurityWrapper();
        if (securityWrapper == null) {
            return as(name);
        }
        final Table wrappedTable = securityWrapper.wrap(this, pm);
        return new TableImpLicense(DSL.name(name), this, wrappedTable, pluginPLUS, pluginCoreModel);
    }

    @Override
    public TableImpLicense getThis() {
        return this;
    }

}
