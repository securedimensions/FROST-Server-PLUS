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

import de.fraunhofer.iosb.ilt.frostserver.plugin.coremodel.CoreModelSettings;
import de.fraunhofer.iosb.ilt.frostserver.settings.ConfigDefaults;
import de.fraunhofer.iosb.ilt.frostserver.settings.CoreSettings;
import de.fraunhofer.iosb.ilt.frostserver.settings.Settings;
import de.fraunhofer.iosb.ilt.frostserver.settings.annotation.DefaultValue;
import de.fraunhofer.iosb.ilt.frostserver.settings.annotation.DefaultValueBoolean;

/**
 * @author hylke
 */
public final class PluginPlusSettings implements ConfigDefaults {

    @DefaultValueBoolean(false)
    public static final String TAG_ENABLE_PLUS = "staplus.enable";
    @DefaultValueBoolean(false)
    public static final String TAG_ENABLE_ENFORCE_OWNERSHIP = "staplus.enable.enforceOwnership";
    @DefaultValueBoolean(false)
    public static final String TAG_ENABLE_ENFORCE_LICENSING = "staplus.enable.enforceLicensing";
    @DefaultValueBoolean(false)
    public static final String TAG_ENABLE_ENFORCE_GROUP_LICENSING = "staplus.enable.enforceGroupLicensing";
    @DefaultValue("https://creativecommons.org")
    public static final String TAG_ENABLE_LICENSE_DOMAIN = "staplus.licenseDomain";
    @DefaultValue("")
    public static final String TAG_ID_TYPE_GROUP = "staplus.idType.group";
    @DefaultValue("")
    public static final String TAG_ID_TYPE_LICENSE = "staplus.idType.license";
    @DefaultValue("")
    public static final String TAG_ID_TYPE_PROJECT = "staplus.idType.project";
    @DefaultValue("")
    public static final String TAG_ID_TYPE_RELATION = "staplus.idType.relation";

    public final String idTypeDefault;
    public final String idTypeGroup;
    public final String idTypeLicense = "STRING"; /* fixed */
    public final String idTypeParty = "STRING"; /* fixed */
    public final String idTypeProject;
    public final String idTypeRelation;

    public PluginPlusSettings(CoreSettings settings) {
        Settings pluginSettings = settings.getPluginSettings();
        idTypeDefault = pluginSettings.get(CoreModelSettings.TAG_ID_TYPE_DEFAULT, PluginPlusSettings.class).toUpperCase();
        idTypeGroup = pluginSettings.get(TAG_ID_TYPE_GROUP, idTypeDefault).toUpperCase();
        idTypeProject = pluginSettings.get(TAG_ID_TYPE_PROJECT, idTypeDefault).toUpperCase();
        idTypeRelation = pluginSettings.get(TAG_ID_TYPE_RELATION, idTypeDefault).toUpperCase();
    }
}
