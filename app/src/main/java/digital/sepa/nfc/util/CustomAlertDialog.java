package digital.sepa.nfc.util;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;

public abstract class CustomAlertDialog {
    private final Context _ctx;
    private final String _title;
    private final String _message;

    /**
     * Constructor
     *
     * @param ctx
     * @param title
     * @param message
     */
    public CustomAlertDialog(Context ctx, String title, String message) {
        super();
        this._ctx = ctx;
        this._title = title;
        this._message = message;
    }

    /**
     * Show the dialog
     */
    public void show() {
        Builder builder = Utils.buildAlertDialog(_ctx, _title, _message);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                onOkClick();
            }
        }).create().show();
    }

    /**
     * Abstract method executed when user clicks ok
     */
    public abstract void onOkClick();
}
