package en.webshop.test;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.both;
import static org.junit.matchers.JUnitMatchers.either;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Locale;

import javax.ejb.EJB;
import javax.security.auth.login.LoginException;

import org.jboss.arquillian.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.security.client.SecurityClient;
import org.jboss.security.client.SecurityClientFactory;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import en.webshop.profileManagement.domain.Profile;
import en.webshop.profileManagement.service.InvalidEmailException;
import en.webshop.profileManagement.service.InvalidLastNameException;
import en.webshop.profileManagement.service.ProfileDeleteArticleException;
import en.webshop.profileManagement.service.ProfileDeleteOrderException;
import en.webshop.profileManagement.service.ProfileDuplicateException;
import en.webshop.profileManagement.service.ProfileManagement;
import en.webshop.profileManagement.service.ProfileNotFoundException;
import en.webshop.profileManagement.service.ProfileValidationException;
import en.webshop.profileManagement.service.StatusAlreadySetException;
import en.webshop.test.util.ArchiveUtil;
import en.webshop.test.util.DbReloadProvider;
import en.webshop.util.ConcurrentDeletedException;
import en.webshop.util.ConcurrentUpdatedException;


@RunWith(Arquillian.class)
public class ProfileManagementTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(ProfileManagement.class);
	
	private static final Locale LOCALE = Locale.GERMAN;
	
	@Rule
	public ExpectedException thrown = ExpectedException.none();
	
	private static SecurityClient securityClient;
	
	@EJB
	private ProfileManagement pm;
	
	private static final int PROFILE_ROLE_CUSTOMER = 1;
	private static final int PROFILE_ROLE_SUPPLIER = 2;
	private static final int PROFILE_ROLE_ADMIN = 0;
	private static final int PROFILE_STATUS_ACTIVATED = 0;
	private static final int PROFILE_STATUS_DEACTIVATED = 1;
	private static final String PROFILE_LAST_NAME_AVAILABLE = "Mustermann";
	private static final String PROFILE_LAST_NAME_UNAVAILABLE = "Unavailable";
	private static final String PROFILE_LAST_NAME_INVALID = "?";
	private static final String PROFILE_EMAIL_AVAILABLE = "max@hs-karlsruhe.de";
	private static final String PROFILE_EMAIL_INVALID = "mail/invalid.de";
	private static final String PROFILE_EMAIL_WITHOUT_ORDERS = "oliver.kahn@fc-bayern.de";
	private static final String PROFILE_EMAIL_UNAVAILABLE = "unavailable@hs-karlsruhe.de";
	private static final String PROFILE_NEW_LAST_NAME = "Newname";
	private static final String PROFILE_NEW_EMAIL = "new@hs-karlsruhe.de";
	private static final String PROFILE_NEW_TELEPHONE_NO = "1234/567890";
	private static final String PROFILE_ADDR_NEW_NAME = "New Address";
	private static final String PROFILE_ADDR_NEW_STREET = "New Street";
	private static final String PROFILE_ADDR_NEW_HOUSE_NO = "99";
	private static final String PROFILE_ADDR_NEW_POST_CODE = "88888";
	private static final String PROFILE_ADDR_NEW_CITY = "New City";
	private static final String PROFILE_NEW_PASSWORD = "pass";
	
	private static final String USERNAME = "rd@sc.de";
	private static final String PASSWORD = "pass";
	
	private static final String ADMIN_USERNAME = "admin@hs-karlsruhe.de";
	private static final String ADMIN_PASSWORD = "pass";
	
	private static final String CONCURRENT_UPDATE = "update";
	
	/**
	 */
	@Deployment
	public static EnterpriseArchive createTestArchive() {
		return ArchiveUtil.getTestArchive();
	}
	
	/**
	 */
	@BeforeClass
	public static void init() {
		try {
			DbReloadProvider.reload();
			securityClient = SecurityClientFactory.getSecurityClient();
		}
		catch (Exception e) {
			e.printStackTrace();
			fail();
		}
		
		assertThat(securityClient, is(notNullValue()));
	}
	
	/**
	 */
	@Before
	public void login() throws SQLException, LoginException {
		securityClient.setSimple(USERNAME, PASSWORD);
		securityClient.login();
	}
	
	/**
	 */
	@After
	public void logoutClient() {
		securityClient.logout();
	}
	
	public void adminLogin() throws SQLException, LoginException {
		securityClient.logout();
		securityClient.setSimple(ADMIN_USERNAME, ADMIN_PASSWORD);
		securityClient.login();
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void findProfileWithLastNameAvailable() throws ProfileNotFoundException, InvalidLastNameException {
		final String lastName = PROFILE_LAST_NAME_AVAILABLE;

		final Collection<Profile> profiles = pm.findProfilesByLastName(lastName, PROFILE_ROLE_CUSTOMER, LOCALE);
		assertThat(profiles, is(notNullValue()));
		assertThat(profiles.isEmpty(), is(false));

		for (Profile p: profiles) {
			assertThat(p.getLastName(), is(lastName));
			assertThat(p.getRole(), either(is(PROFILE_ROLE_CUSTOMER)).or(is(PROFILE_ROLE_SUPPLIER)).or(is(PROFILE_ROLE_ADMIN)));
			
			/** 
			 * Nur zur Veranschaulichung von both().and()
			 * Wenn Gleichheit mit einem anderen Namen, dann ja auch != null ...
			 * @return
			 */
			assertThat(p.getLastName(), both(is(notNullValue())).and(is(lastName)));
			
			/**
			 *  Veranschaulichung von allOf: mehrere Argumente moeglich,
			 * d.h. nicht nur 2 wie bei both().and()
			 * @return
			 */
			assertThat(p.getLastName(), allOf(is(notNullValue()), is(lastName)));
		}
	}

	@Test
	public void findProfileWithLastNameUnavailable() throws ProfileNotFoundException, InvalidLastNameException {
		final String lastName = PROFILE_LAST_NAME_UNAVAILABLE;

		thrown.expect(ProfileNotFoundException.class);
		thrown.expectMessage(lastName.toUpperCase());
		// super("The query for " + query + " was unsuccessful");

		pm.findProfilesByLastName(lastName, PROFILE_ROLE_CUSTOMER, LOCALE);
	}
	
	@Test
	public void findProfileWithEmailUnavailable() throws ProfileNotFoundException, InvalidEmailException {
		final String email = PROFILE_EMAIL_UNAVAILABLE;

		thrown.expect(ProfileNotFoundException.class);
		thrown.expectMessage(email);

		pm.findProfileByEmail(email, LOCALE);
	}
	
	@Test
	public void findProfileWithLastNameInvalid() throws ProfileNotFoundException, InvalidLastNameException {
		final String lastName = PROFILE_LAST_NAME_INVALID;

		thrown.expect(InvalidLastNameException.class);
		thrown.expectMessage(lastName);
		
		pm.findProfilesByLastName(lastName, PROFILE_ROLE_CUSTOMER, LOCALE);
	}

	@Test
	public void findProfileWithInvalidEmail() throws ProfileNotFoundException, InvalidEmailException {
		final String email = PROFILE_EMAIL_INVALID;

		thrown.expect(InvalidEmailException.class);
		thrown.expectMessage(email);
		
		pm.findProfileByEmail(email, LOCALE);
	}

	@Test
	public void createProfile() throws ProfileDuplicateException, ProfileValidationException, ConcurrentDeletedException, ConcurrentUpdatedException {
		final String lastName = PROFILE_NEW_LAST_NAME;
		final String email = PROFILE_NEW_EMAIL;
		final String telephoneNo = PROFILE_NEW_TELEPHONE_NO;
		final int role = PROFILE_ROLE_CUSTOMER;
		final int status = PROFILE_STATUS_ACTIVATED;
		final String name = PROFILE_ADDR_NEW_NAME;
		final String street = PROFILE_ADDR_NEW_STREET;
		final String houseNo = PROFILE_ADDR_NEW_HOUSE_NO;
		final String postcode = PROFILE_ADDR_NEW_POST_CODE;
		final String city = PROFILE_ADDR_NEW_CITY;
		final String password = PROFILE_NEW_PASSWORD;
		
		final Profile newProfile = new Profile();
		
		newProfile.setLastName(lastName);
		newProfile.setEmail(email);
		newProfile.setTelephoneNo(telephoneNo);
		newProfile.setRole(role);
		newProfile.setStatus(status);
		newProfile.setAddrName(name);
		newProfile.setAddrStreet(street);
		newProfile.setAddrHouseNo(houseNo);
		newProfile.setAddrPostcode(postcode);
		newProfile.setAddrCity(city);
		newProfile.setPassword(password);
		newProfile.setRepeatPassword(password);
		
		pm.createProfile(newProfile, LOCALE, false);
		assertThat(newProfile, is(notNullValue()));
	}
	
	@Test
	public void updateProfile() throws ProfileNotFoundException, InvalidEmailException, ProfileValidationException, ProfileDuplicateException, ConcurrentDeletedException, ConcurrentUpdatedException
	{
		final String email = PROFILE_EMAIL_AVAILABLE;
		final String newLastName = PROFILE_LAST_NAME_UNAVAILABLE;
		Profile profile = pm.findProfileByEmail(email, LOCALE);
		
		profile.setLastName(newLastName);
		
		profile = pm.updateProfile(profile, LOCALE, false);
		assertThat(profile.getLastName() , is(newLastName));
		profile = pm.findProfileByEmail(email, LOCALE);
		assertThat(profile.getLastName() , is(newLastName));
	}
	
	@Test
	public void updateProfileConflict() throws ProfileValidationException, ProfileDuplicateException, 
											   InterruptedException, ProfileNotFoundException, 
											   InvalidEmailException, ProfileDeleteOrderException, 
											   LoginException, InvalidEmailException, 
											   ConcurrentDeletedException, ProfileDeleteArticleException, 
											   ConcurrentUpdatedException  {
		
		final String password = PROFILE_NEW_PASSWORD;
		
		thrown.expect(ConcurrentUpdatedException.class);
		
		Profile newProfile = new Profile();
		newProfile.setLastName("Conflict");
		newProfile.setFirstName("Conflict");
		newProfile.setEmail("conflict@conflict.org");
		newProfile.setTelephoneNo("000000000");
		newProfile.setRole(1);
		newProfile.setStatus(0);
		newProfile.setAddrName("Conflict");
		newProfile.setAddrStreet("Conflict");
		newProfile.setAddrHouseNo("0");
		newProfile.setAddrPostcode("00000");
		newProfile.setAddrCity("Conflict");
		newProfile.setPassword(password);
		newProfile.setRepeatPassword(password);
		
		assertThat(newProfile.getRepeatPassword() != null, is(true));
		
		pm.createProfile(newProfile, LOCALE, false);
		Profile createdProfile = pm.findProfileByEmail(newProfile.getEmail(), LOCALE);
		
		final ConcurrencyHelper concurrentUpdate = new ConcurrencyHelper(CONCURRENT_UPDATE, newProfile.getEmail());
		concurrentUpdate.run();		//startet einen parallelen Thread
		concurrentUpdate.join();	//wartet auf das Ende des Threads
		
		newProfile.setFirstName(newProfile.getFirstName() + "Conflict");
		LOGGER.error("updateProfile: begin");
		
		pm.updateProfile(createdProfile, LOCALE, false);
	}
	
	@Test
	public void deleteProfile()
		   throws ProfileDeleteOrderException, ProfileNotFoundException, InvalidEmailException, 
		   ProfileValidationException, ProfileDuplicateException, ProfileDeleteArticleException, 
		   LoginException, SQLException {
		final String email = PROFILE_EMAIL_WITHOUT_ORDERS;
		
		adminLogin();
		
		final Collection<Profile> profilePre = pm.findAllProfilesByRole(PROFILE_ROLE_CUSTOMER);
	
		Profile profile = pm.findProfileByEmail(email, LOCALE);
		LOGGER.debug("profilelog: " + profile);
		assertThat(profile, is(notNullValue()));
		
		profile = pm.findProfileWithOrdersByEmail(profile.getEmail(), LOCALE);
		assertThat(profile.getOrders().isEmpty(), is(true));
		
		pm.deleteProfile(profile);
		
		final Collection<Profile> profilePost = pm.findAllProfilesByRole(PROFILE_ROLE_CUSTOMER);
		assertThat(profilePre.size()-1, is(profilePost.size()));
	}
	
	@Test
	public void deactivateProfile() throws ProfileNotFoundException, InvalidEmailException, 
			InvalidEmailException, StatusAlreadySetException {
		final int deactivated = PROFILE_STATUS_DEACTIVATED;
		final String email = PROFILE_EMAIL_AVAILABLE;
		final Profile profile = pm.findProfileByEmail(email, LOCALE);
	
		final Profile profileBefore = pm.findProfileByEmail(email, LOCALE);
		assertThat(profileBefore, is(notNullValue()));
		
		pm.setProfileStatus(profile, deactivated);
	
		final Profile profileAfter = pm.findProfileByEmail(email, LOCALE);
		assertThat(profileAfter.getStatus(), is(PROFILE_STATUS_DEACTIVATED));
	}
	
	private class ConcurrencyHelper extends Thread {
		private String cmd;
		private String email;
		
		private ConcurrencyHelper(String cmd, String email) {
			super(cmd + "_" + email);       // Der Thread erhaelt das Kommando zzgl. Kundennr als Name
			this.cmd = cmd;
			this.email = email;
		}
		
		@Override
		public void run() {
			if ("update".equals(cmd)) {
				update();
			}
			else if ("delete".equals(cmd)) {
				delete();
			}
			else {
				System.err.println("Zulaessige Kommandos: \"update\" und \"delete\"");
			}
			return;
		}
		
		private void update() {
			try {
				final Profile profile = pm.findProfileByEmail(email, LOCALE);
				profile.setFirstName(profile.getFirstName() + "concurrent");
				pm.updateProfile(profile, LOCALE, false);
			}
			catch (Exception e) {
				throw new IllegalStateException(e);
			}
		}

		private void delete() {
			securityClient.logout();
			securityClient.setSimple(ADMIN_USERNAME, ADMIN_PASSWORD);
			try {
				securityClient.login();
			}
			catch (LoginException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
			
			try {
				Profile profile = pm.findProfileByEmail(email, LOCALE);
				pm.deleteProfile(profile);
			}
			catch (Exception e) {
				throw new IllegalStateException(e);
			}
		}
	}
}