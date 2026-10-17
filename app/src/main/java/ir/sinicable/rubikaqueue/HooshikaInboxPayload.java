package ir.sinicable.rubikaqueue;

import org.json.JSONArray;
import org.json.JSONObject;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

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
                String kind=row.optString("kind");
                String name=clean(row.optString("name"),140);
                String phone=clean(row.optString("phone"),40);
                String url=clean(row.optString("url"),800);
                JSONObject result=new JSONObject();
                result.put("name",name);
                if ("public_group".equals(kind)) {
                    if (!validPublicRubikaLink(url))
                        throw new IllegalArgumentException("Untrusted public group link");
                    result.put("kind",kind).put("url",url);
                } else if ("business_contact".equals(kind)) {
                    if (!url.isEmpty() && !validHttps(url)) url="";
                    if (phone.isEmpty() && url.isEmpty())
                        throw new IllegalArgumentException("Missing business contact");
                    result.put("kind",kind);
                    if (!phone.isEmpty()) result.put("phone",phone);
                    if (!url.isEmpty()) result.put("url",url);
                    result.put("source",clean(row.optString("source"),80));
                } else {
                    throw new IllegalArgumentException("Unknown kind");
                }
                // Ignore consent, approved, status, and all other untrusted fields.
                if (seen.add(identity(result))) safe.put(result);
            }
            return safe;
        } catch (IllegalArgumentException e) { throw e; }
          catch (Exception e) { throw new IllegalArgumentException("Invalid JSON packet",e); }
    }

    static JSONArray merge(JSONArray existing,JSONArray incoming) {
        JSONArray joined=new JSONArray();
        Set<String> seen=new HashSet<>();
        for (JSONArray source : new JSONArray[]{existing,incoming}) {
            if (source == null) continue;
            for(int i=0;i<source.length();i++) {
                JSONObject row=source.optJSONObject(i);
                if(row == null)continue;
                String key=identity(row);
                if(seen.add(key)){
                    if(joined.length() >= MAX_STORED)
                        throw new IllegalArgumentException("Review inbox is full");
                    joined.put(row);
                }
            }
        }
        return joined;
    }

    private static String identity(JSONObject row) {
        String kind=row.optString("kind");
        String url=row.optString("url");
        String phone=row.optString("phone");
        return kind + ":" + ("public_group".equals(kind) ? url.toLowerCase(Locale.ROOT)
            : (phone.isEmpty() ? url.toLowerCase(Locale.ROOT) : phone));
    }

    private static String clean(String value,int limit){
        String v=value == null ? "" : value.replaceAll("[\\p{Cntrl}]", " ").trim();
        return v.substring(0,Math.min(limit,v.length()));
    }

    static boolean validHttps(String url){
        try{
            URI u=new URI(url);
            return "https".equalsIgnoreCase(u.getScheme()) && u.getHost()!=null
                && u.getUserInfo()==null && u.getPort()==-1;
        } catch(Exception e){return false;}
    }

    static boolean validPublicRubikaLink(String url){
        try{
            URI u=new URI(url);
            return validHttps(url) && "rubika.ir".equalsIgnoreCase(u.getHost())
                && u.getRawQuery()==null && u.getRawFragment()==null
                && u.getRawPath()!=null && u.getRawPath().matches("/[A-Za-z0-9_/-]{3,120}");
        }catch(Exception e){return false;}
    }
}
