package de.bund.zrb.compiler;

public class DynamicClassBuilder {

    /**
     * Wraps the provided code in a class definition that implements Runnable.
     *
     * @param className  The name of the class to create.
     * @param methodBody The body of the run method.
     * @return A string containing the full class definition.
     */
    public static String wrapAsRunnable(String className, String methodBody) {
        return ""
                + "public class " + className + " implements java.lang.Runnable {\n"
                + "    public void run() {\n"
                + methodBody + "\n"
                + "    }\n"
                + "}";
    }

    /**
     * Wraps the provided code in a class definition that implements Callable.
     *
     * @param className The name of the class to create.
     * @param code      The body of the call method.
     * @return A string containing the full class definition.
     */
    public static String wrapAsCallable(String className, String code) {
        return ""
                + "import java.util.concurrent.Callable;\n"
                + "public class " + className + " implements Callable<String> {\n"
                + "    @Override\n"
                + "    public String call() throws Exception {\n"
                + code + "\n"
                + "    }\n"
                + "}";
    }
}

