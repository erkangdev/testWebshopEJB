
package en.webshop.test;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.ejb.EJB;
import javax.inject.Inject;

import org.dbunit.DatabaseUnitException;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.security.client.SecurityClient;
import org.jboss.security.client.SecurityClientFactory;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import en.webshop.articleManagement.service.ArticleManagement;
import en.webshop.articleManagement.service.ArticleNotFoundException;
import en.webshop.articleManagement.domain.Article;
import en.webshop.articleManagement.service.ArticleManagement;
import en.webshop.orderManagement.service.OrderNotFoundException;
import en.webshop.orderManagement.domain.OrderPosition;
import en.webshop.orderManagement.domain.Order;
import en.webshop.orderManagement.service.InvalidOrderIdException;
import en.webshop.orderManagement.service.OrderDuplicateException;
import en.webshop.orderManagement.service.OrderManagement;
import en.webshop.orderManagement.service.OrderPositionNotFoundException;
import en.webshop.orderManagement.service.OrderValidationException;
import en.webshop.profileManagement.service.ProfileNotFoundException;
import en.webshop.profileManagement.domain.Profile;
import en.webshop.profileManagement.service.InvalidEmailException;
import en.webshop.profileManagement.service.ProfileManagement;
import en.webshop.test.util.DbReload;
import en.webshop.test.util.DbReloadProvider;

@RunWith(Arquillian.class)
public class OrderManagementTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(ArticleManagementTest.class);
	
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
	
	private static final String PROFILE_ID_EXISTENT = "501"; // Integer.valueOf(501); ?
	private static final String PROFILE_EMAIL_EXISTENT = "max@hs-karlsruhe.de";
	private static final Integer ORDER_ID_EXISTENT = 700;
	private static final String ITEM_1_ID = "500";
	private static final short ITEM_1_AMOUNT = 1;
	private static final String ITEM_2_ID = "501";
	private static final short ITEM_2_AMOUNT = 2;
	
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
	 * Dieser Test sollte fehlschlagen?
	 * TODO comment bearbeiten
	 * @throws OrderNotFoundException
	 * @throws InvalidOrderIdException 
	 */
	@Test
	public void findOrderByFaultId() throws OrderNotFoundException, InvalidOrderIdException{
		thrown.expect(OrderNotFoundException.class);
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
	public void addLineItemToOrder() 
			throws ProfileNotFoundException, OrderDuplicateException, OrderValidationException, ArticleNotFoundException, InvalidEmailException, OrderNotFoundException, OrderPositionNotFoundException {
	
		final String 	profileEmail 	= PROFILE_EMAIL_EXISTENT; 	// "max@hs-karlsruhe.de"
		final String 	item1Id 	= ITEM_1_ID; 			// "500"
		
		Profile profile = pm.findProfileByEmail(profileEmail, LOCALE);
		Article article = am.findArticleByArticleNo(item1Id);
		List<Order> orders = om.findOrdersByCustomer(profile);
		Order order = orders.get(1);
		Long orderId = order.getId();
		
		// hallo
		List<OrderPosition> orderPositions = om.findOrderPositions(orderId);
		int countLineItems = orderPositions.size();

		OrderPosition orderPosition = new OrderPosition(article); 
		orderPositions.add(orderPosition);
		
		assertThat(orderPositions.size(), is(countLineItems+1));
		
	}
	
	
	@SuppressWarnings("unchecked")
	@Test
	public void createOrder() throws ProfileNotFoundException, OrderDuplicateException, OrderValidationException, ArticleNotFoundException, InvalidEmailException {
		final String 	profileId 	= PROFILE_ID_EXISTENT; 	//501
		final String 	item1Id 	= ITEM_1_ID; 			//500
		final short 	item1Amount = ITEM_1_AMOUNT; 		//1
		final String 	item2Id 	= ITEM_2_ID; 			//501
		final short 	item2Amount = ITEM_2_AMOUNT; 		//2
		
		Order order = new Order();
		final List<OrderPosition> orderPosition = new ArrayList<OrderPosition>(); 
		order.setOrderPositions(orderPosition);
		
		Article article = am.findArticleByArticleNo(item1Id);
		OrderPosition pos = new OrderPosition(article);
		pos.setQuantity(item1Amount);
		order.getOrderPositions().add(pos);
		pos.setOrder(order);
		
		
		article = am.findArticleByArticleNo(item2Id);
		pos = new OrderPosition(article);
		pos.setQuantity(item2Amount);
		order.getOrderPositions().add(pos);
		pos.setOrder(order);

		Profile profile = pm.findProfileByEmail(profileId, LOCALE);
		order.setCustomer(profile);
		profile.getOrders().add(order);
		
		order = om.createOrder(order, Locale.getDefault(), false);
		assertThat(order.getOrderPositions().size(), is(2));
		for (OrderPosition li: order.getOrderPositions()) {
			assertThat(li.getArticle().getArticleNo(), anyOf(is(item1Id), is(item2Id)));
		}
			
		profile = order.getCustomer();
		assertThat(profile.getEmail(), is(profileId));
	}
	
}
