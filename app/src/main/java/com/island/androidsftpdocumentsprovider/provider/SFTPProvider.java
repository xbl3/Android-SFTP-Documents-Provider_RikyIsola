package com.island.androidsftpdocumentsprovider.provider;
import android.accounts.*;
import android.content.*;
import android.content.res.*;
import android.database.*;
import android.graphics.*;
import android.net.*;
import android.os.*;
import android.provider.*;
import android.provider.DocumentsContract.*;
import android.util.*;
import android.webkit.*;
import com.island.androidsftpdocumentsprovider.*;
import com.island.androidsftpdocumentsprovider.account.*;
import com.island.sftp.*;
import java.io.*;
import java.util.*;

import android.accounts.OperationCanceledException;
import java.net.*;
public class SFTPProvider extends DocumentsProvider
{
	public static final String TAG="SFTPDocumentsProvider";
	private static final String[]DEFAULT_ROOT_PROJECTION=
	{Root.COLUMN_ROOT_ID,Root.COLUMN_FLAGS,Root.COLUMN_ICON,Root.COLUMN_TITLE,Root.COLUMN_DOCUMENT_ID,Root.COLUMN_SUMMARY};
	private static final String[]DEFAULT_DOCUMENT_PROJECTION=
	{Document.COLUMN_DOCUMENT_ID,Document.COLUMN_SIZE,Document.COLUMN_DISPLAY_NAME,Document.COLUMN_LAST_MODIFIED,Document.COLUMN_MIME_TYPE,Document.COLUMN_FLAGS};
	private final List<SFTP>connections=new ArrayList<>();
	@Override
	public boolean onCreate()
	{
		return true;
	}
	@Override
	public Cursor queryRoots(String[]projection)throws FileNotFoundException
	{
		Log.i(SFTPProvider.TAG,String.format("SFTPProvider queryRoots %s",Arrays.toString(projection)));
		try
		{
			MatrixCursor result=new MatrixCursor(resolveRootProjection(projection));
			AccountManager accountManager=AccountManager.get(getContext());
			Account[]accounts=accountManager.getAccountsByType(AuthenticationActivity.ACCOUNT_TYPE);
			for(Account account:accounts)
			{
				Uri uri=Uri.parse(SFTP.SCHEME+account.name);
				MatrixCursor.RowBuilder row=result.newRow();
				row.add(Root.COLUMN_ROOT_ID,uri.toString());
				String documentId=uri.toString()+"/";
				row.add(Root.COLUMN_DOCUMENT_ID,documentId);
				int icon=R.drawable.ic_launcher;
				row.add(Root.COLUMN_ICON,icon);
				int flags=Root.FLAG_SUPPORTS_CREATE;
				row.add(Root.COLUMN_FLAGS,flags);
				String title=getContext().getString(R.string.sftp);
				row.add(Root.COLUMN_TITLE,title);
				row.add(Root.COLUMN_SUMMARY,uri.getAuthority());
			}
			
			return result;
		}
		catch(Exception e)
		{
			throw exception(e,"QueryRoots");
		}
	}
	
