package wd4j.impl.webdriver.mapping;

import com.google.gson.Gson;
import wd4j.impl.webdriver.command.request.WDBrowsingContextRequest;

public class GsonTest {
    public static void main(String[] args) {
        Gson gson = GsonMapperFactory.getGson();

        WDBrowsingContextRequest.Navigate command = new WDBrowsingContextRequest.Navigate("https://example.com", "my-context");
        String json = gson.toJson(command);
        System.out.println("Serialized Navigate Command: " + json);
    }
}
