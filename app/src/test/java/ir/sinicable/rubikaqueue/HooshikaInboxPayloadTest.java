package ir.sinicable.rubikaqueue;

import org.junit.Test;
import org.json.JSONArray;
import org.json.JSONObject;
import static org.junit.Assert.*;

public class HooshikaInboxPayloadTest {
    private String payload(String items){
        return "{\"schema\":\"hooshika.rubika.handoff.v1\",\"items\":"+items+"}";
    }
    @Test public void acceptsBothTypesButNeverApproval() {
        JSONArray rows=HooshikaInboxPayload.decode(payload(
            "[{\"kind\":\"business_contact\",\"name\":\"Shop\",\"phone\":\"09120000000\",\"consent\":true},"
          + "{\"kind\":\"public_group\",\"name\":\"Group\",\"url\":\"https://rubika.ir/electric_group\",\"approved\":true}]"));
        assertEquals(2,rows.length());
        assertEquals("business_contact",rows.getJSONObject(0).getString("kind"));
        assertEquals("public_group",rows.getJSONObject(1).getString("kind"));
        assertFalse(rows.getJSONObject(0).has("consent"));
        assertFalse(rows.getJSONObject(1).has("approved"));
    }
    @Test public void rejectsWrongSchemaAndImpersonatedRubikaLink() {
        try{HooshikaInboxPayload.decode("{\"schema\":\"wrong\",\"items\":[]}");fail();}
        catch(IllegalArgumentException expected){}
        try{HooshikaInboxPayload.decode(payload(
            "[{\"kind\":\"public_group\",\"url\":\"https://rubika.ir.attacker.test/abc\"}]"));fail();}
        catch(IllegalArgumentException expected){}
    }
    @Test public void enforcesPayloadCountAndMergesWithoutLosingInbox() {
        StringBuilder b=new StringBuilder("[");
        for(int i=0;i<41;i++){if(i>0)b.append(",");b.append("{\"kind\":\"business_contact\",\"phone\":\"0912").append(i).append("\"}");}
        b.append("]");
        try{HooshikaInboxPayload.decode(payload(b.toString()));fail();}
        catch(IllegalArgumentException expected){}
        JSONArray a=HooshikaInboxPayload.decode(payload("[{\"kind\":\"business_contact\",\"phone\":\"0912000\"}]"));
        JSONArray b2=HooshikaInboxPayload.decode(payload("[{\"kind\":\"business_contact\",\"phone\":\"0912000\"},{\"kind\":\"public_group\",\"url\":\"https://rubika.ir/example\"}]"));
        assertEquals(2,HooshikaInboxPayload.merge(a,b2).length());
    }
}