	@Override
	public Cursor queryDocument(String uri,String[]projection)throws FileNotFoundException
	{
		Log.i(SFTPProvider.TAG,String.format("SFTPProvider queryDocuments %s %s",uri,Arrays.toString(projection)));
		try
		{
			Objects.requireNonNull(uri);
			MatrixCursor result=new MatrixCursor(resolveDocumentProjection(projection));
			Uri documentId=Uri.parse(uri);
			putFileInfo(result.newRow(),documentId);
			return result;
		}
		catch(Exception e)
		{
			throw exception(e,"QueryDocument",uri);
		}
	}
	@Override
	public Cursor queryChildDocuments(String parentUri,String[]projection,String sortOrder)throws FileNotFoundException
	{
		Log.i(SFTPProvider.TAG,String.format("SFTPProvider queryChildDocuments %s %s %s",parentUri,Arrays.toString(projection),Arrays.toString(projection)));
		try
		{
			Objects.requireNonNull(parentUri);
			MatrixCursor result=new MatrixCursor(resolveDocumentProjection(projection));
			Uri parentDocumentId=Uri.parse(parentUri);
			SFTP sftp=getSFTP(parentDocumentId);
			try
			{
				File[]files=sftp.listFiles(SFTP.getFile(parentDocumentId));
				for(File file:files)
				{
					putFileInfo(result.newRow(),sftp.getUri(file));
				}
			}
			catch(SocketException e)
			{
				remove(sftp);
				throw e;
			}
			return result;
		}
		catch(Exception e)
		{
			throw exception(e,"QueryChildDocuments",parentUri);
		}
	}
	@Override
	public ParcelFileDescriptor openDocument(String uri,String mode,CancellationSignal signal)throws FileNotFoundException
	{
		Log.i(SFTPProvider.TAG,String.format("SFTPProvider openDocument %s %s %s",uri,mode,signal));
		try
		{
			Objects.requireNonNull(uri);
			Objects.requireNonNull(mode);
			int accessMode=ParcelFileDescriptor.parseMode(mode);
			boolean isWrite=(mode.indexOf('w')!=-1);
			final Uri documentId=Uri.parse(uri);
			SFTP sftp=getSFTP(documentId);
			File cache=new File(getContext().getCacheDir(),SFTP.getFile(documentId).getName());
			try
			{
				sftp.get(SFTP.getFile(documentId),cache);
				if(isWrite)
				{
					Looper looper=getContext().getMainLooper();
					return ParcelFileDescriptor.open(cache,accessMode,new Handler(looper),new ParcelFileDescriptor.OnCloseListener()
						{
							@Override
							public void onClose(IOException exception)
							{
								if(exception==null)
								{
									Intent intent=new Intent(getContext(),UploaderService.class);
									intent.setData(documentId);
									getContext().startService(intent);
								}
								else
								{
									exception(exception,"OnCloseDocument");
								}
							}
						}
					);
				}
				else
				{
					return ParcelFileDescriptor.open(cache,accessMode);
				}
			}
			catch(SocketException e)
			{
				remove(sftp);
				throw e;
			}
		}
		catch(Exception e)
		{
			throw exception(e,"openDocument",uri);
		}
	}
	@Override
    public String createDocument(String parentUri,String mimeType,String displayName)throws FileNotFoundException
	{
		Log.i(SFTPProvider.TAG,String.format("SFTPProvider createDocument %s %s %s",parentUri,mimeType,displayName));
		try
		{
			Objects.requireNonNull(parentUri);
			Objects.requireNonNull(mimeType);
			Objects.requireNonNull(displayName);
			Uri parentDocumentId=Uri.parse(parentUri);
			SFTP sftp=getSFTP(parentDocumentId);
			try
			{
				Uri documentId=sftp.getUri(new File(SFTP.getFile(parentDocumentId),displayName));
				File file=SFTP.getFile(documentId);
				if(Document.MIME_TYPE_DIR.equals(mimeType))sftp.mkdirs(file);
				else sftp.newFile(file);
				return documentId.toString();
			}
			catch(SocketException e)
			{
				remove(sftp);
				throw e;
			}
        }
		catch(Exception e)
		{
			throw exception(e,"CreateDocument",parentUri);
        }
    }
    @Override
    public void deleteDocument(String uri)throws FileNotFoundException
	{
		Log.i(SFTPProvider.TAG,String.format("SFTPProvider deleteDocument %s",uri));
        try
		{
			Objects.requireNonNull(uri);
			Uri documentId=Uri.parse(uri);
			SFTP sftp=getSFTP(documentId);
			try
			{
				sftp.delete(SFTP.getFile(documentId));
			}
			catch(SocketException e)
			{
				remove(sftp);
				throw e;
			}
		}
		catch(Exception e)
		{
			throw exception(e,"DeleteDocument",uri);
		}
    }
    @Override
    public String getDocumentType(String uri)throws FileNotFoundException
	{
		Log.i(SFTPProvider.TAG,String.format("SFTPProvider getDocumentType %s",uri));
		try
		{
			Objects.requireNonNull(uri);
			Uri documentId=Uri.parse(uri);
			SFTP sftp=getSFTP(documentId);
			try
			{
				String mimeType=sftp.getMimeType(SFTP.getFile(documentId));
				return mimeType;
			}
			catch(SocketException e)
			{
				remove(sftp);
				throw e;
			}
		}
		catch(Exception e)
		{
			throw exception(e,"GetDocumentType",uri);
		}
    }

