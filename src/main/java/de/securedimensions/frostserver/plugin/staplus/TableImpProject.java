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
import de.fraunhofer.iosb.ilt.frostserver.model.ModelRegistry;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.JooqPersistenceManager;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.bindings.JsonBinding;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.bindings.JsonValue;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.bindings.MomentBinding;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.factories.EntityFactories;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.relations.RelationManyToMany;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.relations.RelationOneToMany;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.tables.StaMainTable;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.tables.StaTableAbstract;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.tables.TableCollection;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.utils.PropertyFieldRegistry.ConverterTimeInstant;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.utils.validator.SecurityTableWrapper;
import de.fraunhofer.iosb.ilt.frostserver.plugin.coremodel.PluginCoreModel;
import de.fraunhofer.iosb.ilt.frostserver.plugin.coremodel.TableImpDatastreams;
import de.fraunhofer.iosb.ilt.frostserver.plugin.multidatastream.PluginMultiDatastream;
import de.fraunhofer.iosb.ilt.frostserver.plugin.multidatastream.TableImpMultiDatastreams;
import net.time4j.Moment;
import org.jooq.DataType;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultDataType;
import org.jooq.impl.SQLDataType;

/**
 *
 * @author am
 * @author scf
 */
public class TableImpProject extends StaTableAbstract<TableImpProject> {

    /**
     * The column <code>public.PROJECT.EP_NAME</code>.
     */
    public final TableField<Record, String> colName = createField(DSL.name("NAME"), SQLDataType.CLOB.defaultValue(DSL.field("'no name'::text", SQLDataType.CLOB)), this);

    /**
     * The column <code>public.PROJECT.EP_DESCRIPTION</code>.
     */
    public final TableField<Record, String> colDescription = createField(DSL.name("DESCRIPTION"), SQLDataType.CLOB, this);

    /**
     * The column <code>public.PROJECT.EP_PROPERTIES</code>.
     */
    public final TableField<Record, JsonValue> colProperties = createField(DSL.name("PROPERTIES"), DefaultDataType.getDefaultDataType(TYPE_JSONB), this, "", new JsonBinding());

    /**
     * The column <code>public.PROJECT.EP_CLASSIFICATION</code>.
     */
    public final TableField<Record, String> colClassification = createField(DSL.name("CLASSIFICATION"), SQLDataType.CLOB, this);

    /**
     * The column <code>public.PROJECT.EP_TERMSOFUSE</code>.
     */
    public final TableField<Record, String> colTermsOfUse = createField(DSL.name("TERMSOFUSE"), SQLDataType.CLOB, this);

    /**
     * The column <code>public.PROJECT.EP_PRIVACYPOLICY</code>.
     */
    public final TableField<Record, String> colPrivacyPolicy = createField(DSL.name("PRIVACYPOLICY"), SQLDataType.CLOB, this);

    /**
     * The column <code>public.PROJECTS.EP_URL</code>.
     */
    public final TableField<Record, String> colUrl = createField(DSL.name("URL"), SQLDataType.CLOB, this);

    /**
     * The column <code>public.PROJECT.START_TIME</code>.
     */
    public final TableField<Record, Moment> colStartTime = createField(DSL.name("START_TIME"), SQLDataType.TIMESTAMP, this, "", new MomentBinding());

    /**
     * The column <code>public.PROJECT.END_TIME</code>.
     */
    public final TableField<Record, Moment> colEndTime = createField(DSL.name("END_TIME"), SQLDataType.TIMESTAMP, this, "", new MomentBinding());

    /**
     * The column <code>public.PROJECT.CREATION_TIME</code>.
     */
    public final TableField<Record, Moment> colCreationTime = createField(DSL.name("CREATION_TIME"), SQLDataType.TIMESTAMP, this, "", new MomentBinding());

    /**
     * The column <code>public.PROJECT.EP_ID</code>.
     */
    public final TableField<Record, ?> colId = createField(DSL.name("ID"), getIdType(), this);

    public final TableField<Record, ?> colPartyId;

    public final TableField<Record, ?> colLicenseId;

    private final PluginPLUS pluginPLUS;
    private final PluginCoreModel pluginCoreModel;

    private final PluginMultiDatastream pluginMultiDatastream;

