package com.SwagLabs.Pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import graphql.Assert;

public class LoginPage {

	//Encapsulation = private Data+ public function

	private WebDriver  driver;

   //initialize driver;
	public LoginPage (WebDriver driver)
	{//from database

	   this.driver=driver;

}
	//Locators
	private By usern = By.id("user-name");
	private By Paswrd = By.id("password");
	private By Login = By.id("login-button");

	//Actions
	public void VerifyTitle () {
		String Title = driver.getTitle();
		Assert.assertTrue(Title.contains("Swag Labs"), "Test-Fail" ,"Title is not matched");
		System.out.println("Test Pass: Title is matched");
	}
	public void verifyURL ()
	{
		String URL = driver.getCurrentUrl();
		Assert.assertTrue(URL.contains("demo"),"Test case Fail:url is not matched");
		System.out.println("Testcase Pass: URl is matched");
	}
	public void VerifyLogin() {
		driver.findElement(usern).sendKeys("un");
		driver.findElement(Paswrd).sendKeys("pn");
		driver.findElement(Login).click();
		Assert.assertTrue(driver.getCurrentUrl().contains("inventory"),"Login Fail");
		System.out.println("Login Completed");


	}
	public void VerifyApplicationLogin(String string, String string2) {
		// TODO Auto-generated method stub

	}


}

