package edu.carleton.syncronizedtodolists;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.Gson;

import org.w3c.dom.Text;

import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

import communication.Event;
import communication.EventSource;
import communication.Fields;
import communication.JSONEventSource;
import communication.Reactor;
import communication.ThreadWithReactor;

public class MainActivity extends AppCompatActivity {
    private static MainActivity instance;
    private static User user;
    public static EventSource source;
    private static Reactor reactor;
    private static ThreadWithReactor reactorThread;
    //UI components
    private TextView usernameView;
    private TextView displayView;
    private ListView listsView;
    private ListView itemsView;
    private Button newListBtn;

    private ListAdapter listsAdapter;
    private ListAdapter itemsAdapter;

    private ArrayList<List> lists;
    private ArrayList<Item> items;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //init UI components
        usernameView = (TextView) findViewById(R.id.usernameView);
        displayView = (TextView) findViewById(R.id.displayView);
        listsView = (ListView) findViewById(R.id.listListView);
        itemsView = (ListView) findViewById(R.id.listListView);
        newListBtn = (Button) findViewById(R.id.newListBtn);

        lists = new ArrayList<List>();
        items = new ArrayList<Item>();


        instance = this;
        reactor = new Reactor();

        listsAdapter = new ArrayAdapter<List>(instance, android.R.layout.simple_list_item_1, lists);
        itemsAdapter = new ArrayAdapter<Item>(instance, android.R.layout.simple_list_item_1, items);
        listsView.setAdapter(listsAdapter);
        itemsView.setAdapter(itemsAdapter);

        //register handlers here
        reactor.register(Fields.LOGIN_RES, new LoginResHandler());

        ///////handlers^^

        LoginRun login = new LoginRun();
        Thread thread = new Thread(login);
        thread.start();
        //set listener for button to create a new List
        //go to a new List page in edit mode
        newListBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            //dialog box asking for name
            public void onClick(View view) {
                final MainActivity ma = MainActivity.getInstance();
                AlertDialog.Builder builder = new AlertDialog.Builder(ma);
                builder.setTitle("New List: please enter list's name");
                final EditText input = new EditText(MainActivity.getInstance());
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                builder.setView(input);
                // Set up the buttons
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        ma.newList(input.getText().toString());
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                builder.show();
            }
        });
    }

    public void newList(final String name){
        final List list = new List(name, user.getUserName());


        Runnable newList = new Runnable() {
            @Override
            public void run() {
                Intent i = new Intent(getInstance(), ListActivity.class);
                Gson gson = new Gson();
                String listJson = gson.toJson(list).toString();
                HashMap<String, Serializable> map = new HashMap<>();
                map.put(Fields.TYPE, Fields.NEW_LIST);
                map.put(Fields.LIST, list);
                Event event = new Event(source, map);
                try {
                    source.putEvent(event);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                i.putExtra("LIST", listJson);
                startActivity(i);
            }
        };

        Thread thread = new Thread(newList);
        thread.start();
        user.getLists().add(list);
    }


    public void setSource(EventSource s) {
        source = s;
        reactorThread = new ThreadWithReactor(source, reactor);
        reactorThread.start();
    }

    public static MainActivity getInstance(){
        return instance;
    }

    public void setUser(User u){
        user = u;
    }

    synchronized public void renderUserInfo(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                displayView.setText(user.getDisplayName());
                usernameView.setText(user.getUserName());
                Log.i("LISTS", user.getLists().toString());
                lists.addAll(user.getLists());
                items.addAll(user.getItems());
            }
        });
        lists.notify();
        items.notify();
    }
}