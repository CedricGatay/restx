package restx.specs.server;

import com.google.common.base.Optional;
import restx.*;
import restx.factory.Component;
import restx.http.HttpStatus;
import restx.specs.RestxSpec;
import restx.specs.RestxSpecRepository;
import restx.specs.When;
import restx.specs.WhenHttpRequest;

import java.io.IOException;
import java.util.Iterator;

/**
 * User: xavierhanin
 * Date: 4/10/13
 * Time: 12:04 PM
 */
@Component(priority = 1000)
public class SpecsServerRoute implements RestxRoute, RestxHandler {
    private final RestxSpecRepository specRepository;

    public SpecsServerRoute(RestxSpecRepository specRepository) {
        this.specRepository = specRepository;
    }

    @Override
    public Optional<? extends RestxRouteMatch> match(RestxRequest req) {
        Iterable<WhenHttpRequest> spec = specRepository.findSpecsByRequest(req);
        Iterator<WhenHttpRequest> iterator = spec.iterator();
        return iterator.hasNext()
                ? Optional.<StdRestxRouteMatch>of(new Match(req, iterator.next()))
                : Optional.<StdRestxRouteMatch>absent();
    }

    @Override
    public void handle(RestxRouteMatch match, RestxRequest req, RestxResponse resp, RestxContext ctx) throws IOException {
        Match m = (Match) match;
        resp.setStatus(HttpStatus.havingCode(m.spec.getThen().getExpectedCode()));
        if (m.spec.getThen().getExpectedCode() == HttpStatus.OK.getCode()) {
            resp.setContentType("application/json");
        }
        resp.getWriter().print(m.spec.getThen().getExpected());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("-- routes defined by specs:\n");
        for (String spec : specRepository.findAll()){
            Optional<RestxSpec> s = specRepository.findSpecById(spec);
            for (When when : s.get().getWhens()) {
                if (when instanceof WhenHttpRequest) {
                    WhenHttpRequest httpRequest = (WhenHttpRequest) when;
                    sb.append(httpRequest.getMethod()).append(" ").append(httpRequest.getPath())
                            .append(" (").append(spec).append(")\n");
                }
            }
        }
        sb.append("--");

        return sb.toString();
    }

    private class Match extends StdRestxRouteMatch {
        private final WhenHttpRequest spec;

        public Match(RestxRequest req, WhenHttpRequest spec) {
            super(SpecsServerRoute.this, req.getRestxPath());
            this.spec = spec;
        }
    }
}
