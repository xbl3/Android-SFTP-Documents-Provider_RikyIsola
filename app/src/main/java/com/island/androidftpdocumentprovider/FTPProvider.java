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
	private static final String DEBUG_PASSWORD="anonymous";
	private static final String[]DEFAULT_ROOT_PROJECTION=
	{Root.COLUMN_ROOT_ID,Root.COLUMN_FLAGS,Root.COLUMN_ICON,Root.COLUMN_TITLE,Root.COLUMN_DOCUMENT_ID};
	private static final String[] DEFAULT_DOCUMENT_PROJECTION=
	{Document.COLUMN_DOCUMENT_ID,Document.COLUMN_SIZE,Document.COLUMN_DISPLAY_NAME,Document.COLUMN_LAST_MODIFIED,Document.COLUMN_MIME_TYPE,Document.COLUMN_FLAGS};
	@Override public boolean onCreate(){return true;}
	@Override
	public Cursor queryRoots(String[]projection)throws FileNotFoundException
	{
		try
		{
			Log.i(MainActivity.LOG_TAG,"Query Root: Projection="+Arrays.toString(projection));
			MatrixCursor result=new MatrixCursor(resolveRootProjection(projection));
			for(String connection:new String[]{"127.0.0.1:8888@anonymous"})
			{
				MatrixCursor.RowBuilder row=result.newRow();
				row.add(Root.COLUMN_ROOT_ID,connection);
				row.add(Root.COLUMN_DOCUMENT_ID,connection+"/");
				row.add(Root.COLUMN_ICON,R.drawable.ic_launcher);
				row.add(Root.COLUMN_FLAGS,Root.FLAG_SUPPORTS_CREATE);
				row.add(Root.COLUMN_TITLE,connection);
			}
			return result;
		}
		catch(Exception e)
		{
			String msg="Error querying roots";
			Log.e(MainActivity.LOG_TAG,msg,e);
			throw new FileNotFoundException(msg);
		}
	}
	@Override
	public Cursor queryDocument(String documentId,String[]projection)throws FileNotFoundException
	{
		Log.i(MainActivity.LOG_TAG,"Query Document: DocumentId="+documentId+" Projection="+Arrays.toString(projection));
		MatrixCursor result=new MatrixCursor(resolveDocumentProjection(projection));
		try
		{
			FTPFile file=getFile(documentId);
			putFileInfo(result.newRow(),file);
		}
		catch(Exception e)
		{
			String msg="Error querying child "+documentId;
			Log.e(MainActivity.LOG_TAG,msg,e);
			throw new FileNotFoundException(msg);
		}
		return result;
	}
	@Override
	public Cursor queryChildDocuments(String parentDocumentId,String[]projection,String sortOrder)throws FileNotFoundException
	{
		Log.i(MainActivity.LOG_TAG,"Query Child Documents: ParentDocumentId="+parentDocumentId+" Projection="+projection+" SortOrder="+sortOrder);
		MatrixCursor result=new MatrixCursor(resolveDocumentProjection(projection));
		try
		{
			if(parentDocumentId.charAt(parentDocumentId.length()-1)!='/')parentDocumentId+="/";
			FTPFile[]files=getFile(parentDocumentId).listFiles();
			for(FTPFile file:files)
			{
				putFileInfo(result.newRow(),file);
			}
		}
		catch(Exception e)
		{
			String msg="Error querying childs of "+parentDocumentId;
			Log.e(MainActivity.LOG_TAG,msg,e);
			throw new FileNotFoundException(msg);
		}
		return result;
	}
	@Override
	public ParcelFileDescriptor openDocument(String documentId,String mode,CancellationSignal signal)throws FileNotFoundException
	{
		Log.i(MainActivity.LOG_TAG,"Open Document: DocumentId="+documentId+" mode="+mode+" signal="+signal);
		int accessMode=ParcelFileDescriptor.parseMode(mode);
		boolean isWrite=(mode.indexOf('w')!=-1);
		try
		{
			final FTPFile remoteFile=getFile(documentId);
			final File file=new File(getContext().getExternalCacheDir(),remoteFile.getName());
			remoteFile.download(file);
			if(isWrite)
			{
				return ParcelFileDescriptor.open(file,accessMode,new Handler(getContext().getMainLooper()),new ParcelFileDescriptor.OnCloseListener()
					{
						@Override
						public void onClose(IOException exception)
						{
							remoteFile.asyncUpload(file);
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
			String msg="Error opening document "+documentId;
			Log.e(MainActivity.LOG_TAG,msg,e);
			throw new FileNotFoundException(msg);
		}
	}
	@Override
    public String createDocument(String documentId,String mimeType,String displayName)throws FileNotFoundException
	{
		try
		{
			Log.i(MainActivity.LOG_TAG,"Create document: documentId="+documentId+" displayName"+displayName);
			FTPFile parent=getFile(documentId);
			FTPFile remoteFile=new FTPFile(parent,displayName);
			if(Document.MIME_TYPE_DIR.equals(mimeType))remoteFile.mkdir();
			else remoteFile.createNewFile();
			return getDocumentId(remoteFile);
        }
		catch(Exception e)
		{
			String msg="Failed to create document with name "+displayName+" and documentId "+documentId;
			Log.e(MainActivity.LOG_TAG,msg,e);
			throw new FileNotFoundException(msg);
        }
    }
    @Override
    public void deleteDocument(String documentId)throws FileNotFoundException
	{
        try
		{
			Log.i(MainActivity.LOG_TAG,"Delete document: documentId="+documentId);
			FTPFile file=getFile(documentId);
			file.delete();
		}
		catch(Exception e)
		{
			String msg="Error deleting "+documentId;
			Log.e(MainActivity.LOG_TAG,msg,e);
			throw new FileNotFoundException(msg);
		}
    }
    @Override
    public String getDocumentType(String documentId)throws FileNotFoundException
	{
		try
		{
			Log.i(MainActivity.LOG_TAG,"Get document type: documentId="+documentId);
        	FTPFile file=getFile(documentId);
        	return file.getMimeType();
		}
		catch(Exception e)
		{
			String msg="Error getting type of "+documentId;
			Log.e(MainActivity.LOG_TAG,msg,e);
			throw new FileNotFoundException(msg);
		}
    }

	@Override
	public String renameDocument(String documentId,String displayName)throws FileNotFoundException
	{
		try
		{
			Log.i(MainActivity.LOG_TAG,"Rename document: documentId="+documentId+" displayName="+displayName);
			FTPFile source=getFile(documentId);
			FTPFile parent=source.getParentFile();
			FTPFile destination=new FTPFile(parent,displayName,source.lastModified(),source.getSize(),source.isDirectory());
			source.copy(destination,getContext().getExternalCacheDir(),true);
			return getDocumentId(destination);
		}
		catch(Exception e)
		{
			String msg="Error renaming document "+documentId;
			Log.e(MainActivity.LOG_TAG,msg,e);
			throw new FileNotFoundException(msg);
		}
	}
	@Override
	public String moveDocument(String sourceDocumentId,String sourceParentDocumentId,String targetParentDocumentId) throws FileNotFoundException
	{
		try
		{
			Log.i(MainActivity.LOG_TAG,"Move document: sourceDocumentId="+sourceDocumentId+" sourceParentDocumentId="+sourceParentDocumentId+" tergatParentDocumentId="+targetParentDocumentId);
			FTPFile source=getFile(sourceDocumentId);
			FTPFile parent=getFile(targetParentDocumentId);
			FTPFile destination=new FTPFile(parent,source.getName(),source.lastModified(),source.getSize(),source.isDirectory());
			source.copy(destination,getContext().getExternalCacheDir(),true);
			return getDocumentId(destination);
		}
		catch(Exception e)
		{
			String msg="Error moving document "+sourceDocumentId;
			Log.e(MainActivity.LOG_TAG,msg,e);
			throw new FileNotFoundException(msg);
		}
	}
	@Override
	public String copyDocument(String sourceDocumentId,String targetParentDocumentId) throws FileNotFoundException
	{
		try
		{
			Log.i(MainActivity.LOG_TAG,"Move document: sourceDocumentId="+sourceDocumentId+" tergatParentDocumentId="+targetParentDocumentId);
			FTPFile source=getFile(sourceDocumentId);
			FTPFile parent=getFile(targetParentDocumentId);
			FTPFile destination=new FTPFile(parent,source.getName(),source.lastModified(),source.getSize(),source.isDirectory());
			source.copy(destination,getContext().getExternalCacheDir(),false);
			return getDocumentId(destination);
		}
		catch(Exception e)
		{
			String msg="Error moving document "+sourceDocumentId;
			Log.e(MainActivity.LOG_TAG,msg,e);
			throw new FileNotFoundException(msg);
		}
	}
	private static String getRoot(String documentId)
	{
		return documentId.substring(0,documentId.indexOf("/"));
	}
	private static String getIp(String documentId)
	{
		return documentId.substring(0,documentId.indexOf(":"));
	}
	private static int getPort(String documentId)
	{
		return Integer.valueOf(documentId.substring(documentId.indexOf(":")+1,documentId.indexOf("@")));
	}
	private static String getUser(String documentId)
	{
		int end=documentId.indexOf("/");
		if(end==-1)end=documentId.length();
		return documentId.substring(documentId.indexOf("@")+1,end);
	}
	private static String getPath(String documentId)
	{
		int start=documentId.indexOf("/");
		if(start==-1)return"";
		else return documentId.substring(start+1);
	}
	private static final String[]resolveDocumentProjection(String[]projection)
	{
		if(projection==null)return DEFAULT_DOCUMENT_PROJECTION;
		else return projection;
	}
	private static final String[]resolveRootProjection(String[]projection)
	{
		if(projection==null)return DEFAULT_ROOT_PROJECTION;
		else return projection;
	}
	private static final FTPFile getFile(String documentId)throws IOException
	{
		String ip=getIp(documentId);
		int port=getPort(documentId);
		String user=getUser(documentId);
		String password=DEBUG_PASSWORD;
		String path=getPath(documentId);
		return new FTPFile(ip,port,user,password,path);
	}
	private static final String getDocumentId(FTPFile ftp)
	{
		return ftp.getIp()+":"+ftp.getPort()+"@"+ftp.getUser()+"/"+ftp.getPath();
	}
	private void putFileInfo(MatrixCursor.RowBuilder row,FTPFile file)
	{
		int flags;
		if(file.isDirectory())flags=Document.FLAG_DIR_SUPPORTS_CREATE;
		else
		{
			flags=Document.FLAG_SUPPORTS_WRITE;
			row.add(Document.COLUMN_SIZE,file.getSize());
		}
		flags|=Document.FLAG_SUPPORTS_COPY|Document.FLAG_SUPPORTS_DELETE|Document.FLAG_SUPPORTS_MOVE|Document.FLAG_SUPPORTS_RENAME;
		row.add(Document.COLUMN_FLAGS,flags);
		row.add(Document.COLUMN_MIME_TYPE,file.getMimeType());
		row.add(Document.COLUMN_DISPLAY_NAME,file.getName());
		row.add(Document.COLUMN_DOCUMENT_ID,getDocumentId(file));
		row.add(Document.COLUMN_LAST_MODIFIED,file.lastModified());
	}
}
