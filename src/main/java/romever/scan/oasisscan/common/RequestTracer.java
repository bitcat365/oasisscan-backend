package romever.scan.oasisscan.common;


import com.google.common.hash.Hashing;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.function.Supplier;

public class RequestTracer extends OncePerRequestFilter {

	public static final String USR_ID_KEY = "USR_ID";
	private static final String PREV_THREAD_NAME = RequestTracer.class.getName() + ".PREV_THREAD_NAME";
	private static final ThreadLocal<String> REQ_ID_THREADLOCAL = new ThreadLocal<>();
	private static final String REQ_ID_KEY = "REQ_ID";
	private static final String REQ_STATE_KEY = "REQ_STATE";
	private static final String OK_VAL = "ok";
	private static final String ERR_VAL = "err";


	public RequestTracer( ) {
 	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String requestId = random(32);
		request.setAttribute(REQ_ID_KEY, requestId);
		request.setAttribute(REQ_STATE_KEY, OK_VAL);
		request.setAttribute(USR_ID_KEY, "@");
		request.setAttribute(PREV_THREAD_NAME, Thread.currentThread().getName());
		response.addHeader("REQID", requestId);
		Thread.currentThread().setName(String.format("%s:%s", requestId, request.getRequestURI()));


		try {
			filterChain.doFilter(request, response);
		} finally {

			Thread.currentThread().setName((String) request.getAttribute(PREV_THREAD_NAME));
		}
	}

	public static void setState(int status) {
		RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
		if (attributes != null)
			attributes.setAttribute(REQ_STATE_KEY, status == 0 ? OK_VAL : ERR_VAL + ":" + status,
					RequestAttributes.SCOPE_REQUEST);
	}

	public static String getRequestId() {
		RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
		if (attributes != null)
			return (String) attributes.getAttribute(REQ_ID_KEY, RequestAttributes.SCOPE_REQUEST);
		return REQ_ID_THREADLOCAL.get();
	}

	public static <T> T substituteThreadName(Supplier<T> supplier, String threadName, String requestId) {
		String name = Thread.currentThread().getName();
		Thread.currentThread().setName(threadName);
		REQ_ID_THREADLOCAL.set(requestId);
		try {
			return supplier.get();
		} finally {
			Thread.currentThread().setName(name);
			REQ_ID_THREADLOCAL.remove();
		}
	}

	public static void substituteThreadName(Runnable runnable, String threadName, String requestId) {
		String name = Thread.currentThread().getName();
		Thread.currentThread().setName(threadName);
		REQ_ID_THREADLOCAL.set(requestId);
		try {
			runnable.run();
		} finally {
			Thread.currentThread().setName(name);
			REQ_ID_THREADLOCAL.remove();
		}
	}

	public static final SecureRandom RANDOM = new SecureRandom();

	private static String random(int bits) {
		byte[] bytes = new byte[bits];
		RANDOM.nextBytes(bytes);
		return Hashing.goodFastHash(bits).hashBytes(bytes).toString();
	}

}
