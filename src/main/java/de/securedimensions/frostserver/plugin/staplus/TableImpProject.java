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

import static de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.fieldwrapper.StaTimeIntervalWrapper.KEY_TIME_INTERVAL_END;
import static de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.fieldwrapper.StaTimeIntervalWrapper.KEY_TIME_INTERVAL_START;

import de.fraunhofer.iosb.ilt.frostserver.model.EntityType;
import de.fraunhofer.iosb.ilt.frostserver.model.ModelRegistry;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.bindings.JsonBinding;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.bindings.JsonValue;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.bindings.MomentBinding;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.factories.EntityFactories;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.relations.RelationOneToMany;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.tables.StaMainTable;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.tables.StaTableAbstract;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.tables.TableCollection;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.utils.PropertyFieldRegistry.ConverterTimeInstant;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.utils.PropertyFieldRegistry.ConverterTimeInterval;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.utils.PropertyFieldRegistry.NFP;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.utils.validator.SecurityTableWrapper;
import de.fraunhofer.iosb.ilt.frostserver.plugin.coremodel.PluginCoreModel;
import de.fraunhofer.iosb.ilt.frostserver.plugin.coremodel.TableImpDatastreams;
import de.fraunhofer.iosb.ilt.frostserver.plugin.multidatastream.TableImpMultiDatastreams;
import net.time4j.Moment;
import org.jooq.DataType;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableLike;
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
     * The column <code>public.PROJECTS.EP_DESCRIPTION</code>.
     */
    public final TableField<Record, String> colDescription = createField(DSL.name("DESCRIPTION"), SQLDataType.CLOB, this);

    /**
     * The column <code>public.PROJECTS.EP_NAME</code>.
     */
    public final TableField<Record, String> colName = createField(DSL.name("NAME"), SQLDataType.CLOB.defaultValue(DSL.field("'no name'::text", SQLDataType.CLOB)), this);

    /**
     * The column <code>public.PROJECTS.EP_PROPERTIES</code>.
     */
    public final TableField<Record, JsonValue> colProperties = createField(DSL.name("PROPERTIES"), DefaultDataType.getDefaultDataType(TYPE_JSONB), this, "", new JsonBinding());

    /**
     * The column <code>public.PROJECTS.EP_CLASSIFICATION</code>.
     */
    public final TableField<Record, String> colClassification = createField(DSL.name("CLASSIFICATION"), SQLDataType.CLOB, this);

    /**
     * The column <code>public.PROJECTS.EP_TERMSOFUSE</code>.
     */
    public final TableField<Record, String> colTermsOfUse = createField(DSL.name("TERMSOFUSE"), SQLDataType.CLOB, this);

    /**
     * The column <code>public.PROJECTS.EP_PRIVACYPOLICY</code>.
     */
    public final TableField<Record, String> colPrivacyPolicy = createField(DSL.name("PRIVACYPOLICY"), SQLDataType.CLOB, this);

    /**
     * The column <code>public.PROJECTS.EP_URL</code>.
     */
    public final TableField<Record, String> colUrl = createField(DSL.name("URL"), SQLDataType.CLOB, this);

    /**
     * The column <code>public.PROJECTS.RUNTIME_START</code>.
     */
    public final TableField<Record, Moment> colRuntimeTimeStart = createField(DSL.name("RUNTIME_START"), SQLDataType.TIMESTAMP, this, "", new MomentBinding());

    /**
     * The column <code>public.PROJECTS.RUNTIME_END</code>.
     */
    public final TableField<Record, Moment> colRuntimeTimeEnd = createField(DSL.name("RUNTIME_END"), SQLDataType.TIMESTAMP, this, "", new MomentBinding());

    /**
     * The column <code>public.PROJECTS.CREATED_TIME</code>.
     */
    public final TableField<Record, Moment> colCreatedTime = createField(DSL.name("CREATED"), SQLDataType.TIMESTAMP, this, "", new MomentBinding());

    /**
     * The column <code>public.PROJECTS.EP_ID</code>.
     */
    public final TableField<Record, ?> colId = createField(DSL.name("ID"), getIdType(), this);

    private final PluginPLUS pluginPLUS;
    private final PluginCoreModel pluginCoreModel;

    /**
     * Create a <code>public.PROJECTS</code> table reference.
     *
     * @param idType The (SQL)DataType of the Id columns used in the actual
     * database.
     * @param pluginProject the party plugin this table belongs to.
     * @param pluginCoreModel the coreModel plugin that this data model links
     * to.
     */
    public TableImpProject(DataType<?> idType, PluginPLUS pluginProject, PluginCoreModel pluginCoreModel) {
        super(idType, DSL.name("PROJECTS"), null, null);
        this.pluginPLUS = pluginProject;
        this.pluginCoreModel = pluginCoreModel;
    }

    private TableImpProject(Name alias, TableImpProject aliased, PluginPLUS pluginProject, PluginCoreModel pluginCoreModel) {
        this(alias, aliased, aliased, pluginProject, pluginCoreModel);
    }

    private TableImpProject(Name alias, TableImpProject aliased, Table updatedSql, PluginPLUS pluginProject, PluginCoreModel pluginCoreModel) {
        super(aliased.getIdType(), alias, aliased, updatedSql);
        this.pluginPLUS = pluginProject;
        this.pluginCoreModel = pluginCoreModel;
    }

    @Override
    public void initRelations() {
        final TableCollection tables = getTables();

        TableImpDatastreams tableDatastreams = tables.getTableForClass(TableImpDatastreams.class);
        final int projectIdIdx = tableDatastreams.indexOf("PROJECT_ID");

        // Add relation to Datastreams table
        registerRelation(new RelationOneToMany<>(pluginPLUS.npDatastreamsProject, this, tableDatastreams)
                .setSourceFieldAccessor(TableImpProject::getId)
                .setTargetFieldAccessor(table -> (TableField<Record, ?>) table.field(projectIdIdx)));

        // We add the relation to us from the Datastreams table.
        tableDatastreams.registerRelation(new RelationOneToMany<>(pluginPLUS.npProjectDatastream, tableDatastreams, this)
                .setSourceFieldAccessor(table -> (TableField<Record, ?>) table.field(projectIdIdx))
                .setTargetFieldAccessor(TableImpProject::getId));

        TableImpMultiDatastreams tableMultiDatastreams = tables.getTableForClass(TableImpMultiDatastreams.class);
        if (tableMultiDatastreams != null) {
            // We add the relation to MultiDatastreams table
            final int projectMDIdIdx = tableMultiDatastreams.indexOf("PROJECT_ID");
            registerRelation(new RelationOneToMany<>(pluginPLUS.npMultiDatastreamsProject, this, tableMultiDatastreams)
                    .setSourceFieldAccessor(TableImpProject::getId)
                    .setTargetFieldAccessor(table -> (TableField<Record, ?>) table.field(projectMDIdIdx)));

            // We add the relation to us from the MultiDatastreams table.
            tableMultiDatastreams.registerRelation(new RelationOneToMany<>(pluginPLUS.npProjectMultiDatastream, tableMultiDatastreams, this)
                    .setSourceFieldAccessor(table -> (TableField<Record, ?>) table.field(projectMDIdIdx))
                    .setTargetFieldAccessor(TableImpProject::getId));
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

        pfReg.addEntry(pluginPLUS.epProjectCreationTime, table -> table.colCreatedTime,
                new ConverterTimeInstant<>(pluginPLUS.epProjectCreationTime, table -> table.colCreatedTime));
        pfReg.addEntry(pluginPLUS.epProjectStartTime,
                new ConverterTimeInterval<>(pluginPLUS.epProjectStartTime, table -> table.colRuntimeTimeStart, table -> table.colRuntimeTimeEnd),
                new NFP<>(KEY_TIME_INTERVAL_START, table -> table.colRuntimeTimeStart),
                new NFP<>(KEY_TIME_INTERVAL_END, table -> table.colRuntimeTimeEnd));

        pfReg.addEntry(pluginPLUS.npDatastreamsProject, TableImpProject::getId);
        pfReg.addEntry(pluginPLUS.npMultiDatastreamsProject, TableImpProject::getId);

        // We register a navigationProperty on the Datastreams table.
        TableImpDatastreams tableDatastreams = tables.getTableForClass(TableImpDatastreams.class);
        final int projectIdIdx = tableDatastreams.registerField(DSL.name("PROJECT_ID"), getIdType());
        tableDatastreams.getPropertyFieldRegistry()
                .addEntry(pluginPLUS.npProjectDatastream, table -> (TableField<Record, ?>) table.field(projectIdIdx));

        TableImpMultiDatastreams tableMultiDatastreams = tables.getTableForClass(TableImpMultiDatastreams.class);
        if (tableMultiDatastreams != null) {
            final int projectMDIdIdx = tableMultiDatastreams.registerField(DSL.name("PROJECT_ID"), getIdType());
            tableMultiDatastreams.getPropertyFieldRegistry()
                    .addEntry(pluginPLUS.npProjectMultiDatastream, table -> (TableField<Record, ?>) ((TableLike<Record>) table).field(projectMDIdIdx));
        }

    }

    @Override
    public EntityType getEntityType() {
        return pluginPLUS.etProject;
    }

    @Override
    public TableField<Record, ?> getId() {
        return colId;
    }

    @Override
    public TableImpProject as(Name alias) {
        return new TableImpProject(alias, this, pluginPLUS, pluginCoreModel).initCustomFields();
    }

    @Override
    public StaMainTable<TableImpProject> asSecure(String name) {
        final SecurityTableWrapper securityWrapper = getSecurityWrapper();
        if (securityWrapper == null) {
            return as(name);
        }
        final Table wrappedTable = securityWrapper.wrap(this);
        return new TableImpProject(DSL.name(name), this, wrappedTable, pluginPLUS, pluginCoreModel);
    }

    @Override
    public TableImpProject getThis() {
        return this;
    }

}
