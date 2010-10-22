package en.webshop.test;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.both;
import static org.junit.matchers.JUnitMatchers.either;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Locale;

import javax.inject.Inject;

import org.dbunit.DatabaseUnitException;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import en.webshop.profileManagement.service.ProfileNotFoundException;
import en.webshop.profileManagement.domain.Profile;
import en.webshop.profileManagement.service.InvalidEmailException;
import en.webshop.profileManagement.service.InvalidLastNameException;
import en.webshop.profileManagement.service.InvalidEmailException;
import en.webshop.profileManagement.service.ProfileDeleteOrderException;
import en.webshop.profileManagement.service.ProfileDuplicateException;
import en.webshop.profileManagement.service.ProfileManagement;
import en.webshop.profileManagement.service.ProfileValidationException;
import en.webshop.profileManagement.service.StatusAlreadySetException;
import en.webshop.test.util.DbReload;


@RunWith(Arquillian.class)
public class ProfileManagementTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(ProfileManagement.class);
	
	private static final Locale LOCALE = Locale.GERMAN;
	
	@Rule
	public ExpectedException thrown = ExpectedException.none();
	
	@Inject
	private ProfileManagement pm;
	
	private static final char PROFILE_ROLE_CUSTOMER = 'C';
	private static final char PROFILE_ROLE_VENDOR = 'V';
	private static final char PROFILE_ROLE_ADMIN = 'A';
	private static final char PROFILE_STATUS = 'A';
	private static final String PROFILE_LAST_NAME_AVAILABLE = "Mustermann";
	private static final String PROFILE_LAST_NAME_UNAVAILABLE = "Unavailable";
	private static final String PROFILE_LAST_NAME_INVALID = "?";
	private static final Integer PROFILE_ID_AVAILABLE = Integer.valueOf(501);
	private static final Integer PROFILE_ID_INVALID = Integer.valueOf(-1);
	private static final Integer PROFILE_ID_WITHOUT_ORDERS = 503;
	private static final Integer PROFILE_ID_WITH_ADDRESS = 500;
	private static final String ADDRESS_NAME_EXISTANT = "Metall AG";
	private static final String PROFILE_EMAIL_UNAVAILABLE = "unavailable@hs-karlsruhe.de";
	private static final String PROFILE_NEW_LAST_NAME = "Neu";
	private static final String PROFILE_NEW_EMAIL = "neu@hs-karlsruhe.de";
	private static final String PROFILE_NEW_TELEPHONE_NO = "1234/567890";
	private static final String ADDRESS_NEW_NAME = "New Address";
	private static final String ADDRESS_NEW_ROAD = "New-Road";
	private static final String ADDRESS_NEW_HOUSE_NO = "99";
	private static final String ADDRESS_NEW_ZIP_CODE = "88888";
	private static final String ADDRESS_NEW_CITY = "New City";
	private static final char ADDRESS_STATUS = 'A';
	
	@BeforeClass
	public static void reloadDB() throws SQLException, DatabaseUnitException, ClassNotFoundException, InstantiationException, IllegalAccessException, IOException {
		DbReload.reload();
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
			assertThat(p.getRole(), either(is(PROFILE_ROLE_CUSTOMER)).or(is(PROFILE_ROLE_VENDOR)).or(is(PROFILE_ROLE_ADMIN)));
			
			// Nur zur Veranschaulichung von both().and()
			// Wenn Gleichheit mit einem anderen Namen, dann ja auch != null ...
			assertThat(p.getLastName(), both(is(notNullValue())).and(is(lastName)));
			
			// Veranschaulichung von allOf: mehrere Argumente moeglich,
			// d.h. nicht nur 2 wie bei both().and()
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
	public void findProfileWithInvalidProfileId() throws ProfileNotFoundException, InvalidProfileIdException {
		final Integer profileId = PROFILE_ID_INVALID;

		thrown.expect(InvalidProfileIdException.class);
		thrown.expectMessage(profileId.toString());
		
		pm.findProfileById(profileId, LOCALE);
	}

	@Test // error
	public void createProfile() throws ProfileDuplikatException, ProfileValidationException {
		final String lastName = PROFILE_NEW_LAST_NAME;
		final String email = PROFILE_NEW_EMAIL;
		final String telephoneNo = PROFILE_NEW_TELEPHONE_NO;
		final char role = PROFILE_ROLE_CUSTOMER;
		final char pStatus = PROFILE_STATUS;
		final String name = ADDRESS_NEW_NAME;
		final String road = ADDRESS_NEW_ROAD;
		final String houseNo = ADDRESS_NEW_HOUSE_NO;
		final String zipCode = ADDRESS_NEW_ZIP_CODE;
		final String city = ADDRESS_NEW_CITY;
		final char aStatus = ADDRESS_STATUS;
		
		final Profile newProfile = new Profile();
		final Address address = new Address();
		
		newProfile.setLastName(lastName);
		newProfile.setEmail(email);
		newProfile.setTelephoneNo(telephoneNo);
		newProfile.setRole(role);
		newProfile.setStatus(pStatus);
		
		newProfile.setAddress(address);
		newProfile.getAddress().setName(name);
		newProfile.getAddress().setRoad(road);
		newProfile.getAddress().setHouseNo(houseNo);
		newProfile.getAddress().setZipCode(zipCode);
		newProfile.getAddress().setCity(city);
		newProfile.getAddress().setStatus(aStatus);
		
		pm.createProfile(newProfile, LOCALE, false);
		assertThat(newProfile, is(notNullValue()));
	}
	
	@Test
	public void updateProfile() throws ProfileNotFoundException, InvalidProfileIdException
	{
		final Integer profileId = PROFILE_ID_AVAILABLE;
		final String newLastName = PROFILE_LAST_NAME_UNAVAILABLE;
		final Profile profile = pm.findProfileById(profileId, LOCALE);
		
		final String oldLastName = profile.getLastName();
		profile.setLastName(newLastName);
		
		assertThat(profile.getLastName()==oldLastName, is(false));
	}

	@Test
	public void deleteProfile()
		   throws ProfileDeleteOrderException, ProfileNotFoundException, InvalidProfileIdException, ProfileValidationException, ProfileDuplikatException {
		final Integer profileId = PROFILE_ID_WITHOUT_ORDERS;
		
		final Collection<Profile> profilePre = pm.findAllProfiles(PROFILE_ROLE_CUSTOMER);
	
		final Profile profile = pm.findProfileWithOrdersById(profileId, LOCALE);
		LOGGER.debug("profilelog: " + profile);
		assertThat(profile, is(notNullValue()));
		assertThat(profile.getOrders().isEmpty(), is(true));
		
		pm.deleteProfile(profile);
		
		final Collection<Profile> profilePost = pm.findAllProfiles(PROFILE_ROLE_CUSTOMER);
		assertThat(profilePre.size()-1, is(profilePost.size()));
	}
	
	@Test
	public void deactivateProfile() throws ProfileNotFoundException, InvalidProfileIdException, 
			InvalidEmailException, StatusIsAlreadySetException {
		final String email = PROFILE_NEW_EMAIL;
	
		final Profile profileBefore = pm.findProfileByEmail(email, LOCALE);
		assertThat(profileBefore, is(notNullValue()));
		
		pm.deactivateProfile(profileBefore.getProfileId());
	
		final Profile profileAfter = pm.findProfileByEmail(email, LOCALE);
		assertThat(profileAfter.getStatus(), is(PARAM_DEACTIVATE_PROFILE));
	}
	@Test
	public void showAddressForProfile() throws ProfileNotFoundException, InvalidProfileIdException
	{
		final Integer profileId = PROFILE_ID_WITH_ADDRESS;
		final Profile profile = pm.findProfileById(profileId, LOCALE);
		assertThat(profile.getAddress().getName(), is(ADDRESS_NAME_EXISTANT));
	}
	
}