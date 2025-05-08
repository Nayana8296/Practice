package LiveProject;

import java.awt.AWTException;
import java.awt.RenderingHints.Key;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.time.Duration;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.Select;

public class Tricentis {

	public static void main(String[] args) throws InterruptedException, AWTException {
		
		WebDriver driver = new ChromeDriver();
		driver.manage().window().maximize();
		driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(2000));
		
		driver.get("https://www.sparkstone.co.nz/sampleapp/101/app.php");
		
		
		driver.findElement(By.id("nav_automobile")).click();
		
		WebElement DrpDown = driver.findElement(By.name("Make"));
		DrpDown.click();
		
		Select select = new Select(DrpDown);
		
		select.selectByVisibleText("Honda");
		
		driver.findElement(By.id("engineperformance")).sendKeys("80");
		
		driver.findElement(By.id("dateofmanufacture")).sendKeys(Keys.CONTROL +"A");
		driver.findElement(By.id("dateofmanufacture")).sendKeys("10/04/2001");
		
		driver.findElement(By.name("Number of Seats")).sendKeys("5");
		
		
		driver.findElement(By.name("Fuel Type")).sendKeys("Gas");
		
		driver.findElement(By.id("listprice")).sendKeys("5000");
		
		driver.findElement(By.id("licenseplatenumber")).sendKeys("8296");
		
		driver.findElement(By.xpath("//*[@id=\"annualmileage\"]")).sendKeys("500");
		
		driver.findElement(By.id("nextenterinsurantdata")).click();
		
		//Enter insurance data
		
		driver.findElement(By.id("firstname")).sendKeys("Nayana");
		
		driver.findElement(By.name("Last Name")).sendKeys("Kenchagundi");
		
		driver.findElement(By.id("birthdate")).sendKeys(Keys.CONTROL + "A");
		driver.findElement(By.id("birthdate")).sendKeys("10/04/2001");
		
		Thread.sleep(5000);
		driver.findElement(By.xpath("//*[@id=\"insurance-form\"]/div/section[2]/div[4]/p/label[2]/span")).click();
		
		driver.findElement(By.name("Street Address")).sendKeys("Nekar Nagar");
		Thread.sleep(5000);
		
		WebElement DropDown = driver.findElement(By.id("country"));
		DropDown.click();
		Select select1 = new Select(DrpDown);
		driver.findElement(By.id("country")).sendKeys("India");
		driver.findElement(By.id("country")).click();
		
		driver.findElement(By.id("zipcode")).sendKeys("580024");
		
		driver.findElement(By.name("City")).sendKeys("Hubballi");
		Thread.sleep(5000);
		
	    WebElement ThirdDroppdown=	driver.findElement(By.id("occupation"));
	    Select select3 = new Select(ThirdDroppdown);
		select3.selectByVisibleText("Unemployed");
		
		driver.findElement(By.xpath("//*[@id=\"insurance-form\"]/div/section[2]/div[10]/p/label[2]/span")).click();
		
		driver.findElement(By.id("website")).sendKeys("linkedin.com/in/nayana-kenchagundi-75212624b");
		Thread.sleep(5000);
		
		Robot rbt = new Robot();
		
		driver.findElement(By.name("Open")).click();
		
		for(int i=1; i<=8; i++)
		{
			Thread.sleep(5000);
			rbt.keyPress(KeyEvent.VK_DOWN);
			Thread.sleep(4000);
			rbt.keyPress(KeyEvent.VK_DOWN);
			Thread.sleep(5000);rbt.keyPress(KeyEvent.VK_DOWN);
		}
		
		
		driver.findElement(By.id("nextenterproductdata")).click();
		
		//enter product data
		driver.findElement(By.id("startdate")).sendKeys("11/01/2025");
		
		WebElement DropDown4 = driver.findElement(By.id("insurancesum"));
		Select select4 = new Select(DropDown4);
		select4.selectByVisibleText("7.000.000,00");
		
		
		WebElement DropDown5 = driver.findElement(By.id("meritrating"));
		DropDown5.click();
		Thread.sleep(5000);	
		
