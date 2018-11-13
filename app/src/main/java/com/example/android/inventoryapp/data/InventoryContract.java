package com.example.android.inventoryapp.data;


import android.provider.BaseColumns;

public final class InventoryContract {

    public static final class InventoryEntry implements BaseColumns {
        public static final String TABLE_NAME = "inventory";

        public static final String _ID = BaseColumns._ID;
        public static final String COLUMN_INVENTORY_PRODUCT_NAME = "product_name";
        public static final String COLUMN_INVENTORY_PRICE = "price";
        public static final String COLUMN_INVENTORY_QUANTITY = "quantity";
        public static final String COLUMN_INVENTORY_SUPPLIER_NAME = "supplier_name";
        public static final String COLUMN_INVENTORY_SUPPLIER_PHONE_NUMBER = "supplier_phone_number";
    }
}
