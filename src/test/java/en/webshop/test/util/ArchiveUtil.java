package en.webshop.test.util;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.importer.ExplodedImporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import en.webshop.test.ArticleManagementTest;
import en.webshop.test.ProfileManagementTest;


public abstract class ArchiveUtil {
	private static final Logger LOGGER = LoggerFactory.getLogger(ArchiveUtil.class);
	
	private static final String PROJEKT_NAME = "hska";
	
	private static final String EAR_DIR = "../" + PROJEKT_NAME + "/EarContent";
	private static final String EJB_DIR = "../" + PROJEKT_NAME + "EJB/build/classes";
	
	private static final String NEWLINE = System.getProperty("line.separator");
	
	private static final EnterpriseArchive EAR = createTestArchive();
	
	private static EnterpriseArchive createTestArchive() {
		// EAR-Archiv muss test.ear heissen, damit JNDI-Namen richtig aufgeloest werden
		final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "test.ear");
		
		// In das Archiv ein "exploded" Archiv importieren, d.h. ein Directory
		ear.as(ExplodedImporter.class).importDirectory(EAR_DIR);
		// META-INF\application.xml im EAR setzen, um test.war fuer Arquillian zu deklarieren
		ear.setApplicationXML("application.xml");

		// EJB-Modul
		final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, PROJEKT_NAME + "EJB.jar");
		ejbJar.as(ExplodedImporter.class).importDirectory(EJB_DIR);
		// Testklassen mitverpacken, damit sie im JBoss aufgerufen werden
		ejbJar.addPackage(ProfileManagementTest.class.getPackage());
		ejbJar.addPackage(ArticleManagementTest.class.getPackage());
		// Die Hilfsklassen DbReloadProvider und DbReload werden in den JUnit-Tests aufgerufen und
		// muessen deshalb mitverpackt werden
		ejbJar.addClasses(DbReloadProvider.class, DbReload.class);
		ear.addModule(ejbJar);
		
		LOGGER.info(NEWLINE
				    + ear.toString(true)
				    + NEWLINE + NEWLINE
				    + ejbJar.toString(true)
				    + NEWLINE);
		
		return ear;
	}
	
	public static EnterpriseArchive getTestArchive() {
		return EAR;
	}
}
