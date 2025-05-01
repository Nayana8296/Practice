package WebDriverDemos;

import java.time.Duration;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

public class echoecho {

	public static void main(String[] args) {
		
		WebDriver driver = new ChromeDriver();
		driver.manage().window().maximize();
		driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
		
		driver.get("https://echoecho.com/htmlforms09.htm");
		
		JavascriptExecutor js =  (JavascriptExecutor)driver;
		js.executeScript("window.scrollBy(0,400)", "");
		
		WebElement milk = driver.findElement(By.name("option1"));
		 
		System.out.println("Before..");
		System.out.println("CheckBoxisSelected:"+milk.isSelected());
		System.out.println("CheckBoxisEnabled:"+milk.isEnabled());
		System.out.println("CheckBoxisDisplayed:"+milk.isDisplayed());
		
		milk.click();
		System.out.println("After....");
		System.out.println("CheckBoxisSelected:"+milk.isSelected());
		System.out.println("CheckBoxisEnabled:"+milk.isEnabled());
		System.out.println("CheckBoxisDisplayed:"+milk.isDisplayed());
		
	}

}