		Select select5 = new Select(DropDown5);
		select5.selectByVisibleText("Bonus 3");
		DropDown5.click();
		
		WebElement DropDown6 = driver.findElement(By.id("damageinsurance"));
		DropDown6.click();
		
		Select select6 = new Select(DropDown6);
		select6.selectByVisibleText("Full Coverage");
		DropDown6.click();
		Thread.sleep(5000);	
		
		WebElement checkBox = driver.findElement(By.xpath("//*[@id=\"insurance-form\"]/div/section[3]/div[5]/p/label[1]/span"));
		checkBox.click();
		Thread.sleep(5000);		
		WebElement DropDown7 = driver.findElement(By.id("courtesycar"));
		DropDown7.click();
		
		Select select7 = new Select(DropDown7);
		select7.selectByVisibleText("Yes");
		
		driver.findElement(By.id("nextselectpriceoption")).click();
		Thread.sleep(5000);
		
		driver.findElement(By.xpath("//*[@id=\"priceTable\"]/tfoot/tr/th[2]/label[1]/span")).click();
		
		
		//nextpage
		
		driver.findElement(By.id("nextsendquote")).click();
		
		driver.findElement(By.name("E-Mail")).sendKeys("nayanakenchagundi@gmail.com");
		driver.findElement(By.xpath("//*[@id=\"phone\"]")).sendKeys("8296089226");
		
		driver.findElement(By.id("username")).sendKeys("Nayana");
		driver.findElement(By.id("password")).sendKeys("SaRa@4/10/2001");
		
		driver.findElement(By.name("Confirm Password")).sendKeys("SaRa@4/10/2001");
		
		driver.findElement(By.id("Comments")).sendKeys("N/A");
		
		driver.findElement(By.id("sendemail")).click();
		driver.findElement(By.cssSelector("button[tabindex=\"1\"]")).click();
		
		//Truck Insurance
		driver.findElement(By.name("Navigation Truck")).click();
		
		//Enter a Vehicle Insurance
		WebElement DropDown8 = driver.findElement(By.name("Make"));
		DropDown8.click();
		
		Select select8 = new Select(DropDown8);
		
		select8.selectByVisibleText("Audi");
		
		driver.findElement(By.name("[kW]")).sendKeys("500");
		
		driver.findElement(By.xpath("//input[@id='dateofmanufacture']")).sendKeys(Keys.CONTROL+"A");
		driver.findElement(By.xpath("/html[1]/body[1]/div[1]/div[1]/div[1]/div[1]/div[1]/form[1]/div[1]/section[1]/div[3]/input[1]")).sendKeys("1/05/2025");
		
		WebElement DropDown9 =driver.findElement(By.id("numberofseats"));
		DropDown9.click();
		
		Select select9 = new Select(DropDown9);
		select9.selectByVisibleText("6");
		
		WebElement DropDown10 = driver.findElement(By.id("fuel"));
		DropDown10.click();
		Select select10 = new Select(DropDown10);
		select10.selectByVisibleText("Petrol");
		
		driver.findElement(By.id("payload")).sendKeys("850");
		
		driver.findElement(By.name("Total Weight")).sendKeys("900");
		
		driver.findElement(By.id("listprice")).sendKeys("900");
		
		driver.findElement(By.id("licenseplatenumber")).sendKeys("899");
		driver.findElement(By.id("annualmileage")).sendKeys("789");
		
		driver.findElement(By.id("nextenterinsurantdata")).click();
		
		//Enter Insurant Data
		driver.findElement(By.name("First Name")).sendKeys("Nayana");
		driver.findElement(By.id("lastname")).sendKeys("Kenchagundi");
		
		driver.findElement(By.id("birthdate")).sendKeys(Keys.CONTROL+"A");
		driver.findElement(By.id("birthdate")).sendKeys("10/04/2001");
		
		driver.findElement(By.xpath("//*[@id=\"insurance-form\"]/div/section[2]/div[4]/p/label[1]/span")).click();
		
