package com.kuxhausen.huemore.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import com.google.gson.Gson;
import com.kuxhausen.huemore.persistence.DatabaseDefinitions.PreferencesKeys;
import com.kuxhausen.huemore.state.api.BulbAttributes;

public class SetBulbAttributes extends AsyncTask<Void, Void, Integer> {

	private Context cont;
	private int bulbNum;
	private BulbAttributes bulbAtt;
	Gson gson = new Gson();

	public SetBulbAttributes(Context context, int bulbN, BulbAttributes bulbA) {
		cont = context;
		bulbNum = bulbN;
		bulbAtt = bulbA;
	}

	@Override
	protected Integer doInBackground(Void... voids) {

		if (cont == null || bulbAtt == null )
			return -1;

		// Get username and IP from preferences cache
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(cont);
		String bridge = settings.getString(PreferencesKeys.BRIDGE_IP_ADDRESS,
				null);
		String hash = settings.getString(PreferencesKeys.HASHED_USERNAME, "");

		if (bridge == null)
			return -1;

		

		StringBuilder builder = new StringBuilder();
		HttpClient client = new DefaultHttpClient();

		HttpPut httpPut = new HttpPut("http://" + bridge + "/api/" + hash
				+ "/lights/" + bulbNum);
		try {

			StringEntity se = new StringEntity(gson.toJson(bulbAtt));

			// sets the post request as the resulting string
			httpPut.setEntity(se);

			HttpResponse response = client.execute(httpPut);
			StatusLine statusLine = response.getStatusLine();
			int statusCode = statusLine.getStatusCode();
			if (statusCode == 200) {

				HttpEntity entity = response.getEntity();
				InputStream content = entity.getContent();
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(content));
				String line;
				String debugOutput = "";
				while ((line = reader.readLine()) != null) {
					builder.append(line);
					debugOutput += line;
				}
			} else {
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	
		return 1;
	}

	@Override
	protected void onPostExecute(Integer result) {
	}

}