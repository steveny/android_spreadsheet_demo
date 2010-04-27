package com.steveny;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.steveny.Constants.*;

public class NetWorth extends Activity implements View.OnClickListener {

    static final int SETTINGS_REQUEST = 0;

    private final Handler guiThread = new Handler();
    private NetWorthData netWorthData;
    private Button refreshButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        netWorthData = new NetWorthData(this);

        setContentView(R.layout.main);

        View exitButton = findViewById(R.id.exit_button);
        exitButton.setOnClickListener(this);

        refreshButton = (Button) findViewById(R.id.refresh_button);
        refreshButton.setOnClickListener(this);

        View historyButton = findViewById(R.id.history_button);
        historyButton.setOnClickListener(this);

        View aboutButton = findViewById(R.id.about_button);
        aboutButton.setOnClickListener(this);

        enableIfSettings();
    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (!Settings.isSettingsProvided(this)) {
            startActivityForResult(new Intent(this, Settings.class), SETTINGS_REQUEST);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SETTINGS_REQUEST) {
            enableIfSettings();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                startActivityForResult(new Intent(this, Settings.class), SETTINGS_REQUEST);
                return true;
            case R.id.clear:
                new AlertDialog.Builder(this).setMessage(R.string.are_you_sure)
                        .setTitle("Clear History")
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                                clearHistory();
                                new AlertDialog.Builder(NetWorth.this).setMessage(R.string.clear_text)
                                        .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                dialogInterface.dismiss();
                                            }
                                        })
                                        .show();
                            }
                        })
                        .setNegativeButton(R.string.no, null)
                        .show();

                return true;
        }
        return false;
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.refresh_button:
                refresh();
                break;
            case R.id.history_button:
                history();
                break;
            case R.id.exit_button:
                finish();
                break;
            case R.id.about_button:
                startActivity(new Intent(this, About.class));
                break;
        }
    }

    private void refresh() {
        ExecutorService transThread = Executors.newSingleThreadExecutor();
        TextView networth = (TextView) findViewById(R.id.networth_text);
        networth.setTextColor(Color.GRAY);
        networth.setText("Refreshing....");
        refreshButton.setEnabled(false);
        Runnable updateTask = new Runnable() {
            public void run() {
                try {

                    String cellContents = getCell(authorize(), Settings.getSpreadsheet(NetWorth.this), "R" + Settings.getRow(NetWorth.this) + "C" + Settings.getColumn(NetWorth.this));

                    double networth = Double.valueOf(cellContents.substring(1));
                    double previousNetworth = previousNetworth();

                    if (Settings.isDemo(NetWorth.this)) {
                        networth = (networth * Math.random()) / Math.random();
                        previousNetworth = networth * Math.random();
                    }

                    addNetWorth(networth);

                    NumberFormat numberFormat = NumberFormat.getCurrencyInstance();
                    guiSetText((TextView) findViewById(R.id.networth_text),
                            numberFormat.format(networth),
                            previousNetworth < networth ? Color.GREEN : (previousNetworth > networth ? Color.RED : Color.GRAY));
                    guiSetText((TextView) findViewById(R.id.networth_previous), "(" + numberFormat.format(previousNetworth) + ")", Color.GRAY);
                } catch (Exception e) {
                    Log.d("exception in refresh", e.getMessage());
                    throw new RuntimeException(e);
                } finally {
                    guiThread.post(new Runnable() {
                        public void run() {
                            refreshButton.setEnabled(true);
                        }
                    });
                }
            }
        };
        transThread.submit(updateTask);
    }

    private void addNetWorth(double networth) {
        if (!Settings.isDemo(this)) {
            SQLiteDatabase db = netWorthData.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(NET_WORTH, networth);
            values.put(TIME, System.currentTimeMillis());
            db.insertOrThrow(TABLE_NAME, null, values);
        }
    }

    private void clearHistory() {
        if (!Settings.isDemo(this)) {
            SQLiteDatabase db = netWorthData.getWritableDatabase();
            db.execSQL("delete from " + TABLE_NAME);
        }
    }

    private double previousNetworth() {
        SQLiteDatabase db = netWorthData.getReadableDatabase();
        double previous = 0.0;
        Cursor cursor = db.query(TABLE_NAME, new String[]{_ID, NET_WORTH, TIME}, null, null, null, null, TIME + " DESC", " 1 ");
        startManagingCursor(cursor);
        while (cursor.moveToNext()) {
            previous = cursor.getDouble(1);
        }
        cursor.close();
        return previous;
    }

    private String authorize() throws IOException {
        HttpClient httpClient = new DefaultHttpClient();
        HttpPost request = new HttpPost("https://www.google.com/accounts/ClientLogin");
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(5);
        nameValuePairs.add(new BasicNameValuePair("Email", Settings.getEmail(this)));
        nameValuePairs.add(new BasicNameValuePair("source", "sfy-networth"));
        nameValuePairs.add(new BasicNameValuePair("accountType", "GOOGLE"));
        nameValuePairs.add(new BasicNameValuePair("Passwd", Settings.getPassword(this)));
        nameValuePairs.add(new BasicNameValuePair("service", "wise"));
        request.setEntity(new UrlEncodedFormEntity(nameValuePairs));

        String authorization = "";
        HttpResponse response = httpClient.execute(request);
        int status = response.getStatusLine().getStatusCode();
        if (status != HttpStatus.SC_OK) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            response.getEntity().writeTo(outputStream);
        } else {
            BufferedReader content = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            String line;
            while ((line = content.readLine()) != null) {
                if (line.startsWith("Auth=")) {
                    authorization = line;
                }
            }
            content.close();
        }
        return authorization.split("=")[1];
    }


    public interface DomHandler {
        public String getValue(Element root);
    }

    public class IdDomHander implements DomHandler {
        public String getValue(Element root) {
            NodeList items = root.getElementsByTagName("entry");
            String value = null;
            for (int i = 0; i < items.getLength(); i++) {
                Node item = items.item(i);
                value = item.getFirstChild().getFirstChild().getNodeValue();
                value = value.substring(value.lastIndexOf('/') + 1);
            }
            return value;
        }

    }

    private String findSpreadsheet(String authorizationToken, String spreadsheetName) throws IOException, SAXException, ParserConfigurationException {
        return getValue(authorizationToken, "/spreadsheets/private/full?title=" + spreadsheetName, new IdDomHander());
    }

    private String findWorksheet(String authorizationToken, String spreadsheetId) throws IOException, SAXException, ParserConfigurationException {
        return getValue(authorizationToken, "/worksheets/" + spreadsheetId + "/private/full", new IdDomHander());
    }

    private String getCell(String authorizationToken, String spreadsheet, String cellName) throws IOException, SAXException, ParserConfigurationException {
        String spreadsheetId = findSpreadsheet(authorizationToken, spreadsheet);
        String worksheetId = findWorksheet(authorizationToken, spreadsheetId);        

        return getValue(authorizationToken, "/cells/"+spreadsheetId+"/"+worksheetId+"/private/basic/" + cellName, new DomHandler() {
            public String getValue(Element root) {
                NodeList items = root.getElementsByTagName("content");
                String value = null;
                for (int i = 0; i < items.getLength(); i++) {
                    Node item = items.item(i);
                    value = item.getFirstChild().getNodeValue();
                }
                return value;
            }
        });
    }

    private String getValue(String authorizationToken, String path, DomHandler domHandler) throws IOException, SAXException, ParserConfigurationException {
        HttpClient httpClient = new DefaultHttpClient();
        String uri = "http://spreadsheets.google.com/feeds" + path;
        HttpGet request = new HttpGet(uri);
        request.addHeader("Authorization", "GoogleLogin auth=" + authorizationToken);
        request.addHeader("Content-Type", "application/atom+xml");
        HttpResponse response = httpClient.execute(request);
        String cellContents = null;

        int status = response.getStatusLine().getStatusCode();
        if (status != HttpStatus.SC_OK) {
            throw new IOException("Could not retrieve document from " + uri);
        } else {
            InputStream content = response.getEntity().getContent();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document dom = builder.parse(content);
            Element root = dom.getDocumentElement();
            cellContents = domHandler.getValue(root);
            content.close();
        }
        return cellContents;
    }


    private void guiSetText(final TextView view, final String text, final int color) {
        guiThread.post(new Runnable() {
            public void run() {
                view.setText(text);
                view.setTextColor(color);
            }
        });
    }

    private void history() {
        Intent intent = new Intent(this, History.class);
        startActivity(intent);
    }

    private void enableIfSettings() {
        refreshButton.setEnabled(Settings.isSettingsProvided(this));
    }
}
