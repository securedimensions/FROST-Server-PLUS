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

public class TableImpCampaignsMultiDatastreams extends StaLinkTable<TableImpCampaignsMultiDatastreams> {

    public static final String NAME_TABLE = "CAMPAIGNS_MULTI_DATASTREAMS";
    public static final String NAME_COL_TL_DATASTREAM_ID = "MULTI_DATASTREAM_ID";
    public static final String NAME_COL_TL_CAMPAIGN_ID = "CAMPAIGN_ID";

    private static final long serialVersionUID = 1626971276;

    /**
     * The column <code>public.CAMPAIGNS_MULTI_DATASTREAMS.CAMPAIGN_ID</code>.
     */
    public final TableField<Record, ?> colCampaignId;

    /**
     * The column <code>public.CAMPAIGNS_MULTI_DATASTREAMS.DATASTREAM_ID</code>.
     */
    public final TableField<Record, ?> colMultiDatastreamId;

    /**
     * Create a <code>public.CAMPAIGNS_MULTI_DATASTREAMS</code> table reference.
     *
     * @param idTypeCampaign The (SQL)DataType of the Id columns used in the actual
     * database.
     * @param idTypeMultiDatastream
     */
    public TableImpCampaignsMultiDatastreams(DataType<?> idTypeCampaign, DataType<?> idTypeMultiDatastream) {
        super(DSL.name(NAME_TABLE), null);
        colCampaignId = createField(DSL.name("CAMPAIGN_ID"), idTypeCampaign);
        colMultiDatastreamId = createField(DSL.name("MULTI_DATASTREAM_ID"), idTypeMultiDatastream);
    }

    private TableImpCampaignsMultiDatastreams(Name alias, TableImpCampaignsMultiDatastreams aliased) {
        super(alias, aliased);
        colCampaignId = createField(DSL.name("CAMPAIGN_ID"), aliased.colCampaignId.getDataType());
        colMultiDatastreamId = createField(DSL.name("MULTI_DATASTREAM_ID"), aliased.colMultiDatastreamId.getDataType());
    }

    public TableField<Record, ?> getCampaignId() {
        return colCampaignId;
    }

    public TableField<Record, ?> getMultiDatastreamId() {
        return colMultiDatastreamId;
    }

    @Override
    public TableImpCampaignsMultiDatastreams as(Name alias) {
        return new TableImpCampaignsMultiDatastreams(alias, this).initCustomFields();
    }

    @Override
    public TableImpCampaignsMultiDatastreams getThis() {
        return this;
    }

}
