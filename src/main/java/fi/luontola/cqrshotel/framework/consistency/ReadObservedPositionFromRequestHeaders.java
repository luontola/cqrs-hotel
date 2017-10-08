// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework.consistency;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class ReadObservedPositionFromRequestHeaders extends OncePerRequestFilter {

    private final ObservedPosition observedPosition;

    public ReadObservedPositionFromRequestHeaders(ObservedPosition observedPosition) {
        this.observedPosition = observedPosition;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            observedPosition.reset();
            String value = request.getHeader(ObservedPosition.HTTP_HEADER);
            if (value != null) {
                observedPosition.observe(Long.parseLong(value));
            }
            filterChain.doFilter(request, response);
        } finally {
            observedPosition.reset();
        }
    }
}
