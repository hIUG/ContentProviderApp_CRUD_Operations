package com.e.c.a.h.contentproviderapp_20170423_crud_operations;

import android.Manifest;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks {
    private static final int PERMISSION_REQUEST_CODE_CONTACTS = 666;
    private static final int ACTIVITY_SETTINGS_REQUEST_CODE = 777;
    private static final int LOADER_CALLBACK_ID = 888;
    private static final String LOG_TAG = "HACE:";

    private final String BUTTON_TAG_BY_CONTENT_RESOLVER = "byContentResolver";
    private final String BUTTON_TAG_BY_BATCH = "byBatch";
    private final String BUTTON_TAG_BY_INTENT = "byIntent";

    private final String BUTTON_TEXT_BY_CONTENT_RESOLVER = "Add";
    private final String BUTTON_TEXT_BY_BATCH = "Add by batch";
    private final String BUTTON_TEXT_BY_INTENT = "Add by intent";

    private ListView listView;
    private EditText editText;

    private int permissionReadContacts = PackageManager.PERMISSION_DENIED;
    private int permissionWriteContacts = PackageManager.PERMISSION_DENIED;

    private String [] columnsToSelect = {
            ContactsContract.Contacts.NAME_RAW_CONTACT_ID,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            ContactsContract.Contacts.CONTACT_STATUS,
            ContactsContract.Contacts.HAS_PHONE_NUMBER};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = (ListView) findViewById(R.id.listViewMain);
        editText = (EditText) findViewById(R.id.editText);

        ((Button) findViewById(R.id.buttonAdd)).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                //Button tag will always be either "byBatch" or "byIntent"
                String newTag;
                Button b = (Button) v;
                if(v.getTag().equals(BUTTON_TAG_BY_CONTENT_RESOLVER)) {
                    b.setTag(BUTTON_TAG_BY_BATCH);
                    b.setText(BUTTON_TEXT_BY_BATCH);
                    Toast.makeText(b.getContext(), "Add will be done by batch...", Toast.LENGTH_SHORT).show();
                } else if(v.getTag().equals(BUTTON_TAG_BY_BATCH)) {
                    b.setTag(BUTTON_TAG_BY_INTENT);
                    b.setText(BUTTON_TEXT_BY_INTENT);
                    Toast.makeText(b.getContext(), "Add will be done by intent...", Toast.LENGTH_SHORT).show();
                } else {
                    b.setTag(BUTTON_TAG_BY_CONTENT_RESOLVER);
                    b.setText(BUTTON_TEXT_BY_CONTENT_RESOLVER);
                    Toast.makeText(b.getContext(), "Add will be done by content resolver...", Toast.LENGTH_SHORT).show();
                }

                Animation myAnim = AnimationUtils.loadAnimation(b.getContext(), R.anim.bounce);
                b.startAnimation(myAnim);

                return true;
            }
        });

        checkPermissionReadContacts();
    }

    private void checkPermissionReadContacts() {
        permissionReadContacts = ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS);
        permissionWriteContacts = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_CONTACTS);

        if(permissionReadContacts != PackageManager.PERMISSION_GRANTED
                || permissionWriteContacts != PackageManager.PERMISSION_GRANTED) {
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_CONTACTS)
                    || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_CONTACTS)) {
                Snackbar.make(listView, "Please provide permissions to access contacts...", Snackbar.LENGTH_INDEFINITE)
                        .setAction("Provide permissions", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent();
                                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                intent.addCategory(Intent.CATEGORY_DEFAULT);
                                intent.setData(Uri.parse("package:" + getPackageName()));
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                                intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                                startActivityForResult(intent, ACTIVITY_SETTINGS_REQUEST_CODE);
                            }
                        })
                        .show();
            } else {
                ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS}, PERMISSION_REQUEST_CODE_CONTACTS);
            }
        } else {
            if(getSupportLoaderManager().getLoader(LOADER_CALLBACK_ID) == null) {
                getSupportLoaderManager().initLoader(LOADER_CALLBACK_ID, null, this);
            } else {
                getSupportLoaderManager().restartLoader(LOADER_CALLBACK_ID, null, this);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE_CONTACTS:
                checkPermissionReadContacts();
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case ACTIVITY_SETTINGS_REQUEST_CODE:
                checkPermissionReadContacts();
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    public void buttonAddCL(View view) {
        if(!(editText.getText().toString().length() > 0)) {
            Toast.makeText(this, "Wrong request...!", Toast.LENGTH_LONG).show();
            return;
        }
        if(view.getTag().equals(BUTTON_TAG_BY_CONTENT_RESOLVER)) {

            String newName = editText.getText().toString().trim();

            ContentResolver contentResolver;
            ContentValues contentValues;

            if(newName.length() == 0) {
                Toast.makeText(this, "Wrong request...!", Toast.LENGTH_LONG).show();
                return;
            }

            contentResolver = getContentResolver();
            contentValues = new ContentValues();
            contentValues.put(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY, newName);
            contentValues.put(ContactsContract.RawContacts.DISPLAY_NAME_PRIMARY, newName);

            contentResolver.insert(ContactsContract.RawContacts.CONTENT_URI, contentValues);
        } else if(view.getTag().equals(BUTTON_TAG_BY_BATCH)) {
            //Add by batch
            ArrayList<ContentProviderOperation> operationsBatch = new ArrayList<>();

            operationsBatch.add(
                    ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                            .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, editText.getText().toString().trim().split(" ")[0] + "@gmail.com")
                            .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, "google")
                            .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, "com.google")
                            .build());

//            operationsBatch.add(
//                    ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
//                            .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, editText.getText().toString().trim())
//                            .build());

            try {
                getContentResolver().applyBatch(ContactsContract.AUTHORITY, operationsBatch);
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "Error when applying insert batch", e);
                e.printStackTrace();
            } catch (OperationApplicationException e) {
                Log.e(LOG_TAG, "Error when applying insert batch", e);
                e.printStackTrace();
            }
        } else {
            //Add by intent
            Intent intent = new Intent();
            intent.setAction(ContactsContract.Intents.Insert.ACTION);
            intent.setType(ContactsContract.RawContacts.CONTENT_TYPE);
            intent.putExtra(ContactsContract.Intents.Insert.NAME, editText.getText().toString().trim());

            startActivity(intent);
        }
    }

    public void buttonEditCL(View view) {
        String[] updateUserQuery = editText.getText().toString().split(" ");
        String targetID;
        String newName;
        String where;
        String[] whereParams;

        ContentResolver contentResolver;
        ContentValues contentValues;

        if(updateUserQuery.length != 2) {
            Toast.makeText(this, "Wrong request...!", Toast.LENGTH_LONG).show();
            return;
        }

        targetID = updateUserQuery[0];
        newName = updateUserQuery[1];

        where = ContactsContract.RawContacts._ID + " = ?";
        whereParams = new String[] {targetID};

        contentResolver = getContentResolver();
        contentValues = new ContentValues();
        contentValues.put(ContactsContract.RawContacts.DISPLAY_NAME_PRIMARY, newName);

        contentResolver.update(ContactsContract.RawContacts.CONTENT_URI, contentValues, where, whereParams);
    }

    public void buttonRemoveCL(View view) {
        String where = ContactsContract.RawContacts._ID + " = " + "'" + editText.getText().toString()+ "'";
        int rowsDelete = getContentResolver().delete(ContactsContract.RawContacts.CONTENT_URI, where, null);

        Toast.makeText(this, rowsDelete + " contacts deleted successfully...", Toast.LENGTH_LONG).show();
    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        if(id == LOADER_CALLBACK_ID) {
            return new CursorLoader(this, ContactsContract.Contacts.CONTENT_URI, columnsToSelect, null, null, null);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader loader, Object data) {
        ArrayList<String> contacts = new ArrayList<>();
        ArrayAdapter<String> contactsAdapter;

        Cursor cursor = (Cursor) data;

        if(cursor != null && cursor.getCount() > 0) {
            while(cursor.moveToNext()) {
                contacts.add(cursor.getString(0) + ", " + cursor.getString(1) + ", status:" + cursor.getString(1) + ", hasPhone:"  + cursor.getString(2));
            }
        } else {
            contacts.add("No contacts in device");
        }

        contactsAdapter = new ArrayAdapter<String>(this, R.layout.simple_text_view_adapter_layout, contacts);

        listView.setAdapter(contactsAdapter);
    }

    @Override
    public void onLoaderReset(Loader loader) {

    }

    private static class NestedClass {

    }
}
