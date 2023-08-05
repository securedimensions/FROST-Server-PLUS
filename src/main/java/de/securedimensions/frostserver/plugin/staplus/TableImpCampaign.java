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

import de.fraunhofer.iosb.ilt.frostserver.model.EntityType;
import de.fraunhofer.iosb.ilt.frostserver.model.ModelRegistry;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.JooqPersistenceManager;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.bindings.JsonBinding;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.bindings.JsonValue;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.bindings.MomentBinding;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.factories.EntityFactories;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.relations.RelationManyToMany;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.relations.RelationOneToMany;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.tables.StaMainTable;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.tables.StaTableAbstract;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.tables.TableCollection;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.utils.PropertyFieldRegistry.ConverterTimeInstant;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.utils.validator.SecurityTableWrapper;
import de.fraunhofer.iosb.ilt.frostserver.plugin.coremodel.PluginCoreModel;
import de.fraunhofer.iosb.ilt.frostserver.plugin.coremodel.TableImpDatastreams;
import de.fraunhofer.iosb.ilt.frostserver.plugin.multidatastream.PluginMultiDatastream;
import de.fraunhofer.iosb.ilt.frostserver.plugin.multidatastream.TableImpMultiDatastreams;
import net.time4j.Moment;
import org.jooq.DataType;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultDataType;
import org.jooq.impl.SQLDataType;

/**
 *
 * @author am
 * @author scf
 */
public class TableImpCampaign extends StaTableAbstract<TableImpCampaign> {

    /**
     * The column <code>public.CAMPAIGN.EP_NAME</code>.
     */
    public final TableField<Record, String> colName = createField(DSL.name("NAME"), SQLDataType.CLOB.defaultValue(DSL.field("'no name'::text", SQLDataType.CLOB)), this);

    /**
     * The column <code>public.CAMPAIGN.EP_DESCRIPTION</code>.
     */
    public final TableField<Record, String> colDescription = createField(DSL.name("DESCRIPTION"), SQLDataType.CLOB, this);

    /**
     * The column <code>public.CAMPAIGN.EP_PROPERTIES</code>.
     */
    public final TableField<Record, JsonValue> colProperties = createField(DSL.name("PROPERTIES"), DefaultDataType.getDefaultDataType(TYPE_JSONB), this, "", new JsonBinding());

    /**
     * The column <code>public.CAMPAIGN.EP_CLASSIFICATION</code>.
     */
    public final TableField<Record, String> colClassification = createField(DSL.name("CLASSIFICATION"), SQLDataType.CLOB, this);

    /**
     * The column <code>public.CAMPAIGN.EP_TERMSOFUSE</code>.
     */
    public final TableField<Record, String> colTermsOfUse = createField(DSL.name("TERMS_OF_USE"), SQLDataType.CLOB, this);

    /**
     * The column <code>public.CAMPAIGN.EP_PRIVACYPOLICY</code>.
     */
    public final TableField<Record, String> colPrivacyPolicy = createField(DSL.name("PRIVACY_POLICY"), SQLDataType.CLOB, this);

    /**
     * The column <code>public.CAMPAIGNS.EP_URL</code>.
     */
    public final TableField<Record, String> colUrl = createField(DSL.name("URL"), SQLDataType.CLOB, this);

    /**
     * The column <code>public.CAMPAIGN.START_TIME</code>.
     */
    public final TableField<Record, Moment> colStartTime = createField(DSL.name("START_TIME"), SQLDataType.TIMESTAMP, this, "", new MomentBinding());

    /**
     * The column <code>public.CAMPAIGN.END_TIME</code>.
     */
    public final TableField<Record, Moment> colEndTime = createField(DSL.name("END_TIME"), SQLDataType.TIMESTAMP, this, "", new MomentBinding());

    /**
     * The column <code>public.CAMPAIGN.CREATION_TIME</code>.
     */
    public final TableField<Record, Moment> colCreationTime = createField(DSL.name("CREATION_TIME"), SQLDataType.TIMESTAMP, this, "", new MomentBinding());

    /**
     * The column <code>public.CAMPAIGN.EP_ID</code>.
     */
    public final TableField<Record, ?> colId = createField(DSL.name("ID"), getIdType(), this);

    public final TableField<Record, ?> colPartyId;

    public final TableField<Record, ?> colLicenseId;

    private final PluginPLUS pluginPLUS;
    private final PluginCoreModel pluginCoreModel;

    private final PluginMultiDatastream pluginMultiDatastream;

