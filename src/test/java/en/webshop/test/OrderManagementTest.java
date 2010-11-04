
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

import en.webshop.articleManagement.domain.Article;
import en.webshop.articleManagement.service.ArticleManagement;
import en.webshop.articleManagement.service.ArticleNotFoundException;
import en.webshop.orderManagement.domain.Order;
import en.webshop.orderManagement.domain.OrderPosition;
import en.webshop.orderManagement.service.InvalidOrderIdException;
import en.webshop.orderManagement.service.OrderDuplicateException;
import en.webshop.orderManagement.service.OrderManagement;
import en.webshop.orderManagement.service.OrderNotFoundException;
import en.webshop.orderManagement.service.OrderPositionNotFoundException;
import en.webshop.orderManagement.service.OrderValidationException;
import en.webshop.profileManagement.domain.Profile;
import en.webshop.profileManagement.service.InvalidEmailException;
import en.webshop.profileManagement.service.ProfileManagement;
import en.webshop.profileManagement.service.ProfileNotFoundException;
import en.webshop.test.util.ArchiveUtil;
import en.webshop.test.util.DbReloadProvider;

@RunWith(Arquillian.class)
public class OrderManagementTest {
	
	private static final Locale LOCALE = Locale.GERMAN;
	
	@Rule
	public ExpectedException thrown = ExpectedException.none();
	
	private static SecurityClient securityClient;
	
	@EJB
	private OrderManagement om;
	
	@EJB
	private ProfileManagement pm;
	
	@EJB
	private ArticleManagement am;
	
	private static final String PROFILE_EMAIL_EXISTENT = "max@hs-karlsruhe.de";
	// private static final Integer ORDER_ID_EXISTENT = 700;
	private static final String ARTICLE_NO_1 = "VZ90/10";
	private static final short ARTICLE_AMOUNT_1 = 1;
	private static final String ARTICLE_NO_2 = "VZ130/1011";
	private static final short ARTICLE_AMOUNT_2 = 2;
	
	private static final String USERNAME = "rd@sc.de";
	private static final String PASSWORD = "pass";
	
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
	
	/**
	 */
	@Test
	public void findOrderByFaultId() throws OrderNotFoundException, InvalidOrderIdException{
		thrown.expect(OrderNotFoundException.class);
		@SuppressWarnings("unused")
		Order order = om.findOrderByOrderId(new Long(-1), LOCALE);
	}
	
	/*@Test
	public void findProfileByOrderIdExistent() throws ProfileNotFoundException, OrderNotFoundException {	
		final Integer orderId = ORDER_ID_EXISTENT;
		assertThat(om.findProfileByOrderId(Integer.valueOf(orderId)), is(notNullValue()));
		
		Profile testProfile = om.findProfileByOrderId(Integer.valueOf(orderId));
		
	}*/
	
	/*@Test
	public void findOrderByProfileExistent() throws OrderNotFoundException {
		final Integer profileId = PROFILE_ID_EXISTENT;
		List<Order> orderList = om.findOrdersByProfileId(profileId);
		assertThat(orderList.size(), is(2));
	}*/	

	@Test
	public void addOrderPositionToOrder() 
			throws ProfileNotFoundException, OrderDuplicateException, OrderValidationException, ArticleNotFoundException, InvalidEmailException, OrderNotFoundException, OrderPositionNotFoundException {
	
		final String profileEmail = PROFILE_EMAIL_EXISTENT; 	// max@hs-karlsruhe.de
		final String articleNo1 = ARTICLE_NO_1; 				// VZ90/10
		
		Profile profile = pm.findProfileByEmail(profileEmail, LOCALE);
		Article article = am.findArticleByArticleNo(articleNo1);
		List<Order> orders = om.findOrdersByCustomer(profile);
		Order order = orders.get(1);
		Long orderId = order.getId();
		
		List<OrderPosition> orderPositions = om.findOrderPositions(orderId);
		int countOrderPositions = orderPositions.size();

		OrderPosition orderPosition = new OrderPosition(article); 
		orderPositions.add(orderPosition);
		
		assertThat(orderPositions.size(), is(countOrderPositions+1));
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void createOrder() throws ProfileNotFoundException, OrderDuplicateException, OrderValidationException, ArticleNotFoundException, InvalidEmailException {
		final String profileId = PROFILE_EMAIL_EXISTENT;
		final String articleNo1 = ARTICLE_NO_1;				// VZ90/10
		final short amount1 = ARTICLE_AMOUNT_1;				// 1
		final String articleNo2 = ARTICLE_NO_2;				// VZ130/1011
		final short amount2 = ARTICLE_AMOUNT_2;				// 2
		
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

		Profile profile = pm.findProfileByEmail(profileId, LOCALE);
		order.setCustomer(profile);
		profile.getOrders().add(order);
		
		order = om.createOrder(order, Locale.getDefault(), false);
		assertThat(order.getOrderPositions().size(), is(2));
		for (OrderPosition li: order.getOrderPositions()) {
			assertThat(li.getArticle().getArticleNo(), anyOf(is(articleNo1), is(articleNo2)));
		}
			
		profile = order.getCustomer();
		assertThat(profile.getEmail(), is(profileId));
	}
}
