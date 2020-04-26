package ca.ubc.msl.polyst.exception;

public class CacheWarmingException extends RuntimeException {

    public CacheWarmingException( String message ) {
        super( message );
    }

    public CacheWarmingException( Throwable cause ) {
        super( cause );
    }

    public CacheWarmingException( String message, Throwable cause ) {
        super( message, cause );
    }
}