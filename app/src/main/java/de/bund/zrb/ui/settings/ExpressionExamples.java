package de.bund.zrb.ui.settings;

import de.bund.zrb.runtime.ExpressionRegistry;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Enthält vordefinierte Beispielausdrücke für den ExpressionEditor.
 */
public final class ExpressionExamples {

    private ExpressionExamples() {
        // Verhindere Instanziierung
    }

    /**
     * Loads the examples if they are not persisted to file.
     *
     * @param registry registry to register examples into
     */
    public static void ensureExamplesRegistered(ExpressionRegistry registry) {
        for (Map.Entry<String, String> e : getExamples().entrySet()) {
            if (!registry.getCode(e.getKey()).isPresent()) {
                registry.register(e.getKey(), e.getValue());
            }
        }
    }

    public static Map<String, String> getExamples() {
        Map<String, String> examples = new LinkedHashMap<String, String>();

        // -----------------------
        // DATE (übernommen)
        // -----------------------
        examples.put("Date",
                "import java.time.LocalDate;\n" +
                        "import java.time.format.DateTimeFormatter;\n" +
                        "import java.util.List;\n" +
                        "import java.util.function.Function;\n" +
                        "public class Expr_Date implements Function<List<String>, String> {\n" +
                        "    public String apply(List<String> args) {\n" +
                        "        if (args.isEmpty()) return \"\";\n" +
                        "        String pattern = args.get(0);\n" +
                        "        try {\n" +
                        "            return LocalDate.now().format(DateTimeFormatter.ofPattern(pattern));\n" +
                        "        } catch (Exception e) {\n" +
                        "            return \"Ungültiges Format: \" + pattern;\n" +
                        "        }\n" +
                        "    }\n" +
                        "}\n");

        // -----------------------
        // OTP (6-stellig, TOTP-artig)
        // -----------------------
        examples.put("Otp",
                "import java.util.List;\n" +
                        "import java.util.function.Function;\n" +
                        "import javax.crypto.Mac;\n" +
                        "import javax.crypto.spec.SecretKeySpec;\n" +
                        "public class Expr_Otp implements Function<List<String>, String> {\n" +
                        "    public String apply(List<String> args) {\n" +
                        "        String secret = args.isEmpty() ? \"secret\" : String.valueOf(args.get(0));\n" +
                        "        long timestep = 30L; // 30s Timestep\n" +
                        "        long counter = (System.currentTimeMillis() / 1000L) / timestep;\n" +
                        "        try {\n" +
                        "            byte[] key = secret.getBytes(\"UTF-8\");\n" +
                        "            byte[] data = new byte[8];\n" +
                        "            long c = counter;\n" +
                        "            for (int i = 7; i >= 0; i--) { data[i] = (byte)(c & 0xFF); c >>= 8; }\n" +
                        "            Mac mac = Mac.getInstance(\"HmacSHA1\");\n" +
                        "            mac.init(new SecretKeySpec(key, \"HmacSHA1\"));\n" +
                        "            byte[] h = mac.doFinal(data);\n" +
                        "            int o = h[h.length - 1] & 0x0F;\n" +
                        "            int bin = ((h[o] & 0x7F) << 24) | ((h[o+1] & 0xFF) << 16) | ((h[o+2] & 0xFF) << 8) | (h[o+3] & 0xFF);\n" +
                        "            int otp = bin % 1000000;\n" +
                        "            return String.format(\"%06d\", otp);\n" +
                        "        } catch (Exception e) {\n" +
                        "            return \"OTP-Error: \" + e.getMessage();\n" +
                        "        }\n" +
                        "    }\n" +
                        "}\n");

        // -----------------------
        // GROWL (seiteneffekt: Swing-Dialog)
        // -----------------------
        examples.put("Growl",
                "import javax.swing.*;\n" +
                        "import java.util.List;\n" +
                        "import java.util.function.Function;\n" +
                        "public class Expr_Growl implements Function<List<String>, String> {\n" +
                        "    public String apply(List<String> args) {\n" +
                        "        final String msg = args.isEmpty() ? \"OK\" : String.valueOf(args.get(0));\n" +
                        "        SwingUtilities.invokeLater(new Runnable() {\n" +
                        "            public void run() {\n" +
                        "                try { JOptionPane.showMessageDialog(null, msg, \"Hinweis\", JOptionPane.INFORMATION_MESSAGE); }\n" +
                        "                catch (Throwable t) { /* ignore */ }\n" +
                        "            }\n" +
                        "        });\n" +
                        "        return msg;\n" +
                        "    }\n" +
                        "}\n");

        // -----------------------
        // NAVIGATION (seiteneffekt: Standard-Browser öffnen)
        // -----------------------
        examples.put("Navigation",
                "import java.awt.Desktop;\n" +
                        "import java.net.URI;\n" +
                        "import java.util.List;\n" +
                        "import java.util.function.Function;\n" +
                        "public class Expr_Navigation implements Function<List<String>, String> {\n" +
                        "    public String apply(List<String> args) {\n" +
                        "        if (args.isEmpty()) return \"\";\n" +
                        "        String url = String.valueOf(args.get(0));\n" +
                        "        try {\n" +
                        "            if (Desktop.isDesktopSupported()) {\n" +
                        "                Desktop.getDesktop().browse(new URI(url));\n" +
                        "                return \"Navigated: \" + url;\n" +
                        "            }\n" +
                        "            return \"Desktop unsupported\";\n" +
                        "        } catch (Exception e) {\n" +
                        "            return \"Navigation error: \" + e.getMessage();\n" +
                        "        }\n" +
                        "    }\n" +
                        "}\n");

        return examples;
    }
}
