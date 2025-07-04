package de.bund.zrb.mapping;

import com.google.gson.Gson;
import de.bund.zrb.command.request.WDBrowsingContextRequest;
import de.bund.zrb.support.mapping.GsonMapperFactory;

public class GsonTest {
    public static void main(String[] args) {
        Gson gson = GsonMapperFactory.getGson();

        WDBrowsingContextRequest.Navigate command = new WDBrowsingContextRequest.Navigate("https://example.com", "my-context");
        String json = gson.toJson(command);
        System.out.println("Serialized Navigate Command: " + json);
    }
}
