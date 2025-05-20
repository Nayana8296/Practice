package com.SwagLabs.TestCases;

import org.testng.annotations.Test;

public class LoginPageTest extends BaseTest{
  @Test(priority=1)
  public void VerifyApplicationUrl() {

	  lp.verifyURL();
  }

 @Test(priority=2)

 public void VerifyApplicationTitle()
 {

	 lp.VerifyTitle();
 }
  @Test(priority=3)
  public void VerifyLogin() {
	  {
		  lp.VerifyApplicationLogin("standard_user","secret_sauce");

  }



  }
}
