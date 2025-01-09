package iudx.catalogue.server.authenticator.authorization;

public interface AuthorizationStratergy {

  boolean isAuthorized(AuthorizationRequest authRequest);
}
