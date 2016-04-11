package com.bah.paul_matthew;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import javax.net.ssl.HttpsURLConnection;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class HttpHandler {
	private final String USER_AGENT = "Mozilla/5.0";
	
	public HttpHandler() {
		
	}
	
	public String buildJSON(List<int[]> features) throws JSONException {
		// Build the JSON object
				String[] columnNames = new String[65];
				List<String[]> digitFeatures = new ArrayList<String[]>();
				
				for(int i=1; i < 65; i++) {
					columnNames[i-1] = "Col" + String.valueOf(i);
				}
				// Collect JSON objects for each digit
				for(int k = 0; k < features.size(); k++) {
					String[] stringFeatures = new String[65];
					for(int i=0; i < 64; i++) {
						stringFeatures[i] = String.valueOf(features.get(k)[i]);
					}
					stringFeatures[64] = "0";
					digitFeatures.add(stringFeatures);
				}
				columnNames[64] = "Col65";
				JSONArray columns = new JSONArray(columnNames);
				JSONArray[] valuesArray = new JSONArray[features.size()];
				for(int i = 0; i < digitFeatures.size(); i++) {
					JSONArray values = new JSONArray(digitFeatures.get(i));
					valuesArray[i] = values;
				}
				JSONObject input1 = new JSONObject();
				JSONObject Inputs = new JSONObject();
				JSONObject globalParams = new JSONObject();
				JSONObject data = new JSONObject(); // Top of JSON
				
				data.put("Inputs", Inputs);
				Inputs.put("input1", input1);
				data.put("GlobalParameters", globalParams);
				input1.put("ColumnNames", columns);
				input1.put("Values", valuesArray);
				
				String jsonData = data.toString();
				System.out.println(jsonData);
				return jsonData;
	}
	
	@SuppressWarnings("finally")
	public JSONObject createPost(String url, List<int[]> features) throws IOException, JSONException {
		// You'll need to supply the appropriate API key here. I've removed it since we're hosting this code publicly. 
		String apiKey = "";
		String jsonData = this.buildJSON(features);
		JSONObject jsonResponse = null;
		
		// Build the header
		HttpClient httpClient = HttpClientBuilder.create().build();
		
		try {
			HttpPost request = new HttpPost(url);
			StringEntity params = new StringEntity(jsonData);
			request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
			request.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
			request.addHeader(HttpHeaders.ACCEPT, "application/json");
			request.setEntity(params);
			HttpResponse response = httpClient.execute(request);
			
			BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
			StringBuffer result = new StringBuffer();
			String line = "";
			while ((line = rd.readLine()) != null) {
				result.append(line);
			}
			jsonResponse = new JSONObject(result.toString());
			System.out.println(result.toString());
			return jsonResponse;
		}catch (Exception e) {
			e.printStackTrace();
		} 
		finally {
			
			return jsonResponse;
		}
		
		
		
	}

}
