package com.example.android.inventoryapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.example.android.inventoryapp.data.InventoryContract.InventoryEntry;

import java.text.NumberFormat;
import java.util.Locale;

public class InventoryCursorAdapter extends CursorAdapter {

    String LOG_TAG = InventoryCursorAdapter.class.getSimpleName();

    /**
     * Constructs a new {@link InventoryCursorAdapter}.
     *
     * @param context The context
     * @param c       The cursor from which to get the data.
     */
    public InventoryCursorAdapter(Context context, Cursor c) {
        super(context, c, 0 /* flags */);
    }

    /**
     * Makes a new blank list item view. No data is set (or bound) to the views yet.
     *
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already
     *                moved to the correct position.
     * @param parent  The parent to which the new view is attached to
     * @return the newly created list item view.
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        // Inflate a list item view using the layout specified in list_item.xml
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    /**
     * This method binds the pet data (in the current row pointed to by cursor) to the given
     * list item layout. For example, the name for the current pet can be set on the name TextView
     * in the list item layout.
     *
     * @param view    Existing view, returned earlier by newView() method
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already moved to the
     *                correct row.
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        final Context appContext = context;

        // Find individual views that we want to modify in the list item layout
        TextView nameTextView = (TextView) view.findViewById(R.id.product_name_text_view);
        TextView priceTextView = (TextView) view.findViewById(R.id.price_text_view);
        final TextView quantityTextView = (TextView) view.findViewById(R.id.quantity_text_view);
        Button saleButton = (Button) view.findViewById(R.id.sale_button);

        // Find the columns of product attributes that we're interested in
        int nameColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_INVENTORY_PRODUCT_NAME);
        int priceColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_INVENTORY_PRICE);
        int quantityColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_INVENTORY_QUANTITY);
        int idColumnIndex = cursor.getColumnIndex(InventoryEntry._ID);

        // Read the product attributes from the Cursor for the current pet
        String productName = cursor.getString(nameColumnIndex);
        int priceCents = cursor.getInt(priceColumnIndex);
        final String productQuantity = cursor.getString(quantityColumnIndex);
        int currentProductId = cursor.getInt(idColumnIndex);

        final Uri currentProductUri = Uri.parse(InventoryEntry.CONTENT_URI + "/" +
                currentProductId);

        // Convert price as cents to price as dollars
        float priceDollars = (float) priceCents / 100;
        //Convert float price to string price using NumberFormat for proper formatting
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
        String priceString = currencyFormat.format(priceDollars);

        // Update the TextViews with the attributes for the current product
        nameTextView.setText(productName);
        priceTextView.setText(priceString);
        quantityTextView.setText(productQuantity);

        //Sale button to subtract 1 from quantity for the specific product
        saleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //If the quantity is empty or null (which it should not be)
                if (productQuantity.isEmpty()) {
                    return;
                }
                int quantity = Integer.parseInt(productQuantity);

                //Only run following code if quantity > 0
                if (quantity > 0) {
                    quantity--;
                    ContentValues values = new ContentValues();
                    values.put(InventoryEntry.COLUMN_INVENTORY_QUANTITY, quantity);
                    int rowsAffected = appContext.getContentResolver().update(currentProductUri, values,
                            null, null);
                }
            }
        });
    }
}