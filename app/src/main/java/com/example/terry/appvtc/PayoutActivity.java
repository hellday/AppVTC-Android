package com.example.terry.appvtc;

import android.app.Activity;
import android.content.Intent;
import android.preference.PreferenceActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.braintreepayments.api.dropin.DropInActivity;
import com.braintreepayments.api.dropin.DropInRequest;
import com.braintreepayments.api.dropin.DropInResult;
import com.braintreepayments.api.dropin.utils.PaymentMethodType;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;

import java.util.HashMap;

import cz.msebera.android.httpclient.Header;

public class PayoutActivity extends AppCompatActivity {

    private static final String TAG = PayoutActivity.class.getSimpleName();
    final int BRAINTREE_REQUEST_CODE = 1337;
    private static final String PATH_TO_SERVER = "http://10.0.2.2//PHP/Braintree/test.php";
    private String clientToken;

    Button btButton;
    EditText amountEditText;

    ProgressBar loadingCircle;
    ImageView paymentImage;
    TextView paymentDesc;
    ImageButton choosePaymentButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payout);

        // ActionBar
        if(getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Paiements");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        loadingCircle = findViewById(R.id.progressBar);
        paymentDesc = findViewById(R.id.paymentDesc);
        paymentImage = findViewById(R.id.paymentImage);
        choosePaymentButton = findViewById(R.id.choosePaymentButton);

        getClientTokenFromServer();

        amountEditText = findViewById(R.id.editText);

        btButton = findViewById(R.id.btButton);
        btButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBraintreeSubmit(view);
            }
        });

        choosePaymentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBraintreeSubmit(view);
            }
        });
    }

    public void onBraintreeSubmit(View v) {
        DropInRequest dropInRequest = new DropInRequest()
                .vaultManager(true)
                .clientToken(clientToken);
        startActivityForResult(dropInRequest.getIntent(this), BRAINTREE_REQUEST_CODE);
    }

    private void getClientTokenFromServer() {
        AsyncHttpClient androidClient = new AsyncHttpClient();
        androidClient.get(PATH_TO_SERVER, new TextHttpResponseHandler() {
            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                Log.d(TAG, "Token Failed: " + responseString);
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseToken) {
                Log.d(TAG, "Client token: " + responseToken);
                clientToken = responseToken;
                checkLastPaymentMethod();
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == BRAINTREE_REQUEST_CODE) {
            if (RESULT_OK == resultCode) {
                DropInResult result = data.getParcelableExtra(DropInResult.EXTRA_DROP_IN_RESULT);
                if(result != null) {
                    String paymentNonce = result.getPaymentMethodNonce().getNonce();
                    String amount = amountEditText.getText().toString();

                    HashMap<String, String> paramsHash = new HashMap<>();
                    paramsHash.put("NONCE", paymentNonce);
                    paramsHash.put("AMOUNT", amount);

                    //Send to your server
                    Log.d(TAG, "Send Payment Nonce To Server");
                    sendPaymentNonceToServer(paramsHash);
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Log.d(TAG, "User cancelled payment");
            } else {
                Exception error = (Exception) data.getSerializableExtra(DropInActivity.EXTRA_ERROR);
                Log.d(TAG, "Error exception: " + error);
            }
        }

    }

    private void sendPaymentNonceToServer(HashMap paymentInfo){
        RequestParams params = new RequestParams(paymentInfo);
        AsyncHttpClient androidClient = new AsyncHttpClient();
        androidClient.post(PATH_TO_SERVER, params, new TextHttpResponseHandler() {
            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                Log.d(TAG, "Error: Failed to create a transaction");
            }
            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseString) {
                Log.d(TAG, "Output " + responseString);
            }
        });
    }

    private void checkLastPaymentMethod() {
        DropInResult.fetchDropInResult(PayoutActivity.this, clientToken, new DropInResult.DropInResultListener() {
            @Override
            public void onError(Exception exception) {
                // an error occurred
                Toast.makeText(PayoutActivity.this, "Exception : " + exception, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onResult(DropInResult result) {
                if (result.getPaymentMethodType() != null) {
                    // Use the icon and name to show in your UI
                    int icon = result.getPaymentMethodType().getDrawable();
                    int name = result.getPaymentMethodType().getLocalizedName();
                    paymentImage.setImageResource(icon);

                    PaymentMethodType paymentMethodType = result.getPaymentMethodType();
                    if (paymentMethodType == PaymentMethodType.ANDROID_PAY || paymentMethodType == PaymentMethodType.GOOGLE_PAYMENT) {
                        // The last payment method the user used was Android Pay or Google Pay.
                        // The Android/Google Pay flow will need to be performed by the
                        // user again at the time of checkout.
                    } else {
                        // Use the payment method show in your UI and charge the user
                        // at the time of checkout.
                        PaymentMethodNonce paymentMethod = result.getPaymentMethodNonce();
                        if(paymentMethod != null) {
                            paymentDesc.setText(paymentMethod.getDescription());
                            paymentMethod.describeContents();

                            // Trigger Visibility of Buttons
                            loadingCircle.setVisibility(View.INVISIBLE);
                            choosePaymentButton.setVisibility(View.VISIBLE);
                        }
                    }
                } else {
                    // There was no existing payment method
                    loadingCircle.setVisibility(View.INVISIBLE);
                    paymentDesc.setText("Ajouter un moyen de paiement");
                    choosePaymentButton.setImageResource(android.R.drawable.ic_menu_add);
                    choosePaymentButton.setVisibility(View.VISIBLE);
                }
            }

        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}


