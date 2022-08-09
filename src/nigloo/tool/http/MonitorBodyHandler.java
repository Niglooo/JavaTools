package nigloo.tool.http;

import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodySubscriber;
import java.net.http.HttpResponse.ResponseInfo;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

public class MonitorBodyHandler<T> implements BodyHandler<T>
{
	private final BodyHandler<T> delegate;
	private final DownloadListener listener;
	
	public MonitorBodyHandler(BodyHandler<T> delegate, DownloadListener listener)
	{
		this.delegate = Objects.requireNonNull(delegate, "delegate BodyHandler cannot be null");
		this.listener = Objects.requireNonNull(listener, "listener cannot be null");
	}
	
	@Override
	public BodySubscriber<T> apply(ResponseInfo responseInfo)
	{
		OptionalLong contentLength = responseInfo.headers().firstValueAsLong("Content-Length");
		listener.onStartDownload(responseInfo);
		BodySubscriber<T> delegateSubscriber = Objects.requireNonNull(delegate.apply(responseInfo),
		                                                              delegate.getClass().getSimpleName()
		                                                                      + ".apply returned null");
		return new MonitorBodySubscriber<>(delegateSubscriber, listener, contentLength);
	}
	
	private static class MonitorBodySubscriber<T> implements BodySubscriber<T>
	{
		private final BodySubscriber<T> delegate;
		private final DownloadListener listener;
		private final OptionalLong contentLength;
		
		private long nbBytesDownloaded = 0L;
		
		private MonitorBodySubscriber(BodySubscriber<T> delegate, DownloadListener listener, OptionalLong contentLength)
		{
			this.delegate = delegate;
			this.listener = listener;
			this.contentLength = contentLength;
		}
		
		@Override
		public CompletionStage<T> getBody()
		{
			return delegate.getBody();
		}
		
		@Override
		public void onSubscribe(Flow.Subscription subscription)
		{
			delegate.onSubscribe(subscription);
		}
		
		@Override
		public void onNext(List<ByteBuffer> item)
		{
			long nbNewBytes = item.stream().mapToLong(ByteBuffer::remaining).sum();
			delegate.onNext(item);
			nbBytesDownloaded += nbNewBytes;
			listener.onProgress(nbNewBytes, nbBytesDownloaded, contentLength);
		}
		
		@Override
		public void onError(Throwable throwable)
		{
			delegate.onError(throwable);
			listener.onError(throwable);
		}
		
		@Override
		public void onComplete()
		{
			delegate.onComplete();
			listener.onComplete();
		}
	}
}
