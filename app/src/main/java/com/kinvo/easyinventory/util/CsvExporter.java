
package com.kinvo.easyinventory.util;

import com.kinvo.easyinventory.model.Product;
import java.util.List;

public final class CsvExporter {
    private CsvExporter() {}

    public static String toCsv(List<Product> items) {
        StringBuilder sb = new StringBuilder();
        // Adjust columns to match your Product model
        sb.append("SKU,Barcode,Description,Price,Quantity\n");
        for (Product p : items) {
            sb.append(cell(p.getSku()))
                    .append(',').append(cell(p.getBarcode()))
                    .append(',').append(cell(p.getDescription()))
                    .append(',').append(cell(p.getPriceBig()))
                    .append(',').append(cell(p.getCurrentStock()))
                    .append('\n');
        }
        return sb.toString();
    }

    private static String cell(Object v) {
        if (v == null) return "\"\"";
        String s = String.valueOf(v);
        // Escape quotes per CSV rules
        s = s.replace("\"", "\"\"");
        return "\"" + s + "\"";
    }
}

