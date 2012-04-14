package jp.secret.sideroad;
import static org.junit.Assert.*;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

public class QunitAdapter {
	private Map<String,WebDriver> drivers;
	private String baseUrl;
	private FileServer server;
	private File[] files;
	private String serverRoot;
	private String targetRoot;
	private HtmlUnitDriver hud;
	private StringBuffer verificationErrors = new StringBuffer();

	@Before
	public void setUp() throws Exception {
		ResourceBundle bundle = ResourceBundle.getBundle("application");
		Integer port = Integer.valueOf(bundle.getString("test.port"));

		serverRoot = bundle.getString("test.server.root");
		targetRoot = bundle.getString("test.target.root");
		baseUrl = "http://localhost:" + port + "/";

		server = new FileServer(port, serverRoot);
		server.start();

		FileSearch search = new FileSearch();
		files = search.listFiles(targetRoot, bundle.getString("test.file"),
				bundle.getString("test.match"));

		drivers = new HashMap<String, WebDriver>();
		String[] browsers = bundle.getString("webdriver.browsers").split(",");

		hud = new HtmlUnitDriver();

		for (String browserName : browsers) {
			Browser browser = Browser.toEnum(browserName);
			if(browser != null){
				drivers.put(browserName, browser.newWebDriver(bundle));
			}
		}

		for(WebDriver webDriver : drivers.values()){
			webDriver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
		}
	}

	@Test
	public void testQunit() throws Exception {
		for (File file : files) {
			if( file == null ) {
				continue;
			}
			String url = baseUrl + file.getAbsolutePath().replace(serverRoot, "").replace("\\","/");
			hud.get(url);
			String title = hud.getTitle();
			System.out.println(url);
			System.out.println(title);

			for (String browser : drivers.keySet()) {
				WebDriver driver = drivers.get(browser);

				driver.get(url);

				for (int second = 0;; second++) {
					if (second >= 30) {
						System.out.println("timeout");
						break;
					}
					try {
						if (isElementPresent(driver,
								By.cssSelector("#qunit-testresult"))) {
							break;
						}
					} catch (Exception e) {
					}
					Thread.sleep(1000);
				}

				String total = "0";
				String passed = "";
				String failed = "";

				try {
					passed = driver.findElement(
							By.cssSelector("#qunit-testresult span.passed"))
							.getText();
					total = driver.findElement(
							By.cssSelector("#qunit-testresult span.total"))
							.getText();
					failed = driver.findElement(
							By.cssSelector("#qunit-testresult span.failed"))
							.getText();
				} catch (NoSuchElementException e) {
				}
				boolean isSuccessed = passed.equals(total)
						&& failed.equals("0");

				String result = ((isSuccessed) ? "Success!" : "Failed!")
						+ " : Passed [" + passed + "] Failed [" + failed +"]"
						+ " : " + browser;
				System.out.println( result );

				if(!isSuccessed){
					verificationErrors.append(file.getAbsolutePath()+" : " + title + " : " + result );
				}

			}
		}
	}

	@After
	public void tearDown() throws Exception {
		for (String browser : drivers.keySet()) {
			WebDriver driver = drivers.get(browser);
			driver.quit();
			String verificationErrorString = verificationErrors.toString();
			if (!"".equals(verificationErrorString)) {
				fail(verificationErrorString);
			}
		}
		server.stop();
	}

	private boolean isElementPresent(WebDriver driver, By by) {
		try {
			driver.findElement(by);
			return true;
		} catch (NoSuchElementException e) {
			return false;
		}
	}
}
