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

public class TableImpGroupsProjects extends StaLinkTable<TableImpGroupsProjects> {

    public static final String NAME_TABLE = "GROUPS_PROJECTS";
    private static final long serialVersionUID = 1626971276;

    /**
     * The column <code>public.GROUPS_PROJECTS.GROUP_ID</code>.
     */
    public final TableField<Record, ?> colGroupId;

    /**
     * The column <code>public.GROUPS_PROJECTS.PROJECT_ID</code>.
     */
    public final TableField<Record, ?> colProjectId;

    /**
     * Create a <code>public.GROUPS_PROJECTS</code> table reference.
     *
     * @param idTypeGroup The (SQL)DataType of the Id columns used in the actual
     * database.
     * @param idTypeProject
     */
    public TableImpGroupsProjects(DataType<?> idTypeGroup, DataType<?> idTypeProject) {
        super(DSL.name(NAME_TABLE), null);
        colGroupId = createField(DSL.name("GROUP_ID"), idTypeGroup);
        colProjectId = createField(DSL.name("PROJECT_ID"), idTypeProject);
    }

    private TableImpGroupsProjects(Name alias, TableImpGroupsProjects aliased) {
        super(alias, aliased);
        colGroupId = createField(DSL.name("GROUP_ID"), aliased.colGroupId.getDataType());
        colProjectId = createField(DSL.name("PROJECT_ID"), aliased.colProjectId.getDataType());
    }

    public TableField<Record, ?> getProjectId() {
        return colProjectId;
    }

    public TableField<Record, ?> getGroupId() {
        return colGroupId;
    }

    @Override
    public TableImpGroupsProjects as(Name alias) {
        return new TableImpGroupsProjects(alias, this).initCustomFields();
    }

    @Override
    public TableImpGroupsProjects getThis() {
        return this;
    }

}
