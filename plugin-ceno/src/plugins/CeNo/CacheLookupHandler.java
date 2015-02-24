package plugins.CeNo;

import java.io.IOException;
import java.net.MalformedURLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.minidev.json.JSONObject;

import org.eclipse.jetty.server.Request;

import plugins.CeNo.BridgeInterface.Bundle;
import plugins.CeNo.FreenetInterface.HighLevelSimpleClientInterface;
import freenet.client.FetchException;
import freenet.client.FetchException.FetchExceptionMode;
import freenet.client.FetchResult;
import freenet.keys.FreenetURI;

/* ------------------------------------------------------------ */
/** CeNo Plugin Http Communication and Serving server.
 *  CeNo nodes talk to CeNo through HTTP, 
 * - CeNo Client reply the url.
 * - Plugin either serve the content or DNF.
 * - In case of DNF, the client send a request asking plugin
 * - to ping a bridge to bundle the content.
 * - If Freenet being pingged, the plugin will send a 
 *   url to the CeNo bridge to bundle the content
 * - The Bridge then will serve the bundle to the plugin
 *   to insert into Freenet
 */
public class CacheLookupHandler extends CeNoHandler {

	public void handle(String target,Request baseRequest,HttpServletRequest request,HttpServletResponse response) 
			throws IOException, ServletException {
		String requestPath = request.getPathInfo().substring(1);
		String urlParam = (request.getParameter("url") != null) ? request.getParameter("url") : requestPath;
		if (urlParam.isEmpty() && requestPath.isEmpty()) {
			writeWelcome(baseRequest, response, requestPath);
		} else if (requestPath.startsWith("USK@") || requestPath.startsWith("SSK@")) {
			FetchResult result = null;
			try {
				result = HighLevelSimpleClientInterface.fetchURI(new FreenetURI(requestPath));
			} catch (MalformedURLException e) {
				writeError(baseRequest, response, requestPath);
				return;
			} catch (FetchException e) {
				// USK key has been updated, redirect to the new URI
				if (e.getMode() == FetchExceptionMode.PERMANENT_REDIRECT) {
					String newURI = "/".concat(e.newURI.toString());
					response.sendRedirect(newURI);
				} else if (e.isDNF()) {
					// The requested URL has not been found in the cache
					// Return JSON {"bundleFound": "false"}
					JSONObject jsonResponse = new JSONObject();
					jsonResponse.put("bundleFound", false);
					response.setStatus(HttpServletResponse.SC_NOT_FOUND);
					response.setContentType("application/json;charset=utf-8");

					response.getWriter().print(jsonResponse.toJSONString());
					baseRequest.setHandled(true);
					return;
				} else{
					e.printStackTrace();
					writeError(baseRequest, response, requestPath);
					return;
				}
			}
			if (result != null) {
				// Bundler for the requested URL has been successfully retrieved
				Bundle bundle = new Bundle(urlParam);
				bundle.setContent(result.asByteArray());

				response.setContentType(result.getMimeType());
				response.setStatus(HttpServletResponse.SC_OK);
				response.setContentType("application/json;charset=utf-8");
				JSONObject jsonResponse = new JSONObject();
				jsonResponse.put("bundleFound", true);
				jsonResponse.put("bundle", bundle.getContent());

				response.getWriter().print(jsonResponse.toJSONString());
				baseRequest.setHandled(true);
				return;
			} else {
				// Error while retrieving the bundle from the cache
				writeError(baseRequest, response, requestPath);
			}
		// Stop background requests normally made by browsers for website resources,
		// that could start a time-consuming lookup in freenet
		} else if (requestPath.equals("favicon.ico")) {
			writeNotFound(baseRequest, response, requestPath);
		} else {
			// Request path is in form of URL
			// Calculate its USK and redirect the request
			FreenetURI calculatedUSK = null;
			try {
				calculatedUSK = computeUSKfromURL(urlParam);
			} catch (Exception e) {
				writeError(baseRequest, response, requestPath);
				return;
			}

			response.sendRedirect("/" + calculatedUSK.toString());
			baseRequest.setHandled(true);
		}
	}
}