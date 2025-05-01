package WebDriverDemos;
import java.time.Duration;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

public class AssignmentNo2 {



public static void main(String[] args) throws InterruptedException {
		
		WebDriver driver = new ChromeDriver();
		driver.manage().window().maximize();
		driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
		driver.get("https://demoqa.com/automation-practice-form");
		Thread.sleep(500);
		
		JavascriptExecutor js = (JavascriptExecutor)driver;
		
		js.executeScript("window.scrollBy(0,500)","");
		
		driver.findElement(By.id("firstName")).sendKeys("Nayana");
		
		driver.findElement(By.id("lastName")).sendKeys("Kenchagundi");
		
		driver.findElement(By.id("userEmail")).sendKeys("nayanakenchagundi@gmail.com");
		
		WebElement gnfemale = driver.findElement(By.xpath("//*[@id=\"genterWrapper\"]/div[2]/div[2]/label"));
		gnfemale.click();
		
	    driver.findElement(By.xpath("//input[@placeholder=\"Mobile Number\"]")).sendKeys("8296089226");

	    WebElement inputField = driver.findElement(By.id("dateOfBirthInput"));
	    inputField.sendKeys(Keys.CONTROL + "a");
	    
	    
	    //driver.findElement(By.id("dateOfBirthInput")).sendKeys("25 Feb 2025");
	    WebElement dobField= driver.findElement(By.id("dateOfBirthInput"));
	    dobField.sendKeys("25 Feb 2025");
	    dobField.sendKeys(Keys.TAB);
	    dobField.sendKeys(Keys.ENTER);
	    Thread.sleep(100);
	 
	    
	    Thread.sleep(100);                                                                                                                      
	  //js.executeScript("window.scrollBy(0,500)","");
        //Thread.sleep(1000);
	   //WebElement subjectCont = driver.findElement(By.xpath("//div[contains(@class, 'subjects')]"));
	   //subjectCont.sendKeys("eureyury");
	   //Thread.sleep(1000);
	   WebElement subjectContinput = driver.findElement(By.xpath("//input[@id='subjectsInput']"));
	   
	   subjectContinput.sendKeys("Maths");  
	   
	   Thread.sleep(300);
	  subjectContinput.sendKeys(Keys.ENTER);
	  Thread.sleep(300);
	   WebElement Hobbies=driver.findElement(By.xpath("//label[text()='Sports']"));
       
       Hobbies.click();
       
        
        
        //js.executeScript("window.scrollBy(0,500)","");	
        
        Thread.sleep(100);
        WebElement uploadFile = driver.findElement(By.id("uploadPicture"));
        uploadFile.sendKeys("C:/Users/Sam/OneDrive/Pictures/Screenshots/Desktop/Screenshot 2025-02-11 001739.png");
        Thread.sleep(200);
        driver.findElement(By.id("currentAddress")).sendKeys("Nekar Nagar,Hubballi"+Keys.TAB);
        Thread.sleep(200);
        js.executeScript("window.scrollBy(0,500)","");
        WebElement stateDropdown = driver.findElement(By.id("state"));
        stateDropdown.click();
        
        Thread.sleep(200);
        driver.findElement(By.xpath("//div[text()='NCR']")).click();
        Thread.sleep(200);
        WebElement cityDropdown = driver.findElement(By.id("city"));
        cityDropdown.click();
        Thread.sleep(500);
        driver.findElement(By.xpath("//div[text()='Delhi']")).click();
       
        driver.findElement(By.id("submit")).click();
}

	}