    /**
     * Create a <code>public.PROJECTS</code> table reference.
     *
     * @param idType The (SQL)DataType of the Id columns used in the actual
     * database.
     * @param pluginProject the party plugin this table belongs to.
     * @param pluginCoreModel the coreModel plugin that this data model links
     * to.
     */
    public TableImpProject(DataType<?> idType, DataType<?> idTypeParty, DataType<?> idTypeLicense, PluginPLUS pluginProject, PluginCoreModel pluginCoreModel, PluginMultiDatastream pluginMultiDatastream) {
        super(idType, DSL.name("PROJECTS"), null, null);
        this.pluginPLUS = pluginProject;
        this.pluginCoreModel = pluginCoreModel;
        this.pluginMultiDatastream = pluginMultiDatastream;

        colPartyId = createField(DSL.name("PARTY_ID"), idTypeParty.nullable(true));
        colLicenseId = createField(DSL.name("LICENSE_ID"), idTypeLicense.nullable(true));

    }

    private TableImpProject(Name alias, TableImpProject aliased, PluginPLUS pluginPlus, PluginCoreModel pluginCoreModel, PluginMultiDatastream pluginMultiDatastream) {
        this(alias, aliased, aliased, pluginPlus, pluginCoreModel, pluginMultiDatastream);
    }

    private TableImpProject(Name alias, TableImpProject aliased, Table updatedSql, PluginPLUS pluginPlus, PluginCoreModel pluginCoreModel, PluginMultiDatastream pluginMultiDatastream) {
        super(aliased.getIdType(), alias, aliased, updatedSql);
        this.pluginPLUS = pluginPlus;
        this.pluginCoreModel = pluginCoreModel;
        this.pluginMultiDatastream = pluginMultiDatastream;

        colPartyId = createField(DSL.name("PARTY_ID"), aliased.colPartyId.getDataType().nullable(true));
        colLicenseId = createField(DSL.name("LICENSE_ID"), aliased.colLicenseId.getDataType().nullable(true));

    }

    @Override
    public void initRelations() {
        final TableCollection tables = getTables();

        TableImpDatastreams tableDatastreams = tables.getTableForClass(TableImpDatastreams.class);
        final int projectIdIdx = tableDatastreams.indexOf("PROJECT_ID");

        // We add the relation to Party table.
        registerRelation(new RelationOneToMany<>(pluginPLUS.npPartyProject, this, tables.getTableForClass(TableImpParty.class))
                .setSourceFieldAccessor(TableImpProject::getPartyId)
                .setTargetFieldAccessor(TableImpParty::getId));

        // We add the relation to License table.
        registerRelation(new RelationOneToMany<>(pluginPLUS.npLicenseProject, this, tables.getTableForClass(TableImpLicense.class))
                .setSourceFieldAccessor(TableImpProject::getLicenseId)
                .setTargetFieldAccessor(TableImpLicense::getId));

        // Add relation to Datastreams table
        final TableImpProjectsDatastreams tableProjectsDatastreams = tables.getTableForClass(TableImpProjectsDatastreams.class);
        registerRelation(new RelationManyToMany<>(pluginPLUS.npDatastreamsProject, this, tableProjectsDatastreams, tableDatastreams)
                .setSourceFieldAcc(TableImpProject::getId)
                .setSourceLinkFieldAcc(TableImpProjectsDatastreams::getProjectId)
                .setTargetLinkFieldAcc(TableImpProjectsDatastreams::getDatastreamId)
                .setTargetFieldAcc(TableImpDatastreams::getId));
        tableDatastreams.registerRelation(new RelationManyToMany<>(pluginPLUS.npProjectDatastreams, tableDatastreams, tableProjectsDatastreams, this)
                .setSourceFieldAcc(TableImpDatastreams::getId)
                .setSourceLinkFieldAcc(TableImpProjectsDatastreams::getDatastreamId)
                .setTargetLinkFieldAcc(TableImpProjectsDatastreams::getProjectId)
                .setTargetFieldAcc(TableImpProject::getId));

        if (pluginMultiDatastream.isEnabled()) {
            // Add relation to MultiDatastreams table
            TableImpMultiDatastreams tableMultiDatastreams = tables.getTableForClass(TableImpMultiDatastreams.class);
            final TableImpProjectsMultiDatastreams tableProjectsMultiDatastreams = tables.getTableForClass(TableImpProjectsMultiDatastreams.class);
            registerRelation(new RelationManyToMany<>(pluginPLUS.npMultiDatastreamsProject, this, tableProjectsMultiDatastreams, tableMultiDatastreams)
                    .setSourceFieldAcc(TableImpProject::getId)
                    .setSourceLinkFieldAcc(TableImpProjectsMultiDatastreams::getProjectId)
                    .setTargetLinkFieldAcc(TableImpProjectsMultiDatastreams::getMultiDatastreamId)
                    .setTargetFieldAcc(TableImpMultiDatastreams::getId));
            tableMultiDatastreams.registerRelation(new RelationManyToMany<>(pluginPLUS.npProjectMultiDatastreams, tableMultiDatastreams, tableProjectsMultiDatastreams, this)
                    .setSourceFieldAcc(TableImpMultiDatastreams::getId)
                    .setSourceLinkFieldAcc(TableImpProjectsMultiDatastreams::getMultiDatastreamId)
                    .setTargetLinkFieldAcc(TableImpProjectsMultiDatastreams::getProjectId)
                    .setTargetFieldAcc(TableImpProject::getId));

        }

    }

