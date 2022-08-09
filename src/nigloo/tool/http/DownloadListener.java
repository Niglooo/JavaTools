package nigloo.tool.http;

import java.net.http.HttpResponse.ResponseInfo;
import java.util.OptionalLong;

public interface DownloadListener
{
	default void onStartDownload(ResponseInfo responseInfo)
	{
	}
	
	void onProgress(long nbNewBytes, long nbBytesDownloaded, OptionalLong nbBytesTotal);
	
	default void onComplete()
	{
	}
	
	default void onError(Throwable error)
	{
	}
}
