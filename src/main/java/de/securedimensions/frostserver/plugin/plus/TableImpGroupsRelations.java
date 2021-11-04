package de.securedimensions.frostserver.plugin.plus;

import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.tables.StaLinkTable;
import org.jooq.DataType;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.TableField;
import org.jooq.impl.DSL;

public class TableImpGroupsRelations extends StaLinkTable<TableImpGroupsRelations> {

    public static final String NAME_TABLE = "GROUPS_RELATIONS";
    public static final String NAME_COL_TL_RELATION_ID = "RELATION_ID";
    public static final String NAME_COL_TL_GROUP_ID = "GROUP_ID";

    private static final long serialVersionUID = 1626971356;

    /**
     * The column <code>public.GROUPS_RELATIONS.GROUP_ID</code>.
     */
    public final TableField<Record, ?> colGroupId;

    /**
     * The column <code>public.GROUPS_RELATIONS.RELATION_ID</code>.
     */
    public final TableField<Record, ?> colRelationId;

    /**
     * Create a <code>public.GROUPS_OBSERVATIONS</code> table reference.
     *
     * @param idTypeGroup The (SQL)DataType of the GroupId column used in the
     * actual database.
     * @param idTypeRelation The (SQL)DataType of the RelationId column used in
     * the actual database.
     */
    public TableImpGroupsRelations(DataType<?> idTypeGroup, DataType<?> idTypeRelation) {
        super(DSL.name(NAME_TABLE), null);
        colGroupId = createField(DSL.name(NAME_COL_TL_GROUP_ID), idTypeGroup);
        colRelationId = createField(DSL.name(NAME_COL_TL_RELATION_ID), idTypeRelation);
    }

    private TableImpGroupsRelations(Name alias, TableImpGroupsRelations aliased) {
        super(alias, aliased);
        colGroupId = createField(DSL.name(NAME_COL_TL_GROUP_ID), aliased.colGroupId.getDataType());
        colRelationId = createField(DSL.name(NAME_COL_TL_RELATION_ID), aliased.colRelationId.getDataType());
    }

    public TableField<Record, ?> getRelationId() {
        return colRelationId;
    }

    public TableField<Record, ?> getGroupId() {
        return colGroupId;
    }

    @Override
    public TableImpGroupsRelations as(Name alias) {
        return new TableImpGroupsRelations(alias, this).initCustomFields();
    }

    @Override
    public TableImpGroupsRelations getThis() {
        return this;
    }

}