    @Override
    public void initProperties(final EntityFactories entityFactories) {
        final TableCollection tables = getTables();
        pfReg.addEntryId(TableImpProject::getId);
        pfReg.addEntryString(pluginCoreModel.epName, table -> table.colName);
        pfReg.addEntryString(pluginCoreModel.epDescription, table -> table.colDescription);
        pfReg.addEntryMap(ModelRegistry.EP_PROPERTIES, table -> table.colProperties);
        pfReg.addEntryString(pluginPLUS.epClassification, table -> table.colClassification);
        pfReg.addEntryString(pluginPLUS.epProjectTermsOfUse, table -> table.colTermsOfUse);
        pfReg.addEntryString(pluginPLUS.epProjectPrivacyPolicy, table -> table.colPrivacyPolicy);
        pfReg.addEntryString(pluginPLUS.epUrl, table -> table.colUrl);

        pfReg.addEntry(pluginPLUS.epProjectCreationTime, table -> table.colCreationTime,
                new ConverterTimeInstant<>(pluginPLUS.epProjectCreationTime, table -> table.colCreationTime));
        pfReg.addEntry(pluginPLUS.epProjectStartTime, table -> table.colStartTime,
                new ConverterTimeInstant<>(pluginPLUS.epProjectStartTime, table -> table.colStartTime));
        pfReg.addEntry(pluginPLUS.epProjectEndTime, table -> table.colEndTime,
                new ConverterTimeInstant<>(pluginPLUS.epProjectEndTime, table -> table.colEndTime));

        pfReg.addEntry(pluginPLUS.npDatastreamsProject, TableImpProject::getId);
        TableImpDatastreams tableDatastreams = tables.getTableForClass(TableImpDatastreams.class);
        tableDatastreams.getPropertyFieldRegistry()
                .addEntry(pluginPLUS.npProjectDatastreams, TableImpDatastreams::getId);

        if (pluginMultiDatastream.isEnabled()) {
            TableImpMultiDatastreams tableMultiDatastreams = tables.getTableForClass(TableImpMultiDatastreams.class);
            pfReg.addEntry(pluginPLUS.npMultiDatastreamsProject, TableImpProject::getId);
            tableMultiDatastreams.getPropertyFieldRegistry()
                    .addEntry(pluginPLUS.npProjectMultiDatastreams, TableImpMultiDatastreams::getId);
        }
        pfReg.addEntry(pluginPLUS.npPartyProject, TableImpProject::getPartyId);
        pfReg.addEntry(pluginPLUS.npLicenseProject, TableImpProject::getLicenseId);
        pfReg.addEntry(pluginPLUS.npGroupsProject, TableImpProject::getId);
    }

    @Override
    public EntityType getEntityType() {
        return pluginPLUS.etProject;
    }

    @Override
    public TableField<Record, ?> getId() {
        return colId;
    }

    public TableField<Record, ?> getPartyId() {
        return colPartyId;
    }

    public TableField<Record, ?> getLicenseId() {
        return colLicenseId;
    }

    @Override
    public TableImpProject as(Name alias) {
        return new TableImpProject(alias, this, pluginPLUS, pluginCoreModel, pluginMultiDatastream).initCustomFields();
    }

    @Override
    public StaMainTable<TableImpProject> asSecure(String name, JooqPersistenceManager pm) {
        final SecurityTableWrapper securityWrapper = getSecurityWrapper();
        if (securityWrapper == null) {
            return as(name);
        }
        final Table wrappedTable = securityWrapper.wrap(this, pm);
        return new TableImpProject(DSL.name(name), this, wrappedTable, pluginPLUS, pluginCoreModel, pluginMultiDatastream);
    }

    @Override
    public TableImpProject getThis() {
        return this;
    }

}
