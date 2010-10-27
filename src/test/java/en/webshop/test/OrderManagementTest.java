
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
import en.webshop.orderManagement.service.OrderValidationException;
import en.webshop.profileManagement.service.ProfileNotFoundException;
import en.webshop.profileManagement.domain.Profile;
import en.webshop.profileManagement.service.InvalidEmailException;
import en.webshop.profileManagement.service.ProfileManagement;
import en.webshop.test.util.DbReload;

@RunWith(Arquillian.class)
public class OrderManagementTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(ArticleManagementTest.class);
	
	private static final Locale LOCALE = Locale.GERMAN;
	
	@Rule
	public ExpectedException thrown = ExpectedException.none();
	
	@Inject
	private OrderManagement om;
	
	@Inject
	private ProfileManagement pm;
	
	@Inject
	private ArticleManagement am;
	
	private static final Integer PROFILE_ID_EXISTENT = 501; // Integer.valueOf(501); ?
	private static final String PROFILE_EMAIL_EXISTENT = "max@hs-karlsruhe.de";
	private static final Integer ORDER_ID_EXISTENT = 700;
	private static final String ITEM_1_ID = "500";
	private static final short ITEM_1_AMOUNT = 1;
	private static final Integer ITEM_2_ID = 501;
	private static final short ITEM_2_AMOUNT = 2;
	
	/**
	 * Dieser Test sollte fehlschlagen?
	 * TODO comment bearbeiten
	 * @throws OrderNotFoundException
	 */
	@Test
	public void findOrderByFaultId() throws OrderNotFoundException{
		thrown.expect(OrderNotFoundException.class);
		try {
			Order order = om.findOrderByOrderId(new Long(-1), LOCALE);
			fail();
		} catch (InvalidOrderIdException e) {
			LOGGER.error("findOrderByFaultId: InvalidOrderIdException " + e.getStackTrace());
		}		
		
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
			throws ProfileNotFoundException, OrderDuplicateException, OrderValidationException, ArticleNotFoundException, InvalidEmailException, OrderNotFoundException {
	
		final String 	profileEmail 	= PROFILE_EMAIL_EXISTENT; 	// "max@hs-karlsruhe.de"
		final String 	item1Id 	= ITEM_1_ID; 			// "500"
		
		Profile profile = pm.findProfileByEmail(profileEmail, LOCALE);
		Article article = am.findArticleByArticleNo(item1Id);
		List<Order> orders = om.findOrdersByCustomer(profile);
		Order order = orders.get(1);
		Long orderId = order.getId();
		
		
		List<OrderPosition> orderPositions = ((OrderManagement) om).findOrderPositions(orderId);
		int countLineItems = orderPositions.size();

		OrderPosition orderPosition = new OrderPosition(article); 
		orderPositions.add(orderPosition);
		
		assertThat(orderPositions.size(), is(countLineItems+1));
		
	}
	
	
	@SuppressWarnings("unchecked")
	@Test
	public void createOrder() throws ProfileNotFoundException, OrderDuplicateException, OrderValidationException, ItemNotFoundException, InvalidProfileIdException {
		final Integer 	profileId 	= PROFILE_ID_EXISTENT; 	//501
		final String 	item1Id 	= ITEM_1_ID; 			//500
		final short 	item1Amount = ITEM_1_AMOUNT; 		//1
		final Integer 	item2Id 	= ITEM_2_ID; 			//501
		final short 	item2Amount = ITEM_2_AMOUNT; 		//2
		
		Order order = new Order();
		final List<OrderPosition> orderPosition = new ArrayList<OrderPosition>(); 
		order.setOrderPositions(orderPosition);
		
		Article article = am.findArticleByArticleNo(item1Id);
		OrderPosition pos = new OrderPosition(article);
		pos.setQuantity(item1Amount);
		order.getLineItems().add(pos);
		pos.setOrder(order);
		
		
		article = am.findItemById(item2Id);
		pos = new LineItem(article);
		pos.setAmount(item2Amount);
		order.getLineItems().add(pos);
		pos.setOrder(order);

		// Profile profile = pm.findProfileById(profileId, LOCALE);
		Profile profile = pm.findProfileWithOrdersById(profileId, LOCALE);
		order.setProfile(profile);
		profile.getOrders().add(order);
		
		order = om.createOrder(order, Locale.getDefault(), false);
		assertThat(order.getLineItems().size(), is(2));
		for (LineItem li: order.getLineItems()) {
			assertThat(li.getItem().getItemId(), anyOf(is(item1Id), is(item2Id)));
		}
			
		profile = order.getProfile();
		assertThat(profile.getProfileId(), is(profileId));
	}
	
	@BeforeClass
	public static void reloadDB() throws SQLException, DatabaseUnitException, ClassNotFoundException, InstantiationException, IllegalAccessException, IOException {
		DbReload.reload();
	}
}
