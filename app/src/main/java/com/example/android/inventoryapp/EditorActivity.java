package com.example.android.inventoryapp;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.content.CursorLoader;
import android.net.Uri;
import android.os.Bundle;
import android.app.LoaderManager;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.android.inventoryapp.data.InventoryContract.InventoryEntry;

import org.w3c.dom.Text;

import java.math.BigDecimal;

/**
 * Allows user to create a new product or edit an existing one.
 */
public class EditorActivity extends AppCompatActivity implements LoaderManager
        .LoaderCallbacks<Cursor> {

    /**
     * Identifier for the inventory data loader
     */
    private static final int EXISTING_INVENTORY_LOADER = 0;
    String LOG_TAG = EditorActivity.class.getSimpleName();
    /**
     * Content URI for the existing product (null if it's a new product)
     */
    private Uri mCurrentProductUri;

    /**
     * EditText field to enter the product name
     */
    private EditText mNameEditText;

    /**
     * EditText field to enter the price of the product
     */
    private EditText mPriceEditText;

    /**
     * EditText field to enter the quantity of the product
     */
    private EditText mQuantityEditText;

    // Button for decrement and increment quantity of the product
    private Button mDecrementButton;
    private Button mIncrementButton;

    //EditText fields to enter supplier name and number
    private EditText mSupplierNameEditText;
    private EditText mSupplierPhoneEditText;

    private ImageButton mCallSupplierButton;


    /**
     * Boolean flag that keeps track of whether the product has been edited (true) or not
     * (false)
     */
    private boolean mProductHasChanged = false;

    /**
     * OnTouchListener that listens for any user touches on a View, implying that they are modifying
     * the view, and we change the mProductHasChanged boolean to true.
     */
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mProductHasChanged = true;
            return false;
        }
    };

    //Helper method to convert price string (which represents dollars) to a BigDecimal with two
    // decimal places. The BigDecimal value is then multiplied by 100 and converted to an integer
    // value that represents price in cents to be stored in the database. The following
    // stackoverflow answers gave me the idea for this helper method, as I first thought I needed
    // to convert the string to a float. After reading the BigDecimal documentation, I realized I
    // did not have to.
    //https://stackoverflow.com/questions/8911356/whats-the-best-practice-to-round-a-float-to-2-decimals
    //https://docs.oracle.com/javase/7/docs/api/java/math/BigDecimal.html
    private static int priceStringToPriceCents(String priceStringDollars) {
        final int DECIMAL_PLACES = 2;
        int priceIntCents;
        BigDecimal bdPrice = new BigDecimal(priceStringDollars);
        bdPrice = bdPrice.setScale(DECIMAL_PLACES, BigDecimal.ROUND_HALF_UP);
        bdPrice = bdPrice.multiply(new BigDecimal(100)); //converting dollars to cents
        priceIntCents = bdPrice.intValue(); //converting from BigDecimal to int
        return priceIntCents;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        //Examine the intent that was used to launch this activity to figure out if we're
        // creating a new product or editing an existing one. The getData() method is in the
        // Intent class and returns a URI.
        Intent intent = getIntent();
        mCurrentProductUri = intent.getData();

        // If the intent DOES NOT contain a product content URI, then we know that we are creating a
        // new product.
        if (mCurrentProductUri == null) {
            //This is a new product, so change the app bar to say "Add a Product"
            setTitle(getString(R.string.editor_activity_title_new_product));

            // Invalidate the options menu, so the "Delete" menu option can be hidden.
            // (It doesn't make sense to delete a product that hasn't been created yet.)
            invalidateOptionsMenu();
        } else {
            //Otherwise this is an existing product, so change app bar to say "Edit Product"
            setTitle(getString(R.string.editor_activity_title_edit_product));

            // Initialize a loader to read the inventory data from the database and display the
            // current values in the editor. This is for editing an existing product only.
            getLoaderManager().initLoader(EXISTING_INVENTORY_LOADER, null, this);
        }

        // Find all relevant views that we will need to read user input from
        mNameEditText = (EditText) findViewById(R.id.product_name_edit_view);
        mPriceEditText = (EditText) findViewById(R.id.price_edit_view);
        mQuantityEditText = (EditText) findViewById(R.id.quantity_edit_view);
        mSupplierNameEditText = (EditText) findViewById(R.id.supplier_name_edit_view);
        mSupplierPhoneEditText = (EditText) findViewById(R.id.supplier_phone_edit_view);
        mDecrementButton = (Button) findViewById(R.id.quantity_decrement_button);
        mIncrementButton = (Button) findViewById(R.id.quantity_increment_button);
        mCallSupplierButton = (ImageButton) findViewById(R.id.call_supplier_button);


        // Setup OnTouchListeners on all the input fields, so we can determine if the user
        // has touched or modified them. This will let us know if there are unsaved changes
        // or not, if the user tries to leave the editor without saving.
        mNameEditText.setOnTouchListener(mTouchListener);
        mPriceEditText.setOnTouchListener(mTouchListener);
        mQuantityEditText.setOnTouchListener(mTouchListener);
        mIncrementButton.setOnTouchListener(mTouchListener);
        mDecrementButton.setOnTouchListener(mTouchListener);
        mSupplierNameEditText.setOnTouchListener(mTouchListener);
        mSupplierPhoneEditText.setOnTouchListener(mTouchListener);

        //This is to make sure the phone number entry is auto formatted.
        mSupplierPhoneEditText.addTextChangedListener(new PhoneNumberFormattingTextWatcher());

        //Set up onclicklisteners for decrement and increment buttons using helper method below
        setUpIncrementDecrementButtons();

        //Set up OnClickListener for phone icon button to open phone application to call supplier
        //https://stackoverflow.com/questions/11699819/how-do-i-get-the-dialer-to-open-with-phone-number-displayed
        mCallSupplierButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //If phone number field is empty, don't go to phone app
                if (TextUtils.isEmpty(mSupplierPhoneEditText.getText())) {
                    Toast.makeText(EditorActivity.this, getString(R.string
                            .editor_no_number_to_call), Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent callIntent = new Intent(Intent.ACTION_DIAL);
                String data = "tel:" + mSupplierPhoneEditText.getText();
                callIntent.setData(Uri.parse(data));
                startActivity(callIntent);
            }
        });
    }

    //Get user input from editor and save product into database
    private boolean saveProduct() {

        String nameString = mNameEditText.getText().toString().trim();
        String priceString = mPriceEditText.getText().toString().trim();
        String quantityString = mQuantityEditText.getText().toString().trim();
        String supplierNameString = mSupplierNameEditText.getText().toString().trim();
        String supplierPhoneString = mSupplierPhoneEditText.getText().toString().trim();

        //if all fields are empty when adding a new product, don't add to database
        if (mCurrentProductUri == null && TextUtils.isEmpty(nameString) && TextUtils.isEmpty
                (priceString) && TextUtils.isEmpty(quantityString) && TextUtils.isEmpty
                (supplierNameString) && TextUtils.isEmpty(supplierPhoneString)) {

            //Since no fields were modified, we can return early without creating a new product.
            // No need to create ContentValues and no need to do any ContentProvider operations.
            Toast.makeText(this, getString(R.string.editor_insert_product_failed),
                    Toast.LENGTH_SHORT).show();
            return true;
        }

        // Create a ContentValues object where column names are the keys,
        // and product attributes are the values.
        ContentValues values = new ContentValues();

        //We need a product name to identify the product; therefore, we stay on editor screen if
        // user tries to save product without a name.
        if (TextUtils.isEmpty(nameString)) {
            Toast.makeText(this, getString(R.string.save_product_failed_no_name), Toast
                    .LENGTH_SHORT).show();
            return false;
        }
        values.put(InventoryEntry.COLUMN_INVENTORY_PRODUCT_NAME, nameString);

        // If the price is not provided by the user, use 0.00 by default.
        if (priceString.isEmpty()) {
            priceString = "0.00";
            Log.i(LOG_TAG, "priceString was empty, so it was set to 0.00 = " + priceString);
        }
        // Convert priceString (dollars) to an integer price in cents using helper method
        int priceCents = priceStringToPriceCents(priceString);
        Log.i("EditorActivity", "priceCents in saveProduct() = " + priceCents);
        values.put(InventoryEntry.COLUMN_INVENTORY_PRICE, priceCents);

        // If the quantity is not provided by the user, don't try to parse the string into an
        // integer value. Use 0 by default.
        int quantity = 0;
        if (!TextUtils.isEmpty(quantityString)) {
            quantity = Integer.parseInt(quantityString);
        }
        values.put(InventoryEntry.COLUMN_INVENTORY_QUANTITY, quantity);
        // It is possible that the store doesn't have the supplier's name or phone number, so we
        // are going to allow it to save as an empty string. Often times bookstores will purchase
        // used copies of books for resale. As seen with the Product Name, I know how to force
        // the user to enter data before being able to save.
        values.put(InventoryEntry.COLUMN_INVENTORY_SUPPLIER_NAME, supplierNameString);
        values.put(InventoryEntry.COLUMN_INVENTORY_SUPPLIER_PHONE_NUMBER, supplierPhoneString);

        //Are we inserting a new product or updating an existing one?
        if (mCurrentProductUri == null) {
            // This is a NEW product
            // Insert a new product into the provider, returning the content URI for the new product
            Uri newUri = getContentResolver().insert(InventoryEntry.CONTENT_URI, values);

            // Show a toast message depending on whether or not the insertion was successful
            if (newUri == null) {
                // If the new content URI is null, then there was an error with insertion.
                Toast.makeText(this, getString(R.string.editor_insert_product_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the insertion was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_insert_product_successful),
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            // Otherwise this is an EXISTING product, so update the product with content URI:
            // mCurrentProductUri and pass in the new ContentValues. Pass in null for the
            // selection and selection args because mCurrentProductUri will already identify the
            // correct row in the database that we want to modify.
            int rowsAffected = getContentResolver().update(mCurrentProductUri, values, null, null);
            // Show a toast message depending on whether or not the update was successful.
            if (rowsAffected == 0) {
                // If no rows were affected, then there was an error with the update.
                Toast.makeText(this, getString(R.string.editor_update_product_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the update was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_update_product_successful),
                        Toast.LENGTH_SHORT).show();
            }
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    /**
     * This method is called after invalidateOptionsMenu(), so that the
     * menu can be updated (some menu items can be hidden or made visible).
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // If this is a new product, hide the "Delete" menu item.
        if (mCurrentProductUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                //Save product to database
                boolean leaveScreen = saveProduct();
                //Exit activity if saveProduct() returns true
                if (leaveScreen) {
                    finish();
                }
                return true;
            // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                // Pop up confirmation dialog for deletion
                showDeleteConfirmationDialog();
                return true;
            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // If the product hasn't changed, continue with navigating up to parent activity
                // which is the {@link CatalogActivity}.
                if (!mProductHasChanged) {
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    return true;
                }

                // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                // Create a click listener to handle the user confirming that
                // changes should be discarded.
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // User clicked "Discard" button, navigate to parent activity.
                                NavUtils.navigateUpFromSameTask(EditorActivity.this);
                            }
                        };

                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * This method is called when the back button is pressed.
     */
    @Override
    public void onBackPressed() {
        // If the product hasn't changed, continue with handling back button press
        if (!mProductHasChanged) {
            super.onBackPressed();
            return;
        }

        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, close the current activity.
                        finish();
                    }
                };

        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // Since the editor shows all product attributes, define a projection that contains
        // all columns from the inventory table
        String[] projection = {
                InventoryEntry._ID,
                InventoryEntry.COLUMN_INVENTORY_PRODUCT_NAME,
                InventoryEntry.COLUMN_INVENTORY_PRICE,
                InventoryEntry.COLUMN_INVENTORY_QUANTITY,
                InventoryEntry.COLUMN_INVENTORY_SUPPLIER_NAME,
                InventoryEntry.COLUMN_INVENTORY_SUPPLIER_PHONE_NUMBER};

        // This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(this,   // Parent activity context
                mCurrentProductUri,         // Query the content URI for the current product
                projection,             // Columns to include in the resulting Cursor
                null,                   // No selection clause
                null,                   // No selection arguments
                null);                  // Default sort order
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Bail early if the cursor is null or there is less than 1 row in the cursor
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }

        // Proceed with moving to the first row of the cursor and reading data from it
        // (This should be the only row in the cursor)
        if (cursor.moveToFirst()) {
            // Find the columns of product attributes that we're interested in
            int nameColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_INVENTORY_PRODUCT_NAME);
            int priceColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_INVENTORY_PRICE);
            int quantityColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_INVENTORY_QUANTITY);
            int supplierNameColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_INVENTORY_SUPPLIER_NAME);
            int supplierPhoneNumberColumnIndex = cursor.getColumnIndex(InventoryEntry
                    .COLUMN_INVENTORY_SUPPLIER_PHONE_NUMBER);

            // Extract out the value from the Cursor for the given column index
            String productName = cursor.getString(nameColumnIndex);
            int priceCents = cursor.getInt(priceColumnIndex);
            int quantity = cursor.getInt(quantityColumnIndex);
            String supplierName = cursor.getString(supplierNameColumnIndex);
            String supplierPhone = cursor.getString(supplierPhoneNumberColumnIndex);

            // Convert price as cents to price as dollars with 2 decimal places
            //float priceDollars = priceCents / 100;
            BigDecimal bdPrice = new BigDecimal((float) priceCents / 100);
            bdPrice = bdPrice.setScale(2, BigDecimal.ROUND_HALF_UP);
            String priceString = bdPrice.toString();

            // Update the views on the screen with the values from the database
            mNameEditText.setText(productName);
            mPriceEditText.setText(priceString);
            mQuantityEditText.setText(Integer.toString(quantity));
            mSupplierNameEditText.setText(supplierName);
            mSupplierPhoneEditText.setText(supplierPhone);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // If the loader is invalidated, clear out all the data from the input fields.
        mNameEditText.setText("");
        mPriceEditText.setText("");
        mQuantityEditText.setText("");
    }

    /**
     * Show a dialog that warns the user there are unsaved changes that will be lost
     * if they continue leaving the editor.
     *
     * @param discardButtonClickListener is the click listener for what to do when
     *                                   the user confirms they want to discard their changes
     */
    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Keep editing" button, so dismiss the dialog
                // and continue editing the product.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Prompt the user to confirm that they want to delete this product.
     */
    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the product.
                deleteProduct();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the product.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });
        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Perform the deletion of the product in the database.
     */
    private void deleteProduct() {
        // Only perform the delete if this is an existing product.
        if (mCurrentProductUri != null) {
            // Call the ContentResolver to delete the product at the given content URI.
            // Pass in null for the selection and selection args because the mCurrentPetUri
            // content URI already identifies the product that we want.
            int rowsDeleted = getContentResolver().delete(mCurrentProductUri, null, null);
            // Show a toast message depending on whether or not the delete was successful.
            if (rowsDeleted == 0) {
                // If no rows were deleted, then there was an error with the delete.
                Toast.makeText(this, getString(R.string.editor_delete_product_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the delete was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_delete_product_successful),
                        Toast.LENGTH_SHORT).show();
            }
        }
        // Close the activity
        finish();
    }

    private void setUpIncrementDecrementButtons() {
        mDecrementButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String quantityString = mQuantityEditText.getText().toString().trim();
                int quantity = 0;
                //Check that the EditView is not empty or null. If it is, quantity = 0
                if (!quantityString.isEmpty()) {
                    quantity = Integer.parseInt(quantityString);
                    if (quantity > 0) {
                        quantity--;
                    }
                }
                //set EditView for quantity
                mQuantityEditText.setText(Integer.toString(quantity));
            }
        });

        mIncrementButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(LOG_TAG, "Increment Button onClick established");
                String quantityString = mQuantityEditText.getText().toString().trim();
                Log.i(LOG_TAG, "Increment Button quantityString = " + quantityString);
                int quantity = 1;
                Log.i(LOG_TAG, "Increment Button quantity b4 if = " + quantity);
                //Check that the EditView is not empty or null. If it is, quantity = 1;
                if (!quantityString.isEmpty()) {
                    quantity = Integer.parseInt(quantityString);
                    Log.i(LOG_TAG, "Increment Button quantity after parse = " + quantity);
                    quantity++;
                    Log.i(LOG_TAG, "Increment Button quantity++ = " + quantity);
                }
                Log.i(LOG_TAG, "Increment Button quantity b4 setText = " + quantity);
                //set EditView for quantity
                mQuantityEditText.setText(Integer.toString(quantity));
            }
        });
    }
}