package edu.csupomona.cs480.data.provider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileNotFoundException;
import java.io.FileWriter;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.net.URL;
import javax.script.*;
import javax.validation.constraints.Null;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import edu.csupomona.cs480.data.GeoIP;
import edu.csupomona.cs480.data.TruckInfo;
import edu.csupomona.cs480.data.TruckMap;
import edu.csupomona.cs480.userInput.UserInput;
import edu.csupomona.cs480.util.ResourceResolver;
import edu.csupomona.cs480.data.GoogleGeoLatLng;
import edu.csupomona.cs480.data.GoogleGeoResult;
import edu.csupomona.cs480.data.GoogleGeoAdressComponent;
import edu.csupomona.cs480.data.GoogleGeoCode;
import edu.csupomona.cs480.data.GoogleGeoBounds;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.*;
import com.maxmind.geoip2.model.CityResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.FileSystemResourceLoader; 

import okhttp3.*;
import okhttp3.Request.Builder;

public class FSTruckInfoManager implements TruckInfoManager{
	
	private static final ObjectMapper JSON = new ObjectMapper();
	private static DatabaseReader dbReader = null;
	private ResourceLoader resourceLoader;
	private static final String googleurl = "https://www.google.com";
	private static final String searchStr = "/search?newwindow=1&tbm=lcl&q=food+truck+near+me&oq=food+truck+near+me";
	
	
	public void testingJavaScript(){
  
	        /*
	         * var map, infoWindow;
      function initMap() { map = new google.maps.Map(document.getElementById('map'), {center: {lat: -34.397, lng: 150.644}, zoom: 6}); infoWindow = new google.maps.InfoWindow;

        // Try HTML5 geolocation.
        if (navigator.geolocation) {
          navigator.geolocation.getCurrentPosition(function(position) {
            var pos = {
              lat: position.coords.latitude,
              lng: position.coords.longitude
            };

            infoWindow.setPosition(pos);
            infoWindow.setContent('Location found.');
            infoWindow.open(map);
            map.setCenter(pos);
          }, function() {
            handleLocationError(true, infoWindow, map.getCenter());
          });
        } else {
          // Browser doesn't support Geolocation
          handleLocationError(false, infoWindow, map.getCenter());
        }
      }

      function handleLocationError(browserHasGeolocation, infoWindow, pos) {
        infoWindow.setPosition(pos);
        infoWindow.setContent(browserHasGeolocation ?
                              'Error: The Geolocation service failed.' :
                              'Error: Your browser doesn\'t support geolocation.');
        infoWindow.open(map);
      }
	         */
	        try{
	     // create a script engine manager
	        ScriptEngineManager factory = new ScriptEngineManager();
	        // create a JavaScript engine
	        ScriptEngine engine = factory.getEngineByName("JavaScript");
	     // JavaScript code in a String
	        URL url = new URL("https://maps.googleapis.com/maps/api/js?key=AIzaSyDW9iStuk-FiFCHBu8teQ5H5NlJ0sa-WLA&callback=initMap");
	        //InputStreamReader reader = new InputStreamReader(url.openStream() );
	        String script = "var obj = new Object(); obj.hello = function(name) { print('Hello, ' + name) };";
	        String script1 = " function initMap() { map = new google.maps.Map(document.getElementById('map'), {center: {lat: -34.397, lng: 150.644}, zoom: 6}); infoWindow = new google.maps.InfoWindow;}";
	        Invocable inv = (Invocable) engine;
	        	//engine.eval(reader);
	        	engine.eval(script);
	        	//engine.eval(script1);
	        	//inv.invokeFunction("hello", "john" );
	        	//Object obj = engine.get("obj");
	        	//inv.invokeMethod(obj, "hello", "Script Method !!" );
	        }catch(Exception e){
	        	e.printStackTrace();
	        }
	       
      
	}
	