		driver.findElement(By.id("streetaddress")).sendKeys("GaneshNagar");
		WebElement DropDown11 = driver.findElement(By.name("Country"));
		DropDown11.click();
		
		Select select11 = new Select(DropDown11);
		select11.selectByIndex(6);
		
		driver.findElement(By.id("zipcode")).sendKeys("580024");
		driver.findElement(By.name("City")).sendKeys("Vizag");
		
    	WebElement	DropDown12=driver.findElement(By.id("occupation"));
    	DropDown12.click();
    	
    	Select select12 = new Select(DropDown12);
    	select12.selectByValue("Employee");
    	
    	driver.findElement(By.xpath("//*[@id=\"insurance-form\"]/div/section[2]/div[10]/p/label[4]/span")).click();
    	
    	driver.findElement(By.id("website")).sendKeys("https://www.w3schools.com/");
    	
    	driver.findElement(By.id("nextenterproductdata")).click();
    	
    	//Enter Product Data
    	driver.findElement(By.id("startdate")).sendKeys(Keys.CONTROL+"A");
    	driver.findElement(By.id("startdate")).sendKeys("04/15/2026");
    	
    	WebElement DropDown13 =driver.findElement(By.id("insurancesum"));
		DropDown13.click();
		
		Select select13 = new Select(DropDown13);
		select13.selectByValue("30000000");
		
		WebElement DropDown14 = driver.findElement(By.id("damageinsurance"));
		DropDown14.click();
		
		Select select14 = new Select(DropDown14);
		select14.selectByIndex(3);
		
		driver.findElement(By.xpath("//*[@id=\"insurance-form\"]/div/section[3]/div[4]/p/label[2]/span")).click();
		
		driver.findElement(By.id("nextselectpriceoption")).click();
		
		driver.findElement(By.xpath("//*[@id=\"priceTable\"]/tfoot/tr/th[2]/label[2]/span")).click();
		
		driver.findElement(By.id("nextsendquote")).click();
		
		driver.findElement(By.name("E-Mail")).sendKeys("Jeeva123@gmail.com");
		driver.findElement(By.name("Phone")).sendKeys("7854952255");
		driver.findElement(By.name("Username")).sendKeys("Jeeva");
		driver.findElement(By.name("Password")).sendKeys("Jeeva@123");
		driver.findElement(By.id("confirmpassword")).sendKeys("Jeeva@123");
		driver.findElement(By.id("Comments")).sendKeys("N/A");
		driver.findElement(By.name("Send E-Mail")).click();
		
		driver.findElement(By.xpath("/html/body/div[4]/div[7]/div/button")).click();
		
		driver.findElement(By.xpath("//*[@id=\"newmotorcycleinsurance\"]/span/i")).click();
		
		//Motor Cycle insurance
		driver.findElement(By.name("Navigation Motorcycle")).click();
		WebElement DropDown15=driver.findElement(By.id("make"));
		DropDown15.click();
		Select select15 = new Select(DropDown15);
		select15.selectByValue("Suzuki");
		
		WebElement DropDown16=driver.findElement(By.name("Model"));
		DropDown16.click();
		Select select16 = new Select(DropDown16);
		select16.selectByVisibleText("Scooter");
		
		driver.findElement(By.id("cylindercapacity")).sendKeys("500");
		driver.findElement(By.id("engineperformance")).sendKeys("829");
		driver.findElement(By.id("dateofmanufacture")).sendKeys(Keys.CONTROL+"A");
		driver.findElement(By.id("dateofmanufacture")).sendKeys("5/08/2023");
		
		WebElement DropDown17 = driver.findElement(By.id("numberofseatsmotorcycle"));
		DropDown17.click();
		
		Select select17 = new Select (DropDown17);
		select17.selectByVisibleText("3");
		
		driver.findElement(By.id("listprice")).sendKeys("800");
		driver.findElement(By.id("annualmileage")).sendKeys("456");
		driver.findElement(By.name("Next (Enter Insurant Data)")).click();
		
