// @ts-check
import { test, expect } from '@playwright/test';
import { existsSync } from 'node:fs';
import path from 'node:path';
import { readSheet } from 'read-excel-file/node';

const saucedemoUrl = 'https://saucedemo.com';
const actionDelay = 2000;
const checkoutWorkbookPath = path.resolve(
  process.cwd(),
  'test-data',
  'checkout-data.xlsx',
);

async function loadCheckoutData() {
  if (!existsSync(checkoutWorkbookPath)) {
    throw new Error(
      `Checkout workbook not found: ${checkoutWorkbookPath}. ` +
      'Add an .xlsx file with First Name, Last Name, and Zip/Postal Code columns.',
    );
  }

  const rows = await readSheet(checkoutWorkbookPath);
  const headers = rows[0]?.map((value) => String(value ?? '').trim().toLowerCase());
  if (!headers) {
    throw new Error(`Checkout workbook has no header row: ${checkoutWorkbookPath}`);
  }

  const columns = {
    firstName: headers.indexOf('first name'),
    lastName: headers.indexOf('last name'),
    postalCode: headers.indexOf('zip/postal code'),
  };
  const missingColumns = Object.entries(columns)
    .filter(([, index]) => index === -1)
    .map(([name]) => name);
  if (missingColumns.length > 0) {
    throw new Error(
      `Checkout workbook is missing required columns: ${missingColumns.join(', ')}. ` +
      'Use headers: First Name, Last Name, Zip/Postal Code.',
    );
  }

  const dataRow = rows.slice(1).find((row) =>
    row.some((value) => value !== null && value !== undefined && String(value).trim() !== ''),
  );
  if (!dataRow) {
    throw new Error(`Checkout workbook has no checkout data row: ${checkoutWorkbookPath}`);
  }

  const checkoutData = {
    firstName: String(dataRow[columns.firstName] ?? '').trim(),
    lastName: String(dataRow[columns.lastName] ?? '').trim(),
    postalCode: String(dataRow[columns.postalCode] ?? '').trim(),
  };
  const emptyFields = Object.entries(checkoutData)
    .filter(([, value]) => value.length === 0)
    .map(([name]) => name);
  if (emptyFields.length > 0) {
    throw new Error(`Checkout data row has empty values for: ${emptyFields.join(', ')}`);
  }

  return checkoutData;
}

async function fillCheckoutInformation(page) {
  const checkoutData = await loadCheckoutData();

  await page.getByPlaceholder('First Name').fill(checkoutData.firstName);
  await page.getByPlaceholder('Last Name').fill(checkoutData.lastName);
  await page.getByPlaceholder('Zip/Postal Code').fill(checkoutData.postalCode);

  await expect(page.getByPlaceholder('First Name')).toHaveValue(checkoutData.firstName);
  await expect(page.getByPlaceholder('Last Name')).toHaveValue(checkoutData.lastName);
  await expect(page.getByPlaceholder('Zip/Postal Code')).toHaveValue(checkoutData.postalCode);
}

async function clickAndWait(page, locator, optionName) {
  await locator.click();
  console.log(`Clicked: ${optionName}`);
  await page.waitForTimeout(actionDelay);
}

async function continueCheckout(page) {
  await clickAndWait(page, page.getByRole('button', { name: 'Continue' }), 'Continue');
  await expect(page).toHaveURL(/checkout-step-two\.html/);
  await expect(page.getByText('Checkout: Overview', { exact: true })).toBeVisible();
}

test('Sauce Demo has the expected title', async ({ page }) => {
  await page.goto(saucedemoUrl);

  await expect(page).toHaveTitle(/Swag Labs/);
});

test('Sauce Demo adds a shirt to the cart and opens checkout', async ({ page }) => {
  await page.goto(saucedemoUrl);

  await page.getByPlaceholder('Username').fill('standard_user');
  await page.getByPlaceholder('Password').fill('secret_sauce');
  await clickAndWait(page, page.getByRole('button', { name: 'Login' }), 'Login');

  await expect(page).toHaveURL(/inventory\.html/);
  await expect(page.getByText('Products', { exact: true })).toBeVisible();

  const shirt = page.getByText('Sauce Labs Bolt T-Shirt', { exact: true });
  await shirt.scrollIntoViewIfNeeded();
  await clickAndWait(page, shirt, 'Sauce Labs Bolt T-Shirt');

  await expect(page.getByText('Sauce Labs Bolt T-Shirt', { exact: true })).toBeVisible();
  await clickAndWait(page, page.getByRole('button', { name: 'Add to cart' }), 'Add to cart');
  await clickAndWait(page, page.locator('.shopping_cart_link'), 'Shopping cart');

  await expect(page.getByText('Sauce Labs Bolt T-Shirt', { exact: true })).toBeVisible();
  await clickAndWait(page, page.getByRole('button', { name: 'Checkout' }), 'Checkout');

  await expect(page).toHaveURL(/checkout-step-one\.html/);
  await expect(page.getByPlaceholder('First Name')).toBeVisible();
  await fillCheckoutInformation(page);
  await continueCheckout(page);
});

test('Let the user select a product and add it to the cart automatically', async ({ page }) => {
  test.setTimeout(0);

  await page.goto(saucedemoUrl);
  await page.getByPlaceholder('Username').fill('standard_user');
  await page.getByPlaceholder('Password').fill('secret_sauce');
  await page.getByRole('button', { name: 'Login' }).click();

  await page.waitForURL(/\/inventory(?:-item)?\.html(?:\?.*)?$/);

  console.log('Select any product within 15 seconds. The script will add it to the cart automatically.');
  await page.waitForURL(/\/inventory-item\.html(?:\?.*)?$/, { timeout: 15_000 });
  await clickAndWait(page, page.getByRole('button', { name: 'Add to cart' }), 'Add to cart');

  await clickAndWait(page, page.locator('.shopping_cart_link'), 'Shopping cart');
  await expect(page).toHaveURL(/cart\.html/);

  await clickAndWait(page, page.getByRole('button', { name: 'Checkout' }), 'Checkout');
  await expect(page).toHaveURL(/checkout-step-one\.html/);
  await expect(page.getByPlaceholder('First Name')).toBeVisible();
  await fillCheckoutInformation(page);
  await continueCheckout(page);

  const finishButton = page.getByRole('button', { name: 'Finish' });
  await finishButton.scrollIntoViewIfNeeded();
  await clickAndWait(page, finishButton, 'Finish');
  await expect(page).toHaveURL(/checkout-complete\.html/);
  await expect(page.getByText('Thank you for your order!')).toBeVisible();

  const pdfDownloadPromise = page.waitForEvent('download');
  await clickAndWait(page, page.getByRole('button', { name: 'Generate PDF order' }), 'Generate PDF order');
  const pdfDownload = await pdfDownloadPromise;
  await pdfDownload.path();
  console.log(`Generated order PDF: ${pdfDownload.suggestedFilename()}`);

  await clickAndWait(page, page.getByRole('button', { name: 'Back Home' }), 'Back Home');
  await expect(page).toHaveURL(/\/inventory\.html$/);

  console.log('Returned to the product list. Closing the browser.');
  await page.close();
});
