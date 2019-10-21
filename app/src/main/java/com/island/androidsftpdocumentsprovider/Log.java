package com.island.androidsftpdocumentsprovider;
import java.util.logging.*;
public class Log
{
	private static final String LOG_TAG="SFTPDocumentProvider";
	public static final Logger logger=Logger.getLogger(LOG_TAG);
	/**
	 * Fix the android logger handler
	 */
	static
	{
		if(BuildConfig.DEBUG)
		{
			AndroidLoggingHandler.reset(new AndroidLoggingHandler());
			logger.setLevel(Level.ALL);
		}
	}
	/**
	 * Log an exception on the default logger
	 * @param log The log message
	 * @param t The throwable to log
	 */
	static void e(String log,Throwable t)
	{
		logger.log(Level.SEVERE,log,t);
	}
	/**
	 * Log an info on the default logger
	 * @param log The log message
	 */
	static void i(String log)
	{
		logger.info(log);
	}
	/**
	 * Log a debug info on the default logger
	 * @param log The log message
	 */
	static void d(String log)
	{
		logger.fine(log);
	}
}
