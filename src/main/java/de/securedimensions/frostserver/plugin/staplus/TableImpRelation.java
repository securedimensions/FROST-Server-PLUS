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
import de.fraunhofer.iosb.ilt.frostserver.model.ModelRegistry;
import de.fraunhofer.iosb.ilt.frostserver.model.core.Entity;
import de.fraunhofer.iosb.ilt.frostserver.model.core.EntitySet;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.JooqPersistenceManager;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.bindings.JsonBinding;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.bindings.JsonValue;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.factories.EntityFactories;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.relations.RelationOneToMany;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.tables.StaMainTable;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.tables.StaTableAbstract;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.tables.TableCollection;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.utils.validator.SecurityTableWrapper;
import de.fraunhofer.iosb.ilt.frostserver.plugin.coremodel.PluginCoreModel;
import de.fraunhofer.iosb.ilt.frostserver.plugin.coremodel.TableImpObservations;
import de.fraunhofer.iosb.ilt.frostserver.service.UpdateMode;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.IncompleteEntityException;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.NoSuchEntityException;
import java.util.Arrays;
import java.util.List;
import org.jooq.*;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultDataType;
import org.jooq.impl.SQLDataType;

public class TableImpRelation extends StaTableAbstract<TableImpRelation> {

    /**
     * The column <code>public.GROUPS.EP_DESCRIPTION</code>.
     */
    public final TableField<Record, String> colDescription = createField(DSL.name("DESCRIPTION"), SQLDataType.CLOB, this);

    /**
     * The column <code>public.GROUPS.EP_PROPERTIES</code>.
     */
    public final TableField<Record, JsonValue> colProperties = createField(DSL.name("PROPERTIES"), DefaultDataType.getDefaultDataType(TYPE_JSONB), this, "", JsonBinding.instance());

    /**
     * The column <code>public.RELATIONS.EP_ROLE</code>.
     */
    public final TableField<Record, String> colRole = createField(DSL.name("ROLE"), SQLDataType.CLOB, this);

    /**
     * The column <code>public.RELATIONS.EP_EXTERNAL_RESOURCE</code>.
     */
    public final TableField<Record, String> colExternalObject = createField(DSL.name("EXTERNAL_RESOURCE"), SQLDataType.CLOB, this);

    /**
     * The column <code>public.RELATIONS.EP_ID</code>.
     */
    public final TableField<Record, ?> colId = createField(DSL.name("ID"), getIdType(), this);

    /**
     * The column <code>public.RELATIONS.EP_SUBJECT_ID</code>.
     */
    public final TableField<Record, ?> colSubjectId;

    /**
     * The column <code>public.RELATIONS.EP_OBJECT_ID</code>.
     */
    public final TableField<Record, ?> colObjectId;

    /**
     * The column <code>public.RELATIONS.EP_GROUP_ID</code>.
     */
    public final TableField<Record, ?> colGroupId;

    private final PluginPLUS pluginPLUS;
    private final PluginCoreModel pluginCoreModel;

    /**
     * Create a <code>public.TASKS</code> table reference.
     *
     * @param idType The (SQL)DataType of the Id column used in the actual
     * database.
     * @param idTypeObs The (SQL)DataType of the Subject and Object column used
     * in the actual database.
     * @param idTypeGroup The (SQL)DataType of the GroupId column used in the
     * actual database.
     * @param pluginCoreModel the coreModel plugin that this data model links
     * to.
     */
    public TableImpRelation(DataType<?> idType, DataType<?> idTypeObs, DataType<?> idTypeGroup, PluginPLUS pluginGrouping, PluginCoreModel pluginCoreModel) {
        super(idType, DSL.name("RELATIONS"), null, null);
        this.pluginPLUS = pluginGrouping;
        this.pluginCoreModel = pluginCoreModel;
        colSubjectId = createField(DSL.name("SUBJECT_ID"), idTypeObs);
        colObjectId = createField(DSL.name("OBJECT_ID"), idTypeObs);
        colGroupId = createField(DSL.name("GROUP_ID"), idTypeGroup);
    }

    private TableImpRelation(Name alias, TableImpRelation aliased, PluginPLUS pluginGrouping, PluginCoreModel pluginCoreModel) {
        this(alias, aliased, aliased, pluginGrouping, pluginCoreModel);
    }