		driver.findElement(By.id("firstname")).sendKeys("Jaakir");
		driver.findElement(By.id("lastname")).sendKeys("Hussen");
		driver.findElement(By.id("birthdate")).sendKeys(Keys.CONTROL+"A");
		driver.findElement(By.id("birthdate")).sendKeys("8/10/2005");
		
		driver.findElement(By.xpath("//*[@id=\"insurance-form\"]/div/section[2]/div[4]/p/label[1]/span")).click();
		
		driver.findElement(By.id("streetaddress")).sendKeys("Hanamthnagar");
		
		WebElement DropDown18 = driver.findElement(By.id("country"));
		DropDown18.click();
		
		Select select18 = new Select(DropDown18);
		select18.selectByVisibleText("American Samoa");
		
		driver.findElement(By.name("Zip Code")).sendKeys("580024");
		
		driver.findElement(By.name("City")).sendKeys("Vijayawada");
		
		WebElement DropDown19 = driver.findElement(By.name("Occupation"));
		DropDown19.click();
		
		Select select19 = new Select(DropDown19);
		select19.selectByVisibleText("Selfemployed");
		
		driver.findElement(By.xpath("//*[@id=\"insurance-form\"]/div/section[2]/div[10]/p/label[1]/span")).click();
		
		driver.findElement(By.id("website")).sendKeys("https://career.infosys.com/");
		
		driver.findElement(By.name("Next (Enter Product Data)")).click();
		
		
		driver.findElement(By.id("startdate")).sendKeys(Keys.CONTROL+"A");
		driver.findElement(By.id("startdate")).sendKeys("4/20/2026");
		
		WebElement DropDown20 = driver.findElement(By.name("Insurance Sum"));
		DropDown20.click();
		
		Select select20 = new Select(DropDown20);
		select20.selectByVisibleText("7.000.000,00");
		
		WebElement DropDown21 = driver.findElement(By.id("damageinsurance"));
		DropDown21.click();
		
		Select select21 = new Select(DropDown21);
		select21.selectByValue("No Coverage");
		
		driver.findElement(By.xpath("//*[@id=\"insurance-form\"]/div/section[3]/div[4]/p/label[1]/span")).click();
		
		driver.findElement(By.xpath("//*[@id=\"nextselectpriceoption\"]")).click();
		
		driver.findElement(By.xpath("//*[@id=\"priceTable\"]/tfoot/tr/th[2]/label[3]/span")).click();
		
		driver.findElement(By.id("nextsendquote")).click();
		
		driver.findElement(By.name("E-Mail")).sendKeys("Tallurinayan@gmail.com");
		
		driver.findElement(By.id("phone")).sendKeys("8296595855");
		driver.findElement(By.id("username")).sendKeys("Kiran");
		
		driver.findElement(By.name("Password")).sendKeys("Tallu@123");
		driver.findElement(By.id("confirmpassword")).sendKeys("Tallu@123");
		
		driver.findElement(By.id("Comments")).sendKeys("N/A");
		driver.findElement(By.id("sendemail")).click();
		
		driver.findElement(By.xpath("/html/body/div[4]/div[7]/div/button")).click();
		
		driver.findElement(By.xpath("//*[@id=\"newcamperinsurance\"]/span/i")).click();
		
		driver.findElement(By.name("Navigation Camper")).click();
		
		//Enter a vehicle data
		
		WebElement DropDown22=driver.findElement(By.name("Make"));
		DropDown22.click();
		
		Select select22 = new Select(DropDown22);
		select22.selectByVisibleText("Mazda");
		
		driver.findElement(By.id("engineperformance")).sendKeys("855");
		

		WebElement DropDown23=driver.findElement(By.id("dateofmanufacture"));
		DropDown23.click();
		
		driver.findElement(By.id("dateofmanufacture")).sendKeys(Keys.CONTROL+"A");
		driver.findElement(By.id("dateofmanufacture")).sendKeys("7/10/2016");
		
		WebElement DropDown24 =driver.findElement(By.id("numberofseats"));
		DropDown24.click();
		
