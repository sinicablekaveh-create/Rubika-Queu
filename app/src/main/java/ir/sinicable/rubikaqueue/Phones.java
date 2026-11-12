package ir.sinicable.rubikaqueue;
import java.math.BigDecimal;
public final class Phones {
 public static String digits(String s){StringBuilder b=new StringBuilder();for(char c:s.toCharArray()){if(c>='۰'&&c<='۹')c=(char)('0'+c-'۰');if(c>='٠'&&c<='٩')c=(char)('0'+c-'٠');b.append(c);}return b.toString();}
 public static String normalize(String value){
  String s=digits(value==null?"":value).trim().replaceAll("[\\s\\u00a0\\u200e\\u200f\\u202a-\\u202e()\\-]","");
  if(s.startsWith("'"))s=s.substring(1);
  if(s.matches("[0-9]+(?:\\.[0-9]+)?(?:[Ee]\\+?[0-9]+)?"))try{s=new BigDecimal(s).toBigIntegerExact().toString();}catch(Exception e){return "";}
  if(s.startsWith("+"))s=s.substring(1);if(s.startsWith("00"))s=s.substring(2);
  if(s.matches("09[0-9]{9}"))s="98"+s.substring(1);
  else if(s.matches("9[0-9]{9}"))s="98"+s;
  return s.matches("989[0-9]{9}")?s:"";
 }
}
