package de.securedimensions.frostserver.plugin.plus.auth;

import java.security.Principal;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import de.fraunhofer.iosb.ilt.frostserver.util.PrincipalExtended;
import de.securedimensions.frostserver.plugin.plus.PartyTests;

public class PrincipalRequestWrapper extends javax.servlet.http.HttpServletRequestWrapper {

	String user;
	List<String> roles = null;
	
	public PrincipalRequestWrapper(String User, HttpServletRequest request) {
		super(request);
		this.user = User;
	}


	@Override
	public boolean isUserInRole(String role) {
		if (role.equalsIgnoreCase(PartyTests.ADMIN) && user.equalsIgnoreCase(PartyTests.ADMIN)) 
			return true;
		
		return false;
	}

	@Override
	public Principal getUserPrincipal() {
		if (this.user == null) {
			return null;
		}

		if (user.equalsIgnoreCase(PartyTests.ADMIN))
		{
			// Admin principal 
			return new PrincipalExtended(user, true);

		}
		else
		{
			// Principal that returns our user
			return new Principal() {
	
				public String getName() {
					return user;
				}
			};
		}
	}
}