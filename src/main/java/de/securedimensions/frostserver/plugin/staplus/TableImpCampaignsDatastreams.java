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

public class TableImpCampaignsDatastreams extends StaLinkTable<TableImpCampaignsDatastreams> {

    public static final String NAME_TABLE = "CAMPAIGNS_DATASTREAMS";
    private static final long serialVersionUID = 1626971276;

    /**
     * The column <code>public.CAMPAIGNS_DATASTREAMS.CAMPAIGN_ID</code>.
     */
    public final TableField<Record, ?> colCampaignId;

    /**
     * The column <code>public.CAMPAIGNS_DATASTREAMS.DATASTREAM_ID</code>.
     */
    public final TableField<Record, ?> colDatastreamId;

    /**
     * Create a <code>public.CAMPAIGNS_DATASTREAMS</code> table reference.
     *
     * @param idTypeCampaign The (SQL)DataType of the Id columns used in the actual
     * database.
     * @param idTypeDatastream
     */
    public TableImpCampaignsDatastreams(DataType<?> idTypeCampaign, DataType<?> idTypeDatastream) {
        super(DSL.name(NAME_TABLE), null);
        colCampaignId = createField(DSL.name("CAMPAIGN_ID"), idTypeCampaign);
        colDatastreamId = createField(DSL.name("DATASTREAM_ID"), idTypeDatastream);
    }

    private TableImpCampaignsDatastreams(Name alias, TableImpCampaignsDatastreams aliased) {
        super(alias, aliased);
        colCampaignId = createField(DSL.name("CAMPAIGN_ID"), aliased.colCampaignId.getDataType());
        colDatastreamId = createField(DSL.name("DATASTREAM_ID"), aliased.colDatastreamId.getDataType());
    }

    public TableField<Record, ?> getCampaignId() {
        return colCampaignId;
    }

    public TableField<Record, ?> getDatastreamId() {
        return colDatastreamId;
    }

    @Override
    public TableImpCampaignsDatastreams as(Name alias) {
        return new TableImpCampaignsDatastreams(alias, this).initCustomFields();
    }

    @Override
    public TableImpCampaignsDatastreams getThis() {
        return this;
    }

}
