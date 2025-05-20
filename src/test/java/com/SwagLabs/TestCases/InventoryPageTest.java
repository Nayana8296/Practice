package com.SwagLabs.TestCases;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class InventoryPageTest extends BaseTest
{
	
	@BeforeClass
	public void pageSetUp () {
		lp.VerifyApplicationLogin("standard_user","secret_sauce");
		
	}
	@Test(priority=1)
  public void verifyTotalProduct() {
	  ip.CountProduct();
	  
  }
  @Test(priority=2)
  public void verifyProductDetails() {
	  ip.productList();
	  
  }
  @Test(priority=3)
  public void verifyAddProductToCart() {
	  ip.AddProductToCart("Sauce Labs Backpack");
  }
  @Test(priority=4)
  public void verifyCartPageLaunch() {
	  ip.LaunchCartPage();
	  
  }
}
