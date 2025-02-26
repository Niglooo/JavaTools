package nigloo.tool;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.CopyOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.Collator;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public class Utils
{
	private Utils()
	{
		throw new UnsupportedOperationException();
	}
	
	public static final Comparator<CharSequence> NATURAL_ORDER = new NaturalOrderComparator();
	
	private static class NaturalOrderComparator implements Comparator<CharSequence>
	{
		private final Collator collator;

		NaturalOrderComparator()
        {
            this.collator = Collator.getInstance();
			this.collator.setDecomposition(Collator.FULL_DECOMPOSITION);
			this.collator.setStrength(Collator.PRIMARY);
        }

        @Override
		public int compare(CharSequence seq1, CharSequence seq2)
		{
			int len1 = seq1.length();
			int len2 = seq2.length();
			int idx1 = 0;
			int idx2 = 0;
			
			while (true)
			{
				if (idx1 >= len1 && idx2 >= len2)
					return 0;
				else if (idx1 >= len1)
					return -1;
				else if (idx2 >= len2)
					return 1;
				
				char c1 = seq1.charAt(idx1);
				char c2 = seq2.charAt(idx2);
				
				if (!Character.isDigit(c1) || !Character.isDigit(c2))
				{
					int txtEnd1 = idx1 + 1;
					int txtEnd2 = idx2 + 1;

					while (txtEnd1 < len1 && txtEnd2 < len2 && !(Character.isDigit(seq1.charAt(txtEnd1)) && Character.isDigit(seq2.charAt(txtEnd2)))) {
						txtEnd1++;
						txtEnd2++;
					}

					String txt1 = seq1.subSequence(idx1, txtEnd1).toString();
					String txt2 = seq2.subSequence(idx2, txtEnd2).toString();

					int res = collator.compare(txt1, txt2);
					if (res != 0)
						return res;

					idx1 = txtEnd1;
					idx2 = txtEnd2;
				}
				else
				{
					int digitEnd1 = idx1 + 1;
					while (digitEnd1 < len1 && Character.isDigit(seq1.charAt(digitEnd1)))
						digitEnd1++;
					long number1 = Long.parseLong(seq1, idx1, digitEnd1, 10);
					
					int digitEnd2 = idx2 + 1;
					while (digitEnd2 < len2 && Character.isDigit(seq2.charAt(digitEnd2)))
						digitEnd2++;
					long number2 = Long.parseLong(seq2, idx2, digitEnd2, 10);
					
					if (number1 != number2)
						return (number1 < number2) ? -1 : 1;
					
					idx1 = digitEnd1;
					idx2 = digitEnd2;
				}
			}
		}
	}
	
	public static void closeQuietly(Closeable closeable)
	{
		if (closeable != null)
		{
			try
			{
				closeable.close();
			}
			catch (IOException e)
			{
			}
		}
	}
	
	public static boolean isBlank(String s)
	{
		return s == null || s.isBlank();
	}
	
	public static boolean isNotBlank(String s)
	{
		return !isBlank(s);
	}

	public static String stripAccents(String input)
	{
		return Normalizer.normalize(input, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}", "");
	}
	
	/**
	 * Return the first non null argument or null if none.
	 * 
	 * @param <T>
	 * @param args
	 * @return the first non null argument
	 */
	public static <T> T coalesce(@SuppressWarnings("unchecked") T... args)
	{
		if (args == null)
			return null;
		
		for (T arg : args)
			if (arg != null)
				return arg;
		
		return null;
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T cast(Object o)
	{
		return (T) o;
	}
	
	/**
	 * Move/rename a file or directory. If the source is a directory and the target
	 * already exist, merge them.
	 */
	public static void move(Path source, Path target, CopyOption... fileMoveOptions) throws IOException
	{
		if (!Files.isDirectory(source) || !Files.exists(target))
		{
			Files.move(source, target, fileMoveOptions);
			return;
		}
		
		Files.walkFileTree(source, new FileVisitor<Path>()
		{
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
			{
				Path dirTarget = target.resolve(source.relativize(dir));
				
				if (!Files.exists(dirTarget))
				{
					Files.move(dir, dirTarget, fileMoveOptions);
					return FileVisitResult.SKIP_SUBTREE;
				}
				
				return FileVisitResult.CONTINUE;
			}
			
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
			{
				Path fileTarget = target.resolve(source.relativize(file));
				Files.move(file, fileTarget, fileMoveOptions);
				
				return FileVisitResult.CONTINUE;
			}
			
			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException
			{
				exc.printStackTrace();
				return FileVisitResult.CONTINUE;
			}
			
			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException
			{
				System.out.println(dir);
				if (Files.list(dir).findAny().isEmpty())
					Files.delete(dir);
				
				return FileVisitResult.CONTINUE;
			}
		});
	}
	
	
	public static RuntimeException asRunTimeException(Exception e)
	{
		Objects.requireNonNull(e);
		if (e instanceof RuntimeException re)
			return re;
		else if (e instanceof IOException ioe)
			return new UncheckedIOException(ioe);
		else
			return new RuntimeException(e);
	}
	
	public static <T> CompletableFuture<T> observe(CompletableFuture<T> cf, BiConsumer<? super T, Throwable> action)
	{
		Objects.requireNonNull(action, "action cannot be null");
		return cf.handle((result, error) ->
		{
			action.accept(result, error);
			if (error != null)
				return CompletableFuture.<T>failedFuture(error);
			else
				return CompletableFuture.completedFuture(result);
		}).thenCompose(f -> f);
	}
	
	public static String getExtention(String filename)
	{
		if (isBlank(filename))
			return null;
		
		int posExt = filename.lastIndexOf('.');
		if (posExt <= 0) // or equals because if the filename start with a point, then it's a hidden file.
			return null;
		
		return filename.substring(posExt);
	}
	
	public static String getExtention(Path path)
	{
		if (path == null)
			return null;
		
		Path filename = path.getFileName();
		if (filename == null)
			return null;
		
		return getExtention(filename.toString());
	}
}
