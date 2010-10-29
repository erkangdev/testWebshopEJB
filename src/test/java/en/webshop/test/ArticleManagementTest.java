package en.webshop.test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

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
import en.webshop.articleManagement.domain.Attribute;
import en.webshop.articleManagement.domain.Category;
import en.webshop.articleManagement.service.ArticleManagement;
import en.webshop.articleManagement.service.ArticleNotFoundException;
import en.webshop.articleManagement.service.AttributeNotFoundException;
import en.webshop.articleManagement.service.CategoryNotFoundException;
import en.webshop.test.util.ArchiveUtil;
import en.webshop.test.util.DbReloadProvider;


@RunWith(Arquillian.class)
public class ArticleManagementTest {
	
	@Rule
	public ExpectedException thrown = ExpectedException.none();
	
	private static SecurityClient securityClient;
	
	private static final String ARTICLE_NO_AVAILABLE = "VZ90/10";
	private static final String CATEGORY_NAME_AVAILABLE = "Dimension";
	private static final String ATTRIBUTE_NAME_AVAILABLE = "Holz";
	
	private static final String USERNAME = "mario-gomez@hs-karlsruhe.de";
	private static final String PASSWORD = "pass";
	
	@EJB
	private ArticleManagement am;
	
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
	
	@Test
	public void findArticleVorhanden() throws ArticleNotFoundException {
		
		// ArticleNo VZ90/10
		final String articleNo = ARTICLE_NO_AVAILABLE;
		
		// Suche ein objekt anhand einer vorhandenen articleNo
		final Article article = am.findArticleByArticleNo(articleNo);
		
		// Speichere alle attribute des article objekts in attributes 
		final List<Attribute> attributes = article.getAttributes();
		
		// Artikel VZ90/10 hat Attribute 5,12,14,?,
		assertThat(attributes.isEmpty(),is(false));
		
		// Hole alle Artikel zu jedem attribute des objekts article
		if(attributes != null)
		{
			// Falls irgendwo die Verbindung nicht passt wird die var false
			// boolean doeswork = true;
			
			for (int i = 0; i < attributes.size(); i++) {

				// hole zu jedem attribute alle articles
				List <Article> articlesPerAttribute = attributes.get(i).getArticles();
				
				// article(VZ90/10).Attribute.articles.containarticleWithID(VZ90/10)
				for (int j = 0; j <articlesPerAttribute.size(); j++) {
					// Umstaendlich TODO: Weg wenn andere methode funktioniert:
					// ueberpruefe ob bei jedem attribute genau ein article 
					// das gleiche ist wie das objekt article
					/*
					Integer count = 0; 
					if (articlesPerAttribute.get(j).equals(article)) {
						count++;
					}
					if(count != 1)
						doeswork = false;
						*/
					if (articlesPerAttribute.get(j).getArticleNo() == "501")
						assertThat(article, is(sameInstance(articlesPerAttribute.get(j))));
				}
			}
		}
	}
	
	@Test
	public void findarticleByFaultId() throws ArticleNotFoundException{
		thrown.expect(ArticleNotFoundException.class);
		@SuppressWarnings("unused")
		Article article = am.findArticleByArticleNo("-1");
	}
	
	@Test
	public void findCategoriesAvailable() throws CategoryNotFoundException {		
		final Collection<Category> categories = am.findAllCategories();
		assertThat(categories.isEmpty(),is(false));
	}
	
	@Test
	public void findCategoriesByName() throws CategoryNotFoundException {		
		final String catname = CATEGORY_NAME_AVAILABLE;
		final Collection<Category> categories = am.findCategoriesByName(catname);
		assertThat(categories.isEmpty(),is(false));
		for (Category c: categories) {
			assertThat(c.getName().equals(catname), is(true));
		}
	}
	
	@Test
	public void findAttributesAvailable() throws AttributeNotFoundException {
		final Collection<Attribute> attributes = am.findAllAttributes();
		assertThat(attributes.isEmpty(),is(false));
	}
	
	@Test
	public void findAttributesByName() throws AttributeNotFoundException {
		final String attributename = ATTRIBUTE_NAME_AVAILABLE;
		final Collection<Attribute> attributes = am.findAttributesByName(attributename);
		assertThat(attributes.isEmpty(),is(false));
		for (Attribute a: attributes) {
			assertThat(a.getName().equals(attributename), is(true));
			assertThat(a.getCategory(), is(notNullValue()));
		}
	}
}