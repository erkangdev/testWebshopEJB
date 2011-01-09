package en.webshop.test;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.ejb.EJB;
import javax.ejb.EJBAccessException;
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

import en.webshop.articleManagement.domain.Article;
import en.webshop.articleManagement.service.ArticleManagement;
import en.webshop.articleManagement.service.ArticleNotFoundException;
import en.webshop.articleManagement.service.ArticleQuantityException;
import en.webshop.orderManagement.domain.Order;
import en.webshop.orderManagement.domain.OrderPosition;
import en.webshop.orderManagement.service.InvalidOrderIdException;
import en.webshop.orderManagement.service.NoOrderPostionsException;
import en.webshop.orderManagement.service.OrderDuplicateException;
import en.webshop.orderManagement.service.OrderManagement;
import en.webshop.orderManagement.service.OrderNotFoundException;
import en.webshop.orderManagement.service.OrderPositionNotFoundException;
import en.webshop.orderManagement.service.OrderValidationException;
import en.webshop.orderManagement.service.StatusAlreadySetException;
import en.webshop.profileManagement.domain.Profile;
import en.webshop.profileManagement.service.InvalidEmailException;
import en.webshop.profileManagement.service.ProfileManagement;
import en.webshop.profileManagement.service.ProfileNotFoundException;
import en.webshop.test.util.ArchiveUtil;
import en.webshop.test.util.DbReloadProvider;
import en.webshop.util.ConcurrentDeletedException;
import en.webshop.util.ConcurrentUpdatedException;

@RunWith(Arquillian.class)
public class OrderManagementTest {
	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private static final Logger LOGGER = LoggerFactory
			.getLogger(ArticleManagementTest.class);
	private static final String CONCURRENT_UPDATE = "update";
	private static final String CONCURRENT_DELETE = "delete";
	private static final Locale LOCALE = Locale.GERMAN;

	private static SecurityClient securityClient;

	@EJB
	private OrderManagement om;

	@EJB
	private ProfileManagement pm;

	@EJB
	private ArticleManagement am;

	private static final String PROFILE_EMAIL_EXISTENT = "max@hs-karlsruhe.de";
	private static final long ORDER_ID_EXISTENT = 502;
	private static final long ORDER_ID_EXISTENT_2 = 501;
	private static final String ARTICLE_NO_1 = "VZ90/10";
	private static final short ARTICLE_AMOUNT_1 = 1;
	private static final String ARTICLE_NO_2 = "VZ130/1011";
	private static final short ARTICLE_AMOUNT_2 = 2;

	private static final String USERNAME = "rd@sc.de";
	private static final String PASSWORD = "pass";

	private static final int MODE_OF_PAYMENT = 0;

	private static final String ADDR_NAME = "Schlossstr";

	private static final String ADDR_STREET = null;

	private static final String ADDR_HOUSE_NO = null;

	private static final String ADDR_POST_CODE = null;

	private static final String ADDR_CITY = null;

