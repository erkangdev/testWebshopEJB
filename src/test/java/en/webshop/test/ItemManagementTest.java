package en.webshop.test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import org.dbunit.DatabaseUnitException;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import en.webshop.itemManagement.db.AttributeNotFoundException;
import en.webshop.itemManagement.db.CategoryNotFoundException;
import en.webshop.itemManagement.db.ItemNotFoundException;
import en.webshop.itemManagement.pojo.Attribute;
import en.webshop.itemManagement.pojo.Category;
import en.webshop.itemManagement.pojo.Item;
import en.webshop.itemManagement.service.ItemManagement;
import en.webshop.test.util.DbReload;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations="classpath:test.xml")
public class ItemManagementTest {
	
	@Rule
	public ExpectedException thrown = ExpectedException.none();
	private static final Integer ITEMID_AVAILABLE = 501;
	private static final String CATEGORY_NAME_AVAILABLE = "Dimension";
	private static final String ATTRIBUTE_NAME_AVAILABLE = "Holz";

	@Inject
	private ItemManagement im;
	
	
	@BeforeClass
	public static void reloadDB() throws SQLException, DatabaseUnitException, ClassNotFoundException, InstantiationException, IllegalAccessException, IOException {
		DbReload.reload();
	}
	
	
	@Test
	public void findItemVorhanden() throws ItemNotFoundException {
		
		// ItemId 501
		final Integer itemId = ITEMID_AVAILABLE;
		
		// Suche ein objekt anhand einer vorhandenen itemID
		final Item item = im.findItemById(itemId);
		
		// Speichere alle attribute des item objekts in attributes 
		final List<Attribute> attributes = item.getAttributes();
		
		// Item 501 hat Attribute 5,12,14,?,
		assertThat(attributes.isEmpty(),is(false));
		
		// Hole alle items zu jedem attribute des objekts item
		if(attributes != null)
		{
			// Falls irgendwo die Verbindung nicht passt wird die var false
			// boolean doeswork = true;
			
			for (int i = 0; i < attributes.size(); i++) {

				// hole zu jedem attribute alle items
				List <Item> itemsPerAttribute = attributes.get(i).getItems();
				
				// Item(501).Attribute.Items.containItemWithID(501)
				for (int j = 0; j <itemsPerAttribute.size(); j++) {
					// Umstaendlich TODO: Weg wenn andere methode funktioniert:
					// ueberpruefe ob bei jedem attribute genau ein item 
					// das gleiche ist wie das objekt item
					/*
					Integer count = 0; 
					if (itemsPerAttribute.get(j).equals(item)) {
						count++;
					}
					if(count != 1)
						doeswork = false;
						*/
					if (itemsPerAttribute.get(j).getItemId() == 501)
						assertThat(item, is(sameInstance(itemsPerAttribute.get(j))));
				}
			}
		}
	}
	
	@Test
	public void findItemByFaultId() throws ItemNotFoundException{
		thrown.expect(ItemNotFoundException.class);
		Item item = im.findItemById(-1);
	}
	
	@Test
	public void findCategoriesAvailable() throws CategoryNotFoundException {
		final Collection<Category> categories = im.findCategories();
		assertThat(categories.isEmpty(),is(false));
	}
	
	@Test
	public void findCategoriesByName() throws CategoryNotFoundException {
		final String catname = CATEGORY_NAME_AVAILABLE;
		final Collection<Category> categories = im.findCategoriesByName(catname);
		assertThat(categories.isEmpty(),is(false));
		for (Category c: categories) {
			assertThat(c.getName().equals(catname), is(true));
		}
	}
	
	@Test
	public void findAttributesAvailable() throws AttributeNotFoundException {
		final Collection<Attribute> attributes = im.findAttributes();
		assertThat(attributes.isEmpty(),is(false));
	}
	
	@Test
	public void findAttributesByName() throws AttributeNotFoundException {
		final String attributename = ATTRIBUTE_NAME_AVAILABLE;
		final Collection<Attribute> attributes = im.findAttributesByName(attributename);
		assertThat(attributes.isEmpty(),is(false));
		for (Attribute a: attributes) {
			assertThat(a.getName().equals(attributename), is(true));
			assertThat(a.getCategory(), is(notNullValue()));
		}
	}
}