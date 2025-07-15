package de.bund.zrb.tools;

import com.microsoft.playwright.Page;
import de.bund.zrb.service.UserRegistry;

public interface LoginStrategy {
    void performLogin(Page page, UserRegistry.User user);
}
