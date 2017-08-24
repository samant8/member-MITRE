package org.mitre.tangerine.exception;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * Created on Jun 18, 2008
 *
 * Copyright:   (c) 2006 The MITRE Corporation All Rights Reserved
 * 
 *              This software was produced for the U.S. Government under Contract
 *              No. DAAB07-01-C-C201, and is subject to the Rights in Noncommercial
 *              Computer Software and Noncommercial Computer Software Documentation
 *              clause at DFARS 252.227-7014 (JUNE 1995)
 * 
 *              This work was done under DoD Contract Number DAAB07-00-C-C201 and
 *              MITRE Number DAAB070C0E601"
 *              
 * 				MITRE IS PROVIDING THE PRODUCT "AS IS" AND MAKES NO WARRANTY, EXPRESS
 * 				OR IMPLIED, AS TO THE ACCURACY, CAPABILITY, EFFICIENCY,
 * 				MERCHANTABILITY, OR FUNCTIONING OF THIS SOFTWARE AND DOCUMENTATION. IN
 * 				NO EVENT WIMETExceptionLL MITRE BE LIABLE FOR ANY GENERAL, CONSEQUENTIAL,
 * 				INDIRECT, INCIDENTAL, EXEMPLARY OR SPECIAL DAMAGES, EVEN IF MITRE HAS
 * 				BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.     
 * 
 * Revision History:
 * 
 * Version Person       Date        Reason
 * ------- ------       ----        ------
 * 1.0     JJ        	Jun 18  Initial Version
 *
 */

/**
 * @version 1.0.0
 * @author Joe Jubinski
 * date: Sep 17, 2006
 * 
 */
public class AETException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private int reasonNumber;
	private Date date;
	private String occuredAt;
	private String reason;
	private String callerClass;
	private String callerMethod;
	private Level eventType;
	private ResourceBundle resources;
	private String logFileName;
	private Logger logger;
	private FileHandler handler;

	private class DumpClass extends SecurityManager implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		////////////////////////////////////////////
		public void getCallersClass() {
			Class<?>[] stack = getClassContext();
			callerClass = new String(stack[2].toString());
		}
	}

	private DumpClass dC;

	private class DumpMethod implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		////////////////////////////////////////////
		public void getCallersMethod() {
			Throwable t = new Throwable();
			StackTraceElement elements[] = t.getStackTrace();
			callerMethod = new String(elements[2].getMethodName());
		}
	}

	private DumpMethod dM;

	/**
     * package Exception.
     *
     * @param eventType Type of Error (i.e. 1=Pure Event, 2=Information, 3=Warning, 4=Error, 5=Fatal)
     * @param reason Error String.
     * @param reasonNumber The Error Number.
     */
    ////////////////////////////////////////////
    public AETException(Level eventType, String reason, int reasonNumber) {// constructor
        super();

        this.dC = new DumpClass();
        this.dC.getCallersClass();
        this.dM = new DumpMethod();
        this.dM.getCallersMethod();
        this.reasonNumber = reasonNumber;
        this.date = new Date();
        this.occuredAt = new String(this.date.toString());
        this.resources = ResourceBundle.getBundle(this.getClass().getSimpleName(), Locale.getDefault());
        this.eventType = eventType;
        //log setup
        Calendar cal = Calendar.getInstance();
        this.logFileName = new String(System.getProperty("user.name") + "-" + Integer.toString(cal.get(Calendar.MONTH)) + Integer.toString(cal.get(Calendar.DAY_OF_MONTH)) + Integer.toString(cal.get(Calendar.YEAR)) + ".log");
        this.logger = Logger.getLogger("AdapterException");
        this.reason = reason;
        
        try {
            this.handler = new FileHandler(System.getProperty("java.io.tmpdir") + File.separator + this.logFileName, true);
            this.logger.addHandler(this.handler); // Add Logging to A file.
            this.logger.setUseParentHandlers(false); // Turn off Logging to the console/stderr.
            
             logger.logp(eventType, this.callerClass, this.callerMethod, this.reason);

        }catch (IOException e) {
            e.printStackTrace();
            System.exit(199);// Use the highest exit number in Unix/Linux
        }
    }

	/**
	 * <code>getMessage</code> gets the fully composed exception string message.
	 * 
	 * <pre>
	 *      example:
	 *          ['ReasonNumber'] 'Event Type' 'reason' Time: ['Time Stamp'] Method: ['thowing method']
	 * </pre>
	 * 
	 * @return Exception String message.
	 */
	////////////////////////////////////////////
	public String getMessage() {
		MessageFormat formatter = new MessageFormat("{0}: {1} Time: [{2}] Method: [{3}] Class: [{4}]");
		Object[] args = { this.eventType, new String(this.reason), this.occuredAt, this.callerMethod, this.callerClass };
		formatter.setLocale(this.resources.getLocale());
		String message = formatter.format(args);
		return message;
	}

	////////////////////////////////////////////
	public String getCallerClass() {
		return callerClass;
	}

	////////////////////////////////////////////
	public Date getDate() {
		return date;
	}

	////////////////////////////////////////////
	public Level getEventType() {
		return eventType;
	}

	////////////////////////////////////////////
	public String getOccuredAt() {
		return occuredAt;
	}

	////////////////////////////////////////////
	public String getReason() {
		return reason;
	}

	////////////////////////////////////////////
	public int getReasonNumber() {
		return reasonNumber;
	}

	////////////////////////////////////////////
	public String getCallerMethod() {
		return callerMethod;
	}
}
