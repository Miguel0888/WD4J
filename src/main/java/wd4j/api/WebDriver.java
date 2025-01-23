package wd4j.api;

public interface WebDriver {
    WebElement findElement(By locator);
    void get(String url);
    String getCurrentUrl();
    String getTitle();

    void close();
    // Weitere Methoden...
}
