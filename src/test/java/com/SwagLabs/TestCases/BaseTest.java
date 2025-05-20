package com.SwagLabs.TestCases;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;

import com.SwagLabs.Pages.InventoryPage;
import com.SwagLabs.Pages.LoginPage;

public class BaseTest {

	public WebDriver driver;
	public LoginPage lp;
    public InventoryPage ip;

	@BeforeTest
	public void SetUp() {
		driver = new ChromeDriver();
		driver.get("https://www.saucedemo.com/");
		lp=new LoginPage(driver);
		ip=new InventoryPage(driver);

	}
/*	@AfterTest
	public void teardown() {
		driver.close();*/
	}




