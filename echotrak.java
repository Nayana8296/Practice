package WebDriverDemos;

import java.time.Duration;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

public class echotrak {

	public static void main(String[] args) {
		
		WebDriver driver = new ChromeDriver();
		driver.manage().window().maximize();
		driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5000));
		
		driver.get("https://www.echotrak.com/Login.aspx?ReturnUrl=%2f");
		
		WebElement username = driver.findElement(By.id("txtCustomerID"));
		username.sendKeys("Nayana");
		
		WebElement password = driver.findElement(By.cssSelector("input[type=password]"));
		password.sendKeys("Nayana@8296");
		
		WebElement loginBtn = driver.findElement(By.name("Butsub"));
		loginBtn.click();
		
		WebElement errormessage = driver.findElement(By.id("lblMsg"));
		System.out.println(errormessage.getText());
		

	}

}
