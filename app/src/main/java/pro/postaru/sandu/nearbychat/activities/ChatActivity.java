package pro.postaru.sandu.nearbychat.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import pro.postaru.sandu.nearbychat.R;
import pro.postaru.sandu.nearbychat.adapters.ChatAdapter;
import pro.postaru.sandu.nearbychat.constants.Database;
import pro.postaru.sandu.nearbychat.models.Message;
import pro.postaru.sandu.nearbychat.models.UserProfile;

public class ChatActivity extends AppCompatActivity {

    public static final String PARTNER_USER_PROFILE = "PARTNER_USER_PROFILE";

    private String conversationId;

    private List<Message> messages;

    private ChatAdapter chatAdapter;

    private EditText messageEditView;
    private ImageButton messageSendButton;
    private final TextWatcher editMessageTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

            if (s.length() == 0) {
                messageSendButton.setEnabled(false);
            } else {
                messageSendButton.setEnabled(true);
            }
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };
    private ListView messageListView;
    private ProgressBar progressBar;
    private final ChildEventListener messageListener = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {

            Message message = dataSnapshot.getValue(Message.class);

            if (message != null) {
                chatAdapter.add(message);
                messageListView.setSelection(messages.size() - 1);
            } else {
                Log.w("BBB", "No messages");
            }

            if (progressBar.getVisibility() == View.VISIBLE) {
                hideProgressBar();
            }
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {
        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            Log.w("BBB", "loadPost:onCancelled", databaseError.toException());
        }
    };
    private UserProfile conversationPartner;
    private FirebaseAuth auth;
    private FirebaseUser user;
    private DatabaseReference database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference();
        user = auth.getCurrentUser();

        // spinner

        progressBar = (ProgressBar) findViewById(R.id.chat_spinner);

        conversationPartner = (UserProfile) getIntent().getSerializableExtra(PARTNER_USER_PROFILE);

        messageEditView = (EditText) findViewById(R.id.message_edit);
        messageEditView.addTextChangedListener(editMessageTextWatcher);

        messageSendButton = (ImageButton) findViewById(R.id.message_send);
        messageSendButton.setEnabled(false);
        messageSendButton.setOnClickListener(v -> sendMessage());

        messages = new ArrayList<>();

        conversationId = getConversationId(conversationPartner.getId());

        database.child(Database.userMessages)
                .child(conversationId)
                .child("messages")
                .addChildEventListener(messageListener);

        chatAdapter = new ChatAdapter(this, R.layout.chat_entry, messages);
        messageListView = (ListView) findViewById(R.id.message_list);
        messageListView.setVisibility(View.GONE);

        messageListView.setAdapter(chatAdapter);

        // set conversation title
        setTitle(conversationPartner.getUserName());

        // hide keyboard by default
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }

    public void sendMessage() {

        String content = messageEditView.getText().toString();
        messageEditView.setText("");

        Message newMessage = new Message();
        newMessage.setText(content);
        newMessage.setDate(new Date());
        newMessage.setSenderId(user.getUid());

        String id = database.child(Database.userMessages)
                .child(conversationId)
                .child("messages")
                .push()
                .getKey();

        newMessage.setId(id);

        database.child(Database.userMessages)
                .child(conversationId)
                .child("messages")
                .child(id)
                .setValue(newMessage);
    }

    private String getConversationId(String partnerId) {
        String myId = user.getUid();

        if (myId.compareTo(partnerId) < 0) {
            return myId + "-" + partnerId;
        } else {
            return partnerId + "-" + myId;
        }
    }

    private void hideProgressBar() {
        progressBar.setVisibility(View.GONE);

        messageListView.setVisibility(View.VISIBLE);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        database.child(Database.userMessages)
                .child(conversationId)
                .child("messages")
                .removeEventListener(messageListener);
    }

}
