package de.securedimensions.frostserver.plugin.plus.auth;

import java.io.IOException;
import java.util.Base64;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fraunhofer.iosb.ilt.frostserver.settings.CoreSettings;

public class PrincipalFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrincipalFilter.class);

    private String anonRead;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    	
    	anonRead = filterConfig.getInitParameter(CoreSettings.TAG_AUTH_ALLOW_ANON_READ);
        LOGGER.info("Turning on OAuth Authentication.");
    }

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		
		HttpServletRequest httpRequest = (HttpServletRequest) request;
		HttpServletResponse httpResponse = (HttpServletResponse) response;
    
		if ("T".equals(anonRead) && "GET".equalsIgnoreCase(httpRequest.getMethod())) {
			chain.doFilter(request, response);
		}
		else
		{

	        String authHeader = httpRequest.getHeader("Authorization");
	        if (authHeader != null) {
	            LOGGER.debug("Authorization: " + authHeader);
	
	            String[] authHeaderSplit = authHeader.split("\\s");
	
	            for (int i = 0; i < authHeaderSplit.length; i++) {
	                String token = authHeaderSplit[i];
	                if (token.equalsIgnoreCase("Basic")) {
	
	                    String credentials = new String(Base64.getDecoder().decode(authHeaderSplit[i + 1]));
	                    int index = credentials.indexOf(":");
	                    if (index != -1) {
	                        String username = credentials.substring(0, index).trim();
	                        LOGGER.debug("Username: " + username);
	                        chain.doFilter(new PrincipalRequestWrapper(username,httpRequest), httpResponse);
	                    }
	                }
	            }
	        }
	        else
	        {
	        	String realm = "foo bar";
	        	httpResponse.setHeader("WWW-Authenticate", "Basic realm=\"" + realm + "\"");
	        	httpResponse.sendError(401, "Authentication missing");
	        }
        
		}
	}
}
