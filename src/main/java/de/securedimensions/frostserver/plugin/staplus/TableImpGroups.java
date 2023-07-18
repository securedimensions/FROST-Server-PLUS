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
import de.fraunhofer.iosb.ilt.frostserver.plugin.coremodel.TableImpObservations;
import de.fraunhofer.iosb.ilt.frostserver.plugin.multidatastream.PluginMultiDatastream;
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
 * @author am
 * @author scf
 */
public class TableImpGroups extends StaTableAbstract<TableImpGroups> {

    /**
     * The column <code>public.GROUPS.EP_DESCRIPTION</code>.
     */
    public final TableField<Record, String> colDescription = createField(DSL.name("DESCRIPTION"), SQLDataType.CLOB, this);

    /**
     * The column <code>public.GROUPS.EP_NAME</code>.
     */
    public final TableField<Record, String> colName = createField(DSL.name("NAME"), SQLDataType.CLOB.defaultValue(DSL.field("'no name'::text", SQLDataType.CLOB)), this);

    /**
     * The column <code>public.GROUPS.EP_PROPERTIES</code>.
     */
    public final TableField<Record, JsonValue> colProperties = createField(DSL.name("PROPERTIES"), DefaultDataType.getDefaultDataType(TYPE_JSONB), this, "", new JsonBinding());

    /**
     * The column <code>public.GROUPS.EP_CLASSIFICATION</code>.
     */
    public final TableField<Record, String> colPurpose = createField(DSL.name("PURPOSE"), SQLDataType.CLOB, this);

    /**
     * The column <code>public.GROUPS.EP_END_TIME</code>.
     */
    public final TableField<Record, Moment> colEndTime = createField(DSL.name("END_TIME"), SQLDataType.TIMESTAMP, this, "", new MomentBinding());

    /**
     * The column <code>public.GROUPS.EP_CREATION_TIME</code>.
     */
    public final TableField<Record, Moment> colCreationTime = createField(DSL.name("CREATION_TIME"), SQLDataType.TIMESTAMP, this, "", new MomentBinding());

    /**
     * The column <code>public.GROUPS.EP_DATA_QUALITY</code>.
     */
    public final TableField<Record, JsonValue> colDataQuality = createField(DSL.name("DATA_QUALITY"), DefaultDataType.getDefaultDataType(TYPE_JSONB), this, "", new JsonBinding());

    /**
     * The column <code>public.GROUPS.EP_TERMS_OF_USE</code>.
     */
    public final TableField<Record, String> colTermsOfUse = createField(DSL.name("TERMS_OF_USE"), SQLDataType.CLOB, this);

    /**
     * The column <code>public.GROUPS.EP_PRIVACY_POLICY</code>.
     */
    public final TableField<Record, String> colPrivacyPolicy = createField(DSL.name("PRIVACY_POLICY"), SQLDataType.CLOB, this);

    /**
     * The column <code>public.GROUPS.EP_ID</code>.
     */
    public final TableField<Record, ?> colId = createField(DSL.name("ID"), getIdType(), this);

    public final TableField<Record, ?> colPartyId;

    public final TableField<Record, ?> colLicenseId;
    private final PluginPLUS pluginPLUS;
    private final PluginCoreModel pluginCoreModel;
    private final PluginMultiDatastream pluginMultiDatastream;

    /**
     * Create a <code>public.GROUPS</code> table reference.
     *
     * @param idType The (SQL)DataType of the Id columns used in the actual
     * database.
     * @param pluginPLUS
     * @param pluginCoreModel the coreModel plugin that this data model links
     * to.
     * @param pluginMultiDatastream
     */
    public TableImpGroups(DataType<?> idType, DataType<?> idTypeParty, DataType<?> idTypeLicense, PluginPLUS pluginPLUS, PluginCoreModel pluginCoreModel, PluginMultiDatastream pluginMultiDatastream) {
        //StaTableAbstract(DataType<?> idType, Name alias, StaTableAbstract<T> aliasedBase, Table updatedSql)
        super(idType, DSL.name("GROUPS"), null, null);
        this.pluginPLUS = pluginPLUS;
        this.pluginCoreModel = pluginCoreModel;
        this.pluginMultiDatastream = pluginMultiDatastream;

        colPartyId = createField(DSL.name("PARTY_ID"), idTypeParty.nullable(true));
        colLicenseId = createField(DSL.name("LICENSE_ID"), idTypeLicense.nullable(true));
    }

