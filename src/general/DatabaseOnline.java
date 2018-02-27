package general;

import com.google.firebase.*;
import com.google.firebase.database.*;
import com.google.firebase.auth.*;
import individuals.IssueFirebase;
import individuals.IssueOnline;
import individuals.SprintOnline;
import general.Fields.Field;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

/*********************************************************
 * Description:
 *
 * Accesses Firebase and writes JIRA data to it
 *
 * User: Hazel
 * Date: 8/9/2017
 * Copyright FlowPlay, Inc (www.flowplay.com)
 *********************************************************/

public class DatabaseOnline {

    private static final String kDatabaseURL = "https://database.firebaseio.com/";
    static File kSAccountURL;

    //NOTE: database - DO NOT WRITE TO DIRECTLY UNDER ANY CIRCUMSTANCES
    //To see why, go to https://firebase.google.com/docs/database/admin/save-data; "Saving Lists of Data"
    private static DatabaseReference databaseIssuesParent;
    //this is the one we are building:
    private static DatabaseReference databaseIssues;
    //this is the one we can read old data from:
    private static DatabaseReference databaseOldIssues;
    private static Long currVersion = (long) -1;

    /**
     * Connects database to a writeable and readable firebase at kDatabaseURL
     */
    public static void initialize(String path) {
        System.out.println("Connecting to Firebase " + path);
        try {
            // [START initialize]
            FileInputStream serviceAccount = new FileInputStream(kSAccountURL);
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredential(FirebaseCredentials.fromCertificate(serviceAccount))
                    .setDatabaseUrl(kDatabaseURL)
                    .build();
            FirebaseApp.initializeApp(options);
            System.out.println("Connected to Firebase successfully.");
            // [END initialize]
        } catch (FileNotFoundException e) {
            System.out.println("ERROR: invalid service account credentials. See README.");
            System.out.println(e.getMessage());

            System.exit(1);
        } catch (Exception e) {
            System.out.println("ERROR");
            System.out.println(e.getMessage());
        }

        // Shared Database reference
        databaseIssuesParent = FirebaseDatabase.getInstance().getReference(path);

        // Counter set to 1 so only waiting for one countDown()
        CountDownLatch counter = new CountDownLatch(1) ;

        System.out.println("Reading current issue version.");
        databaseIssuesParent.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Object version = dataSnapshot.child("version").getValue();
                if (version instanceof Long) {
                    currVersion = (Long) version + 1;
                    System.out.println("Current version is: issues-" + version);
                    databaseIssues = databaseIssuesParent.child("issues-" + currVersion);
                    databaseOldIssues = databaseIssuesParent.child("issues-" + (currVersion - 1));

                    //There's only something here if the clear broke last time
                    //(version number incremented, then clear failed so we're not quite done with last time)
                    Object leftoverClear = dataSnapshot.child("issues-" + (currVersion - 2)).getValue();
                    if (leftoverClear != null) {
                        System.err.println("Last time, clearing issues-" + (currVersion - 2) + " failed.");
                        clear(databaseIssuesParent.child("issues-" + (currVersion - 2)));
                    }

                    //There's only something here if writing failed last time
                    //(version number didn't increment)
                    Object leftoverWrite = dataSnapshot.child("issues-" + currVersion).getValue();
                    if (leftoverWrite != null) {
                        System.err.println("Last time, writing to issues-" + currVersion + " failed.");
                        clear(databaseIssues);
                    }
                } else {
                    System.err.println("Unrecognized version " + version.toString());
                }

                counter.countDown();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.out.println("Reading from firebase failed: " + databaseError.getMessage());
                counter.countDown();
            }
        });

        waitForCounter(counter, "current issue version") ;
    }

    private static void waitForCounter(CountDownLatch counter, String description) {
        try {
            //System.out.println("Waiting for " + description) ;
            counter.await();
            //System.out.println("Finished    " + description) ;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Writes [data] to firebase [kDatabaseURL]'s child [folder]
     * @param data An object to write to the firebase
     * @param id A child in firebase to write to
     */
    public static void write(Object data, String id) {

        if (data instanceof IssueOnline) {
            IssueOnline iss = (IssueOnline) data;
            HashMap<Field, String> fieldsMap = iss.getFieldsMap();
            HashMap<String, String> stringMap = new HashMap<>();

            for (Field field : Fields.Field.values()) {
                stringMap.put(field.name(), fieldsMap.get(field));
            }

            IssueFirebase firebase = new IssueFirebase();
            firebase.setComments(iss.getComments());
            firebase.setFields(stringMap);
            firebase.setWatchers(iss.getWatchers());

            data = firebase;
        }

        CountDownLatch counter = new CountDownLatch(1) ;

        databaseIssues.child(id).setValue(data, (DatabaseError databaseError, DatabaseReference databaseReference) ->
        {
            if (databaseError != null) {
                System.err.println("Data could not be saved " + databaseError);
            }

            counter.countDown();
        });

        waitForCounter(counter, "writing to firebase");
    }

    /**
     * Reads every issue from firebase
     */
    public static void readIssues(HashMap<String, IssueOnline> issueList) {
        System.out.println("Reading from firebase.");

        // Counter set to 1 so only waiting for one countDown()
        CountDownLatch counter = new CountDownLatch(1) ;

        databaseOldIssues.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //Loop through each issue/sprint/epic and create an object for it
                for (DataSnapshot obj : dataSnapshot.getChildren()) {
                    if (obj != null) {
                        IssueOnline newIssue = new IssueOnline();

                        HashMap<Field, String> fieldsMap = new HashMap<>();

                        for (Field currField : Fields.Field.values()) {
                            fieldsMap.put(currField, (String) obj.child("fields/" + currField.name()).getValue());
                        }

                        newIssue.setWatchers((ArrayList<String>) obj.child("watchers").getValue());
                        newIssue.setComments((ArrayList<HashMap<String, String>>) obj.child("comments").getValue());

                        newIssue.setFieldsMap(fieldsMap);
                        issueList.put(fieldsMap.get(Fields.Field.id), newIssue);
                    }
                }

                counter.countDown();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.out.println("Reading from firebase failed: " + databaseError.getMessage());
                counter.countDown();
            }
        });

        waitForCounter(counter, "reading issues from firebase");
    }

    /**
     * Reads every sprint from firebase
     */
    public static void readSprints(HashMap<String, SprintOnline> issueList) {
        System.out.println("Reading from firebase.");

        // Counter set to 1 so only waiting for one countDown()
        CountDownLatch counter = new CountDownLatch(1) ;

        databaseOldIssues.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //Loop through each issue/sprint/epic and create an object for it
                for (DataSnapshot obj : dataSnapshot.getChildren()) {
                    if (obj != null) {
                        SprintOnline newIssue = new SprintOnline();

                        newIssue.setName((String) obj.child("name").getValue());
                        newIssue.setState((String) obj.child("state").getValue());
                        String id = (String) obj.child("id").getValue();
                        newIssue.setID(id);

                        issueList.put(id, newIssue);
                    }
                }

                counter.countDown();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.out.println("Reading from firebase failed: " + databaseError.getMessage());
                counter.countDown();
            }
        });

        waitForCounter(counter, "reading sprints from firebase");
    }

    /**
     * Updates the version of the list of issues
     */
    public static void updateVersion() {
        CountDownLatch counter = new CountDownLatch(1) ;

        databaseIssuesParent.child("version").setValue(currVersion, (DatabaseError databaseError, DatabaseReference databaseReference) ->
        {
            if (databaseError != null) {
                System.err.println("Version could not be updated " + databaseError);
            } else {
                System.out.println("New version is " + currVersion);
            }

            counter.countDown();
        });

        waitForCounter(counter, "updating version on firebase");
    }

    /**
     * Clears the given firebase database child
     */
    public static void clear() {
        if (databaseOldIssues != null) {
            databaseOldIssues.removeValue();
            System.out.println("Cleared database.");
        } else {
            System.out.println("Couldn't find issues to clear.");
        }
    }

    /**
     * Clears the given firebase database child
     * @param child The database node to clear
     */
    private static void clear(DatabaseReference child) {
        child.removeValue();
        System.out.println("Cleared database " + child);
    }
}
