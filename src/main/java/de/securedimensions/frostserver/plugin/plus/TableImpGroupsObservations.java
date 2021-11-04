package de.securedimensions.frostserver.plugin.plus;

import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.tables.StaLinkTable;
import org.jooq.DataType;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.TableField;
import org.jooq.impl.DSL;

public class TableImpGroupsObservations extends StaLinkTable<TableImpGroupsObservations> {

    public static final String NAME_TABLE = "GROUPS_OBSERVATIONS";
    public static final String NAME_COL_TL_OBSERVATION_ID = "OBSERVATION_ID";
    public static final String NAME_COL_TL_GROUP_ID = "GROUP_ID";

    private static final long serialVersionUID = 1626971276;

    /**
     * The column <code>public.GROUPS_OBSERVATIONS.GROUP_ID</code>.
     */
    public final TableField<Record, ?> colGroupId;

    /**
     * The column <code>public.GROUPS_OBSERVATIONS.OBSERVATION_ID</code>.
     */
    public final TableField<Record, ?> colObservationId;

    /**
     * Create a <code>public.GROUPS_OBSERVATIONS</code> table reference.
     *
     * @param idTypeGroup The (SQL)DataType of the Id columns used in the actual
     * database.
     */
    public TableImpGroupsObservations(DataType<?> idTypeGroup, DataType<?> idTypeObs) {
        super(DSL.name(NAME_TABLE), null);
        colGroupId = createField(DSL.name(NAME_COL_TL_GROUP_ID), idTypeGroup);
        colObservationId = createField(DSL.name(NAME_COL_TL_OBSERVATION_ID), idTypeObs);
    }

    private TableImpGroupsObservations(Name alias, TableImpGroupsObservations aliased) {
        super(alias, aliased);
        colGroupId = createField(DSL.name(NAME_COL_TL_GROUP_ID), aliased.colGroupId.getDataType());
        colObservationId = createField(DSL.name(NAME_COL_TL_OBSERVATION_ID), aliased.colObservationId.getDataType());
    }

    public TableField<Record, ?> getObservationId() {
        return colObservationId;
    }

    public TableField<Record, ?> getGroupId() {
        return colGroupId;
    }

    @Override
    public TableImpGroupsObservations as(Name alias) {
        return new TableImpGroupsObservations(alias, this).initCustomFields();
    }

    @Override
    public TableImpGroupsObservations getThis() {
        return this;
    }

}
