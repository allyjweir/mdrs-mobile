package ggow.teamt.mdrs;

import com.loopj.android.http.*;

public class mdrsHttpUpload {
	private static final String BASE_URL = "http://penida.dcs.gla.ac.uk/webapp/upload";

	private static AsyncHttpClient client = new AsyncHttpClient();

	public static void get(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
		client.get(getAbsoluteUrl(url), params, responseHandler);
	}

	public void post(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
		client.post(getAbsoluteUrl(url), params, responseHandler);
	}

	private static String getAbsoluteUrl(String relativeUrl) {
		return BASE_URL + relativeUrl + "/";
	}
}