	@Override
	public String renameDocument(String uri,String displayName)throws FileNotFoundException
	{
		Log.i(SFTPProvider.TAG,String.format("SFTPProvider renameDocument %s %s",uri,displayName));
		try
		{
			Objects.requireNonNull(uri);
			Objects.requireNonNull(displayName);
			Uri documentId=Uri.parse(uri);
			SFTP sftp=getSFTP(documentId);
			try
			{
				File source=SFTP.getFile(documentId);
				File parent=source.getParentFile();
				File destination=uniqueFile(sftp,parent,displayName);
				sftp.renameTo(source,destination);
				return sftp.getUri(destination).toString();
			}
			catch(SocketException e)
			{
				remove(sftp);
				throw e;
			}
		}
		catch(Exception e)
		{
			throw exception(e,"RenameDocument",uri);
		}
	}
	@Override
	public String moveDocument(String sourceUri,String sourceParentUri,String targetParentUri)throws FileNotFoundException
	{
		Log.i(SFTPProvider.TAG,String.format("SFTPProvider moveDocument %s %s %s",sourceUri,sourceParentUri,targetParentUri));
		try
		{
			Objects.requireNonNull(sourceUri);
			Objects.requireNonNull(sourceParentUri);
			Objects.requireNonNull(targetParentUri);
			Uri sourceDocumentId=Uri.parse(sourceUri);
			File source=SFTP.getFile(sourceDocumentId);
			SFTP sftp=getSFTP(sourceDocumentId);
			try
			{
				File destination=uniqueFile(sftp,SFTP.getFile(Uri.parse(targetParentUri)),source.getName());
				sftp.renameTo(source,destination);
				return sftp.getUri(destination).toString();
			}
			catch(SocketException e)
			{
				remove(sftp);
				throw e;
			}
		}
		catch(Exception e)
		{
			throw exception(e,"MoveDocument",sourceUri,targetParentUri);
		}
	}
	@Override
	public String copyDocument(String sourceUri,String targetParentUri)throws FileNotFoundException
	{
		Log.i(SFTPProvider.TAG,String.format("SFTPProvider copyDocument %s %s",sourceUri,targetParentUri));
		try
		{
			Objects.requireNonNull(sourceUri);
			Objects.requireNonNull(targetParentUri);
			Uri sourceDocumentId=Uri.parse(sourceUri);
			File source=SFTP.getFile(sourceDocumentId);
			SFTP sftp=getSFTP(sourceDocumentId);
			try
			{
				File destination=uniqueFile(sftp,SFTP.getFile(Uri.parse(targetParentUri)),source.getName());
				sftp.copy(source,destination);
				return sftp.getUri(destination).toString();
			}
			catch(SocketException e)
			{
				remove(sftp);
				throw e;
			}
		}
		catch(Exception e)
		{
			throw exception(e,"CopyDocument",sourceUri,targetParentUri);
		}
	}
	private static String[]resolveDocumentProjection(String[]projection)
	{
		if(projection==null)return DEFAULT_DOCUMENT_PROJECTION;
		else return projection;
	}
	private static String[]resolveRootProjection(String[]projection)
	{
		if(projection==null)return DEFAULT_ROOT_PROJECTION;
		else return projection;
	}
	public static String getToken(Context context,Uri documentId)throws IOException
	{
		Objects.requireNonNull(context);
		Objects.requireNonNull(documentId);
		AccountManager accountManager=AccountManager.get(context);
		Account account=null;
		for(Account acc:accountManager.getAccountsByType(AuthenticationActivity.ACCOUNT_TYPE))if(acc.name.equals(documentId.getAuthority()))account=acc;
		try
		{
			return accountManager.getAuthToken(account,AuthenticationActivity.TOKEN_TYPE,null,false,null,null).getResult().getString(AccountManager.KEY_AUTHTOKEN);
		}
		catch(AuthenticatorException|OperationCanceledException e)
		{
			throw new IOException(e);
		}
	}
	private SFTP getSFTP(Uri documentId)throws IOException
	{
		assert documentId!=null;
		SFTP sftp=null;
		for(SFTP connection:connections)
		{
			if(connection.uri.getAuthority().equals(documentId.getAuthority()))
			{
				sftp=connection;
				break;
			}
		}
		if(sftp==null)
		{
			sftp=new SFTP(documentId,getToken(getContext(),documentId));
			connections.add(sftp);
		}
		return sftp;
	}
	private void putFileInfo(MatrixCursor.RowBuilder row,Uri uri)throws IOException
	{
		assert row!=null;
		assert uri!=null;
		int flags;
		SFTP sftp=getSFTP(uri);
		try
		{
			File file=SFTP.getFile(uri);
			if(sftp.isDirectory(file))flags=Document.FLAG_DIR_SUPPORTS_CREATE;
			else
			{
				flags=Document.FLAG_SUPPORTS_WRITE;
				row.add(Document.COLUMN_SIZE,sftp.length(file));
			}
			flags|=Document.FLAG_SUPPORTS_DELETE;
			if(Build.VERSION.SDK_INT>=24)flags|=Document.FLAG_SUPPORTS_COPY|Document.FLAG_SUPPORTS_MOVE|Document.FLAG_SUPPORTS_RENAME;
			row.add(Document.COLUMN_FLAGS,flags);
			String mimeType=sftp.getMimeType(file);
			row.add(Document.COLUMN_MIME_TYPE,mimeType);
			String name=file.getName();
			row.add(Document.COLUMN_DISPLAY_NAME,name);
			String documentId=sftp.getUri(file).toString();
			row.add(Document.COLUMN_DOCUMENT_ID,documentId);
			long lastModified=sftp.lastModified(file);
			row.add(Document.COLUMN_LAST_MODIFIED,lastModified);
		}
		catch(SocketException e)
		{
			remove(sftp);
			throw e;
		}
	}
	private File uniqueFile(SFTP sftp,File parent,String displayName)throws IOException
	{
		assert sftp!=null;
		assert parent!=null;
		assert displayName!=null;
		File destination=new File(parent,displayName);
		while(sftp.exists(destination))
		{
			int lastDot=displayName.lastIndexOf('.');
			String name,extension;
			if(lastDot>=0)
			{
				name=displayName.substring(0,lastDot);
				extension=displayName.substring(lastDot+1);
			}
			else name=extension=null;
			name+=" 2";
			displayName=name+"."+extension;
			destination=new File(parent,displayName);
		}
		return destination;
	}
	private FileNotFoundException exception(Exception e,String msg,Object...args)
	{
		assert e!=null;
		assert msg!=null;
		assert args!=null;
		for(Object arg:args)msg+=" "+arg;
		Log.e(TAG,msg,e);
		FileNotFoundException exception=new FileNotFoundException(msg);
		exception.initCause(e);
		return exception;
	}
	private void remove(SFTP sftp)
	{
		assert sftp!=null;
		connections.remove(sftp);
		sftp.close();
	}
}
