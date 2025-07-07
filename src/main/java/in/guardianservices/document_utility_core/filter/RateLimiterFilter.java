package in.guardianservices.document_utility_core.filter;

import io.github.bucket4j.*;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Component
public class RateLimiterFilter implements Filter {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    private Bandwidth getBandwidthForEndpoint(String endpoint) {
        return switch (endpoint) {
            case "/pdf/unlock" -> Bandwidth.classic(10, Refill.greedy(10, Duration.ofMinutes(1)));
            case "/pdf/lock" -> Bandwidth.classic(20, Refill.greedy(20, Duration.ofMinutes(1)));
            case "/pdf/to-doc" -> Bandwidth.classic(30, Refill.greedy(30, Duration.ofMinutes(1)));
            case "/doc/to-pdf" -> Bandwidth.classic(50, Refill.greedy(50, Duration.ofMinutes(1)));
            default -> Bandwidth.classic(100, Refill.greedy(100, Duration.ofMinutes(1)));
        };
    }

    private Bucket resolveBucket(String key, String uri) {
        return cache.computeIfAbsent(key, k -> Bucket4j.builder()
                .addLimit(getBandwidthForEndpoint(uri))
                .build());
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        String ip = req.getRemoteAddr();
        String uri = req.getRequestURI();
        String key = ip + ":" + uri;

        Bucket bucket = resolveBucket(key, uri);
        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            HttpServletResponse res = (HttpServletResponse) response;
            res.setStatus(429); // Too Many Requests
            res.getWriter().write("Too many requests to " + uri + ". Please try again later.");
        }
    }
}
