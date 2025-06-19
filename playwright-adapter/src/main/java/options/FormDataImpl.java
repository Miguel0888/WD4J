package options;

import com.microsoft.options.FilePayload;
import com.microsoft.options.FormData;

import java.nio.file.Path;

/**
 * NOT IMPLEMENTED YET
 */
public class FormDataImpl implements FormData {
    /**
     * Appends a new value onto an existing key inside a FormData object, or adds the key if it does not already exist. File
     * values can be passed either as {@code Path} or as {@code FilePayload}. Multiple fields with the same name can be added.
     *
     * <p> The difference between {@link com.microsoft.playwright.FormData#set FormData.set()} and {@link
     * com.microsoft.playwright.FormData#append FormData.append()} is that if the specified key already exists, {@link
     * com.microsoft.playwright.FormData#set FormData.set()} will overwrite all existing values with the new one, whereas
     * {@link com.microsoft.playwright.FormData#append FormData.append()} will append the new value onto the end of the
     * existing set of values.
     * <pre>{@code
     * import com.microsoft.playwright.options.FormData;
     * // ...
     * FormData form = FormData.create()
     *     // Only name and value are set.
     *     .append("firstName", "John")
     *     // Name and value are set, filename and Content-Type are inferred from the file path.
     *     .append("attachment", Paths.get("pic.jpg"))
     *     // Name, value, filename and Content-Type are set.
     *     .append("attachment", new FilePayload("table.csv", "text/csv", Files.readAllBytes(Paths.get("my-tble.csv"))));
     * page.request().post("http://localhost/submit", RequestOptions.create().setForm(form));
     * }</pre>
     *
     * @param name  Field name.
     * @param value Field value.
     * @since v1.44
     */
    @Override
    public FormData append(String name, String value) {
        return null;
    }

    /**
     * Appends a new value onto an existing key inside a FormData object, or adds the key if it does not already exist. File
     * values can be passed either as {@code Path} or as {@code FilePayload}. Multiple fields with the same name can be added.
     *
     * <p> The difference between {@link com.microsoft.playwright.FormData#set FormData.set()} and {@link
     * com.microsoft.playwright.FormData#append FormData.append()} is that if the specified key already exists, {@link
     * com.microsoft.playwright.FormData#set FormData.set()} will overwrite all existing values with the new one, whereas
     * {@link com.microsoft.playwright.FormData#append FormData.append()} will append the new value onto the end of the
     * existing set of values.
     * <pre>{@code
     * import com.microsoft.playwright.options.FormData;
     * // ...
     * FormData form = FormData.create()
     *     // Only name and value are set.
     *     .append("firstName", "John")
     *     // Name and value are set, filename and Content-Type are inferred from the file path.
     *     .append("attachment", Paths.get("pic.jpg"))
     *     // Name, value, filename and Content-Type are set.
     *     .append("attachment", new FilePayload("table.csv", "text/csv", Files.readAllBytes(Paths.get("my-tble.csv"))));
     * page.request().post("http://localhost/submit", RequestOptions.create().setForm(form));
     * }</pre>
     *
     * @param name  Field name.
     * @param value Field value.
     * @since v1.44
     */
    @Override
    public FormData append(String name, boolean value) {
        return null;
    }

    /**
     * Appends a new value onto an existing key inside a FormData object, or adds the key if it does not already exist. File
     * values can be passed either as {@code Path} or as {@code FilePayload}. Multiple fields with the same name can be added.
     *
     * <p> The difference between {@link com.microsoft.playwright.FormData#set FormData.set()} and {@link
     * com.microsoft.playwright.FormData#append FormData.append()} is that if the specified key already exists, {@link
     * com.microsoft.playwright.FormData#set FormData.set()} will overwrite all existing values with the new one, whereas
     * {@link com.microsoft.playwright.FormData#append FormData.append()} will append the new value onto the end of the
     * existing set of values.
     * <pre>{@code
     * import com.microsoft.playwright.options.FormData;
     * // ...
     * FormData form = FormData.create()
     *     // Only name and value are set.
     *     .append("firstName", "John")
     *     // Name and value are set, filename and Content-Type are inferred from the file path.
     *     .append("attachment", Paths.get("pic.jpg"))
     *     // Name, value, filename and Content-Type are set.
     *     .append("attachment", new FilePayload("table.csv", "text/csv", Files.readAllBytes(Paths.get("my-tble.csv"))));
     * page.request().post("http://localhost/submit", RequestOptions.create().setForm(form));
     * }</pre>
     *
     * @param name  Field name.
     * @param value Field value.
     * @since v1.44
     */
    @Override
    public FormData append(String name, int value) {
        return null;
    }

