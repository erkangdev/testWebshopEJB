
package en.webshop.test;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import org.dbunit.DatabaseUnitException;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import en.webshop.itemManagement.db.ItemNotFoundException;
import en.webshop.itemManagement.pojo.Item;
import en.webshop.itemManagement.service.ItemManagement;
import en.webshop.orderManagement.db.OrderNotFoundException;
import en.webshop.orderManagement.pojo.LineItem;
import en.webshop.orderManagement.pojo.Order;
import en.webshop.orderManagement.service.OrderDuplicateException;
import en.webshop.orderManagement.service.OrderManagement;
import en.webshop.orderManagement.service.OrderValidationException;
import en.webshop.profileManagement.db.ProfileNotFoundException;
import en.webshop.profileManagement.pojo.Profile;
import en.webshop.profileManagement.service.InvalidProfileIdException;
import en.webshop.profileManagement.service.ProfileManagement;
import en.webshop.test.util.DbReload;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations="classpath:test.xml")
public class OrderManagementTest {
	
	private static final Locale LOCALE = Locale.GERMAN;
	
	@Rule
	public ExpectedException thrown = ExpectedException.none();
	
	@Inject
	private OrderManagement om;
	
	@Inject
	private ProfileManagement pm;
	
	@Inject
	private ItemManagement im;
	
	private static final Integer PROFILE_ID_EXISTENT = 501; // Integer.valueOf(501); ?
	private static final Integer ORDER_ID_EXISTENT = 700;
	private static final Integer ITEM_1_ID = 500;
	private static final short ITEM_1_AMOUNT = 1;
	private static final Integer ITEM_2_ID = 501;
	private static final short ITEM_2_AMOUNT = 2;
	
	
	@Test
	public void findOrderByFaultId() throws OrderNotFoundException{
		thrown.expect(OrderNotFoundException.class);
		Order order = om.findOrderByOrderId(-1);
	}
	
	@Test
	public void findProfileByOrderIdExistent() throws ProfileNotFoundException, OrderNotFoundException {	
		final Integer orderId = ORDER_ID_EXISTENT;
		assertThat(om.findProfileByOrderId(Integer.valueOf(orderId)), is(notNullValue()));
		
		Profile testProfile = om.findProfileByOrderId(Integer.valueOf(orderId));
		
	}
	
	@Test
	public void findOrderByProfileExistent() throws OrderNotFoundException {
		final Integer profileId = PROFILE_ID_EXISTENT;
		List<Order> orderList = om.findOrdersByProfileId(profileId);
		assertThat(orderList.size(), is(2));
	}	


	@Test
	public void addLineItemToOrder() 
			throws ProfileNotFoundException, OrderDuplicateException, OrderValidationException, ItemNotFoundException, InvalidProfileIdException, OrderNotFoundException {
	
		final Integer 	profileId 	= PROFILE_ID_EXISTENT; 	// 501
		final Integer 	item1Id 	= ITEM_1_ID; 			// 500
		
		Profile profile = pm.findProfileById(profileId, LOCALE);
		Item item = im.findItemById(item1Id);
		List<Order> orders = om.findOrders(profile);
		Order order = orders.get(1);
		
		
		List<LineItem> lineItems = om.findLineItemsByOrder(order);
		int countLineItems = lineItems.size();

		LineItem lineitem = new LineItem(item); 
		lineItems.add(lineitem);
		
		assertThat(lineItems.size(), is(countLineItems+1));
		
	}
	
	
	@SuppressWarnings("unchecked")
	@Test
	public void createOrder() throws ProfileNotFoundException, OrderDuplicateException, OrderValidationException, ItemNotFoundException, InvalidProfileIdException {
		final Integer 	profileId 	= PROFILE_ID_EXISTENT; 	//501
		final Integer 	item1Id 	= ITEM_1_ID; 			//500
		final short 	item1Amount = ITEM_1_AMOUNT; 		//1
		final Integer 	item2Id 	= ITEM_2_ID; 			//501
		final short 	item2Amount = ITEM_2_AMOUNT; 		//2
		
		Order order = new Order();
		final List<LineItem> lineItems = new ArrayList<LineItem>(); 
		order.setLineItems(lineItems);
		
		Item item = im.findItemById(item1Id);
		LineItem pos = new LineItem(item);
		pos.setAmount(item1Amount);
		order.getLineItems().add(pos);
		pos.setOrder(order);
		
		
		item = im.findItemById(item2Id);
		pos = new LineItem(item);
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
