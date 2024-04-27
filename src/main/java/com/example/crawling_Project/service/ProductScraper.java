package com.example.crawling_Project.service;

import com.example.crawling_Project.exceptions.ExternalIdNotFoundException;
import com.example.crawling_Project.exceptions.PriceNotFoundException;
import com.example.crawling_Project.model.Product;
import com.example.crawling_Project.model.RetailShop;
import com.example.crawling_Project.repository.ProductRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Component
public class ProductScraper {
    private static final Logger logger = LoggerFactory.getLogger(ProductScraper.class);

    private final ProductService productService;
    private final ProductRepository productRepository;
    private final WebDriver driver;

    @Autowired
    public ProductScraper(ProductService productService, ProductRepository productRepository) {
        this.productService = productService;
        this.productRepository = productRepository;
        this.driver = setupWebDriver();
    }

    private WebDriver setupWebDriver() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        return new ChromeDriver(options);
    }

    private void navigateToMainPage(String targetURL) {
        logger.info("Navigating to {}", targetURL);
        this.driver.get(targetURL);
    }

    private List<WebElement> extractProducts() {
        logger.info("Waiting for product list to become visible..");
        By selector = By.cssSelector(".min-h-screen > .grid > .flex.flex-col.gap-2.relative.px-3");
        new WebDriverWait(this.driver, Duration.ofSeconds(3))
            .until(ExpectedConditions.visibilityOfElementLocated(selector));
        return this.driver.findElements(selector);
    }

    public void scrapeAndSaveProducts(String seedUrl) {
        logger.info("Starting web scraping operation");
        if (seedUrl == null || seedUrl.isEmpty()) {
            throw new InvalidArgumentException("seedUrl is empty");
        }
        try {
            navigateToMainPage(seedUrl);
            List<WebElement> products = extractProducts();
            processProducts(products);
        } catch (Exception e) {
            logger.error("Error during web scraping operation", e);
        } finally {
            logger.info("Quitting web driver..");
            driver.quit(); // Ensure the driver is closed after the scraping is done
        }
        logger.info("Web scraping operation completed");
    }

    private String extractProductId(WebElement linkElement) {
        String targetURL = linkElement.getAttribute("href");
        Matcher matcher = Pattern.compile("/p/(.*)/").matcher(targetURL);
        return matcher.find() ?  matcher.group(1) : null;
    }

    private Double extractRating(WebElement ratingElement) {
        // number with at lease 1 digit optionally followed by a dot and a number with at least one digit
        // e.g. 5 or 4.3 or 4.21 but not 5.
        Pattern pattern = Pattern.compile("\\d+(\\.\\d+)?");
        Matcher matcher = pattern.matcher(ratingElement.getAttribute("title"));
        return matcher.find() ? Double.parseDouble(matcher.group(0)) : null;
    }

    private Product extractProductDetails(WebElement productElement) {
        Product product = new Product();

        WebElement titleElement = productElement.findElement(By.cssSelector("#title"));
        product.setTitle(titleElement.getText());

        WebElement descriptionElement = productElement
                .findElement(By.cssSelector("div.max-h-\\[47px\\].sm\\:max-h-\\[44px\\].text-secondary.text-xs.sm\\:text-sm.tracking-normal.leading-4.overflow-hidden.mt-4.mb-3.line-clamp-2"));
        product.setDescription(descriptionElement.getText());

        try {
            WebElement ratingElement = productElement.findElement(By.cssSelector("span[title*='out of 5']"));
            product.setRating(extractRating(ratingElement));
        } catch (Exception ignored) {}

        WebElement linkElement = productElement.findElement(By.cssSelector("a[href^='/p/SF-']"));
        product.setExternalId(extractProductId(linkElement));
        product.setLinkTarget(linkElement.getAttribute("href"));

        Date now = new Date();
        product.setCreatedAt(now);
        product.setUpdatedAt(now);

        return product;
    }


    private RetailShop extractRetailShopDetails(WebElement retailShopElement) throws PriceNotFoundException, ExternalIdNotFoundException {
        RetailShop retailShop = new RetailShop();

        WebElement priceElement = retailShopElement.findElement(By.cssSelector("#price > div.text-2xl"));
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(priceElement.getText());
        if (!matcher.find()) throw new PriceNotFoundException();

        String integerPart = matcher.group(0);
        priceElement = retailShopElement.findElement(By.cssSelector(".text-xs.sm\\:\\text-sm.ml-1.mt-\\[3px\\]"));
        matcher = pattern.matcher(priceElement.getText());
        String decimalPart = matcher.find() ? matcher.group(0) : "0";

        Double price = Double.parseDouble(String.format("%s.%s", integerPart, decimalPart));
        retailShop.setPrice(price);

        WebElement merchantLinkElement = retailShopElement.findElement(By.cssSelector("a[href^='/merchants/']"));
        String merchantLinkTarget = merchantLinkElement.getAttribute("href");
        pattern = Pattern.compile("/merchants/(.*?)/.*");
        matcher = pattern.matcher(merchantLinkTarget);
        if (!matcher.find()) throw new ExternalIdNotFoundException();
        retailShop.setExternalId(matcher.group(1));

        WebElement name = retailShopElement.findElement(By.cssSelector(".text-\\[14px\\].sm\\:text-\\[18px\\].text-primary.font-bold.mb-\\[5px\\].mr-\\[10px\\]"));
        retailShop.setName(name.getText());

        retailShop.setRating_shop(getRetailShopRating(retailShopElement));

        Date now = new Date();
        retailShop.setCreatedAt(now);
        retailShop.setUpdatedAt(now);

        return retailShop;
    }


    private List<WebElement> extractRetailShopList() {
        logger.info("Waiting for shop list to become visible..");
        By selector = By.cssSelector(".scroll-mt-\\[225px\\] .max-w-container.m-auto.p-4 > div");
        try {
            new WebDriverWait(this.driver, Duration.ofSeconds(3))
                    .until(ExpectedConditions.visibilityOfElementLocated(selector));
            return this.driver.findElements(selector);
        } catch (TimeoutException te) {
            logger.error("Could not get retail shop list");
            return Collections.emptyList();
        }
    }


    private void processProductDetails(Map<Long, String> productDetails) {
        for (Map.Entry<Long, String> entry : productDetails.entrySet()) {
            Long productId = entry.getKey();
            String linkTarget = entry.getValue();
            driver.get(linkTarget);

            List<WebElement> shopList = extractRetailShopList();
            for (WebElement shopElement : shopList) {
                RetailShop shop;
                try {
                    shop = extractRetailShopDetails(shopElement);
                    productService.addRetailShopToProduct(productId, shop);
                } catch (PriceNotFoundException ignored) {
                    logger.warn("could not find price for {} in {}", productId, linkTarget);
                } catch (ExternalIdNotFoundException ignored) {
                    logger.warn("could not find merchant id for {} in {}", productId, linkTarget);
                } catch (DataIntegrityViolationException e) {
                    Throwable cause = e.getCause();
                    if (!(cause instanceof ConstraintViolationException) )
                        throw e;
                    ConstraintViolationException cve = (ConstraintViolationException) cause;
                    if (!cve.getConstraintName().equals("retail_shop.retail_shop.product_id_external_id_unique"))
                        throw e;
                    logger.info("Retail shop already exists");
                }
            }
            driver.navigate().back();
        }
    }

    private void processProducts(List<WebElement> products) {
        Map<Long, String> productDetails = new HashMap<>();
        for (WebElement productElement : products) {
            Product product = extractProductDetails(productElement);
            if (product.getExternalId() == null) {
                logger.warn("no external id found for {}", product.getTitle());
                continue;
            }

            try {
                Product updatedProduct = productService.saveProduct(product);
                productDetails.put(updatedProduct.getId(),  product.getLinkTarget());
            } catch (DataIntegrityViolationException e) {
                Throwable cause = e.getCause();
                if (!(cause instanceof ConstraintViolationException) )
                    throw e;
                ConstraintViolationException cve = (ConstraintViolationException) cause;
                if (!cve.getConstraintName().equals("products.external_id"))
                    throw e;
                Product existingProduct = productRepository.findByExternalId(product.getExternalId());
                // re-scrap existing product for additional (new) retail shops
                logger.info("re-scraping {} for product with external_id = {}",  product.getLinkTarget(), product.getExternalId());
                productDetails.put(existingProduct.getId(), product.getLinkTarget());
            }
        }
        processProductDetails(productDetails);
    }

    private Double getRetailShopRating(WebElement webElement) {
        try {
            WebElement rating = webElement.findElement(By.cssSelector("p"));
            Pattern pattern = Pattern.compile("\\d+");
            Matcher matcher = pattern.matcher(rating.getText());
            return matcher.find() ? Double.parseDouble(matcher.group()) : null;
        } catch (Exception e) {
            logger.warn("Could not get retail shop rating: " + e.getMessage());
            return null;
        }
    }
}