    /**
     * Appends a new value onto an existing key inside a FormData object, or adds the key if it does not already exist. File
     * values can be passed either as {@code Path} or as {@code FilePayload}. Multiple fields with the same name can be added.
     *
     * <p> The difference between {@link com.microsoft.playwright.FormData#set FormData.set()} and {@link
     * com.microsoft.playwright.FormData#append FormData.append()} is that if the specified key already exists, {@link
     * com.microsoft.playwright.FormData#set FormData.set()} will overwrite all existing values with the new one, whereas
     * {@link com.microsoft.playwright.FormData#append FormData.append()} will append the new value onto the end of the
     * existing set of values.
     * <pre>{@code
     * import com.microsoft.playwright.options.FormData;
     * // ...
     * FormData form = FormData.create()
     *     // Only name and value are set.
     *     .append("firstName", "John")
     *     // Name and value are set, filename and Content-Type are inferred from the file path.
     *     .append("attachment", Paths.get("pic.jpg"))
     *     // Name, value, filename and Content-Type are set.
     *     .append("attachment", new FilePayload("table.csv", "text/csv", Files.readAllBytes(Paths.get("my-tble.csv"))));
     * page.request().post("http://localhost/submit", RequestOptions.create().setForm(form));
     * }</pre>
     *
     * @param name  Field name.
     * @param value Field value.
     * @since v1.44
     */
    @Override
    public FormData append(String name, Path value) {
        return null;
    }

    /**
     * Appends a new value onto an existing key inside a FormData object, or adds the key if it does not already exist. File
     * values can be passed either as {@code Path} or as {@code FilePayload}. Multiple fields with the same name can be added.
     *
     * <p> The difference between {@link com.microsoft.playwright.FormData#set FormData.set()} and {@link
     * com.microsoft.playwright.FormData#append FormData.append()} is that if the specified key already exists, {@link
     * com.microsoft.playwright.FormData#set FormData.set()} will overwrite all existing values with the new one, whereas
     * {@link com.microsoft.playwright.FormData#append FormData.append()} will append the new value onto the end of the
     * existing set of values.
     * <pre>{@code
     * import com.microsoft.playwright.options.FormData;
     * // ...
     * FormData form = FormData.create()
     *     // Only name and value are set.
     *     .append("firstName", "John")
     *     // Name and value are set, filename and Content-Type are inferred from the file path.
     *     .append("attachment", Paths.get("pic.jpg"))
     *     // Name, value, filename and Content-Type are set.
     *     .append("attachment", new FilePayload("table.csv", "text/csv", Files.readAllBytes(Paths.get("my-tble.csv"))));
     * page.request().post("http://localhost/submit", RequestOptions.create().setForm(form));
     * }</pre>
     *
     * @param name  Field name.
     * @param value Field value.
     * @since v1.44
     */
    @Override
    public FormData append(String name, FilePayload value) {
        return null;
    }

    /**
     * Sets a field on the form. File values can be passed either as {@code Path} or as {@code FilePayload}.
     * <pre>{@code
     * import com.microsoft.playwright.options.FormData;
     * // ...
     * FormData form = FormData.create()
     *     // Only name and value are set.
     *     .set("firstName", "John")
     *     // Name and value are set, filename and Content-Type are inferred from the file path.
     *     .set("profilePicture1", Paths.get("john.jpg"))
     *     // Name, value, filename and Content-Type are set.
     *     .set("profilePicture2", new FilePayload("john.jpg", "image/jpeg", Files.readAllBytes(Paths.get("john.jpg"))))
     *     .set("age", 30);
     * page.request().post("http://localhost/submit", RequestOptions.create().setForm(form));
     * }</pre>
     *
     * @param name  Field name.
     * @param value Field value.
     * @since v1.18
     */
    @Override
    public FormData set(String name, String value) {
        return null;
    }

