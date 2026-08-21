package com.google.api.client.http.javanet;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.LowLevelHttpRequest;
import com.google.api.client.http.LowLevelHttpResponse;
import java.io.IOException;

/**
 * 응답 gzip 압축을 요청하지 않도록 강제하는 HttpTransport.
 *
 * <p>일부 로컬/네트워크 환경에서 gzip 응답 스트림이 깨져
 * "Not in GZIP format" 예외로 FCM 발송이 실패로 오인되는 것을 우회한다.
 * 요청에서 {@code Accept-Encoding: gzip} 과 User-Agent 의 {@code (gzip)} 표기를 제거하면
 * 구글 서버가 평문으로 응답하므로 압축 해제 단계 자체가 사라진다.
 *
 * <p>{@link NetHttpTransport} 는 {@code final} 이고 {@code buildRequest} 가 protected 라
 * 상속/외부호출이 불가능하다. 그래서 이 클래스만 예외적으로 google-http-client 와 같은
 * 패키지({@code com.google.api.client.http.javanet})에 두어 protected 메서드에 접근한다.
 */
public class GzipDisablingHttpTransport extends HttpTransport {

    private final NetHttpTransport delegate = new NetHttpTransport();

    @Override
    public boolean supportsMethod(String method) throws IOException {
        return delegate.supportsMethod(method);
    }

    @Override
    protected LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
        // 같은 패키지라 protected buildRequest 접근 가능
        LowLevelHttpRequest real = delegate.buildRequest(method, url);
        return new LowLevelHttpRequest() {
            @Override
            public void addHeader(String name, String value) throws IOException {
                if ("Accept-Encoding".equalsIgnoreCase(name)) {
                    return; // gzip 응답 요청 제거
                }
                if ("User-Agent".equalsIgnoreCase(name) && value != null) {
                    value = value.replace("(gzip)", "").replaceAll("\\s+", " ").trim();
                }
                real.addHeader(name, value);
            }

            @Override
            public void setTimeout(int connectTimeout, int readTimeout) throws IOException {
                real.setTimeout(connectTimeout, readTimeout);
            }

            @Override
            public void setWriteTimeout(int writeTimeout) throws IOException {
                real.setWriteTimeout(writeTimeout);
            }

            @Override
            public LowLevelHttpResponse execute() throws IOException {
                // content setter 는 base 에서 final 이라, 실제 요청 객체로 여기서 전달한다.
                if (getContentType() != null) {
                    real.setContentType(getContentType());
                }
                if (getContentEncoding() != null) {
                    real.setContentEncoding(getContentEncoding());
                }
                real.setContentLength(getContentLength());
                if (getStreamingContent() != null) {
                    real.setStreamingContent(getStreamingContent());
                }
                return real.execute();
            }
        };
    }
}
