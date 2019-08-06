package com.island.androidftpdocumentprovider;
import android.content.*;
import android.database.*;
import android.os.*;
import android.provider.*;
import android.provider.DocumentsContract.*;
import android.webkit.*;
import java.io.*;
import org.apache.commons.net.ftp.*;
public class FtpProvider extends DocumentsProvider
{
	private static final String[] DEFAULT_ROOT_PROJECTION =
	new String[]{Root.COLUMN_ROOT_ID, Root.COLUMN_MIME_TYPES,
        Root.COLUMN_FLAGS, Root.COLUMN_ICON, Root.COLUMN_TITLE,
        Root.COLUMN_SUMMARY, Root.COLUMN_DOCUMENT_ID,
        Root.COLUMN_AVAILABLE_BYTES,};
	private static final String[] DEFAULT_DOCUMENT_PROJECTION = new
	String[]{Document.COLUMN_DOCUMENT_ID, Document.COLUMN_MIME_TYPE,
        Document.COLUMN_DISPLAY_NAME, Document.COLUMN_LAST_MODIFIED,
        Document.COLUMN_FLAGS, Document.COLUMN_SIZE,};
	private final FTPClient client=new FTPClient();
	@Override
	public boolean onCreate()
	{
		SharedPreferences preferences=getContext().getSharedPreferences(MainActivity.CONFIG,0);
		try
		{
			client.connect(preferences.getString(MainActivity.PORT,null),preferences.getInt(MainActivity.PORT,0));
			return true;
		}
		catch(IOException e)
		{
			throw new RuntimeException(e);
		}
	}
	@Override
	public Cursor queryRoots(String[]projection)throws FileNotFoundException
	{
		MatrixCursor result=new MatrixCursor(projection!=null?projection:DEFAULT_ROOT_PROJECTION);
		result.addRow(resolveRootProjection(projection));
		return result;
	}
	@Override
	public Cursor queryDocument(String documentId,String[]projection) throws FileNotFoundException
	{
		MatrixCursor result=new MatrixCursor(projection!=null?projection:DEFAULT_ROOT_PROJECTION);
		String[][]resolved=resolveProjection(documentId,projection);
		for(String[]resolve:resolved)result.addRow(resolve);
		return result;
	}
	@Override
	public Cursor queryChildDocuments(String p1,String[] p2,String p3) throws FileNotFoundException
	{
		// TODO: Implement this method
		return null;
	}
	@Override
	public ParcelFileDescriptor openDocument(String p1,String p2,CancellationSignal p3) throws FileNotFoundException
	{
		// TODO: Implement this method
		return null;
	}
	public String[][]resolveProjection(String document,String[]projection)
	{
		try
		{
			FTPFile[]files=client.listDirectories(document);
			String[][]resolved=new String[files.length][projection.length];
			for(int b=0;b<files.length;b++)
			{
				FTPFile file=files[b];
				for(int a=0;a<projection.length;a++)
				{
					switch(projection[a])
					{
						case DocumentsContract.Document.COLUMN_DISPLAY_NAME:
							resolved[b][a]=file.getName();
							break;
						case DocumentsContract.Document.COLUMN_DOCUMENT_ID:
							resolved[b][a]=document+file.getName();
							if(file.isDirectory())resolved[b][a]+="/";
							break;
						case DocumentsContract.Document.COLUMN_FLAGS:
							resolved[b][a]=String.valueOf(Document.FLAG_DIR_SUPPORTS_CREATE|Document.FLAG_SUPPORTS_COPY|Document.FLAG_SUPPORTS_DELETE|Document.FLAG_SUPPORTS_DELETE|Document.FLAG_SUPPORTS_MOVE|Document.FLAG_SUPPORTS_REMOVE|Document.FLAG_SUPPORTS_RENAME|Document.FLAG_SUPPORTS_WRITE);
							break;
						case DocumentsContract.Document.COLUMN_ICON:
							resolved[b][a]=null;
							break;
						case DocumentsContract.Document.COLUMN_LAST_MODIFIED:
							resolved[b][a]=null;
							break;
						case DocumentsContract.Document.COLUMN_MIME_TYPE:
							resolved[b][a]=MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.getName().substring(file.getName().lastIndexOf(".")));
							break;
						case DocumentsContract.Document.COLUMN_SIZE:
							resolved[b][a]=String.valueOf(file.getSize());
							break;
						case DocumentsContract.Document.COLUMN_SUMMARY:
							resolved[b][a]=null;
							break;
					}
				}
			}
			return resolved;
		}
		catch(IOException e)
		{
			throw new RuntimeException(e);
		}
	}
	public String[]resolveRootProjection(String[]projection)
	{
		String[]resolved=new String[projection.length];
		for(int a=0;a<projection.length;a++)
		{
			switch(projection[a])
			{
				case DocumentsContract.Root.COLUMN_AVAILABLE_BYTES:
					resolved[a]=null;
					break;
				case DocumentsContract.Root.COLUMN_CAPACITY_BYTES:
					resolved[a]=null;
					break;
				case DocumentsContract.Root.COLUMN_DOCUMENT_ID:
					resolved[a]="";
					break;
				case DocumentsContract.Root.COLUMN_FLAGS:
					resolved[a]=String.valueOf(DocumentsContract.Root.FLAG_SUPPORTS_CREATE);
					break;
				case DocumentsContract.Root.COLUMN_ICON:
					resolved[a]=String.valueOf(R.mipmap.ic_launcher);
					break;
				case DocumentsContract.Root.COLUMN_MIME_TYPES:
					resolved[a]=null;
					break;
				case DocumentsContract.Root.COLUMN_ROOT_ID:
					resolved[a]="Ftp document provider";
					break;
				case DocumentsContract.Root.COLUMN_SUMMARY:
					resolved[a]=null;
					break;
				case DocumentsContract.Root.COLUMN_TITLE:
					resolved[a]="FTP";
			}
		}
		return resolved;
	}
}
