package com.island.logging;
import android.util.Log;
import java.util.logging.*;
public class Log extends Handler
{
	private static Logger logger;
	private Log(){}
    static
	{
        Logger rootLogger=LogManager.getLogManager().getLogger("");
        Handler[]handlers=rootLogger.getHandlers();
        for(Handler handler:handlers)
		{
            rootLogger.removeHandler(handler);
        }
        rootLogger.addHandler(new com.island.logging.Log());
    }
    @Override
    public void close()
	{}
    @Override
    public void flush()
	{}
    @Override
    public void publish(LogRecord record)
	{
        if(!super.isLoggable(record))return;
		String name=record.getLoggerName();
        int maxLength=30;
        String tag=name.length()>maxLength?name.substring(name.length()-maxLength):name;
        try
		{
            int level=getAndroidLevel(record.getLevel());
            Log.println(level,tag,record.getMessage());
            if(record.getThrown()!=null)
			{
                Log.println(level,tag,Log.getStackTraceString(record.getThrown()));
            }
        }
		catch(RuntimeException e)
		{
            Log.e("AndroidLoggingHandler","Error logging message.",e);
        }
    }
	private static int getAndroidLevel(Level level)
	{
        int value=level.intValue();
        if(value>=Level.SEVERE.intValue())
		{
            return Log.ERROR;
        }
		else if(value>=Level.WARNING.intValue())
		{
            return Log.WARN;
        }
		else if(value>=Level.INFO.intValue())
		{
            return Log.INFO;
        }
		else
		{
            return Log.DEBUG;
        }
    }
	public static void setTag(String tag)
	{
		logger=Logger.getLogger(tag);
	}
	public static void e(String log,Throwable t)
	{
		logger.log(Level.SEVERE,log,t);
	}
	public static void w(String log)
	{
		logger.warning(log);
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

