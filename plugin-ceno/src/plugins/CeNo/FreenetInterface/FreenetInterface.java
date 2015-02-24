package plugins.CeNo.FreenetInterface;

import java.io.IOException;
import java.util.HashMap;

import plugins.CeNo.CacheInsertHandler.InsertCallback;
import freenet.client.FetchException;
import freenet.client.FetchResult;
import freenet.client.InsertException;
import freenet.keys.FreenetURI;
import freenet.support.api.Bucket;

public interface FreenetInterface {

	FetchResult fetchURI(FreenetURI uri) throws FetchException;
	FreenetURI[] generateKeyPair();
	boolean insertFreesite(FreenetURI insertURI, String docName, String content, InsertCallback cb) throws IOException, InsertException;
	Bucket makeBucket(int length) throws IOException;
	public FreenetURI insertManifest(FreenetURI insertURI, HashMap<String, Object> bucketsByName, String defaultName, short priorityClass) throws InsertException;
}
