/*
 * Copyright (C) 2020 Fraunhofer Institut IOSB, Fraunhoferstr. 1, D 76131
 * Karlsruhe, Germany.
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
package de.securedimensions.frostserver.plugin.plus;

import de.fraunhofer.iosb.ilt.frostserver.plugin.coremodel.CoreModelSettings;
import de.fraunhofer.iosb.ilt.frostserver.settings.ConfigDefaults;
import de.fraunhofer.iosb.ilt.frostserver.settings.CoreSettings;
import de.fraunhofer.iosb.ilt.frostserver.settings.Settings;
import de.fraunhofer.iosb.ilt.frostserver.settings.annotation.DefaultValue;
import de.fraunhofer.iosb.ilt.frostserver.settings.annotation.DefaultValueBoolean;

/**
 *
 * @author hylke
 */
public final class PluginPlusSettings implements ConfigDefaults {

    @DefaultValueBoolean(false)
    public static final String TAG_ENABLE_PLUS = "plus.enable";
    @DefaultValueBoolean(false)
    public static final String TAG_ENABLE_ENFORCE_OWNERSHIP = "plus.enable.enforceOwnsership";
    @DefaultValueBoolean(false)
    public static final String TAG_ENABLE_ENFORCE_LICENSING = "plus.enable.enforceLicensing";
    @DefaultValue("")
    public static final String TAG_ID_TYPE_GROUP = "plus.idType.group";
    @DefaultValue("")
    public static final String TAG_ID_TYPE_LICENSE = "plus.idType.license";
    @DefaultValue("")
    public static final String TAG_ID_TYPE_PROJECT = "plus.idType.project";
    @DefaultValue("")
    public static final String TAG_ID_TYPE_RELATION = "plus.idType.relation";
 
    public final String idTypeDefault;
    public final String idTypeGroup;
    public final String idTypeLicense;
    public final String idTypeParty = "UUID"; /* fixed */
    public final String idTypeProject;
    public final String idTypeRelation;

    public PluginPlusSettings(CoreSettings settings) {
        Settings pluginSettings = settings.getPluginSettings();
        idTypeDefault = pluginSettings.get(CoreModelSettings.TAG_ID_TYPE_DEFAULT, PluginPlusSettings.class).toUpperCase();
        idTypeGroup = pluginSettings.get(TAG_ID_TYPE_GROUP, idTypeDefault).toUpperCase();
        idTypeLicense = pluginSettings.get(TAG_ID_TYPE_LICENSE, idTypeDefault).toUpperCase();
        idTypeProject = pluginSettings.get(TAG_ID_TYPE_PROJECT, idTypeDefault).toUpperCase();
        idTypeRelation = pluginSettings.get(TAG_ID_TYPE_RELATION, idTypeDefault).toUpperCase();
    }
}