	private static final int ORDER_STATUS = 0;
	private static final int ORDER_STATUS_FINISHED = 2;

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
		} catch (Exception e) {
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

	/**
	 */
	@Test
	public void findOrderByFaultId() throws OrderNotFoundException,
			InvalidOrderIdException {
		thrown.expect(OrderNotFoundException.class);
		@SuppressWarnings("unused")
		Order order = om.findOrderByOrderId(new Long(-1), LOCALE);
	}

	@Test
	public void findOrdersByEmail() throws OrderNotFoundException {

		List<Order> orders = om.findOrdersByEmail(PROFILE_EMAIL_EXISTENT);

		assertThat(orders == null || orders.size() == 0, is(false));
	}
	
	@Test
	public void setOrderStatus() throws OrderNotFoundException, InvalidOrderIdException, StatusAlreadySetException, ConcurrentUpdatedException, ConcurrentDeletedException {
		Order order = om.findOrderByOrderId(ORDER_ID_EXISTENT, LOCALE);
		int statusOld = order.getStatus();
		om.setStatusForOrder(order, ORDER_STATUS_FINISHED, LOCALE);
		
		order = om.findOrderByOrderId(ORDER_ID_EXISTENT, LOCALE);
		int statusNew = order.getStatus();
		assertThat(statusOld == statusNew, is(false));
	}
	
	@Test
	public void findComplaintsByCustomer() throws ProfileNotFoundException, InvalidEmailException {
		Profile customer = pm.findProfileByEmail("tim-wiese@hs-karlsruhe.de", LOCALE);
		List<OrderPosition> complaints = om.findComplaintsByCustomer(customer);
		for (int i = 0; i < complaints.size(); i++)
			assertThat(complaints.get(i) == null, is(false));
	}

	@Test
	public void findOrderPositionQuantity(Long orderId) throws OrderPositionNotFoundException {
		
		List<OrderPosition> orderPosition = om.findOrderPositions(ORDER_ID_EXISTENT_2);
		
		OrderPosition pos1 = orderPosition.get(0);
		int quantity = pos1.getQuantity();
		
		assertThat(quantity == 7, is (true) );
	}

	/*
	 * @Test public void findProfileByOrderIdExistent() throws
	 * ProfileNotFoundException, OrderNotFoundException { final Integer orderId
	 * = ORDER_ID_EXISTENT;
	 * assertThat(om.findProfileByOrderId(Integer.valueOf(orderId)),
	 * is(notNullValue()));
	 * 
	 * Profile testProfile = om.findProfileByOrderId(Integer.valueOf(orderId));
	 * 
	 * }
	 */

	/*
	 * @Test public void findOrderByProfileExistent() throws
	 * OrderNotFoundException { final Integer profileId = PROFILE_ID_EXISTENT;
	 * List<Order> orderList = om.findOrdersByProfileId(profileId);
	 * assertThat(orderList.size(), is(2)); }
	 */

	@Test
	public void addOrderPositionToOrder() throws ProfileNotFoundException,
			OrderDuplicateException, OrderValidationException,
			ArticleNotFoundException, InvalidEmailException,
			OrderNotFoundException, OrderPositionNotFoundException {

		final String profileEmail = PROFILE_EMAIL_EXISTENT; // max@hs-karlsruhe.de
		final String articleNo1 = ARTICLE_NO_1; // VZ90/10

		Profile profile = pm.findProfileByEmail(profileEmail, LOCALE);
		Article article = am.findArticleByArticleNo(articleNo1);
		List<Order> orders = om.findOrdersByCustomer(profile);
		Order order = orders.get(1);
		Long orderId = order.getId();

		List<OrderPosition> orderPositions = om.findOrderPositions(orderId);
		int countOrderPositions = orderPositions.size();

		OrderPosition orderPosition = new OrderPosition(article);
		orderPositions.add(orderPosition);

		assertThat(orderPositions.size(), is(countOrderPositions + 1));
	}

	@Test
	public void createOrder() throws ProfileNotFoundException,
			OrderDuplicateException, OrderValidationException,
			ArticleNotFoundException, InvalidEmailException,
			ArticleQuantityException, ConcurrentDeletedException, ConcurrentUpdatedException, NoOrderPostionsException {
		final String profileId = PROFILE_EMAIL_EXISTENT;
		final String articleNo1 = ARTICLE_NO_1; // VZ90/10
		final short amount1 = ARTICLE_AMOUNT_1; // 1
		final String articleNo2 = ARTICLE_NO_2; // VZ130/1011
		final short amount2 = ARTICLE_AMOUNT_2; // 2

		Order order = new Order();
		final List<OrderPosition> orderPosition = new ArrayList<OrderPosition>();
		order.setOrderPositions(orderPosition);

		Article article = am.findArticleByArticleNo(articleNo1);
		OrderPosition pos = new OrderPosition(article);
		pos.setQuantity(amount1);
		order.getOrderPositions().add(pos);
		pos.setOrder(order);

		article = am.findArticleByArticleNo(articleNo2);
		pos = new OrderPosition(article);
		pos.setQuantity(amount2);
		order.getOrderPositions().add(pos);
		pos.setOrder(order);

		Profile profile = pm.findProfileWithOrdersByEmail(profileId, LOCALE);
		order.setCustomer(profile);
		order = om.createOrder(order, Locale.getDefault(), false);

		profile.addOrder(order);

		assertThat(order.getOrderPositions().size(), is(2));
		for (OrderPosition li : order.getOrderPositions()) {
			assertThat(li.getArticle().getArticleNo(),
					anyOf(is(articleNo1), is(articleNo2)));
		}

		profile = order.getCustomer();
		assertThat(profile.getEmail(), is(profileId));
	}
	
	@Test
	public void createOrderLogout() throws ProfileNotFoundException,
			OrderDuplicateException, OrderValidationException,
			ArticleNotFoundException, InvalidEmailException,
			ArticleQuantityException, ConcurrentDeletedException, ConcurrentUpdatedException, NoOrderPostionsException {
		
		securityClient.logout();
		
		thrown.expect(EJBAccessException.class);
		
		final String profileId = PROFILE_EMAIL_EXISTENT;
		final String articleNo1 = ARTICLE_NO_1; // VZ90/10
		final short amount1 = ARTICLE_AMOUNT_1; // 1
		final String articleNo2 = ARTICLE_NO_2; // VZ130/1011
		final short amount2 = ARTICLE_AMOUNT_2; // 2

		Order order = new Order();
		final List<OrderPosition> orderPosition = new ArrayList<OrderPosition>();
		order.setOrderPositions(orderPosition);

		Article article = am.findArticleByArticleNo(articleNo1);
		OrderPosition pos = new OrderPosition(article);
		pos.setQuantity(amount1);
		order.getOrderPositions().add(pos);
		pos.setOrder(order);

		article = am.findArticleByArticleNo(articleNo2);
		pos = new OrderPosition(article);
		pos.setQuantity(amount2);
		order.getOrderPositions().add(pos);
		pos.setOrder(order);

		Profile profile = pm.findProfileWithOrdersByEmail(profileId, LOCALE);
		order.setCustomer(profile);
		order = om.createOrder(order, Locale.getDefault(), false);

		profile.addOrder(order);

		assertThat(order.getOrderPositions().size(), is(2));
		for (OrderPosition li : order.getOrderPositions()) {
			assertThat(li.getArticle().getArticleNo(),
					anyOf(is(articleNo1), is(articleNo2)));
		}

		profile = order.getCustomer();
		assertThat(profile.getEmail(), is(profileId));
	}

	@Test
	public void testArticleQuantytyNotEnough() throws ArticleNotFoundException, ProfileNotFoundException, InvalidEmailException, OrderValidationException, OrderDuplicateException, ArticleQuantityException, ConcurrentDeletedException, ConcurrentUpdatedException, NoOrderPostionsException {
		
		thrown.expect(ArticleQuantityException.class);
		
		final String profileEmail = PROFILE_EMAIL_EXISTENT;
		final String articleNo1   = ARTICLE_NO_1; // VZ90/10
		final short amount1       = ARTICLE_AMOUNT_1; // 1
		final String articleNo2   = ARTICLE_NO_2;     // VZ130/1011
		final short amount2       = ARTICLE_AMOUNT_2; // 2
		final int modeOfPayment   = MODE_OF_PAYMENT;  // 0
		final String addrName     = ADDR_NAME;
		final String addrStreet   = ADDR_STREET;
		final String addrHouseNo  = ADDR_HOUSE_NO;
		final String addrPostcode = ADDR_POST_CODE;
		final String addrCity     = ADDR_CITY;
		final int status          = ORDER_STATUS; 
		
		Article article = am.findArticleByArticleNo(articleNo1);
		OrderPosition op = new OrderPosition(article, 1000);
		List<OrderPosition> orderPositions = new ArrayList<OrderPosition>();
		orderPositions.add(op);
		

		Profile customer = pm.findProfileByEmail(profileEmail, LOCALE);
		
		
		Order o = new Order(customer, modeOfPayment, addrName, addrStreet,
				addrHouseNo, addrPostcode, addrCity, status, orderPositions);
		
		om.createOrder(o, LOCALE, true);
		
		/*Order o2 = new Order(customer, modeOfPayment, addrName, addrStreet,
				addrHouseNo, addrPostcode, addrCity, status, orderPositions);
		
		thrown.expect(ArticleQuantityException.class);
		
		om.createOrder(o2, LOCALE, true);*/
		
		assertThat(true, is(true));

	}

	/**
	 */
	private class ConcurrencyHelper extends Thread {
//		private String cmd;
//		private Long orderId;
//
//		/**
//		 */
//		private ConcurrencyHelper(String cmd, Long orderId) {
//			// Der Thread erhaelt das Kommando zzgl. Kundennr als Name
//			super(cmd + "_" + orderId);
//			this.cmd = cmd;
//			this.orderId = orderId;
//		}
//
//		/**
//		 */
//		@Override
//		public void run() {
//			LOGGER.debug("BEGINN run");
//
//			if ("update".equals(cmd)) {
//				update();
//			} else if ("delete".equals(cmd)) {
//				delete();
//			} else {
//				System.err
//						.println("Zulaessige Kommandos: \"update\" und \"delete\"");
//			}
//			LOGGER.debug("ENDE run");
//			return;
//		}

		/**
		 */
//		private void update() {
//			LOGGER.debug("BEGINN update");
//
//			try {
//				final Order order = om.findOrderByOrderId(orderId, LOCALE);
//				order.setAddrStreet(order.getAddrStreet() + "concurrent");
//				om.equals(obj);
//				kv.updateKunde(kunde, LOCALE, false, false);
//			} catch (Exception e) {
//				throw new IllegalStateException(e);
//			}
//
//			LOGGER.debug("ENDE update");
//		}
//
//		/**
//		 */
//		private void delete() {
//			LOGGER.debug("BEGINN delete");
//
//			securityClient.logout();
//			securityClient.setSimple(USERNAME_ADMIN, PASSWORD_ADMIN);
//			try {
//				securityClient.login();
//			} catch (LoginException e) {
//				e.printStackTrace();
//				throw new RuntimeException(e);
//			}
//
//			try {
//				kv.deleteKundeById(orderId, LOCALE);
//			} catch (Exception e) {
//				throw new IllegalStateException(e);
//			}
//
//			LOGGER.debug("ENDE delete");
//		}
	}

}
