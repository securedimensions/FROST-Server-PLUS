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

public class TableImpProjectsDatastreams extends StaLinkTable<TableImpProjectsDatastreams> {

    public static final String NAME_TABLE = "PROJECTS_DATASTREAMS";
    private static final long serialVersionUID = 1626971276;

    /**
     * The column <code>public.PROJECTS_DATASTREAMS.PROJECT_ID</code>.
     */
    public final TableField<Record, ?> colProjectId;

    /**
     * The column <code>public.PROJECTS_DATASTREAMS.DATASTREAM_ID</code>.
     */
    public final TableField<Record, ?> colDatastreamId;

    /**
     * Create a <code>public.PROJECTS_DATASTREAMS</code> table reference.
     *
     * @param idTypeProject The (SQL)DataType of the Id columns used in the actual
     * database.
     * @param idTypeDatastream
     */
    public TableImpProjectsDatastreams(DataType<?> idTypeProject, DataType<?> idTypeDatastream) {
        super(DSL.name(NAME_TABLE), null);
        colProjectId = createField(DSL.name("PROJECT_ID"), idTypeProject);
        colDatastreamId = createField(DSL.name("DATASTREAM_ID"), idTypeDatastream);
    }

    private TableImpProjectsDatastreams(Name alias, TableImpProjectsDatastreams aliased) {
        super(alias, aliased);
        colProjectId = createField(DSL.name("PROJECT_ID"), aliased.colProjectId.getDataType());
        colDatastreamId = createField(DSL.name("DATASTREAM_ID"), aliased.colDatastreamId.getDataType());
    }

    public TableField<Record, ?> getProjectId() {
        return colProjectId;
    }

    public TableField<Record, ?> getDatastreamId() {
        return colDatastreamId;
    }

    @Override
    public TableImpProjectsDatastreams as(Name alias) {
        return new TableImpProjectsDatastreams(alias, this).initCustomFields();
    }

    @Override
    public TableImpProjectsDatastreams getThis() {
        return this;
    }

}
