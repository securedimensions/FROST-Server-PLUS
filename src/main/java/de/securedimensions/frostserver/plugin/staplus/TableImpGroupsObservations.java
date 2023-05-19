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
