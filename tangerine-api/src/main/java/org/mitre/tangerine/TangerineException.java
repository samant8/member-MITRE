package org.mitre.tangerine;

public class TangerineException extends Exception {

    /**
     * A basic exception to allow for reporting Exceptions that are distinctly Tangerine or Analysis Exchange in nature.
     */
    private static final long serialVersionUID = 1L;
    public TangerineException(String msg){
        super(msg);
    }
    public TangerineException(String msg, Throwable cause){
        super(msg, cause);
    }
}
