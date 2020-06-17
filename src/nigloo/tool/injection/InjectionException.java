package nigloo.tool.injection;

public class InjectionException extends RuntimeException {
	private static final long serialVersionUID = 4709852420700639755L;

	public InjectionException() {
	}

	public InjectionException(String message) {
		super(message);
	}

	public InjectionException(Throwable cause) {
		super(cause);
	}

	public InjectionException(String message, Throwable cause) {
		super(message, cause);
	}
}
