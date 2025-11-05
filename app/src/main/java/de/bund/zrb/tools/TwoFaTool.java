package de.bund.zrb.tools;

import de.bund.zrb.expressions.builtins.tooling.ToolExpressionFunction;
import de.bund.zrb.expressions.domain.ExpressionFunction;
import de.bund.zrb.expressions.domain.FunctionContext;
import de.bund.zrb.service.BrowserService;
import de.bund.zrb.service.TotpService;
import de.bund.zrb.service.UserRegistry;
import de.bund.zrb.ui.debug.OtpTestDialog;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class TwoFaTool extends AbstractUserTool {

    private final BrowserService browserService;
    private final TotpService totpService;

    public TwoFaTool(BrowserService browserService, TotpService totpService) {
        this.browserService = browserService;
        this.totpService = totpService;
    }

    public void showOtpDialog(Window parent) {
        UserRegistry.User user = getCurrentUserOrFail();

        if (user.getOtpSecret() == null || user.getOtpSecret().isEmpty()) {
            JOptionPane.showMessageDialog(parent, "Benutzer hat kein OTP-Secret.", "Hinweis", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        new OtpTestDialog(parent, user).setVisible(true);
    }

    public String generateOtp(String username) {
        return totpService.getOtp(username);
    }
    public String generateOtpForCurrentUser() {
        return totpService.getOtp();
    }

    public Collection<ExpressionFunction> builtinFunctions() {
        java.util.List<ExpressionFunction> list = new ArrayList<ExpressionFunction>();

        list.add(new ToolExpressionFunction(
                ToolExpressionFunction.meta(
                        "TwoFa",
                        "Submit a given 2FA code or compute a TOTP code and submit it.",
                        ToolExpressionFunction.params("username?"),
                        Arrays.asList("Optional username. If omitted, a TOTP is generated for current active user. ")
                ),
                0, 1,
                new ToolExpressionFunction.Invoker() {
                    public String invoke(List<String> args, FunctionContext ctx) throws Exception {
                        if (args.size() >= 1 && args.get(0) != null && args.get(0).length() > 0) {
                            return generateOtp(args.get(0));
                        }
                        return generateOtpForCurrentUser();
                    }
                }
        ));

        return list;
    }
}