    private TableImpRelation(Name alias, TableImpRelation aliased, Table updatedSql, PluginPLUS pluginGrouping, PluginCoreModel pluginCoreModel) {
        super(aliased.getIdType(), alias, aliased, updatedSql);
        this.pluginPLUS = pluginGrouping;
        this.pluginCoreModel = pluginCoreModel;
        colSubjectId = createField(DSL.name("SUBJECT_ID"), aliased.colSubjectId.getDataType());
        colObjectId = createField(DSL.name("OBJECT_ID"), aliased.colObjectId.getDataType());
        colGroupId = createField(DSL.name("GROUP_ID"), aliased.colGroupId.getDataType());
    }

    @Override
    public void initRelations() {
        final TableCollection tables = getTables();
        final TableImpObservations tableObservations = tables.getTableForClass(TableImpObservations.class);

        registerRelation(new RelationOneToMany<>(pluginPLUS.npSubjectRelation, this, tableObservations)
                .setSourceFieldAccessor(TableImpRelation::getSubjectId)
                .setTargetFieldAccessor(TableImpObservations::getId));
        tableObservations.registerRelation(new RelationOneToMany<>(pluginPLUS.npObjectsObservation, tableObservations, this)
                .setSourceFieldAccessor(TableImpObservations::getId)
                .setTargetFieldAccessor(TableImpRelation::getSubjectId));

        registerRelation(new RelationOneToMany<>(pluginPLUS.npObjectRelation, this, tableObservations)
                .setSourceFieldAccessor(TableImpRelation::getObjectId)
                .setTargetFieldAccessor(TableImpObservations::getId));
        tableObservations.registerRelation(new RelationOneToMany<>(pluginPLUS.npSubjectsObservation, tableObservations, this)
                .setSourceFieldAccessor(TableImpObservations::getId)
                .setTargetFieldAccessor(TableImpRelation::getObjectId));

    }

    @Override
    public void initProperties(final EntityFactories entityFactories) {
        final TableCollection tables = getTables();
        final TableImpObservations tableObservations = tables.getTableForClass(TableImpObservations.class);
        pfReg.addEntryId(TableImpRelation::getId);
        pfReg.addEntryString(pluginPLUS.epRelationDescription, table -> table.colDescription);
        pfReg.addEntryMap(ModelRegistry.EP_PROPERTIES, table -> table.colProperties);

        pfReg.addEntryString(pluginPLUS.epRelationRole, table -> table.colRole);
        pfReg.addEntryString(pluginPLUS.epExternalObject, table -> table.colExternalObject);

        pfReg.addEntry(pluginPLUS.npSubjectRelation, TableImpRelation::getSubjectId);
        pfReg.addEntry(pluginPLUS.npObjectRelation, TableImpRelation::getObjectId);

        pfReg.addEntry(pluginPLUS.npRelationGroups, TableImpRelation::getId);

        // We register a navigationProperty for Subject on the Observations table.
        tableObservations.getPropertyFieldRegistry()
                .addEntry(pluginPLUS.npSubjectsObservation, TableImpObservations::getId);
        // We register a navigationProperty for Object on the Observations table.
        tableObservations.getPropertyFieldRegistry()
                .addEntry(pluginPLUS.npObjectsObservation, TableImpObservations::getId);

    }

    @Override
    protected void updateNavigationPropertySet(Entity group, EntitySet linkedSet, JooqPersistenceManager pm, UpdateMode updateMode) throws IncompleteEntityException, NoSuchEntityException {
        super.updateNavigationPropertySet(group, linkedSet, pm, updateMode);
    }

    @Override
    public EntityType getEntityType() {
        return pluginPLUS.etRelation;
    }

    @Override
    public List<Field> getPkFields() {
        return Arrays.asList(colId);
    }

    public TableField<Record, ?> getId() {
        return colId;
    }

    public TableField<Record, ?> getSubjectId() {
        return colSubjectId;
    }

    public TableField<Record, ?> getObjectId() {
        return colObjectId;
    }

    public TableField<Record, ?> getGroupId() {
        return colGroupId;
    }

    @Override
    public TableImpRelation as(Name alias) {
        return new TableImpRelation(alias, this, pluginPLUS, pluginCoreModel).initCustomFields();
    }

    @Override
    public StaMainTable<TableImpRelation> asSecure(String name, JooqPersistenceManager pm) {
        final SecurityTableWrapper securityWrapper = getSecurityWrapper();
        if (securityWrapper == null) {
            return as(name);
        }
        final Table wrappedTable = securityWrapper.wrap(this, pm);
        return new TableImpRelation(DSL.name(name), this, wrappedTable, pluginPLUS, pluginCoreModel);
    }

    @Override
    public TableImpRelation getThis() {
        return this;
    }

}