		Select select24 = new Select(DropDown24);
		select24.selectByIndex(6);
		
		driver.findElement(By.xpath("//*[@id=\"insurance-form\"]/div/section[1]/div[5]/p/label[1]/span")).click();
		WebElement DropDown25 = driver.findElement(By.id("fuel"));
		DropDown25.click();
		
		Select select25 = new Select(DropDown25);
		select25.selectByValue("Diesel");
		
		driver.findElement(By.id("payload")).sendKeys("800");
		driver.findElement(By.name("Total Weight")).sendKeys("800");
		driver.findElement(By.id("listprice")).sendKeys("800");
		driver.findElement(By.id("licenseplatenumber")).sendKeys("800");
		driver.findElement(By.name("Annual Mileage")).sendKeys("800");
		driver.findElement(By.id("nextenterinsurantdata")).click();
		
		driver.findElement(By.id("firstname")).sendKeys("Nandu");
		driver.findElement(By.name("Last Name")).sendKeys("Kenchagundi");
		
		driver.findElement(By.id("birthdate")).sendKeys(Keys.CONTROL+"A");
		driver.findElement(By.id("birthdate")).sendKeys("5/20/2001");
		
		driver.findElement(By.xpath("//*[@id=\"insurance-form\"]/div/section[2]/div[4]/p/label[1]/span")).click();
		driver.findElement(By.name("Street Address")).sendKeys("Nekar NAgar");
		
		WebElement DropDown28=driver.findElement(By.name("Country"));
		DropDown28.click();
		
		Select select28 = new Select(DropDown28);
		select28.selectByVisibleText("Andorra");
		
		driver.findElement(By.name("Zip Code")).sendKeys("589265");
		driver.findElement(By.name("City")).sendKeys("Vijayanagar");
		
		
 		
		WebElement DropDown29 = driver.findElement(By.id("occupation"));
		DropDown29.click();
		
		Select select29 = new Select(DropDown29);
		select29.selectByValue("Public Official");
		
		driver.findElement(By.xpath("//*[@id=\"insurance-form\"]/div/section[2]/div[10]/p/label[5]/span")).click();
		driver.findElement(By.id("website")).sendKeys("https://github.com/Nayana8296");
		
		driver.findElement(By.name("Next (Enter Product Data)")).click();
		
		driver.findElement(By.id("startdate")).sendKeys(Keys.CONTROL+"A");
		driver.findElement(By.id("startdate")).sendKeys("05/09/2027");
		
		WebElement DropDown30 = driver.findElement(By.name("Insurance Sum"));
		DropDown30.click();
		
		Select select30 = new Select(DropDown30);
		select30.selectByValue("15000000");
		
		WebElement DropDown31=driver.findElement(By.id("damageinsurance"));
		DropDown31.click();
		
		Select select31 = new Select(DropDown31);
		select31.selectByVisibleText("Full Coverage");
		
		driver.findElement(By.xpath("//*[@id=\"insurance-form\"]/div/section[3]/div[4]/p/label[2]/span")).click();
		
		driver.findElement(By.id("nextselectpriceoption")).click();
		
		driver.findElement(By.xpath("//*[@id=\"priceTable\"]/tfoot/tr/th[2]/label[4]/span")).click();
		
		driver.findElement(By.id("nextsendquote")).click();
		
		driver.findElement(By.cssSelector("input[type=email]")).sendKeys("nayana123@gmail.com");
		driver.findElement(By.id("phone")).sendKeys("854825556");
		driver.findElement(By.id("username")).sendKeys("hdhs");
		driver.findElement(By.name("Password")).sendKeys("Nayana123");
		driver.findElement(By.name("Confirm Password")).sendKeys("Nayana123");
		driver.findElement(By.id("Comments")).sendKeys("nnnxnzc");
		
		driver.findElement(By.name("Send E-Mail")).click();
		
		driver.findElement(By.cssSelector("button[class=confirm]")).click();
		
		
		
		
		
		
		
		
		
		
		
		
		
		
	
		
	
		
		
		

	}

}