    /**
     * Sets a field on the form. File values can be passed either as {@code Path} or as {@code FilePayload}.
     * <pre>{@code
     * import com.microsoft.playwright.options.FormData;
     * // ...
     * FormData form = FormData.create()
     *     // Only name and value are set.
     *     .set("firstName", "John")
     *     // Name and value are set, filename and Content-Type are inferred from the file path.
     *     .set("profilePicture1", Paths.get("john.jpg"))
     *     // Name, value, filename and Content-Type are set.
     *     .set("profilePicture2", new FilePayload("john.jpg", "image/jpeg", Files.readAllBytes(Paths.get("john.jpg"))))
     *     .set("age", 30);
     * page.request().post("http://localhost/submit", RequestOptions.create().setForm(form));
     * }</pre>
     *
     * @param name  Field name.
     * @param value Field value.
     * @since v1.18
     */
    @Override
    public FormData set(String name, boolean value) {
        return null;
    }

    /**
     * Sets a field on the form. File values can be passed either as {@code Path} or as {@code FilePayload}.
     * <pre>{@code
     * import com.microsoft.playwright.options.FormData;
     * // ...
     * FormData form = FormData.create()
     *     // Only name and value are set.
     *     .set("firstName", "John")
     *     // Name and value are set, filename and Content-Type are inferred from the file path.
     *     .set("profilePicture1", Paths.get("john.jpg"))
     *     // Name, value, filename and Content-Type are set.
     *     .set("profilePicture2", new FilePayload("john.jpg", "image/jpeg", Files.readAllBytes(Paths.get("john.jpg"))))
     *     .set("age", 30);
     * page.request().post("http://localhost/submit", RequestOptions.create().setForm(form));
     * }</pre>
     *
     * @param name  Field name.
     * @param value Field value.
     * @since v1.18
     */
    @Override
    public FormData set(String name, int value) {
        return null;
    }

    /**
     * Sets a field on the form. File values can be passed either as {@code Path} or as {@code FilePayload}.
     * <pre>{@code
     * import com.microsoft.playwright.options.FormData;
     * // ...
     * FormData form = FormData.create()
     *     // Only name and value are set.
     *     .set("firstName", "John")
     *     // Name and value are set, filename and Content-Type are inferred from the file path.
     *     .set("profilePicture1", Paths.get("john.jpg"))
     *     // Name, value, filename and Content-Type are set.
     *     .set("profilePicture2", new FilePayload("john.jpg", "image/jpeg", Files.readAllBytes(Paths.get("john.jpg"))))
     *     .set("age", 30);
     * page.request().post("http://localhost/submit", RequestOptions.create().setForm(form));
     * }</pre>
     *
     * @param name  Field name.
     * @param value Field value.
     * @since v1.18
     */
    @Override
    public FormData set(String name, Path value) {
        return null;
    }

    /**
     * Sets a field on the form. File values can be passed either as {@code Path} or as {@code FilePayload}.
     * <pre>{@code
     * import com.microsoft.playwright.options.FormData;
     * // ...
     * FormData form = FormData.create()
     *     // Only name and value are set.
     *     .set("firstName", "John")
     *     // Name and value are set, filename and Content-Type are inferred from the file path.
     *     .set("profilePicture1", Paths.get("john.jpg"))
     *     // Name, value, filename and Content-Type are set.
     *     .set("profilePicture2", new FilePayload("john.jpg", "image/jpeg", Files.readAllBytes(Paths.get("john.jpg"))))
     *     .set("age", 30);
     * page.request().post("http://localhost/submit", RequestOptions.create().setForm(form));
     * }</pre>
     *
     * @param name  Field name.
     * @param value Field value.
     * @since v1.18
     */
    @Override
    public FormData set(String name, FilePayload value) {
        return null;
    }
}
