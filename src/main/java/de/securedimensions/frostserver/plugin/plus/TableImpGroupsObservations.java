package de.securedimensions.frostserver.plugin.plus;

import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.tables.StaLinkTable;
import org.jooq.DataType;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.TableField;
import org.jooq.impl.DSL;

public class TableImpGroupsObservations<J extends Comparable> extends StaLinkTable<J, TableImpGroupsObservations<J>> {

    public static final String NAME_TABLE = "GROUPS_OBSERVATIONS";
    public static final String NAME_COL_TL_OBSERVATION_ID = "OBSERVATION_ID";
    public static final String NAME_COL_TL_GROUP_ID = "GROUP_ID";

    private static final long serialVersionUID = 1626971276;

    /**
     * The column <code>public.GROUPS_OBSERVATIONS.GROUP_ID</code>.
     */
    public final TableField<Record, J> colGroupId = createField(DSL.name(NAME_COL_TL_GROUP_ID), getIdType(), this);

    /**
     * The column <code>public.GROUPS_OBSERVATIONS.OBSERVATION_ID</code>.
     */
    public final TableField<Record, J> colObservationId = createField(DSL.name(NAME_COL_TL_OBSERVATION_ID), getIdType(), this);

    /**
     * Create a <code>public.GROUPS_OBSERVATIONS</code> table reference.
     *
     * @param idType The (SQL)DataType of the Id columns used in the actual
     * database.
     */
    public TableImpGroupsObservations(DataType<J> idType) {
        super(idType, DSL.name(NAME_TABLE), null);
    }

    private TableImpGroupsObservations(Name alias, TableImpGroupsObservations<J> aliased) {
        super(aliased.getIdType(), alias, aliased);
    }

    public TableField<Record, J> getObservationId() {
        return colObservationId;
    }

    public TableField<Record, J> getGroupId() {
        return colGroupId;
    }

    @Override
    public TableImpGroupsObservations<J> as(Name alias) {
        return new TableImpGroupsObservations<>(alias, this).initCustomFields();
    }

    @Override
    public TableImpGroupsObservations<J> getThis() {
        return this;
    }

}
