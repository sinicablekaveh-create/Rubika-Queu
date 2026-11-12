package ir.sinicable.rubikaqueue;

import java.net.UnknownHostException;
import java.security.KeyPair;
import java.util.*;
import org.json.*;

public final class RubikaTransportTests {
    static int checks=0;
    static void check(boolean ok,String msg){if(!ok)throw new AssertionError(msg);checks++;}
    static JSONObject ok(JSONObject data)throws Exception {
        return RubikaClient.obj("status","OK","status_det","OK","data",data);
    }
    static JSONObject discovery()throws Exception {
        JSONObject api=new JSONObject();
        api.put("139","https://messengerg2c39.iranlms.ir");
        api.put("142","https://messengerg2c42.iranlms.ir");
        JSONArray defaults=new JSONArray().put("139").put("142");
        return RubikaClient.obj("data",RubikaClient.obj("API",api,"default_api","139","default_apis",defaults));
    }

    static final class FailoverTransport implements RubikaClient.Transport {
        int discoveryCalls,readCalls,sendCalls;
        public JSONObject exchange(String url,JSONObject body)throws Exception {
            if(body==null){discoveryCalls++;return discovery();}
            // Requests are encrypted, so use URL and call sequence to model transport behavior.
            if(url.contains("c39.")){readCalls++;throw new UnknownHostException("host39");}
            readCalls++;
            return ok(RubikaClient.obj("user",RubikaClient.obj("user_guid","u0ABCDEFGHIJK","phone","989121234567")));
        }
    }

    static final class SendOnceTransport implements RubikaClient.Transport {
        int sendAttempts;
        public JSONObject exchange(String url,JSONObject body)throws Exception {
            if(body==null)return discovery();
            sendAttempts++;
            throw new UnknownHostException("ambiguous send transport failure");
        }
    }

    static final class ResolveTransport implements RubikaClient.Transport {
        int step;
        public JSONObject exchange(String url,JSONObject body)throws Exception {
            if(body==null)return discovery();
            step++;
            if(step==1) return ok(new JSONObject()); // addAddressBook did not expose GUID
            if(step==2) {
                JSONArray contacts=new JSONArray().put(
                    RubikaClient.obj("user_guid","u0ABCDEFGHIJK","phone","989121234567"));
                return ok(RubikaClient.obj("contacts",contacts,"has_continue",false));
            }
            return ok(RubikaClient.obj("user",
                RubikaClient.obj("user_guid","u0ABCDEFGHIJK","phone","989121234567")));
        }
    }

    static RubikaClient ready(RubikaClient.Transport transport)throws Exception {
        RubikaClient c=new RubikaClient(transport);
        KeyPair keys=CryptoCore.generate();
        c.privateKey=keys.getPrivate();
        c.guid="u0SELFABCDEFGHI";
        c.phone="989120000000";
        return c;
    }

    public static void main(String[] args)throws Exception {
        FailoverTransport f=new FailoverTransport();
        RubikaClient a=ready(f);
        JSONObject me=a.call("getUserInfo",RubikaClient.obj("user_guid","u0ABCDEFGHIJK"),false);
        check("u0ABCDEFGHIJK".equals(me.getJSONObject("user").getString("user_guid")),
            "safe read should succeed on fallback host");
        check(f.discoveryCalls==1,"discovery once");
        check(f.readCalls==2,"read tried preferred then fallback host");

        SendOnceTransport s=new SendOnceTransport();
        RubikaClient b=ready(s);
        boolean failed=false;
        try{b.send("u0ABCDEFGHIJK","test",RubikaClient.freshMessageRnd());}
        catch(UnknownHostException expected){failed=true;}
        check(failed,"send transport failure surfaced");
        check(s.sendAttempts==1,"sendMessage must never auto-failover/repeat");

        ResolveTransport rt=new ResolveTransport();
        RubikaClient c=ready(rt);
        String resolved=c.resolve("989121234567","Test");
        check("u0ABCDEFGHIJK".equals(resolved),"getContacts fallback resolves phone");
        check(rt.step==3,"resolve performs addAddressBook, fallback contacts, verification");
        System.out.println("PASS RubikaTransportTests "+checks+" assertions");
    }
}
