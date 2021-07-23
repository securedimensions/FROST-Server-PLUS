package de.securedimensions.frostserver.plugin.plus;

import de.fraunhofer.iosb.ilt.frostserver.model.EntityType;
import de.fraunhofer.iosb.ilt.frostserver.model.core.Entity;
import de.fraunhofer.iosb.ilt.frostserver.model.core.EntitySet;
import de.fraunhofer.iosb.ilt.frostserver.persistence.IdManager;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.PostgresPersistenceManager;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.factories.EntityFactories;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.relations.RelationManyToMany;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.relations.RelationOneToMany;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.tables.StaTableAbstract;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.tables.TableCollection;
import de.fraunhofer.iosb.ilt.frostserver.plugin.coremodel.PluginCoreModel;
import de.fraunhofer.iosb.ilt.frostserver.plugin.coremodel.TableImpObservations;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.IncompleteEntityException;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.NoSuchEntityException;

import java.util.ArrayList;
import java.util.List;

import org.jooq.DSLContext;
import org.jooq.DataType;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.TableField;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TableImpRelations<J extends Comparable> extends StaTableAbstract<J, TableImpRelations<J>> {

	private static final Logger LOGGER = LoggerFactory.getLogger(TableImpGroups.class.getName());

    private static final long serialVersionUID = 1626971259;

    /**
     * The column <code>public.RELATIONS.ROLE</code>.
     */
    public final TableField<Record, String> colRole = createField(DSL.name("ROLE"), SQLDataType.CLOB, this);

    /**
     * The column <code>public.RELATIONS.EP_NAMESPACE</code>.
     */
    public final TableField<Record, String> colNamespace = createField(DSL.name("NAMESPACE"), SQLDataType.CLOB, this);

    /**
     * The column <code>public.RELATIONS.EP_ID</code>.
     */
    public final TableField<Record, J> colId = createField(DSL.name("ID"), getIdType(), this);

    /**
     * The column <code>public.RELATIONS.EP_SUBJECT_ID</code>.
     */
    public final TableField<Record, J> colSubjectId = createField(DSL.name("SUBJECT_ID"), getIdType(), this);

    /**
     * The column <code>public.RELATIONS.EP_OBJECT_ID</code>.
     */
    public final TableField<Record, J> colObjectId = createField(DSL.name("OBJECT_ID"), getIdType(), this);

    /**
     * The column <code>public.RELATIONS.EP_GROUP_ID</code>.
     */
    public final TableField<Record, J> colGroupId = createField(DSL.name("GROUP_ID"), getIdType(), this);

    private final PluginPLUS pluginPLUS;
    private final PluginCoreModel pluginCoreModel;

    /**
     * Create a <code>public.TASKS</code> table reference.
     *
     * @param idType The (SQL)DataType of the Id columns used in the actual
     * database.
     * @param pluginActuation the actuation plugin this table belongs to.
     * @param pluginCoreModel the coreModel plugin that this data model links
     * to.
     */
    public TableImpRelations(DataType<J> idType, PluginPLUS pluginGrouping, PluginCoreModel pluginCoreModel) {
        super(idType, DSL.name("RELATIONS"), null);
        this.pluginPLUS = pluginGrouping;
        this.pluginCoreModel = pluginCoreModel;
    }

    private TableImpRelations(Name alias, TableImpRelations<J> aliased, PluginPLUS pluginGrouping, PluginCoreModel pluginCoreModel) {
        super(aliased.getIdType(), alias, aliased);
        this.pluginPLUS = pluginGrouping;
        this.pluginCoreModel = pluginCoreModel;
    }

    @Override
    public void initRelations() {
        final TableCollection<J> tables = getTables();
        final TableImpObservations<J> tableObservations = tables.getTableForClass(TableImpObservations.class);

        registerRelation(new RelationOneToMany<>(pluginPLUS.npSubjectRelation, this, tableObservations)
                .setSourceFieldAccessor(TableImpRelations::getSubjectId)
                .setTargetFieldAccessor(TableImpObservations::getId)
        );
        tableObservations.registerRelation(new RelationOneToMany<>(pluginPLUS.npSubjectsObservation, tableObservations, this)
                .setSourceFieldAccessor(TableImpObservations::getId)
                .setTargetFieldAccessor(TableImpRelations::getSubjectId)
        );

        registerRelation(new RelationOneToMany<>(pluginPLUS.npObjectRelation, this, tableObservations)
                .setSourceFieldAccessor(TableImpRelations::getObjectId)
                .setTargetFieldAccessor(TableImpObservations::getId)
        );
        tableObservations.registerRelation(new RelationOneToMany<>(pluginPLUS.npObjectsObservation, tableObservations, this)
                .setSourceFieldAccessor(TableImpObservations::getId)
                .setTargetFieldAccessor(TableImpRelations::getObjectId)
        );
        
    }

    @Override
    public void initProperties(final EntityFactories<J> entityFactories) {
        final TableCollection<J> tables = getTables();
        final TableImpObservations<J> tableObservations = tables.getTableForClass(TableImpObservations.class);
        final IdManager idManager = entityFactories.getIdManager();
        pfReg.addEntryId(idManager, TableImpRelations::getId);
        pfReg.addEntryString(pluginPLUS.epRelationRole, table -> table.colRole);
        pfReg.addEntryString(pluginPLUS.epNamespace, table -> table.colNamespace);

        pfReg.addEntry(pluginPLUS.npSubjectRelation, TableImpRelations::getSubjectId, idManager);
        pfReg.addEntry(pluginPLUS.npObjectRelation, TableImpRelations::getObjectId, idManager);
        
        // We register a navigationProperty for Subject on the Observations table.
        tableObservations.getPropertyFieldRegistry()
                .addEntry(pluginPLUS.npSubjectsObservation, TableImpObservations::getId, idManager);
        // We register a navigationProperty for Object on the Observations table.
        tableObservations.getPropertyFieldRegistry()
                .addEntry(pluginPLUS.npObjectsObservation, TableImpObservations::getId, idManager);
        
    }

    @Override
    protected void updateNavigationPropertySet(Entity group, EntitySet linkedSet, PostgresPersistenceManager<J> pm, boolean forInsert) throws IncompleteEntityException, NoSuchEntityException {
        EntityType linkedEntityType = linkedSet.getEntityType();

        // Todo ???
        
        super.updateNavigationPropertySet(group, linkedSet, pm, forInsert);
    }

    @Override
    public EntityType getEntityType() {
        return pluginPLUS.etRelation;
    }

    @Override
    public TableField<Record, J> getId() {
        return colId;
    }

    public TableField<Record, J> getSubjectId() {
        return colSubjectId;
    }

    public TableField<Record, J> getObjectId() {
        return colObjectId;
    }

    public TableField<Record, J> getGroupId() {
        return colGroupId;
    }

    @Override
    public TableImpRelations<J> as(Name alias) {
        return new TableImpRelations<>(alias, this, pluginPLUS, pluginCoreModel).initCustomFields();
    }

    @Override
    public TableImpRelations<J> getThis() {
        return this;
    }

}
