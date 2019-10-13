package com.island.androidsftpdocumentsprovider;
import java.util.logging.*;
public class Log
{
	private static final String LOG_TAG="SFTPDocumentProvider";
	public static final Logger logger=Logger.getLogger(LOG_TAG);
	static
	{
		if(BuildConfig.DEBUG)
		{
			AndroidLoggingHandler.reset(new AndroidLoggingHandler());
			logger.setLevel(Level.ALL);
		}
	}
	public static void e(String log,Throwable t)
	{
		logger.log(Level.SEVERE,log,t);
	}
	public static void i(String log)
	{
		logger.info(log);
	}
	public static void d(String log)
	{
		logger.fine(log);
	}
}
