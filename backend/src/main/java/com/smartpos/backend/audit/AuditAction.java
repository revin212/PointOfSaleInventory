package com.smartpos.backend.audit;

public final class AuditAction {
    private AuditAction() {}

    public static final String LOGIN_SUCCESS  = "LOGIN_SUCCESS";
    public static final String LOGOUT         = "LOGOUT";

    public static final String USER_CREATE    = "USER_CREATE";
    public static final String USER_UPDATE    = "USER_UPDATE";
    public static final String USER_SET_ACTIVE = "USER_SET_ACTIVE";

    public static final String CATEGORY_CREATE = "CATEGORY_CREATE";
    public static final String CATEGORY_UPDATE = "CATEGORY_UPDATE";
    public static final String CATEGORY_DELETE = "CATEGORY_DELETE";

    public static final String SUPPLIER_CREATE = "SUPPLIER_CREATE";
    public static final String SUPPLIER_UPDATE = "SUPPLIER_UPDATE";
    public static final String SUPPLIER_DELETE = "SUPPLIER_DELETE";

    public static final String PRODUCT_CREATE = "PRODUCT_CREATE";
    public static final String PRODUCT_UPDATE = "PRODUCT_UPDATE";
    public static final String PRODUCT_DELETE = "PRODUCT_DELETE";

    public static final String PURCHASE_CREATE  = "PURCHASE_CREATE";
    public static final String PURCHASE_RECEIVE = "PURCHASE_RECEIVE";

    public static final String SALE_CREATE = "SALE_CREATE";
    public static final String SALE_CANCEL = "SALE_CANCEL";

    public static final String STOCK_ADJUSTMENT = "STOCK_ADJUSTMENT";
}
