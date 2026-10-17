package ir.sinicable.rubikaqueue;

import org.json.JSONArray;
import org.json.JSONObject;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;

/** A review-only inbox protocol. Untrusted fields never become a queue job or consent. */
final class HooshikaInboxPayload {
    static final String MIME="application/vnd.hooshika.rubika-results+json";
    static final String SCHEMA="hooshika.rubika.handoff.v1";
    static final int MAX_BYTES=65536, MAX_INCOMING=40, MAX_STORED=100;

    static JSONArray decode(String raw) {
        if (raw == null || raw.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES)
            throw new IllegalArgumentException("Payload size invalid");
        try {
            JSONObject packet=new JSONObject(raw);
            if (!SCHEMA.equals(packet.optString("schema")))
                throw new IllegalArgumentException("Unknown handoff schema");
            JSONArray incoming=packet.optJSONArray("items");
            if (incoming == null || incoming.length() == 0 || incoming.length() > MAX_INCOMING)
                throw new IllegalArgumentException("Item count invalid");
            JSONArray safe=new JSONArray();
            Set<String> seen=new HashSet<>();
            for (int i=0;i<incoming.length();i++) {
                JSONObject row=incoming.optJSONObject(i);
                if (row == null) throw new IllegalArgumentException("Malformed record");
                String kind=string(row,"kind",32);
                String name=string(row,"name",140);
                String rawPhone=string(row,"phone",40);
                String phone=DiscoveryLinks.phone(rawPhone);
                if(!rawPhone.isEmpty()&&phone.isEmpty())throw new IllegalArgumentException("Invalid phone");
                String url=string(row,"url",800);
                JSONObject result=new JSONObject();
                result.put("name",name);
                if (DiscoveryLinks.publicKind(kind)) {
                    url=DiscoveryLinks.rubika(url);
                    if (url.isEmpty())
                        throw new IllegalArgumentException("Untrusted public group link");
                    result.put("kind",kind).put("url",url);
                } else if ("business_contact".equals(kind)) {
                    if (!url.isEmpty()) {
                        url=DiscoveryLinks.https(url);
                        if(url.isEmpty())throw new IllegalArgumentException("Untrusted contact link");
                    }
                    if (phone.isEmpty() && url.isEmpty())
                        throw new IllegalArgumentException("Missing business contact");
                    result.put("kind",kind);
                    if (!phone.isEmpty()) result.put("phone",phone);
                    if (!url.isEmpty()) result.put("url",url);
                } else {
                    throw new IllegalArgumentException("Unknown kind");
                }
                result.put("source",string(row,"source",80));
                result.put("observedAt",Math.max(0,row.optLong("observedAt",0)));
                // Ignore consent, approved, status, and all other untrusted fields.
                Set<String> keys=identities(result);
                boolean duplicate=false;
                for(String key:keys)if(seen.contains(key)){duplicate=true;break;}
                if(!duplicate){seen.addAll(keys);safe.put(result);}
            }
            return safe;
        } catch (IllegalArgumentException e) { throw e; }
          catch (Exception e) { throw new IllegalArgumentException("Invalid JSON packet",e); }
    }

    static JSONArray merge(JSONArray existing,JSONArray incoming) {
        JSONArray joined=new JSONArray();
        Map<String,Integer> seen=new HashMap<>();
        for (JSONArray source : new JSONArray[]{existing,incoming}) {
            if (source == null) continue;
            for(int i=0;i<source.length();i++) {
                JSONObject row=source.optJSONObject(i);
                if(row == null)continue;
                JSONObject copy=copyOf(row);
                Set<String> keys=identities(copy);
                Integer match=null;
                for(String key:keys)if(seen.containsKey(key)){match=seen.get(key);break;}
                if(match!=null){
                    JSONObject prior=joined.optJSONObject(match);
                    if(prior!=null){
                        Iterator<String> fields=copy.keys();
                        while(fields.hasNext()){
                            String key=fields.next();
                            if(!prior.has(key))try{prior.put(key,copy.get(key));}catch(Exception ignored){}
                        }
                    }
                    for(String key:keys)seen.put(key,match);
                }else{
                    if(joined.length() >= MAX_STORED)
                        throw new IllegalArgumentException("Review inbox is full");
                    int index=joined.length();
                    joined.put(copy);
                    for(String key:keys)seen.put(key,index);
                }
            }
        }
        return joined;
    }

    static JSONArray authorizeSelected(JSONArray existing,Set<Integer> indexes,String basis,long now) {
        if(existing==null||indexes==null||indexes.isEmpty())
            throw new IllegalArgumentException("Select at least one review item");
        String note=basis==null?"":basis.replaceAll("[\\p{Cntrl}]"," ").trim();
        if(note.length()<8||note.length()>250)
            throw new IllegalArgumentException("A short authorization note is required");
        JSONArray result=new JSONArray();
        for(int i=0;i<existing.length();i++) {
            JSONObject row=existing.optJSONObject(i);
            if(row==null)throw new IllegalArgumentException("Malformed saved inbox");
            JSONObject copy=copyOf(row);
            if(indexes.contains(i))try{
                copy.put("reviewApproved",true);
                copy.put("authorizationBasis",note);
                copy.put("authorizedAt",Math.max(0,now));
            }catch(Exception e){throw new IllegalArgumentException("Could not authorize row",e);}
            result.put(copy);
        }
        for(Integer index:indexes)
            if(index==null||index<0||index>=existing.length())
                throw new IllegalArgumentException("Selection changed");
        return result;
    }

    private static Set<String> identities(JSONObject row) {
        Set<String> keys=new HashSet<>();
        String kind=row.optString("kind");
        String url=row.optString("url");
        String phone=DiscoveryLinks.phone(row.optString("phone"));
        if(DiscoveryLinks.publicKind(kind))keys.add(kind+":"+DiscoveryLinks.rubika(url));
        else {
            if(!phone.isEmpty())keys.add("business:"+phone);
            if(!url.isEmpty())keys.add("business-url:"+DiscoveryLinks.https(url).toLowerCase(Locale.ROOT));
        }
        return keys;
    }

    private static JSONObject copyOf(JSONObject row) {
        try { return new JSONObject(row.toString()); }
        catch (Exception e) { throw new IllegalArgumentException("Invalid review item",e); }
    }

    private static String string(JSONObject row,String key,int limit){
        Object raw=row.opt(key);
        if(raw==null||raw==JSONObject.NULL)return "";
        if(!(raw instanceof String))throw new IllegalArgumentException("Invalid "+key);
        String value=((String)raw).replaceAll("[\\p{Cntrl}]", " ").trim();
        if(value.length()>limit)throw new IllegalArgumentException(key+" too long");
        return value;
    }

    static boolean validHttps(String url){
        return !DiscoveryLinks.https(url).isEmpty();
    }

    static boolean validPublicRubikaLink(String url){
        return !DiscoveryLinks.rubika(url).isEmpty();
    }
}