	/**
	 * https://halexv.blogspot.mx/2015/07/java-geocoding-using-google-maps-api.html
	 * Given an address asks google for geocode
	 * 
	 * If ssl is true API_KEY should be a valid developer key (given by google)
	 *
	 * @param address the address to find
	 * @param ssl defines if ssl should be used
	 * @return the GoogleGeoCode found
	 * @throws Exception in case of any error
	 *="AIzaSyDW9iStuk-FiFCHBu8teQ5H5NlJ0sa-WLA"
	 */
	@Override
	public GoogleGeoCode getGeoCode(String address, boolean ssl, String API_KEY) throws Exception {
	    // build url
	    StringBuilder url = new StringBuilder("http");
	    if ( ssl ) {
	        url.append("s");
	    }
	   
	    url.append("://maps.googleapis.com/maps/api/geocode/json?");
	   
	    if ( ssl ) {
	        url.append("key=");
	        url.append(API_KEY);
	        url.append("&");
	    }
	    url.append("sensor=false&address=");
	    url.append(URLEncoder.encode(address) );
	   
	    // request url like: http://maps.googleapis.com/maps/api/geocode/json?address=" + URLEncoder.encode(address) + "&sensor=false"
	    // do request
	    try (CloseableHttpClient httpclient = HttpClients.createDefault();) {
	        HttpGet request = new HttpGet(url.toString());

	        // set common headers (may useless)
	        request.setHeader("User-Agent", "Mozilla/5.0 (X11; Linux x86_64; rv:31.0) Gecko/20100101 Firefox/31.0 Iceweasel/31.6.0");
	        request.setHeader("Host", "maps.googleapis.com");
	        request.setHeader("Connection", "keep-alive");
	        request.setHeader("Accept-Language", "en-US,en;q=0.5");
	        request.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
	        request.setHeader("Accept-Encoding", "gzip, deflate");

	        try (CloseableHttpResponse response = httpclient.execute(request)) {
	            HttpEntity entity = response.getEntity();

	            // recover String response (for debug purposes)
	            StringBuilder result = new StringBuilder();
	            try (BufferedReader in = new BufferedReader(new InputStreamReader(entity.getContent()))) {
	                String inputLine;
	                while ((inputLine = in.readLine()) != null) {
	                    result.append(inputLine);
	                    result.append("\n");
	                }
	            }

	            // parse result
	            ObjectMapper mapper = new ObjectMapper();
	            GoogleGeoCode geocode = mapper.readValue(result.toString(), GoogleGeoCode.class);

	            if (!"OK".equals(geocode.getStatus())) {
	                if (geocode.getError_message() != null) {
	                    throw new Exception(geocode.getError_message());
	                }
	                throw new Exception("Can not find geocode for: " + address);
	            }
	            return geocode;
	        }
	    }
	}


    
    @Override
	public List<TruckInfo> getGoogleList() throws IOException{
    	testingJavaScript();
		
		Document doc = null;
		List<TruckInfo> truckInfos = new ArrayList<TruckInfo>();
		try{
			doc = Jsoup.connect(new String(googleurl + searchStr)).get();
			Elements newsHeadLines = doc.select("div[class=_gt] div[class=_rl]");
			
			for(int i= 0; i < newsHeadLines.size(); i++){
				TruckInfo item = new TruckInfo();
				item.setName(newsHeadLines.get(i).text());
				truckInfos.add(item);
			}
		}catch(IOException e){
			e.printStackTrace();
		}
		return truckInfos;
	}
    
	public GeoIP getLocation(String ip) {
        GeoIP item = null;
     	try{
    		Resource testResource = null;
    		ResourceLoader testResourceLoader = new FileSystemResourceLoader();
    		try{
    			 testResource = testResourceLoader.getResource("GeoLite2-City.mmdb" );  	   
    			 if(!testResource.exists()){
    				System.out.println("Could not load test resource: " + testResource);
    			}
    		}catch(Exception e){
    			e.printStackTrace();
    		}
    		
    		File database = testResource.getFile();
    		DatabaseReader reader = new DatabaseReader.Builder(database).build();
    
    		InetAddress ipAddress = InetAddress.getByName(ip);
	    	
	    	CityResponse response = reader.city(ipAddress);
	    	String cityName = response.getCity().getName();
	        String latitude = response.getLocation().getLatitude().toString();
	        String longitude = response.getLocation().getLongitude().toString();
	        return new GeoIP(ip, cityName, latitude, longitude);
        } catch(AddressNotFoundException e){
        	System.out.println("address not found...");
        }catch (GeoIp2Exception e) {  
        } catch (IOException e) {	
        	e.printStackTrace();
        }catch (Exception e) {	
        	e.printStackTrace();
        }
       return item;
    }
	
    @Override
    public GeoIP getGeoIP() throws IOException{
    	String address = InetAddress.getLocalHost().getHostAddress();
		//System.out.println("Current IP address: " + address);
		/* problem current home location is not location in the db therefore using the 128.101.101.101*/
		
		GeoIP g = getLocation("128.101.101.101");
		//GeoIP g = getLocation(address);
		return g;
    }
   
	public TruckMap getTruckMap(){
		TruckMap truckMap = null;
		File truckFile = ResourceResolver.getTruckFile();
		if(truckFile.exists()){
			try{
				truckMap = JSON.readValue(truckFile, TruckMap.class);
			}catch(IOException e){
				e.printStackTrace();
			}
		}else{
			truckMap = new TruckMap();
		}
		return truckMap;
	}
	
