package com.island.androidftpdocumentprovider;
import android.content.*;
import android.database.*;
import android.os.*;
import android.provider.*;
import android.provider.DocumentsContract.*;
import android.webkit.*;
import java.io.*;
import com.enterprisedt.net.ftp.*;
import android.util.*;
import java.util.*;
import java.text.*;
public class FTPProvider extends DocumentsProvider
{
	private static final String[] DEFAULT_ROOT_PROJECTION =
	new String[]{Root.COLUMN_ROOT_ID,
        Root.COLUMN_FLAGS, Root.COLUMN_ICON, Root.COLUMN_TITLE,
        //Root.COLUMN_SUMMARY, 
		Root.COLUMN_DOCUMENT_ID};
	private static final String[] DEFAULT_DOCUMENT_PROJECTION = new
	String[]{Document.COLUMN_DOCUMENT_ID,Document.COLUMN_SIZE,
        Document.COLUMN_DISPLAY_NAME,Document.COLUMN_LAST_MODIFIED,Document.COLUMN_MIME_TYPE,
        Document.COLUMN_FLAGS};
	String[]connections={"127.0.0.1:8888"};
	@Override
	public boolean onCreate()
	{
		/*StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().build());
		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().build());
		SharedPreferences preferences=getContext().getSharedPreferences(MainActivity.CONFIG,0);
		try
		{
			client=new FTPClient(preferences.getString(MainActivity.PORT,"127.0.0.1"),preferences.getInt(MainActivity.PORT,8888));
			client.login("anonymous","anonymous@anonymous.com");
			Log.i(MainActivity.LOG_TAG,client.toString());
			return true;
		}
		catch(FTPException e)
		{
			Log.e(MainActivity.LOG_TAG,"Query child exception",e);
		}
		catch(IOException e)
		{
			Log.e(MainActivity.LOG_TAG,"Query child exception",e);
		}*/
		return true;
	}
	@Override
	public Cursor queryRoots(String[]projection)throws FileNotFoundException
	{
		Log.i(MainActivity.LOG_TAG,"Query Root: Projection="+Arrays.toString(projection));
		MatrixCursor result=new MatrixCursor(projection!=null?projection:DEFAULT_ROOT_PROJECTION);
		for(String connection:connections)
		{
			MatrixCursor.RowBuilder row=result.newRow();
			row.add(Root.COLUMN_ROOT_ID,connection);
			row.add(Root.COLUMN_DOCUMENT_ID,connection+"/");
			//row.add(Root.COLUMN_SUMMARY, "ftp provider test");
			row.add(Root.COLUMN_ICON, R.drawable.ic_launcher);
			row.add(Root.COLUMN_FLAGS, Root.FLAG_SUPPORTS_CREATE);
			row.add(Root.COLUMN_TITLE,connection);
		}
		return result;
	}
	@Override
	public Cursor queryDocument(String documentId,String[]projection)throws FileNotFoundException
	{
		Log.i(MainActivity.LOG_TAG,"Query Document: DocumentId="+documentId+" Projection="+Arrays.toString(projection));
		MatrixCursor result=new MatrixCursor(projection!=null?projection:DEFAULT_DOCUMENT_PROJECTION);
		if(documentId.equals("children"))return result;
		try
		{
			getFileInfo(documentId,"drwx------   0 anonymous anonymous            0 Aug 11 03:11 ",new Date(),result.newRow());
			//return queryChildDocuments(documentId,projection,(String)null);
		}
		catch(Exception e)
		{
			String msg="Error querying child "+getPath(documentId);
			Log.e(MainActivity.LOG_TAG,msg,e);
			throw new FileNotFoundException(msg);
		}
		return result;
	}
	@Override
	public Cursor queryChildDocuments(String parentDocumentId,String[]projection,String sortOrder)throws FileNotFoundException
	{
		Log.i(MainActivity.LOG_TAG,"Query Child Documents: ParentDocumentId="+parentDocumentId+" Projection="+projection+" SortOrder="+sortOrder);
		MatrixCursor result=new MatrixCursor(projection!=null?projection:DEFAULT_DOCUMENT_PROJECTION);
		try
		{
			if(parentDocumentId.charAt(parentDocumentId.length()-1)!='/')parentDocumentId+="/";
			String[]protocolParameters=getRoot(parentDocumentId);
			FTPClient client=new FTPClient(protocolParameters[0],Integer.valueOf(protocolParameters[1]));
			client.login("anonymous","anonymous");
			String path=getPath(parentDocumentId);
			String[]filesDescription=client.dir(path,true);
			String[]files=client.dir(path);
			for(int a=0;a<filesDescription.length;a++)
			{
				getFileInfo(parentDocumentId+files[a],filesDescription[a],client.modtime(path+files[a]),result.newRow());
			}
		}
		catch(Exception e)
		{
			String msg="Error querying childs of "+getPath(parentDocumentId);
			Log.e(MainActivity.LOG_TAG,msg,e);
			throw new FileNotFoundException(msg);
		}
		return result;
	}
	@Override
	public ParcelFileDescriptor openDocument(final String documentId,String mode,CancellationSignal signal)throws FileNotFoundException
	{
		Log.i(MainActivity.LOG_TAG,"Open Document: DocumentId="+documentId+" mode="+mode+" signal="+signal);
		final File file=new File(getContext().getExternalCacheDir(),"tmp");
		int accessMode=ParcelFileDescriptor.parseMode(mode);
		boolean isWrite=(mode.indexOf('w')!=-1);
		String[]connectionParameter=getRoot(documentId);
		try
		{
			final FTPClient client=new FTPClient(connectionParameter[0],Integer.valueOf(connectionParameter[1]));
			client.login("anonymous","anonymous");
			client.get(file.getPath(),getPath(documentId));
			if(isWrite)
			{
				return ParcelFileDescriptor.open(file,accessMode,new Handler(getContext().getMainLooper()),new ParcelFileDescriptor.OnCloseListener()
					{
						@Override
						public void onClose(IOException exception)
						{
							try
							{
								client.put(file.getPath(),getPath(documentId));
							}
							catch(Exception e)
							{
								Log.e(MainActivity.LOG_TAG,"Error opening document "+getPath(documentId),e);
							}
						}
					});
			}
			else
			{
				return ParcelFileDescriptor.open(file,accessMode);
			}
		}
		catch(Exception e)
		{
			String msg="Error opening document "+getPath(documentId);
			Log.e(MainActivity.LOG_TAG,msg,e);
			throw new FileNotFoundException(msg);
		}
	}
	private static String[]getRoot(String documentId)
	{
		return documentId.substring(0,documentId.indexOf("/")).split(":");
	}
	private static String getPath(String documentId)
	{
		return documentId.substring(documentId.indexOf("/")+1);
	}
	private void getFileInfo(String documentId,String fileDetails,Date date,MatrixCursor.RowBuilder row)
	{
		int flags;
		String mime;
		Scanner scanner=new Scanner(fileDetails);
		String permissions=scanner.next();
		scanner.next();
		scanner.next();
		scanner.next();
		long size=scanner.nextLong();
		scanner.next();
		scanner.next();
		scanner.next();
		String file=scanner.nextLine();
		if(permissions.charAt(0)=='d')
		{
			Log.i(MainActivity.LOG_TAG,"directory found");
			flags=Document.FLAG_DIR_SUPPORTS_CREATE;
			mime=Document.MIME_TYPE_DIR;
			if(file.equals(" "))file="";
		}
		else
		{
			flags=Document.FLAG_SUPPORTS_WRITE;
			mime=getTypeForName(file);
			row.add(Document.COLUMN_SIZE,size);
		}
		flags|=Document.FLAG_SUPPORTS_COPY|Document.FLAG_SUPPORTS_DELETE|Document.FLAG_SUPPORTS_MOVE|Document.FLAG_SUPPORTS_REMOVE|Document.FLAG_SUPPORTS_RENAME;
		row.add(Document.COLUMN_FLAGS,flags);
		row.add(Document.COLUMN_MIME_TYPE,mime);
		row.add(Document.COLUMN_DISPLAY_NAME,file);
		row.add(Document.COLUMN_DOCUMENT_ID,documentId);
		row.add(Document.COLUMN_LAST_MODIFIED,date.getTime());
	}
	private static String getTypeForName(String name) {
        final int lastDot = name.lastIndexOf('.');
        if (lastDot >= 0) {
            final String extension = name.substring(lastDot + 1);
            final String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            if (mime != null) {
                return mime;
            }
        }
        return "application/octet-stream";
    }
}
