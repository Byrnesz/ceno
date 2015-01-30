package plugins.CeNo;

import freenet.client.FetchException;
import freenet.client.FetchResult;
import freenet.client.HighLevelSimpleClient;
import freenet.client.InsertBlock;
import freenet.client.InsertContext;
import freenet.client.InsertException;
import freenet.client.async.ClientPutCallback;
import freenet.client.async.ClientPutter;
import freenet.keys.FreenetURI;
import freenet.node.Node;
import freenet.node.NodeClientCore;
import freenet.node.RequestStarter;

public class HighLevelSimpleClientInterface {

	private static volatile HighLevelSimpleClientInterface HLSCInterface = null;

	private HighLevelSimpleClient client;
	private Node node;

	private HighLevelSimpleClientInterface() {
	}

	public HighLevelSimpleClientInterface(Node node, NodeClientCore core) {
		synchronized (HighLevelSimpleClientInterface.class) {
			if (HLSCInterface == null) {
				HLSCInterface = new HighLevelSimpleClientInterface();
				HLSCInterface.node = node;
				HLSCInterface.client = node.clientCore.makeClient(RequestStarter.INTERACTIVE_PRIORITY_CLASS, false, false);
			}
		}
	}

	public HighLevelSimpleClientInterface(HighLevelSimpleClient hlSimpleClient) {
		synchronized (HighLevelSimpleClientInterface.class) {
			if (HLSCInterface == null) {
				HLSCInterface = new HighLevelSimpleClientInterface();
				HLSCInterface.client = hlSimpleClient;
			}
		}
	}

	/**
	 * Synchronously fetch a file from Freenet, given a FreenetURI
	 * 
	 * @param uri of the file to be fetched
	 * @return a FetchResult instance upon successful fetch
	 * @throws FetchException
	 */
	public static FetchResult fetchURI(FreenetURI uri) throws FetchException {
		FetchResult result = HLSCInterface.client.fetch(uri);
		return result;
	}
	
	/**
	 * Generates a new key pair, consisting of the insert URI at index 0 and the
	 * request URI at index 1.
	 *
	 * @param docName
	 *            The document name
	 * @return An array containing the insert and request URI
	 */
	public static FreenetURI[] generateKeyPair(String docName) {
		FreenetURI[] keyPair = HLSCInterface.generateKeyPair(docName);
		return keyPair;
	}
	
	/**
	 * Non-blocking insert.
	 * @param isMetadata If true, insert metadata.
	 * @param cb Will be called when the insert completes. If the request is persistent
	 * this will be called on the database thread with a container parameter.
	 * @param ctx Insert context so you can customise the insertion process.
	 */
	public static ClientPutter insert(InsertBlock insert, String filenameHint, boolean isMetadata, InsertContext ctx, ClientPutCallback cb) throws InsertException {
		ClientPutter clientPutter = HLSCInterface.insert(insert, filenameHint, isMetadata, ctx, cb);
		return clientPutter;
	}

}