    /**
     * Create a <code>public.CAMPAIGNS</code> table reference.
     *
     * @param idType The (SQL)DataType of the Id columns used in the actual
     * database.
     * @param pluginPLUS the STAplus plugin
     * @param pluginCoreModel the coreModel plugin that this data model links
     * to.
     */
    public TableImpCampaign(DataType<?> idType, DataType<?> idTypeParty, DataType<?> idTypeLicense, PluginPLUS pluginPLUS, PluginCoreModel pluginCoreModel, PluginMultiDatastream pluginMultiDatastream) {
        super(idType, DSL.name("CAMPAIGNS"), null, null);
        this.pluginPLUS = pluginPLUS;
        this.pluginCoreModel = pluginCoreModel;
        this.pluginMultiDatastream = pluginMultiDatastream;

        colPartyId = createField(DSL.name("PARTY_ID"), idTypeParty.nullable(true));
        colLicenseId = createField(DSL.name("LICENSE_ID"), idTypeLicense.nullable(true));

    }

    private TableImpCampaign(Name alias, TableImpCampaign aliased, PluginPLUS pluginPlus, PluginCoreModel pluginCoreModel, PluginMultiDatastream pluginMultiDatastream) {
        this(alias, aliased, aliased, pluginPlus, pluginCoreModel, pluginMultiDatastream);
    }

    private TableImpCampaign(Name alias, TableImpCampaign aliased, Table updatedSql, PluginPLUS pluginPlus, PluginCoreModel pluginCoreModel, PluginMultiDatastream pluginMultiDatastream) {
        super(aliased.getIdType(), alias, aliased, updatedSql);
        this.pluginPLUS = pluginPlus;
        this.pluginCoreModel = pluginCoreModel;
        this.pluginMultiDatastream = pluginMultiDatastream;

        colPartyId = createField(DSL.name("PARTY_ID"), aliased.colPartyId.getDataType().nullable(true));
        colLicenseId = createField(DSL.name("LICENSE_ID"), aliased.colLicenseId.getDataType().nullable(true));

    }

    @Override
    public void initRelations() {
        final TableCollection tables = getTables();

        TableImpDatastreams tableDatastreams = tables.getTableForClass(TableImpDatastreams.class);
        final int projectIdIdx = tableDatastreams.indexOf("CAMPAIGN_ID");

        // We add the relation to Party table.
        registerRelation(new RelationOneToMany<>(pluginPLUS.npPartyCampaign, this, tables.getTableForClass(TableImpParty.class))
                .setSourceFieldAccessor(TableImpCampaign::getPartyId)
                .setTargetFieldAccessor(TableImpParty::getId));

        // We add the relation to License table.
        registerRelation(new RelationOneToMany<>(pluginPLUS.npLicenseCampaign, this, tables.getTableForClass(TableImpLicense.class))
                .setSourceFieldAccessor(TableImpCampaign::getLicenseId)
                .setTargetFieldAccessor(TableImpLicense::getId));

        // Add relation to Datastreams table
        final TableImpCampaignsDatastreams tableCampaignsDatastreams = tables.getTableForClass(TableImpCampaignsDatastreams.class);
        registerRelation(new RelationManyToMany<>(pluginPLUS.npDatastreamsCampaign, this, tableCampaignsDatastreams, tableDatastreams)
                .setSourceFieldAcc(TableImpCampaign::getId)
                .setSourceLinkFieldAcc(TableImpCampaignsDatastreams::getCampaignId)
                .setTargetLinkFieldAcc(TableImpCampaignsDatastreams::getDatastreamId)
                .setTargetFieldAcc(TableImpDatastreams::getId));
        tableDatastreams.registerRelation(new RelationManyToMany<>(pluginPLUS.npCampaignDatastreams, tableDatastreams, tableCampaignsDatastreams, this)
                .setSourceFieldAcc(TableImpDatastreams::getId)
                .setSourceLinkFieldAcc(TableImpCampaignsDatastreams::getDatastreamId)
                .setTargetLinkFieldAcc(TableImpCampaignsDatastreams::getCampaignId)
                .setTargetFieldAcc(TableImpCampaign::getId));

        if (pluginMultiDatastream != null) {
            // Add relation to MultiDatastreams table
            TableImpMultiDatastreams tableMultiDatastreams = tables.getTableForClass(TableImpMultiDatastreams.class);
            final TableImpCampaignsMultiDatastreams tableCampaignsMultiDatastreams = tables.getTableForClass(TableImpCampaignsMultiDatastreams.class);
            registerRelation(new RelationManyToMany<>(pluginPLUS.npMultiDatastreamsCampaign, this, tableCampaignsMultiDatastreams, tableMultiDatastreams)
                    .setSourceFieldAcc(TableImpCampaign::getId)
                    .setSourceLinkFieldAcc(TableImpCampaignsMultiDatastreams::getCampaignId)
                    .setTargetLinkFieldAcc(TableImpCampaignsMultiDatastreams::getMultiDatastreamId)
                    .setTargetFieldAcc(TableImpMultiDatastreams::getId));
            tableMultiDatastreams.registerRelation(new RelationManyToMany<>(pluginPLUS.npCampaignMultiDatastreams, tableMultiDatastreams, tableCampaignsMultiDatastreams, this)
                    .setSourceFieldAcc(TableImpMultiDatastreams::getId)
                    .setSourceLinkFieldAcc(TableImpCampaignsMultiDatastreams::getMultiDatastreamId)
                    .setTargetLinkFieldAcc(TableImpCampaignsMultiDatastreams::getCampaignId)
                    .setTargetFieldAcc(TableImpCampaign::getId));

        }

    }

