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
