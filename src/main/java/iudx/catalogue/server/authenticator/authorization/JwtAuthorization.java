package iudx.catalogue.server.authenticator.authorization;


public class JwtAuthorization {

  private final AuthorizationStratergy authStrategy;

  public JwtAuthorization(final AuthorizationStratergy authStrategy) {
    this.authStrategy = authStrategy;
  }

  public boolean isAuthorized(AuthorizationRequest authRequest) {
    return authStrategy.isAuthorized(authRequest);
  }
}