    @Override
    public void initProperties(final EntityFactories entityFactories) {
        final TableCollection tables = getTables();
        pfReg.addEntryId(TableImpCampaign::getId);
        pfReg.addEntryString(pluginCoreModel.epName, table -> table.colName);
        pfReg.addEntryString(pluginCoreModel.epDescription, table -> table.colDescription);
        pfReg.addEntryMap(ModelRegistry.EP_PROPERTIES, table -> table.colProperties);
        pfReg.addEntryString(pluginPLUS.epClassification, table -> table.colClassification);
        pfReg.addEntryString(pluginPLUS.epCampaignTermsOfUse, table -> table.colTermsOfUse);
        pfReg.addEntryString(pluginPLUS.epCampaignPrivacyPolicy, table -> table.colPrivacyPolicy);
        pfReg.addEntryString(pluginPLUS.epUrl, table -> table.colUrl);

        pfReg.addEntry(pluginPLUS.epCampaignCreationTime, table -> table.colCreationTime,
                new ConverterTimeInstant<>(pluginPLUS.epCampaignCreationTime, table -> table.colCreationTime));
        pfReg.addEntry(pluginPLUS.epCampaignStartTime, table -> table.colStartTime,
                new ConverterTimeInstant<>(pluginPLUS.epCampaignStartTime, table -> table.colStartTime));
        pfReg.addEntry(pluginPLUS.epCampaignEndTime, table -> table.colEndTime,
                new ConverterTimeInstant<>(pluginPLUS.epCampaignEndTime, table -> table.colEndTime));

        pfReg.addEntry(pluginPLUS.npDatastreamsCampaign, TableImpCampaign::getId);
        TableImpDatastreams tableDatastreams = tables.getTableForClass(TableImpDatastreams.class);
        tableDatastreams.getPropertyFieldRegistry()
                .addEntry(pluginPLUS.npCampaignDatastreams, TableImpDatastreams::getId);

        if (pluginMultiDatastream != null) {
            TableImpMultiDatastreams tableMultiDatastreams = tables.getTableForClass(TableImpMultiDatastreams.class);
            pfReg.addEntry(pluginPLUS.npMultiDatastreamsCampaign, TableImpCampaign::getId);
            tableMultiDatastreams.getPropertyFieldRegistry()
                    .addEntry(pluginPLUS.npCampaignMultiDatastreams, TableImpMultiDatastreams::getId);
        }
        pfReg.addEntry(pluginPLUS.npPartyCampaign, TableImpCampaign::getPartyId);
        pfReg.addEntry(pluginPLUS.npLicenseCampaign, TableImpCampaign::getLicenseId);
        pfReg.addEntry(pluginPLUS.npGroupsCampaign, TableImpCampaign::getId);
    }

    @Override
    public EntityType getEntityType() {
        return pluginPLUS.etCampaign;
    }

    @Override
    public TableField<Record, ?> getId() {
        return colId;
    }

    public TableField<Record, ?> getPartyId() {
        return colPartyId;
    }

    public TableField<Record, ?> getLicenseId() {
        return colLicenseId;
    }

    @Override
    public TableImpCampaign as(Name alias) {
        return new TableImpCampaign(alias, this, pluginPLUS, pluginCoreModel, pluginMultiDatastream).initCustomFields();
    }

    @Override
    public StaMainTable<TableImpCampaign> asSecure(String name, JooqPersistenceManager pm) {
        final SecurityTableWrapper securityWrapper = getSecurityWrapper();
        if (securityWrapper == null) {
            return as(name);
        }
        final Table wrappedTable = securityWrapper.wrap(this, pm);
        return new TableImpCampaign(DSL.name(name), this, wrappedTable, pluginPLUS, pluginCoreModel, pluginMultiDatastream);
    }

    @Override
    public TableImpCampaign getThis() {
        return this;
    }

}
