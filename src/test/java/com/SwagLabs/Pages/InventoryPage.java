package com.SwagLabs.Pages;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.Assert;

public class InventoryPage {

   private WebDriver driver;
   
   
   public InventoryPage(WebDriver driver)
   {
	   this.driver = driver;
	   
   }
   //Locators
   private By productList = By.xpath("//div[@class='inventory_list']//div[@class='inventory_item_name ']");
   private By AddToCart = By.id("add-to-cart");
   private By CartBtn = By.id("shopping_cart_container");
   
    //actions
   public void CountProduct() {
	   
	 int count  = driver.findElements(productList).size();
	 Assert.assertEquals(count,6,"Test caseFail:Total product not matched");
	 System.out.println("TestCase Pass:Total product Count="+count);
	   
   }

   public void productList() {
	   List<WebElement>list=driver.findElements(productList);
	   System.out.println("*******Product List******");
	   for(WebElement i:list) {
		   System.out.println(i.getText());
		   
	   }
   }
   //product Cart
   public void AddProductToCart(String pname) {
	   
	  List<WebElement>list =  driver.findElements(productList);
	  for (WebElement i:list)
	  {
		  if(i.getText().contains(pname))
				 
				  i.click();
		          break;
		  
	  }
	//add to cart
	 driver.findElement(CartBtn).click();
	  System.out.println("Product"+ pname +"Added to cart");
   }

   public void LaunchCartPage() {
	   driver.findElement(CartBtn).click();
	   System.out.println("Cart Page Launch");
   }
   }
