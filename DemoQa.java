package WebDriverDemos;

import java.time.Duration;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.Select;

public class DemoQa {

	public static void main(String[] args) throws InterruptedException {
		WebDriver driver = new ChromeDriver();
		driver.manage().window().maximize();
		driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
		driver.get("https://demoqa.com/automation-practice-form");
		
		
		JavascriptExecutor js = (JavascriptExecutor)driver;
		js.executeScript("window.scrollBy(0,500)", "");
		driver.findElement(By.id("firstName")).sendKeys("Nayana");
		
		driver.findElement(By.id("lastName")).sendKeys("Kenchagundi");
		
		driver.findElement(By.id("userEmail")).sendKeys("nayanakenchagundi@gmail.com");
		
		js.executeScript("arguments[0].click()",driver.findElement(By.xpath("//*[@id=\"genterWrapper\"]/div[2]/div[2]/label")));
		/*WebElement gnfemale=driver.findElement(By.xpath("//*[@id=\"genterWrapper\"]/div[2]/div[2]/label"));
		gnfemale.click();*/
		
		WebElement mobilenumber = driver.findElement(By.xpath("//*[@id=\"userNumber\"]"));
		mobilenumber.sendKeys("8296089226");
		
		WebElement inputfield = driver.findElement(By.id("dateOfBirthInput"));
		inputfield.sendKeys(Keys.CONTROL+"A");
		
		WebElement inputfield1= driver.findElement(By.id("dateOfBirthInput"));
		inputfield1.sendKeys("4 oct 2001");
		inputfield1.sendKeys(Keys.TAB);
		inputfield1.sendKeys(Keys.ENTER);
		
		
		js.executeScript("window.scrollBy(0,400)", "");
		
		WebElement subjectsinput = driver.findElement(By.id("subjectsInput"));
		subjectsinput.sendKeys("M");
		driver.findElement(By.xpath("//*[@id=\"subjectsContainer\"]/div/div[1]/div[1]/div[1]"));
		subjectsinput.sendKeys(Keys.ENTER);
		
		
		
		driver.findElement(By.xpath("//*[@id=\"hobbiesWrapper\"]/div[2]/div[3]/label")).click();
		
		driver.findElement(By.id("uploadPicture")).sendKeys("C:\\Users\\HP\\OneDrive\\Pictures\\Screenshots\\2025-03-27.png");
		
		Thread.sleep(5000);
		
		driver.findElement(By.id("currentAddress")).sendKeys("Nekar Nagar,Old Hubballi"+Keys.TAB);
		
		//city
		js.executeScript("window.scrollBy(0,300)", "");
		
		
	   WebElement stateDropdown= driver.findElement(By.id("stateCity-wrapper"));
	   stateDropdown.click();
	   driver.findElement(By.xpath("//div[text()='Uttar Pradesh']")).click();
		
		WebElement cityDropdown = driver.findElement(By.id("city"));
		cityDropdown.click();
		driver.findElement(By.xpath("//div[text()='Agra']")).click();
		
		
		driver.findElement(By.id("submit")).click();
		
		
		
		
		
		
		
		

		
	}

}
