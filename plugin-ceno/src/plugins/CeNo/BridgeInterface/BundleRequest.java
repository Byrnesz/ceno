package plugins.CeNo.BridgeInterface;

public class BundleRequest {

	public static Bundle requestURI(String URI) {
		Bundle bundle = new Bundle(URI);
		bundle.requestFromBundler();
		return bundle;
	}
}