	private void persistTruckMap(TruckMap truckMap){
		try{
			JSON.writeValue(ResourceResolver.getTruckFile(), truckMap);
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	

	@Override
	public TruckInfo getTruckInfo(Integer truckId){
		TruckMap truckMap = getTruckMap();
		return truckMap.get(truckId);
	}
	@Override
	public void updateTruckInfo(TruckInfo truck){
	    TruckMap truckMap = getTruckMap();
		truckMap.put(truck.getId(), truck);
		persistTruckMap(truckMap);
	}
	@Override
	public void deleteTruckInfo(Integer truckId){
		TruckMap truckMap = getTruckMap();
		truckMap.remove(truckId);
		persistTruckMap(truckMap);
	}
	@Override
	public List<TruckInfo> listAllTrucks(){
		TruckMap truckMap = getTruckMap();
		return new ArrayList<TruckInfo>(truckMap.values());
	}
	@Override
	public List<TruckInfo> searchGoogleResult() throws IOException{
		TruckMap truckMap = getTruckMap();
		List<TruckInfo> result = getGoogleList();
		return result;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<TruckInfo> searchYelp(String type, String address,String city,double lat, double lon) throws IOException{	
		List<TruckInfo> result = new ArrayList<TruckInfo>();
		String accessToken="";
		OkHttpClient client = new OkHttpClient();
		accessToken = "NNW_d42viWMIZVJyu5Rjq2WmQr3gDt8CXRH5-wN7Z2UKQWuBv_ov-ojvyqAMxSUfvOktjv1UVXSfS0iNGYOVllV2uqPOVMaCI9J_Oe4dpbOnq7openFgqbS7puj3WXYx";
        String term = type; 
        String location="";
        Request request2;
        if(address!=null) {
        	if(address.equals(""))
            	location = city;            
        	else
        		location=address + " " + city;
            request2 = new Builder()
                    .url("https://api.yelp.com/v3/businesses/search?term=" + term 
                    		+ "&location=" + location 
                    		+ "&radius=16094"
                    		+ "&categories=foodtrucks"
                    		+ "&sort_by=best_match")
                    .get()
                    .addHeader("authorization", "Bearer"+" "+accessToken)
                    .addHeader("cache-control", "no-cache")
                    .addHeader("postman-token", "b5fc33ce-3dad-86d7-6e2e-d67e14e8071b")
                    .build();
        }
        else {
            request2 = new Builder()
                    .url("https://api.yelp.com/v3/businesses/search?term=" + term 
                    		+ "&latitude=" + lat 
                    		+ "&longitude=" + lon 
                    		+ "&radius=16094"
                    		+ "&categories=foodtrucks"
                    		+ "&sort_by=best_match")
                    .get()
                    .addHeader("authorization", "Bearer"+" "+accessToken)
                    .addHeader("cache-control", "no-cache")
                    .addHeader("postman-token", "b5fc33ce-3dad-86d7-6e2e-d67e14e8071b")
                    .build();
        }

        try {
            Response response2 = client.newCall(request2).execute();
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(response2.body().string());
            JSONObject jsonObject = (JSONObject) obj;       // parser
            JSONArray list = (JSONArray) jsonObject.get("businesses");
            
            int size;
            // Making sure list isn't empty
            if (list == null) {
            	size = 0;
            } else {
            	size = list.size();
            	if(size>=40)
            		size=40;
            }
            
            for(int i=0;i<size;i++) {
            	JSONObject temp = (JSONObject) list.get(i);
            	TruckInfo truck = new TruckInfo();
    			truck.setId((String) temp.get("id"));
            	truck.setName((String) temp.get("name"));
            	truck.setImageUrl((String)temp.get("image_url")); 
    			truck.setType(type);
 		
    			JSONObject tempLoc = (JSONObject) temp.get("location");
    			truck.setAddress((String) tempLoc.get("address1"));
    		  
    			
    			String pN = (String) temp.get("phone");
    			if (pN != null && !pN.isEmpty()) {
	     			// Eliminate the "+" in front
	     			pN = pN.substring(1);
	     			truck.setPhoneNumber(pN.substring(pN.length()-7));
	     			truck.setAreaCode(Integer.parseInt(pN.substring(pN.length()-10, pN.length()-7)));
    		    }
    			truck.setCity((String) tempLoc.get("city"));
    			truck.setZipCode((String) tempLoc.get("zip_code"));
    			JSONObject tempCoord = (JSONObject) temp.get("coordinates");
    			truck.setLat(Double.parseDouble( tempCoord.get("latitude").toString() ));
    			truck.setLon(Double.parseDouble( tempCoord.get("longitude").toString() ));
    			
    		    result.add(truck);
    			//truckMap.put(truck.getId(), truck);
            }
        } 
        catch (Exception e) {
            e.printStackTrace();
        }
        return result;     
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<TruckInfo> searchYelpV2(UserInput userInput) throws IOException{
		List<TruckInfo> result = new ArrayList<TruckInfo>();
        List<String> foodTypes = userInput.getFoodTypes();
    	HashMap<String,Integer> fTList= new HashMap<String,Integer>();
		for(int i=0;i<foodTypes.size();i++) {
			String foodType = foodTypes.get(i);
			if(foodType.equalsIgnoreCase("Surprise Me!")){
				foodType="";
			}
			OkHttpClient client = new OkHttpClient();
			String accessToken = "NNW_d42viWMIZVJyu5Rjq2WmQr3gDt8CXRH5-wN7Z2UKQWuBv_ov-ojvyqAMxSUfvOktjv1UVXSfS0iNGYOVllV2uqPOVMaCI9J_Oe4dpbOnq7openFgqbS7puj3WXYx";
			String url="";
			if(!foodType.equals("")) {
				url="https://api.yelp.com/v3/businesses/search?term="+foodType+"&";
			}
			else {
				url="https://api.yelp.com/v3/businesses/search?";
			}
			if(userInput.getLocationType().equalsIgnoreCase("current location")) {
				url+="latitude=" + userInput.getLat()  
				+ "&longitude=" + userInput.getLon() ;
			}
			else {
				url+="location=" + userInput.getLocationValue();
			}
			url+= "&radius=16094"
					+ "&categories=foodtrucks";
			Request request = new Builder()
					.url(url)
					.get()
					.addHeader("authorization", "Bearer"+" "+accessToken)
					.addHeader("cache-control", "no-cache")
					.addHeader("postman-token", "b5fc33ce-3dad-86d7-6e2e-d67e14e8071b")
					.build();
			try {
				Response response2 = client.newCall(request).execute();
            	JSONParser parser = new JSONParser();
            	Object obj = parser.parse(response2.body().string());
            	JSONObject jsonObject = (JSONObject) obj;       // parser
            	JSONArray list = (JSONArray) jsonObject.get("businesses");
            	int size;
            	// Making sure list isn't empty
            	if (list == null) {
            		size = 0;
            	} else {
            		size = list.size();
            		if(size>=(40/foodTypes.size()))
            			size=(40/foodTypes.size());
            	}
            	for(int j=0;j<size;j++) {
            		JSONObject temp = (JSONObject) list.get(j);
            		TruckInfo truck = new TruckInfo();
            		truck.setId((String) temp.get("id"));
					if(fTList.containsKey(truck.getId())) {
						result.get(fTList.get(truck.getId())).setType(result.get(fTList.get(truck.getId())).getType()+" "+foodType);
						continue;
					}
					else {
						fTList.put(truck.getId(), result.size());
					}
					truck.setName((String) temp.get("name"));
					truck.setImageUrl((String)temp.get("image_url")); 
					JSONObject tempLoc = (JSONObject) temp.get("location");
					truck.setAddress((String) tempLoc.get("address1"));    			
					String pN = (String) temp.get("phone");
					if (pN != null && !pN.isEmpty()) {
						// Eliminate the "+" in front
						pN = pN.substring(1);
						truck.setPhoneNumber(pN.substring(pN.length()-7));
						truck.setAreaCode(Integer.parseInt(pN.substring(pN.length()-10, pN.length()-7)));
					}
					truck.setCity((String) tempLoc.get("city"));
					truck.setZipCode((String) tempLoc.get("zip_code"));
					JSONObject tempCoord = (JSONObject) temp.get("coordinates");
					truck.setLat(Double.parseDouble( tempCoord.get("latitude").toString() ));
					truck.setLon(Double.parseDouble( tempCoord.get("longitude").toString() ));	
					if(foodType.equals(""))
						truck.setType("Surprise!");
					else
						truck.setType(foodType);
					result.add(truck);
					//truckMap.put(truck.getId(), truck);
            	}
			} 
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		if(foodTypes.get(0).equalsIgnoreCase("Surprise Me!")) {
			// If user entered a searchable/correct address/zip code/city
			if (!result.isEmpty()) {
				Collections.shuffle(result);
				return result.subList(0,10);
			}
		}
        return result;     
	}

}
