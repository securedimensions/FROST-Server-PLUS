package de.securedimensions.frostserver.plugin.plus.auth;

import static de.fraunhofer.iosb.ilt.frostserver.settings.CoreSettings.TAG_AUTH_ALLOW_ANON_READ;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;

import de.fraunhofer.iosb.ilt.frostserver.settings.ConfigDefaults;
import de.fraunhofer.iosb.ilt.frostserver.settings.CoreSettings;
import de.fraunhofer.iosb.ilt.frostserver.settings.Settings;
import de.fraunhofer.iosb.ilt.frostserver.util.AuthProvider;
import de.fraunhofer.iosb.ilt.frostserver.util.AuthUtils;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.UpgradeFailedException;
import de.securedimensions.frostserver.plugin.plus.PartyTests;

public class PrincipalAuthProvider implements AuthProvider, ConfigDefaults {

    public static final String[] HTTP_URL_PATTERNS = {"/v1.0", "/v1.0/*", "/v1.1", "/v1.1/*"};
    
    private CoreSettings coreSettings;

	@Override
	public void init(CoreSettings coreSettings) {
        this.coreSettings = coreSettings;
	}

	@Override
	public String checkForUpgrades() {
		return "";
	}

	@Override
	public boolean doUpgrades(Writer out) throws UpgradeFailedException, IOException {
		return true;
	}

	@Override
	public void addFilter(Object context, CoreSettings coreSettings) {
        if (!(context instanceof ServletContext)) {
            throw new IllegalArgumentException("Context must be a ServletContext to add Filters.");
        }
        ServletContext servletContext = (ServletContext) context;
        String filterClass = PrincipalFilter.class.getName();
        String filterName = "PrincipalFilterSta";
        FilterRegistration.Dynamic principalFilterSta = servletContext.addFilter(filterName, filterClass);
        
        String[] urlPatterns = Arrays.copyOf(HTTP_URL_PATTERNS, HTTP_URL_PATTERNS.length);
        principalFilterSta.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD), true, urlPatterns);
	}

	@Override
	public boolean isValidUser(String clientId, String username, String password) {
		return false;
	}

	@Override
	public boolean userHasRole(String clientId, String userName, String roleName) {

		if (userName.equalsIgnoreCase(PartyTests.ADMIN))
			return true;
		else
			return false;
	}

}
