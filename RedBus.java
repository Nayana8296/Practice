package WebDriverDemos;

import java.time.Duration;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

public class RedBus {

	public static void main(String[] args) throws InterruptedException {
		
		WebDriver driver = new ChromeDriver();
		driver.manage().window().maximize();
		//driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5000));
		
		driver.get("https://www.redbus.in/");
		
		/*WebElement source = driver.findElement(By.id("src"));
		source.sendKeys("Hubli");
		driver.findElement(By.xpath("//*[@id=\"autoSuggestContainer\"]/div/div/div[1]/div/div[1]/div/div/div/div/text")).click();
		Thread.sleep(5000);*/
		
		driver.findElement(By.id("src")).sendKeys("Kolh");
		driver.findElement(By.xpath("//*[@id=\"autoSuggestContainer\"]/div/div/div[1]/div/div[1]/ul/li[1]/div/text")).click();
		
		
		driver.findElement(By.id("dest")).sendKeys("Ban");
		driver.findElement(By.xpath("//*[@id=\"autoSuggestContainer\"]/div/div/div[3]/div[1]/ul/li[1]/div/text")).click();
		
		
		
		
		

	}

}