    private TableImpGroups(Name alias, TableImpGroups aliased, PluginPLUS pluginPLUS, PluginCoreModel pluginCoreModel, PluginMultiDatastream pluginMultiDatastream) {
        this(alias, aliased, aliased, pluginPLUS, pluginCoreModel, pluginMultiDatastream);
    }

    private TableImpGroups(Name alias, TableImpGroups aliased, Table updatedSql, PluginPLUS pluginPLUS, PluginCoreModel pluginCoreModel, PluginMultiDatastream pluginMultiDatastream) {
        super(aliased.getIdType(), alias, aliased, updatedSql);
        this.pluginPLUS = pluginPLUS;
        this.pluginCoreModel = pluginCoreModel;
        this.pluginMultiDatastream = pluginMultiDatastream;

        colPartyId = createField(DSL.name("PARTY_ID"), aliased.colPartyId.getDataType().nullable(true));
        colLicenseId = createField(DSL.name("LICENSE_ID"), aliased.colLicenseId.getDataType().nullable(true));
    }

    @Override
    public void initRelations() {
        final TableCollection tables = getTables();

        initObservationGroups(tables);
        initGroupsRelations(tables);
        initGroupsProjects(tables);

        // We add the relation to Party table.
        registerRelation(new RelationOneToMany<>(pluginPLUS.npPartyGroup, this, tables.getTableForClass(TableImpParty.class))
                .setSourceFieldAccessor(TableImpGroups::getPartyId)
                .setTargetFieldAccessor(TableImpParty::getId));

        // We add the relation to License table.
        registerRelation(new RelationOneToMany<>(pluginPLUS.npLicenseGroup, this, tables.getTableForClass(TableImpLicense.class))
                .setSourceFieldAccessor(TableImpGroups::getLicenseId)
                .setTargetFieldAccessor(TableImpLicense::getId));
    }

    private void initObservationGroups(TableCollection tables) {
        final TableImpGroupsObservations tableGroupObservations = tables.getTableForClass(TableImpGroupsObservations.class);
        TableImpObservations tableObservations = tables.getTableForClass(TableImpObservations.class);

        registerRelation(new RelationManyToMany<>(pluginPLUS.npObservationsGroup, this, tableGroupObservations, tableObservations)
                .setSourceFieldAcc(TableImpGroups::getId)
                .setSourceLinkFieldAcc(TableImpGroupsObservations::getGroupId)
                .setTargetLinkFieldAcc(TableImpGroupsObservations::getObservationId)
                .setTargetFieldAcc(TableImpObservations::getId));
        tableObservations.registerRelation(new RelationManyToMany<>(pluginPLUS.npObservationGroups, tableObservations, tableGroupObservations, this)
                .setSourceFieldAcc(TableImpObservations::getId)
                .setSourceLinkFieldAcc(TableImpGroupsObservations::getObservationId)
                .setTargetLinkFieldAcc(TableImpGroupsObservations::getGroupId)
                .setTargetFieldAcc(TableImpGroups::getId));

    }

    private void initGroupsRelations(TableCollection tables) {
        final TableImpGroupsRelations tableGroupRelations = tables.getTableForClass(TableImpGroupsRelations.class);
        TableImpRelations tableRelations = tables.getTableForClass(TableImpRelations.class);

        registerRelation(new RelationManyToMany<>(pluginPLUS.npRelationsGroup, this, tableGroupRelations, tableRelations)
                .setSourceFieldAcc(TableImpGroups::getId)
                .setSourceLinkFieldAcc(TableImpGroupsRelations::getGroupId)
                .setTargetLinkFieldAcc(TableImpGroupsRelations::getRelationId)
                .setTargetFieldAcc(TableImpRelations::getId));
        tableRelations.registerRelation(new RelationManyToMany<>(pluginPLUS.npRelationGroups, tableRelations, tableGroupRelations, this)
                .setSourceFieldAcc(TableImpRelations::getId)
                .setSourceLinkFieldAcc(TableImpGroupsRelations::getRelationId)
                .setTargetLinkFieldAcc(TableImpGroupsRelations::getGroupId)
                .setTargetFieldAcc(TableImpGroups::getId));

    }

