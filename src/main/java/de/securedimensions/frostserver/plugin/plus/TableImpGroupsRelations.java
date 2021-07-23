package de.securedimensions.frostserver.plugin.plus;

import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.tables.StaLinkTable;
import org.jooq.DataType;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.TableField;
import org.jooq.impl.DSL;

public class TableImpGroupsRelations<J extends Comparable> extends StaLinkTable<J, TableImpGroupsRelations<J>> {

    public static final String NAME_TABLE = "GROUPS_RELATIONS";
    public static final String NAME_COL_TL_RELATION_ID = "RELATION_ID";
    public static final String NAME_COL_TL_GROUP_ID = "GROUP_ID";

    private static final long serialVersionUID = 1626971356;

    /**
     * The column <code>public.GROUPS_RELATIONS.GROUP_ID</code>.
     */
    public final TableField<Record, J> colGroupId = createField(DSL.name(NAME_COL_TL_GROUP_ID), getIdType(), this);

    /**
     * The column <code>public.GROUPS_RELATIONS.RELATION_ID</code>.
     */
    public final TableField<Record, J> colRelationId = createField(DSL.name(NAME_COL_TL_RELATION_ID), getIdType(), this);

    /**
     * Create a <code>public.GROUPS_OBSERVATIONS</code> table reference.
     *
     * @param idType The (SQL)DataType of the Id columns used in the actual
     * database.
     */
    public TableImpGroupsRelations(DataType<J> idType) {
        super(idType, DSL.name(NAME_TABLE), null);
    }

    private TableImpGroupsRelations(Name alias, TableImpGroupsRelations<J> aliased) {
        super(aliased.getIdType(), alias, aliased);
    }

    public TableField<Record, J> getRelationId() {
        return colRelationId;
    }

    public TableField<Record, J> getGroupId() {
        return colGroupId;
    }

    @Override
    public TableImpGroupsRelations<J> as(Name alias) {
        return new TableImpGroupsRelations<>(alias, this).initCustomFields();
    }

    @Override
    public TableImpGroupsRelations<J> getThis() {
        return this;
    }

}
