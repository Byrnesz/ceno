package plugins.CeNo;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.minidev.json.JSONObject;
import net.minidev.json.parser.ParseException;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.util.log.Log;

import plugins.CeNo.BridgeInterface.Bundle;
import freenet.client.InsertException;
import freenet.client.async.BaseClientPutter;
import freenet.client.async.ClientContext;
import freenet.client.async.ClientPutCallback;
import freenet.keys.FreenetURI;
import freenet.node.RequestClient;
import freenet.support.Logger;
import freenet.support.api.Bucket;
import freenet.support.io.ResumeFailedException;

/* ------------------------------------------------------------ */
/** CeNo Plugin handler for requests to cache bundles
 * 	
 * CacheInsertHandler listens to {@link CeNo#cacheInsertPort} port
 * and under the "/store" route for POST requests.
 * Those POST requests include the bundled page as well as the original url.
 * The handler caches the given bundle under its signed subspace.
 * Upon successful insertion, the handler replies with "stored".
 */
public class CacheInsertHandler extends CeNoHandler {
	
	public class InsertCallback implements ClientPutCallback {

		public void onResume(ClientContext context)
				throws ResumeFailedException {
			// TODO Auto-generated method stub
			
		}

		public RequestClient getRequestClient() {
			return CeNo.nodeInterface.getClient();
		}

		public void onGeneratedURI(FreenetURI uri, BaseClientPutter state) {
			// TODO Auto-generated method stub
			
		}

		public void onGeneratedMetadata(Bucket metadata, BaseClientPutter state) {
			// TODO Auto-generated method stub
			
		}

		public void onFetchable(BaseClientPutter state) {
			// TODO Auto-generated method stub
			
		}

		public void onSuccess(BaseClientPutter state) {
			Logger.normal(this, "Caching successful");	
		}

		public void onFailure(InsertException e, BaseClientPutter state) {
			Logger.error(this, "Failed to insert freesite " + e);
		}
		
	}

	public void handle(String target,Request baseRequest,HttpServletRequest request,HttpServletResponse response)
			throws IOException, ServletException {
		// Only POST requests at /store route will be handled
		if (!request.getMethod().equals("POST")) {
			writeError(baseRequest, response, "Not a POST request at /store route");
			return;
		}

		// Get the data from the POST request
		//TODO Add support for multi-part POST requests
		JSONObject requestJSON;
		try {
			requestJSON = readJSONbody(request.getReader());
		} catch (ParseException e) {
			writeError(baseRequest, response, "Could not parse JSON");
			return;
		}
		if(!requestJSON.containsKey("url")) {
			writeError(baseRequest, response, "No url attribute in request body");
			return;
		}
		if(!requestJSON.containsKey("bundle")) {
			writeError(baseRequest, response, "No bundle attribute in request body");
			return;
		}
		
		String urlParam = requestJSON.get("url").toString();
		if((urlParam == null) || urlParam.isEmpty()) {
			writeError(baseRequest, response, "Invalid url attribute");
			return;
		}
		
		Bundle bundle = new Bundle(urlParam);
		if ((requestJSON.get("bundle") != null)) {
			bundle.setContent(requestJSON.get("bundle").toString());
		} else {
			writeError(baseRequest, response, "Bundle attribute was null");
			return;
		}

		if (!bundle.getContent().isEmpty()) {
			//TODO non-blocking insert the bundle content in freenet with the computed USK
			Map<String, String> splitMap = splitURL(urlParam);
			FreenetURI insertKey = computeInsertURI(splitMap.get("domain"));
			try {
				CeNo.nodeInterface.insertFreesite(insertKey, splitMap.get("extraPath"), bundle.getContent(), new InsertCallback());
			} catch (InsertException e) {
				writeError(baseRequest, response, "Error during insertion");
				return;
			}
			
			response.setContentType("text/html;charset=utf-8");
			response.setStatus(HttpServletResponse.SC_OK);
			response.getWriter().println("stored");
			baseRequest.setHandled(true);
		} else {
			writeError(baseRequest, response, urlParam);
		}
	}
}