    private void initGroupsProjects(TableCollection tables) {
        final TableImpGroupsProjects tableGroupProjects = tables.getTableForClass(TableImpGroupsProjects.class);
        TableImpProject tableProjects = tables.getTableForClass(TableImpProject.class);

        registerRelation(new RelationManyToMany<>(pluginPLUS.npProjectsGroup, this, tableGroupProjects, tableProjects)
                .setSourceFieldAcc(TableImpGroups::getId)
                .setSourceLinkFieldAcc(TableImpGroupsProjects::getGroupId)
                .setTargetLinkFieldAcc(TableImpGroupsProjects::getProjectId)
                .setTargetFieldAcc(TableImpProject::getId));
        tableProjects.registerRelation(new RelationManyToMany<>(pluginPLUS.npGroupsProject, tableProjects, tableGroupProjects, this)
                .setSourceFieldAcc(TableImpProject::getId)
                .setSourceLinkFieldAcc(TableImpGroupsProjects::getProjectId)
                .setTargetLinkFieldAcc(TableImpGroupsProjects::getGroupId)
                .setTargetFieldAcc(TableImpGroups::getId));

    }

    @Override
    public void initProperties(final EntityFactories entityFactories) {
        final TableCollection tables = getTables();
        pfReg.addEntryId(TableImpGroups::getId);
        pfReg.addEntryString(pluginCoreModel.epName, table -> table.colName);
        pfReg.addEntryString(pluginCoreModel.epDescription, table -> table.colDescription);
        pfReg.addEntryMap(ModelRegistry.EP_PROPERTIES, table -> table.colProperties);
        pfReg.addEntryString(pluginPLUS.epGroupPurpose, table -> table.colPurpose);
        pfReg.addEntryString(pluginPLUS.epProjectTermsOfUse, table -> table.colTermsOfUse);
        pfReg.addEntryString(pluginPLUS.epProjectPrivacyPolicy, table -> table.colPrivacyPolicy);
        pfReg.addEntryMap(pluginPLUS.epGroupDataQuality, table -> table.colDataQuality);

        pfReg.addEntry(pluginPLUS.epGroupCreationTime, table -> table.colCreationTime,
                new ConverterTimeInstant<>(pluginPLUS.epGroupCreationTime, table -> table.colCreationTime));
        pfReg.addEntry(pluginPLUS.epGroupEndTime, table -> table.colEndTime,
                new ConverterTimeInstant<>(pluginPLUS.epGroupEndTime, table -> table.colEndTime));

        // Register with Observations
        pfReg.addEntry(pluginPLUS.npObservationsGroup, TableImpGroups::getId);

        TableImpObservations tableObservations = tables.getTableForClass(TableImpObservations.class);
        tableObservations.getPropertyFieldRegistry()
                .addEntry(pluginPLUS.npObservationGroups, TableImpObservations::getId);

        // Register with Relations
        pfReg.addEntry(pluginPLUS.npRelationsGroup, TableImpGroups::getId);

        // Register with Parties
        pfReg.addEntry(pluginPLUS.npPartyGroup, TableImpGroups::getPartyId);

        // Register with Licenses
        pfReg.addEntry(pluginPLUS.npLicenseGroup, TableImpGroups::getLicenseId);

        // Register with Groups
        pfReg.addEntry(pluginPLUS.npProjectsGroup, TableImpGroups::getId);
    }

    @Override
    public EntityType getEntityType() {
        return pluginPLUS.etGroup;
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
    public TableImpGroups as(Name alias) {
        return new TableImpGroups(alias, this, pluginPLUS, pluginCoreModel, pluginMultiDatastream).initCustomFields();
    }

    @Override
    public StaMainTable<TableImpGroups> asSecure(String name, JooqPersistenceManager pm) {
        final SecurityTableWrapper securityWrapper = getSecurityWrapper();
        if (securityWrapper == null) {
            return as(name);
        }
        final Table wrappedTable = securityWrapper.wrap(this, pm);
        return new TableImpGroups(DSL.name(name), this, wrappedTable, pluginPLUS, pluginCoreModel, pluginMultiDatastream);
    }

    @Override
    public TableImpGroups getThis() {
        return this;
    }

}
